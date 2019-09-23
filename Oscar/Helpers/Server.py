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
from Helpers import TargetManager
from Helpers import DataHandler
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from threading import Lock
import sys
import os

TCP_PACKET_DELIMITER_START=chr(2)
TCP_PACKET_DELIMITER_END=chr(3)

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
            self.m_socket.settimeout(0.5)
        except Exception as ex:
            Log.getLogger().error("Invalid Socket IP or Port " + str(self) +" - " + str(ex))
            return False

        if 0 == self.getPort(): # let the OS choose the port
             self.Port = self.m_socket.getsockname()[1] #can use this to pass to Marvin

        self.m_ConnPoint.Port = self.Port # ungly kludge
        self.m_ConnPoint.IP = self.IP # ungly kludge

        Log.getLogger().info(self.getTypeStr() +" listening on -->" + str(self))
        
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


class ServerTCP(ServerUDP):
    def __init__(self,ConnPoint,ConnType,downstreamServer,upstreamServer):
        super().__init__(ConnPoint,ConnType)     
        self._downstreamServer = downstreamServer
        self._upstreamServer = downstreamServer
        self.m_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        self.m_socket.settimeout(.5)

        # kludgy stuff, to make this object look like a Target object
        self.MarkedForRemoval = False
        self.m_PacketsSent = 0
        self.m_BytestSent = 0
        self.m_hasTimedOut = False

        self._secondaryInit()

    def getResolvedIP(self):
        return self.getIP()

    
    def _secondaryInit(self):
        self.__clients=[]   
        # Get messages to send towards Marvin
        self.m_Name = "ProxyTCP Server:"+str(self)
        TargetManager.GetTargetManager().AddDownstreamTarget(self,self.m_Name)


    def Start(self):
        try:
            self.m_socket.bind((self.getIP(),self.getPort()))
            #self.m_socket.setblocking(True)
        except Exception as ex:
            Log.getLogger().error("Invalid Socket IP or Port " + str(self) +" - " + str(ex))
            return False
# this should not be OK for TCP, remove
        if 0 == self.getPort(): # let the OS choose the port
             self.Port = self.m_socket.getsockname()[1] #can use this to pass to Marvin

        self.m_ConnPoint.Port = self.Port # ungly kludge
        self.m_ConnPoint.IP = self.IP # ungly kludge

        Log.getLogger().info(self.getTypeStr() +" listening on -->" + str(self))
        
        ThreadManager.GetThreadManager().CreateThread(self.m_Name,self.acceptThread)
        ThreadManager.GetThreadManager().StartThread(self.m_Name)

        return True

    def _CheckForTimedOutConnection(self):
        
        if len(self.__clients) > 0:
            removeList=[]
            for clientSocket in self.__clients:
                if True == clientSocket._closed:
                    removeList.append(clientSocket)

            for clientSocket in removeList:
                self.__clients.remove(clientSocket)

    def Send(self,sendPacket,ignoreTimeout=False):        
        self._CheckForTimedOutConnection() # need only do this when somebody is writing

        if len(self.__clients) < 1:
            return

        delimeteredPacket = TCP_PACKET_DELIMITER_START + sendPacket + TCP_PACKET_DELIMITER_END
        delimeteredPacket = bytes(delimeteredPacket,'utf-8')
        for clientSocket in self.__clients:
            try:
                sentSize = 0
                sendLen = len(delimeteredPacket)
                while sentSize < sendLen:
                    sentSize += clientSocket.send(delimeteredPacket[sentSize:])
            except:
                continue # go onto next one

        return True
                        

    def acceptThread(self,fnKillSignalled,userData):
        self.m_socket.listen(5)
        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            try:
                clientSock, clientAddr = self.m_socket.accept()
                clientThreadName = self.m_Name + str(clientAddr)
                ThreadManager.GetThreadManager().CreateThread(clientThreadName,self.recvTCP_WorkerProc,(clientSock,clientAddr))
                ThreadManager.GetThreadManager().StartThread(clientThreadName)
                
                self.__clients.append(clientSock)

            except socket.timeout:
                time.sleep(.5)
                pass                    


    def recvTCP_WorkerProc(self,fnKillSignalled,userData):
        from Helpers import Configuration
        buffSize = Configuration.get().GetRecvBufferSize()
        clientSock, clientAddr = userData
        clientSock.settimeout(.1)

        currPacket=None
        try:
            while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
                try:
                    rawData = clientSock.recv(buffSize).strip().decode("utf-8")
                    if not rawData:  # other end disconnected
                        Log.getLogger().debug("Client Disconnected " + str(userData))
                        clientSock.close()
                        return

                    self.m_rxBytes += len(rawData)
                    
                    for dataByte in rawData:
                        if dataByte == TCP_PACKET_DELIMITER_START:
                            if None != currPacket:
                                #print("Received Start of Packet before an End Of Packet Delimeter")
                                currPacket = None
                            else:
                                currPacket =""

                        elif dataByte == TCP_PACKET_DELIMITER_END:
                            self.m_rxPackets +=1
                            self.__processIncomingData(currPacket,clientAddr)
                            currPacket = None

                        else:
                            currPacket += dataByte

                except:# socket.error:
                    time.sleep(.01) #no data, so sleep for 10ms
                    continue

                if not fnKillSignalled():
                    pass
                    #dataHandler.HandleLiveData(rawData,clientAddr)

        except Exception as ex:
            Log.getLogger().debug("Thread Error: " + str(ex) + " --> " + traceback.format_exc())

        def __processIncomingData(self,buffer,clientAddr):
            # for Server packets, the data is coming from direction of Marvin 
            DataHandler.GetDataHandler().HandleLiveData(buffer,clientAddr)

            pass
