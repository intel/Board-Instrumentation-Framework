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
#    Canned collector to go get some basic network stats 
#
##############################################################################

# This 'Collector' Script uses psutil library to get several kinds of data
# See http://code.google.com/p/psutil/ for details.

import psutil
import re
import sys
from Collectors import CPU

#Helper routine, uses psutil to read network counters from the specified device - like 'eth0'
def GetNetworkData(device):
    try:
        netData = psutil.network_io_counters(pernic=True)
    except Exception :
        netData = psutil.net_io_counters(pernic=True)  # they changed the API call!

    try:
        devData = str(netData[device])
        pattern = re.compile("\=(.*?)\,") 
        stats = re.findall(pattern, devData)
        return stats
    except Exception as ex:
        retStr = device + " is not a known Network device."
        return [retStr,retStr]  #return an array with error, because the callers are looking for an array

    
#returns Rx Bytes/Sec from specified device
def GetNetworkRx(device): #like eth0
    if CPU.Is_PSUTIL_Installed("GetNetworkRx"):
        stats = GetNetworkData(device)
        return stats[1]
    return "PSUTIL not installed"

#returns Tx Bytes/Sec from specified device
def GetNetworkTx(device):
    if CPU.Is_PSUTIL_Installed("GetNetworkTx"):
        stats = GetNetworkData(device)
        return stats[0] 
    return "PSUTIL not installed"

#returns Bi-directional Bytes/Sec from specified device
def GetNetworkBx(device):
    if CPU.Is_PSUTIL_Installed("GetNetworkTx"):
        stats = GetNetworkData(device)
        return str(int(stats[0]) + int(stats[1]))
    return "PSUTIL not installed"

