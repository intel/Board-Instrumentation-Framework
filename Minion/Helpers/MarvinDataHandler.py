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
#    Marvin Data Hander - deals with all incoming data from Marvin
#
##############################################################################

import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Util import Utility
from Util import Sleep
from Helpers import Actor
from Helpers import Alias

class MarvinDataHandler(object):
    _instance = None
    def __init__(self):
        if MarvinDataHandler._instance == None: # singleton pattern
            MarvinDataHandler._instance = self
            self.__initialize()

    def __initialize(self):
        pass

    def HandleIncomingPacket(self,node,rawData,fromAddr,objNamespace):
        try:
            packetType = node.attributes["Type"].nodeValue  #All should have a Type attribute
            version  = node.getElementsByTagName('Version')
        except Exception as ex:
            Log.getLogger().warning("Received malformed Marvin Packet Type: " + str(rawData))
            return

        if packetType == "MinionTask":
            self.HandleMinionTask(node,rawData,objNamespace)

        else :
            Log.getLogger().error("Received unknown Packet Type: " + rawData)

    # is a remote Minion task, go perform it
    def HandleMinionTask(self,node,rawData,objNamespace):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="MinionTask">
        #    <Version>1.0</Version>
        #    <UniqueID>1233456</UniqueID>
        #     <Task Namespace="fuBar" ID="RunMyTask"/>
        #     <Param>p1</Param>
        #</Marvin>
        from Helpers import Namespace

        objTempActor = Actor.Actor()
        try:
            objTempActor.LastUniqueID = node.getElementsByTagName('UniqueID')[0].firstChild.nodeValue
            taskNode = node.getElementsByTagName('Task')[0] 
            attributes = taskNode.attributes
            objTempActor.Namespace = Alias.Alias(attributes["Namespace"].nodeValue)
            objTempActor.ID = Alias.Alias(attributes["ID"].nodeValue)

            for param in node.getElementsByTagName("Param"): # Make an array of the params for the script
                strParam = Alias.Alias(param.firstChild.nodeValue)
                objTempActor.Parameters.append(strParam)

        except Exception as ex:
           Log.getLogger().error("Error Parsing Minion Task: " + rawData + " : " + str(ex))
           return

        objNamespace.Enact(objTempActor)

def GetDataHandler():
    if MarvinDataHandler._instance == None:
        return OscarDataHandler()
    return MarvinDataHandler._instance
