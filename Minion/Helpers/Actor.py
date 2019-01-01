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
#    Wrapper class containing all the info needed to execute an actor/task
#
##############################################################################

from Helpers import Log
from Helpers import Worker
from Helpers import ThreadManager
from Helpers import Worker
from Util import Sleep
import threading
import subprocess

#Class for actors - actions (scripts) to be called remotely
class Actor:
    def __init__(self):
        self.ID = 'Invalid'
        self.ExecutableName = ''
        self.Namespace = '' #only used for recv actions
        self.Parameters = []
        self.LastUniqueID = ''

    def __StartWorkerProc(self):
        ThreadManager.GetThreadManager().CreateThread(self._Name,self.__enactProc)
        ThreadManager.GetThreadManager().StartThread(self._Name)
                

    # prints all the info about this collector
    def GetInfo(self):
        retString = "ID: " + self.ID + " -->script: " + self.ExecutableName
        for param in self.Parameters:
            retString = retString + " [" + param + "]"

        return retString

    def Enact(self,ExtraParams):
        Parameters = []

        Parameters.extend(list(self.Parameters))  # parameters listed in XML
        Parameters.extend(ExtraParams)      # any passed parameters from Marvin

        ThreadManager.RunWorkerThread(self.__enactProc,Parameters)

    def __enactProc(self,allParams):
        try:
            Log.getLogger().debug("Calling Script : " + self.ExecutableName + str(allParams))
            Worker.Worker.RunScript(self.ExecutableName,allParams)

        except Exception as ex:
            Log.getLogger().error(str(ex))


def ExecutableExists(strApplication,params):
    try:
        if None == params:
            subprocess.call(strApplication)
        else:
            paramCount = len(params)
            if 1 == paramCount:
                subprocess.call(strApplication,params)
            elif 2 == paramCount:
                subprocess.call(strApplication,params[0],params[1])
            elif 3 == paramCount:
                subprocess.call(strApplication,params[0],params[1],params[2])
            elif 4 == paramCount:
                subprocess.call(strApplication,params[0],params[1],params[2],params[3])
            elif 5 == paramCount:
                subprocess.call(strApplication,params[0],params[1],params[2],params[3],params[4])
            elif 6 == paramCount:
                subprocess.call(strApplication,params[0],params[1],params[2],params[3],params[4],params[5])

            else:
                print("Too many parameters for the ExecutableExists fn - increase capabilities")
                return False

    except Exception as Ex:
        print(str(Ex))
        return False

    return True


