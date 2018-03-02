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
#       Send a bunch of collected data within a single packet
##############################################################################

from Helpers import Log
from Util import Sleep
from Util import Time
from Helpers import ThreadManager
from Helpers import Worker
from Helpers import Collector
from Helpers import Namespace

# Helper class to hold the information of the script or app it will call to gather some kind of data
class Group(Collector.Collector):
    ID = 1
    def __init__(self,objNamespace):
        Collector.Collector.__init__(self,objNamespace,"Group:" + str(Group.ID))
        Group.ID += 1
        self._CollectorList = []
        self._ForceCollectionEvenIfNoUpdate=True

    def AddCollector(self,objCollector,beforeID=None):
        if True == self._NamespaceObject.AddCollector(objCollector,beforeID):
            # Dynamic Collectors should be inserted right AFTER the DynamicCollector collector, otherwise if appended to the end, operators that use data from
            # a dynamic collector will be run using stale data
            if None == beforeID: 
                self._CollectorList.append(objCollector) 

            else:
                Namespace.InsertAfterInList(self._CollectorList,beforeID, objCollector)

            return True

        return False

    def PerformCollection(self):
        #Get collected time after collection, can't be sure each collection take same amount of time
        currMS = Time.GetCurrMS()
        elapsedTime = currMS - self._LastCollectionTime
        self._LastCollectionTime = currMS 

        retValue = None
        collectedGroupData=""
        for collector in self._CollectorList:
            id = collector.GetID()
            if self._ForceCollectionEvenIfNoUpdate or collector.NeedsCollecting(): # by default a group ALWAYS collects, but can override that with AlwaysCollect="False" as group attribute
                collectorData = collector.PerformCollection()
                if None != collectorData:
                    collectedGroupData += collectorData

        if len(collectedGroupData) > 1:
            retValue ="<MinionGroup>" + collectedGroupData + "</MinionGroup>"

        if self._RefreshRequested:
            self._RefreshRequested = False

        return retValue



