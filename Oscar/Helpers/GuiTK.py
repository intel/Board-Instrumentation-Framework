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
#    Abstraction for UI GUI
#
##############################################################################
from tkinter import ttk
from tkinter import Tk
from tkinter import *
from Helpers import GuiMgr
from Helpers import TargetManager
from Helpers import Configuration
from Helpers import Target
from Helpers import Log
from Helpers import Recorder
from Helpers import Statistics
from Data import ConnectionPoint
import tkinter.messagebox
import tkinter.filedialog
import ntpath
import os
import tkinter.simpledialog
from Helpers import Playback
from Util import Time
from Helpers import VersionMgr


def enabled(objWidget):
    if 'normal' == objWidget.cget('state'):
        return True
    return False

def enable(objWidget):
    objWidget.config(state=NORMAL)

def disable(objWidget):
    objWidget.config(state=DISABLED)

class GuiTK(object):
    def __init__(self):
        self.updateList=[]
        self.root = Tk()
        useTab = True
        self._LastDataPointCount=0

        if useTab:
            self.TabPane = ttk.Notebook(self.root)
            self.tabFrameData = ttk.Frame(self.TabPane)
            self.tabFrameStatistics = ttk.Frame(self.TabPane)
            self.setupStatisticsTab(self.tabFrameStatistics)
            self.TabPane.add(self.tabFrameData,text="Data")
            self.TabPane.add(self.tabFrameStatistics,text="Statistics")
            self.TabPane.grid(row=0,sticky=(N,S,E,W))
            self.TabPane.rowconfigure(0,weight=1) # Makes it so the data frame will grow
            self.TabPane.columnconfigure(0,weight=1) # Makes it so the data frame will grow

        else:
            self.tabFrameData = ttk.Frame(self.root)
            self.tabFrameData.grid(row=0,column=0,sticky=(N,W,E,S)) #Sticky says to grow in those directions,
#            self.tabFrame1.rowconfigure(0,weight=1)
#            self.tabFrame1.columnconfigure(0,weight=1)

        self.tabFrameData.rowconfigure(0,weight=1)
        self.tabFrameData.columnconfigure(0,weight=1)

        self.setupDataTab(self.tabFrameData) 
            
        #Makes tabFrame1 frame grow to size of app
        self.root.rowconfigure(0,weight=1)
        self.root.columnconfigure(0,weight=1)

        self._menu = MenuSystem(self.root)
        self.root.config(menu=self._menu.get())

        self.updateList.append(self._menu)
        
        if Configuration.get().GetMinimizeGui():
            self.root.iconify()

    def SetTitle(self,titleStr):
        self.root.title(titleStr)

    def MessageBox_Error(self,Title,Message):
        tkinter.messagebox.showerror(Title,Message)

    def MessageBox_Info(self,Title,Message):
        tkinter.messagebox.showinfo(Title,Message)

    def MessageBox_OkCancel(self,Title,Message):
        return tkinter.messagebox.askokcancel(Title,Message)

    def setupDataTab(self,parent):
        self.dataFrame = ttk.Frame(parent,borderwidth=5)  #contains data tree and clear btn
        self.dView = DataView(self.dataFrame)
        self.dataFrame.grid(row=0,column=0,sticky=(N,E,S,W))

        self.dataText = StringVar()
        self.dataText.set("Data")
        Label(self.dataFrame,textvariable = self.dataText).grid(row=0,column=0,sticky=(N))
        self.dView.get().grid(row=1,column=0,sticky=(N,W,S,E))

        self.dataFrame.rowconfigure(1,weight=1) # Makes it so the data frame will grow
        self.dataFrame.columnconfigure(0,weight=1)

        self.targetFrame = ttk.Frame(parent,borderwidth=5)
        self.targetFrame.grid(row=0,column=1,sticky=(N,S))

        self.liveCtrl = LiveControls(parent)
        self.liveCtrl.get().grid(row=3,sticky=S)
        self.targetView = TargetView(self.targetFrame)

        self.playbackCtrl = PlaybackControls(parent)
        self.playbackCtrl.get().grid(row=3,column=1)
        self.playbackCtrl.get().grid_remove()

        self.updateList.append(self.playbackCtrl)
        self.updateList.append(self.dView)
        self.updateList.append(self.liveCtrl)
        self.updateList.append(self.targetView)

    def setupStatisticsTab(self,pane):
        self.statsView = StatisticsView(pane)
        self.updateList.append(self.statsView)

    def OnStart(self):
        Log.getLogger().info("Starting GUI")

        self.root.after(100,self.UpdateGui)
        self.root.mainloop()

    def OnQuit(self):
        self.root.quit()

    def OnClearData(self):
        self.dView.Clear()


    def UpdateGui(self):
        for widget in self.updateList:
            widget.updateGui()

        currCount = len(GuiMgr.get().GetDatalist())
        if currCount != self._LastDataPointCount:
            self.dataText.set("Data {" + str(currCount) +"}")
            self._LastDataPointCount = currCount 
        self.root.after(100,self.UpdateGui)

