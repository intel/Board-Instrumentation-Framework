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
#       Applicaiton entry point
#
##############################################################################

import sys
if sys.version_info < (3, 3):
    pass

else:
    import argparse
    import os
    import logging
    from Helpers import Configuration
    from Data.ConnectionPoint import ConnectionType
    from Helpers.Playback import RepeatMode
    from Helpers import ServerUDP
    from Helpers import ThreadManager
    from Util import Sleep
    from Util import Time
    from Helpers import Log
    from Helpers import Watchdog
    from Helpers import GuiMgr
    from Helpers import VersionMgr
    from Helpers import Playback


def existFile(filename):
    if not os.path.exists(filename):
        print("Specified file: " + str(filename) + " does not exist.")
        return False
    return True

def performBatchConvert(filematch):
    import os
    import fnmatch

    GuiMgr.Initialize(GuiMgr.UI.NONE,None,None)
    #dir_path = os.path.dirname(os.path.realpath(filematch))
    convertCount = 0
    rel_path,filename = os.path.split(filematch)
    if len(rel_path) < 1:
        rel_path='.'
    for file in os.listdir(rel_path):
        if fnmatch.fnmatch(file, filename):
            inputFilename = os.path.join(rel_path,file)
            if Playback.get().ReadFromFile(inputFilename):
                Playback.get().Clear()
                baseName,ext = os.path.splitext(inputFilename)
                csvFilename = baseName+".csv"
                Playback.get().WriteCSVFile(csvFilename,1)
                print("{0} --> {1}".format(inputFilename,csvFilename))
                convertCount += 1
    print("Converted {0} files".format(convertCount))
    GuiMgr.Quit()
   
    

def HandleCommandlineArguments():
    parser = argparse.ArgumentParser(description='Oscar the wonderful')

    parser.add_argument("-i","--input",help='specifies application configuration file file',type=str)
    parser.add_argument("-l","--logfile",help='specifies log file name',type=str)
    parser.add_argument("-v","--verbose",help="prints debug information",action="store_true")
    parser.add_argument("-m","--minimize",help='run with GUI minimized',action="store_true")

    exclustionGroup = parser.add_mutually_exclusive_group()

    exclustionGroup.add_argument("-p","--playback",help='specifies file to load and playback',type=str)
    exclustionGroup.add_argument("-r","--record",help='specifies file to record to, used with --time',type=str)

    group_Play = parser.add_argument_group('Playback',"Parameters to be used when you use the --playback option")
    group_Play.add_argument("-s","--speed",help='specifies payback speed',type=float)

    foo = group_Play.add_mutually_exclusive_group()
    foo.add_argument("-ex","--exit",help="exit after playback finished,not valid with repeat or loop",action="store_true")
    foo.add_argument("-rp","--repeat",help="repeat the dataset continously",action="store_true")
    foo.add_argument("-lp","--loop",help="loop from one datalocation to the next repeatedly (use -begin and -end)",action="store_true")

    group_Play.add_argument("-b","--begin",help='start dataset number for mode=loop',default=0,type=int)
    group_Play.add_argument("-e","--end",help='end dataset number for mode=loop',type=int)

    parser.add_argument("-t","--time",help='specifies time (in minutes) to run before automatically exiting, used with Recording and Playback',type=int)
    parser.add_argument("-ng","--nogui",help='run without GUI',action="store_true")
    parser.add_argument("-bc","--batchconvert",help="batch convert biff files to csv",type=str)
    
    try:    
        args = parser.parse_args()

    except:
       return False

    _Verbose = args.verbose

    if None != args.logfile:
        Configuration.get().SetLogFilename(args.logfile)

    if True == _Verbose:
        Log.setLevel(logging.DEBUG)

    if None != args.batchconvert:
        performBatchConvert(args.batchconvert)
        return False

    conf = Configuration.get()

    if True == args.minimize:
        conf.SetMinimizeGui(True)

    if None != args.input:
        if existFile(args.input):
            conf.SetConfigFilename(args.input)
        else:
            return False

    if None != args.playback:
        if existFile(args.playback):
            conf.SetAutorunFilename(args.playback)
            conf.SetExitAfterAutoPlay(args.exit)
        else:
            return False

    if None != args.time:
        Configuration.get().SetAutorunTime(args.time)
        if None == args.playback and None == args.record:
            print("time option only valid when doing a playback or a recording")
            return false
        
    if None != args.record:
        Configuration.get().SetRecordFilename(args.record)

    if None != args.speed:
        conf.SetPlaybackSpeed(args.speed)

    conf.SetAutrunLocations(args.begin,args.end)

    if args.repeat:
        conf.SetAutoRunMode(RepeatMode.REPEAT)

    elif args.loop:
        conf.SetAutoRunMode(RepeatMode.LOOP)

    elif None != args.playback:
        conf.SetAutoRunMode(RepeatMode.NONE)

    conf.SetUseGUI(not args.nogui)

    return True

