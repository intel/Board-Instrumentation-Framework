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
package kutch.biff.marvin.widget.widgetbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.control.TabPane;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.ApplyOnDemandTabStyle;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.TabWidget;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class OnDemandTabBuilder implements OnDemandWidgetBuilder
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String __tabID;
    private int __builtCount = 0;
    private FrameworkNode __node;
    private int __tabIndex;
    DynamicItemInfoContainer __onDemandTrigger;
    private List<TabWidget> __createdTabs;

    public OnDemandTabBuilder(String tabID, int index, DynamicItemInfoContainer info)
    {
        __tabID = tabID;
        __tabIndex = index;
        __onDemandTrigger = info;
        __node = null;
        __createdTabs = new ArrayList<TabWidget>();
    }

    public String getTabID()
    {
        return __tabID;
    }

    public void setSourceNode(FrameworkNode sourceNode)
    {
        __node = sourceNode;
    }

    @Override
    public boolean Build(String Namespace, String ID, String Value, String strSortValue)
    {
        LOGGER.info("Creating OnDemand Tab for namespace: " + Namespace + ",  using Tab template ID: " + __tabID);
        Configuration config = Configuration.getConfig();
        TabPane parentPane = config.getPane();

        __builtCount++;

        String strTabID = __tabID + "." + Integer.toString(__builtCount);
        AliasMgr.getAliasMgr().PushAliasList(true);
        TabWidget tab = new TabWidget(strTabID);
        __createdTabs.add(tab);
        tab.setOnDemandSortBy(strSortValue);
        AliasMgr.getAliasMgr().AddAlias("TriggeredNamespace", Namespace); // So tab knows namespace
        AliasMgr.getAliasMgr().AddAlias("TriggeredID", ID);
        AliasMgr.getAliasMgr().AddAlias("TriggeredValue", Value);
        AliasMgr.getAliasMgr().AddAlias("TriggeredIndex", Integer.toString(__builtCount));
        AliasMgr.getAliasMgr().AddAlias("TabID", strTabID);
        __onDemandTrigger.tokenizeAndCreateAlias(ID);

        tab = ConfigurationReader.ReadTab(__node, tab, strTabID);

        if (null != tab)
        {
            tab.setCreatedOnDemand();
            if (tab.Create(parentPane, DataManager.getDataManager(), __tabIndex))
            {
                ConfigurationReader.GetConfigReader().getTabs().add(tab);
                tab.PerformPostCreateActions(null, false);

                LOGGER.info("Performed LateCreateTask on Tab: " + tab.getName());
            }
            else
            {
                LOGGER.info("Error ocurred performing LateCreateTask on Tab: " + tab.getName());
            }
        }
        else
        {
            return false;
        }
        AliasMgr.getAliasMgr().PopAliasList();
        TabWidget.ReIndexTabs(parentPane);
        TaskManager tm = TaskManager.getTaskManager();
        ApplyOnDemandTabStyle objTask = new ApplyOnDemandTabStyle();
        tm.AddPostponedTask(objTask, 1000);
        if (null != tab.getOnDemandTask())
        {
            tm.AddDeferredTask(tab.getOnDemandTask());
        }

        return true;
    }

    public void ApplyOddEvenStyle()
    {
        int iIndex = 1;
        for (TabWidget tabWidget : ConfigurationReader.GetConfigReader().getTabs())
        {
            if (__createdTabs.contains(tabWidget))  // on demand tab, that was created by this builder
            {
                __onDemandTrigger.ApplyOddEvenStyle(tabWidget, iIndex, tabWidget.getTitle());
            }
            iIndex++;
        }
    }
}