# Found some of this nice scroll bar code at: http://svn.python.org/projects/python/branches/pep-0384/Demo/tkinter/ttk/dirbrowser.py
def autoscroll(sbar, first, last):
    """Hide and show scrollbar as needed."""
    first, last = float(first), float(last)
    if first <= 0 and last >= 1:
        sbar.grid_remove()
    else:
        sbar.grid()
    sbar.set(first, last)

class DataView():
    def __init__(self,parent):
        self.parent = parent
        self.root = ttk.Frame(parent,borderwidth=5,relief="ridge")       # Frame with Tree Control in it and button
        vsb = ttk.Scrollbar(self.root,orient="vertical")
        hsb = ttk.Scrollbar(self.root,orient="horizontal")

        self.dataViewTree = ttk.Treeview(self.root,yscrollcommand=lambda f, l: autoscroll(vsb, f, l),
                xscrollcommand=lambda f, l:autoscroll(hsb, f, l))
        vsb['command'] = self.dataViewTree.yview
        hsb['command'] = self.dataViewTree.xview
        vsb.grid(column=1, row=0, sticky='ns')
        hsb.grid(column=0, row=1, sticky='ew')

        self.dataViewTree['columns'] = ('Namespace','ID','Value','Source')
        self.dataViewTree.heading('Namespace',text='Namespace')
        self.dataViewTree.heading('ID',text='ID')
        self.dataViewTree.heading('Value',text='Value')
        self.dataViewTree.heading('Source',text='Source')

        self.dataViewTree.column('Namespace',width=150)
        self.dataViewTree.column('ID',width=100)
        self.dataViewTree.column('Value',width=200,anchor='e')
        self.dataViewTree.column('Source',width=60)
        self.dataViewTree['show'] = 'headings'  # gets rid of 1st empty column

        self.dataViewTree.grid(row=0,column=0,padx=2,pady=2,sticky=(N,E,S,W))

        #allows the root frame to grow
        self.root.columnconfigure(0,weight=1)
        self.root.rowconfigure(0,weight=1)

        Button(self.root,text="Clear",command=self.onClearBtn).grid(row=2,column=0)

    def onClearBtn(self):
        GuiMgr.get().ClearDataView()

    def Clear(self):
        self.dataViewTree.delete(*self.dataViewTree.get_children())

    def get(self):
        return self.root

    def __findIndex(self,key):
        index = 0
        for child in self.dataViewTree.get_children():
            if key < child:
                return index

            index +=1


        return 'end'


    def updateGui(self):
        try:
            dlist = GuiMgr.get().GetDatalist()
            
            for key in dlist.keys():
                try:
                    objData = dlist[key][0]
                    strFrom = dlist[key][1]
                
                    self.dataViewTree.set(key,'Value',objData.Value)
                    self.dataViewTree.set(key,'Source',strFrom)
                except Exception as Ex:
                    try:
                        index = self.__findIndex(key)
                        self.dataViewTree.insert('',index,key,values=(objData.Namespace,objData.ID,str(objData.Value),strFrom))
                    except Exception as Ex:
                        Log.getLogger().error(str(Ex))
        except Exception as Ex: # Likely had the list updated while I was iterating (didn't make this thread safe), just ignore and wait for next loop
            pass

class TargetView():
    def __init__(self,parent):
        self.root =  parent#ttk.Frame(parent,borderwidth=5,relief="sunken")
        self.tree = ttk.Treeview(self.root)
        self.tree['columns'] = ('IP','Port','Type','Packets','Bytes')
        self.tree.heading('IP',text='IP')
        self.tree.heading('Port',text='Port')
        self.tree.heading('Type',text='Type')
        self.tree.heading('Packets',text='Packets')
        self.tree.heading('Bytes',text='Bytes')

        self.tree.column('IP',width=100)
        self.tree.column('Port',width=50,anchor='e')
        self.tree.column('Type',width=70,anchor='e')
        self.tree.column('Packets',width=70,anchor='e')
        self.tree.column('Bytes',width=90,anchor='e')

        self.tree['show'] = 'headings'  # gets rid of 1st empty column
        #self.root.grid(sticky=(N,S))
        Label(self.root,text="Targets").grid()
        self.tree.grid(row=1,sticky=(N,S))
        self.root.columnconfigure(0,weight=1)
        self.root.rowconfigure(1,weight=1)
        self.PreviousTargetCount = 0

        #self.tree.grid(row=1,column=0,padx=2,pady=2,sticky=(N,E,S,W))


    
    def get(self):
        return self.root

    def updateGui(self):
        index = 0
        targets = TargetManager.GetTargetManager().GetDownstreamTargets()
        
        # was a channge in number (maybe a dynamci marvin went away) so just clear the tree and re-poplualte
        if len(targets) != self.PreviousTargetCount:
            self.PreviousTargetCount = len(targets)
            self.tree.delete(*self.tree.get_children())
            

        for key in targets:
            target = TargetManager.GetTargetManager().GetDownstreamTarget(key)
            strPackets=str(target.m_PacketsSent)
            strBytes = str(target.m_BytestSent)
            strType = target.getTypeStr()
            try:
                self.tree.set(key,'Packets',strPackets)
                self.tree.set(key,'Bytes',strBytes)
                self.tree.set(key,'Type',strType)
                if True == target.m_hasTimedOut:
                    self.tree.set(key,'IP',"*"+target.getIP())
                        
                else:
                    self.tree.set(key,'IP',target.getIP())
            except Exception as Ex:
                try:
                    self.tree.insert('','end',key,values=(target.getIP(),str(target.getPort()),strType,strPackets,strBytes))
                except Exception as Ex:
                    Log.getLogger().error(str(Ex))


