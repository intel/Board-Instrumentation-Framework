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
#    Some kinda slick code to try and dynamically load an external python file
#    for the case where a phython app is specified as a collector.  Saves a not
#    incosiderable amount of time and processing not having to start a new system
#    process each time
#
##############################################################################

from Helpers import Log
import sys
import platform
import importlib
import inspect
import os
import imp

# kind of a cool class I came up with (help via Google) to dynamically load external python scripts!
class DynamicLoader:
    _LoadedScripts= {} #list of successful loads, so don't need to load it for next time
    _FailedScripts = {} #list of FAILED loads, so we don't try to load it again

    #Tries to load the python script dynamically
    @staticmethod
    def tryToLoadPythonScriptFunction(ScriptName,FunctionName):
        ScriptName = DynamicLoader.convertPath(ScriptName)
        return DynamicLoader.getDynamicClass(ScriptName,FunctionName)

    #Will try to dynamically load the pyton script
    @staticmethod
    def getDynamicClass(ScriptName,FunctionName):
        key = ScriptName.upper() + FunctionName

        if key in DynamicLoader._LoadedScripts:
            return DynamicLoader._LoadedScripts[key]  #already successfully loaded this sucker, so just return it again

        if key in DynamicLoader._FailedScripts: #already failed on this one, so let's not try again
            return None

        directory, file = os.path.split(ScriptName)
        fileName, fileExtension = os.path.splitext(file)
        modname = fileName #os.path.basename(fileName)

        try:
            sys.path.append(directory)
            module = __import__(modname)

            if sys.version_info < (2, 7):
                pFn = getattr(module,fromlist=[FunctionName])
            else:
                pFn = getattr(module,FunctionName)

            DynamicLoader._LoadedScripts[key] = pFn
            Log.getLogger().debug("Performed successful Dynamic Loading of Python script: " + ScriptName +" function: " + FunctionName)

        except Exception as Ex:
            Log.getLogger().error("Unable to Dynamically Load Python script: " + ScriptName +" function: " + FunctionName + ": " + str(Ex))
            DynamicLoader._FailedScripts[key] = "No Workie"
            return None

        return pFn

    #Makes path (if passed) os independent
    @staticmethod
    def convertPath(path):
        separator = os.path.sep

        if separator == '/':
            path = path.replace('\\',os.path.sep)

        elif separator =='\\':
            path = path.replace('/',os.path.sep)

        return path
        




