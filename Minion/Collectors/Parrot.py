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
#    Crazy simple collector that just returns the string passed.  This is done
#    in order to maintain design philosopy that Minion knows nothing about the
#    collectors themselves.  This routine may be useful for sending the server 
#    name, or maybe a single status word.
#
##############################################################################

# VERY simple routine, to just echo back a value, useful if you just want a
# collector to send a specific string, especially for a 1-shot send, like 'Finished' or such
def Echo(strEcho):
    return str(strEcho)
