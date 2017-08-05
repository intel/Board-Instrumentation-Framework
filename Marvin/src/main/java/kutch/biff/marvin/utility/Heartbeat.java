/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * # 
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * # 
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * # 
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract: 
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick Kutch
 */
public class Heartbeat extends TimerTask 
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private TaskManager TASKMAN = TaskManager.getTaskManager();
    private int _interval;
    private Timer _Timer;
    private String _HeartbeatTaskID;
    
    public Heartbeat(int interval) 
    {
        _HeartbeatTaskID =  TaskManager.getTaskManager().CreateWatchdogTask();
        _interval = interval*1000;
        _Timer = new Timer();
    }
    
    public void Start()
    {
        _Timer.scheduleAtFixedRate(this, 100, _interval);
    }
    
    public void Stop()
    {
        _Timer.cancel();
    }
    
    @Override
    public void run()
    {
        TaskManager.getTaskManager().PerformTask(_HeartbeatTaskID);
    }
}
