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
#    This is the main file for the Minion data collector 
#
##############################################################################

from Helpers import Log
from Util import Sleep
from Util import Time
from Util import Utility
from Helpers import ThreadManager
from Helpers import Worker
from Helpers import Configuration

class BoundAction():
    Invalid = 0
    Drop = 1
    Set  = 2
    RepeatLast = 3

# Helper class to hold the information of the script or app it will call to
# gather some kind of data
class Collector:
    __InitialMagicValue = -3232
    __SleepInterval = 100
    ErrorValue = "ErrorCallingCollector"
    def __init__(self,objNamespace,ID,InGroup=False):
        self._MinionID = ID
        self._OverrideID = ID
        self._ScriptName = ''
        self._ScriptActive = False
        self._Normalize = False
        self._NormalizeValue = 0
        self._SendOnlyOnDelta = False
        self._SentValueCount = 0
        self._Parameters = []
        self._kwargs=None
        self._LastCollectionTime = 0
        self._LastValue = Collector.__InitialMagicValue  #Magic number - indicating not initialized
        self._LastSentValue = Collector.__InitialMagicValue
        self._RefreshRequested = False
        self._PollingInterval = 0
        self._DoNotSend = False
        self._NamespaceObject = objNamespace
        self._Name = str(objNamespace) + ":" + ID
        self._RunOnce = False
        self._InGroup = InGroup
        self._OnDemand = False
        self.LastUniqueID = 323232 # used for On-Demand, to make sure repeated requests are dropped
        self.IsDynamicallyCreated = False  # for dynamicall created (DynamicCollector)
        self.DynamicValue = self._LastValue # for dynamicall created (DynamicCollector)
        self.DynamicCollectorParent=None
        self.DynamicValueCollectedElapsedTime=0
        self.DynamicValueCollected = False
        self.ScaleValue = 1
        self.Precision = objNamespace.getDefaultPrecision()
        self._DebugNum = 0
        self._LastElapsedTimePeriod=0
        self._SyncFile = None   # use file timestamp changes for normilization
        self._ProcessThreadID = 'Default' # For mulit-threaded processing
        self._Bound_Max=None
        self._Bound_Min=None
        self._Bound_Action=BoundAction.Invalid
        self._ReadyForConsumptionByAnother = False
        self._NamespaceOverride=None

    def SetOverrideNamespaceString(self,newNamespaceString):
        self._NamespaceOverride = newNamespaceString
        
        for ns in Configuration.GetNamespaces():
            if ns.GetID().lower() == newNamespaceString.lower():
                Log.getLogger().warning("Setting OverrideNamespace for collector {0} to {1}.  However that Namespace already exists - conflicts may occur.".format(newNamespaceString,self.GetID()))
                break

    def GetLastValue(self):
        return str(self._LastSentValue)

    def ReadyForConsumption(self):
        return self._ReadyForConsumptionByAnother

    def EnableForConsumption(self):
        if self._LastValue != Collector.__InitialMagicValue:
            self._ReadyForConsumptionByAnother = True

    def SetProcessThreadID(self,ThreadID):
        self._ProcessThreadID = ThreadID

    def GetProcessThreadID(self):
        return self._ProcessThreadID

    def SetDynamicData(self,newData,elapsedTime):
        self.DynamicValue = newData
        self.DynamicValueCollectedElapsedTime = elapsedTime
        self.DynamicValueCollected = False

    def SetOverrideID(self, newID):
        if len(newID) > 0:
            self._OverrideID = newID
            Log.getLogger().info("Setting OverrideID for " + self.GetID() + " to: " + self.GetTransmitID())
            return True

        Log.getLogger().error("Empty OverrideID specified.")
        return false

    def GetTransmitID(self):
        return self._OverrideID

    def GetID(self):
        return self._MinionID 

    def SetScaleValue(self,newVal):
        try:
            self.ScaleValue = float(newVal)
            if self.ScaleValue != 1.0:
                Log.getLogger().info("Setting Scale value for " + self.GetID() + " to: " + str(newVal))
        except Exception:
            Log.getLogger().error("Invalid Scale value: " + newVal)
            return False

        self.ScaleValue = float(newVal)

        return True

    def IsOnDemand(self):
        return self._OnDemand

    def IsInGroup(self):
        return self._InGroup

    def RequestRefresh(self):
        self._RefreshRequested = True
        self._SentValueCount = 0

    def BeginCollecting(self,runOnce):
        ThreadManager.GetThreadManager().CreateThread(self._Name,self.__collectionProc)
        ThreadManager.GetThreadManager().StartThread(self._Name)

    # prints all the info about this collector
    def GetInfo(self):
        retString = "ID: " + self.DisplayID + " -->script: " + self.ScriptName
        for param in self.Parameters:
            retString = retString + " [" + param + "]"

        return retString

    def __AssignPrecisionAndScale(self,value,skipScale=False):
        try:
            if False == skipScale:
                scaleVal = self.ScaleValue
            else:
                scaleVal = 1.0

            fValue = float(value) * float(scaleVal)
            
            return str(format(fValue,'.' + str(int(self.Precision)) + 'f'))
        except Exception as Ex:
            return value


    def __Scale(self,value):
        try:
            float(value)
            try:
                return self.__AssignPrecisionAndScale(value)
            except Exception as Ex:
                Log.getLogger().warning("Collector: " + self.GetID() +" tried to scale : " + value)
                return value
        except:
            pass
        #is an array
        newValue = []
        try :
            valList = value.split(",") #split at commas
        except:
            return value # wast not a float, nor an array, so could be an operator for a dynamic collector or dynamic collector with no data yet

        for item in valList:
            try:
                newValue.append(self.__AssignPrecisionAndScale(item))
            except:
                #Log.getLogger().warning("tried to scale : " + value)
                return value
        return ",".join(newValue)

    def __BoundData(self,sendValue):
