##############################################################################
#  Copyright (c) 2016-2017 Intel Corporation
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
#    Config reader and info store
#
##############################################################################
import xml.dom.minidom
import os
import datetime
from xml.parsers.expat import ExpatError
import socket
import hashlib
from Helpers import  Log
from Helpers import Alias
from Helpers import Target
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import Target
from Helpers import TargetManager
from Data import MarvinData
from Data import MarvinGroupData
import re
import sys


# Global function to return my logger object
def get():
    return Configuration.GetConfiguration()

class Configuration():
    _ConfigurationInstance = None
    @staticmethod
    def GetConfiguration():
        if None == Configuration._ConfigurationInstance:
            Configuration._ConfigurationInstance = Configuration()

        return Configuration._ConfigurationInstance

    def __init__(self):
            self.__initialize()
    
    def __initialize(self):
        self.__FromUpstreamConnection = ConnectionPoint.ConnectionPoint() #comes from Minions and other Oscars, primary data channel, going towards Marvin
        self.__FromDownstreamConnection =  ConnectionPoint.ConnectionPoint() # comes from Marvins and Oscars - things like heartbeats and tasks
        self.__OutgoingDownstreamConnections = []   # Target Marvins and chained Oscars
        self.__ID = "Undefined Oscar ID" # ID of this Oscar Instance
        self.__HasBeenRead = False
        self.__IsValid = False
        self.__IsMemorex = False
        self.__HeartbeatTimeoutPeriod = 60000
        self.__ConnectionUpdateInterval = 20000
        self.__ConfigFilename = "OscarConfig.xml"
        self.__ConfigFileTimeStamp = None
        self.__NoGui=False
        self.__MinimizedGUI=False
        self.__AutoLoadFile=None
        self.__LogFile="OscarLog.txt"
        self.__AutoRunMode = None
        self.__AutoRunLoopStart=0
        self.__AutoRunLoopEnd=None
        self.__RecordFileName = None
        self.__AutoRunTime = None
        self.__PlaybackSpeed=1
        self.__UseGUI=True
        self.__ExitAfterAutoPlay=False
        #### Governer - throttles transmit as to not overload system -- experimental -- #####
        self.__UseGoverner = False
        self.__GovernerThreshhold = 2048 
        self.__GovernerThreshhold *= 1024 # turn it into KB
        self.__GovernerBackoffPeriod = 10
        self.__GovernerMaxPacketsBeforeRest = 500
        ####  Governer ####
        self.__DynamicConnectMarvinMap = {} 
        self.__ShuntMap = {}       # includes wildcards, is a map of namespaces, each entry is a map of IDs that contain dataTuples
        self.__ShuntMapResolved = {} # only specific NS + ID resolved, key is ns+id, contains dataTuple
        self.__NotShuntedMap = {} # If in here, look no further
        self.__ShuntingFiles = {}
        self.__Shunting = None
        self.__ShuntWorkerThreadInterval = 250
        self.__BITW_NamespaceMap = {}
        self.__BITW_NotMatchedMap={}
        self.__BITW_Active = False
        self.__ReceiveBufferSize=32768 #size of buffer to read data into

    def GetMinimizeGui(self):
        return self.__MinimizedGUI

    def SetMinimizeGui(self, newVal):
        self.__MinimizedGUI = newVal

    def GetBITW_Active(self):
        return self.__BITW_Active

    def GetBITW_NamespaceMap(self):
        return self.__BITW_NamespaceMap

    def GetShuntWorkerInterval(self):
        return self.__ShuntWorkerThreadInterval

    def GetRecvBufferSize(self):
        return self.__ReceiveBufferSize

    def GetShuntMap(self):
        return self.__ShuntMap;

    def GetResolvedShuntMap(self):
        return self.__ShuntMapResolved

    def GetNotShuntedMap(self):
        return self.__NotShuntedMap

    def GetShunting(self):
        if None == self.__Shunting:
            self.__Shunting = len(self.__ShuntMap) != 0

        return self.__Shunting

    def GetUseGoverner(self):
        return self.__UseGoverner

    def GetGovernerThreshhold(self):
        return self.__GovernerThreshhold

    def GetGovernerBackoffPeriod(self):
        return self.__GovernerBackoffPeriod

    def GetGovernerMaxPacketsBeforeRest(self):
        return self.__GovernerMaxPacketsBeforeRest


    def SetExitAfterAutoPlay(self,value):
        self.__ExitAfterAutoPlay=value

    def GetExitAfterAutoPlay(self):
        return self.__ExitAfterAutoPlay

    def SetUseGUI(self,value):
        self.__UseGUI = value

    def GetUseGUI(self):
        return self.__UseGUI
    def SetPlaybackSpeed(self,value):
        self.__PlaybackSpeed = value

    def GetPlaybackSpeed(self):
        return self.__PlaybackSpeed

    def SetRecordFilename(self,strName):
        self.__RecordFileName = strName

    def GetRecordFilename(self):
        return self.__RecordFileName

    def SetAutorunTime(self,period):
        self.__AutoRunTime = period

    def GetAutorunTime(self):
        return self.__AutoRunTime 

    def SetAutrunLocations(self,start,stop):
        self.__AutoRunLoopStart = start
        self.__AutoRunLoopEnd = stop

    def GetAutorunLocations(self):
        return (self.__AutoRunLoopStart ,self.__AutoRunLoopEnd)

    def SetAutoRunMode(self,mode):
        self.__AutoRunMode = mode

    def GetAutoRunMode(self):
        return self.__AutoRunMode 

    def SetNoGui(self,flag):
        self.__NoGui = flag

    def GetNoGui(self):
        return self.__NoGui

    def SetLogFilename(self,strName):
        self.__LogFile = strName

    def GetLogFilename(self):
        return self.__LogFile

    def GetAutorunFilename(self):
        return self.__AutoLoadFile

    def SetAutorunFilename(self,strName):
        self.__AutoLoadFile = strName

    def SetConfigFilename(self,strName):
        self.__ConfigFilename = strName

    def GetConfigFilename(self):
        return self.__ConfigFilename
        
    def GetConnectionUpdateInterval(self):
        return self.__ConnectionUpdateInterval

    def GetTimeoutPeriod(self):
        return self.__HeartbeatTimeoutPeriod

    def GetDownstreamConnection(self):
        return self.__FromDownstreamConnection

    def GetUpstreamConnection(self):
        return self.__FromUpstreamConnection

    def GetOutgoingDownstreamConnections(self):
        return self.__OutgoingDownstreamConnections

    def GetConfFileModificationInfo(self):
        return self.__ConfigFileTimeStamp

    def GetID(self):
        return self.__ID

    def modification_date(filename):
        t = os.path.getmtime(filename)
        return datetime.datetime.fromtimestamp(t)

    def ReadConfigFile(self):
        if self.__HasBeenRead == True:
            return False
        
        self.__HasBeenRead = True 

        filename = self.GetConfigFilename()

        self.__ConfigFileTimeStamp = Configuration.modification_date(filename)

        #open the xml file for reading:
        file = open(filename,'r')
        #convert to string:
        data = file.read()
        #close file because we dont need it anymore:
        file.close()
        try:
            domDoc = xml.dom.minidom.parseString(data)
        except Exception as ex:
            self.__HandleInvalidXML("Bad Content - XML error: " + str(ex))
            return False
        self.__ReadAliasList(domDoc)

        attributes = domDoc.getElementsByTagName('Oscar')[0].attributes

        if None != attributes:
            if "ID" in attributes.keys():
                ID = Alias.Alias(attributes["ID"].nodeValue)

        if None == ID:
            Log.getLogger().error("No ID configured for Oscar");
            return False

        else:
            Log.getLogger().info("Oscar ID = " + ID)
            self.__ID = ID

        try:
            strVal = Alias.Alias(domDoc.getElementsByTagName('SendStatus')[0].firstChild.nodeValue)
            if strVal.upper() == "TRUE":
                self.__IsMemorex = True

        except Exception:
            pass
 
        nodeList = domDoc.getElementsByTagName("IncomingMarvinConnection")
        if None != nodeList and len(nodeList) > 0:
            attributes = nodeList[0].attributes
            if "IP" in attributes:
                self.__FromDownstreamConnection.IP = Alias.Alias(attributes["IP"].nodeValue)

            else:
                Log.getLogger().info("No Incoming Marvin IP specified, will listen on all interfaces")
                self.__FromDownstreamConnection.IP = "0.0.0.0"
                 
            if "PORT" in attributes:
                try:
                   self.__FromDownstreamConnection.Port = int(Alias.Alias(attributes["PORT"].nodeValue))
                except Exception as Ex:
                    Log.getLogger().error(str(ex))
                    Log.getLogger().error("Invalid Port set for Incoming Minion Connection")
                    return  False
            else:
                Log.getLogger().info("No Incoming Marvin Connection Port specified, will choose one")
                
        else:
            Log.getLogger().info("Incoming Marvin Connection not defined,will choose dynamically")
            self.__FromDownstreamConnection.IP = "0.0.0.0"

        nodeList = domDoc.getElementsByTagName("IncomingMinionConnection")
        if None != nodeList and len(nodeList) > 0:
            attributes = nodeList[0].attributes
            if "IP" in attributes:
                self.__FromUpstreamConnection.IP = Alias.Alias(attributes["IP"].nodeValue)

            else:
                self.__FromUpstreamConnection.IP="0.0.0.0" #listen on all interfaces
                Log.getLogger().info("No IncomingMinionConnection IP specified, listening on all interfaces")
                
            if "PORT" in attributes:
                try:
                   self.__FromUpstreamConnection.Port = int(Alias.Alias(attributes["PORT"].nodeValue))
                except Exception as Ex:
                    Log.getLogger().error(str(ex))
                    Log.getLogger().error("Invalid Port set for IncomingMinionConnection ")
                    return  False
            else:
                Log.getLogger().error("No IncomingMinionConnection Port specified")
                return False
        else:
            Log.getLogger().error("IncomingMinionConnection not defined")
            return False

        if False == self.__ReadAutoConnectInfo(domDoc):
            return False

        self.__OutgoingDownstreamConnections = self.__ReadDownstreamTargets(domDoc)

        if None == self.GetOutgoingDownstreamConnections(): # go read targets, if none, then Houston we have an problemo
            return False

        if False == self.__ReadShuntInfo(domDoc):
            return False

        if False == self.__ReadBumpInTheWireInfo(domDoc):
            return False

        self.Valid = True
        
        return True

    # this routine (mostly a cut and paste of __ReadDownstreamTargets) will, if the config file has
    # changed, go see if there are any new targets.  It won't remove existing targets, but will add new ones.
    def RescanTargets(self):
        try:
            if self.GetConfFileModificationInfo() == Configuration.modification_date(self.GetConfigFilename()):
                return

            filename = self.GetConfigFilename()
            Log.getLogger().info("Re-scanning config file: " + filename)
            self.__ConfigFileTimeStamp = Configuration.modification_date(filename)
            filename = self.GetConfigFilename()
            #open the xml file for reading:
            file = open(filename,'r')
            #convert to string:
            data = file.read()
            #close file because we dont need it anymore:
            file.close()
        except:
            return

        try:
            domDoc = xml.dom.minidom.parseString(data)
        except Exception as ex:
            self.__HandleInvalidXML("Bad Content - XML error: " + str(ex))
            return 

        nodeList = domDoc.getElementsByTagName("TargetConnection")
        if None != nodeList and len(nodeList) > 0:
            for node in nodeList:
                attributes = node.attributes
                if "IP" in attributes:
                    IP = Alias.Alias(attributes["IP"].nodeValue)

                else:
                    Log.getLogger().error("No Target IP specified")
                    return False
                
                if "PORT" in attributes:
                    try:
                       Port = int(Alias.Alias(attributes["PORT"].nodeValue))
                    except Exception as Ex:
                        Log.getLogger().error(str(Ex))
                        Log.getLogger().error("Invalid Port set for Target Connection")
                        return  False
                else:
                    Log.getLogger().error("No Target IP specified")
                    return False

                objTarget = Target.Target(IP,Port,ConnectionType.Unknown,True)# could be Marvin or another Oscar
                
                #Key = socket.gethostbyname(IP) + ":" +str(Port)
                Key = IP + ":" +str(Port)
                if not TargetManager.GetTargetManager().GetDownstreamTarget(Key):
                    Log.getLogger().info("Adding new Downstream target: " + Key)
                    TargetManager.GetTargetManager().AddDownstreamTarget(objTarget,Key)

    ## Go and read the targets!
    def __ReadDownstreamTargets(self,domDoc):
        retList = []
        nodeList = domDoc.getElementsByTagName("TargetConnection")
        if None != nodeList and len(nodeList) > 0:
            for node in nodeList:
                attributes = node.attributes
                if "IP" in attributes:
                    IP = Alias.Alias(attributes["IP"].nodeValue)

                else:
                    Log.getLogger().error("No Target IP specified")
                    return False
                
                if "PORT" in attributes:
                    try:
                       Port = int(Alias.Alias(attributes["PORT"].nodeValue))
                    except Exception as Ex:
                        Log.getLogger().error(str(Ex))
                        Log.getLogger().error("Invalid Port set for Target Connection")
                        return  False
                else:
                    Log.getLogger().error("No Target IP specified")
                    return False

                objTarget = Target.Target(IP,Port,ConnectionType.Unknown,True)# could be Marvin or another Oscar
                
                #Key = socket.gethostbyname(IP) + ":" +str(Port)
                Key = IP + ":" +str(Port)
                TargetManager.GetTargetManager().AddDownstreamTarget(objTarget,Key)
                
                retList.append(Target)
                
        elif None == self.__DynamicConnectMarvinMap :
            Log.getLogger().error("TargetConnection not defined")
 
        return retList 

    def __ReadAutoConnectInfo(self,domDoc):
        retList = []
        nodeList = domDoc.getElementsByTagName("MarvinAutoConnect")
        if None != nodeList and len(nodeList) > 0:
            for node in nodeList:
                attributes = node.attributes
                  
                if "Key" in attributes:
                    Key = Alias.Alias(attributes["Key"].nodeValue)
                else:
                    Log.getLogger().error("No MarvinAutoConnect Key specified")
                    return False

                hashGen = hashlib.md5(str.encode(Key))
                HashOfKey = hashGen.hexdigest()
                
                if HashOfKey in self.__DynamicConnectMarvinMap.keys():
                    Log.getLogger().error("Duplicate MarvinAutoConnect Keys: " + Key)
                    return False

                self.__DynamicConnectMarvinMap[HashOfKey] = Key

        return True

    def __ReadBumpInTheWireInfo(self,domDoc):
        bumps = domDoc.getElementsByTagName("BITW")
        if None != bumps and len(bumps) > 0:
            for bump in bumps:
                try:
                    inputList = bump.getElementsByTagName("Input")
                    outputList = bump.getElementsByTagName("Output")
                    appendMode = False
                    modeStr = bump.getElementsByTagName("Mode")

                    if None == inputList or None == outputList:
                        Log.getLogger().error("BITW required an <Input> and and <Output>")
                        return False

                    if len(inputList) > 1 or len(outputList) > 1:
                        Log.getLogger().error("BITW required a single <Input> and and <Output>")
                        return False

                    input = inputList[0]
                    output = outputList[0]
                    if None != modeStr:
                        modeStr = modeStr[0].firstChild.nodeValue
                        if modeStr.lower() == "append":
                            appendMode=True
                        elif modeStr.lower() == "replace":
                            pass
                        else:
                            Log.getLogger().error("Invalid BITW <Mode>: " + modeStr)
                            return False


                    if False == input.hasAttribute("Namespace"):
                        Log.getLogger().error("BITW <Input> requires Namespace attribute.")
                        return False
                        
                    if False == output.hasAttribute("Namespace"):
                        Log.getLogger().error("BITW <Output> requires Namespace attribute.")
                        return False

                    inputNS = input.getAttribute("Namespace")
                    outputNS = output.getAttribute("Namespace")

                    if inputNS.upper() == outputNS.upper():
                        Log.getLogger().error("BITW <Input> and <Output> Namespace are the same. {" + inputNS + "}")
                        return False

                    key=inputNS.upper()
                    if key in self.__BITW_NamespaceMap:
                        Log.getLogger().error("BITW <Input> Namespace " + inputNS + " already defined.")
                        return False

                    try:
                        INP_NS_Comp = re.compile(key)
                                    
                    except Exception as Ex:
                        Log.getLogger().error("Invalid <BITW> regEx expression for Namespace: " + inputNS)
                        return False

                    try:
                        NS_Comp = re.compile(outputNS.upper())
                                    
                    except Exception as Ex:
                        Log.getLogger().error("Invalid <BITW> regEx expression for Namespace: " + outputNS)
                        return False

                    self.__BITW_NamespaceMap[key] = (outputNS, appendMode)
                    self.__BITW_Active = True

                except Exception as Ex:
                    Log.getLogger().error(str(Ex))
                    return False

    def __ReadShuntInfo(self,domDoc):
        nodeList = domDoc.getElementsByTagName("Shunt")
        if None != nodeList and len(nodeList) > 0:
            for node in nodeList:
                attributes = node.attributes
                  
                if "ID" in attributes:
                    ID = Alias.Alias(attributes["ID"].nodeValue)
                else:
                    Log.getLogger().error("No Shunt ID specified")
                    return False

                if "Namespace" in attributes:
                    Namespace = Alias.Alias(attributes["Namespace"].nodeValue)
                else:
                    Log.getLogger().error("No Shunt Namespace specified")
                    return False

                Historical=False
                if "Type" in attributes:
                    Type = Alias.Alias(attributes["Type"].nodeValue)
                    if Type.lower() == "history":
                        Historical=True
                    else:
                        Log.getLogger().error("Unknown Shunt Type file specified ID=" + ID + " Namespace=" + Namespace +": " + Type)
                        return False

                ShuntFile = None
                if 0 != len(node.childNodes):
                    ShuntFile=node.firstChild.nodeValue           
                if None == ShuntFile:     
                    Log.getLogger().error("No Shunt file specified ID=" + ID + " Namespace=" + Namespace)
                    return False

                NamespaceKey=Namespace.lower()
                IDKey=ID.lower()

                try:
                    ID_Comp = re.compile(IDKey)
                                    
                except Exception as Ex:
                    Log.getLogger().error("Invalid Shunt regEx expression for ID: " + ID)
                    return False

                try:
                    NS_Comp = re.compile(NamespaceKey)
                                    
                except Exception as Ex:
                    Log.getLogger().error("Invalid Shunt regEx expression for Namespace: " + Namespace)
                    return False

                if ShuntFile.lower() in self.__ShuntingFiles:
                    if True == Historical:
                        Log.getLogger().warn("Sending Historical data from more than 1 data source to the same file: " + ShuntFile)
                else:
                    self.__ShuntingFiles[ShuntFile.lower()] = ShuntFile;

                dataTuple = (Namespace,ID,ShuntFile,ID_Comp,Historical)

                if NamespaceKey in self.__ShuntMap:
                    IdMap = self.__ShuntMap.get(NamespaceKey)
                    if IDKey in IdMap:
                         Log.getLogger().error("Duplicate Shunt specified ID=" + ID + " Namespace=" + Namespace)
                         return False
                    else:
                        IdMap[IDKey] = dataTuple

                else:
                    IdMap = {}
                    IdMap[IDKey] = dataTuple
                    self.__ShuntMap[NamespaceKey] = IdMap

                self.__ShuntMapResolved[NamespaceKey+IDKey] = dataTuple 

                ## Initialize the file ##
                try:
                    with open(ShuntFile,"w") as sf:
                        sf.write("##### Generated by Oscar Shunt #####\n");
                except Exception as Ex:
                    Log.getLogger().error("Invalid Shunt filename specified: " + ShuntFile)
                    return False                    

                Log.getLogger().info("Creating Shunt [" + Namespace + ":" + ID +"] --> " + ShuntFile)

        return True


    def GetMarvinAutoConnectKeyFromHash(self,keyHash):
        if keyHash in self.__DynamicConnectMarvinMap.keys():
            return self.__DynamicConnectMarvinMap[keyHash]

        return None

    # spits out an error message about invalid file
    def __HandleInvalidXML(self,Error):
        Log.getLogger().error(("Error parsing " + self.GetConfigFilename() + ": " + Error))
        self.Valid = False

    def __ReadAliasList(self,doc):
        for aliasList in  doc.getElementsByTagName('AliasList'):
            for tag in  aliasList.getElementsByTagName('Alias'):
                attributes = tag.attributes
                if None != attributes:
                    for attrName,attrValue in attributes.items():
                        Alias.AliasMgr.AddAlias(attrName,Alias.Alias(attrValue))


    def getNamespacesFromBuffer(self,buffer):
        from Helpers import Configuration
        config = Configuration.get()

        start = '<Namespace>'
        end = '</Namespace>'
        reg = "(?<=%s).*?(?=%s)" % (start,end)
        try:

            result = re.search(reg, buffer)
            r= re.compile(reg)
            NamespaceList = r.findall(buffer)

            if None == NamespaceList:
                Log.getLogger().error("Error Something bad in trying to find namespaces HandleBITW " + buffer)
                return None

            list=[]

            map = config.GetBITW_NamespaceMap()
            checkedMap={}
            for namespace in NamespaceList:
                if not namespace in checkedMap: 
                    checkedMap[namespace] = namespace # could be a group, and only need to check once, but could be case differences, so don't use upper
                    strUp = namespace.upper() 
                    if strUp in map:
                        list.append((namespace,map[strUp]))

        except Exception as ex:
            print(str(ex))

        return list


    def HandleBITWBuffer(self,sendBuffer):
        if "<Oscar Type=\"ConnectionInformation\">" in sendBuffer:
            return sendBuffer # only care about Data

        start = '<Namespace>'
        end = '</Namespace>'

        list = self.getNamespacesFromBuffer(sendBuffer)

        for namespace,newNamespaceTuple in list:
            newNamespace = self.GenerateBITW_String(namespace,newNamespaceTuple)
            sendBuffer = sendBuffer.replace(start+namespace+end,start+newNamespace+end)

        return sendBuffer

    def GenerateBITW_String(self,namespaceToCheck,tuple):
        if True == tuple[1]: # is append mode
            return namespaceToCheck + tuple[0]
        return tuple[0]

    def HandleBITWNamespace(self, namespaceToCheck):
        if self.__BITW_Active:
            strUpper = namespaceToCheck.upper()
            if strUpper in self.__BITW_NotMatchedMap:
                return namespaceToCheck

            if strUpper in self.__BITW_NamespaceMap:
                return self.GenerateBITW_String(namespaceToCheck,self.__BITW_NamespaceMap[strUpper])

            else: # Try regEx check
                for key in self.__BITW_NamespaceMap.keys():
                    regEx = re.compile(key)
                    if regEx.match(strUpper) : #found a regEx match
                        self.__BITW_NamespaceMap[strUpper] = self.__BITW_NamespaceMap[key] # put in resolved hash for speed
                        return self.GenerateBITW_String(namespaceToCheck,self.__BITW_NamespaceMap[strUpper])

            self.__BITW_NotMatchedMap[strUpper] = strUpper # map of notmatched, to speed up future data

        return namespaceToCheck

    def HandleBITW_MarvinDataObject(self,objMarvinData):
        objMarvinData.Namespace = self.HandleBITWNamespace(objMarvinData.Namespace)
        return objMarvinData

    def HandleBITW_FromLoad(self,entries):
        if self.__BITW_Active:
            for entry in entries:
                if isinstance(entry,MarvinGroupData.MarvinDataGroup):
                    for datapoint in entry._DataList:
                        datapoint = self.HandleBITW_MarvinDataObject(datapoint)

                elif isinstance(entry,MarvinData.MarvinData):
                    entry = self.HandleBITW_MarvinDataObject(entry)

        return entries