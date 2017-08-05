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
#    The Abstraction UI for no GUI
#
##############################################################################

from Util import Sleep
from Helpers import ThreadManager
from Helpers import Log
import signal


class GuiNone(object):
    def __init__(self):
        self._End = False

    def SetTitle(self,titleStr):
        pass

    def MessageBox_Error(self,Title,Message):
        pass

    def MessageBox_Info(self,Title,Message):
        pass

    def MessageBox_OkCancel(self,Title,Message):
        return False

    def signal_handler(self,signal, frame):
        print("Ctrl+C pressed - exiting")
        self._End = True

        
    def OnStart(self):
        print("Using Console UI")
        signal.signal(signal.SIGINT, self.signal_handler) # make my own Ctrl+C handler now

        while not self._End:
            Sleep.SleepMs(250)
        

    def OnQuit(self):
        self._End = True

    def OnClearData(self):
        pass


    def __WorkerProc(self,fnKillSignalled,userData):
        while not fnKillSignalled():
            Sleep.SleepMs(250)
