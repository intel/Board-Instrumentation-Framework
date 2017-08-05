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
#       Entry point for processing incoming data
#
##############################################################################
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from threading import Lock
from Helpers import  Log
from Data import MarvinData
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import TargetManager
from Helpers import Statistics
from Helpers import Target
from Helpers import OscarDataHandler
from Helpers import MinionDataHandler
from Helpers import MarvinDataHandler
from Helpers import ThreadManager
from Util import Sleep
import threading
import sys

class DataHandler(object):
    _instance = None
    def __init__(self):
        if DataHandler._instance == None: # singleton pattern
            DataHandler._instance = self
            self.__initialize()
            

    def __initialize(self):
        self._OscarDataHandler = OscarDataHandler.GetDataHandler()
        self._MinionDataHandler = MinionDataHandler.GetDataHandler()
        self._MarvinDataHandler = MarvinDataHandler.MarvinDataHandler()
        self.__MinionRecvQueueLock = Lock()
        self.__MinionRecvQueue = []
        self.__WorkerThreadCount = 0
        self.__WorkerThreadCountLock = Lock()

    def _GetWorkerThreadCount(self):
        self.__WorkerThreadCountLock.acquire()
        retVal = self.__WorkerThreadCount
        self.__WorkerThreadCountLock.release()
        return retVal

    def _IncrementWorkerThreadCount(self):
        self.__WorkerThreadCountLock.acquire()
        self.__WorkerThreadCount += 1
        retVal = self.__WorkerThreadCount
        self.__WorkerThreadCountLock.release()

        return retVal

    def _DecrementWorkerThreadCount(self):
        self.__WorkerThreadCountLock.acquire()
        self.__WorkerThreadCount -= 1
        retVal = self.__WorkerThreadCount
        self.__WorkerThreadCountLock.release()

        return retVal

    def AddToSynchQueue(self,item):
        self.__MinionRecvQueueLock.acquire()
        self.__MinionRecvQueue.append(item)
        retLen = len(self.__MinionRecvQueue)
        self.__MinionRecvQueueLock.release()
        return retLen

    def GetItemFromSynchQueue(self):
        self.__MinionRecvQueueLock.acquire()
        try:
            retVal = self.__MinionRecvQueue.pop(0)
        except:
            retVal = None
        self.__MinionRecvQueueLock.release()
        return retVal


    def HandleLiveData(self,rawData,fromAddr):
        length = self.AddToSynchQueue((rawData,fromAddr))
        threadCount = self._GetWorkerThreadCount()
        if  threadCount < 1 or  length / threadCount > 25: # if > 25 items per thread, spawn another
            newThread = threading.Thread(target=self.__SimpleWorker)
            newThread.start()
            self._IncrementWorkerThreadCount()
            #Log.getLogger().debug("Adding worker thread #" + str(self._GetWorkerThreadCount()) + " to process Minion data. " + str(length) + " datapoints queued up")

    def __SimpleWorker(self):
        while not ThreadManager.GetThreadManager().AllStopSignalled():
            dataBlock = self.GetItemFromSynchQueue() # get data to process

            if None != dataBlock:
                rawData,FromAddr = dataBlock
                self.__HandleLiveData(rawData,FromAddr) # go process teh data
            else: # no data to process, maybe reduce woker count
                if self._GetWorkerThreadCount() > 2:
                    self._DecrementWorkerThreadCount()
                    #Log.getLogger().debug("Reducing worker threads")
                    return
                else:
                    Sleep.SleepMs(10) 

    def __HandleLiveData(self,rawData,fromAddr):
        try:
            dom = xml.dom.minidom.parseString(rawData)
            node = dom._get_firstChild()
        except Exception as ex:
           Log.getLogger().error("Error Something bad in DecodeIncomingData - " + str(rawData))
           Log.getLogger().error(str(ex))
           return

        if node.nodeName == "Minion":
            self._MinionDataHandler.HandleIncomingPacket(node,rawData,fromAddr)

        elif node.nodeName == "MinionGroup":
            self._MinionDataHandler.HandleIncomingPacket(node,rawData,fromAddr)

        elif node.nodeName == "Oscar":
            self._OscarDataHandler.HandleIncomingPacket(node,rawData,fromAddr)

        elif node.nodeName == "OscarGroup":
            self._OscarDataHandler.HandleIncomingPacket(node,rawData,fromAddr)

        elif node.nodeName == "Marvin":
            self._MarvinDataHandler.HandleIncomingPacket(node,rawData,fromAddr)

        elif node.nodeName == "Foghorn" : # is a forhorn packet
            Log.getLogger().info(node.nodeName)
            self.HandleFoghornPacket(node,rawData,fromAddr)
            
        else: # no idea what this sucker is
            Log.getLogger().warning("Received unknown Packet Type: " + node.nodeName)


def HandleForhornPacket(self,node,rawData,fromAddr):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Foghorn>
        #    <Version>1.0</Version>
        #    <ConfigAddress>Kutch.system.foo.com</ConfigAddress>
        #    <NewIPAddress>129.234.1.22</NewIPAddress>
        #    <Port>Port</Port>
        #</Foghorn>
        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            ConfigAddr = node.getElementsByTagName('ConfigAddress')[0].firstChild.nodeValue
            NewAddr= node.getElementsByTagName('NewIPAddress')[0].firstChild.nodeValue

        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Oscar Connection Information Packet : " + rawData)
            return

        TargetManager.GetTargetManager().UpdateDownstreamTarget(ConfigAddr,NewAddr)

def GetDataHandler():
    if DataHandler._instance == None:
        return DataHandler()
    return DataHandler._instance

