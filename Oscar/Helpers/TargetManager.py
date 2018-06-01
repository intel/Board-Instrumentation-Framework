##############################################################################
#  Copyright (c) 2016 Intel Corporation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##############################################################################
#    File Abstract: 
#    Keepts track of all targets that I'm sending data to
#
##############################################################################
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Data import ConnectionPoint
from Util import Sleep
from Helpers import Statistics
from Helpers import Target
from Helpers import ThreadManager
import threading
import sys
import re
import time
import os
from os import remove, close
from tempfile import mkstemp
from shutil import move, copy

 
class TargetManager():
    _instance = None
    def __init__(self):
        if TargetManager._instance == None: # singleton pattern
            self.__initialize()
        else:
            self = TargetManager._instance

    def __initialize(self):
        self._UpstreamTargets = {} # data being sent towards Minion
        self._DownstreamTargets = {} # data being sent torwads Marvin
        TargetManager._instance = self

        from Helpers import Configuration

        self.__UseGoverner = Configuration.get().GetUseGoverner()
        self.__GovernerThreshhold = Configuration.get().GetGovernerThreshhold()
        self.__GovernerBackoffPeriod = Configuration.get().GetGovernerBackoffPeriod()
        self.__GovernerMaxPacketsBeforeRest = Configuration.get().GetGovernerMaxPacketsBeforeRest()
        self.__PacketSentSinceRest = 0
        self.__BytestSentSinceRest = 0
        self.__ShuntedDataByFile = {}
        self.__ShuntLock = threading.Lock()
        self.__ShuntThreadCreated = False
        self.__DownstreamPacketQueue = []
        self.__DownstreamPacketLock = threading.Lock()
        self.__DownstreamPacketThreads = 0
        self.__DownstreamPacketThreadsLock = threading.Lock() 


    def GetUpstreamTarget(self,TargetID):
        if TargetID in self._UpstreamTargets:
            return self._UpstreamTargets[TargetID]
        else:
            return None

    def GetDownstreamTarget(self,TargetID):
        if TargetID in self._DownstreamTargets:
            return self._DownstreamTargets[TargetID]
        else:
            return None

    #searched for DNS name and IP
    def GetDownstreamTargetEx(self,IP,Port):
        for targetTuple in self._DownstreamTargets.items():
            target = targetTuple[1]
            if target.getResolvedIP() == IP and int(Port) == int(target.getPort()):
                return target
        return None

    def GetDownstreamTargets(self):
        return self._DownstreamTargets

    # towards a Minion
    def AddUpstreamTarget(self,objTarget,TargetID):
        if None != self.GetUpstreamTarget(TargetID):
            Log.getLogger().error("Attempted to add duplicate Upstream target: " + TargetID)
            return

        self._UpstreamTargets[TargetID] = objTarget
        Log.getLogger().info("Adding Upstream Target: " + str(objTarget))

    # towards a Marvin
    def AddDownstreamTarget(self,objTarget,TargetID):
        if None != self.GetDownstreamTarget(TargetID):
            Log.getLogger().warning("Attempted to add duplicate Downstream  target: " + TargetID)
            return

        self._DownstreamTargets[TargetID] = objTarget
        Log.getLogger().info("Adding Downstream Target: " + str(objTarget))


    def RemoveDynamicDownstreamTarget(self, TargetID):
        if None == self.GetDownstreamTarget(TargetID):
            Log.getLogger().error("Attempted to remove non-existant Dynamic Marvin Connection: " + TargetID)
            return

        del self._DownstreamTargets[TargetID]


    def __ResetGovernerStats(self):
        self.__PacketSentSinceRest = 0
        self.__BytestSentSinceRest = 0

    def __GovernerRest(self):
        Sleep.SleepMs(self.__GovernerBackoffPeriod)

    ## This is to make sure we don't overflow TX socket buffer
    def __Governer(self,sentSize):
        self.__PacketSentSinceRest += 1
        self.__BytestSentSinceRest += sentSize
        if self.__PacketSentSinceRest >= self.__GovernerMaxPacketsBeforeRest:
            self.__GovernerRest()
            self.__PacketSentSinceRest = 0
            #Log.getLogger().debug("sleeping - Packets")

            return

        if self.__BytestSentSinceRest >= self.__GovernerThreshhold:
            #Log.getLogger().debug("sleeping - Buff Size: " +
            #str(self.__BytestSentSinceRest))

            self.__GovernerRest()
            self.__ResetGovernerStats()
            return

    # Towards a Marvin
    def SendToDownstreamTarget(self,sendBuffer,TargetID,ignoreTimeout):
        target = self.GetDownstreamTarget(TargetID)

        if None == target:
            Log.getLogger().error("Attempted to send to invalid downstream target id: " + TargetID)
            return False

        if True == target.Send(sendBuffer,ignoreTimeout):
            Statistics.GetStatistics().OnPacketSentDownstream(sendBuffer)
            if self.__UseGoverner:
                self.__Governer(len(sendBuffer))
            return True

        else:
            Statistics.GetStatistics().OnPacketDropped()
            return False

    # towards a Minion
    def SendToUpstreamTarget(self,sendBuffer,TargetID):
        target = self.GetUpstreamTarget(TargetID)
        if None == target:
            Log.getLogger().error("Attempted to send to invalid upstream target id: " + TargetID)
            return False

        target.Send(sendBuffer,True) # timeouts don't matter for upstream, only down, so sent True to ignore
        Statistics.GetStatistics().OnPacketSentUpstream(sendBuffer)
        return True

    def BroadcastUpstream(self,sendBuffer):
        Statistics.GetStatistics().OnPacketBroadcastUpstream()
        sentCount = 0
        for targetKey in self._UpstreamTargets.keys():
            if self.SendToUpstreamTarget(sendBuffer,targetKey):
                sentCount += 1

        return sentCount

    def AddDownstreamPacket(self,packet):
        self.__DownstreamPacketLock.acquire()
        self.__DownstreamPacketQueue.append(packet)
        retLen = len(self.__DownstreamPacketQueue)
        self.__DownstreamPacketLock.release()

        return retLen

    def GetDownstreamPacket(self):
        self.__DownstreamPacketLock.acquire()
        if len(self.__DownstreamPacketQueue) > 0:
            retPacket = self.__DownstreamPacketQueue.pop()
        else:
            retPacket = None
        self.__DownstreamPacketLock.release()

        return retPacket

    def IncrementWorkerThreadCount(self):
        self.__DownstreamPacketThreadsLock.acquire()
        self.__DownstreamPacketThreads += 1
        retVal = self.__DownstreamPacketThreads
        self.__DownstreamPacketThreadsLock.release()

        return retVal

    def DecrementWorkerThreadCount(self):
        self.__DownstreamPacketThreadsLock.acquire()
        self.__DownstreamPacketThreads -= 1
        retVal = self.__DownstreamPacketThreads
        self.__DownstreamPacketThreadsLock.release()

        return retVal

    def GetWorkerThreadCount(self):
        self.__DownstreamPacketThreadsLock.acquire()
        retVal = self.__DownstreamPacketThreads
        self.__DownstreamPacketThreadsLock.release()

        return retVal

    def __SimpleWorker(self):
        while not ThreadManager.GetThreadManager().AllStopSignalled():
            packet = self.GetDownstreamPacket() # get data to process

            if None != packet:
                sendBuffer,ignoreTimeout,domNode,isGroup = packet
                self._BroadcastDownstream(sendBuffer,ignoreTimeout,domNode,isGroup)
            else: # no data to process, maybe reduce woker count
                if self.GetWorkerThreadCount() > 2:
                    self.DecrementWorkerThreadCount()
                    #Log.getLogger().debug("Reducing worker threads [" + str(self.GetWorkerThreadCount()) +']')
                    return
                else:
                    Sleep.SleepMs(10) 


    def BroadcastDownstream(self,sendBuffer,ignoreTimeout,domNode,isGroup=False):
        packet = (sendBuffer,ignoreTimeout,domNode,isGroup)
        waiting = self.AddDownstreamPacket(packet)
        threadCount = self.GetWorkerThreadCount()
        if  threadCount < 1 or  waiting / threadCount > 25: # if > 25 items per thread, spawn another
            newThread = threading.Thread(target=self.__SimpleWorker)
            newThread.start()
            threadCount = self.IncrementWorkerThreadCount()
            #Log.getLogger().debug(str(waiting) + " outstanding packets, adding worker thread #" + str(threadCount))


    def _BroadcastDownstream(self,sendBuffer,ignoreTimeout,domNode,isGroup=False):
        from Helpers import Configuration
        sentCount = 0

        for targetKey in self._DownstreamTargets.keys():
            if True == self.SendToDownstreamTarget(sendBuffer,targetKey,ignoreTimeout):
                sentCount += 1

        if sentCount > 0:
            Statistics.GetStatistics().OnPacketBroadcastDownstream()

        if Configuration.get().GetShunting() and (None != domNode or True == isGroup):
            if None != domNode and not isGroup:
                self.HandleShuntingData(domNode)
            else: # is a group and need to go through each item individually
                if None == domNode:
                    try:
                        dom = xml.dom.minidom.parseString(sendBuffer)
                        domNode = dom._get_firstChild()
                    except Exception as ex:
                       Log.getLogger().error("Error Something bad in Trying to read xml in BroadcastDownstream")

                if None != domNode:
                    for dataNode in domNode.getElementsByTagName('Oscar'):
                        self.HandleShuntingData(dataNode)

        return sentCount


    def HandleShuntingData(self,node):
        try:
            namespace = node.getElementsByTagName('Namespace')[0].firstChild.nodeValue
            ID = node.getElementsByTagName('ID')[0].firstChild.nodeValue
            Value = node.getElementsByTagName('Value')[0].firstChild.nodeValue
        except Exception as Ex:
            Log.getLogger().error("Can't read Namespace and ID in HandleShuntingData ")
            return
        
        self._ShuntWorker(namespace,ID,Value)
             
            
    def _ShuntWorker(self,Namespace,ID,Value):
        from Helpers import Configuration
        config = Configuration.get()

        Namespace = config.HandleBITWNamespace(Namespace)
        lowerNS = Namespace.lower()

        lowerID = ID.lower()
        key = lowerNS + lowerID

        try:
            if key in config.GetNotShuntedMap():
                return # already know this one isn't shunted

            elif key in config.GetResolvedShuntMap():
                self.Shunt(Namespace,ID,config.GetResolvedShuntMap()[key],Value)
                return

            elif lowerNS in config.GetShuntMap():
                nsMap = config.GetShuntMap()[lowerNS]
                if id in nsMap: # matches a shunt, but wasn't in resolved
                    dataTuple = nsMap[id]
                    config.GetResolvedShuntMap()[key] = dataTuple
                    self.Shunt(Namespace,ID,dataTuple,Value)
                    return

                for idKey, dataTuple in nsMap.items():
                    idPattern = dataTuple[3]
                    matched = idPattern.match(lowerID)
                    if None != matched:
                        #Found a match!
                        dataTuple = nsMap[idKey]
                        config.GetResolvedShuntMap()[key] = dataTuple
                        self.Shunt(Namespace,ID,dataTuple,Value)
                        return


            else:
                for nsKey in config.GetShuntMap().keys():
                    nsPattern = re.compile(nsKey) 
                    matched = nsPattern.match(lowerNS)          
                    if None != matched:  # Matched on RegEx Namespace, now check for ID
                        nsMap = config.GetShuntMap()[nsKey]                    
                        if id in nsMap: # ID isn't a RegEx
                            dataTuple = nsMap[id]
                            config.GetResolvedShuntMap()[key] = dataTuple
                            self.Shunt(Namespace,ID,dataTuple,Value)
                            return

                        # see if ID is a regEx match
                        for idKey, dataTuple in nsMap.items():
                            idPattern = dataTuple[3]
                            matched = idPattern.match(lowerID)
                        
                            if None != matched:
                                #Found a match!
                                dataTuple = nsMap[idKey]
                                config.GetResolvedShuntMap()[key] = dataTuple
                                self.Shunt(Namespace,ID,dataTuple,Value)
                                return
                                
                config.GetNotShuntedMap()[key] = key # didn't match any filters, so add to a map so we don't do all the checking
                                                     # again


        except Exception as Ex:
             Log.getLogger().error("Unknown error in _ShuntWorker: " + str(Ex))


    # used to send Watchdog packets upstream to Oscars, and passing along Oscar
    # Tasks
    def BroadcastUpstreamToType(self,sendBuffer,targetType):
        sent = False
        for targetKey in self._UpstreamTargets.keys():
            objTarget = self.GetUpstreamTarget(targetKey)
            if objTarget.getType() == targetType:
                self.SendToUpstreamTarget(sendBuffer,targetKey)
                sent = True

        if True == sent:
           Statistics.GetStatistics().OnPacketBroadcastUpstream()

        return sent

    def UpdateDownstreamTarget(self,ConfigAddr,NewAddr):
        Log.getLogger().info("Received Foghorn message.  Changing")
        for target in self.GetDownstreamTargets():
            if target.ConfigurationDefinedTarget.lower() == ConfigAddr.lower():
                target.m_IP_InUse = newAddr

                #what to keep DNS resolution from re-starting....


    def CheckForRemovalOfDynamicMarvins(self):
        for TargetKey in self._DownstreamTargets.keys():
            objTarget = self.GetDownstreamTarget(TargetKey)
            if True == objTarget.MarkedForRemoval and objTarget.Type == ConnectionPoint.ConnectionType.DynamicMarvin_To_Remove :
                Log.getLogger().info("Removing Dynamic Marvin connection with Key: " + objTarget._UserKey)
                objTarget.StopProcessing()
                self.RemoveDynamicDownstreamTarget(TargetKey)
                self.CheckForRemovalOfDynamicMarvins() # call myself repeatedly in case more than 1 in list
                return


    def Shunt(self,namespace,ID,dataTuple,Value):

        Statistics.GetStatistics().OnPacketShunted()
        newTuple = (namespace,ID,dataTuple[4],Value)
        shuntFile = dataTuple[2]

        self.__ShuntLock.acquire()

        try:
            if not shuntFile in self.__ShuntedDataByFile:
                self.__ShuntedDataByFile[shuntFile] = []

            self.__ShuntedDataByFile[shuntFile].append(newTuple)

        except Exception as Ex:
            Log.getLogger().info("Unknown in Shunt function:  " + str(Ex))
        
        finally:
            self.__ShuntLock.release()

        if not self.__ShuntThreadCreated:
            self.__ShuntThreadCreated = True
            threadName = "ShuntProc:" + str(self)
            ThreadManager.GetThreadManager().CreateThread(threadName,self.ShuntWorkerProc)
            ThreadManager.GetThreadManager().StartThread(threadName)


    def ShuntWorkerProc(self,fnKillSignalled,userData):
        sleepTime = Configuration.get().GetShuntWorkerInterval()
        try:
            while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
                self.__ShuntLock.acquire()
                DupMap = None
                try:
                    DupMap = self.__ShuntedDataByFile
                    self.__ShuntedDataByFile = {}
                except Exception as Ex:
                    Log.getLogger().info("Unknown error in Shunt Worker Proc:  " + str(Ex))
        
                finally:
                    self.__ShuntLock.release()

                if None != DupMap:
                    for file in DupMap.keys():
                        tfh, tfh_path = mkstemp() # create temp file, copy target file to it then do all updates and copy it
                                                  # back to original
                        close(tfh)

                        if os.path.exists(file):
                            copy(file,tfh_path) #copy contents to temp file

                        mapEntry = DupMap[file]
                        for shuntEntry in mapEntry:
                            namespace = shuntEntry[0]
                            ID = shuntEntry[1]
                            History = shuntEntry[2]
                            Value = shuntEntry[3]
                            if True == History:
                                self.ShuntHistory(namespace,ID,Value,tfh_path)
                            else:
                                self.ShuntDynamicCollectorStyle(namespace,ID,Value,tfh_path)

                        #all done processing this file, so copy temp file to
                        #real file
                        #Remove original file
                        try:
                            remove(file)
                        except Exception as Ex:
                            pass
                        #Move new file
                        move(tfh_path, file)

                        
            Sleep.SleepMs(sleepTime)

        except Exception as Ex:
            Log.getLogger().info("Unknown error in Shunt Worker Proc:  " + str(Ex))      


    def ShuntDynamicCollectorStyle(self,namespace,ID,Value,shuntFile):

        fpShuntFile, absolute_path = mkstemp()
        
        searchStr = namespace + "." + ID + "="
        Entry = searchStr + Value + "\n"
        Found = False
        with open(absolute_path,'w') as fpTargetFile:
            try:
                with open(shuntFile) as fpInputFile:
                    for line in fpInputFile:
                        if not Found and searchStr in line:
                            fpTargetFile.write(Entry)
                            Found = True
                        else:
                            fpTargetFile.write(line)

                    if not Found:
                        fpTargetFile.write(Entry)

            except Exception as Ex: # file went away while running
                    fpTargetFile.write("##### Generated by Oscar Shunt #####\n")
                    fpTargetFile.write(Entry)


        close(fpShuntFile)
        #Remove original file
        try:
            remove(shuntFile)
        except Exception as Ex:
            pass
        #Move new file
        move(absolute_path, shuntFile)

    def DebugRefresh(self):
        for targ in self._UpstreamTargets:
            self._UpstreamTargets[targ].m_InitialRefreshSent = False
            self._UpstreamTargets[targ].StrokeWatchdogTimer()
            return #only need it for the 1st one

    def ShuntHistory(self,namespace,ID,value,fileName):
        strData = time.strftime("%c") + " " + namespace + "." + ID + "=" + value + "\n"
        with open(fileName,"a") as sf:
            sf.write(strData)

def GetTargetManager():
    if TargetManager._instance == None:
        TargetManager._instance = TargetManager()
    return TargetManager._instance
