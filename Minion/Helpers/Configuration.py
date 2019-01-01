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
#    Config reader and info store
#
##############################################################################

import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Helpers import Alias
from Helpers import Namespace
from Helpers import Collector
from Helpers import CollectorParam
from Helpers import DynamicCollector
from Helpers import Operator
from Helpers import Actor
from Helpers import Group

class Configuration():
    _Instance = None
    _MaxTxBeforeRest = int(32768/3) #8192 is max buff size in Oscar. div by 3 to handle 3 Minions running at MAX speed
    _RepeatPktCnt=2 ## for UDP
    def __init__(self, filename,verbose):
        self._ConfigFilename = filename
        self._Verbose = verbose
        self._SingleThreadingModel = False
        self._DefaultNamespacePrecision=2

        #open the xml file for reading:
        file = open(filename,'r')
        #convert to string:
        data = file.read()
        #close file because we dont need it anymore:
        file.close()
        self.m_Namespaces = None
        try:
            self.m_dom = xml.dom.minidom.parseString(data)
        except Exception as Ex:
            self.HandleInvalidXML("Bad Content - XML error: " + str(Ex))
            return

        self.Valid = False
        self.GetThreadingModel()

        self.m_Namespaces = self.__ReadNamespaces()
        Configuration._Instance = self

    def GetThreadingModel(self):
        try:
            attributes = self.m_dom.getElementsByTagName('Minion')[0].attributes
        except Exception:
            attributes = None

        if None != attributes and "SingleThreading" in attributes.keys():  
            strBool = Alias.Alias(attributes["SingleThreading"].nodeValue)
            if strBool.lower() == "true" :
                self._SingleThreadingModel = True

        Namespace._UseSingleCollectorThreadPerNamespace = self._SingleThreadingModel
        

    def IsSingleThreadingModel(self):
        return self._SingleThreadingModel

    def IsValid(self):
        return self.Valid

    def GetNamespaces(self):
        return self.m_Namespaces

    def GetCollectorCount(self):
        total=0
        for ns in self.m_Namespaces:
            total += ns.getCollectorCount()

        return total

    def GetInfo(self):
        retString = ""
        if None != self.m_Namespaces :
            for inst in self.m_Namespaces:
                retString = retString + "--------- " + inst.Name + "---------"
                retString = retString + "\n" + inst.GetInfo()
        
        return retString


    # spits out an error message about invalid file
    def HandleInvalidXML(self,Error):
        Log.getLogger().error("Error parsing " + self._ConfigFilename + ": " + Error)
        self.Valid = False

    def __GetOperatorInputs(self,node,objOperator):
        inpCount = 0
        try:
            for child in node.childNodes:
                isConstant = False
                defValue = None
                if child.nodeName.lower() == "input":
                    strInputID = Alias.Alias(child.firstChild.nodeValue)
                    attributes = child.attributes
                    if 'Constant' in attributes.keys():
                        constVal = Alias.Alias(attributes['Constant'].nodeValue).strip().upper() # don't use the value here
                        isConstant = constVal == 'TRUE'

                    elif 'DefaultValue' in attributes.keys():
                        defValue = Alias.Alias(attributes['DefaultValue'].nodeValue).strip()

                    if not objOperator.AddInput(strInputID,isConstant):
                        return -1

                    if None != defValue:
                        objOperator.AddInput(strInputID,True, defValue)

                    inpCount += 1
                
                elif child.nodeName.lower() == "repeat":
                    inpCount += self.__RepeatReadInputs(child,objOperator)

        except Exception as ex:
            pass
        return inpCount
        
    def __GetRepeatAttribute(self,node,strItem,required=False):
        attributes = node.attributes
        if not strItem in attributes.keys():
            if True == required:
                Log.getLogger().error("Error parsing " + self._ConfigFilename + ": " + "Repeat specified for Operator Input, but no " + strItem + " given.  Ignoring")
                return -1
            return -1

        try:

            retVal = int(Alias.Alias(attributes[strItem].nodeValue))
        except:
            Log.getLogger().error("Error parsing " + self._ConfigFilename + ": " + "inalid " + required + " given for Repeat.  Ignoring")
            return -1

        if retVal < 0:
            Log.getLogger().error("Error parsing " + self._ConfigFilename + ": " + "inalid " + required + " given for Repeat.  Ignoring")
            return -1

        return retVal

    def __RepeatReadInputs(self,repeatNode,objOperator):
        count = self.__GetRepeatAttribute(repeatNode,"Count",True)
        if count < 0:
            return 0

        if count == 0:
            Log.getLogger().warning("Repeat with count of 0.  Ignoring.")
            return 0

        start = self.__GetRepeatAttribute(repeatNode,"StartValue")
        if start < 1:
            start = 0

        step = self.__GetRepeatAttribute(repeatNode,"StepValue")
        if step < 1:
            step = 1


        attributes = repeatNode.attributes
        strCurrentValAlias = None
        strCurrentCountAlias = None
        useAltAlias = False

        if "CurrentCountAlias" in attributes.keys():
            strCurrentCountAlias = Alias.Alias(attributes["CurrentCountAlias"].nodeValue)
            useAltAlias = True

        if "CurrentValueAlias" in attributes.keys():
            strCurrentValAlias = Alias.Alias(attributes["CurrentValueAlias"].nodeValue)
            useAltAlias = True

        currCount = 0
        itemCount = 0
        
        for index in range(0,count,step):
            Alias.AliasMgr.Push()

            if strCurrentCountAlias != None:
                Alias.AliasMgr.AddAlias(strCurrentCountAlias,str(currCount))

            if strCurrentValAlias != None:
                Alias.AliasMgr.AddAlias(strCurrentValAlias,str(index + start))

            if strCurrentCountAlias == None or strCurrentCountAlias.lower() != "CurrentCountAlias".lower():
                Alias.AliasMgr.AddAlias("CurrentCountAlias",str(currCount))

            if strCurrentValAlias == None or strCurrentValAlias.lower() != "CurrentValueAlias".lower():
                Alias.AliasMgr.AddAlias("CurrentValueAlias",str(index + start))

            itemCount += self.__GetOperatorInputs(repeatNode,objOperator)
            currCount += 1

            Alias.AliasMgr.Pop()

        return itemCount

    def __ReadBounds(self,nodeParent,objCollector):

