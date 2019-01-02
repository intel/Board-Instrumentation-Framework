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
#       Gathers CPU info
##############################################################################


# This 'Collector' Script uses psutil library to get several kinds of data
# See http://code.google.com/p/psutil/ for details.

try:
    import psutil #requires psutil 3rd party library (google it)
except Exception:
    pass
import re
import sys

# Checks to see if PSUTIL is installed or not
def Is_PSUTIL_Installed(collectorName):
    if not hasattr(Is_PSUTIL_Installed,"Checked"): #kewl way of doing static local vars
        Is_PSUTIL_Installed.Checked = False
        Is_PSUTIL_Installed.Value = True

    if False == Is_PSUTIL_Installed.Checked:
        Is_PSUTIL_Installed.Checked = True
        try:
            psutil.cpu_percent(.01,percpu=False)
        except Exception as ex:
            Is_PSUTIL_Installed.Value = False
            print("ERROR ** psutil library not installed - required for built in collector " + collectorName +"  **")

    return Is_PSUTIL_Installed.Value

#Tell it which core
def GetCPU_Core_Percentage(which):
    if Is_PSUTIL_Installed("GetCPU_Core_Percentage"):
        which = int(which)
    
        stats = psutil.cpu_percent(.25,percpu=True)

        if len(stats) < which:  # in case you specify a core that doesn't exist on this system!
            return str("Invalid Core [" + str(which) +"] Requested.  # available is: " + str(len(stats)))

        try:
            return str(stats[which])

        except Exception as ex: #should not get here, but better safe than sorry
            return "0.0"

    return "PSUTIL not installed"

#Gets overall CPU
def GetCPU_Percentage():
    if Is_PSUTIL_Installed("GetCPU_Core_Percentage"):
        return str(psutil.cpu_percent(.25,percpu=False))
    return "PSUTIL not installed"

#Tell it which core
def GetCPU_Core_PercentageList(startCore,count):
    if Is_PSUTIL_Installed("GetCPU_Core_Percentage"):
        startCore = int(startCore)
        count = int(count)
    
        stats = psutil.cpu_percent(.25,percpu=True)

        if len(stats) < startCore + count:  # in case you specify an invalid range!
            return str("Invalid Core Range")

        strRet = ""
        first = True
        for core in range(startCore,startCore+count):
            if False == first:
                strRet = strRet + "," # only put a comma at end if not the first entry
            else:
                first = False;
            try:
                strRet = strRet+ str(stats[core])

            except Exception as ex: #should not get here, but better safe than sorry
                return "Error ocurred in GetCPU_Core_PercentageList"

        return strRet

    return "PSUTIL not installed"
