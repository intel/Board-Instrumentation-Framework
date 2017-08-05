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
#       Some 'back-door' stuff to reset some Operators with a history
##############################################################################

from Helpers import Namespace

def ZeroOutAverageOperatorHistory(NamespaceID, CollectorID):
    try:
        objNamespace = Namespace.GetNamespace(NamespaceID)
        if None != objNamespace:
            objCollector = objNamespace.GetCollector(CollectorID)
            if None != objCollector:
                if hasattr(objCollector, "historyList"):
                    objCollector.historyList = []  

    except Exception as Ex:
        pass


def SetMaxValueOperatorValue(NamespaceID, CollectorID, newValue):
    try:
        objNamespace = Namespace.GetNamespace(NamespaceID)
        if None != objNamespace:
            objCollector = objNamespace.GetCollector(CollectorID)
            if None != objCollector:
                if hasattr(objCollector, "SetMaxValueForAll"):
                    objCollector.SetMaxValueForAll(newValue)

    except Exception as Ex:
        pass

## EXAMPLE ###
#### Marvin #####
   # <TaskList ID="ResetMin">
   #     <TaskItem Type="Minion">
   #         <Actor ID="ResetMin" Namespace="DemoNamespace"/> 
			#<Param>DemoNamespace</Param>
			#<Param>Min</Param>
			#<Param>12</Param>
   #     </TaskItem>
   # </TaskList>

#### Minion #####
    #<Actor ID="ResetMin">
    #  <Executable>Collectors/ManipulateCollector.py</Executable>
    #  <Param>SetMinValueOperatorValue</Param>
    #</Actor>

def SetMinValueOperatorValue(NamespaceID, CollectorID, newValue):
    try:
        objNamespace = Namespace.GetNamespace(NamespaceID)
        if None != objNamespace:
            objCollector = objNamespace.GetCollector(CollectorID)
            if None != objCollector:
                if hasattr(objCollector, "SetMinValueForAll"):
                   objCollector.SetMinValueForAll(newValue)

    except Exception as Ex:
        pass