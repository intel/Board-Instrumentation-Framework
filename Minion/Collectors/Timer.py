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
#    Implements a Timer.  You create a timer with an ID, start it, stop it, pause etc.
#    Call to Timer() takes 2 params - an ID and an action
#    Actions:
#       get - returns elapsed seconds
#       create - creates a timer, but does not start it
#       create_and_start - creates and starts timer
#       get_auto_create - creates, starts and gets a timer
#       stop - stops a timer
#       start - starts a timer, resumes ones that is paused, if stopped it resets counter
#       pause - pauses a timer, 'start' resumes
#
##############################################################################
import time

class TimerInfo:
    def __init__(self,strID):
        self.__ID = strID
        self.__Active = False
        self.__Paused = False
        self.__StartTime = 0
        self.__PausedTime = 0

    def __GetCurrentTime():
        return  int(round(time.time() )) 


    def getElapsedTime(self):
        if self.__Active:
            if not self.__Paused:
                return str(TimerInfo.__GetCurrentTime() - self.__StartTime)

            return str(self.__PausedTime - self.__StartTime)

        else:
            return "Timer [" + self.__ID +"] not started"

    def Start(self):
        if not self.__Active:
            self.__Active = True

        if self.__Paused:
            self.__Paused = False
            self.__PausedTime = 0

        else:
            self.__StartTime = TimerInfo.__GetCurrentTime()


    def Pause(self):
        if self.__Active and not self.__Paused:
            self.__Paused = True
            self.__PausedTime = TimerInfo.__GetCurrentTime()

    def Stop(self):
        if self.__Active:
            self.__Active = False

            if self.__Paused:
                self.__Paused = False
                self.__PausedTime = 0


# Actions include 'create','create_and_start','get_auto_create', 'get', 'start','stop','pause'
def Timer(ID,Action):
    if not hasattr(Timer, "timerMap"):
        Timer.timerMap = {}     

    Action = str(Action)
    _action = Action.lower()
    _ID = ID.lower()
    
    if _action == "create":
        if _ID in Timer.timerMap.keys():
            return "Timer with ID: " + ID + " already exists."

        timerState = TimerInfo(_ID)
        Timer.timerMap[_ID] = timerState
        return True

    if _action == "create_and_start":
        retVal = Timer(ID,"create")
        if retVal != True:
             return retVal

        timerState = Timer.timerMap[_ID]
        Timer(ID,"start")
        return Timer(ID,"get")

    if _action == "get_auto_create":
        if not _ID in Timer.timerMap.keys():
            Timer("create_and_start",ID)

        return Timer(ID,"get")

    if not _ID in Timer.timerMap.keys():
       return "Timer [" + ID +"] does not exist.  Can't perform action :" + Action
    
    timerState = Timer.timerMap[_ID]

    if _action == "get":
        return timerState.getElapsedTime()

    if _action == "start":
        return timerState.Start()

    if _action == "stop":
        return timerState.Stop()

    if _action == 'pause':
        return timerState.Pause()

    print("Unknown Timer Action: " + Action)
