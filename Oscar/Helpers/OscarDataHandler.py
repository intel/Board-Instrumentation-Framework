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
#       Entry point for processing incoming data from another Oscar
#
##############################################################################
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Helpers import GuiMgr
from Helpers import Statistics
from Helpers import Recorder
from Data import MarvinData
from Data import MarvinGroupData
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import TargetManager
from Helpers import Statistics
from Helpers import Target
from Helpers import Watchdog
import sys

class OscarDataHandler(object):
    _instance = None
    def __init__(self):
        if OscarDataHandler._instance == None: # singleton pattern
            OscarDataHandler._instance = self
            self.__initialize()

    def __initialize(self):
        self.__WatchdogTimer = None


    def __StartWatchdogTimer(self):
        if None == self.__WatchdogTimer:
            self.__WatchdogTimer = Watchdog.WatchdogTimer()
           # self.__WatchdogTimer.Start()
            
    def HandleIncomingPacket(self,node,rawData,fromAddress):
        if node.nodeName == "OscarGroup":
            self.HandleIncomingGroupPacket(rawData,node,fromAddress)
            return

        try:
            packetType = node.attributes["Type"].nodeValue  #All should have a Type attribute
        except Exception as ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Malformed Oscar Packet: " + rawData)
            return

        if packetType == "Data" :  # is a data packet in a chained Oscar, just send it on down the road
            self.HandleIncomingOscarDatapacket(node,rawData,fromAddress)
        elif packetType == "ConnectionInformation":
            self.HandleIncomingOscarConnectionInformation(node,rawData,fromAddress)
        elif packetType == "WatchdogTimer":
            self.HandleIncomingWatchdogPacket(node,rawData,fromAddress)

        elif packetType == "Refresh":
            self.HandleChainedRefresh(rawData)

        else:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received unknown Oscar Packet Type: " + rawData)


    def HandleChainedRefresh(self,rawData):
        Log.getLogger().info("Sending Refresh from another Oscar")
        TargetManager.GetTargetManager().BroadcastUpstream(rawData)

    def CreateMarvinData(self,node,rawData,fromAddress):
        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue # for future use, not there now
            namespace = node.getElementsByTagName('Namespace')[0].firstChild.nodeValue
            value = node.getElementsByTagName('Value')[0].firstChild.nodeValue
            ID = node.getElementsByTagName('ID')[0].firstChild.nodeValue
            
        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received Chained Oscar Packet:" + rawData)
            return None

        objData = MarvinData.MarvinData(namespace,ID,value,0,version,True)
        return objData


    def HandleIncomingOscarDatapacket(self,node,rawData,fromAddress):
        from Helpers import Configuration
        # Use this object to update the gui and for recording
        # is also where BITW check update of checked map occurs
        objData = self.CreateMarvinData(node,rawData,fromAddress) 
        
        if Configuration.get().GetBITW_Active():
            rawData = Configuration.get().HandleBITWBuffer(rawData) # handle Bump in the wire

        if 0 != TargetManager.GetTargetManager().BroadcastDownstream(rawData,False,node): # send to all - towards a Marvin
            Statistics.GetStatistics().OnPacketChainedDownstream(rawData)

        if None != objData:
            GuiMgr.OnDataPacketSentDownstream(objData,"Chained")
            Recorder.get().AddData(objData)


    def HandleIncomingGroupPacket(self,rawData,node,fromAddress):
        from Helpers import Configuration
        
        if Configuration.get().GetBITW_Active():
            rawData = Configuration.get().HandleBITWBuffer(rawData) # handle Bump in the wire

        if 0 != TargetManager.GetTargetManager().BroadcastDownstream(rawData,False,node,True): # send to all - towards a Marvin
            Statistics.GetStatistics().OnPacketChainedDownstream(rawData)

        # Break up packet and do bump in the wire.
        # If BITW not active, maybe we don't need to do this could be a lot of processing for nothing....
        objGroupPacket = MarvinGroupData.MarvinDataGroup("","","",0,"1.0",True)
        for packet in node.getElementsByTagName('Oscar'):
            objMarvinPacket = self.CreateMarvinData(packet,rawData,fromAddress)
            if None == objMarvinPacket:
                return
            objGroupPacket.AddPacket(objMarvinPacket)

