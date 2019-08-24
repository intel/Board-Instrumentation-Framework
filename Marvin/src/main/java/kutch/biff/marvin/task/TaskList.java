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

import java.util.ArrayList;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class TaskList
{
    protected  ArrayList<BaseTask> _TaskItems;
    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    protected  TaskManager TASKMAN = TaskManager.getTaskManager();
    
    public TaskList()
    {
        _TaskItems = null;
    }

    public ArrayList<BaseTask> GetTasks()
    {
        return _TaskItems;
    }
    
    public void AddTaskItem(BaseTask objTask)
    {
        if (null == _TaskItems)
        {
            _TaskItems = new ArrayList<>();
        }

        _TaskItems.add(objTask);
    }

    public boolean PerformTasks()
    {
        if (null == _TaskItems)
        {
            LOGGER.severe("Attempted to perform a task with no items!");
            return false;
        }
        
        for (ITask task : _TaskItems)
        {
            if (null != task)
            {
                if (task.getPostponePeriod() > 0)
                {
                    TaskManager.getTaskManager().AddPostponedTaskThreaded(task, task.getPostponePeriod());
                }
                else if (task.getMustBeInGUIThread())
                {
                    TASKMAN.AddDeferredTaskObject(task); // some tasks manipulate the GUI and must be done in GUI thread
                }
                else
                {
                    task.PerformTask();
                }
            }
            else
            {
                LOGGER.severe("Tried to perform a NULL task");
            }
        }
        return true;
    }
}
