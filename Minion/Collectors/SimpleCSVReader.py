##############################################################################
#  Copyright (c) 2018 Intel Corporation
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
#       
##############################################################################
"""
  Minion Config should look something like:

        <DynamicCollector Frequency="3000">
            <Plugin>
                <PythonFile>Collectors\SimpleCSVReader.py</PythonFile>
                <EntryPoint SpawnThread="True">CollectFunction</EntryPoint>
                <Param>memcached-results.180313-093719.csv</Param>
                <Param>True</Param>
            </Plugin>
        </DynamicCollector>
"""
import time

def CollectFunction(frameworkInterface,Filename,Repeat=True):
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting Simple CSV ")

    file = open(Filename,'rt')
    if None == file:
        Logger.error("File [" + Filename + "] does not exist")
        return

    sleepTime = float(frameworkInterface.Interval)/1000
    
    headers = None
    currCount = 0
    DatatMap={}

    Done = False

    try:
        while not Done:
            for line in file:
                if None == headers:
                    headers = line.split(",")
                    continue
                else:
                    currCount += 1
                    parts = line.split(",")
                    host = parts[1]
            
                    for index,value in enumerate(parts):
                        if index < 2:
                            continue # don't care about 1st 2
                        ID = host +"."+headers[index]
                        if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
                            frameworkInterface.AddCollector(ID)    # Nope, so go add it

                        frameworkInterface.SetCollectorValue(ID,value) # update the value with the ID, let the framework handle the interval

                if currCount == 4:
                    time.sleep(sleepTime)
                    currCount = 0

                if not Repeat or frameworkInterface.KillThreadSignalled():
                    Done = True
            if Repeat:
                file.seek(0)
                headers=None
                currCount = 0


    except Exception as Ex:
        Logger.error("Simple CSVCollector catastrophic error: " + str(Ex))

    file.close()









        
