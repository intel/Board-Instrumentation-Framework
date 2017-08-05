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
#    Minion Data Hander - deals with all incoming data from Minion
#
##############################################################################
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Data import MarvinData
from Data import MarvinGroupData
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import TargetManager
from Helpers import Statistics
from Helpers import Target
from Helpers import GuiMgr
from Helpers import Recorder
from Helpers import Configuration
import sys

class MinionDataHandler(object):
    _instance = None
    def __init__(self):
        if MinionDataHandler._instance == None: # singleton pattern
            MinionDataHandler._instance = self
            self.__initialize()

    def __initialize(self):
        pass

    # entry point for all packets coming from a minion
    def HandleIncomingPacket(self,node,rawData,fromAddress):
        if node.nodeName == "MinionGroup":
            self.HandleIncomingGroupPacket(rawData,node,fromAddress)
            return

        Statistics.GetStatistics().OnPacketReceivedFromUpstream(rawData)
        try:
            packetType = node.attributes["Type"].nodeValue  #All should have a Type attribute
        except Exception as ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Malformed Minion Packet: " + rawData)
            return

        if packetType == "Data" :
            self.HandleIncomingMinionData(rawData,node,fromAddress)
        elif packetType == "ConnectionInformation":
            self.HandleIncomingMinionConnectionInformation(node,rawData,fromAddress)
        else :
            Statistics.GetStatistics().OnMalformedPacketReceived("Received unknown Minion Packet Type: " + rawData)

    def CreateMarvinPacket(self,rawData,node,fromAddress):
        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue # for future use, not there now
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Minion Data Packet without a Version tag: "+ rawData)
            return  None

        try:
            namespace = node.getElementsByTagName('Namespace')[0].firstChild.nodeValue

        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Minion Packet without a namespace: "+ rawData)
            return  None

        try:
            ID = node.getElementsByTagName('ID')[0].firstChild.nodeValue
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Minion Packet without an ID: "+ rawData)
            return  None
        try:
            value = node.getElementsByTagName('Value')[0].firstChild.nodeValue
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Minion Packet without any data :" + rawData)
            return  None

        try:
            eTime = node.getElementsByTagName('ElapsedTime')[0].firstChild.nodeValue
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Minion Packet without elapsed time tag :" + rawData)
            return None

        objData = MarvinData.MarvinData(namespace,ID,value,eTime,1.0)

        return objData

    # Handles incoming data packet from minion
    def HandleIncomingMinionData(self,rawData,node,fromAddress):
        # <?xml version="1.0" encoding="utf-8"?>
        # <Minion>
        #    <Version>1</Version>
        #    <PacketNumber>44</PacketNumber>
        #    <Namespace>Namespace_Foo</Namespace>
        #    <Name>CPU_UTIL_CORE0</Name>
        #    <Value>33.2</Value>
        #    <Normalized>False</Normalized>
        #    <ElapsedTime>253</ElapsedTime>
        # </Minion>
        objData = self.CreateMarvinPacket(rawData,node,fromAddress)
        if None != objData:
            TargetManager.GetTargetManager().BroadcastDownstream(objData.ToXML(),False,node)
            GuiMgr.OnDataPacketSentDownstream(objData,"Minion")
            Recorder.get().AddData(objData)

    # Handles incoming data packet from minion
    def HandleIncomingGroupPacket(self,rawData,node,fromAddress):
        # <?xml version="1.0" encoding="utf-8"?>
        # <MinionGroup>
        #   <Minion>
        #      <Version>1</Version>
        #      <PacketNumber>44</PacketNumber>
        #      <Namespace>Namespace_Foo</Namespace>
        #      <Name>CPU_UTIL_CORE0</Name>
        #      <Value>33.2</Value>
        #      <Normalized>False</Normalized>
        #      <ElapsedTime>253</ElapsedTime>
        #   </Minion>
        #   <Minion>
        #      <Version>1</Version>
        #      <PacketNumber>45</PacketNumber>
        #      <Namespace>Namespace_Foo</Namespace>
        #      <Name>CPU_UTIL_CORE1</Name>
        #      <Value>3.2</Value>
        #      <Normalized>False</Normalized>
        #      <ElapsedTime>273</ElapsedTime>
        #   </Minion>
        # </MinionGroup>

        objGroupPacket = MarvinGroupData.MarvinDataGroup("","","",0,"1.0",True)
        for packet in node.getElementsByTagName('Minion'):
            objMarvinPacket = self.CreateMarvinPacket(rawData,packet,fromAddress)
            if None == objMarvinPacket:
                return
            objGroupPacket.AddPacket(objMarvinPacket)

        GuiMgr.OnDataPacketSentDownstream(objGroupPacket,"Minion")

        TargetManager.GetTargetManager().BroadcastDownstream(objGroupPacket.ToXML(),False,None,True)
        Recorder.get().AddData(objGroupPacket)

        
    # handles the connection information updates from a minion
    def HandleIncomingMinionConnectionInformation(self,node,rawData,fromAddr):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Minion Type="ConnectionInformation">
        #    <Version>1.0</Version>"
        #    <MinionVersion>17.02.12 Build 3</MinionVersion>"
        #    <Namespace>NamespaceFoo</Namespace>
        #    <Port>12345</Port>
        #</Minion>

        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Minion Connection Information Packet : " + rawData)
            return

        try:
            namespace = node.getElementsByTagName('Namespace')[0].firstChild.nodeValue 
            namespace = namespace.lower()
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Minion ConnectionInformation Packet : " + rawData)
            return

        try:
           port = node.getElementsByTagName('Port')[0].firstChild.nodeValue 
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Minion ConnectionInformation Packet : " + rawData)
            return

        IP =  fromAddr[0].lower()
        TargetID = namespace + ":" + IP + ":" + port 
                                              
        CP = TargetManager.GetTargetManager().GetUpstreamTarget(TargetID)
        if None == CP:
            objUpstreamTarget = Target.Target(IP,port,ConnectionPoint.ConnectionType.Minion,False)
            TargetManager.GetTargetManager().AddUpstreamTarget(objUpstreamTarget,TargetID) # add it as a upstream target for tasks and such
            try:
                minionVersion = node.getElementsByTagName('MinionVersion')[0].firstChild.nodeValue 
                Log.getLogger().info("Received Connection from Minion " + TargetID  + " version: " + minionVersion)
            except Exception as Ex:
                pass

        elif CP.Type != ConnectionPoint.ConnectionType.Minion:
            Statistics.GetStatistics().OnMalformedPacketReceived("Unexpected Connection Type: " + str(CP.Type))


def GetDataHandler():
    if MinionDataHandler._instance == None:
        return MinionDataHandler()
    return MinionDataHandler._instance


