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
    stopIndex = input[index:].find(")")

    if stopIndex < index:
        otherIndex = input[index:].find(")")
        stopIndex = index + otherIndex

    while index != -1 and stopIndex != -1:  # do it in a loop in case there are more than one alias
        strAlias = input[index+2:stopIndex]
        if AliasMgr.IsAliased(strAlias):
            newStr = ""
            newStr = newStr + input[0:index] + AliasMgr.GetAlias(strAlias) + input[stopIndex+1:len(input)]
            input = newStr
            index = input.find("$(")
            stopIndex = input[index:].find(")") + index

        else:        
            Log.getLogger().warn("Something looks like an Alias, but there is no alias registered for it --> " + input)
            return orig
    
    return input

#helper class to manage alias list
class AliasMgr:
    __AliasList=[] #alias list, full of dictionaries dictorary
    __AliasList.append({}) # create the base Alias List 

    @staticmethod
    def AddAlias(Alias,Value):
        strAlias = Alias.upper()
        if strAlias in AliasMgr.__AliasList[0]:
            Log.getLogger().warning("Attempt to create duplicate alias key: " + strAlias)
            return

        AliasMgr.__AliasList[0][strAlias] = Value  # only add to the top list

    # returns the aliased value
    @staticmethod
    def GetAlias(Alias):
        strAlias = Alias.upper()
        for aliasMap in AliasMgr.__AliasList: # go through each map in the array
            if strAlias in aliasMap:
                return aliasMap[strAlias]

        return "" #should not get here, should always call IsAliased before calling this fn

    # returns true if there is an alias for the given string
    @staticmethod
    def IsAliased(Alias):
        return not AliasMgr.GetAlias(Alias) == ""

    @staticmethod
    def Push():
        AliasMgr.__AliasList.insert(0,{})

    @staticmethod
    def Pop():
        if len(AliasMgr.__AliasList) > 1:
            del AliasMgr.__AliasList[0]
        else:
            Log.getLogger().error("Attempted to pop Alias list too far!")

    # Adds all the environment variables to the alias list - kewl eh?
    @staticmethod
    def AddEnvironmentVariables():
        for key in os.environ.keys():
            AliasMgr.AddAlias(key,os.environ[key])

        # on linux, computername is not an environment vairable, so let's add it :-)
        if False == AliasMgr.IsAliased("ComputerName"):
            try:
                import platform
                AliasMgr.AddAlias("ComputerName",platform.uname()[1])
            except Exception as ex:
                Log.getLogger().warning("Unable to get computer name for Alias: " + str(ex))


    @staticmethod
    def LoadExternalAliasFile(filename):
        returnVal = True
        #open the xml file for reading:
        try:
            file = open(filename,'r')
        except Exception as ex:
            Log.getLogger().error("Invalid Alias File: " + filename)
            return False

        Log.getLogger().info("Loading Aliases from external file: " + filename)
        AliasMgr.Push()
        for line in file:
            line = line.strip()
            parts = line.split("#")
            if len(parts[0]) > 0:
                whole = parts[0]
                parts=whole.split("=")
                if len(parts)==2:
                    AliasMgr.AddAlias(parts[0],parts[1])
                    Log.getLogger().info("Adding alias from external file: " + parts[0] + " = " + parts[1])
                else:
                    Log.getLogger().error("Invalid Alias: " + line +" defined in file: " + filename)
                    returnVal = False
            

        file.close()
        return returnVal

        


    

        
