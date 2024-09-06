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
#       Collector that generates different kinds of random data - good for 
#       testing your widgets without a data source setup.
##############################################################################

import re
import sys
import random

#Returns a random integer between the two values, not inclusive of second
#value.
def GetBoundedRandomValue(min,max):
    val = random.randrange(int(min),int(max))
    return  str(val)

#Returns a comma separated list of random integer between the two values, not
#inclusive of second value.  Size of list is determined by listSize param
def GetBoundedRandomList(min,max,listSize):	
    retString = str(random.randrange(int(min),int(max)))
    for x in range(1,int(listSize)):
        val = random.randrange(int(min),int(max))
        randVal = str(val)
        retString = retString + "," + randVal

    return retString

#Returns a scaled random value.  So if you want values of 1.0 to 100.0 send
#min=10,max=100,scale=0.1 to get the float value.
def GetScaledBoundedRandomValue(min,max,scale):
    val = float(GetBoundedRandomValue(min,max)) * float(scale)
    return str(round(val,2))


def StepValue(id, start, stop, step):
    start = int(start)
    stop = int(stop)
    step = int(step)

    if not hasattr(StepValue, "state"):
        StepValue.state = {}

    if id not in StepValue.state:
        StepValue.state[id] = start - step

    StepValue.state[id] += step
    if StepValue.state[id] >= stop:
        StepValue.state[id] = start

    return StepValue.state[id]