class LiveControls():
    def __init__(self,parent):
        self.parent = parent
        self.root = ttk.Frame(parent,borderwidth=5,relief="groove")
        self.Visible = True
        baseRow=1

        self.btnStartLive = Button(self.root,text="Go Live",width=10,command=self.onLiveStartBtn)
        self.btnStopLive = Button(self.root,text="Stop Live",width=10,command=self.onLiveStopBtn)
        self.btnStartRecording = Button(self.root,text="Record",width=10,command=self.onRecordStartBtn)
        self.btnStopRecording = Button(self.root,text="Stop Recording",width=15,command=self.onRecordStopBtn)
        
        Label(self.root,text = "Live Data Control").grid(columnspan=4)
        self.btnStartLive.grid(row=baseRow,column=0)
        self.btnStopLive.grid(row=baseRow,column=1)
        self.btnStartRecording.grid(row=baseRow,column=2)
        self.btnStopRecording.grid(row=baseRow,column=3)

        # treeview of recording info
        self.RecordingInfoFrame = ttk.Frame(self.root)
        self.RecordingInfoFrame.grid(row=baseRow+1,columnspan=4)

        self.RecordingTree = ttk.Treeview(self.RecordingInfoFrame,height=1)
        self.lblRecordedInfo = Label(self.RecordingInfoFrame,text="Recorded Data")
        self.RecordingTree['columns'] = ('COUNT','MEM','SECS')
        self.RecordingTree.heading('COUNT',text='Count')
        self.RecordingTree.heading('MEM',text='Approx. Mem')
        self.RecordingTree.heading('SECS',text='Seconds')
        self.RecordingTree.column('COUNT',width=70,anchor='center')
        self.RecordingTree.column('MEM',width=75,anchor='center')
        self.RecordingTree.column('SECS',width=75,anchor='center')
        self.RecordingTree['show'] = 'headings'  # gets rid of 1st empty column
        self.RecordingTree.insert('',0,"foo",values=('0','0','0'))
        self.lblRecordedInfo.grid()
        self.RecordingTree.grid(row=1,column=0)
        self.RecordingInfoFrame.grid_remove()


    def get(self):
        return self.root
        
    def onLiveStartBtn(self):
        if not Recorder.get().HasBeenSaved() and Recorder.get().GetRecordedCount()>0:
            response = GuiMgr.MessageBox_OkCancel("Warning","You have not saved the current recorded data.  OK to Discard?")
            if False == response:
               return
        GuiMgr.OnStartLiveData()
        GuiMgr.SetPlaybackFilename("")
        #GuiMgr.SetTitle("")

    def onLiveStopBtn(self):
        GuiMgr.OnStopLiveData()

    def onRecordStartBtn(self):
        if not Recorder.get().HasBeenSaved() and Recorder.get().GetRecordedCount()>0:
            response = GuiMgr.MessageBox_OkCancel("Restart Recording?","You have not saved the current recorded data.  OK to Discard?")
            if False == response:
               return

        GuiMgr.OnStartRecording()

    def onRecordStopBtn(self):
        GuiMgr.OnStopRecording()

    def updateGui(self):
        if True == GuiMgr.get().Playback_Playing and (enabled(self.btnStopLive) or enabled(self.btnStartRecording)):
            disable(self.btnStopLive)
            disable(self.btnStartRecording)

        if False == GuiMgr.get().Live_Active and True == self.Visible:
            self.root.grid_remove()
            self.Visible = False
            return

        if True == GuiMgr.get().Live_Active and False == self.Visible:
            self.root.grid()
            self.Visible=True

        if False == self.Visible:
            return

        if True == GuiMgr.get().Playback_Playing and enabled(self.btnStartLive):
            disable(self.btnStartLive)

        if True == GuiMgr.get().Playback_Playing:
            return
        
        if True == GuiMgr.get().Live_Receiving and enabled(self.btnStartLive):
            self.btnStartLive.config(state=DISABLED)
            self.btnStopLive.config(state=NORMAL)
            self.RecordingInfoFrame.grid_remove()

        if False == GuiMgr.get().Live_Receiving and enabled(self.btnStopLive):
            enable(self.btnStartLive)
            disable(self.btnStopLive)

        if True == GuiMgr.get().Live_Recording and enabled(self.btnStartRecording):
            disable(self.btnStartRecording)
            enable(self.btnStopRecording)
            disable(self.btnStopLive)
            self.RecordingInfoFrame.grid()

        if enabled(self.btnStartRecording) and not enabled(self.btnStopLive):
            enable(self.btnStopLive)

        if enabled(self.btnStartLive) and enabled(self.btnStartRecording):
            self.btnStartRecording.config(state=DISABLED)

        if not enabled(self.btnStartLive) and not enabled(self.btnStartRecording) and not enabled(self.btnStopRecording):
            enable(self.btnStartRecording)
        
        if False == GuiMgr.get().Live_Recording and enabled(self.btnStopRecording):
            enable(self.btnStartRecording)
            disable(self.btnStopRecording)

        if True == GuiMgr.get().Live_Recording:
            self.RecordingTree.set("foo","COUNT",str(Recorder.get().GetRecordedCount()))
            bytes = Recorder.get().GetBytesRecorded()
            if bytes < 1024:
                strVal = str(bytes)+" B"
            elif bytes < 1024 * 1024:
                strVal = "{0:.2f}".format(float(bytes/1024))+" KB"

            else:
                strVal = "{0:.2f}".format(float((bytes/1024)/1024))+" MB"
            
            self.RecordingTree.set("foo","MEM",str(strVal))
            self.RecordingTree.set("foo","SECS",str(Recorder.get().GetRecordingTime()))

