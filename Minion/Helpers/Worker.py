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
#    Code for the worker threads that call the external collectors
#
##############################################################################

from Helpers import Log
from Helpers import DynamicPython
import subprocess

#Helper class that will run a given script, does this in the caller's thread, a new one is not spawned
class Worker:
    @staticmethod
    def RunScript(ScriptName,Parameters):

        # <Exe>python</Exe>
        # <Param>myscript.py</Param>

        # <Exe>myscript.py</Exe>

        # <Exe>python</Exe>
        # <Param>myscript.py</Param>
        # <Param>myFn</Param>
        # <Param>22</Param>   

        GiveItATry=True
        ScriptName = DynamicPython.DynamicLoader.convertPath(ScriptName)
        if ScriptName.lower().startswith("python") or ScriptName.lower().endswith(".py") and len(Parameters) > 0:
       
            if ScriptName.lower().endswith(".py"):  #if script name is a script file (instead of python.exe), then the fnParam is 1st param, rather than the 2nd
                script = ScriptName
                fnName = Parameters[0]

            elif ScriptName.lower().startswith("python") and len(Parameters) == 1: #could be a python script with no function, so is just script name, which I can't load dynamically now
                GiveItATry = False 

            else:
               script = Parameters[0]
               fnName = Parameters[1]

            if True == GiveItATry:
                pFn = DynamicPython.DynamicLoader.tryToLoadPythonScriptFunction(script,fnName)
                if None != pFn:
                    retVal = Worker.__TryToCallImportedPython(pFn,Parameters,ScriptName.lower().endswith(".py"))
                    if "Oops" != retVal:
                        return retVal

        #could NOT load dynamically, or is not a python script, so start an exernal process    
        invoke=[]
        invoke.append(ScriptName)
        for param in Parameters:
            invoke.append(param.rstrip())
        
        strRet = ""
        try:
            strRet = subprocess.check_output(invoke).decode('utf-8').rstrip()
            
        except Exception as Ex:
            Log.getLogger().error(ScriptName + ": " + str(Ex))
            pass

        if True == ("Error" in strRet):
           ReturnString = strRet
           raise Exception(strRet) 
    
        return  strRet # all good, should be a vaue of some kind

    #try to call a python fn
    @staticmethod
    def __TryToCallImportedPython(pFn,Parameters,ExecutableIsScript):
        offset = 2
        lenCheck = len(Parameters)      #Param1 = script name, #2  fnName

        if True == ExecutableIsScript:
            offset = 1                  #Param1 =  fnName
            lenCheck +=1

        try: #different fn calls based upon # of params.
            RetVal = None
            if pFn == None :
                RetVal = None

            elif 2 == lenCheck: 
                RetVal = pFn()

            elif 3 == lenCheck: 
                RetVal = pFn(str(Parameters[offset]))

            elif 4 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]))

            elif 5 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]))

            elif 6 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]))

            elif 7 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]))

            elif 8 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]))

            elif 9 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]))

            elif 10 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]))

            elif 11 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]))

            elif 12 == lenCheck:
                RetVal = pFn(str(Parameters[offset]),str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),str(Parameters[offset+9]))

            else:
                Log.getLogger().error("Attempted to call external python script with too many parameters, update Worker.py to support more")
                RetVal = "oops"

        except Exception as ex:
            Log.getLogger().error(str(ex))

        return  RetVal