#        self._Bound_Max=None
#        self._Bound_Min=None
#        self._Bound_Action=BoundAction.Invalid

        if None == self._Bound_Max and None == self._Bound_Min:
            return sendValue
        
        try:
            value = float(sendValue)
        except Exception:
            Log.getLogger().warning("Collector [" + self.GetID() + "] tried to perform data bounding, but data is not numeric.")
            return sendValue

        if None != self._Bound_Min and value < self._Bound_Min:
            if self._Bound_Action == BoundAction.Set:
                returnVal =  self._Bound_Min
            elif self._Bound_Action == BoundAction.RepeatLast:
                returnVal =  self._LastSentValue
            else:
                returnVal =  None

        elif None != self._Bound_Max and value > self._Bound_Max:
            if self._Bound_Action == BoundAction.Set:
                returnVal =  self._Bound_Max
            elif self._Bound_Action == BoundAction.RepeatLast:
                returnVal =  self._LastSentValue
            else:
                returnVal =  None

        else:
            returnVal = sendValue

        returnVal = self.__AssignPrecisionAndScale(returnVal,True)
        return returnVal

    def __NormalizeData(self,newValue,timeDelta):
        if False == self._Normalize:  # if not normalizing, then get out of here!
            dataRateWithNormFactor = newValue
            
        else:
            if self._LastValue == Collector.__InitialMagicValue: #special tag - nothing to compare to
                return self.__Scale(newValue)

            try:
                valueDelta = float(newValue) - float(self._LastValue)
            
            except Exception:
                return self.__Scale(self.__NormalizeArray(newValue,timeDelta)) #tried to normalize non-int data, so assume it's a comma separated array and
                                                                               #go try to normalize it

            if 0 == self._NormalizeValue: # special case, just return the absolute delta
                dataRateWithNormFactor = newValue

            else:
                dataRatePerSec = float(float(valueDelta) / float(float(timeDelta) / 1000.0)) # normalize to Per/sec
                dataRateWithNormFactor = dataRatePerSec * float(self._NormalizeValue)
    
        return str(self.__Scale(dataRateWithNormFactor))

    #try to normalize a comma separated array.  if it doesn't work, just send
    #back the orginial string
    def __NormalizeArray(self,newValue,timeDelta):
        splitData = newValue.split(",")
        oldData = self._LastValue.split(",")
        index = 0
        retString = None

        if len(splitData) == 0:  #wasn't comma separated data
            return newValue

        for dataPoint in splitData:
            try:
                valueDelta = float(dataPoint) - float(oldData[index])
            except Exception:
                return newValue #tried to normalize non-int data, so just return what we got and forget it

            dataRatePerSec = float(float(valueDelta) / float(float(timeDelta) / 1000.0))
            dataRateWithNormFactor = dataRatePerSec * float(self._NormalizeValue)
            index+=1

            if None == retString:
                retString = str(dataRateWithNormFactor)
            else:
                retString += "," + str(dataRateWithNormFactor)

        return retString #should be a comma separated normalized data

    # Creates the packaged up buffer, but not the UTF-8 header block
    def __CreateSendBuffer(self,value,elapsedtime,normalized):
        if None == value:  #whoa, this should not happen
            Log.getLogger().error("Asked to send a non existant value. ID=" + self.GetID())
            return None
        if len(str(value)) == 0:
            Log.getLogger().warn("Collector [" + self.GetID() +"] returned empty string.  Dropping.")
            return None
        if None == self._NamespaceOverride:
            namespaceStr = str(self._NamespaceObject)
        else:
            namespaceStr = self._NamespaceOverride
        buffer = ""
        buffer = buffer + "<Minion Type=\"Data\">"
        buffer = buffer + "<Version>1</Version>"
        buffer = buffer + "<PacketNumber>" + str(self._NamespaceObject.getNextPacketNumber()) + "</PacketNumber>"
        buffer = buffer + "<Namespace>" + namespaceStr + "</Namespace>"
        buffer = buffer + "<ID>" + self.GetTransmitID() + "</ID>"
        buffer = buffer + "<Value>" + value + "</Value>"
        buffer = buffer + "<Normalized>" + str(normalized) + "</Normalized>"
        buffer = buffer + "<ElapsedTime>" + str(elapsedtime) + "</ElapsedTime>"
        buffer = buffer + "</Minion>"

        return buffer
        
    def _VerifyParams(self,paramList):
        for param in paramList:
            if str(param) == "Invalid input collector ID":
                # is a Collector as an input,and it is invalid, or not yet created
                return False
        return True

    def Collect(self):
        if False == self.IsDynamicallyCreated:  #Dynamic collectors have their value collected from something else
            try:
                if not self._VerifyParams(self._Parameters):
                    return None  

                collectedValue = Worker.Worker.RunScript(self._ScriptName,self._Parameters)
                return str(collectedValue)
            except Exception as Ex:
                Log.getLogger().error("Error Calling: " + self._ScriptName + ": " + str(Ex))
                return Collector.ErrorValue

        #print(self._DebugNum)
        self.DynamicValueCollected=True

        return self.DynamicValue # set from outside here

    def NeedsCollecting(self):
        refresh = self._RefreshRequested
        retVal = self._LastCollectionTime + self._PollingInterval < Time.GetCurrMS() or True == refresh
        #a = self.GetID()
        #b = self._RunOnce
        if self._RunOnce and self._LastCollectionTime > 0:
            retVal = False

        if self.IsDynamicallyCreated and self.DynamicValueCollected:
            retVal = False # Don't collect if it is a dynamic collector that hasn't been updated since last collect

        return retVal

    def GetTimeMS(self):
        if None == self._SyncFile:
            return Time.GetCurrMS()

        return Time.GetFileTimestampMS(self._SyncFile)


    def GetElapsedTimeForDynamicWidget(self):
        if None == self._SyncFile:
            elapsedTime = self.DynamicValueCollectedElapsedTime

        else:
            elapsedTime = self.GetTimeMS() - self._LastCollectionTime 

        return elapsedTime

    #Use a function here rather than _LastCollectionTime (that is used in NeedsCollecting()) because
    #an Operator might need to verride the elapsed time to match that of a dynamic collector input...
    def GetElapsedTimeSinceLast(self):
        elapsedTime = self.GetTimeMS() - self._LastCollectionTime
        return elapsedTime

    def SetLastCollectionTime(self,timeVal):
        self._LastElapsedTimePeriod = timeVal - self._LastCollectionTime
        self._LastCollectionTime = timeVal 

    def SetLastCollectionTimeForDynamicWidget(self,timeVal):
        self._LastCollectionTime = self.DynamicCollectorParent._LastCollectionTime
        self._LastElapsedTimePeriod = self.DynamicCollectorParent._LastElapsedTimePeriod

    def GetLastElapsedTimePeriod(self):
        return self._LastElapsedTimePeriod

    def PerformCollection(self):
        from Helpers import Configuration # circular import if I do at top of file

        returnVal = None
        collectedValue = self.Collect()

        #Get collected time after collection, can't be sure each collection
        #take same amount of time
        elapsedTime = self.GetElapsedTimeSinceLast()
        self.SetLastCollectionTime(self.GetTimeMS())

        if collectedValue != Collector.ErrorValue and elapsedTime > 0:
            #Have collected data, now normalize, check bounds and send
            sendValue = self.__NormalizeData(collectedValue,elapsedTime)

            if self._Bound_Action != BoundAction.Invalid:
                sendValue = self.__BoundData(sendValue)
                if None == sendValue and self.ReadyForConsumption():  
                    sendValue = "HelenKeller" # outside bounds, and has run through whole process once

            refresh = self._RefreshRequested

            # Some checking for SendOnlyOnDelta, since this is UDP
            # going to send it n times just to make sure it gets there
            if collectedValue == self._LastValue:
                if self._SendOnlyOnDelta:
                    if self._SentValueCount < Configuration.GetTimesToRepeatPacket():
                        refresh = True

            else:
                self._SentValueCount = 0

            if collectedValue == self._LastValue and self._SendOnlyOnDelta and not refresh:  # nothing changed, and only want on change
                pass

            elif not self._DoNotSend and sendValue != "HelenKeller": # HelenKeller means it is a mute collector, so don't send the actual data
                if True == self._Normalize and self._LastValue == Collector.__InitialMagicValue:
                    pass # skip this piece of data - it is normalized, but we have no previous data point to normalize against.  If we don't skip, big jump on 1st datapoint in widgets
                else:
                    returnVal = self.__CreateSendBuffer(sendValue,elapsedTime,sendValue != collectedValue)
                self._SentValueCount += 1

            if True == self._Normalize and self._LastValue == Collector.__InitialMagicValue:
                pass # skip this piece of data - it is normalized, but we have no previous data point to normalize against.  If we don't skip, big jump on 1st datapoint in widgets
            else:
                self._LastSentValue = sendValue  # if if don't send, update this because operators could use this value

            self._LastValue = collectedValue
            

            if not self.ReadyForConsumption():
                self.EnableForConsumption()


            self.HandleAnyExtraStuff()

            if True == refresh:
                self._RefreshRequested = False

        return returnVal


    def HandleAnyExtraStuff(self):
        try:
            if hasattr(self, "MaxCollectedValue"):
                if Utility.IsNumeric(self._LastValue):
                    if Utility.IsNumeric(self.MaxCollectedValue):
                        if float(self._LastValue) > float(self.MaxCollectedValue):
                            self.MaxCollectedValue = self._LastValue
                    else:
                        self.MaxCollectedValue = self._LastValue # last val wasn't numeric

            if hasattr(self, "MinCollectedValue"):
                if Utility.IsNumeric(self._LastValue):
                    if Utility.IsNumeric(self.MinCollectedValue):
                        if float(self._LastValue) < float(self.MaxCollectedValue):
                            self.MinCollectedValue = self._LastValue
                    else:
                        self.MinCollectedValue = self._LastValue # last val wasn't numeric

        except Exception as Ex:
            pass

    def alternateCollectionProc(self):
        if self.IsOnDemand():
            Log.getLogger().error("On Demand Collector called with alternateCollectionProc")

        if self.NeedsCollecting():
            startCollectionTime = Time.GetCurrMS()
            buffer = self.PerformCollection()
            TimeToCollect = Time.GetCurrMS() - startCollectionTime
            #print(TimeToCollect)
            if None != buffer:
                buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + buffer
                
                if not  self._NamespaceObject.SendPacket(buffer):
                    return 0

                #self._NamespaceObject.CheckMTU(len(buffer),self._MinionID)

                if TimeToCollect > self._PollingInterval:  # do a sanity check to see if collection time is longer than collector frequency
                    Log.getLogger().warning("Collector: " + self.GetID() + " took longer to perform the actual collection than the specified frequency for it (" + str(self._PollingInterval) + ".  It is suggested you change something to fix this.")

                return len(buffer)
        return 0

    #def alternateCollectionProcNotUsedAnymore(self):
    #    if self.IsOnDemand():
    #        Log.getLogger().error("On Demand Collector called with alternateCollectionProc1")
    #    if self.NeedsCollecting():
    #        collectedValue = self.Collect()

    #        #Get collected time after collection, can't be sure each collection
    #        #take same amount of time
    #        currMS = Time.GetCurrMS()
    #        elapsedTime = currMS - self._LastCollectionTime
    #        self._LastCollectionTime = currMS 

    #        if collectedValue != Collector.ErrorValue:
    #            sendValue = self.__NormalizeData(collectedValue,elapsedTime)
    #            refresh = self._RefreshRequested
    #            if sendValue == self._LastValue and self._SendOnlyOnDelta and not refresh and not self._DoNotSend :  # nothing changed, and only want on change
    #                pass

    #            elif sendValue != "HelenKeller": # HelenKeller means it is a mute collector, so don't send the actual data
    #                if self.__SendData(sendValue,elapsedTime,sendValue != collectedValue):
    #                # if, and only if sucessful transmit
    #                    if self._RefreshRequested:
    #                        self._RefreshRequested = False

    #            self._LastValue = collectedValue

    #        return True

    #    else:
    #        return False

    # Do an on-demand collection - Marvin sends a task to collect
    def CollectOnDemand(self,params):
        Log.getLogger().info("On Demand Collector called")

        if params != []: 
            origParams = self._Parameters.copy() # save orginal parameters
            self._Parameters.extend(params) # add any passed params

        self._RefreshRequested = True
        buffer = self.PerformCollection()

        if None != buffer:
            buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + buffer

        self._NamespaceObject.SendPacket(buffer)

        if params != []:
            self._Parameters = origParams.copy() #restore original params

    def __collectionProc(self,fnKillSignalled,userData):
        if True == self._RunOnce:
            self.alternateCollectionProc()
            return

        sleepTime = Collector.__SleepInterval
        while not fnKillSignalled():
            self.alternateCollectionProc()
            Sleep.SleepMs(sleepTime) # small sleep

            #if self._LastCollectionTime + self._PollingInterval - sleepTime <
            #Time.GetCurrMS(): #reduce sleep time to line up with next desired
            #interval
            #    sleepTime = self._LastCollectionTime + self._PollingInterval -
            #    Time.GetCurrMS()



