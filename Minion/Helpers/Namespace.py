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
#       Deals with all the namespace stuff, inclding all the collectors
##############################################################################

import socket
import sys
import time
from Util import Time
from Util import Sleep
from Helpers import ThreadManager
from Helpers import Log
from Helpers import ServerUDP
from Helpers import VersionMgr
import itertools
import threading

__ActiveProcessThread=0

def GetActiveProcessingThreadCount():
    global __ActiveProcessThread
    return __ActiveProcessThread
    
def AddActiveProcessingThreads(howMany):
    global __ActiveProcessThread
    __ActiveProcessThread += howMany

class Namespace():
    ConnectionInfoUpdateInterval = 30000 # 30 seconds
    ConnectionUpdateThreadSleepinterval = 300 # how long to sleep each loop
    SleepIntervalIfNoDataCollected = 50 # If no data was collected in a loop, then take a little snooze

    _LogTimePerProcessLoop = False   # logs how many ms it takes to do each loop of collections
    _LoopTimePeriodWarningThreshold = 2500
    _UseExperimentalPauseForFlush = False   
    
    ##### These 2 are deprecated and need to be remove and cleaned up 
    _UseSingleCollectorThreadPerNamespace = False  
    _UseMultiThreadPerNamespace = True
    _AlternateNamespacesUsedByCollectors={}

    def __init__(self,ID,TargetIP,TargetPort,DefaultInterval):
        self.__TargetIP = TargetIP
        self.__TargetPort = int(TargetPort)
        self._DefaultInterval = int(DefaultInterval)
        self.__ListenPort = 0
        self.__ListenIP = "0.0.0.0"
        self._Socket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        self._Socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
        self._DefaultPrecision = 0 # default precision for namespace, unless overridden in namespace or
                                   # collector, is set when reading configuration (even if not specified)
        self._CheckMTU = True
        self._MTUWarningThreshold = 1500 - 20 - 8 # IPV4 + UDP Header
        self._LastFreshUniqueID = 0
        
        #self.__SEND_BUFFER_SIZE =
        #self._Socket.getsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF)

        self._ID = ID
        self._Collectors = []
        self._CollectorMap = {} # TODO, added a map because DynamicCollecters were super slow > 1000.  Get rid
                                # of the_Collectors and make all use Map
        self.__Actors = []
        self.__PacketNumber = 1
        self.__objPacketNumberLock = threading.Lock()
        self._Server = None
        self._SentSizeLock = threading.Lock()
        self._SentBytes = 0
        self.__ProcessThreadGroupings = {} # a map of collector ProcessThreads
        self.__LastActorCalled="No Actors Called Yet"

        Log.getLogger().info("Namespace [" + ID + "] Target is " + TargetIP + ":" + str(TargetPort))
        
        if sys.version_info < (3, 0):
            Namespace.SendPacket = Namespace.SendPacket_Python2
        else:
            Namespace.SendPacket = Namespace.SendPacket_Python3

        try:
            self.__TargetIP = socket.gethostbyname(self.__TargetIP)
        except:
            pass # was likely a bad bad dns name, or was an IP address to start with

    def __str__(self):
        return self._ID

    def addAltNamespaceInCollector(self,altNS):
        self._AlternateNamespacesUsedByCollectors[altNS.toupper()] = altNS

    def hasAlternateNamespace(self,altNs):
        return altNs.toupper() in self._AlternateNamespacesUsedByCollectors

    def getCollectorCount(self):
        return len(self._Collectors)

    #Used for tracking - currently not used by other end, but could be later
    def getNextPacketNumber(self):
        self.__objPacketNumberLock.acquire()
        retVal = self.__PacketNumber
        self.__PacketNumber += 1
        self.__objPacketNumberLock.release()

        return retVal

    def setDefaultPrecision(self, value):
        self._DefaultPrecision = value

    def getDefaultPrecision(self):
        return self._DefaultPrecision

    def Begin(self,runOnce=False):
        #start udp server
        # start collectors
        if  False and False == Namespace._UseSingleCollectorThreadPerNamespace and not Namespace._UseMultiThreadPerNamespace:  # deprecated
            for collector in self._Collectors:
                if not collector.IsInGroup() and not collector.IsOnDemand():
                    collector.BeginCollecting(runOnce)
                    Sleep.SleepMs(5) # so not at all same time

        # this has now really been deprecated and should not be used anymore
        elif True == Namespace._UseSingleCollectorThreadPerNamespace or runOnce: #one thread to do all collecting
            ThreadManager.GetThreadManager().CreateThread(self._ID,self.__AlternateCollectionMethod,runOnce)
            ThreadManager.GetThreadManager().StartThread(self._ID)

        else: # many threads, with multiple collectors per thread
            ThreadManager.GetThreadManager().CreateThread(self._ID,self.__AlternateCollectionMethodMultiThread)
            ThreadManager.GetThreadManager().StartThread(self._ID)

        if True == runOnce:
            return len(self._Collectors)

        self._Server = ServerUDP.ServerUDP(self.__ListenIP,self.__ListenPort,self)
        self._Server.Start()

        threadName = "ConnUpdateThread:" + str(self) + ":" + str(self.__ListenPort)
        ThreadManager.GetThreadManager().CreateThread(threadName,self.__sendConnectionInfoProc)
        ThreadManager.GetThreadManager().StartThread(threadName)
        return len(self._Collectors)

    #takes a reference actor and looks for a match in this namespace
    def Enact(self,ReferenceActor):
                                                #This is a Namesapce, so
                                                #self._ID = namespace
        if ReferenceActor.Namespace.lower() == self._ID.lower() or ReferenceActor.Namespace.lower() == "broadcast":
            for objActor in self.__Actors:
                if objActor.ID.lower() == ReferenceActor.ID.lower():             
                    if objActor.LastUniqueID != ReferenceActor.LastUniqueID:  # can be sent > 1 because this is UDP traffic
                        objActor.LastUniqueID = ReferenceActor.LastUniqueID
                        
                        objActor.Enact(ReferenceActor.Parameters)
                        self.SetLastActorInfo(objActor.GetInfo())

                        break # found it, now lets get out of here
                    else:
                        #Log.getLogger().info("Ignoring Duplicate Task")
                        break

        # Don't care if you name a task and a collector the same, will execute
        # both!  :-)
        if ReferenceActor.Namespace.lower() == self._ID.lower():
            for objCollector in self._Collectors:   # TODO, use the MAP for this, much faster
                if objCollector.GetID().lower() == ReferenceActor.ID.lower():             
                    if objCollector.LastUniqueID != ReferenceActor.LastUniqueID:  # can be sent > 1 because this is UDP traffic
                        objCollector.LastUniqueID = ReferenceActor.LastUniqueID
                        objCollector.CollectOnDemand(ReferenceActor.Parameters)
                        return # found it, now lets get out of here
                    else:
                        #Log.getLogger().info("Ignoring Duplicate On-Demand")
                        break

                
    def GetID(self):
        return self._ID

    def AddActor(self,objActor):
        self.__Actors.append(objActor)

    def AddCollector(self,objCollector,beforeID=None):
        if objCollector.GetID().lower() in self._CollectorMap:
            Log.getLogger().error("Duplicate Collector found: " + objCollector.GetID())
            return False

        #Collectors are in a MAP for fast retrieval
        self._CollectorMap[objCollector.GetID().lower()] = objCollector
        # Dynamic Collectors should be inserted right AFTER the
        # DynamicCollector collector, otherwise if appended to the end,
        # operators that use data from
        # a dynamic collector will be run using stale data
        if None == beforeID: 
            self._Collectors.append(objCollector) 

        else:
            InsertAfterInList(self._Collectors,beforeID, objCollector)
            
            if objCollector.GetProcessThreadID() in self.__ProcessThreadGroupings.keys():
                InsertAfterInList(self.__ProcessThreadGroupings[objCollector.GetProcessThreadID()],beforeID, objCollector)
            else:
                Log.getLogger().error("Not supposed to end up here!")
        
        return True

    def GetCollector(self,CollectorID):
        if CollectorID.lower() in self._CollectorMap:
            return self._CollectorMap[CollectorID.lower()]

        return None

    #tell's ALL collectors to send data, regardless of time period
    # or 'only' send on update.  This is done when a Marvin 1st comes online
    def Refresh(self,UniqueID):
        if UniqueID == self._LastFreshUniqueID: # avoid dups, from UDP packets
            return

        self._LastFreshUniqueID = UniqueID

        Log.getLogger().debug("Namespace[" + str(self) + "] performing collector refresh")
        for collector in self._Collectors:
            collector.RequestRefresh()

    def SendPacket_Python3(self,buffer):
        try:
            self._Socket.sendto(bytes(buffer,'utf-8'),(self.__TargetIP,self.__TargetPort))
        except Exception as ex:
            Log.getLogger().warning("Error sending data :" + str(ex))
            return False
        
        return True

    def SendPacket_Python2(self,buffer):
        try:
            self._Socket.sendto(str(buffer),(self.__TargetIP,self.__TargetPort))
        except Exception as ex:
            Log.getLogger().warning("Error sending data :" + str(ex))
            return False
        
        return True

    ## Main workes, does the collections
    def __AlternateCollectionMethod(self,fnKillSignalled,runOnce):
        while not fnKillSignalled():
            for collector in self._Collectors:
                if fnKillSignalled(): # get out of possible long loop if we are to exit
                    return

                if not collector.IsInGroup() and not collector.IsOnDemand():
                    SizeOfSentData = collector.alternateCollectionProc()

            if count == 0:  # no data processed, sleep a bit
                Sleep.SleepMs(100)
                
            if runOnce:
                return             

    def GetSentBytes(self):
        self._SentSizeLock.acquire()
        retData = 0
        try:
            retData = self._SentBytes
        finally:
            self._SentSizeLock.release()
        return retData

    def IncrementSentBytes(self,addVal):
        self._SentSizeLock.acquire()
        retData = 0
        try:
            self._SentBytes += int(addVal)
            retData = self._SentBytes
        finally:
            self._SentSizeLock.release()
        return retData

    def ClearSentBytes(self):
        self._SentSizeLock.acquire()
        try:
            self._SentBytes = 0 
        finally:
            self._SentSizeLock.release()

    def __CollectSingleRange(self,fnKillSignalled,processThreadID):
        from Helpers import Configuration
        count = 0
        currTotal = 0
        
        maxTx = Configuration.GetMaxTransmitBufferBeforeRest()
        startTime = Time.GetCurrMS()
        collectorList = self.__GetCollectorListForThreadGroup(processThreadID)
        
        for collector in collectorList:
            if fnKillSignalled(): # get out of possible long loop if we are to exit
                return
            if not collector.IsInGroup() and not collector.IsOnDemand():
                SizeOfSentData = collector.alternateCollectionProc()
                if SizeOfSentData > 0:
                    self.IncrementSentBytes(SizeOfSentData)
                    count+=1

                currTotal += SizeOfSentData

            if currTotal > maxTx:  # don't want to overload Oscar
                Sleep.SleepMs(50)                         
                currTotal = 0
                

        #timeTaken = Time.GetCurrMS() - startTime
        #print(processThreadID +": " + str(timeTaken))
        #if Namespace._LogTimePerProcessLoop and timeTaken > 0:
        #    Log.getLogger().debug("Process Thread: " + collectorList[0].GetProcessThreadID() + " took " + str(timeTaken) + "ms to process one loop")

        #if timeTaken > Namespace._LoopTimePeriodWarningThreshold and not Namespace._LogTimePerProcessLoop :
        #    Log.getLogger().warning("Process Thread: " + collectorList[0].GetProcessThreadID() + " took " + str(timeTaken) + "ms to process one loop - you may want to investigate.")

        #if "Default" != processThreadID:
        #    print(processThreadID + " Collected " + str(count) +"/" + str(len(collectorList)))
                                  
        return count
                
    def __SlicedThreadProc(self,fnKillSignalled,processThreadID):
        while not fnKillSignalled():
            count = self.__CollectSingleRange(fnKillSignalled,processThreadID)
            
            if 0 == count:
               Sleep.SleepMs(Namespace.SleepIntervalIfNoDataCollected)  
    
    def __CreateInitialCollectorThreadGroupings(self):
        GroupingCount = 0
        newGroup = {}
        for objCollector in self._Collectors:
            processThreadID = objCollector.GetProcessThreadID()
            if not processThreadID in newGroup.keys():
                newGroup[processThreadID] = [] # initialize the thread group
                GroupingCount += 1
                if 'Default' != processThreadID:
                    Log.getLogger().debug("Creating ProcessThread: " + processThreadID)

            newGroup[processThreadID].append(objCollector) #insert the collector into the list that is in a map
        self.__ProcessThreadGroupings= newGroup
        
        return GroupingCount

    def __GetCollectorListForThreadGroup(self,processThreadID):
        if processThreadID in self.__ProcessThreadGroupings:
            return self.__ProcessThreadGroupings[processThreadID]
        
        Log.getLogger().error("Asked to get Collector list for thread group that does not exist: " + processThreadID)
        return None
           
    def __AlternateCollectionMethodMultiThread(self,fnKillSignalled,startIndex):
        processedWithoutRestdummymakelooklikeother = 0
        ThreadCount = -1
        ProcessThreadCount = self.__CreateInitialCollectorThreadGroupings()
        AddActiveProcessingThreads(ProcessThreadCount)
        firstGroupID = None
        firstGroupCollectors = []
        collectorCount = len(self._Collectors)

        if collectorCount < 1:
            Log.getLogger().error("No Collectors to process")
            return

        for processThreadID,collectorList in self.__ProcessThreadGroupings.items():
            if None == firstGroupID:
                firstGroupID = processThreadID
                firstGroupCollectors = collectorList

            else:
                ID = str(self) + processThreadID
                ThreadManager.GetThreadManager().CreateThread(ID,self.__SlicedThreadProc,processThreadID) # create a worker thread and pass it a list of collectors to update
                ThreadManager.GetThreadManager().StartThread(ID)
                ThreadCount += 1

        while not fnKillSignalled():  # now go process the 1st group in this thread
            processed = self.__CollectSingleRange(fnKillSignalled,firstGroupID)
            if processed == 0:
               Sleep.SleepMs(Namespace.SleepIntervalIfNoDataCollected)
               if collectorCount != len(self._Collectors): # dynamic collectos must have added some
                   pass
                   #self.__CreateInitialCollectorThreadGroupings() # regenerate


    def __sendConnectionInfoProc(self,fnKillSignalled,userData):
        buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        buffer = buffer + "<Minion Type=\"ConnectionInformation\">"
        buffer = buffer + "<Version>1.0</Version>"
        buffer = buffer + "<MinionVersion>" + VersionMgr.ReadVer()+ "</MinionVersion>"
        buffer += "<Namespace>" + str(self) + "</Namespace>"
        buffer += "<Port>" + str(self._Server.getPort()) + "</Port>"
        buffer = buffer + "</Minion>"

        lastUpdate = 0
        while not fnKillSignalled():
            if lastUpdate + Namespace.ConnectionInfoUpdateInterval < Time.GetCurrMS():
                if self.SendPacket(buffer):
                    Log.getLogger().debug("Sent announcement to Oscar")

                lastUpdate = Time.GetCurrMS()
        
            Sleep.SleepMs(Namespace.ConnectionUpdateThreadSleepinterval) # Don't want to sleep for Namespace.ConnectionInfoUpdateInterval in case
                                                                         # fnKillSignalled() during tha time, so use smaller interval

    def CheckMTU(self, len, MinionID):
        if self._CheckMTU:
            if len >= self._MTUWarningThreshold:
                pass
                # I need to get back to this and see if this is accurate or now
                #Log.getLogger().warning("Minion [" + MinionID + "] is sending a packet of length " + str(len) + " which may be too large for Network.")

    def GetLastActorCalled(self):
        return self.__LastActorCalled

    def SetLastActorInfo(self,strInfo):
        self.__LastActorCalled = strInfo

def InsertAfterInList(collectorList,strAfter,objToInsert):
    index = 1

    for node in collectorList:
        if node.GetID().lower() == strAfter.lower():
            if len(collectorList) == index: 
                collectorList.append(objToInsert) 
            else:
                collectorList.insert(index,objToInsert) 

            return True

        index += 1
    return False # shouldn't reach here unless beforeID wasn't found


def GetNamespace(strNamespaceID):
    from Helpers import Configuration
    return Configuration.GetNamespace(strNamespaceID)