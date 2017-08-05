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
#    App Statistics
#
##############################################################################
from Helpers import  Log
from Util import Time

class Statistics(object):
    _instance = None
    def __init__(self):
        if Statistics._instance == None: # singleton pattern
            Statistics._instance = self
            self.__initialize()

        else:
            self = GetStatistics()
    
    def __initialize(self):
        self._TotalPacketsUpstream = 0  # incremented for each packet sent
        self._UniquePacketsUpstream = 0 # incremented once regardless of # of clents sent to
        self._TotalPacketsDownstream = 0
        self._UniquePacketsDownstream = 0
        self._totalTxBytesUpstream = 0
        self._totalTxBytesDownstream = 0
        self._totalRxBytesUpstream = 0
        self._totalRxBytesDownstream = 0
        self._TotalRxPacketsFromUpstream = 0
        self._TotalRxPacketsFromDownstream = 0
        self._TotalChainedDownstreamPackets = 0
        self._TotalMalformedPacketsReceived = 0
        self._TotalOscarTasksReceived = 0
        self._TotalMinionTasksReceived = 0
        self._TotalPacketsDropped = 0
        self._TotalLocalOscarTasksRecieved=0
        self._TotalMarvinTasksReceived = 0
        self._TotalShuntedPackets = 0

    def OnMarvinTaskReceived(self):
        self._TotalMarvinTasksReceived += 1

    def OnPacketShunted(self):
        self._TotalShuntedPackets +=1

    def OnPacketChainedDownstream(self,buffer):
        self._TotalChainedDownstreamPackets +=1

    def OnLocalOscarTaskReceived(self):
        self._TotalLocalOscarTasksRecieved +=1

    def OnOscarTaskReceived(self):
        self._TotalOscarTasksReceived +=1

    def OnMinionTaskReceived(self):
        self._TotalMinionTasksReceived +=1

    def OnPacketDropped(self,numberDropped=1):
        self._TotalPacketsDropped += numberDropped

    def OnMalformedPacketReceived(self,Msg=None):
        self._TotalMalformedPacketsReceived +=1
        if None != Msg:
            Log.getLogger().info(Msg)

    def OnPacketChainedUpstream(self):
        self._TotalChainedDownstreamPackets +=1

    def OnPacketBroadcastUpstream(self):
        self._UniquePacketsUpstream +=1

    def OnPacketSentUpstream(self,packet):
        self._TotalPacketsUpstream +=1
        self._totalTxBytesUpstream += len(packet)

    def OnPacketBroadcastDownstream(self):
        self._UniquePacketsDownstream +=1

    def OnPacketSentDownstream(self,packet):
        self._TotalPacketsDownstream +=1
        self._totalTxBytesDownstream += len(packet)

    def OnPacketReceivedFromDownstream(self,packet):
        self._totalRxBytesDownstream+= len(packet)
        self._TotalRxPacketsFromDownstream +=1

    def OnPacketReceivedFromUpstream(self,packet):
        self._totalRxBytesUpstream += len(packet)
        self._TotalRxPacketsFromUpstream += 1

def GetStatistics():
    if Statistics._instance == None:
        return  Statistics()
    return Statistics._instance



