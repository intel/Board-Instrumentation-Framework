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
#    Playback Data Hander - deals with all incoming data from Minion
#
##############################################################################
from Helpers import  Log
import pickle
import sys
from Helpers import Log
from Data import MarvinData
from Data import MarvinGroupData
from Util import Time
from Util import Sleep
from Util import Utility
from Helpers import ThreadManager
from Helpers import TargetManager
from Helpers import Entry
from Helpers import Configuration
import collections
import datetime

class RepeatMode():
    NONE=0
    REPEAT=1
    LOOP=2
    Unknown = 20

    @staticmethod
    def toString(mode):
        if mode == RepeatMode.NONE:
            return "NONE"
        if mode == RepeatMode.REPEAT:
            return "REPEAT"
        if mode == RepeatMode.LOOP:
            return "LOOP"

        return "Unknown"
        
def get():
    return Playback._get()


class Playback(object):
    _instance = None

    def __init__(self):
        if None != Playback._instance:
            return

        Playback._instance = self

        self.PlaybackData=[]
        self.LoopCount = 1
        self.PlaybackSpeed=1
        self.CurrentIndex=0
        self.LoopMode = RepeatMode.NONE
        self.Stopped=True
        self.Paused=False
        self.StartTime=None
        self.threadName = "PlaybackObject" + ":" + str(self)
        self.IndexExternallySet=False

        self.startIndex=0
        self.endIndex = None

        ThreadManager.GetThreadManager().CreateThread(self.threadName,self.__workerProc)
        ThreadManager.GetThreadManager().StartThread(self.threadName)

    @staticmethod
    def _get():
        if None == Playback._instance:
            Playback._instance = Playback()
        return Playback._instance

    def Clear(self):
        if None != self.PlaybackData:
            self.PlaybackData.clear()

    def SetData(self,dataList):
        if False==self.Stopped:
            Log.getLogger().error("Setting Playback data, but playback has not been stopped")
            return

        self.PlaybackData = dataList
        self.LoopMode=RepeatMode.NONE
        self.LoopCount=1
        self.StartTime=None
        self.CurrentIndex=0
        self.startIndex=0
        self.endIndex=len(dataList)

        NamespaceMap={}
        self.NamespaceCount = 0
        self.ID_Count = 0
        # go get some stats on the loaded data
        for entry in dataList:
            if not entry.Namespace in NamespaceMap:
                NamespaceMap[entry.Namespace]={}
                self.NamespaceCount += 1

            if hasattr(entry,'_DataList'):
                for subEntry in entry._DataList:
                    if not subEntry.ID in NamespaceMap[entry.Namespace]:
                        NamespaceMap[entry.Namespace][subEntry.ID]=subEntry.ID
                        self.ID_Count +=1

            elif not entry.ID in NamespaceMap[entry.Namespace]:
                NamespaceMap[entry.Namespace][entry.ID]=entry.ID
                self.ID_Count += 1

    def GetDataCount(self):
        return len(self.PlaybackData)

    def GetCountNamespace(self):
        return self.NamespaceCount

    def GetCountID(self):
        return self.ID_Count

    def GetPlayTime(self):
        try:
            return self.PlaybackData[-1].ArrivalTime - self.PlaybackData[0].ArrivalTime
        except:
            return 0

    def GetCurrentNumber(self):
        return self.CurrentIndex

    def GetCurrentPlaybackTime(self):
        try:
            return  self.PlaybackData[self.CurrentIndex].ArrivalTime - self.PlaybackData[0].ArrivalTime
        except Exception as Ex:
            return 0

    def GetLoopCount(self):
        return self.LoopCount

    def GetPlaybackSpeed(self):
        return self.PlaybackSpeed

    def SetCurrentNumber(self,newNumber):
        if Utility.IsNumeric(newNumber) and newNumber> -1 and newNumber < self.GetDataCount():
            self.CurrentIndex = newNumber
            self.IndexExternallySet = True

        else:
            Log.getLogger().warn("Tried to set playback index to invalid value: " + str(newNumber))

    def SetPlaybackSpeed(self,newVal):
        if False == Utility.IsNumeric(newVal):
            Log.getLogger().warn("Tried to set playback speed to invalid value: " + str(newVal))
            return

        if newVal > 0 and newVal < 100:
            self.PlaybackSpeed = newVal
        else:
            Log.getLogger().warn("Tried to set playback speed to invalid value: " + str(newVal))

    def SetLoopMode(self, newMode,startIndex=0,endIndex=None):
        self.LoopMode = newMode
        self.startIndex=startIndex
        if None == endIndex or endIndex > len(self.PlaybackData) -1:
            endIndex = len(self.PlaybackData) -1
        
        self.endIndex = endIndex

    def GetLoopMode(self):
        if None == self.endIndex or self.endIndex > self.GetDataCount()-1:
            self.endIndex = self.GetDataCount() - 1

        return (self.LoopMode,self.startIndex,self.endIndex)

    def Stop(self):
        self.Stopped = True
        if self.Paused == True:
            self.Paused = False

    def Start(self):
        if len(self.PlaybackData) == 0:
            Log.getLogger().error("Tried to play empty playback data.")
            return

        if None == self.endIndex or self.endIndex > len(self.PlaybackData)-1:
            self.endIndex = len(self.PlaybackData)-1

        if True == self.Stopped:

            if self.startIndex > self.endIndex:
                Log.getLogger().error("Start Index > End Index for Playback, ignoring")
                self.startIndex = 0
                self.endIndex = len(self.PlaybackData)-1
                return

            if False == self.IndexExternallySet:
                self.CurrentIndex=self.startIndex

            self.StartTime = None #let worker calculate
            self.IndexExternallySet = False
            self.Stopped = False

        elif True == self.Paused:
            self.Paused = False

    def Pause(self):
        if False == self.Paused:
            self.Paused = True

    def __workerProc(self,fnKillSignalled,userData):
        from Helpers import GuiMgr

        self.CurrentIndex = self.startIndex
        self.StartTime = None

        xmlList=[]
        
        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            if self.Paused or self.Stopped:
                Sleep.SleepMs(100)
                continue

            if None == self.StartTime:
                self.StartTime = int(self.PlaybackData[self.CurrentIndex].ArrivalTime) - 10 # can't remember why I subract 10ms...

            objData = self.PlaybackData[self.CurrentIndex]
            sleepVal = (int(objData.ArrivalTime)-self.StartTime)/self.PlaybackSpeed
            Sleep.SleepMs(sleepVal)

            try: # when looping, this data will be here after the 1st loop
                xmlData = self.PlaybackData[self.CurrentIndex].xmlData
                node = self.PlaybackData[self.CurrentIndex].firstNode
            except:
                xmlData = objData.ToXML() # be more efficient if I create a list of already created xmldata
                self.PlaybackData[self.CurrentIndex].xmlData = xmlData # LOVe how you can just add stuff to an object in Python!
                if Configuration.get().GetShunting():
                    try:
                        dom = xml.dom.minidom.parseString(rawData)
                        node = dom._get_firstChild()
                    except Exception as ex:
                       Log.getLogger().error("Error Something bad in Trying to re-encode saved data" )

                else:
                   node = None

                self.PlaybackData[self.CurrentIndex].firstNode = node

            TargetManager.GetTargetManager().BroadcastDownstream(xmlData,False,node)
            GuiMgr.OnDataPacketSentDownstream(objData,"Playback")

            try:
                self.StartTime = int(self.PlaybackData[self.CurrentIndex].ArrivalTime)
            except Exception:
                self.StartTime = None # just in case get an out of bounds error

            self.CurrentIndex+=1

            if None == self.endIndex:
                self.endIndex = len(self.PlaybackData)-1

            if self.CurrentIndex >= self.endIndex:
                preProcessingDone = True
                if self.LoopMode == RepeatMode.NONE:
                    GuiMgr.OnStopPlayback()
                    if Configuration.get().GetExitAfterAutoPlay():
                        Log.getLogger().info("Playback finished, exiting application per arguments")
                        GuiMgr.Quit()

                elif self.LoopMode == RepeatMode.REPEAT:
                    self.CurrentIndex = 0
                    self.LoopCount += 1

                elif self.LoopMode == RepeatMode.LOOP:
                    self.CurrentIndex = self.startIndex
                    self.LoopCount += 1

                self.StartTime = None

    def WriteToFile(self,filename):
        with open(filename,'w+b') as fp:
           pickle.dump(self.PlaybackData, fp, pickle.DEFAULT_PROTOCOL)

        from Helpers import Recorder

        if not Recorder.get().HasBeenSaved():
            Recorder.get().OnSaved()

    def ReadFromFile(self,filename):
        from Helpers import GuiMgr
        
        import os.path
        if not os.path.isfile(filename):
            Log.getLogger().error("Asked to read from non-existant file: " + filename)
            return False
        with open(filename,'rb') as fp:
            try:
                entries = pickle.load(fp)
                
            except Exception as ex:
                Log.getLogger().error(filename+": " + str(ex))
                GuiMgr.MessageBox_Error("Error Loading File","The format of this file requires Python 3.4 or greater.")
                return False

            if len(entries) < 1:
                Log.getLogger().error(filename+" does not appear to have any data in it. ")
                return False 

            objEntry = entries[0]

            if hasattr(objEntry,'FormatVersion'):
                entries=Configuration.get().HandleBITW_FromLoad(entries)
                self.SetData(entries)
                return True

            elif hasattr(objEntry,'PacketNumber'):
                newList = self.ConvertFromAlphaFormat(entries)

            else :
                newList = self.ConvertFromBetaFormat(entries)

            if None != newList:
                self.SetData(newList)
                return True

            return False

    def ConvertFromAlphaFormat(self,entries):
        newList = []
        try:
            for objEntry in entries:
                newList.append(MarvinData.MarvinData(objEntry.Namespace,objEntry.Entity,objEntry.Value,objEntry.ArrivalTime,"1.0",False))
        except Exception  as ex:
            Log.getLogger().error("Unable to convert Alpha formatted file.")
            Log.getLogger().error(str(ex))
            #GuiMgr.MessageBox_Error("Python Error","Unable to convert old file format.")
            return none

        return newList

    def ConvertFromBetaFormat(self,entries):
        newList = []

        try:
            for objEntry in entries:
                newList.append(MarvinData.MarvinData(objEntry.Namespace,objEntry.ID,objEntry.Value,objEntry.ArrivalTime,"1.0",False))

        except Exception  as ex:
            Log.getLogger().error("Unable to convert Beta formatted file.")
            Log.getLogger().error(str(ex))
            #GuiMgr.MessageBox_Error("Python Error","Unable to convert old file format.")
            return none

        return newList

    def __IsNumeric(self,value):
        try:
            val = float(value)
            return True
        except ValueError:
            return False

    def AddToMapList(self,map,node):
        key = node.Namespace + node.ID
        if len(key) > 1:
            try:
                list = map[key]
                list.append(node)
            except Exception: #didn't exist yet
                list = []
                list.append(node)
                map[key] = list
            return

        elif isinstance(node,MarvinGroupData.MarvinDataGroup):
            for datapoint in node._DataList:
                self.AddToMapList(map,datapoint)
        else:
            Log.getLogger().error("Unknown item when trying to save to CSV: " + str(node))


    def WriteCSVFile(self,filename,interval):
        if interval == 0:
            return self.WriteRawCSVFile(filename)

        dict = {}
        timeInterval = interval * 1000 #secs to ms

        entries = self.PlaybackData

        with open(filename,'w+t') as fp:
            fp.write("Namespace,Name,TimeInterval(secs),# of Datapoints, Total Time(secs), Avg Time (ms),# of samples,Average Value,Values...\n")


            for Node in entries: # create a dictionary of each namespace+entity, then a list of values for each
                self.AddToMapList(dict,Node)

            #Sort the dictionary so it looks better in the file
            sortedDictionary = collections.OrderedDict(sorted(dict.items()))
            
            for key in sortedDictionary:
                list = sortedDictionary[key]
                
                count = 0
                tValue=0
                startTime=0
                newList = []
                #newList.append(list[0]) # use 1st entry
                startTime = list[0].ArrivalTime
                timespan = list[-1].ArrivalTime - startTime #  
                
                fp.write(list[0].Namespace + "," + list[0].ID + ","+ str(interval) + ",")
                fp.write(str(len(list)) + "," + str(int(timespan/1000)) + "," + str(int(timespan/len(list))))

                timespan = 0

                for Node in list:
                    timespan += Node.ArrivalTime - startTime

                    if True == self.__IsNumeric(Node.Value) :
                        tValue += float(Node.Value)
                        count += 1.0

                        if timespan >= timeInterval : 
                            avgValue = tValue / count
                            NewNode = Entry.Entry(Node.ID,avgValue,Node.Namespace,'0')

                    else : # is string data of some kind
                        if timespan >= timeInterval : 
                            NewNode = Entry.Entry(Node.ID,Node.Value,Node.Namespace,'0')
                            newList.append(NewNode)

                    if timespan  >= timeInterval : 
                        newList.append(NewNode)
                        #interval and data arrival don't match, so by doing this we average out better
                        startTime = Node.ArrivalTime - (timespan-timeInterval)

                        count = 0
                        tValue = 0
                        timespan = 0

                    else:
                      startTime = Node.ArrivalTime

                if len(newList):
                    fp.write("," + str(len(newList))) # Write the count
                
                    if newList != None and self.__IsNumeric(newList[0].Value):
                        tVal = 0
                        for Node in newList:
                            tVal += float(Node.Value)

                        if tVal !=0:
                            avgVal = tVal/len(newList)
                        else:
                            avgVal = 0

                        fp.write(","+ "{0:.2f}".format(avgVal)) # write the average

                        for Node in newList:
                            fp.write("," + "{0:.2f}".format(float(Node.Value))) #write the data
                
                    else:
                        fp.write(",NA") #no Average for text
                        for Node in newList:
                            fp.write("," + Node.Value) #write the data - it is a string

                else:
                    pass  # sometimes get here, though I don't know why

                fp.write("\n")            

        
    def AddRawEntryToDict(self,targetDict,entry):
        if isinstance(entry,MarvinGroupData.MarvinDataGroup):
            for datapoint in entry._DataList:
                self.AddRawEntryToDict(targetDict,datapoint)
            return

        if not entry.Namespace in targetDict:
            targetDict[entry.Namespace] = {} 

        entriesDict = targetDict[entry.Namespace]
        if not entry.ID in entriesDict:
            entriesDict[entry.ID]= []
            entry.timeDelta=0
            entriesDict[entry.ID].append(entry)

        else:
            t1 = entry.ArrivalTime
            t2 = entriesDict[entry.ID][-1].ArrivalTime
            entry.timeDelta = t1-t2
            entriesDict[entry.ID].append(entry)

    def WriteRawCSVFile(self,filename):
            namespaceDict = {}

            entries = self.PlaybackData
            firstPktTime = Time.GetCurrMS()

            for entry in entries:
                self.AddRawEntryToDict(namespaceDict,entry)
                if entry.ArrivalTime < firstPktTime:
                    firstPktTime = entry.ArrivalTime

            sortedNamespaceDictionary = collections.OrderedDict(sorted(namespaceDict.items()))

            writeData=[]
            writeData.append( "BIFF CSV File.  Created {0}.  Time(ms) is the # of ms data value received after the 1st value (so a relative time).".format(datetime.datetime.now().strftime("%I:%M%p on %B %d, %Y")))
            writeData.append( "Namespace,,")
            ROW_NS=1
            writeData.append("ID,,")
            ROW_ID=2
            writeData.append(",,") #Samples,Average Header Line
            ROW_AVG_HDR = 3
            writeData.append(",,") #Samples,Average Line
            ROW_AVG = 4
            writeData.append(",,") #Values, Time Header Line
            ROW_DATA_HDR=5
            startRow = len(writeData) 
            line = ""
            maxDp = 0

            #go through each NS, making 1st two rows, keep track of longest ID set, and append that many lines

            for namespace in sortedNamespaceDictionary:
                nsDict = sortedNamespaceDictionary[namespace]
                for id in nsDict:
                    if len(nsDict[id]) > maxDp:
                        maxDp = len(nsDict[id])

            for i in range(0,maxDp+1):
                writeData.append(",,")

            rows = len(writeData)


            for namespace in sortedNamespaceDictionary:
                nsDict = sortedNamespaceDictionary[namespace]
                sortedIDDictionary = collections.OrderedDict(sorted(nsDict.items()))

                for ID in sortedIDDictionary:
                    DataSet = sortedIDDictionary[ID]
                    writeData[ROW_NS] += namespace + ",,,"
                    writeData[ROW_ID] += ID + ",,,"
                    writeData[ROW_AVG_HDR] += "Average,#Samples,,"
                    writeData[ROW_DATA_HDR] += "Values,Time(ms),,"
                    row = startRow
                    
                    tValue=0.0
                    nonNumeric=False

                    for entry in DataSet:
                        rowData = writeData[row]
                        if "," in entry.Value:
                            writeData[row] += str(entry.Value.replace(",",";") +",")
                        else:
                            writeData[row] += str(entry.Value) +"," 
                          
                        writeData[row] += str(entry.ArrivalTime - firstPktTime) +",,"

                        row += 1
                        if not nonNumeric:
                            try:
                                tValue += float(entry.Value)
                            except Exception:
                                nonNumeric = True


                    if nonNumeric:
                        writeData[ROW_AVG] += "NA,"
                    else:
                        writeData[ROW_AVG] += "{0},".format(tValue/len(DataSet))

                    writeData[ROW_AVG] += str(len(DataSet)) +",,"


                    if len(DataSet) < maxDp:
                        for row in range(startRow+len(DataSet),startRow + maxDp):
                            writeData[row] += ",,,"

            
            with open(filename,'w+t') as fp:
                for line in writeData:
                    fp.write(line + "\n")
                
            








