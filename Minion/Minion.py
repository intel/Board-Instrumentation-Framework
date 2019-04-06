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
#    This is the main/entry point file for the Minion data collector program
#
##############################################################################
import os
from Helpers import Configuration
from Helpers import Log
import logging
import sys
import traceback
from Helpers import Alias
from Util import Sleep
import argparse
import signal
from Helpers import VersionMgr
from Helpers import ThreadManager
from Helpers import VersionCheck

_ConfigFilename = "MinionConfig.xml"
_ThreadActive = True

def extant_file(x):
    """
    'Type' for argparse - checks that file exists but does not open.
    """
    if not os.path.exists(x):
        raise argparse.ArgumentError("{0} does not exist".format(x))
    else:
       global _ConfigFilename
       _ConfigFilename = x
    return 

def ShowVersion():
    Log.setLevel(logging.INFO)
    Log.getLogger().info("Minion " + VersionMgr.ReadVer())

def signal_handler(signal, frame):
        global _ThreadActive
        _ThreadActive = False

def spinning_cursor(which=0):
    cursors=('/-\|'),('.o0o')
    cursor = cursors[which]
    i = 0
    global _ThreadActive
    while True == _ThreadActive:
        yield cursor[i]
        i = (i + 1) % len(cursor)


def main():
    parser = argparse.ArgumentParser(description='Minion Data Collector.')
    parser.add_argument("-i","--input",dest='argFilename',help='specifies input file',type=extant_file,metavar="FILE")
    parser.add_argument("-v","--verbose",help="prints information, values 0-3",type=int)
    parser.add_argument("-r","--runonce",help="calls all collectors once and exits",action="store_true")
    parser.add_argument("-a","--aliasfile",help="specify an external file that has alias defintions",type=str)
    
    try:
        args = parser.parse_args()
        if None == args.verbose:
            _VerboseLevel = 0
        else:
            _VerboseLevel = args.verbose
        _RunOnce = args.runonce
    
    except:
        return
    
    ShowVersion()
    if not VersionCheck.CheckVersion():
        Log.getLogger().error("Invalid version of Python")
        return

    if 3 <= _VerboseLevel:
        Log.setLevel(logging.DEBUG)

    elif 1 == _VerboseLevel:
        Log.setLevel(logging.WARNING)

    elif 2 == _VerboseLevel:
        Log.setLevel(logging.INFO)

    else:
        Log.setLevel(logging.ERROR)

    curr_dir_path = os.path.dirname(os.path.realpath(__file__))
    Alias.AliasMgr.AddAlias("WORKING_DIR",curr_dir_path)

    Alias.AliasMgr.AddEnvironmentVariables()
    if None != args.aliasfile:
        if not Alias.AliasMgr.LoadExternalAliasFile(args.aliasfile):
             return

    signal.signal(signal.SIGINT, signal.SIG_IGN)  # turn of Ctrl+C signal handler (will get inherted by sub processes

    if not os.path.exists(_ConfigFilename):
      Log.getLogger().error("Config file [" + _ConfigFilename +"] not found!")
      return

    config = Configuration.Configuration(_ConfigFilename,True)

    if None == config or not config.IsValid():
        pass

    else:
        print("Starting Collectors...")    
        totalCollectors = 0
        for namespace in config.GetNamespaces():
            totalCollectors += namespace.Begin(_RunOnce)

        signal.signal(signal.SIGINT, signal_handler) # make my own Ctrl+C handler now


        print(str(totalCollectors) + " Collectors started.")

        if False == _RunOnce:
            print("Press CTRL+C to Exit")
        else:
            print("Running Once")


        if False == _RunOnce:
            while _ThreadActive:
                if 0 == _VerboseLevel:
                    for c in spinning_cursor():
                        countStr = '['+str(config.GetCollectorCount())+'] '
                        sys.stdout.write(countStr)
                        sys.stdout.write(c)
                        Sleep.SleepMs(100)
                        sys.stdout.flush()
                        sys.stdout.write('\b')        
                        for c in countStr:
                            sys.stdout.write('\b')        

                    
                else:
                    Sleep.SleepMs(100)

        print("Shutting down...")
        try:
            ThreadManager.GetThreadManager().StopAllThreads()
        except:
            pass

if __name__ == "__main__":
   try:
      main()
   except KeyboardInterrupt:
      # do nothing here
      pass

   except Exception as ex:
        Log.getLogger().error("Uncaught app error: " + str(ex))
        Log.getLogger().error(traceback.print_exc())


   