def StartupWorkerProc(fnKillSignalled,userData):
    downstreamServer = userData[0]
    upstreamServer = userData[1]

    Sleep.SleepMs(500)
    downstreamServer.Start()
    upstreamServer.DropPackets(True)
    upstreamServer.Start()

    cut = Watchdog.ConnectionUpdateTimer()
    wdt = Watchdog.WatchdogTimer()

    conf = Configuration.get()
    if None != conf.GetAutorunFilename():
        GuiMgr.OnStopLiveData()
        #GuiMgr.OnStopPlayback()
        #GuiMgr.OnStopRecording(True) #drop all recorded packets
        GuiMgr.OnSetPlaybackSpeed(Configuration.get().GetPlaybackSpeed())
        ss = Configuration.get().GetAutorunLocations()

        #GuiMgr.OnEnablePlayback()
        GuiMgr.ReadFromFile(Configuration.get().GetAutorunFilename())
        GuiMgr.OnStopPlayback()
        Sleep.SleepMs(100) # let gui worker threads catch up, so gui updates properly
        GuiMgr.OnStartPlayback()
        GuiMgr.OnSetRepeatMode(Configuration.get().GetAutoRunMode(),ss[0],ss[1])

    else:
        upstreamServer.DropPackets(False)

    if None != conf.GetAutorunTime() and conf.GetAutorunTime() > 0 :  # specified a --time, so let's hang out for that long
        endTime = Time.GetCurrMS() + conf.GetAutorunTime() * 60 * 1000
        Log.getLogger().info("Waiting for " + str(conf.GetAutorunTime()) + " minutes before auto shutdown")
        if conf.GetRecordFilename():
            GuiMgr.OnStartRecording()

        while not fnKillSignalled() and endTime > Time.GetCurrMS():
            Sleep.SleepMs(250)

        Log.getLogger().info("Shutting down after time period")
        if conf.GetRecordFilename():  # was a recording session, so quit after that time
            GuiMgr.OnStopRecording()
            GuiMgr.WriteToFile(conf.GetRecordFilename())
            Log.getLogger().info("Saving Recorded data to file: " + conf.GetRecordFilename())

        GuiMgr.Quit()


def PrintVersion():
    print("Oscar - Version: " + VersionMgr.ReadVer())

def main():
    if not HandleCommandlineArguments():
        return

    if not Configuration.get().ReadConfigFile():
        return

    PrintVersion()
    downstreamConnInfo = Configuration.get().GetDownstreamConnection()
    upstreamConnInfo = Configuration.get().GetUpstreamConnection()

    downstreamServer = ServerUDP.ServerUDP(downstreamConnInfo,ConnectionType.DownstreamServer)
    upstreamServer = ServerUDP.ServerUDP(upstreamConnInfo,ConnectionType.UpstreamServer)

#    if None == Configuration.get().GetAutorunFilename() or 1==1:
    if upstreamServer.Start():
        if None == downstreamConnInfo:
            return

        if None == upstreamConnInfo:
            return

        ThreadManager.GetThreadManager().CreateThread("StartupStuff",StartupWorkerProc,(downstreamServer,upstreamServer))
        if not Configuration.get().GetUseGUI():
            GuiMgr.Initialize(GuiMgr.UI.NONE,downstreamServer,upstreamServer)

        else:
            try:
                GuiMgr.Initialize(GuiMgr.UI.TKINTR,downstreamServer,upstreamServer)
            except Exception as Ex:
                print(str(Ex))
                GuiMgr.Initialize(GuiMgr.UI.NONE,downstreamServer,upstreamServer)

        ThreadManager.GetThreadManager().StartThread("StartupStuff")

        GuiMgr.Start()

    ThreadManager.GetThreadManager().StopAllThreads()

if __name__ == '__main__':
    if sys.version_info < (3, 3):
        print("must use python 3.3 or greater")
    else:
        main()
    