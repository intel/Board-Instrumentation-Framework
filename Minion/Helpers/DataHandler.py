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
#    Data Hander - deals with all incoming data
#
##############################################################################

import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Helpers import OscarDataHandler
from Helpers import MarvinDataHandler
import sys

class DataHandler(object):
    _instance = None
    def __init__(self):
        if DataHandler._instance == None: # singleton pattern
            DataHandler._instance = self
            self.__initialize()

    def __initialize(self):
        self._OscarDataHandler = OscarDataHandler.GetDataHandler()
        self._MarvinDataHandler = MarvinDataHandler.MarvinDataHandler()

    def HandleLiveData(self,rawData,fromAddr,objNamespace):
        try:
            dom = xml.dom.minidom.parseString(rawData)
            node = dom._get_firstChild()
        except Exception as ex:
           Log.getLogger().error("Error Something bad in DecodeIncomingData - " + str(rawData))
           Log.getLogger().error(str(ex))
           return

        if node.nodeName == "Oscar":
            self._OscarDataHandler.HandleIncomingPacket(node,rawData,fromAddr,objNamespace)

        elif node.nodeName == "Marvin":
            self._MarvinDataHandler.HandleIncomingPacket(node,rawData,fromAddr,objNamespace)

        else: # no idea what this sucker is
            Log.getLogger().warning("Received unknown Packet Type: " + node.nodeName)

def GetDataHandler():
    if DataHandler._instance == None:
        return DataHandler()
    return DataHandler._instance