class PlaybackControls():
    def __init__(self,parent):
        self.parent = parent
        self.root = ttk.Frame(parent,borderwidth=5,relief="groove")
        self.Visible = False
        self.LoopValuesVisible=True

        self.btnStartPlayback = Button(self.root,text="Play",width=10,command=self.onPlayBtn)
        self.btnStopPlayback = Button(self.root,text="Stop",width=10,command=self.onStopBtn)
        self.btnPausePlayback = Button(self.root,text="Pause",width=10,command=self.onPauseBtn)

        self.lstBoxRepeatMode = ttk.Combobox(self.root,text="foo",width=6,state="readonly")
        self.lstBoxRepeatMode.bind('<<ComboboxSelected>>',self.onSetPlaybackRepeatMode)
        self.lstBoxRepeatMode['values'] = ("NONE", "REPEAT", "LOOP")
        self.lstBoxRepeatMode.current(0)

        self.lstBoxPlaybackSpeed = ttk.Combobox(self.root,width=3,state="readonly")
        self.lstBoxPlaybackSpeed.bind('<<ComboboxSelected>>',self.onSetPlaybackSpeed)
        self.lstBoxPlaybackSpeed['values'] = (".1", ".25", ".5",".75","1","2","5","10")
        self.lstBoxPlaybackSpeed.current(4)
        self.lblStartLoop = Label(self.root,width=3,justify=CENTER)
        self.lblEndLoop = Label(self.root,width=3,justify=CENTER)
        self.lblPlaybackTime = Label(self.root,width=10,justify=CENTER)

        self.slider = Scale(self.root,from_=0, to=100, orient=HORIZONTAL,length=300,command=self.sliderUpdate)
        self.slider.bind("<ButtonRelease-1>",self.sliderHandler)
        self.lblPacketNumber = Label(self.root,text="0/0",justify=RIGHT)

        self.btnStartLoop = Button(self.root,text="Begin",command=self.onStartLoopBtn)
        self.btnStopLoop = Button(self.root,text="End",command=self.onStopLoopBtn)
        labelRow=0
        btnRow=1
        sliderRow=3
        loopRow=4
        loopBtnRow=5
         
        #playLenStr = str(int(Playback.get().GetPlayTime()/1000))
        
        #Label(self.root,text="playLenStr").grid(row=labelRow,column=0,sticky=(N),columnspan=3) 
        Label(self.root,text="Speed").grid(row=labelRow,column=4,sticky=(N))
        Label(self.root,text="Mode").grid(row=labelRow,column=5,sticky=(N))

        self.btnStartPlayback.grid(row=btnRow,column=0)
        self.btnStopPlayback.grid(row=btnRow,column=1)
        self.btnPausePlayback.grid(row=btnRow,column=2)
        self.lstBoxPlaybackSpeed.grid(row=btnRow,column=4)
        self.lstBoxRepeatMode.grid(row=btnRow,column=5)
        self.lblStartLoop.grid(row=loopRow,column=0,columnspan=2)
        self.lblEndLoop.grid(row=loopRow,column=2,columnspan=2)
        self.slider.grid(row=sliderRow,column=0,columnspan=4)
        self.lblPacketNumber.grid(row=sliderRow,column=4,columnspan=2)
        self.btnStartLoop.grid(row=loopBtnRow,column=0,columnspan=2)
        self.btnStopLoop.grid(row=loopBtnRow,column=2,columnspan=2)
        self.lblPlaybackTime.grid(row=labelRow,column=0,sticky=(N),columnspan=3) 

        disable(self.btnStopPlayback)
        disable(self.btnPausePlayback)

    def get(self):
        return self.root

    def onStartLoopBtn(self):
        currNum = Playback.get().GetCurrentNumber()
        currEnd = Playback.get().GetLoopMode()[2]
        maxNum = Playback.get().GetDataCount()

        if currNum >= currEnd:
            GuiMgr.MessageBox_Error("Error","You cannot set the beginning of a loop to be beyond the end")
            return
        Playback.get().SetLoopMode(Playback.RepeatMode.LOOP,currNum,currEnd)

    def onStopLoopBtn(self):
        currNum = Playback.get().GetCurrentNumber()
        currStart = Playback.get().GetLoopMode()[1]
        maxNum = Playback.get().GetDataCount()

        if currStart >= currNum : 
            GuiMgr.MessageBox_Error("Error","You cannot set the end of a loop to be before the start")
            return
        Playback.get().SetLoopMode(Playback.RepeatMode.LOOP,currStart,currNum)

    def sliderUpdate(self,event):
        number = Playback.get().GetCurrentNumber()
        strVal = str(number)+"/"+str(Playback.get().GetDataCount())

        if GuiMgr.get().GetRepeatMode()[0] != Playback.RepeatMode.NONE:
            strVal = strVal + " Loop: " + str(Playback.get().GetLoopCount())

        self.lblPacketNumber.config(text=strVal)


    def sliderHandler(self,event):
        if GuiMgr.get().Playback_Playing:
            return
        percent = float(self.slider.get())/100.0
        number = int(Playback.get().GetDataCount()*percent)
        Playback.get().SetCurrentNumber(number)
        strVal = str(number)+"/"+str(Playback.get().GetDataCount())
        self.lblPacketNumber.config(text=strVal)

    def onPlayBtn(self):
        GuiMgr.OnStartPlayback()

    def onStopBtn(self):
        GuiMgr.OnStopPlayback()

    def onPauseBtn(self):
        GuiMgr.OnPausePlayback()

    def onSetPlaybackRepeatMode(self,event):
        if "NONE" == self.lstBoxRepeatMode.get():
            GuiMgr.OnSetRepeatMode(Playback.RepeatMode.NONE)
        elif "REPEAT" == self.lstBoxRepeatMode.get():
            GuiMgr.OnSetRepeatMode(Playback.RepeatMode.REPEAT)
        else:
            GuiMgr.OnSetRepeatMode(Playback.RepeatMode.LOOP)

        self.updateLoopValue()

    def onSetPlaybackSpeed(self,event):
        GuiMgr.get().OnSetPlaybackSpeed(float(self.lstBoxPlaybackSpeed.get()))

    def updatePlaybackSpeed(self):
        if GuiMgr.get().GetPlaybackSpeed() == float(self.lstBoxPlaybackSpeed.get()):
            return

        currSpeed = GuiMgr.get().GetPlaybackSpeed()
        insertIndex = 0
        index = 0

        for strVal in self.lstBoxPlaybackSpeed['values']:
            fVal = float(strVal)
            if fVal < currSpeed:
                insertIndex = index
            if fVal == currSpeed:
                self.lstBoxPlaybackSpeed.set(strVal)
                return
            index +=1

        #so it wasn't there, must have been set via cmdline OR via Oscar Task
        itemList = list(self.lstBoxPlaybackSpeed['values'])
        itemList.insert(insertIndex, str(currSpeed))
        
        self.lstBoxPlaybackSpeed['values'] = tuple(itemList)

    def updateLoopValue(self):
        mode = GuiMgr.get().GetRepeatMode()[0]
        if Playback.RepeatMode.toString(mode) == self.lstBoxRepeatMode.get():
            return
        self.lstBoxRepeatMode.set(Playback.RepeatMode.toString(mode))

    def updatePlaybackTime(self):
        currTime = Playback.get().GetCurrentPlaybackTime()
        if currTime > 0:
            currTime = str(int(currTime/1000))
        else:
            currTime = "0"

        endTime = Playback.get().GetPlayTime()
        if endTime > 0:
            endTime = str(int(endTime/1000))
        else:
            endTime = "0"

        strVal = currTime  + "/" + endTime + " secs"
        self.lblPlaybackTime.config(text=strVal) 

    def updateGui(self):
        playbackMgr = Playback.get()
        guiMgr = GuiMgr.get()
        if False == guiMgr.Playback_Active and False == self.Visible:
            return

        self.updatePlaybackSpeed()
        self.updateLoopValue()
        self.updatePlaybackTime()

        if guiMgr.Playback_Active and False == self.Visible:
            self.root.grid()
            self.Visible=True
            self.slider.set(0)
            self.lstBoxRepeatMode.current(0)
            self.lstBoxPlaybackSpeed.current(4)

        if guiMgr.Live_Receiving and self.Visible:
            self.Visible = False
            guiMgr.ShowPlayback(False)
            self.root.grid_remove()


        if guiMgr.GetRepeatMode()[0] == Playback.RepeatMode.LOOP and False == self.LoopValuesVisible:
            self.LoopValuesVisible = True
            self.lblEndLoop.grid()
            self.lblStartLoop.grid()

        if guiMgr.GetRepeatMode()[0] != Playback.RepeatMode.LOOP and True == self.LoopValuesVisible:
            self.LoopValuesVisible = False
            self.lblEndLoop.grid_remove()
            self.lblStartLoop.grid_remove()

        if guiMgr.Playback_Playing and not (enabled(self.btnPausePlayback) or enabled(self.btnStopPlayback)):
            enable(self.btnPausePlayback)
            enable(self.btnStopPlayback)
            disable(self.btnStartPlayback)


        if not guiMgr.Playback_Playing and (enabled(self.btnPausePlayback) or enabled(self.btnStopPlayback)):
            disable(self.btnPausePlayback)
            disable(self.btnStopPlayback)
            enable(self.btnStartPlayback)

        if guiMgr.Playback_Playing and enabled(self.slider):
            disable(self.slider)

        if not guiMgr.Playback_Playing and not enabled(self.slider):
            enable(self.slider)

        currNum = playbackMgr.GetCurrentNumber()
        total = playbackMgr.GetDataCount()

        if guiMgr.Playback_Playing:
            enable(self.slider)
            self.slider.set(int(currNum*100 / total))
            disable(self.slider)

        elif currNum==total and self.slider.get() != 100:
            self.slider.set(100)

        if guiMgr.GetRepeatMode()[0] != Playback.RepeatMode.LOOP and enabled(self.btnStartLoop):
            disable(self.btnStartLoop)
            self.btnStartLoop.grid_remove()
            self.btnStopLoop.grid_remove()

        if guiMgr.Playback_Playing and enabled(self.btnStartLoop):
            disable(self.btnStartLoop)
            self.btnStartLoop.grid_remove()
            self.btnStopLoop.grid_remove()

        if not guiMgr.Playback_Playing and guiMgr.GetRepeatMode()[0] == Playback.RepeatMode.LOOP and not enabled(self.btnStartLoop):
            enable(self.btnStartLoop)
            self.btnStartLoop.grid()
            self.btnStopLoop.grid()

        if True == self.LoopValuesVisible:
            mode = guiMgr.GetRepeatMode()
            self.lblStartLoop.config(text=str(mode[1]))
            self.lblEndLoop.config(text=str(mode[2]))


