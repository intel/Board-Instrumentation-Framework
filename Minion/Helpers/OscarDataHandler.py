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
#       Deals with data coming from an Oscar
##############################################################################

import xml.dom.minidom
from xml.parsers.expat import ExpatError
import sys
from Helpers import Log

class OscarDataHandler(object):
    _instance = None
    def __init__(self):
        if OscarDataHandler._instance == None: # singleton pattern
            OscarDataHandler._instance = self
            self.__initialize()
            self._LastFreshUniqueID = 0

    def __initialize(self):
        pass
            
    def HandleIncomingPacket(self,node,rawData,fromAddress,objNamespace):
        try:
            packetType = node.attributes["Type"].nodeValue  #All should have a Type attribute
        except Exception as ex:
            Log.getLogger().error("Malformed Oscar Packet: " + rawData)
            return

        if packetType == "Refresh" :  # is a data packet in a chained Oscar, just send it on down the road
            self.HandleRefreshPacket(node,rawData,fromAddress,objNamespace)

        else:
            Log.getLogger().error("Unknown Oscar Packet: " + rawData)


    def HandleRefreshPacket(self,node,rawData,fromAddress,objNamespace):
        #<?xml version=\"1.0\" encoding=\"utf-8\"?>
        #<Oscar Type=\"Refresh\">
        #   <Version>1.0</Version>
        #   <UniqueID>3434</UniqueID>
        #</Oscar>
        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            uniqueID = node.getElementsByTagName('UniqueID')[0].firstChild.nodeValue 

        except Exception as Ex:
            Log.getLogger().error("Malformed Oscar Refresh Packet: " + rawData)
            return
        
        objNamespace.Refresh(uniqueID)


def GetDataHandler():
    if OscarDataHandler._instance == None:
        return OscarDataHandler()
    return OscarDataHandler._instance
