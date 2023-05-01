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

import java.util.Iterator;

/**
 * @author Patrick
 */
public class SteppedTaskList extends TaskList {
    private boolean _Looped;
    Iterator<BaseTask> _iter = null;

    public SteppedTaskList() {
        super();
        _Looped = true;
    }

    @Override
    public boolean PerformTasks() {
        if (null == _TaskItems) {
            LOGGER.severe("Attempted to perform a task with no items!");
            return false;
        }

        if (null == _iter) {
            _iter = _TaskItems.iterator();
        }

        if (_iter.hasNext()) {
            ITask task = _iter.next();

            if (null != task) {
                if (task.getPostponePeriod() > 0) {
                    TaskManager.getTaskManager().AddPostponedTaskThreaded(task, task.getPostponePeriod());
                } else if (task.getMustBeInGUIThread()) {
                    TASKMAN.AddDeferredTaskObject(task); // some tasks manipulate the GUI and must be done in GUI thread
                } else {
                    task.PerformTask();
                }
            } else {
                LOGGER.severe("Tried to perform a NULL task");
            }
        }
        if (!_iter.hasNext()) {
            if (!_Looped) {
                _iter = null; // reset, will run again if 'clicked' or selected
            } else {
                _iter = _TaskItems.iterator();
            }
        }

        return true;
    }

    public void setLooped(boolean fValue) {
        _Looped = fValue;
    }

}