class StatisticsView():
    def __init__(self,parent):
        #Downstream
        strType = "sunken"
        downstreamFrame = ttk.Frame(parent,borderwidth=5, relief="sunken")
        upstreamFrame = ttk.Frame(parent,borderwidth=5, relief="sunken")
        otherFrame = ttk.Frame(parent,borderwidth=5, relief="sunken")
        downstreamFrame.grid(row=1,column=1)
        upstreamFrame.grid(row=1,column=3)
        otherFrame.grid(row=2,column=1,columnspan=3)
        self._LastUpdate = 0
        self._Interval=500

        Label(downstreamFrame,text="Downstream Statistics",font=("Helvetica", 16)).grid(row=1,column=1,columnspan=3)
        self.CreateLabel(downstreamFrame,"Total Packets sent downstream",2,1)
        self.lblTotalPacketsDownstream = self.CreateStatLabel(downstreamFrame,2,3)
        self.CreateLabel(downstreamFrame,"Packets sent downstream",3,1)
        self.lblPacketDownstream = self.CreateStatLabel(downstreamFrame,3,3)
        self.CreateLabel(downstreamFrame,"Bytes Transmitted downstream",4,1)
        self.lblBytesTransmittedDownstream = self.CreateStatLabel(downstreamFrame,4,3)
        self.CreateLabel(downstreamFrame,"Bytes Received from downstream",5,1)
        self.lblBytesReceivedFromDownstream = self.CreateStatLabel(downstreamFrame,5,3)

        Label(upstreamFrame,text="Upstream Statistics",font=("Helvetica", 16)).grid(row=1,column=1,columnspan=3)
        self.CreateLabel(upstreamFrame,"Total Packets sent upstream",2,1)
        self.lblTotalPacketsUpstream = self.CreateStatLabel(upstreamFrame,2,3)
        self.CreateLabel(upstreamFrame,"Packets sent upstream",3,1)
        self.lblPacketUpstream = self.CreateStatLabel(upstreamFrame,3,3)
        self.CreateLabel(upstreamFrame,"Bytes Transmitted upstream",4,1)
        self.lblBytesTransmittedUpstream = self.CreateStatLabel(upstreamFrame,4,3)
        self.CreateLabel(upstreamFrame,"Bytes Received from  upstream",5,1)
        self.lblBytesReceivedFromUpstream = self.CreateStatLabel(upstreamFrame,5,3)

        Label(otherFrame,text="Other Statistics",font=("Helvetica", 16)).grid(row=1,column=1,columnspan=3)
        self.lblTotalDroppedPackets = self.CreateStatLabel(otherFrame,2,3)
        self.CreateLabel(otherFrame,"Total Downstream Packets Dropped",2,1)
        self.CreateLabel(otherFrame,"Malformed Packets Received",3,1)
        self.lblTotalMalformedPackets = self.CreateStatLabel(otherFrame,3,3)
        self.CreateLabel(otherFrame,"Total Chained Packets",4,1)
        self.lblTotalChainedPackets = self.CreateStatLabel(otherFrame,4,3)
        self.CreateLabel(otherFrame,"Total Oscar Tasks",5,1)
        self.lblTotalOscarTasks = self.CreateStatLabel(otherFrame,5,3)
        self.CreateLabel(otherFrame,"Total Minion Tasks",6,1)
        self.lblTotalMinionTasks = self.CreateStatLabel(otherFrame,6,3)
        self.CreateLabel(otherFrame,"Total Shunted Packets",7,1)
        self.lblTotalShuntedPackets = self.CreateStatLabel(otherFrame,7,3)


    def CreateLabel(self,root,strText,rowVal,columnVal):
        return Label(root,text=strText).grid(row=rowVal,column=columnVal,sticky=W)

    def CreateStatLabel(self,root,rowVal,columnVal,strText=None):
        if None==strText:
            strText="undefined"

        objLabel = Label(root,text=strText,width=30,bg="white",borderwidth=2,pady=5,padx=5,anchor=E,justify=RIGHT)
        objLabel.grid(row=rowVal,column=columnVal,sticky=E)
        return objLabel
                
    def updateGui(self):
        if self._LastUpdate + self._Interval < Time.GetCurrMS():
            self._LastUpdate = Time.GetCurrMS()
            sm = Statistics.GetStatistics()
            self.lblTotalPacketsDownstream.configure(text=str(sm._TotalPacketsDownstream))
            self.lblPacketDownstream.configure(text=str(sm._UniquePacketsDownstream))
            self.lblBytesTransmittedDownstream.configure(text=str(sm._totalTxBytesDownstream))
            self.lblBytesReceivedFromDownstream.configure(text=str(sm._totalRxBytesDownstream))

            self.lblTotalPacketsUpstream.configure(text=str(sm._TotalPacketsUpstream))
            self.lblPacketUpstream.configure(text=str(sm._UniquePacketsUpstream))
            self.lblBytesTransmittedUpstream.configure(text=str(sm._totalTxBytesUpstream))
            self.lblBytesReceivedFromUpstream.configure(text=str(sm._totalRxBytesUpstream))

            self.lblTotalDroppedPackets.configure(text=str(sm._TotalPacketsDropped))
            self.lblTotalMalformedPackets.configure(text=str(sm._TotalMalformedPacketsReceived))
            self.lblTotalChainedPackets.configure(text=str(sm._TotalChainedDownstreamPackets))
            self.lblTotalOscarTasks.configure(text=str(sm._TotalOscarTasksReceived))
            self.lblTotalMinionTasks.configure(text=str(sm._TotalMinionTasksReceived))
            self.lblTotalShuntedPackets.configure(text=str(sm._TotalShuntedPackets))

