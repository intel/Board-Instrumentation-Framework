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
#    Helper class to hold incoming data
#
##############################################################################
from Util import Time
class Entry():
    def __init__(self,ID,Value,ElapsedTime,Namespace,ReceivedTime="NotFromFile"):
        self.Value = Value
        self.ArrivalTime = ElapsedTime
        self.Namespace = Namespace
        self.ID = ID
#        self.PacketNumber = PacketNumber
#        self.Normalized = False
        if ReceivedTime == "NotFromFile":
            #self.ArrivalTime = TimeUtils.GetTimeDeltaMS()
            self.ArrivalTime = Time.GetCurrMS()
        else:
            self.ArrivalTime = ReceivedTime
