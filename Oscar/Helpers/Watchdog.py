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
#    Sinds an 'I am alive' message once in a while
#
##############################################################################
import xml.dom.minidom
from xml.parsers.expat import ExpatError
from Helpers import  Log
from Data import ConnectionPoint
from Data.ConnectionPoint import ConnectionType
from Helpers import Target
from Helpers import TargetManager
from Helpers import ThreadManager
from Helpers import Configuration
from Helpers import VersionMgr
from Util import Time
from Util import Sleep

#############
# This class sends a heartbeat (watchdog re-arm) to all upstream Oscars, so we don't timout
#############
class WatchdogTimer(object):
    def __init__(self):
        name = "Watchdog Timer Thread"
        self.__WorkerThread = ThreadManager.GetThreadManager().CreateThread(name,self.WatchdogProc)
        ThreadManager.GetThreadManager().StartThread(name)


    def WatchdogProc(self,fnKillSignalled,userData):
        lastUpdate = 0
        interval = Configuration.get().GetTimeoutPeriod() * 0.25 # send a watchdog at 4x rate of timeout  

        buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        buffer = buffer + "<Oscar Type=\"WatchdogTimer\">"
        buffer = buffer + "<Version>1.0</Version>"
        buffer = buffer + "<Port>"+str(Configuration.get().GetUpstreamConnection().getPort())+"</Port>"
        buffer = buffer + "</Oscar>"

        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            if lastUpdate < Time.GetCurrMS() - interval:
                TargetManager.GetTargetManager().BroadcastUpstreamToType(buffer,ConnectionType.UpstreamOscar) # send heartbeat to all upstream Oscars
                lastUpdate = Time.GetCurrMS()

            Sleep.Sleep(0.25) #snooze for 250 ms
                
#############
# This class sends connection info to everything downstream periodically
# those downstream things (other Oscars and Marvins) use this to send packets back
#############
class ConnectionUpdateTimer(object):
    def __init__(self):
        name = "Connection Update Timer Thread"
        self.__WorkerThread = ThreadManager.GetThreadManager().CreateThread(name,self.WorkerProc)
        ThreadManager.GetThreadManager().StartThread(name)

    def WorkerProc(self,fnKillSignalled,userData):
        lastUpdate = 0
        interval = Configuration.get().GetConnectionUpdateInterval()

        buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        buffer = buffer + "<Oscar Type=\"ConnectionInformation\">"
        buffer = buffer + "<Version>1.0</Version>"
        buffer = buffer + "<OscarVersion>" + VersionMgr.ReadVer() + "</OscarVersion>"
        buffer = buffer + "<ID>" + Configuration.get().GetID()+"</ID>"
        buffer = buffer + "<Port>"+str(Configuration.get().GetDownstreamConnection().getPort())+"</Port>"
        buffer = buffer + "</Oscar>"

        #<?xml version="1.0" encoding="utf-8"?>
        #<Oscar Type="ConnectionInformation">
        #    <Version>1.0</Version>
        #    <ID>Foo</Foo>
        #    <Port>Port</Port>
        #</Oscar>


        while not fnKillSignalled(): # run until signalled to end - call passed function to check for the signal
            if lastUpdate < Time.GetCurrMS() - interval:
                TargetManager.GetTargetManager().BroadcastDownstream(buffer,True,None) # send Connection Data to all downstream things (Oscars & Marvins)
                lastUpdate = Time.GetCurrMS()
                
                Configuration.get().RescanTargets()

            else:
                Sleep.Sleep(0.25)
                TargetManager.GetTargetManager().CheckForRemovalOfDynamicMarvins()