#            dataHandler.HandleLiveData(currPacket,clientAddr)



class ClientTCP(ServerTCP):
    def __init__(self,ConnPoint,ConnType,downstreamServer,upstreamServer):
        super().__init__(ConnPoint,ConnType,downstreamServer,upstreamServer)     

    def _secondaryInit(self):
        self.__clients=[]
        # Get messages to send towards Minion
        self.m_Name = "ProxyTCP Client:"+str(self)

        TargetManager.GetTargetManager().AddUpstreamTarget(self,self.m_Name)

    def __processIncomingData(self,buffer,clientAddr):
        DataHandler.GetDataHandler().HandleLiveData(buffer,(self.getIP(),self.getPort()))
        pass

    def _connect(self):
        try:
            self.m_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
            self.m_socket.settimeout(.5)

            self.m_socket.connect((self.getIP(),self.getPort()))

        except Exception as _:
            self.m_socket = None
            return False

        return True

    def _CheckForTimedOutConnection(self):
        pass

    def Start(self):
        Log.getLogger().info(self.getTypeStr() +" Connecting to -->" + str(self))
        
        ThreadManager.GetThreadManager().CreateThread(self.m_Name,self.recvTCP_WorkerProc_Client,None)
        ThreadManager.GetThreadManager().StartThread(self.m_Name)

        return True


    def recvTCP_WorkerProc_Client(self,fnKillSignalled,_):
        from Helpers import Configuration
        buffSize = Configuration.get().GetRecvBufferSize()
        self._connect()
        currPacket=None
        try:
            while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
                if None == self.m_socket:
                    time.sleep(.5)    
                    self._connect()
                    continue

                try:
                    rawData = self.m_socket.recv(buffSize).strip().decode("utf-8")
                    if len(rawData) == 0:  # other end disconnected
                        time.sleep(.5)    
                        self.m_socket.close()                    
                        self._connect()
                        currPacket=None
                        continue

                    self.m_rxBytes += len(rawData)
                    
                    for dataByte in rawData:
                        if dataByte == TCP_PACKET_DELIMITER_START:
                            if None != currPacket:
#                                print("Received Start of Packet before an End Of Packet Delimeter")
                                currPacket = None
                            else:
                                currPacket =""

                        elif dataByte == TCP_PACKET_DELIMITER_END:
                            self.m_rxPackets +=1
                            self.__processIncomingData(currPacket,None)
                            currPacket = None

                        else:
                            currPacket += dataByte

                except socket.timeout:
                    pass

                except Exception as _:# socket.error:
                    #print(str(Ex))
                    pass

                    continue

                if not fnKillSignalled():
                    pass
                    #dataHandler.HandleLiveData(rawData,clientAddr)

        except Exception as ex:
            Log.getLogger().debug("Thread Error: " + str(ex) + " --> " + traceback.format_exc())        

    def Send(self,sendPacket,ignoreTimeout=False):        

        if None == self.m_socket: # lost connection
            return False

        delimeteredPacket = TCP_PACKET_DELIMITER_START + sendPacket + TCP_PACKET_DELIMITER_END
        delimeteredPacket = bytes(delimeteredPacket,'utf-8')

        try:
            sentSize = 0
            sendLen = len(delimeteredPacket)
            while sentSize < sendLen:
                sentSize += self.m_socket.send(delimeteredPacket[sentSize:])

        except Exception as _:
            pass

        return True
                        