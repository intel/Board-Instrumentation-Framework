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
#   UDP Client code - sends packets to Marvin and Minion    
#
##############################################################################
import socket
import threading
import time
import random
from Util import Time
from Util import Sleep
from Helpers import ThreadManager
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import Statistics

from Helpers import Log

class Target(ConnectionPoint.ConnectionPoint):
    def __init__(self,ip=None,Port=None,ConnType=ConnectionType.Unknown,canTimeout=True):
        super(Target,self).__init__(ip,Port,ConnType)
        self.ConfigurationDefinedTarget = ip
        self.m_IP_InUse = None #m_ip could be DNS name
        self.m_socket = None
        self.m_lastHeartbeat = Time.GetCurrMS()
        self.m_PacketsSent = 0
        self.m_BytestSent = 0
        self.m_InitialRefreshSent = False
        self.m_objLockDict = threading.Lock()
        self.m_SendList = []
        self.m_hasTimedOut = False
        self.m_LastDNSResolution = Time.GetCurrMS()
        self.m_DNSResolutionPeriod = 30000 #30 seconds
        self.m_CanTimeout = canTimeout
        self.threadName = None
        self.lastRefreshRequestID = 0
        self.MarkedForRemoval = False

        try:
            self.m_socket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM,socket.IPPROTO_UDP)
            self.m_socket.setblocking(True)
            self.m_socket.settimeout(0.001)

        except Exception as ex:
                Log.getLogger().error("Error setting up Target Socket -->" + str(self.m_Connection))

        self.threadName = "Target:" + self.getIP() + "[" + str(self.getPort()) +"]"
        ThreadManager.GetThreadManager().CreateThread(self.threadName,self.WorkerProc)
        ThreadManager.GetThreadManager().StartThread(self.threadName)

    def getResolvedIP(self):
        return self.m_IP_InUse

    def Send(self,buffer,ignoreTimeout=False):
        timedOut = self.__CheckForTimeout()
        if False == ignoreTimeout and True == timedOut: #some messages (like connection info to Marvin, should always go)
            return False

        self.m_objLockDict.acquire()
        self.m_SendList.append(buffer)
        self.m_objLockDict.release() 
        return True

    def __CheckForTimeout(self):
        from Helpers import Configuration

        if not self.m_CanTimeout :
            return False

        if self.m_hasTimedOut:
            return True

        toPeriod = Configuration.get().GetTimeoutPeriod()
        if toPeriod < Time.GetCurrMS() - self.m_lastHeartbeat: # a timeout has ocurred
            self.m_hasTimedOut = True
            Log.getLogger().info("Target [" + str(self) + "] has timed out.")
            self.lastRefreshRequestID = 0
            #self.ReArmRefreshRequest(123456789)

        return self.m_hasTimedOut
        

    def ResetStats(self):
        self.m_lastHeartbeat = Time.GetCurrMS()
        self.m_PacketsSent = 0
        self.m_BytestSent = 0
        self.m_hasTimedOut = False
        self.LastPacket = None

    def StrokeWatchdogTimer(self):
        if True == self.m_CanTimeout:
            self.m_lastHeartbeat = Time.GetCurrMS()
            self.m_hasTimedOut = False

        if False == self.m_InitialRefreshSent:
            self.m_InitialRefreshSent = True
            buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            buffer = buffer + "<Oscar Type=\"Refresh\">"
            buffer = buffer + "<Version>1.0</Version>"
            uID = str(random.randint(0,500000))
            buffer = buffer + "<UniqueID>" + uID + "</UniqueID>"
            buffer = buffer + "</Oscar>"
            from Helpers import TargetManager
            if TargetManager.GetTargetManager().BroadcastUpstream(buffer):
                Log.getLogger().info("Sending Refresh Request to Minions [" + uID + ']')
                TargetManager.GetTargetManager().BroadcastUpstream(buffer) # is UDP, so send a couple more times, dups will be filtered on Minion
                TargetManager.GetTargetManager().BroadcastUpstream(buffer)

    def ReArmRefreshRequest(self, UniquieID):
        if self.lastRefreshRequestID != UniquieID: 
            self.lastRefreshRequestID = UniquieID
            self.m_InitialRefreshSent = False

    
    #actual thread proc to send the data
    def WorkerProc(self,fnKillSignalled,userData):
        #sequentialSent = 0 - experiment to reduce CPU utilization, but I think it was a bad idea now
        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            if self.alternateWorker():
                pass
                #sequentialSent +=1

            else:
                Sleep.SleepMs(100) # no data to send, so rest for a while
                #sequentialSent = 0

            #if sequentialSent > 10:
                #Sleep.SleepMs(10) # sent 10 packets without a rest, take a snooze
             #   sequentialSent = 0

        # all done, so close things down
        if None != self.m_socket:
            self.m_socket.close()

    def StopProcessing(self):
        ThreadManager.GetThreadManager().StopThread(self.threadName)
        ThreadManager.GetThreadManager().RemoveThread(self.threadName)


    def alternateWorker(self):  ## Maybe have one thread that goes and
        self.m_objLockDict.acquire() # thread safety

        if len(self.m_SendList) == 0: # rest if nothing to send, or sent a bunch of packets without a sleep
            dataToProcess = False

        else:
            buffer = self.m_SendList[0]
            del self.m_SendList[0]
            dataToProcess = True

        self.m_objLockDict.release() 

        if dataToProcess:
            if None == self.m_IP_InUse:
                Log.getLogger().info("Getting IP address for host: " + self.ConfigurationDefinedTarget)
                try:
                    self.m_LastDNSResolution = Time.GetCurrMS()
                    self.m_IP_InUse = socket.gethostbyname(self.ConfigurationDefinedTarget)  #use this for looking at heartbeats

                except Exception as ex:
                    self.m_IP_InUse = self.ConfigurationDefinedTarget

            try:
                self.m_socket.sendto(bytes(buffer,'utf-8'),(self.m_IP_InUse,self.getPort()))
                self.m_PacketsSent +=1
                self.m_BytestSent += len(buffer)

            except Exception as ex:
                Log.getLogger().info("It appears that the target [" + self.ConfigurationDefinedTarget + "] has went away.")
                self.m_objLockDict.acquire()
                Statistics.GetStatistics().OnPacketDropped(len(self.m_SendList) + 1)
                self.m_SendList.clear()
                self.m_objLockDict.release()


        if self.m_LastDNSResolution + self.m_DNSResolutionPeriod < Time.GetCurrMS() and self.m_hasTimedOut == True:
            self.m_IP_InUse = None  # Force a DNS resolution, may help when move laptop and gets new address -
                                    # eventually
        if self.m_hasTimedOut and self.Type == ConnectionType.DynamicMarvin:
            self.MarkedForRemoval = True
            self.Type = ConnectionType.DynamicMarvin_To_Remove


        return dataToProcess



