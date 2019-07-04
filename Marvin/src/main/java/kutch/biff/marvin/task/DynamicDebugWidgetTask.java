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

import kutch.biff.marvin.widget.DynamicTabWidget;

/**
 *
 * @author Patrick Kutch
 */
public class DynamicDebugWidgetTask extends BaseTask
{
    private final TaskManager TASKMAN = TaskManager.getTaskManager();
    private String Namespace;
    private String ID;
    private String Value;
    
    public DynamicDebugWidgetTask(String Namespace, String ID, String Value)
    {
        this.Namespace = Namespace;
        this.ID = ID;
        this.Value = Value;
    }
    
    @Override
    public boolean getMustBeInGUIThread()
    {
        return true;
    }    
    @Override
    public void PerformTask()
    {
        DynamicTabWidget objWidget = DynamicTabWidget.getTab(Namespace,TASKMAN.getDataMgr());
        objWidget.AddWidget(TASKMAN.getDataMgr(), ID, Value);
    }
}