class MenuSystem():
    def __init__(self,parent):
        self.__KludgeCounter = 0
        self.parent = parent
        self.root = Menu(parent)
        self.SaveStr = "Save ..."
        self.SaveAsCSV = "Export to CSV..."
        self.ExportToDB = "Export to Database..."
        self.OpenStr = "Open..."

        self._PreviousFileMenuStatus = NORMAL

        self.FileMenu = Menu(self.root,tearoff=0)
        self.FileMenu.add_command(label = self.OpenStr,command=self.HandleOpen)
        self.FileMenu.add_command(label = self.SaveStr,command=self.HandleSave,state=DISABLED)
        self.FileMenu.add_command(label = self.SaveAsCSV,command=self.HandleSaveAsCSV)
        self.FileMenu.add_command(label = self.ExportToDB,command=self.HandleExportToDB)
        self.FileMenu.add_separator()
        self.FileMenu.add_command(label = "Exit",command=self.HandleExit)
        self.root.add_cascade(label="File",menu=self.FileMenu)

        HelpMenu = Menu(self.root,tearoff=0)
        HelpMenu.add_command(label="Help",command=self.HandleHelp)
        HelpMenu.add_separator()
        HelpMenu.add_command(label="About",command=self.HandleAbout)
        HelpMenu.add_command(label="Send Refresh",command=self.HandleRefresh)
        self.root.add_cascade(label="Help",menu=HelpMenu)

        self.parent.protocol("WM_DELETE_WINDOW",self.HandleExit)

    def get(self):
        return self.root

    def HandleOpen(self):
        options = {}
        options['filetypes'] = [('Oscar Data files', '.biff')]
        options['parent'] = self.root
       
        filename = tkinter.filedialog.askopenfilename(**options)
        if len(filename) < 1 :
            return

        if not GuiMgr.ReadFromFile(filename):
               GuiMgr.MessageBox_Error("Python Error","Error loading file: " + filename)

        self._PreviousFileMenuStatus = None
        GuiMgr.SetPlaybackFilename(filename)
        #GuiMgr.SetTitle("- {"+filename+"}")


    def HandleSave(self):
        options = {}
        options['filetypes'] = [('Oscar Data files', '.biff')]
        options['initialfile'] = 'OscarSaveFile.biff'
        options['defaultextension'] = '.biff'
        options['parent'] = self.root
       
        filename = tkinter.filedialog.asksaveasfilename(**options)
        if len(filename) < 1 :
            return

        GuiMgr.WriteToFile(filename)
        GuiMgr.SetPlaybackFilename(filename)
        #GuiMgr.SetTitle("- {"+filename+"}")

    def HandleSaveAsCSV(self):
        Interval = tkinter.simpledialog.askinteger("Interval","What time interval rate (seconds) to save data at?",parent=self.root,minvalue=1,initialvalue=1)
        if None == Interval:
            return

        options = {}
        options['filetypes'] = [('Comma Separated Value File', '.csv')]
        strInitFile = "oscar"
        pbFilename = GuiMgr.GetPlaybackFilename()
        if pbFilename != None and len(pbFilename) > 1:
            try:
                strInitFile, ext = os.path.splitext(ntpath.basename(pbFilename))
            except:
                pass
         
        options['initialfile'] = strInitFile
        options['defaultextension'] = '.csv'
        options['parent'] = self.root
       
        filename = tkinter.filedialog.asksaveasfilename(**options)
        if len(filename) < 1 :
            return

        GuiMgr.WriteCSVFile(filename,Interval)

    def HandleExportToDB(self):
        GuiMgr.MessageBox_Info("Export to Database","Grimock does not currently support this.\nPlease provide feedback to Patrick on what DB\nsupport you would like.")
        
    def HandleAbout(self):
        GuiMgr.MessageBox_Info("About Oscar","Oscar is the BIFF project\ndata collection and recording module.\n\n" + VersionMgr.ReadVer())

    def HandleRefresh(self):
        TargetManager.GetTargetManager().DebugRefresh()

    def HandleHelp(self):
        GuiMgr.MessageBox_Info("Help","Our prime purpose in this life is to help others.\nAnd if you can't help them, at least don't hurt them.\n\n - Dalai Lama")

    def HandleExit(self):
        if not Recorder.get().HasBeenSaved() and Recorder.get().GetRecordedCount()>0:
            response = GuiMgr.MessageBox_OkCancel("Warning","You have not saved the current recorded data.  OK to Discard?")
            if False == response:
               return
        GuiMgr.Quit()

    def updateGui(self):
        gm = GuiMgr.get()

        if gm.Live_Receiving or gm.Playback_Playing:
            val = DISABLED
        else:
            val = NORMAL

        if self._PreviousFileMenuStatus != val:  #will kill performance if you do this every loop!
            self.FileMenu.entryconfigure(self.SaveAsCSV,state=val)        
            self.FileMenu.entryconfigure(self.SaveStr,state=val)  
            self.FileMenu.entryconfigure(self.OpenStr,state=val)  
            self.FileMenu.entryconfigure(self.ExportToDB,state=val)  
            self._PreviousFileMenuStatus = val
            self.__KludgeCounter = 0

            if val == NORMAL:
                if Playback.get().GetDataCount() < 1:
                    self.FileMenu.entryconfigure(self.SaveStr,state=DISABLED)  
                    self.FileMenu.entryconfigure(self.SaveAsCSV,state=DISABLED)
                    self.FileMenu.entryconfigure(self.ExportToDB,state=DISABLED)  

        else:
            self.__KludgeCounter += 1           # easy to get out of sync if stopped live, but no data, etc.  Quick hack
            if self.__KludgeCounter > 100:
                self._PreviousFileMenuStatus = None
                self.__KludgeCounter = 0       




