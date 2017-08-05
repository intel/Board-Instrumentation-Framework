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
package kutch.biff.marvin.task;

import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class PostponedTask
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private ITask _objTask;
    private double _PerformTime;

    public PostponedTask(ITask objTask, long Period)
    {
        if (null == objTask)
        {
            LOGGER.severe("Invalid Task Object");
            return;
        }
        _objTask = objTask;
        _PerformTime = System.currentTimeMillis() + Period;
    }

    public boolean ReadyToPerform()
    {
        if (System.currentTimeMillis() >= _PerformTime)
        {
            return true;
        }
        return false;
    }

    public void Perform()
    {
        if (null == _objTask)
        {
            LOGGER.severe("Invalid Task Object");
            return;
        }
        _objTask.PerformTask();
    }

}
