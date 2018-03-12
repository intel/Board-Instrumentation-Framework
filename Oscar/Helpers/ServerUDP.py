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
#    This file contains the conde to handle the incoming UDP frames from Minion
#
##############################################################################
import socket
import threading
import traceback
import time
from Util import Time
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import Log
from Helpers import ThreadManager
from Helpers import DataHandler
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from threading import Lock
import sys
import os

#################################
# Listens for incoming data and gets it processed
class ServerUDP(ConnectionPoint.ConnectionPoint):
    #def __init__(self,ip,Port,ConnType):
    def __init__(self,ConnPoint,ConnType):
        super().__init__(ConnPoint.getIP(),ConnPoint.getPort(),ConnType)
        self.m_rxPackets = 0
        self.m_rxBytes = 0
        self.m_Name = None
        self.m_ConnPoint = ConnPoint
        self.m_socket = None
        self.m_DropPackets=False
        self.m_objLock = threading.Lock()
        
    #start receiving and processing data
    def Start(self):
        if None != self.m_Name:
            return

        self.m_Name = "ServerUDP:"+str(self)
        self.m_socket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        try:
            self.m_socket.bind((self.getIP(),self.getPort()))
            self.m_socket.setblocking(True)
            self.m_socket.settimeout(0.001)
        except Exception as ex:
            Log.getLogger().error("Invalid Socket IP or Port " + str(self) +" - " + str(ex))
            return False

        if 0 == self.getPort(): # let the OS choose the port
             self.Port = self.m_socket.getsockname()[1] #can use this to pass to Marvin

        self.m_ConnPoint.Port = self.Port # ungly kludge
        self.m_ConnPoint.IP = self.IP # ungly kludge

        Log.getLogger().info(self.getTypeStr()+" listening on -->" + str(self))
        
        ThreadManager.GetThreadManager().CreateThread(self.m_Name,self.workerProc)
        ThreadManager.GetThreadManager().StartThread(self.m_Name)

        return True

    def Stop(self):
        ThreadManager.GetThreadManager().StopThread(self.m_Name)
        
        if None != self.m_socket :
            self.m_socket.close()
            self.m_socket = None

    def DropPackets(self,flag):
        self.m_objLock.acquire()
        self.m_DropPackets = flag
        self.m_objLock.release()

    def __DropPackets(self):
        self.m_objLock.acquire()
        retVal = self.m_DropPackets
        self.m_objLock.release()
        return retVal

    # Main worker thread for receiving data from a socket.
    # data comes in and another thread is created to actually process it.
    def workerProc(self,fnKillSignalled,userData):
        dataHandler = DataHandler.GetDataHandler()
        from Helpers import Configuration
        buffSize = Configuration.get().GetRecvBufferSize()
        try:
            while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
                try:
                    data, fromAddress = self.m_socket.recvfrom(buffSize)
                    data = data.strip().decode("utf-8")
                    self.m_rxPackets +=1
                    self.m_rxBytes += len(data)

                except:# socket.error:
                    #time.sleep(.01) #no data, so sleep for 10ms
                    continue

                if not fnKillSignalled():
                    if False == self.__DropPackets():
                        dataHandler.HandleLiveData(data,fromAddress)
                    elif "<Marvin Type=\"Bullhorn\">" in data:
                        dataHandler.HandleLiveData(data,fromAddress) # drop all but the Marvin Bullhorn

        except Exception as ex:
            Log.getLogger().debug("Thread Error: " + str(ex) + " --> " + traceback.format_exc())


