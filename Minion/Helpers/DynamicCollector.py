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
#    This is the file for the Dynamic Collector, which creates collectors based
#    upon the contents of a file where each line has and ID=Value
#    This collector will read all values and create new collectors on initial read
#    subsequent reads it will update those collectors with values within the file
#    the thought is that something outside Minion is creating and updating that file
#
##############################################################################

from Helpers import Log
from Util import Time
from Util import Sleep
from Util import Utility
from Helpers import ThreadManager
from Helpers import Worker
from Collectors import FileCollector
from Helpers import Collector
from Helpers import UserPluginFramework
import os
import re

class DynamicCollector(Collector.Collector):
    __ID_Number = 1 # in case you specify the same file for different dynamic collectors
    __CollectorNumber = 1
    def __init__(self,objNamespace,InGroup,InputFileName):
        Collector.Collector.__init__(self,objNamespace,InputFileName + "." + str(DynamicCollector.__ID_Number),InGroup)
        self.__FileName = InputFileName
        self.__PrefixStr = ""
        self.__SuffixStr = ""
        self.__Group = None
        DynamicCollector.__ID_Number += 1
        self.__ModifyList = []
        self.__PreviousTimestamp=0
        self.__LockFileName = None
        self.__pluginInfo = None
        self.__LoadWarningSent=False
        self.__TokenList=['=','= ',': ',':',' ']
        
    def SetParseTokens(self,tokenList):
        self.__TokenList = tokenList

    def SetPluginInfo(self, pluginFile, pluginFunction, pluginSpawnThread):
        pluginInterface = self.CreatePluginInterfaceObject()

        self.__pluginInfo = UserPluginFramework.UserPluginFramework(pluginFile,pluginFunction,self._Parameters, pluginInterface, pluginSpawnThread)
        self.__pluginInfo.kwargs = self._kwargs
        valid = self.__pluginInfo.ValidateUserPlugin()
        self.Collect = self.__CollectProcForPlugin # remap collect prox
        return valid

    def SetLockfile(self,strFile):
        self.__LockFileName = FileCollector.convertPath(strFile)

    def GetLockFile(self):
        return self.__LockFileName

    def SetGroup(self,newGroup):
        self.__Group = newGroup

    def SetPrefix(self,newStr):
        self.__PrefixStr = newStr.strip()

    def SetSuffix(self,newStr):
        self.__SuffixStr = newStr.strip()

    def SetSendOnlyOnDelta(self,newFlag):
        self._SendOnlyOnDelta = newFlag

    # For dynamic collectors, you can modify some of the individual attributes, so here I
    # store them for looking up when the dynamic collector gets created
    def AddCollectorModifier(self,CollectorID,Precision,Normalize,SyncFile,Scale,DoNotSendStr, SendOnlyOnChangeStr):
        if None != Precision:
            try:
                Precision = int(Precision)
            except:
                Log.getLogger().error("Invalid ModifyCollector <Precision>: " + str(Precision))
                return False

        if None != Normalize:
            try:
                Normalize = float(Normalize)
            except:
                Log.getLogger().error("Invalid ModifyCollector <Normalize>: " + str(Normalize))
                return False
            
        if None != Scale:
            try:
                Scale = float(Scale)
            except:
                Log.getLogger().error("Invalid ModifyCollector <Scale>: " + str(Scale))
                return False

        SendOnlyOnChange = None
        DoNotSend = None
        if None != DoNotSendStr:
                DoNotSend = VerifyBoolString(DoNotSendStr)
                if None == DoNotSend:
                    Log.getLogger().error("Invalid ModifyCollector <DoNotSend>: " + str(DoNotSendStr))
                    return False

        if None != SendOnlyOnChangeStr:
                SendOnlyOnChange = VerifyBoolString(SendOnlyOnChangeStr)
                if None == SendOnlyOnChange:
                    Log.getLogger().error("Invalid ModifyCollector <SendOnlyOnChange>: " + str(SendOnlyOnChange))
                    return False

        for modItem in self.__ModifyList:
            if modItem[0].lower() == CollectorID.lower():
                Log.getLogger().error("Invalid ModifyCollector, Modfy ID already specified: " + str(CollectorID))
                return False

        dataToStore = [CollectorID,[Precision,Normalize,SyncFile,Scale,DoNotSend, SendOnlyOnChange]]
        self.__ModifyList.append(dataToStore)
        return True

    # Goes though the specified file, updates existing collectors, and creates
    # ones that don't exist
    def Collect(self):
        #AddedNewDynamicCollectors = False
        try:
            fname = FileCollector.convertPath(self.__FileName)
            
            if None != self.__LockFileName:
                lockName = self.__LockFileName
                if False == WaitForLock(lockName ):
                    Log.getLogger().error("Timeout getting exclusive lockfile: " + self.__LockFileName)
                    return "HelenKeller"

            elapsedTime = self.GetElapsedTimeSinceLast()
            with open(self.__FileName,"rt") as inpFile:
                data = inpFile.read()

            if None != self.__LockFileName:
                try :
                    os.remove(lockName)

                except Exception as ex:
                    return "0"

        except Exception as Ex:
            if not self.__LoadWarningSent:
                self.__LoadWarningSent = True
                Log.getLogger().warn("Problem loading DynamicCollector File: " + self.__FileName +". No additional warnings after this")
            return "HelenKeller"


        #Entire file is now in Data and file is closed.  So go parse it
        lines = data.split('\n') # might need os.linesp here....
        
        ts = Time.GetFileTimestampMS(fname)
        #timeDelta =  ts - self.__PreviousTimestamp
        self.__PreviousTimestamp = ts
