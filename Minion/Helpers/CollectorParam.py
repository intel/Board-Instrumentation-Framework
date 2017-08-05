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
#    Wrapper allowing a Param to be data from a collector, rather than a static
#    string.
##############################################################################
 
from Helpers import Log
from Helpers import Namespace


class CollectorParam:
    def __init__(self, ID, Namespace):
        self._ID=ID
        self._Namespace=Namespace

    def __str__(self):
        collector = self._Namespace.GetCollector(self._ID)
        if None == collector:
            Log.getLogger().warn("<Input> specified a collector, but ID for collector is invalid:" + self._ID)
            return "Invalid input collector ID"

        return str(collector._LastSentValue)

def CheckForCollectorAsParam(input,objNamespace):
    orig = input
    index = input.find("@(")        #Collector ID is surrounded by -->@( ) <--
    stopIndex = input.find(")")
    if index != -1 and stopIndex != -1:
        # found a Collector as an input
        return CollectorParam(input[index+2:stopIndex],objNamespace)

    return input # was just a string

