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
    private final ArrayList<Pair<String, String>> __includeCriterea;
    private final ArrayList<Pair<String, String>> __excludeCriterea;
    private final String __Namespace, __ID;
    private final Map<String, Boolean> __PreviouslyChecked;
    private final Map<String, Pair<String, Boolean>> __dirtyMap;
    private final Map<String, ChangeListener> _listenerMap;
    private int __precision;
    private int __minFrequency;
    private long __lastUpdate;
    private double __Scale;
    private String __ProxyID;
    private int __csvEntry;
    
    public enum GenerateMethod
    {
	ADD, AVERAGE, PROXY, INVALID
    };
    
    public enum RefreshPolicy
    {
	REMOVE, REUSE, ZERO_OUT, INVALD
    };
    
    private GenerateMethod _Method;
    private RefreshPolicy _Policy;
    
    public GenerateDatapointInfo(String namespace, String id, ArrayList<Pair<String, String>> includeList,
	    ArrayList<Pair<String, String>> excludeList)
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
	__Scale = 1.0;
	__ProxyID = null;
	__csvEntry = -1;
    }
    
    public int getListEntry()
    {
	return __csvEntry;
    }
    
    public boolean setListEntry(int newVal)
    {
	if (newVal < 0)
	{
	    return false;
	}
	__csvEntry = newVal;
	return true;
    }
    
    public String getProxyID()
    {
	return __ProxyID;
    }
    
    public void setProxyID(String id)
    {
	__ProxyID = id;
    }
    
    public void setMethod(GenerateMethod method)
    {
	_Method = method;
    }
    
    public double getScale()
    {
	return __Scale;
    }
    
    public void setScale(double __Scale)
    {
	this.__Scale = __Scale;
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
	String namespace = checkNamespace.toUpperCase();
	String id = checkID.toUpperCase();
	// if already checked, no need to do it again
	if (__PreviouslyChecked.containsKey(namespace + id))
	{
	    return false;
	}
	if (Matches(namespace, id, __includeCriterea) && !Matches(namespace, id, __excludeCriterea))
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
    
    private boolean Matches(String checkNamespace, String checkID, ArrayList<Pair<String, String>> criterea)
    {
	for (Pair<String, String> matchPattern : criterea)
	{
	    if (Glob.check(matchPattern.getKey(), checkNamespace) && Glob.check(matchPattern.getValue(), checkID))
	    {
		return true;
	    }
	}
	return false;
    }
    
    private void ZeroOutStaleEntries()
    {
	for (String key : __dirtyMap.keySet())
	{
	    if (!__dirtyMap.get(key).getValue())
	    {
		Pair<String, Boolean> entry = new Pair<>("0.0", false);
		__dirtyMap.put(key, entry);
	    }
	}
    }
    
    private void RemoveOutStaleEntries()
    {
	Map<String, Pair<String, Boolean>> newDirtyMap = new HashMap<>();
	
	for (String key : __dirtyMap.keySet())
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
	String dataStr = null;
	String proxyID=null,proxyNS=null;
	synchronized (__dirtyMap)
	{
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
	    for (String key : __dirtyMap.keySet())
	    {
		if (_Method == GenerateMethod.PROXY)
		{
		    String parts[] = Utility.splitKey(key);
		    proxyNS = parts[0];
		    proxyID = parts[1];
		    try
		    {
			Total += Float.parseFloat(__dirtyMap.get(key).getKey());
		    }
		    
		    catch(NumberFormatException ex)
		    {
			dataStr = __dirtyMap.get(key).getKey();
		    }
		    int l = __dirtyMap.size();
		    LOGGER.info(Integer.toString(l));
		    //break; // for proxy, only 1 value will be there
		}
		// Value contains boolean, if false, then it isn't ready to be processed
		// so don't worry about it, and return.
		if (__dirtyMap.get(key).getValue() || forceUpdate)
		{
		    Total += Float.parseFloat(__dirtyMap.get(key).getKey());
		}
		else
		{
		    return; // not all of them had been updated
		}
	    }
	    
	    if (_Method == GenerateMethod.AVERAGE && Total != 0.0)
	    {
		Total /= __dirtyMap.size();
	    }
	    // this likely needs to be a postponed task - otherwise can have endless loop of
	    // a mobius strip (maybe a marvindata task...
	    // DataManager.getDataManager().ChangeValue(__ID, __Namespace,
	    // Float.toString(Total));
	    
	    try
	    {
		Total *= __Scale;
	    }
	    catch(NumberFormatException ex)
	    {
		
	    }
	    
	    for (String key : __dirtyMap.keySet())
	    { // go flip the dirty bit
		Pair<String, Boolean> entry = new Pair<>(__dirtyMap.get(key).getKey(), false);
		__dirtyMap.put(key, entry);
	    }
	}
	
	MarvinTask mt = new MarvinTask();
	DecimalFormat df = new DecimalFormat();
	df.setGroupingUsed(false);
	df.setMaximumFractionDigits(__precision);
	df.setMinimumFractionDigits(__precision);

	try
	{
	    dataStr = df.format(Total);
	}
	    catch(NumberFormatException ex)
	    {
		if (null == dataStr)
		    {
		    	dataStr= "oops"; 
		    }
	    }
	
	if (_Method == GenerateMethod.PROXY)
	{

	    mt.AddDataset(proxyID , __Namespace, dataStr );
	    LOGGER.info(proxyID);
	}
	else
	{
	    mt.AddDataset(__ID, __Namespace, dataStr );
	}
	TaskManager.getTaskManager().AddDeferredTaskObject(mt);
	__lastUpdate = System.currentTimeMillis();
    }
    
    public void BuildDatapoint(String inputNamespace, String inputID)
    {
	// LOGGER.info(String.format("Adding Input of %s:%s to Build Data point
	// %s:%s",inputNamespace,inputID,__Namespace,__ID));
	ChangeListener objListener = new ChangeListener()
	{
	    @Override
	    public void changed(ObservableValue o, Object oldVal, Object newVal)
	    {
		if (__csvEntry > -1)
		{
		    String parts[] = newVal.toString().split(",");
		    if (parts.length < __csvEntry)
		    {
			LOGGER.severe(String.format(
				"GenerateDatapoint indicated invalid ListEntry, data does not match. [%d,%s] does not meet this.",
				__csvEntry, newVal.toString()));
			return;
		    }
		    newVal = parts[__csvEntry]; // make newVal then let the rest parse it
		}
		try
		{
		    if (_Method != GenerateMethod.PROXY)
		    {
			Float.parseFloat(newVal.toString()); // only allow numeric values for non proxy
		    }
		    Pair<String, Boolean> entry = new Pair<>(newVal.toString(), true);
		    __dirtyMap.put(Utility.generateKey(inputNamespace, inputID), entry);
		    CheckForUpdate();
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe(String.format(
			    "GenerateDatapoint can only accept inputs that are numeric. [%s,%s] does not meet this.",
			    inputNamespace, inputID));
		}
	    }
	};
	DataManager.getDataManager().AddListener(inputID, inputNamespace, objListener);
	_listenerMap.put(inputNamespace.toUpperCase() + inputID.toLowerCase(), objListener);
    }
}
