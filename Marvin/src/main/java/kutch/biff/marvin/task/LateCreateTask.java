/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.TabWidget;

/**
 *
 * @author Patrick
 */
public class LateCreateTask extends BaseTask
{
    private BaseWidget __lateCreateObject = null;
    private GridPane   __parentPane = null;
    private TabPane    __parentTabPane = null;
    private int        __tabIndex;

    public LateCreateTask(BaseWidget objWidget,TabPane parentTab,int index)
    {
        __lateCreateObject = objWidget;
        __parentTabPane = parentTab;
        __tabIndex = index;
    }
    
    public LateCreateTask(BaseWidget objWidget,GridPane parent)
    {
        __lateCreateObject = objWidget;
        __parentPane = parent;
    }
    @Override
    public  void PerformTask()
    {
        if (null == __parentPane) // is null when a Tab
        {
            HandleCreateTab();
        }
        else
        {
            HandleCreateWidget();
        }
    }
    
    private void HandleCreateTab()
    {
        if (null == __lateCreateObject)
        {
            LOGGER.severe("Attempted to perform LateCreateTask on object, however no object provided");
            return;
        }
        TabWidget _tabObj = (TabWidget)__lateCreateObject;
        if (_tabObj.Create(__parentTabPane, DataManager.getDataManager(),__tabIndex))
        {
            __lateCreateObject.PerformPostCreateActions(null, false);
            ConfigurationReader.GetConfigReader().getTabs().add((TabWidget) _tabObj);
            
            LOGGER.info("Performed LateCreateTask on Tab: " + _tabObj.getName());
        }
        else
        {
            LOGGER.info("Error ocurred performing LateCreateTask on Tab: " + _tabObj.getName());
        }
    }
    
    private void HandleCreateWidget()
    {
        if (null == __lateCreateObject)
        {
            LOGGER.severe("Attempted to perform LateCreateTask on widget, however no widget provided");
            return;
        }
        if (__lateCreateObject.Create(__parentPane, DataManager.getDataManager()))
        {
            LOGGER.info("Performed LateCreateTask on Widget: " + __lateCreateObject.getName());
        }
        else
        {
            LOGGER.info("Error ocurred performing LateCreateTask on Widget: " + __lateCreateObject.getName());
        }
    }
}
