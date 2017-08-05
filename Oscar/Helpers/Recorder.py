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
#    Records you data man!
#
##############################################################################
from Helpers import  Log
from Helpers import Playback
import sys

from Helpers import Log
from Data import MarvinData
from Util import Time
import sys

def get():
    return Recorder.get()

class Recorder(object):
    _instance = None

    def __init__(self):
        if None != Recorder._instance:
            return

        Recorder._instance = self

        self._RecordedData=[]
        self._Bytes = 0
        self._StartTime = Time.GetCurrMS()
        self._StopTime = Time.GetCurrMS()
        self._Stopped=True
        self._Saved = True


    @staticmethod
    def get():
        if None == Recorder._instance:
            Recorder() # create a new object, singleton

        return Recorder._instance

    def OnSaved(self):
        self._Saved = True

    def HasBeenSaved(self):
        return self._Saved

    def GetData(self):
        return self._RecordedData

    def GetRecordedCount(self):
        return len(self._RecordedData)

    def GetBytesRecorded(self):
        return self._Bytes

    def GetRecordingTime(self):
        if True == self._Stopped:
            return int((self._StopTime - self._StartTime)/1000)

        return int((Time.GetCurrMS() - self._StartTime)/1000)

    def AddData(self,objData):
        if True == self._Stopped:
            return

        if 0 == len(self._RecordedData): # only start timing when get 1st packet
            self._StartTime = Time.GetCurrMS()

        self._RecordedData.append(objData)
        self._Bytes += sys.getsizeof(objData)
        self._Saved = False

    def Start(self):
        self._StartTime = Time.GetCurrMS()
        self._Stopped=False
        self._Saved = False

    def Stop(self,flush):
        self._StopTime = Time.GetCurrMS()
        self._Stopped=True
        if True == flush:
            self._Flush()

        if self._Bytes > 0:
            Playback.get().SetData(self._RecordedData)

    def _Flush(self):
        self._RecordedData=[]
        self._Bytes = 0
        self._StartTime = Time.GetCurrMS()
        self._StopTime = Time.GetCurrMS()
        self._Stopped=True
        self._Saved = True


    def Clear(self):
        self._RecordedData.clear()
        self._Bytes = 0







