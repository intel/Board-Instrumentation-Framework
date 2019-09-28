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
#    Wrapper class for a piece of data, could be from file or from network
#
##############################################################################
from Data import MarvinData


class MarvinDataGroup(MarvinData.MarvinData):
    def __init__(self,Namespace,ID,Value,ElapsedTime,FormatVersion,isLive=True): 
        MarvinData.MarvinData.__init__(self,Namespace,ID,Value,ElapsedTime,FormatVersion,isLive)
        self._DataList = []

    def AddPacket(self,packet):
        self._DataList.append(packet)

    def ToXML(self,destIsFile=False): 
        if False == destIsFile:
            buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        else:
            buffer = ""
        buffer += "<OscarGroup>"
        for packet in self._DataList:
            buffer += packet.ToXML(True)

        buffer += "</OscarGroup>"

        return buffer