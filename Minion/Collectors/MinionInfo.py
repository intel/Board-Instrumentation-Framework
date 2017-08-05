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
#    Kind of a place to pu miscelaneous collectors that I've not figured out
#    where else to put yet.
#
##############################################################################
import time
from Helpers import Namespace

# For debugging, returns last actor information called
def LatestTaskInfo(NamespaceID):
    try:
        objNamespace = Namespace.GetNamespace(NamespaceID)
        if None != objNamespace:
            return objNamespace.GetLastActorCalled()

    except Exception as Ex:
        pass


# Simply returns how long in seconds Minion has been running
def MinionUptime():
    if not hasattr(MinionUptime, "uptime_start"):
        MinionUptime.uptime_start = __GetCurrentTime() 

    seconds = round(__GetCurrentTime() - MinionUptime.uptime_start)
    strTime = ''
    for scale in 86400, 3600, 60:
        result, seconds = divmod(seconds, scale)
        result = (int)(result)
        seconds = (int) (seconds)
        if strTime != '' or result > 0:
            strTime += '{0:02d}:'.format(result)
    strTime += '{0:02d}'.format(seconds)
    return strTime


    #return str(strUptime)
        
# Returns the current system time and date based upon a format string you provide
# uses the python time.strftime function directly.
def SystemCurrentDateTime(strFormat):
    # Don't validate the format string, just pass it along
    return time.strftime(strFormat)

#internal helper
def __GetCurrentTime():
    return  int(round(time.time() )) # Gives you float secs since epoch, so make it ms and chop
