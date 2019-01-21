/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class GenerateDatapointInfo
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final ArrayList<Pair<String,String>> __includeCriterea;
    private final ArrayList<Pair<String,String>> __excludeCriterea;
    private final String __Namespace,__ID;
    private final Map<String, Boolean> __PreviouslyChecked;
    private final Map<String,Pair<Float,Boolean>> __dirtyMap;
    private final Map<String,ChangeListener> _listenerMap;
    private int __precision;
    private int __minFrequency;
    private long __lastUpdate;
    
    public enum GenerateMethod
    {
        ADD, AVERAGE, INVALID
    };  
    
    public enum RefreshPolicy
    {
        REMOVE,REUSE,ZERO_OUT,INVALD
    };
    private GenerateMethod _Method;
    private RefreshPolicy _Policy;
    public GenerateDatapointInfo(String namespace, String id,ArrayList<Pair<String,String>> includeList, ArrayList<Pair<String,String>> excludeList)
    {
        _Method = GenerateMethod.INVALID;
        _Policy = RefreshPolicy.INVALD;
        __includeCriterea = includeList;
        __excludeCriterea = excludeList;
        __dirtyMap = new HashMap<>();
        _listenerMap = new HashMap<>();
        __Namespace = namespace;
        __ID = id;
        __PreviouslyChecked = new HashMap<>();
        __precision = 2;
        __minFrequency = 0;
        __lastUpdate = System.currentTimeMillis();
    }
    
    public void setMethod(GenerateMethod method)
    {
        _Method = method;
    }

    public int getPrecision()
    {
        return __precision;
    }

    public RefreshPolicy getPolicy()
    {
        return _Policy;
    }

    public void setPolicy(RefreshPolicy _Policy)
    {
        this._Policy = _Policy;
    }

    public void setPrecision(int __precision)
    {
        this.__precision = __precision;
    }

    public int getMinFrequency()
    {
        return __minFrequency;
    }

    public void setMinFrequency(int __minFrequency)
    {
        this.__minFrequency = __minFrequency;
    }
    
    public boolean Matches(String checkNamespace, String checkID)
    {
        String namespace=checkNamespace.toUpperCase();
        String id = checkID.toUpperCase();
        // if already checked, no need to do it again
        if (__PreviouslyChecked.containsKey(namespace + id))
        {
            return false;
        }
        if (Matches(namespace,id,__includeCriterea) && ! Matches(namespace,id,__excludeCriterea))
        {
            __PreviouslyChecked.put(namespace + id, true);
            return true;
        }
        else
        {
            __PreviouslyChecked.put(namespace + id, false);
        }
        return false;
    }
    
    private boolean Matches(String checkNamespace, String checkID, ArrayList<Pair<String,String>> criterea )
    {
        for (Pair<String,String> matchPattern : criterea)
        {
            if (Glob.check(matchPattern.getKey(),checkNamespace) && Glob.check(matchPattern.getValue(),checkID))
            {
                return true;
            }
        }
        return false;
    }
    
    private void ZeroOutStaleEntries()
    {
        for ( String key :__dirtyMap.keySet())
        {
            if (!__dirtyMap.get(key).getValue())
            {
                Pair<Float,Boolean> entry = new Pair<>(new Float(0.0),false);
                __dirtyMap.put(key, entry);
            }
        }
    }
    
    private void RemoveOutStaleEntries()
    {
        Map<String,Pair<Float,Boolean>> newDirtyMap = new HashMap<>();
        
        for ( String key :__dirtyMap.keySet())
        {
            if (__dirtyMap.get(key).getValue()) // have received an update recently, so keep
            {
                newDirtyMap.put(key, __dirtyMap.get(key));
            }
            else
            {
                ChangeListener objListener = _listenerMap.get(key);
                _listenerMap.remove(key);
                LOGGER.info("Removing Stale GenerateDataPoing Input: " + key);
                // very inefficeint - this should be re-woredk.
                DataManager.getDataManager().RemoveListener(objListener);
                __PreviouslyChecked.remove(key); // in case it comes back
            }
        }
        __dirtyMap.clear();
        __dirtyMap.putAll(newDirtyMap);
    }
    
    private void CheckForUpdate()
    {
        float Total = 0;
        boolean forceUpdate = false;
        if (__minFrequency > 0)
        {
            if (__lastUpdate + __minFrequency < System.currentTimeMillis())
            {
                if (_Policy == RefreshPolicy.ZERO_OUT)
                {
                    ZeroOutStaleEntries();
                }
                else if (_Policy == RefreshPolicy.REUSE)
                {
                    forceUpdate = true;
                }
                else if (_Policy == RefreshPolicy.REMOVE)
                {
                    RemoveOutStaleEntries();
                }
            }
        }
        for ( String key :__dirtyMap.keySet())
        {
            if (__dirtyMap.get(key).getValue() || forceUpdate)
            {
                Total += __dirtyMap.get(key).getKey();
            }
            else
            {
                return;
            }
        }
        if (_Method == GenerateMethod.ADD || Total == 0.0)
        {   
        }
        else
        {
            Total/=__dirtyMap.size();
        }
        // this likely needs to be a postponted task - otherwise can have endless loop of a mobius strip (maybe a marvindata task...
        //DataManager.getDataManager().ChangeValue(__ID, __Namespace, Float.toString(Total));
        MarvinTask mt = new MarvinTask();
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(__precision);
        mt.AddDataset(__ID, __Namespace, df.format(Total));
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
        
        for ( String key :__dirtyMap.keySet())
        {   // go flip the dirty bit
            Pair<Float,Boolean> entry = new Pair<>(__dirtyMap.get(key).getKey(),false);
            __dirtyMap.put(key, entry);
        }        
        __lastUpdate = System.currentTimeMillis();
    }
    
    public void BuildDatapoint(String inputNamespace, String inputID)
    {
       //LOGGER.info(String.format("Adding Input of %s:%s to Build Data point %s:%s",inputNamespace,inputID,__Namespace,__ID));
        ChangeListener objListener = new ChangeListener()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                try
                {
                    Pair<Float,Boolean> entry = new Pair<>(Float.parseFloat(newVal.toString()),true);
                    __dirtyMap.put(inputNamespace.toLowerCase() + inputID.toLowerCase(), entry);
                    CheckForUpdate();
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.severe(String.format("GenerateDatapoint can only accept inputs that are numberic. [%s,%s] does not meet this.",inputNamespace,inputID));
                }
            }
        };
        DataManager.getDataManager().AddListener(inputID, inputNamespace, objListener);
        _listenerMap.put(inputNamespace.toUpperCase() + inputID.toLowerCase(),objListener);
    }
}
