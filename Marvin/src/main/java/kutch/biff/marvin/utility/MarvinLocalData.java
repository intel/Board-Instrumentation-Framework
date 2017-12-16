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

import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.BaseTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick
 */
public class MarvinLocalData
{
    private final ConfigurationReader CONFIG = ConfigurationReader.GetConfigReader();
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final TaskManager TASKMAN = TaskManager.getTaskManager();
    private final String Namespace = "MarvinLocalNamespace";
    private final long startTime = System.currentTimeMillis();
    private final int _interval;
    private ScheduledExecutorService executor;
    private boolean _StopSignalled;

    public MarvinLocalData(int interval)
    {
        _interval = interval;
        if (_interval > 0)
        {
            _StopSignalled = false;
            Runnable myRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    if (false == _StopSignalled)
                    {
                        PerformMagic();
                    }
                }
            };

            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(myRunnable, 0, 1, TimeUnit.SECONDS);
            DoStaticMagic();
        }
    }
    
    public void Shutdown()
    {
        _StopSignalled = true;
        executor.shutdown();
    }

    private void DoStaticMagic()
    {
        TASKMAN.getDataMgr().ChangeValue("TaskCount", Namespace, Integer.toString(BaseTask.getTaskCount()));
        if (CONFIG.getConfiguration().GetApplicationID().length() > 0)
        {
            TASKMAN.getDataMgr().ChangeValue("MarvinID", Namespace, CONFIG.getConfiguration().GetApplicationID());
        }
        else
        {
            TASKMAN.getDataMgr().ChangeValue("MarvinID", Namespace, "Not Set");
        }
    }

    private void PerformMagic()
    {
        TASKMAN.getDataMgr().ChangeValue("Datapoints", Namespace, Integer.toString(DataManager.getDataManager().NumberOfRegisteredDatapoints()));
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        TASKMAN.getDataMgr().ChangeValue("RuntimeSecs", Namespace, Long.toString(runtime));
        TASKMAN.getDataMgr().ChangeValue("RuntimeFormatted", Namespace, GetTimeString(runtime));
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        TASKMAN.getDataMgr().ChangeValue("LocalTime", Namespace, GetTimeString(now.toSecondOfDay()));
        TASKMAN.getDataMgr().ChangeValue("WidgetCount", Namespace, Integer.toString(BaseWidget.getWidgetCount()));
        TASKMAN.getDataMgr().ChangeValue("DataUpdateCount", Namespace, Long.toString(TASKMAN.getDataMgr().getUpdateCount()));
        TASKMAN.getDataMgr().ChangeValue("UnassignedDatapointCount", Namespace, Long.toString(TASKMAN.getDataMgr().getUnassignedCount()));
        TASKMAN.getDataMgr().ChangeValue("TasksExecutedCount", Namespace, Long.toString(TASKMAN.GetPerformedCount()));
        TASKMAN.getDataMgr().ChangeValue("PendingTasksCount", Namespace, Long.toString(TASKMAN.GetPendingTaskCount()));
        long freeMem = Runtime.getRuntime().freeMemory();
        String KBMemStr = NumberFormat.getNumberInstance(Locale.US).format(freeMem / 1024);
        String BytesStr = NumberFormat.getNumberInstance(Locale.US).format(freeMem);
        TASKMAN.getDataMgr().ChangeValue("FreeMemB", Namespace, BytesStr);
        TASKMAN.getDataMgr().ChangeValue("FreeMemKB", Namespace, KBMemStr);
    }

    private String GetTimeString(long seconds)
    {
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;

        String strRet = String.format("%02d", sec);
        if (seconds > 60)
        {
            strRet = String.format("%02d", minutes) + ":" + strRet;
        }
        if (seconds > 3600)
        {
            strRet = String.format("%02d", hours) + ":" + strRet;
        }
        if (seconds > 86400)
        {
            strRet = Long.toString(days) + ":" + strRet;
        }

        return strRet;
    }
}