#        objData = MarvinData.MarvinData(namespace,ID,value,0,version,True)
        GuiMgr.OnDataPacketSentDownstream(objGroupPacket,"Chained")
        Recorder.get().AddData(objGroupPacket)


    # Recevied a connection info packet from another Oscar - must be upstream
    # from this one.
    def HandleIncomingOscarConnectionInformation(self,node,rawData,fromAddress):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Oscar Type="ConnectionInformation">
        #    <Version>1.0</Version>
        #    <OccarVersion>16.11.21 Build 2</OscarVersion>
        #    <ID>Foo</Foo>
        #    <Port>Port</Port>
        #</Oscar>

        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            ID = node.getElementsByTagName('ID')[0].firstChild.nodeValue
            Port = int(node.getElementsByTagName('Port')[0].firstChild.nodeValue)

        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Oscar Connection Information Packet : " + rawData)
            return

        try:
            oscarVersion = node.getElementsByTagName('OscarVersion')[0].firstChild.nodeValue 
        except Exception:
            oscarVersion = 'Unknown'

        IP = fromAddress[0].lower()
        Key = "Oscar:" + ID
        objTarget = TargetManager.GetTargetManager().GetUpstreamTarget(Key)  # Chained Oscar, from Upstream

#        if None == objTarget:
#            objTarget = TargetManager.GetTargetManager().GetDownstreamTargetEx(IP,Port)  # if using DNS, do lookup based on real IP, not DNS name

        if None == objTarget:
            Log.getLogger().info("Adding Upstream Oscar: " + Key + " version- " + oscarVersion)
            objTarget = Target.Target(IP,Port,ConnectionType.UpstreamOscar,False)
            TargetManager.GetTargetManager().AddUpstreamTarget(objTarget,Key)

        elif IP != objTarget.getIP() or Port != objTarget.getPort(): #hmm, doesn't match, Oscar ID's current connection inf should be unique, so assume is an update from a restart of Oscar
            strOld = str(objTarget)
            objTarget.IP = IP
            objTarget.Port = Port
            Log.getLogger().warning("Received a Oscar Connection Information Packet, with Different Connection Info from previously configured [" + ID +"] " + strOld + "--> " + str(objTarget))


    # Recevied a watchdog packet from another Oscar - must be downstream
    # from this one.
    def HandleIncomingWatchdogPacket(self,node,rawData,fromAddress):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Oscar Type=?WatchdogTimer">
        #    <Version>1.0</Version>
        #     <Port>5000</Port>
        #</Oscar>

        Statistics.GetStatistics().OnPacketReceivedFromDownstream(rawData)
        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            IP = fromAddress[0].lower()
            Port = node.getElementsByTagName('Port')[0].firstChild.nodeValue

        except Exception as Ex:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Oscar WatchdogTimer  Packet : " + rawData)
            return

        Key = IP + ":" + Port
        objTarget = TargetManager.GetTargetManager().GetDownstreamTarget(Key)  # Chained Oscar

        if None == objTarget:
            objTarget = TargetManager.GetTargetManager().GetDownstreamTargetEx(IP,Port)  # Chained Oscar, used resolved IP

        if None == objTarget:
            Log.getLogger().warning("Received Oscar Watchdog for unknown downstream Target: ",IP+":"+Port)
            return
        
        if objTarget.getType() != ConnectionType.DownstreamOscar and objTarget.getType() != ConnectionType.DynamicOscar : # would not know what this is until you hear back
            objTarget.Type = ConnectionType.DownstreamOscar
        objTarget.StrokeWatchdogTimer()

def GetDataHandler():
    if OscarDataHandler._instance == None:
        return OscarDataHandler()
    return OscarDataHandler._instance