#      <Bound Max="20000" Min="15555" Action="set"/>

        try:
            node = nodeParent.getElementsByTagName('Bound')[0]
        except:
            node = None

        if node != None:
            attributes = node.attributes
            Action = None
            Min = None
            Max = None
            if None == attributes:
                Log.getLogger().error("Collector [" + objCollector.GetID() + "] has a <Bound>, however it is empty.")
                return False

            if "Min" in attributes.keys():
                Min = Alias.Alias(attributes["Min"].nodeValue)
                try:
                    Min = float(Min)
                except:
                    Log.getLogger().error("Collector [" + objCollector.GetID() + "] has a <Bound>, with non numeric Min specified.")
                    return False

            if "Max" in attributes.keys():
                Max = Alias.Alias(attributes["Max"].nodeValue)
                try:
                    Max = float(Max)
                except:
                    Log.getLogger().error("Collector [" + objCollector.GetID() + "] has a <Bound>, with non numeric Max specified.")
                    return False

            if "Action" in attributes.keys():
                Action = Alias.Alias(attributes["Action"].nodeValue)

            if Min == None and Max == None:
                Log.getLogger().error("Collector [" + objCollector.GetID() + "] has a <Bound>, with no Min or Max specified.")
                return False

            if None == Action: 
                Action = Collector.BoundAction.Set # Make it a default

            elif Action.lower() == "drop":
                Action = Collector.BoundAction.Drop

            elif Action.lower() == "set":
                Action = Collector.BoundAction.Set

            elif Action.lower() == "repeatlast":
                Action = Collector.BoundAction.RepeatLast

            else:
                Log.getLogger().error("Collector [" + objCollector.GetID() + "] has a <Bound>, with invalid Action: " + Action)
                return False

            objCollector._Bound_Min = Min
            objCollector._Bound_Max = Max
            objCollector._Bound_Action = Action

        return True

    def __CreateCollectorObject(self,node,objNamespace,MinionID,IsInGroup):
        objCollector = None
        try:
            ScriptName = Alias.Alias(node.getElementsByTagName('Executable')[0].firstChild.nodeValue)
            objCollector = Collector.Collector(objNamespace,MinionID,IsInGroup)
            objCollector._ScriptName = ScriptName

            try:
                _Which = 'Param'
                for param in node.getElementsByTagName(_Which): # Make an array of the params for the script
                    strParam = Alias.Alias(param.firstChild.nodeValue)
                    try:
                        key,value=strParam.split('=')
                        kwargs[key] = value
                        if None == objCollector._kwargs:
                            objCollector._kwargs={}
                        objCollector._kwargs[key] = value
                    except:
                        params.append(strParam)

                        Param = CollectorParam.CheckForCollectorAsParam(strParam,objNamespace)
                        objCollector._Parameters.append(Param)

            except Exception:
                pass


            try:
                OperatorType = Alias.Alias(node.getElementsByTagName('Operator')[0].firstChild.nodeValue)

                if OperatorType.lower() != "userdefined":  # want to keep going if userdefined
                    return objCollector

            except Exception as Ex:
               return objCollector


        except Exception:  # OK, wasn't a traditional collector, let's see if it is an operator
            pass

        try:
            OperatorType = Alias.Alias(node.getElementsByTagName('Operator')[0].firstChild.nodeValue)
            if OperatorType.lower() == "addition":
                objCollector = Operator.Operator_Addition(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "average":
                objCollector = Operator.Operator_Average(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "makelist":
                objCollector = Operator.Operator_MakeList(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "duplicate":
                objCollector = Operator.Operator_Duplicate(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_eq":
                objCollector = Operator.Operator_Compare_EQ(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_ne":
                objCollector = Operator.Operator_Compare_NE(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_gt":
                objCollector = Operator.Operator_Compare_GT(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_ge":
                objCollector = Operator.Operator_Compare_GE(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_lt":
                objCollector = Operator.Operator_Compare_LT(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "compare_le":
                objCollector = Operator.Operator_Compare_LE(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "greatest":
                objCollector = Operator.Operator_Greatest(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "least":
                objCollector = Operator.Operator_Least(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "maxvalue":
                objCollector = Operator.Operator_MaxValue(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "minvalue":
                objCollector = Operator.Operator_MinValue(objNamespace,MinionID,IsInGroup)
            elif OperatorType.lower() == "userdefined":
                objCollector = Operator.Operator_UserDefined(objNamespace,MinionID,IsInGroup,objCollector)

            else:
                self.HandleInvalidXML("Unknown Operator: " + OperatorType)
                return None
            try:

                retVal = self.__GetOperatorInputs(node,objCollector)
                if retVal == 0:
                    Log.getLogger().error("Operator Collector [" + MinionID + "] with no <Input>")
                    return None

                if retVal < 1:
                    return None
                
            except Exception as Ex:
                
                return None

            return objCollector

        except Exception as ex:  # OK, wasn't a traditional collector, let's see if it is an operator
            self.HandleInvalidXML("Executable | Operator")
            return None


    # go read a collector
    def __ReadCollector(self,node,objNamespace,IsInGroup):
        objCollector = None

        attributes = node.attributes
        if None == attributes:
            Log.getLogger().error("Collector defined with no ID")
            return None

        else:
            if not IsInGroup:
                Frequency = None
            else:
                Frequency = 0

            if "ID" in attributes.keys():
                MinionID = Alias.Alias(attributes["ID"].nodeValue)
                objCollector = self.__CreateCollectorObject(node,objNamespace,MinionID,IsInGroup)
                if None == objCollector:
                    return None

            else:
                Log.getLogger().error("Collector without ID attribute!")
                return None

            if "OverrideID" in attributes.keys():
                AltID = Alias.Alias(attributes["OverrideID"].nodeValue)
                if not objCollector.SetOverrideID(AltID):
                    return None

            if "OverrideNamespace" in attributes.keys():
                AltNS = Alias.Alias(attributes["OverrideNamespace"].nodeValue)
                objCollector.SetOverrideNamespaceString(AltNS)

            if "Frequency" in attributes.keys():
                if not IsInGroup:
                    Frequency = Alias.Alias(attributes["Frequency"].nodeValue)
                else:
                    Log.getLogger().warning("Collector [" + MinionID + "] specified a frequency.  Ignoring since it is in a group.")

            if "OnlySendOnChange" in attributes.keys():  #  only sends if data changes
                strBool = Alias.Alias(attributes["OnlySendOnChange"].nodeValue)
                if strBool.lower() == "true" :
                    objCollector._SendOnlyOnDelta = True

            elif "SendOnlyOnChange" in attributes.keys():  #  only sends if data changes
                strBool = Alias.Alias(attributes["SendOnlyOnChange"].nodeValue)
                if strBool.lower() == "true" :
                    objCollector._SendOnlyOnDelta = True
                    
            if "DoNotSend" in attributes.keys():  #  Collect, but do not send
                strBool = Alias.Alias(attributes["DoNotSend"].nodeValue)
                if strBool.lower() == "true" :
                    objCollector._DoNotSend = True

            if "Scale" in attributes.keys(): #Scale the data
                strVal = Alias.Alias(attributes["Scale"].nodeValue)
                if not objCollector.SetScaleValue(strVal):
                    return None

            if "ProcessThread" in attributes.keys():
                objCollector.SetProcessThreadID(Alias.Alias(attributes["ProcessThread"].nodeValue))

        if not self.__ReadBounds(node,objCollector):
            return None

        try:
            _Which = 'Normalize'
            try:
                objCollector._NormalizeValue = float(Alias.Alias(node.getElementsByTagName(_Which)[0].firstChild.nodeValue))
                objCollector._Normalize = True
                normNode = node.getElementsByTagName(_Which)[0]
                normAttributes = normNode.attributes
                if "SyncFile" in normAttributes.keys():
                    objCollector._SyncFile = Alias.Alias(normAttributes["SyncFile"].nodeValue)
            except Exception:
                pass

            _Which = 'Precision'
            try:
                strPrecision = Alias.Alias(node.getElementsByTagName(_Which)[0].firstChild.nodeValue)
                try:
                    objCollector.Precision = float(strPrecision)
                    Log.getLogger().info("Setting " + MinionID + " precision to " + strPrecision)

                except Exception as Ex:
                    self.HandleInvalidXML("Invalid <Precision>: " + strPrecision + " for " + MinionID)
                    return None

            except Exception:
                pass

            _Which = 'Frequency'

            try:
                objCollector._PollingInterval = int(Frequency)
            except Exception:
                if str(Frequency).lower() == "ondemand":
                    objCollector._OnDemand = True
                    Log.getLogger().debug("On Demand Collector Found")
                else:
                    if str(Frequency).lower() == "runonce":
                        objCollector._RunOnce = True
                    objCollector._PollingInterval = int(objNamespace._DefaultInterval)

            if not IsInGroup and not objCollector.IsOnDemand() and objCollector._PollingInterval < 1:
                Log.getLogger().error("Collector with invalid Polling Interval: " + str(objCollector._PollingInterval))
                return None
                
        except Exception as ex: 
            self.HandleInvalidXML("Error Parsing " + _Which + " for " + MinionID + ": " + str(ex))
            objCollector = None

        return objCollector

    def __ReadDynamicCollectorModifiers(self,node,objDynamicCollector):
        ID=""
        Precision = None
        Normalize = None
        Scale = None
        DoNotSend = None
        SendOnlyOnChange = None
        SyncFile = None

        attributes = node.attributes
        if "ID" in attributes.keys():
            ID = Alias.Alias(attributes["ID"].nodeValue)

        else:
            Log.getLogger().error("DynamicCollector <Modifyr> without an ID")
            return False

        if "DoNotSend" in attributes.keys():
            DoNotSend = Alias.Alias(attributes["DoNotSend"].nodeValue)

        if "SendOnlyOnChange" in attributes.keys():
            SendOnlyOnChange = Alias.Alias(attributes["SendOnlyOnChange"].nodeValue)

        if "OnlySendOnChange" in attributes.keys():
            SendOnlyOnChange = Alias.Alias(attributes["OnlySendOnChange"].nodeValue)

        if "Scale" in attributes.keys():
            Scale = Alias.Alias(attributes["Scale"].nodeValue)

        try:
            Precision = Alias.Alias(node.getElementsByTagName("Precision")[0].firstChild.nodeValue)
        except:
            pass # might want to make this an error ....

        normNodes = node.getElementsByTagName("Normalize")
        if None != normNodes and len(normNodes) > 0:
            normNode = normNodes[0]
            normAttributes = normNode.attributes
            if "SyncFile" in normAttributes.keys():
                SyncFile = Alias.Alias(normAttributes["SyncFile"].nodeValue)

            try:
                Normalize = Alias.Alias(node.getElementsByTagName("Normalize")[0].firstChild.nodeValue)
            
            except:
                pass # might want to make this an error ....

        return objDynamicCollector.AddCollectorModifier(ID,Precision,Normalize,SyncFile,Scale,DoNotSend,SendOnlyOnChange)

    # go read a collector
    def __ReadDynamicCollector(self,node,objNamespace,objGroup=None):
        objDynaCollector = None
        Prefix = ""
        Suffix = ""
        FileName = ""
        SendOnlyOnDelta = False
        DoNotSend = False
        Scale = 1
        IsInGroup = objGroup != None

        attributes = node.attributes

        if not IsInGroup:
            Frequency = None
        else:
            Frequency = objGroup._PollingInterval

        if "ID" in attributes.keys():
            Log.getLogger().error("DynamicCollector does not take an ID")
            return None

        if "Prefix" in attributes.keys():
            Prefix = Alias.Alias(attributes["Prefix"].nodeValue)

        if "Suffix" in attributes.keys():
            Suffix = Alias.Alias(attributes["Suffix"].nodeValue)

        if "Frequency" in attributes.keys():
            if not IsInGroup:
                Frequency = Alias.Alias(attributes["Frequency"].nodeValue)
            else:
                Log.getLogger().warning("DynamicCollector specified a frequency.  Ignoring since it is in a group.")


        if "OnlySendOnChange" in attributes.keys():  #  only sends if data changes
            strBool = Alias.Alias(attributes["OnlySendOnChange"].nodeValue)
            if strBool.lower() == "true" :
                SendOnlyOnDelta = True

        elif "SendOnlyOnChange" in attributes.keys():  #  only sends if data changes
            strBool = Alias.Alias(attributes["SendOnlyOnChange"].nodeValue)
            if strBool.lower() == "true" :
                SendOnlyOnDelta = True

        if "DoNotSend" in attributes.keys():  #  Collect, but do not send
            strBool = Alias.Alias(attributes["DoNotSend"].nodeValue)
            if strBool.lower() == "true" :
                DoNotSend = True

        if "Scale" in attributes.keys(): #Scale the data
            Scale = Alias.Alias(attributes["Scale"].nodeValue)

        _Which = 'File'
        try:
            FileName = Alias.Alias(node.getElementsByTagName(_Which)[0].firstChild.nodeValue)
        except Exception:
            try:
                node.getElementsByTagName('Plugin')[0].nodeValue # could be a plugin, so check before spitting out error
                FileName = None
            except:
                Log.getLogger().error("DynamicCollector specified, did not have <" + _Which + "> Tag.  Invalid.")
                return None
        if None == FileName:
            objDynaCollector = DynamicCollector.DynamicCollector(objNamespace,IsInGroup,"DynamicCollectorPlugin")
        else:
            objDynaCollector = DynamicCollector.DynamicCollector(objNamespace,IsInGroup,FileName)
            ## go see if they have defined specific tokens to use to split up a line
            defaultTokenList = ['=','= ',': ',':',' '] 

            tokenList = []
            try:
                tokenNodeList = node.getElementsByTagName('SplitToken')
                for token in tokenNodeList:
                    tokenList.append( Alias.Alias(token.firstChild.nodeValue))

            except Exception as Ex:
                tokenList = []

            if len(tokenList) < 1:
                tokenList = defaultTokenList # no tokens defined - so use default

            objDynaCollector.SetParseTokens(tokenList)

        if "OverrideNamespace" in attributes.keys():
            AltNS = Alias.Alias(attributes["OverrideNamespace"].nodeValue)
            objDynaCollector.SetOverrideNamespaceString(AltNS)

        if "ProcessThread" in attributes.keys():
            objDynaCollector.SetProcessThreadID(Alias.Alias(attributes["ProcessThread"].nodeValue))

        else:
            objDynaCollector.SetProcessThreadID(objDynaCollector.GetID())

        objDynaCollector.SetPrefix(Prefix)
        objDynaCollector.SetSuffix(Suffix)
        objDynaCollector.SetSendOnlyOnDelta(SendOnlyOnDelta)
        objDynaCollector._DoNotSend = DoNotSend
        objDynaCollector.SetScaleValue(Scale)

        _Which = 'LockFile'
        try:
            FileName = Alias.Alias(node.getElementsByTagName(_Which)[0].firstChild.nodeValue)
            objDynaCollector.SetLockfile(FileName)
        except Exception:
            pass # no problem if not defined, just move on

        nodeList = node.getElementsByTagName("Modifier")
        if None != nodeList and len(nodeList) != 0 :
            Log.getLogger().info("DynamicCollector Modifier found")
            for modifier in nodeList:
                if False == self.__ReadDynamicCollectorModifiers(modifier,objDynaCollector):
                    return None
        try:
            _Which = 'Normalize'
            
            # since <Precision> and Normalize are also valid for the ModifyCollector secions, need to make sure the used values are at right level
            tNodeList = node.getElementsByTagName(_Which)
            for tNode in tNodeList:
                if node == tNode.parentNode:
                    try:                        
                        normAttributes = tNode.attributes
                        if "SyncFile" in normAttributes.keys():
                            objDynaCollector._SyncFile = Alias.Alias(normAttributes["SyncFile"].nodeValue)

                        if None == tNode.firstChild:
                            Log.getLogger().info("SyncFile specified for DynamicCollector, but no Normilzation Factor.")
                            break
                        
                        objDynaCollector._NormalizeValue = float(Alias.Alias(tNode.firstChild.nodeValue))
                        objDynaCollector._Normalize = True 
                        break
                    except Exception:
                        self.HandleInvalidXML("Invalid <Normalize>: " + tNode.firstChild.nodeValue + " for DynamicCollector")
                        return None
            
            _Which = 'Precision'
            tNodeList = node.getElementsByTagName(_Which)
            for tNode in tNodeList:
                if node == tNode.parentNode:
                    try:     
                        strPrecision = Alias.Alias(tNode.firstChild.nodeValue)
                        objDynaCollector.Precision = int(strPrecision)
                        Log.getLogger().info("Setting DynamicCollector precision to " + strPrecision)

                        break
                    except Exception:
                        self.HandleInvalidXML("Invalid <Precision>: " + strPrecision + " for DynamicCollector")
                        return None

            _Which = 'Frequency'

            try:
                objDynaCollector._PollingInterval = int(Frequency)
            except Exception:
                if str(Frequency).lower() == "ondemand":
                    objDynaCollector._OnDemand = True
                    Log.getLogger().debug("On Demand Collector Found")
                else:
                    objDynaCollector._PollingInterval = int(objNamespace._DefaultInterval)

            if not IsInGroup and not objDynaCollector.IsOnDemand() and objDynaCollector._PollingInterval < 1:
                Log.getLogger().error("Collector with invalid Polling Interval: " + str(objDynaCollector._PollingInterval))
                return None
                
        except Exception as ex: 
            self.HandleInvalidXML("Error Parsing " + _Which + " for DynamicCollector: " + str(ex))
            objDynaCollector = None

        objDynaCollector = self.ReadUserPluginSettings(node,objDynaCollector)

        return objDynaCollector



    def ReadUserPluginSettings(self,baseNode,objDynaCollector):

#        <DynamicCollector Prefix="MyPrefix" DoNotSend="True" Frequency="1000">
#           <Modifier ID="*.x_bytes" DoNotSend="False"/>
#            <Plugin>
#                <PythonFile>Collectors/Collectd.py</PythonFile>
#                <EntryPoint SpawnThread="True">CollectionThread</EntryPoint>
#                <Param>localhost</Param>
#                <Param>3232</Param>
#            </Plugin>
#        </DynamicCollector>

        spawnThread = False
        try:
            pluginNode = baseNode.getElementsByTagName('Plugin')[0]
        except Exception as ex:
            return objDynaCollector

        try:
            PythonFile = Alias.Alias(pluginNode.getElementsByTagName('PythonFile')[0].firstChild.nodeValue)
        except Exception as ex:
            Log.getLogger().error("DynamicCollector <Plugin> section did not have <PythonFile> tag")
            return None

        try:
            functionNode = pluginNode.getElementsByTagName('EntryPoint')[0]
            functionName = Alias.Alias(functionNode.firstChild.nodeValue)
            attributes = functionNode.attributes
            if attributes != None and "SpawnThread" in attributes.keys():
                spawnStr = attributes["SpawnThread"].nodeValue
                if spawnStr.lower() == "true":
                    spawnThread = True
                elif spawnStr.lower() == "false":
                    spawnThread = False
                else:
                    Log.getLogger().error("DynamicCollector <PythonFIle> section had invalid SpawnThread value of: " + spawnStr)
                    return None
                
        except Exception as ex:
            Log.getLogger().error("DynamicCollector <Plugin> section did not have <EntryPoint> tag")
            return None

        params=[]
        kwargs={}
        try:
            _Which = 'Param'
            for param in pluginNode.getElementsByTagName(_Which): # Make an array of the params for the script
                strParam = Alias.Alias(param.firstChild.nodeValue)
                try:
                    key,value=strParam.split('=')
                    kwargs[key] = value
                except:
                    params.append(strParam)
                #Param = CollectorParam.CheckForCollectorAsParam(strParam,objNamespace)

            objDynaCollector._Parameters = params
            if len(kwargs) > 0:
                objDynaCollector._kwargs = kwargs


        except Exception:
            pass


        if not objDynaCollector.SetPluginInfo(PythonFile, functionName, spawnThread):
            objDynaCollector = None

        return objDynaCollector



    


    # go read a collector
    def __ReadGroup(self,groupNode,objNamespace):
        attributes = groupNode.attributes

        objGroup = Group.Group(objNamespace)

        if "Frequency" in attributes.keys():
            Frequency = Alias.Alias(attributes["Frequency"].nodeValue)

        else:
            Frequency = objNamespace._DefaultInterval

        try:
            objGroup._PollingInterval = int(Frequency)
        except Exception:
            if str(Frequency).lower() == "ondemand":
                objGroup._OnDemand = True
                Log.getLogger().error("A Group cannot be on Demand")
                return None
            else:
                Log.getLogger().error("An invalid frequency for a group specified: " + Frequency)
                return None

        if "OverrideNamespace" in attributes.keys():
            AltNS = Alias.Alias(attributes["OverrideNamespace"].nodeValue)
            objGroup.SetOverrideNamespaceString(AltNS)

        if "ProcessThread" in attributes.keys():
            objGroup.SetProcessThreadID(Alias.Alias(attributes["ProcessThread"].nodeValue))

        if "OnlySendOnChange" in attributes.keys():  #  only sends if data changes
            strBool = Alias.Alias(attributes["OnlySendOnChange"].nodeValue)
            if strBool.lower() == "true" :
                objGroup._SendOnlyOnDelta = True

        elif "SendOnlyOnChange" in attributes.keys():  #  only sends if data changes
            strBool = Alias.Alias(attributes["SendOnlyOnChange"].nodeValue)
            if strBool.lower() == "true" :
                objGroup._SendOnlyOnDelta = True

        if "DoNotSend" in attributes.keys():  #  Collect, but do not send
            strBool = Alias.Alias(attributes["DoNotSend"].nodeValue)
            if strBool.lower() == "true" :
                objGroup._DoNotSend = True

        if "AlwaysCollect" in attributes.keys():  #  Collect, but do not send
            strBool = Alias.Alias(attributes["AlwaysCollect"].nodeValue)
            if strBool.lower() == "false" :
                objGroup._ForceCollectionEvenIfNoUpdate = False

        for node in groupNode.childNodes:
            if node.nodeName.lower() == "#text":
                continue # don't care
            elif node.nodeName.lower() == "#comment":
                continue # don't care

            elif node.nodeName.lower() == "actor":
                continue # Handle these someplace else - read them all at once

            elif node.nodeName.lower() == "collector":
                objCollector = self.__ReadCollector(node,objNamespace,True)

                if None == objCollector:
                    return None

                if False == objGroup.AddCollector(objCollector):
                    return None

            elif node.nodeName.lower() == "group":
                objNewGroup = self.__ReadGroup(node,objNamespace)
                if None != objNewGroup:
                    objGroup.AddCollector(objNewGroup)

            elif node.nodeName.lower() == "externalfile":
               if not self.__ReadExternalFile(node,objNamespace):
                return None
               

            elif node.nodeName.lower() == "dynamiccollector":
               objDynamicCollector_InGroup = self.__ReadDynamicCollector(node,objNamespace,objGroup)

               if None == objDynamicCollector_InGroup:
                   return False

               objDynamicCollector_InGroup.SetGroup(objGroup)

               if not objGroup.AddCollector(objDynamicCollector_InGroup):
                   return False

            else:
                Log.getLogger().error("Unknown Tag:" + node.nodeName)
                return None
        return objGroup

   #Fetches the Namespaces, and each of the collectors in the Namespaces
    def __ReadNamespaces(self):
        Namespaces = []

        if not self.__ReadAliasList(self.m_dom):
            return False
        #go through every Namespace and create an Namespace Class for each, and
        #put all in an array
        for inst in self.m_dom.getElementsByTagName('Namespace'):
            _Which = 'Name'  
            try:            
                ID = Alias.Alias(inst.getElementsByTagName(_Which)[0].firstChild.nodeValue)

                _Which = 'TargetConnection'
                node = inst.getElementsByTagName(_Which)[0]
                _Which = 'IP'
                TargetIP = Alias.Alias(node.attributes["IP"].nodeValue)
                _Which = 'PORT'
                TargetPort = Alias.Alias(node.attributes["PORT"].nodeValue)

                _Which = 'DefaultFrequency'
                Interval = Alias.Alias(inst.getElementsByTagName(_Which)[0].firstChild.nodeValue)
                
                try:
                    _Which = 'DefaultPrecision'
                    Precision = Alias.Alias(inst.getElementsByTagName(_Which)[0].firstChild.nodeValue)
                    try:
                        Precision = int(Precision)   

                    except Exception as Ex:
                        Log.getLogger().error(str(Ex))
                        self.HandleInvalidXML(_Which)
                        return

                except:
                    Precision = self._DefaultNamespacePrecision # the default
                    pass

                objNamespace = Namespace.Namespace(ID,TargetIP,TargetPort,Interval)
                objNamespace.setDefaultPrecision(Precision)

            except Exception as Ex: 
                Log.getLogger().error(str(Ex))
                self.HandleInvalidXML(_Which)
                return

            nodeList = inst.getElementsByTagName("IncomingConnection")
            if None != nodeList and len(nodeList) > 0:
                attributes = nodeList[0].attributes
                if "IP" in attributes:
                    objNamespace.__ListenIP = Alias.Alias(attributes["IP"].nodeValue)
                
                if "PORT" in attributes:
                    try:
                        objNamespace.__ListenPort = int(Alias.Alias(attributes["PORT"].nodeValue))
                    except Exception as Ex:
                        Log.getLogger().error(str(ex))
                        Log.getLogger().error("Invalid Port set for Incoming Connection")
                        return None
            
            if not self.ReadGoodiesFromFile(inst,objNamespace):
                return

            Namespaces.append(objNamespace)

        self.Valid = True
        return Namespaces

    def ReadGoodiesFromFile(self,baseNode,objNamespace,ThreadID=None):

        self.ReadActors(baseNode,objNamespace) # go read the actors

        for node in baseNode.childNodes:
            if node.nodeName.lower() == "defaultfrequency":
                continue # don't care
            elif node.nodeName.lower() == "targetconnection":
                continue # don't care
            elif node.nodeName.lower() == "#text":
                continue # don't care
            elif node.nodeName.lower() == "name":
                continue # don't care
            elif node.nodeName.lower() == "#comment":
                continue # don't care
            elif node.nodeName.lower() == "aliaslist":
                continue # don't care
            elif node.nodeName.lower() == "defaultprecision":
                continue # don't care

            elif node.nodeName.lower() == "collector":
                objCollector = self.__ReadCollector(node,objNamespace,False)

                if None == objCollector:
                    return False

                if None != ThreadID:
                    if objCollector.GetProcessThreadID() != 'Default' and objCollector.GetProcessThreadID() != ThreadID:
                        Log.getLogger().warning("You specified Collector with ID: " + objCollector.GetID() + " to have a ProcessThread as an XML attribute that does not match the <ProcessThread> Tag it is within.")
                    else:
                        objCollector.SetProcessThreadID(ThreadID)

                if False == objNamespace.AddCollector(objCollector):
                    return False

            elif node.nodeName.lower() == "group":
                objGroup = self.__ReadGroup(node,objNamespace)
                if None == objGroup:
                    return False

                if None != ThreadID:
                    objGroup.SetProcessThreadID(ThreadID)

                if not objNamespace.AddCollector(objGroup):
                    return False

            elif node.nodeName.lower() == "externalfile":
               if not self.__ReadExternalFile(node,objNamespace,ThreadID):
                   return False

            elif node.nodeName.lower() == "actor":
                continue # Handle these someplace else - read them all at once


            elif node.nodeName.lower() == "dynamiccollector":
               objDyna = self.__ReadDynamicCollector(node,objNamespace)
               if None == objDyna:
                   return False

               if None != ThreadID:
                   if objDyna.GetProcessThreadID() != 'Default' and objDyna.GetProcessThreadID() != ThreadID:
                       Log.getLogger().warning("You specified DynamicCollector with ID: " + objDyna.GetID() + " to have a ProcessThread as an XML attribute that does not match the <ProcessThread> Tag it is within.")
                   else:
                       objDyna.SetProcessThreadID(ThreadID)

               
               if not objNamespace.AddCollector(objDyna):
                   return False

            elif node.nodeName.lower() == "processthread":
                if 'Name' in node.attributes:
                    newThreadID = Alias.Alias(node.attributes["Name"].nodeValue)
                    if not self.ReadGoodiesFromFile(node,objNamespace,newThreadID):
                        return False
                else:
                    Log.getLogger().error("<ProcessThread> found without Name.")
                    return False


            else:
                Log.getLogger().error("Unknown Tag:" + node.nodeName)
                return False
        return True

    def ReadActors(self,NamespaceNode,objNamespace):
        actorList = None
        # go through each collector and create a new Collector object, and add
        # to an array in the Namespace
        for actor in  NamespaceNode.getElementsByTagName('Actor'):
            objActor = Actor.Actor()
            attributes = actor.attributes
            if None == attributes:
                Log.getLogger().error("Actor defined with no ID")
                return None

            else:
                Frequency = None
                if "ID" in attributes.keys():
                    objActor.ID = Alias.Alias(attributes["ID"].nodeValue)
                else:
                    Log.getLogger().error("Actor without ID attribute!")
                    return None

            try:
                #_Which = 'Executable' # this is the local script to run to
                #gather the info
                _Which = 'Executable'
                objActor.ExecutableName = Alias.Alias(actor.getElementsByTagName(_Which)[0].firstChild.nodeValue)
                    
                _Which = 'Param'
                for param in actor.getElementsByTagName(_Which): # Make an array of the params for the script
                    strParam = Alias.Alias(param.firstChild.nodeValue)
                    try:
                        key,value=strParam.split('=')
                        kwargs[key] = value
                        if None == objCollector._kwargs:
                            objCollector._kwargs={}
                        objCollector._kwargs[key] = value
                    except:
                        Param = CollectorParam.CheckForCollectorAsParam(strParam,objNamespace)
                        objActor.Parameters.append(Param)

            except Exception as Ex: 
                Log.getLogger().error(str(Ex))
                self.HandleInvalidXML(_Which)
                return None

            if None == actorList:
                actorList = []

            objNamespace.AddActor(objActor)

        return actorList


    def __ReadAliasList(self,doc):
        for aliasList in doc.getElementsByTagName('AliasList'):
            attributes = aliasList.attributes
            if None != attributes and "File" in attributes.keys():
                if not Alias.AliasMgr.LoadExternalAliasFile(attributes["File"].nodeValue):
                    return False

            for tag in  aliasList.getElementsByTagName('Alias'):
                attributes = tag.attributes
                if None != attributes:
                    for attrName,attrValue in attributes.items():
                        Alias.AliasMgr.AddAlias(attrName,Alias.Alias(attrValue))

        return True


    def __ReadExternalFile(self,node,objNamespace,ThreadID=None):
        filename = Alias.Alias(node.firstChild.nodeValue)
        Log.getLogger().info("Loading External file: " + filename)
        try:
            file = open(filename,'r')
            data = file.read()
            file.close()
        except Exception:
            Log.getLogger().error("Invalid external file: " + filename)
            return False

        try:
            objdom = xml.dom.minidom.parseString(data)
        except Exception:
            BaseFN = self._ConfigFilename
            self._ConfigFilename = filename
            self.HandleInvalidXML("Bad Content - XML error")
            self._ConfigFilename = BaseFN
            return False

        attributes = node.attributes
        Alias.AliasMgr.Push()

        for aliasKey in attributes.keys():
            strAlias = Alias.Alias(attributes[aliasKey].nodeValue)
            Alias.AliasMgr.AddAlias(aliasKey,strAlias)

        try:
            node = objdom.getElementsByTagName("ExternalMinionFile")[0]
        except Exception:
            self.HandleInvalidXML("Bad Content - root must be -->ExternalMinionFile")
            return False

        if not self.__ReadAliasList(node): # read aliases defined in external file
            return False

        if not self.ReadGoodiesFromFile(node,objNamespace,ThreadID):
            return False

        Alias.AliasMgr.Pop()

        return True
       
def GetMaxTransmitBufferBeforeRest():
    ThreadCount = Namespace.GetActiveProcessingThreadCount()
    if ThreadCount < 1:
        ThreadCount = 1
    return Configuration._MaxTxBeforeRest / ThreadCount

def GetTimesToRepeatPacket():
    return Configuration._RepeatPktCnt

def GetNamespace(strNamespaceID):
    retObj = None
    if None != Configuration._Instance:
        for ns in Configuration._Instance.GetNamespaces():
            if ns.GetID().lower() == strNamespaceID.lower():
                retObj = ns
                break

    return retObj


