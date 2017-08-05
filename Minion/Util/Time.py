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
#    Some helper cod for time related goodies
#
##############################################################################

import time
import ntpath
import os
from Helpers import Log



#gets current MS since epoch
def GetCurrMS():
    return  int(round(time.time() *1000)) # Gives you float secs since epoch, so make it ms and chop

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator =='\\':
        path = path.replace('/',os.path.sep)

    return path

def GetFileTimestampMS(strFile):
    strFile = convertPath(strFile)
    try:
        modTime = int(ntpath.getmtime(strFile)*1000) # Convert last modified time in ns to ms
        return modTime 

    except:
        Log.getLogger().warning("Unable to access SyncFile: " + strFile +". Using system clock instead.")
        return GetCurrMS()

    


