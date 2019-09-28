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
from Data import MarvinData
from Data import ConnectionPoint
from Util import Time
from Helpers import Playback
from Helpers import GuiMgr
from Data.ConnectionPoint import ConnectionType
from Helpers import TargetManager
from Helpers import Statistics
from Helpers import Target
from Helpers import Recorder
from Helpers import Configuration
from Helpers import Alias
from Util import Sleep

import sys

class MarvinDataHandler(object):
    _instance = None
    def __init__(self):
        if MarvinDataHandler._instance == None: # singleton pattern
            MarvinDataHandler._instance = self
            self.__initialize()

    def __initialize(self):
        self.LastUniqueID = ''
        

    def HandleIncomingPacket(self,node,rawData,fromAddr):
        Statistics.GetStatistics().OnPacketReceivedFromDownstream(rawData)
        try:
            packetType = node.attributes["Type"].nodeValue  #All should have a Type attribute
            _  = node.getElementsByTagName('Version')
        except Exception as _:
            Statistics.GetStatistics().OnMalformedPacketReceived("Malformed Marvin Packet: " + rawData)
            return
        
        if packetType == "WatchdogTimer":
            self.HandleIncomingWatchdogPacket(node,rawData,fromAddr)

        elif packetType == "MinionTask":
            self.HandleMinionTask(rawData,node)

        elif packetType == "OscarTask":
            self.HandleOscarTask(node,rawData)

        elif packetType == "RemoteMarvinTask":
            self.HandleRemoteMarvinTask(rawData)

        elif packetType == "Bullhorn":
            self.HandleBullhornAnnouncement(node,rawData,fromAddr)

        else :
            Statistics.GetStatistics().OnMalformedPacketReceived("Received unknown Packet Type: " + rawData)

    # is a remote marvin task, blast downstream
    def HandleOscarTask(self,node,rawData):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <UniqueID>1233456</UniqueID>
        #     <Task>taskid</Task>
        #     <Param>p1</Param>
        #</Marvin>
        Statistics.GetStatistics().OnOscarTaskReceived()
        try:
            UniqueID = node.getElementsByTagName('UniqueID')[0].firstChild.nodeValue

            OscarID = node.getElementsByTagName('OscarID')[0].firstChild.nodeValue
            task = node.getElementsByTagName('Task')[0].firstChild.nodeValue
            tTask=task.lower()
            Params = []

            for param in node.getElementsByTagName('Param'): # Make an array of the params for the script
                strParam = param.firstChild.nodeValue
                Params.append(strParam)
        except Exception as _:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received bad Oscar Task: " + rawData)

        if OscarID.lower() != Configuration.get().GetID().lower(): # Oscar packet, but probably not destined for this Oscar
            if OscarID.lower() == "Broadcast".lower():
                Log.getLogger().info("Received Broadcast Oscar task: " + task)
            else:
                Log.getLogger().info("Received Oscar task [" + task +"] for another Oscar with ID: " + OscarID) # not for this guy
                #increment dropped stats?
                return

        if UniqueID == self.LastUniqueID:
            #Log.getLogger().info("Dropping duplicate Oscar Task")
            return

        self.LastUniqueID = UniqueID

        if tTask == "StartLive".lower():
            self.PerformGoLiveTask(Params)

        elif tTask == "StopLive".lower():
            self.PerformStopLiveTask(Params)

        elif tTask == "LoadFile".lower():
            self.PerformLoadFileTask(Params)

        elif tTask == "Playback".lower():
            self.PerformPlayFileTask(Params)

        elif tTask == "StopPlayback".lower():
            self.PerformStopPlaybackTask(Params)

        elif tTask == "PausePlayback".lower():
            self.PerformPausePlaybackTask(Params)

        elif tTask == "StartRecording".lower():
            self.PerformStartRecordingTask(Params)

        elif tTask == "StopRecording".lower():
            self.PerformStopRecordingTask(Params)

        elif tTask == "InsertBookmark".lower():
            self.PerformInsertBookmark(Params)

        else:
            Log.getLogger().warn("Unknown Oscar Task: " + task)

    def PerformInsertBookmark(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <UniqueID>1233456</UniqueID>
        #     <Task>InsertBookmark</Task>
        #     <Param>Namespace=foo</Param>
        #     <Param>ID=TestMarker</Param>
        #     <Param>Data=StartTest</Param>
        #     <Param>Propagate=True</Param> -- if True, the data is sent back upstream towards Marvin
        #</Marvin>
        if len(Params) >= 3:
            fPropagate = False
            for param in Params:
                parts = param.split("=")
                if len(parts)==2:
                    if parts[0].lower()=='namespace':
                        namespace = Alias.Alias(parts[1])
                    elif parts[0].lower()=='id':
                        id = Alias.Alias(parts[1])
                    elif parts[0].lower()=='data':
                        data = Alias.Alias(parts[1])
                    elif parts[0].lower()=='propagate':
                        propagate = Alias.Alias(parts[1])
                        if propagate.lower() == "true":
                            fPropagate = True

                    else:
                        Log.getLogger().error("Received invalid InsertBookmark task parameter: " + str(param))
                        return
        else:
            Log.getLogger().error("Received invalid InsertBookmark task.  Insufficient Parameters.")
            return

        if None == namespace:
            Log.getLogger().error("Received invalid InsertBookmark task.  Namespace not specified.")
            return

        if None == id:
            Log.getLogger().error("Received invalid InsertBookmark task.  ID not specified.")
            return

        if None == data:
            Log.getLogger().error("Received invalid InsertBookmark task.  Data not specified.")
            return
        
        eTime = Time.GetCurrMS()
        objData = MarvinData.MarvinData(namespace,id,data,eTime,1.0)
        if None != objData:
            Recorder.get().AddData(objData)
            GuiMgr.OnDataPacketSentDownstream(objData,"Minion")
            if fPropagate:
                TargetManager.GetTargetManager().BroadcastDownstream(objData.ToXML(),False,None)


    def PerformStartRecordingTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>StartRecording</Task>
        #</Marvin>
        GuiMgr.OnStartRecording()

    def PerformStopRecordingTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>StopRecording</Task>
        #     <Param>File=SaveFile.glk</Param>
        #</Marvin>

        fileName = None
        param=""
        if len(Params) > 0:
            param = Params[0]
            parts = param.split("=")
            if len(parts)==2:
                if parts[0].lower()=='file':
                    fileName = Alias.Alias(parts[1])

        if None == fileName:
            Log.getLogger().error("Received invalid Stop Recording task.  No save file: " + str(param))
            return

        GuiMgr.OnStopRecording()
        GuiMgr.WriteToFile(fileName)
        

    def PerformStopPlaybackTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>PausePlayback</Task>
        #</Marvin>
        Log.getLogger().info("Performing Oscar task: Stop Playback.")
        GuiMgr.OnStopPlayback()


    def PerformPausePlaybackTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>PausePlayback</Task>
        #</Marvin>
        Log.getLogger().info("Performing Oscar task: Pause Playback.")
        GuiMgr.OnPausePlayback()

    def PerformStopLiveTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>StopLive</Task>
        #</Marvin>
        Log.getLogger().info("Performing Oscar task: Stop Live Data.")
        GuiMgr.OnStopLiveData()

    def PerformGoLiveTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>Go Live</Task>
        #</Marvin>
        Log.getLogger().info("Performing Oscar task: Start Live Data.")
        GuiMgr.OnStopPlayback()
        GuiMgr.OnStopRecording()
        GuiMgr.OnStartLiveData()

    def PerformLoadFileTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>LoadFile</Task>
        #     <Param>filename</Param>
        #</Marvin>
        if len(Params) !=1:
            Log.getLogger().error("Oscar Task to load file failed - no file specified.")

        filename = Alias.Alias(Params[0])
        Log.getLogger().info("Performing Oscar task: Load File -->" + str(filename))
        GuiMgr.OnStopPlayback()
        GuiMgr.OnStopRecording(True) #drop all recorded packets
        GuiMgr.OnStopLiveData()
        if GuiMgr.ReadFromFile(filename):
            GuiMgr.OnEnablePlayback()
            GuiMgr.SetPlaybackFilename(filename)
        else:
            Log.getLogger().warning("Oscar Task to load file [" + filename +"] failed")
            return False

        return True

    def PerformPlayFileTask(self,Params):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>Playback</Task>
        #     <Param>speed=2</Param>
        #</Marvin>
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>Playback</Task>
        #     <Param>speed=2</Param>
        #     <Param>repeat</Param>
        #</Marvin>
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="OscarTask">
        #    <Version>1.0</Version>
        #     <OscarID>DemoOscar</OscarID>
        #     <Task>Playback</Task>
        #     <Param>loop</Param>
        #     <Param>speed=2</Param>
        #     <Param>start=10</Param>
        #     <Param>end=2400</Param>
        #</Marvin>

        speed = 1
        start = 0
        end = Playback.get().GetDataCount()
        type = Playback.RepeatMode.NONE
        file = None

        for param in Params:
            tParam = Alias.Alias(param.lower())
            if tParam == "repeat":
                type = Playback.RepeatMode.REPEAT
            elif tParam == "loop":
                type = Playback.RepeatMode.LOOP
            else:
                parts = tParam.split("=")
                if len(parts)==2:
                    if parts[0].lower()=='start' and Utility.IsNumeric(Alias.Alias(parts[1])):
                        start = int(parts[1])
                    elif parts[0].lower()=='end' and Utility.IsNumeric(Alias.Alias(parts[1])):
                        end = int(parts[1])
                    elif parts[0].lower()=='speed' and Utility.IsNumeric(Alias.Alias(parts[1])):
                        speed = int(parts[1])
                    elif parts[0].lower()=='file':
                        file=parts[1]
                        if False == self.PerformLoadFileTask([file]):
                            return

                        end = Playback.get().GetDataCount()

                    else:
                        Log.getLogger().info("Received unknown Oscar Play parameter: " + param)
                        return
                else:
                    Log.getLogger().info("Received unknown Oscar Play parameter: " + param)
                    return

        if Playback.get().GetDataCount() == 0:
            Log.getLogger().info("Oscar Play received - but no file loaded.")
            return

        if start >= end:
            Log.getLogger().info("Oscar Play Loop  - but start > end.")
            return 

        Log.getLogger().info("Performing Oscar task: Start Playback.")
        GuiMgr.OnStopRecording(True) #drop all recorded packets
        GuiMgr.OnStopLiveData()

        GuiMgr.OnSetPlaybackSpeed(speed)
        GuiMgr.OnSetRepeatMode(type,start,end)
        GuiMgr.OnStartPlayback()

    # is a remote marvin task, blast downstream, like a loopback
    def HandleRemoteMarvinTask(self,rawData):
        Statistics.GetStatistics().OnMarvinTaskReceived()
        TargetManager.GetTargetManager().BroadcastDownstream(rawData,False,None)

    # is a minion task, so send it upstream
    def HandleMinionTask(self,rawData,node):
        Statistics.GetStatistics().OnMinionTaskReceived()
        TargetManager.GetTargetManager().BroadcastUpstream(rawData) # I could filter out based upon Namespace, but with chained Oscars it got harder, so now let minion filter it out

        try:
            TaskNode   = node.getElementsByTagName('Task')[0]
            
            Log.getLogger().debug("Passing Minion Task --> ID: " + TaskNode.attributes['ID'].nodeValue + " Namespace: " + TaskNode.attributes['Namespace'].nodeValue)
        except:
            pass


    # Recevied a watchdog packet from a Marvin - must be downstream
    def HandleIncomingWatchdogPacket(self,node,rawData,fromAddr):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="WatchdogTimer">
        #    <Version>1.0</Version>
        #    <MarvinVersion>17.12.22</MarvinVersion>
        #    <UniqueID>3236</UniqueID>
        #    <Port>5000</Port>
        #</Marvin>

        Statistics.GetStatistics().OnPacketReceivedFromDownstream(rawData)
        try:
            _ = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            IP = fromAddr[0].lower()
            Port = node.getElementsByTagName('Port')[0].firstChild.nodeValue
            UniqueID = node.getElementsByTagName('UniqueID')[0].firstChild.nodeValue

        except Exception as _:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Marvin WatchdogTimer  Packet : " + rawData)
            return

        try:
            marvinVersion = node.getElementsByTagName('MarvinVersion')[0].firstChild.nodeValue 

        except Exception:
            marvinVersion='Unknown'

        Key = IP + ":" + Port
        objTarget = TargetManager.GetTargetManager().GetDownstreamTarget(Key)  

        if None == objTarget:
            objTarget = TargetManager.GetTargetManager().GetDownstreamTargetEx(IP,Port)  # if using DNS, do lookup based on real IP, not DNS name

        if None == objTarget:
            Sleep.Sleep(50) #give it another shot, other thread may be doing a DNS resolution            
            objTarget = TargetManager.GetTargetManager().GetDownstreamTargetEx(IP,Port)  # if using DNS, do lookup based on real IP, not DNS name

        if None == objTarget:
            Log.getLogger().warning("Received Marvin Watchdog for unknown downstream Target: " +IP+":"+Port + " Version: " + marvinVersion)
            return
        
        if objTarget.getType() != ConnectionType.Marvin and objTarget.getType() != ConnectionType.DynamicMarvin : # would not know what this is until you hear back (could be another Oscar)
            objTarget.Type = ConnectionType.Marvin
            Log.getLogger().info("Connection established with Marvin Target: "+ IP + ":" + Port + " Version: " + marvinVersion)

        try:
            _ = node.getElementsByTagName('RefreshRequested')[0].firstChild.nodeValue 
            objTarget.ReArmRefreshRequest(UniqueID)  # Asked to refresh!

        except Exception as _:
            pass

        objTarget.StrokeWatchdogTimer()

    def HandleBullhornAnnouncement(self,node,rawData,fromAddr):
        #<?xml version="1.0" encoding="utf-8"?>
        #<Marvin Type="Bullhorn">
        #    <Version>1.0</Version>
        #    <UniqueID>3236</UniqueID>
        #    <Hostname>pgkutch.beervana.net</Hostname>
        #    <Key>md5 hash</Key>
        #    <Port>5000</Port>
        #</Marvin>

        try:
            version = node.getElementsByTagName('Version')[0].firstChild.nodeValue 
            Hash = node.getElementsByTagName('Key')[0].firstChild.nodeValue
            Port = node.getElementsByTagName('Port')[0].firstChild.nodeValue
            UniqueID = node.getElementsByTagName('UniqueID')[0].firstChild.nodeValue
            IP = fromAddr[0].lower()
            Hostname = node.getElementsByTagName('Hostname')[0].firstChild.nodeValue

        except Exception as _:
            Statistics.GetStatistics().OnMalformedPacketReceived("Received invalid Marvin Bullhorn  Packet : " + rawData)
            return

        RemoteKey = Configuration.get().GetMarvinAutoConnectKeyFromHash(Hash)

        strID = Hostname + ":[" + IP + ":" + Port +"]" 

        if None == RemoteKey:  #don't have anything configured that matches
            Log.getLogger().warning("Received Marvin Dynamic Connection Message, with no corropsonding Key from: " + strID)
            return
        
        strID += " Key=" + RemoteKey

        HashMapKey = IP + ":" + str(Port)
        objExisting = TargetManager.GetTargetManager().GetDownstreamTarget(HashMapKey)
        if None == objExisting: # could be NDS name not resolved, so try by IP address
            objExisting = TargetManager.GetTargetManager().GetDownstreamTargetEx(IP,Port)

        if None != objExisting:
            if hasattr(objExisting,'_ReceivedOnUniqueID') and UniqueID != objExisting._ReceivedOnUniqueID:
                Log.getLogger().warning("Received Marvin Dynamic Connection Message, for already active connection: " + strID)
            else:
                pass # is simply the additional packets (marvin sends multiples as it is UDP traffic)
            return

        # doesn't already exist, so let's to add!
        if "1.0" == version:
            objTarget = Target.Target(IP,Port,ConnectionType.DynamicMarvin,True)
        else:
            objTarget = Target.Target(IP,Port,ConnectionType.DynamicOscar,True)

        objTarget._ReceivedOnUniqueID = UniqueID # so we can filter out dups due to UDP
        objTarget._UserKey = RemoteKey
        TargetManager.GetTargetManager().AddDownstreamTarget(objTarget,HashMapKey)

        Log.getLogger().info("Creating Dynamic Connection:" + strID)

        return


def GetDataHandler():
    if MarvinDataHandler._instance == None:
        return MarvinDataHandler()
    return MarvinDataHandler._instance
