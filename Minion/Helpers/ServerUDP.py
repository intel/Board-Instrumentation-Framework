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
#    This file contains the conde to handle the incoming UDP frames 
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
from threading import Lock
import sys
import os

#################################
# Listens for incoming data and gets it processed
class ServerUDP():
    def __init__(self,IP,Port,objNamespace):
        self.__IP = IP
        self.__Port=Port
        self.m_rxPackets = 0
        self.m_rxBytes = 0
        self.m_Name = None
        self.m_socket = None
        self.m_DropPackets=False
        self.m_objLock = threading.Lock()
        self._objNamespace = objNamespace


    def __str__(self):
        return self._objNamespace._ID +":"+ self.getIP()+":"+str(self.getPort())

    def getIP(self):
        return self.__IP

    def getPort(self):
        return self.__Port

    #start receiving and processing data
    def Start(self):
        if None != self.m_Name:
            return

        self.m_Name = str(self)
        self.m_socket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        try:
            self.m_socket.bind((self.getIP(),self.getPort()))
            self.m_socket.setblocking(0)
        except Exception:
            Log.getLogger().error("Invalid Socket IP or Port " + str(self))
            return False

        if 0 == self.getPort(): # let the OS choose the port
             self.__Port = self.m_socket.getsockname()[1] #can use this to pass to Marvin

        self.__Port = self.__Port # ungly kludge
        self.__IP = self.__IP # ungly kludge

        self._objNamespace.__ListenPort=self.__Port

        Log.getLogger().debug("Namespace[" + str(self._objNamespace) +"] listening on -->" + str(self))
        
        ThreadManager.GetThreadManager().CreateThread(self.m_Name,self.workerProc)
        ThreadManager.GetThreadManager().StartThread(self.m_Name)

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
        iCount = 0
        dataHandler = DataHandler.GetDataHandler()
        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            try:
                data, fromAddress = self.m_socket.recvfrom(8192)
                data = data.strip().decode("utf-8")
         #       iCount += 1
                self.m_rxPackets +=1
                self.m_rxBytes += len(data)

            except socket.error:
                time.sleep(.01) #no data, so sleep for 10ms
          #      iCount = 0
                continue

            if not fnKillSignalled() and False == self.__DropPackets():
                dataHandler.HandleLiveData(data,fromAddress,self._objNamespace)

            else:
                pass # drop it into the bit bucket

            #if iCount > 10 : # just in case getting flooded after 10 consecutive packets
            #    time.sleep(.01) #small rest
            
        #    iCount = 0

