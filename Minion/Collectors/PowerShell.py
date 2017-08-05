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
#    Allows the calling of powershell scripts
#
##############################################################################

import subprocess 

#Executes a powershell script
def InvokePowerScript(ScriptName) :
    return __InvokeScript(ScriptName,None)

def InvokePowerScript_1Param(ScriptName,Param) :
    return __InvokeScript(ScriptName,[Param])

def InvokePowerScript_2Param(ScriptName,Param1,Param2) :
    return __InvokeScript(ScriptName,[Param1,Param2])

def InvokePowerScript_3Param(ScriptName,Param1,Param2,Param3) :
    return __InvokeScript(ScriptName,[Param1,Param2,Param3])


def __InvokeScript(ScriptName, Params):
    invoke = ['powershell.exe']
    invoke.append('-ExecutionPolicy')
    invoke.append('Unrestricted')
    invoke.append('-file')
#    invoke.append('.\Collectors\PSGetCounter.ps1')
    invoke.append(ScriptName)

    if None != Params:
        for param in Params:
            if param[0] != '-': #powershell needs a dash for each param
                #invoke.append('-param')
                #param = '-' + param
                pass
            invoke.append(param)


    result = subprocess.Popen(invoke,shell=False,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    output, error = result.communicate()
    rc = result.returncode

    retData = output.decode('utf-8').rstrip()
    retData = retData.lstrip()
    
    return str(retData)


#print (str(CallPowerShell("1","\Processor(_total)\% Processor Time")))  #uncomment to test calling via standalone
