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
#    Logger code!
#
##############################################################################

import logging

# Global function to return my logger object
def getLogger():
    return Logger.getLogger()

def getLogToConsole():
    return Logger.LogToConsole

def setLevel(level):
    Logger.getLogger();

    if Logger._console != None:
        Logger._console.setLevel(level)

    if Logger._logger != None:
        Logger._logger.setLevel(level)
    


#My logger wrapper
class Logger():
    _logger = None  #logger object
    _console = None
    LogToConsole = True

    @staticmethod
    def getLogger():
        from Helpers import Configuration

        if None == Logger._logger:
            Logger._logger = logging.getLogger("Minion")
            logging.basicConfig(level=logging.ERROR,
                                format='%(asctime)s %(name)-12s %(levelname)-8s %(message)s',
                                datefmt='%m-%d %H:%M',
                                filename="MinionLog.txt",
                                filemode='w')

            console = logging.StreamHandler()
            console.setLevel(logging.INFO)

            # create formatter
            formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

            # add formatter to console
            console.setFormatter(formatter)

            # add ch to logger
            if Logger.LogToConsole == True:
                Logger._logger.addHandler(console)
                Logger._console = console

        return Logger._logger



