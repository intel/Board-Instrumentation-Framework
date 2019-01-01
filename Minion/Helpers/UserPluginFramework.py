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
#    provides some framework for holdign the relevant data for a Plugin that
#    is a DynamicCollector
##############################################################################

import inspect
from Helpers import Log
from Helpers import ThreadManager
from Helpers import DynamicPython
from pprint import pprint as pprint

class UserPluginFramework:
    Instance = 1
    def __init__(self, ScriptName, FunctionName, Params, pluginInterface, SpawnThread):
        self.ScriptName = ScriptName
        self.FunctionName = FunctionName
        self.Params = []
        self.Params.append(pluginInterface)
        self.PluginInterface = pluginInterface
        self.Params.extend(Params)
        self.ptrFunction = None
        self.ThreadName = None
        self.SpawnThread = SpawnThread

    def ValidateUserPlugin(self):
        if not self.ScriptName.lower().endswith(".py"):
            Log.getLogger().error("User Defined collectors must be in a python (.py) file. Invalid file provided: " + ScriptName)
            return False

        self.ptrFunction = DynamicPython.DynamicLoader.tryToLoadPythonScriptFunction(self.ScriptName,self.FunctionName)
                                                                                         
        if None == self.ptrFunction:
            Log.getLogger().error("Unable to load User Plugin: " + self.FunctionName + " in " + self.ScriptName)
            return False

        x = inspect.getargspec(self.ptrFunction)
        
        pprint(x)
        
        paramCount = len(inspect.getargspec(self.ptrFunction).args)
        if paramCount != len(self.Params):
            Log.getLogger().error("Unable to load User Plugin: " + self.FunctionName + " in " + self.ScriptName + ". Wrong number of parameters found.")
            return False

        return True

    def CollectionProc(self):
        if True == self.SpawnThread:
            if None ==  self.ThreadName:
                self.ThreadName = "UserPlugin." + str(UserPluginFramework.Instance) + "." + self.ScriptName + self.FunctionName
                UserPluginFramework.Instance += 1
                ThreadManager.GetThreadManager().CreateThread(self.ThreadName,self.__CallUserPlugin)
                ThreadManager.GetThreadManager().StartThread(self.ThreadName)
            return "HelenKeller"

        else:
            self.__CallUserPlugin()


    def __CallUserPlugin(self,fnKillSignalled = None, UserData=None):
        Parameters = self.Params
        pFn = self.ptrFunction

        Parameters[0].KillThreadSignalled = fnKillSignalled

        lenCheck = len(Parameters)      

        offset = 0                  #Param1 =  fnName

        try: #different fn calls based upon # of params.
            if 1 == lenCheck: 
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],**self.kwargs)
                else:
                    RetVal = pFn(Parameters[offset])

            elif 2 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),**self.kwargs)
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]))

            elif 3 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),**self.kwargs)
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]))

            elif 4 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),**self.kwargs)
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]))

            elif 5 == lenCheck: 
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),**self.kwargs)
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]))

            elif 6 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),**self.kwargs)
                    
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]))

            elif 7 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),**self.kwargs)
                    
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]))

            elif 8 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),**self.kwargs)
                    
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]))

            elif 9 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),**self.kwargs)
                    
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]))

            elif 10 == lenCheck:
                if None != self.kwargs:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),str(Parameters[offset+9]),**self.kwargs)
                    
                else:
                    RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),str(Parameters[offset+9]))

            elif 11 == lenCheck:
                if None != self.kwargs:
                   RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),str(Parameters[offset+9]),str(Parameters[offset+10]),**self.kwargs)
                    
                else:
                   RetVal = pFn(Parameters[offset],str(Parameters[offset+1]),str(Parameters[offset+2]),str(Parameters[offset+3]),str(Parameters[offset+4]),str(Parameters[offset+5]),str(Parameters[offset+6]),str(Parameters[offset+7]),str(Parameters[offset+8]),str(Parameters[offset+9]),str(Parameters[offset+10]))
            else:
                Log.getLogger().error("Attempted to call external python plugin script with too many parameters, update UserPluginFramework.py to support more")
                RetVal = "oops"

        except Exception as ex:
            Log.getLogger().error(str(ex))

        return "HellenKeller"
        

    

