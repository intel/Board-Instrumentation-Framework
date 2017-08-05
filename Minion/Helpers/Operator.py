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
#       Contains all the Operator functionality
##############################################################################

from Helpers import Log
from Util import Sleep
from Util import Time
from Util import Utility
from Helpers import ThreadManager
from Helpers import Worker
from Helpers import Collector
from decimal import Decimal

class ConstantCollector(Collector.Collector):
    def __init__(self,ID, DefaultValue):
        self._MinionID = ID
        self._OverrideID = ID
        self.IsDefaultValue = False
        if None != DefaultValue:
            self._LastSentValue = DefaultValue
            self.IsDefaultValue = True
        else:
            self._LastSentValue = ID

        self._ReadyForConsumptionByAnother = True

    def GetLastElapsedTimePeriod(self):
        return Time.GetCurrMS()


# Helper class to hold the information of the script or app it will call to
# gather some kind of data
class Operator(Collector.Collector):
    def __init__(self,objNamespace,ID,InGroup):
        Collector.Collector.__init__(self,objNamespace,ID,InGroup)
        self._InputList = []
        self._Collectors = None
        self._ConstantCollectorsList = []
        self._InvalidInpWarningSent = False


    def AddInput(self,CollectorID,isConstant,constVal=None):
        if isConstant:
            objConstant = ConstantCollector(CollectorID,constVal)
            self._ConstantCollectorsList.append(objConstant)

        #objCollector = self._NamespaceObject.GetCollector(CollectorID)
        if None == constVal: # constVal != None, then they specified DefaultValue attribute, and we handle it
            self._InputList.append(CollectorID) # differently, because we only want def value to be used until input is valid

        return True

    def GetCollectors(self):
        incomplete=False
        list = []
        
        if None == self._Collectors:
            for CollectorID in self._InputList:
                objCollector = self._NamespaceObject.GetCollector(CollectorID) # is it a real collector that has been created yet
                if None == objCollector:    # nope, let's see if it has a constant val instead
                    for objCollector in self._ConstantCollectorsList:
                        if CollectorID == objCollector.GetID():
                            if objCollector.IsDefaultValue:
                                incomplete=True
                            break

                if None == objCollector:
                    if False == self._InvalidInpWarningSent:
                        Log.getLogger().warn("Operator with invalid/unknown <Input>: " + CollectorID + ". Ignoring until it becomes valid.")
                        self._InvalidInpWarningSent = True

                    list = []
                    break
                
                if not objCollector._ReadyForConsumptionByAnother:
                    objCollector = None
                    for objCol in self._ConstantCollectorsList:
                        if CollectorID == objCol.GetID():
                            if objCol.IsDefaultValue:
                                incomplete=True
                            objCollector = objCol
                            break

                    if None == objCollector:
                        if False == self._InvalidInpWarningSent:
                            Log.getLogger().warn("Operator with invalid/unknown <Input>: " + CollectorID + ". Ignoring until it becomes valid.")
                            self._InvalidInpWarningSent = True
                        list = []
                        incomplete = True
                        break

                list.append(objCollector)

            #if False == incomplete:
            ##    self._Collectors = list # made it through, have all of them!

        else:
            list = self._Collectors

        return list

    def Collect(self):
        raise Exception("Patrick forgot to override the Collect() method on an Operator")
        pass


