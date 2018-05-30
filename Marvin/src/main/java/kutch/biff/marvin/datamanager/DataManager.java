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
package kutch.biff.marvin.datamanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.DynamicDebugWidgetTask;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick Kutch
 */
public class DataManager
{

    private static DataManager _DataManager = null;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private ConcurrentHashMap<String, DataSet> _DataMap;
    private ConcurrentHashMap<String, List<WildcardListItem>> _WildcardDataMap;
    private long _UpdateCount;
    private long _UnassignedDataPoints;

    public DataManager()
    {
        _DataMap = new ConcurrentHashMap<>();
        _WildcardDataMap = new ConcurrentHashMap<>();
        _DataManager = this;
        _UpdateCount = 0;
        _UnassignedDataPoints = 0;
    }

    public static DataManager getDataManager()
    {
        return _DataManager;
    }

    public int NumberOfRegisteredDatapoints()
    {
        return _DataMap.size();
    }

    public long getUpdateCount()
    {
        return _UpdateCount;
    }

    public long getUnassignedCount()
    {
        return _UnassignedDataPoints;
    }

    public void AddListener(String ID, String Namespace, ChangeListener listener)
    {
        if (null == ID || null == Namespace)
        {
            return;
        }

        String Key = Namespace.toUpperCase() + ID.toUpperCase();

        if (false == _DataMap.containsKey(Key))
        {
            _DataMap.put(Key, new DataSet());
        }

        _DataMap.get(Key).addListener(listener);
    }

    @SuppressWarnings("unchecked")
    public void AddWildcardListener(String ID, String Namespace, ChangeListener listener)
    {
        if (null == ID || null == Namespace)
        {
            LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
            return;
        }

        //LOGGER.info("Adding Wildcard Listener for [" + Namespace + "] ID: " + ID);
        String Key = Namespace.toUpperCase();

        if (false == _WildcardDataMap.containsKey(Key))
        {
            WildcardListItem item = new WildcardListItem(ID);
            ArrayList list = new ArrayList<>();
            list.add(item);
            _WildcardDataMap.put(Key, list);
        }

        for (WildcardListItem wcNode : _WildcardDataMap.get(Key)) // go through the list for the namespace
        {
            if (wcNode.getWildCard().equalsIgnoreCase(ID))
            {
                wcNode.getDataSet().addListener(listener);
                return; // found match no need to keep going, each pattern is unique but not repeated
            }
        }
        // Not found, so add a new listener
        WildcardListItem item = new WildcardListItem(ID);
        item.getDataSet().addListener(listener);
        _WildcardDataMap.get(Key).add(item);
    }

    private boolean HandleWildcardChangeValue(String ID, String Namespace, String Value)
    {
        String Key = Namespace.toUpperCase();
        boolean RetVal = false;
        if (_WildcardDataMap.containsKey(Key))
        {
            for (WildcardListItem wcNode : _WildcardDataMap.get(Key)) // go through the list for the namespace
            {
                if (wcNode.Matches(ID))
                {
                    wcNode.getDataSet().setLatestValue(ID + ":" + Value); // need 2 pass ID here, since it's a RegEx
                    RetVal = true;
                }
            }
        }

        return RetVal;
    }

    public void ChangeValue(String ID, String Namespace, String Value)
    {
        synchronized (this)
        {
            String Key = Namespace.toUpperCase() + ID.toUpperCase();

            _UpdateCount++;

            boolean inWildcard = HandleWildcardChangeValue(ID, Namespace, Value);

            if (false == _DataMap.containsKey(Key))
            {
                _DataMap.put(Key, new DataSet());

                if (false == inWildcard)
                {
                    _UnassignedDataPoints++;

                    LOGGER.info("Received Data update not associated with a widget: " + Namespace + " : " + ID + " [" + Value + "]");
                    // nifty stuff to dynamically add a tab to show 'unregistered' data points.
                    if (kutch.biff.marvin.widget.DynamicTabWidget.isEnabled())
                    {
                        DynamicDebugWidgetTask objTask = new DynamicDebugWidgetTask(Namespace, ID, Value);
                        TaskManager.getTaskManager().AddPostponedTask(objTask, 0);
                    }
                }
            }
            if (_DataMap.containsKey(Key)) // if didn't exist, is created above
            {
                _DataMap.get(Key).setLatestValue(Value);
            }
        }
    }

    public String GetValue(String ID, String Namespace)
    {
        synchronized (this)
        {
            if (null == ID || null == Namespace)
            {
                LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
                return null;
            }

            String Key = Namespace.toUpperCase() + ID.toUpperCase();

            if (_DataMap.containsKey(Key))
            {
                return _DataMap.get(Key).getLatestValue();
            }
            return null;
        }
    }
    
    public String GetValueForMath(String ID, String Namespace)
    {
        synchronized (this)
        {
            if (null == ID || null == Namespace)
            {
                LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
                return null;
            }

            String Key = Namespace.toUpperCase() + ID.toUpperCase();

            if (_DataMap.containsKey(Key))
            {
                return _DataMap.get(Key).getLatestValueForMath();
            }
            return null;
        }
    }
    

    public int PerformUpdates()
    {
        int updatesPerformed = 0;
        for (Map.Entry<String, DataSet> entry : _DataMap.entrySet())
        {
            updatesPerformed += entry.getValue().Update();
        }
        if (!_WildcardDataMap.isEmpty())
        {
            for (String Key : _WildcardDataMap.keySet()) // go through each namespace
            {
                for (WildcardListItem wcNode : _WildcardDataMap.get(Key)) // go through the list for the namespace
                {
                    updatesPerformed += wcNode.getDataSet().Update();
                }
            }
        }
        return updatesPerformed;
    }
}
