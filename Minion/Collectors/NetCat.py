##############################################################################
#  Copyright (c) 2017 Intel Corporation
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
#    NetCat wrapper to be used to send tasks, or by other collectors
##############################################################################
import socket

def SendAndReceive(hostname, port, whatToSend):
    try:
        port = int(port)
    except:
        return "Error: Invalid Port: " + port

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((hostname, port))
        sock.sendall(bytes(whatToSend, 'UTF-8'))
        sock.shutdown(socket.SHUT_WR)
        retData=None
        while True:
            recvData = sock.recv(4096)

            if len(recvData) == 0:
                break
            if retData == None:
               retData = recvData

            else:
                retData += recvData

        sock.close()
        return retData.decode().splitlines()

    except Exception as Ex:
        return "Error Connecting to " + hostname + '[' + str(port) +"] " + str(Ex)

def Send(hostname, port, content):
    try:
        port = int(port)
    except:
        return "Error: Invalid port: " + port

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((hostname, port))
        sock.sendall(bytes(whatToSend, 'UTF-8'))
        sock.close()

    except Exception as Ex:
        return "Error Connecting to " + hostname + '[' + str(port) +"] " + str(Ex)

#data = SendAndReceive("nd-r730-1.jf.intel.com","56789","show stat csv")
#print(data)