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
#    Handles dealing with Aliases --> Definition: Foo = "MyAlias" use $(Foo)
#
##############################################################################
import os
from Helpers import Log

# Routine that does most of the magic of aliases, including combining aliases, such as ($Alias1).$(Alias2)
def Alias(input):
    orig = input
    index = input.find("$(")        #Alias is surrounded by -->$( ) <--
    stopIndex = input.find(")")
    while index != -1 and stopIndex != -1:  # do it in a loop in case there are more than one alias
        strAlias = input[index+2:stopIndex]
        if AliasMgr.IsAliased(strAlias):
            newStr = ""
            newStr = newStr + input[0:index] + AliasMgr.GetAlias(strAlias) + input[stopIndex+1:len(input)]
            input = newStr
            index = input.find("$(")
            stopIndex = input.find(")")

        else:        
            Log.getLogger().warn("Something looks like an Alias, but there is no alias registered for it --> " + input)
            return orig
    
    return input

#helper class to manage alias list
class AliasMgr:
    __AliasList= {} #alias dictorary

    def AddAlias(Alias,Value):
        strAlias = Alias.upper()
        if strAlias in AliasMgr.__AliasList:
            Log.getLogger().warning("Attempt to create duplicate alias key: " + strAlias)
            return

        AliasMgr.__AliasList[strAlias] = Value

    # returns the aliased value
    def GetAlias(Alias):
        strAlias = Alias.upper()
        if strAlias in AliasMgr.__AliasList:
            return AliasMgr.__AliasList[strAlias]

        return "" #should not get here, should always call IsAliased before calling this fn

    # returns true if there is an alias for the given string
    def IsAliased(Alias):
        strAlias = Alias.upper()
        return strAlias in AliasMgr.__AliasList

    # Adds all the environment variables to the alias list - kewl eh?
    def AddEnvironmentVariables():
        for key in os.environ.keys():
            AliasMgr.AddAlias(key,os.environ[key])

        # on linux, computername is not an environment vairable, so let's add it :-)
        if False == AliasMgr.IsAliased("ComputerName"):
            import socket
            try:
                AliasMgr.AddAlias("ComputerName",socket.gethostbyaddr(socket.gethostname())[0])
            except Exception as ex:
                Log.getLogger().info("Unable to get computer name for Alias: " + str(ex))


    

        