#will sum other collectors
class Operator_Addition(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        total = 0
        for collector in self.GetCollectors():
            try:
                total = total + float(collector.GetLastValue())
            except Exception:
                if not self._InvalidInpWarningSent:
                    self._InvalidInpWarningSent = True
                    return "Operator Addition Collectors are only valid for collectors that collect a numeric value"
                else:
                    return self.ErrorValue

        return str(total)

#will average the data from other collectors, or a single collector
class Operator_Average(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        total = 0
        list = self.GetCollectors()
        if len(list) < 1:
            return str(0)

        for collector in list:
            try:
                val = float(collector.GetLastValue())
                if collector.ReadyForConsumption():
                    total = total + val

            except Exception as Ex:
                if not self._InvalidInpWarningSent:
                    self._InvalidInpWarningSent = True
                    return "Operator Average Collectors are only valid for collectors that collect a numeric value:-->  " + str(collector.GetLastValue())
                else:
                    return self.ErrorValue
        
        if len(list) > 1:
            if total != 0:		
                avg = total / len(list)
            else:
                avg = 0			

        else: #just average this collector with itself
            if not hasattr(self, "historyList"):
                self.historyList = []     

            if len(self.historyList) > 10:  # keep a bunch of samples for average, could make this configurable I suppose
                del self.historyList[0]

            self.historyList.append(total) 

            listTotal = 0
            for val in self.historyList:
                listTotal += val

            if listTotal != 0:
                avg = listTotal / len(self.historyList)
            else:
                avg = 0

        return str(avg)

#will make a list of data from other collectors
class Operator_MakeList(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        total = ""
        first = True
        for collector in self.GetCollectors():
            if first == True:
                total = total + str(collector.GetLastValue()) # 1st one doesnt' have a comma before it
                first = False
            else:
                total = total + "," + str(collector.GetLastValue())

        return total

# will simply send the value collected from another collector
class Operator_Duplicate(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)
        self._WarningSent = False

    #override this for duplicate, as could be off by quite some time
    #if the operator is called significantly later than the dynamic collector
    #or other collector associated with ti
    def GetElapsedTimeSinceLast(self):
        if None == self._SyncFile: 
            dupCol = self.GetCollectors()
            if len(dupCol) > 0:
                elapsedTime =  dupCol[0].GetLastElapsedTimePeriod()
            else:
                elapsedTime = 0

        else:
            elapsedTime = self.GetTimeMS() - self._LastCollectionTime 

        return elapsedTime

    def Collect(self):
        list = self.GetCollectors()
        
        if len(list) !=1 :
            if len(list) > 1 and not self._WarningSent:
                Log.getLogger().warn("Duplicate Operator given more than 1 collector to duplicate.  Only sending the 1st one.")

            self._WarningSent = True
            return "Invalid Duplicate Operator"
                
        #if collector.ReadyForConsumption():
        return list[0].GetLastValue()

        #return None
        
class Operator_Compare_EQ(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)
        self._Value1 = None
        self._Value2 = None
        self._If = None
        self._Else = None
        self._ReallyDoNotSend = False
        self._Verified = False

    def Value1(self):
        return self._Value1.GetLastValue().strip()

    def Value2(self):
        return self._Value2.GetLastValue().strip()

    def _VerifyInput(self,CompareType):
        if self._Verified:
            return True
        
        collectors = self.GetCollectors()
        if len(collectors) == 0:
            return "Operator -Input- still pending"

        if len(collectors) < 3:
            if not self._WarningSent:
                Log.getLogger().warn(CompareType + " Operator does not have enough input.")
                return 'Invalid " + CompareType + " Configuration'

        if len(collectors) > 4:
                Log.getLogger().warn(CompareType + " Operator has too many inputs")
                return 'Invalid " + CompareType + " Configuration'

        self._Value1 = collectors[0]
        self._Value2 = collectors[1]

        self._If = collectors[2]

        if len(collectors) == 4:
            self._Else = collectors[3]

        self._ReallyDoNotSend = self._DoNotSend

        if None != self._Collectors:
            self._Verified = True

        return True

    def _Perform(self,compareResult):
        if compareResult:
            self._DoNotSend = self._ReallyDoNotSend
            return self._If.GetLastValue() #if

        if None != self._Else:
            self._DoNotSend = self._ReallyDoNotSend
            return self._Else.GetLastValue()

        self._DoNotSend = True # do nothing
        return str(compareResult)

    def Collect(self):
        valid = self._VerifyInput("Compare_EQ")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) == float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() == self.Value2())

class Operator_Compare_NE(Operator_Compare_EQ):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator_Compare_EQ.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        valid = self._VerifyInput("Compare_NE")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) != float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() != self.Value2())

class Operator_Compare_GT(Operator_Compare_EQ):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator_Compare_EQ.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        valid = self._VerifyInput("Compare_GT")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) > float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() > self.Value2())

class Operator_Compare_GE(Operator_Compare_EQ):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator_Compare_EQ.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        valid = self._VerifyInput("Compare_GE")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) >= float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() >= self.Value2())

class Operator_Compare_LT(Operator_Compare_EQ):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator_Compare_EQ.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        valid = self._VerifyInput("Compare_LT")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) < float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() < self.Value2())

class Operator_Compare_LE(Operator_Compare_EQ):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator_Compare_EQ.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        valid = self._VerifyInput("Compare_LE")
        if valid != True:
            return valid #is an error string

        try: # try numeric compare first
            return self._Perform(float(self.Value1()) <= float(self.Value2()))
        except Exception:
            pass

        return self._Perform(self.Value1() <= self.Value2())

def is_number(strNumber):
    for char in strNumber:
        if char == ' ':  # Have to do this because the float line below doesn't care about spaces
            return False
    try:
        float(strNumber)   
        return True
    except ValueError:
        return False

