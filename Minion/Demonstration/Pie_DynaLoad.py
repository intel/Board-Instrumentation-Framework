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
# Simple collector that creates an array of numbers that add up to 100, for a pie chart demo
# this is an example of a collector that is dynamically loaded into Minion's process space
#
##############################################################################
import random
from random import randint
def Get():
    val1 = randint(5,30)
    val2 = randint(15,25)
    val3 = randint(2,60)
    val4 = 100-val1-val2-val3

    strRet =  str(val1)+","
    strRet += str(val2)+","
    strRet += str(val3)+","
    strRet += str(val4)

    return strRet