#        print("File Time Delta: "  + str(timeDelta) + " Elapsed Time: " + str(elapsedTime) + "Entries: " + str(len(lines)))

        #altTokens=['= ',': ',':',' ']
        try:
            for line in lines:
                for firstToken in self.__TokenList:
                    dataPoint = line.split(firstToken,1)
                    if len(dataPoint) > 1:
                        break

                if len(dataPoint) > 1: # filter out empties
                    ID = dataPoint[0]
                    Value = dataPoint[1]
                    ID = ID.strip()
                    Value = Value.strip()
                    ID = self.__PrefixStr + ID + self.__SuffixStr
                    objCollector = self._NamespaceObject.GetCollector(ID)

                    if None == objCollector:
                        objCollector = self.__createCollector(ID)
                        Log.getLogger().debug("Dynamic Collector found with token: '" + firstToken + "'")

                    objCollector.SetDynamicData(Value,elapsedTime)

        except Exception as ex:
            Log.getLogger().error("Something bad happened in DynamicCollector Collector(): " + str(ex))

        return "HelenKeller"


    def __CollectProcForPlugin(self):
        if None == self.__pluginInfo:
            Log.getLogger().error("Severe error, Collection Proc for DynamicCollector Plugin called with no plugin info.  Report to Patrick.")
            exit()

        retVal = self.__pluginInfo.CollectionProc()
        if None == retVal:
            retVal = "HelenKeller"
        return retVal
        
    def __createCollector(self,ID,fromPlugin=False):
        objCollector = Collector.Collector(self._NamespaceObject,ID,self._InGroup)

        objCollector._OnDemand = self._OnDemand
        objCollector._PollingInterval = self._PollingInterval
        objCollector.IsDynamicallyCreated = True
        objCollector.Precision = self.Precision
        objCollector._SendOnlyOnDelta = self._SendOnlyOnDelta
        objCollector._Normalize = self._Normalize
        objCollector._NormalizeValue = self._NormalizeValue
        objCollector._DoNotSend = self._DoNotSend
        objCollector.DynamicCollectorParent=self
        objCollector.GetElapsedTimeSinceLast = objCollector.GetElapsedTimeForDynamicWidget #remap the fn called
        objCollector.SetLastCollectionTime = objCollector.SetLastCollectionTimeForDynamicWidget
        objCollector.SetProcessThreadID(self.GetProcessThreadID())
        objCollector.GetElapsedTimeSinceLast = objCollector.GetElapsedTimeSinceLastForDynamicWidget

        if True == fromPlugin: # bit of a hack, only need this if from a plugin
        # 'hack' to have multiple namespaces coming from same plugin - like influxdb
            if ID[-2:] == "-]" :
                ID = ID.split("[-")[0]
                
            if len(self.__PrefixStr) > 0:
                objCollector._OverrideID = self.__PrefixStr + ID

            if len(self.__SuffixStr) > 0:
                objCollector._OverrideID = ID + self.__SuffixStr

        if fromPlugin:
            objCollector.DynamicSuffix="" #=str(self)
        else:
            objCollector.DynamicSuffix=""

        if float(self.ScaleValue) != 1.0:
            objCollector.SetScaleValue(self.ScaleValue)


        Log.getLogger().debug("Creating Dynamic Collector[" + str(DynamicCollector.__CollectorNumber) + "]: " + ID[:-len(objCollector.DynamicSuffix)])
        objCollector._DebugNum = DynamicCollector.__CollectorNumber
        DynamicCollector.__CollectorNumber += 1

        # go see if there are any modifies for this collector, and for fun, let's do a RegEx, cause that could be interesting
        for mod in self.__ModifyList:
            try:
                pattern = re.compile(mod[0].lower())
                matched = pattern.match(ID.lower())
            except Exception as Ex:
                Log.getLogger().error("Invalid RegEx patter for <ModifyCollector>: " + mod[0] + ": " + str(Ex))
                return None
            
            if matched:
                Log.getLogger().info("Applying Modifiers to DynamicCollector " + ID)
                # data format
                # [CollectorID,[Precision,Normalize,Scale,DoNotSend,
                # SendOnlyOnChange]]

                Precision = mod[1][0]
                Normalize = mod[1][1]
                SyncFile = mod[1][2]
                Scale = mod[1][3]
                DoNotSend = mod[1][4]
                SendOnlyOnChange = mod[1][5]

                objCollector._SyncFile = SyncFile

                if None != Precision:
                    objCollector.Precision = Precision

                if None != Normalize:
                    objCollector._NormalizeValue = Normalize
                    objCollector._Normalize = True

                if None != Scale:
                    objCollector.SetScaleValue(Scale)

                if None != DoNotSend:
                    objCollector._DoNotSend = DoNotSend

                if None != SendOnlyOnChange:
                    objCollector._SendOnlyOnDelta = SendOnlyOnChange

                # remove this node from the list, and get outta here
                if mod[0].lower() == ID.lower(): # was an exact match and not a RegEx match, so can nuke it
                    self.__ModifyList.remove(mod)
                
                break # if matched

        if None == self.__Group:
            self._NamespaceObject.AddCollector(objCollector,self._MinionID)
        else:
            self.__Group.AddCollector(objCollector,self._MinionID)
        
        return objCollector

    def SetLastCollectionTime(self,timeVal):
        self._LastElapsedTimePeriod = timeVal - self._LastCollectionTime 
        self._LastCollectionTime = timeVal 

    def CreatePluginInterfaceObject(self):
        #Create a little plugin interface for the plugins to do what they need to do
        class UserPluginInterface:
           def __init__(self,objDyna):
                self.DoesCollectorExist = objDyna.CollectorExistsFromPlugin
                self.AddCollector = objDyna.AddCollectorFromPlugin
                self.SetCollectorValue = objDyna.SetCollectorValueFromPlugin
                self.SetNormilization = objDyna.SetNormilizationFromPlugin
                self.SetPrecision = objDyna.SetPrecisionFromPlugin
                self.SetScale = objDyna.SetScaleFromPlugin
                self.KillThreadSignalled = None
                self.LockFileName = objDyna.GetLockFile()
                self.Interval = objDyna._PollingInterval
                self.Logger = Log.getLogger()
               
        interface = UserPluginInterface(self)
        return interface

    def __specialSuffix(self,customNamespaceString):
        if None == customNamespaceString:
            return ""
        return "[-{}-]".format(customNamespaceString)

    def SetPrecisionFromPlugin(self, collectorID, preicsionValue,customNamespaceString=None):
        objCollector = self._NamespaceObject.GetCollector(self.__PrefixStr +  collectorID + self.__SuffixStr + self.__specialSuffix(customNamespaceString))

        if None == objCollector:
            Log.getLogger().error("User defined DynamicCollector tried to Set a value to a collector that does not exist, with ID: " + collectorID)
            return

        try:
            objCollector.Precision = float(preicsionValue)
        except:
            Log.getLogger().error("User defined DynamicCollector tried to Set an invalid Precision value of {0} to a collector that does not exist, with ID: {1}".format(preicsionValue,collectorID))

    def SetNormilizationFromPlugin(self, collectorID, normilizationValue,customNamespaceString=None):
        objCollector = self._NamespaceObject.GetCollector(self.__PrefixStr +  collectorID + self.__SuffixStr+ self.__specialSuffix(customNamespaceString))

        if None == objCollector:
            Log.getLogger().error("User defined DynamicCollector tried to Set a value to a collector that does not exist, with ID: " + collectorID)
            return

        try:
            objCollector._NormalizeValue = float(normilizationValue)
            objCollector._Normalize=True
        except:
            Log.getLogger().error("User defined DynamicCollector tried to Set an invalid Normilization value of {0} to a collector that does not exist, with ID: {1}".format(normilizationValue,collectorID))

    def SetScaleFromPlugin(self, collectorID, scaleValue,customNamespaceString=None):
        objCollector = self._NamespaceObject.GetCollector(self.__PrefixStr +  collectorID + self.__SuffixStr+ self.__specialSuffix(customNamespaceString))

        if None == objCollector:
            Log.getLogger().error("User defined DynamicCollector tried to Set a scale to a collector that does not exist, with ID: " + collectorID)
            return

        objCollector.SetScaleValue(scaleValue)
 
    def CollectorExistsFromPlugin(self, collectorID,customNamespaceString=None): 
        objCollector = self._NamespaceObject.GetCollector(self.__PrefixStr +  collectorID + self.__SuffixStr+ self.__specialSuffix(customNamespaceString))
        return not objCollector == None

    def AddCollectorFromPlugin(self, collectorID,customNamespaceString=None):
        if self.CollectorExistsFromPlugin(collectorID):
            Log.getLogger().error("User defined DynamicCollector tried to Add a collector with ID that already exists: " + collectorID)
            return False

        objCollector = self.__createCollector(self.__PrefixStr +  collectorID + self.__SuffixStr+ self.__specialSuffix(customNamespaceString),True)

        if objCollector == None:
            Log.getLogger().error("Error creating collector using User defined DynamicCollector ID: " + collectorID)
            return False
            
        objCollector._OverrideID = self.__PrefixStr +  collectorID + self.__SuffixStr
    
        if None != customNamespaceString:
            objCollector.SetOverrideNamespaceString(customNamespaceString)

        return True

    def SetCollectorValueFromPlugin(self,collectorID,Value,elapsedTime=None,customNamespaceString=None):
        objCollector = self._NamespaceObject.GetCollector(self.__PrefixStr +  collectorID + self.__SuffixStr+ self.__specialSuffix(customNamespaceString))

        if None == objCollector:
            Log.getLogger().error("User defined DynamicCollector tried to Set a value to a collector that does not exist, with ID: " + collectorID)
            return False

        if None == elapsedTime:
            elapsedTime = Time.GetCurrMS() - objCollector._LastCollectionTime

        objCollector.SetDynamicData(Value,elapsedTime)
        return True

def VerifyBoolString(strVal):
    if strVal.lower() == "true":
        return True
           
    if strVal.lower ()== "false":
        return False

    return None

# worker routine to wait for a lock file to not be there
def WaitForLock(LockFileName):
    iCount = 0
    while True == os.path.isfile(LockFileName) :
        iCount+=1
        Sleep.SleepMs(2)
        if iCount > 100 : # if lock lasts too long, just return something TODO - maybe log something?
            return False
            
    try :
        lockFile = os.open(LockFileName,os.O_CREAT|os.O_EXCL|os.O_RDWR)
        os.write(lockFile,"lock".encode('utf-8'))
        os.close(lockFile)
        return True

    except Exception as ex:
        #give it one more try
       # iCount=0
        while True == os.path.isfile(LockFileName) :
            iCount+=1
            Sleep.SleepMs(5)
            if iCount > 100 : # if lock lasts too long, just return something TODO - maybe log something?
                return False

        try :
            lockFile = os.open(LockFileName,os.O_CREAT|os.O_EXCL|os.O_RDWR)
            os.write(lockFile,"lock".encode('utf-8'))
            os.close(lockFile)

            return True
                
        except Exception as ex:
            #print("Bottom of routine: " + str(ex))
            pass

    return False
