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
#    Gui Abstraction
#
##############################################################################

import importlib
from Helpers import Recorder
from Helpers import Playback
from Data import MarvinGroupData
from Util import Utility
from Helpers import Log
from Helpers import VersionMgr
from Helpers import Configuration
from Helpers import ThreadManager

class UI():
    TKINTR = 0
    NCURSES = 1
    NONE = 2
    Unknown = 20


def get():
    return GuiMgr.get()

def Initialize(whichInterface,downstreamServer,upstreamServer):
    GuiMgr.get().Init(whichInterface,downstreamServer,upstreamServer)
               
def OnDataPacketSentDownstream(objData,sentFrom=None):
    if isinstance(objData,MarvinGroupData.MarvinDataGroup):
        for packet in objData._DataList:
            GuiMgr.get().OnDataPacketSentDownstream(packet,sentFrom)
        return

    GuiMgr.get().OnDataPacketSentDownstream(objData,sentFrom)

def OnStartLiveData():
    GuiMgr.get().OnStartLiveData()

def OnStopLiveData():
    GuiMgr.get().OnStopLiveData()

def OnStartRecording():
    GuiMgr.get().OnStartRecording()

def OnStopRecording(flush=False):
    GuiMgr.get().OnStopRecording(flush)

def OnStartPlayback():
    GuiMgr.get().OnStartPlayback()
    
def OnStopPlayback():
    GuiMgr.get().OnStopPlayback()

def OnPausePlayback():
    GuiMgr.get().OnPausePlayback()

def OnSetPlaybackSpeed(speed):
    GuiMgr.get().OnSetPlaybackSpeed(speed)

def Start():
    GuiMgr.get().Start()

def Quit():
    GuiMgr.get().Quit()

def SetTitle(titleStr):
    GuiMgr.get().SetTitle(titleStr)

def SetPlaybackFilename(filename):
    GuiMgr.get().SetPlaybackFilename(filename)

def GetPlaybackFilename():
    return GuiMgr.get().GetPlaybackFilename()


def OnSetRepeatMode(mode,startLoc=0,endLoc=None):
    GuiMgr.get().OnSetRepeatMode(mode,startLoc,endLoc)

def OnEnablePlayback():
    GuiMgr.get().OnEnablePlayback()

def ReadFromFile(filename):
    return GuiMgr.get().ReadFromFile(filename)

def WriteToFile(filename):
    GuiMgr.get().WriteToFile(filename)

def WriteCSVFile(filename,interval):
    GuiMgr.get().WriteCSVFile(filename,interval)

def MessageBox_Info(Title,Message):
    GuiMgr.get().MessageBox_Info(Title,Message)

def MessageBox_Error(Title,Message):
    GuiMgr.get().MessageBox_Error(Title,Message)

def MessageBox_OkCancel(Title,Message):
    return GuiMgr.get().MessageBox_OkCancel(Title,Message)