class Operator_Greatest(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        if len(self.GetCollectors()) < 2:
            if not self._WarningSent:
                Log.getLogger().warn("Greatest Operator must have at least 2 inputs")
                return "Greatest Operating has insufficent Inputs"
        greatest = self.GetCollectors()[0].GetLastValue()

        for collector in self.GetCollectors():
            val = collector.GetLastValue()
            if is_number(greatest) and is_number(val): # is numeric
                if float(greatest) < float(val):
                    greatest = val

            elif greatest < collector.GetLastValue():  # is string compare
                greatest = collector.GetLastValue()

        return greatest

class Operator_Least(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    def Collect(self):
        if len(self.GetCollectors()) < 2:
            if not self._WarningSent:
                Log.getLogger().warn("Least Operator must have at least 2 inputs")
                return "Least Operating has insufficent Inputs"
        least = self.GetCollectors()[0].GetLastValue()
        for collector in self.GetCollectors():
            val = collector.GetLastValue()
            if is_number(least) and is_number(val): # is numeric
                if float(least) > float(val):
                    greatest = val

            elif least > collector.GetLastValue():  # is string compare
                least = collector.GetLastValue()

        return least


class Operator_MaxValue(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    ### Only used from ManipulateCollector ###
    def SetMaxValueForAll(self,newValue):
        list = self.GetCollectors()
        if len(list) < 1:
            return None
        try:
            for objCollector in list:
                if hasattr(objCollector, "MaxCollectedValue"):
                    objCollector.MaxCollectedValue = newValue

        except Exception as Ex:
            pass

    def Collect(self):
        list = self.GetCollectors()
        if len(list) < 1:
            return None
        try:
            for objCollector in list:
                if not hasattr(objCollector, "MaxCollectedValue"):
                    objCollector.MaxCollectedValue = objCollector.GetLastValue()

            if not Utility.IsNumeric(list[0].MaxCollectedValue):
                if not self._InvalidInpWarningSent:
                    Log.getLogger().warn("An Input to Operator MaxValue is non numeric.--> " + objCollector.MaxCollectedValue)
                    self._InvalidInpWarningSent = True
                return "HelenKeller"

            max = float(list[0].MaxCollectedValue)
            for objCollector in list:
                if not Utility.IsNumeric(objCollector.MaxCollectedValue):
                    return None

                val = float(objCollector.MaxCollectedValue)
                if not objCollector.ReadyForConsumption() and not objCollector.IsDefaultValue():
                    
                    return None # hasn't yet collecte

                if val > max:
                    max = val

        except Exception as Ex:
            if not self._InvalidInpWarningSent:
                Log.getLogger().warn("An Input to Operator MaxValue is non numeric.--> " + objCollector.MaxCollectedValue)
                self._InvalidInpWarningSent = True
            max = ""

        return str(max)


class Operator_MinValue(Operator):
    def __init__(self,objNamespace,ID,InGroup=False):
        Operator.__init__(self,objNamespace,ID,InGroup)

    ### Only used from ManipulateCollector ###
    def SetMinValueForAll(self,newValue):
        list = self.GetCollectors()
        if len(list) < 1:
            return None
        try:
            for objCollector in list:
                if hasattr(objCollector, "MinCollectedValue"):
                    objCollector.MinCollectedValue = newValue

        except Exception as Ex:
            pass

    def Collect(self):

        list = self.GetCollectors()
        if len(list) < 1:
            return None
        try:
            for objCollector in list:
                    if not hasattr(objCollector, "MinCollectedValue"):
                        objCollector.MinCollectedValue = objCollector._LastValue

            min = float(list[0].MinCollectedValue)
            for objCollector in list:
                val = float(objCollector.MinCollectedValue)
                if not objCollector.ReadyForConsumption():
                    return "" # hasn't yet collected

                if val < min:
                    min = val

        except Exception as Ex:
            Log.getLogger().warn("An Input to Operator MinValue is non numeric.")
            min = ""

        return str(min)
        


# will call a user defined script (a collector) with normal <Param>s as well as
# <Inputs>
class Operator_UserDefined(Operator):
    def __init__(self,objNamespace,ID,InGroup, objCollector):
        Operator.__init__(self,objNamespace,ID,InGroup)
        self._WarningSent = False
        self._objNormalCollector = objCollector
        self.__Verify()

    def __Verify(self):
        if None == self._objNormalCollector:
            Log.getLogger().error("Invalid UserDefined Operator, no collector defined")
            return None

        if None == self._objNormalCollector._ScriptName:
            Log.getLogger().error("Invalid UserDefined Operator, no <Excecutable>  defined")
            return None

        return self

    def Collect(self):
        origParams = list(self._objNormalCollector._Parameters)  # Save orig params (are static)
        newParams = self._objNormalCollector._Parameters
        for inputCollector in self.GetCollectors():
            newParams.append(str(inputCollector.GetLastValue()))  # Make new list that is old params, plus <Inputs>

        self._objNormalCollector._Parameters = newParams

        collectedVal = self._objNormalCollector.Collect()  # go use normal collector, with all <Param> and Inputs!

        self._objNormalCollector._Parameters = origParams

        return collectedVal