class GuiMgr(object):
    __inst = None
    def __init__(self):
        GuiMgr.__inst = self
        self._downstreamServer = None
        self._upstreamServer = None
        self.dataList = {}
        
        self.Live_Active=True           #The live window is Visable
        self.Live_Receiving=True       
        self.Live_Recording=False

        self.Playback_Active=False      # The Playback window is Visable
        self.Playback_Playing=False
        self.Playback_Stopped=False
        self.Playback_Paused=False
        self.Playback_File=None

    def Init(self,whichUI,downstreamServer,upstreamServer):
        self.pGui = None

        if whichUI == UI.TKINTR :
            self._SetupTkGui()

        else:
            self._SetupGuiNone()

        self._downstreamServer = downstreamServer
        self._upstreamServer = upstreamServer


    def SetPlaybackFilename(self,filename):
        self.Playback_File = filename
        if None == filename or len(filename) <1:
            self.SetTitle("")
        else:
            self.SetTitle("- {"+filename+"}")

    def GetPlaybackFilename(self):
        return self.Playback_File

    def Start(self):
        self.SetPlaybackFilename("")
        #self.SetTitle("")
        self.pGui.OnStart()

    def Quit(self):
        ThreadManager.GetThreadManager().StopAllThreads()
        self.pGui.OnQuit()

    def _SetupGuiNone(self):
        from Helpers import GuiNone
        self.pGui = GuiNone.GuiNone()

    def _SetupTkGui(self):
        try:
            from Helpers import GuiTK
        except ImportError:
            raise ImportError('GUI unavailable, using Console Mode.')
        
        self.pGui = GuiTK.GuiTK()

    @staticmethod
    def get():
        if None == GuiMgr.__inst :
            GuiMgr.__inst = GuiMgr()

        return GuiMgr.__inst

    def GetDatalist(self):
        return self.dataList

    def ClearDataView(self):
        newDl = {}
        self.dataList=newDl
        self.pGui.OnClearData()

    def OnDataPacketSentDownstream(self,objData,sentFrom):
        key = objData.Namespace.upper()+":"+objData.ID.upper()
        self.dataList[key] = (objData,sentFrom)

    def OnStartLiveData(self):
        self._upstreamServer.DropPackets(False)
        self.Live_Receiving = True
        Playback.get().Clear()
        Recorder.get().Clear()

    def OnStopLiveData(self):
        self._upstreamServer.DropPackets(True)
        self.Live_Receiving = False
        if Recorder.get().GetRecordedCount()>0:
            self.OnEnablePlayback()

    def OnStartRecording(self):
        if True == self.Live_Active and True == self.Live_Receiving:
            self.Live_Recording=True
            Recorder.get().Clear()
            Recorder.get().Start()

    def OnStopRecording(self,flush):
        if True == self.Live_Recording:
            self.Live_Recording=False
            Recorder.get().Stop(flush)

    def ShowLive(self,showFlag):
        self.Live_Active = showFlag

    def ShowPlayback(self,showFlag):
        self.Playback_Active = showFlag

    def OnStartPlayback(self):
        if False == self.Playback_Active:
            return
        
        if True == self.Playback_Playing:
            return

        self.Playback_Playing = True
        self.Playback_Stopped = False
        self.Playback_Paused = False

        Playback.get().Start()

    def OnEnablePlayback(self):
        self.Playback_Active = True
        pass

    
    def OnStopPlayback(self):
        self.Playback_Stopped = True
        self.Playback_Playing = False

        Playback.get().Stop()

    def OnPausePlayback(self):
        self.Playback_Playing = False
        self.Playback_Paused = True
        Playback.get().Pause()

    def SetTitle(self,titleStr):
        name = Configuration.get().GetID()
        if None == name:
            name =""

        name = "[" + name + "]"
        TitleString = "Oscar " + name +" - " +  VersionMgr.ReadVer() + " [" + str(Configuration.get().GetUpstreamConnection()) +"]" + titleStr
        self.pGui.SetTitle(TitleString)

    def OnSetPlaybackSpeed(self,speed):
        if not Utility.IsNumeric(speed):
            Log.getLogger().error("Tried to set playback speed to invalid value: " + str(speed))
            return

        if speed<=0 :
            Log.getLogger().error("Tried to set playback speed to invalid value: " + str(speed))
            return

        Playback.get().SetPlaybackSpeed(speed)

    def GetPlaybackSpeed(self):
        return Playback.get().GetPlaybackSpeed()

    def OnSetRepeatMode(self,mode,startIndex=0,endIndex=None):
        Playback.get().SetLoopMode(mode,startIndex,endIndex)

    def GetRepeatMode(self):
        return Playback.get().GetLoopMode()

    def MessageBox_Info(self,Title,Message):
        self.pGui.MessageBox_Info(Title,Message)

    def MessageBox_Error(self,Title,Message):
        self.pGui.MessageBox_Error(Title,Message)

    def MessageBox_OkCancel(self,Title,Message):
        return self.pGui.MessageBox_OkCancel(Title,Message)

    def ReadFromFile(self,filename):
        if Playback.get().ReadFromFile(filename):
            self.OnEnablePlayback()
            self.ClearDataView()
            Recorder.get().OnSaved()
            return True
        return False

    def WriteToFile(self,filename):
        if Playback.get().WriteToFile(filename):
            Recorder.get().OnSaved()
        
    def WriteCSVFile(self,filename,interval):
        Playback.get().WriteCSVFile(filename,interval)
    
