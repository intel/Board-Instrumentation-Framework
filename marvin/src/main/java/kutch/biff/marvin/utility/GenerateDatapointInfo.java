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
    @SuppressWarnings("rawtypes")
    private final Map<String, ChangeListener> _listenerMap;
    private int __precision;
    private int __minFrequency;
    private long __lastUpdate;
    private double __Scale;
    private String __ProxyID;
    private int __csvEntry;
    private String __splitToken;
    
    public enum GenerateMethod
    {
	ADD, AVERAGE, PROXY, SPLIT_LIST, INVALID
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
	__splitToken = null;
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
    
    public void setSplitToken(String newToken)
    {
	__splitToken = newToken;
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
	if (GenerateMethod.PROXY == _Method)
	{
	    setPrecision(-1);
	}
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
    
    public void ProxyReset(String newNamespaceFilter, String newIDFilter)
    {
	synchronized (__dirtyMap)
	{
	    removeAllListeners();
	    __dirtyMap.clear();
	    
	    Pair<String, String> current = __includeCriterea.get(0);
	    if (null == newNamespaceFilter)
	    {
		newNamespaceFilter = current.getKey();
	    }
	    if (null == newIDFilter)
	    {
		newIDFilter = current.getValue();
	    }
	    __includeCriterea.clear(); // only 1 criteria for proxy
	    __includeCriterea.add(new Pair<String, String>(newNamespaceFilter, newIDFilter));
	}
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
		ChangeListener<?> objListener = _listenerMap.get(key);
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
    
    private void removeAllListeners()
    {
	synchronized (this)
	{
	    
	    for (String key : _listenerMap.keySet())
	    {
		// go through all listeners for this data and nuke it.
		ChangeListener<?> objListener = _listenerMap.get(key);
		// very inefficient - this should be re-worked.
		DataManager.getDataManager().RemoveListener(objListener);
	    }
	    _listenerMap.clear();
	    __PreviouslyChecked.clear();
	}
    }
    
    private void HandleProxiedValue(String proxyNS, String proxyID, String strValue)
    {
	MarvinTask mt = new MarvinTask();
	String dataStr;
	
	try
	{
	    float value;
	    value = Float.parseFloat(strValue);
	    value *= __Scale;
	    int precision = __precision;
	    
	    if (-1 == precision)
	    { // use precision of value received
		int integerPlaces = strValue.indexOf('.');
		if (integerPlaces > 0)
		{
		    precision = strValue.length() - integerPlaces - 1;
		}
		else
		{
		    precision = 0;
		}
		
	    }
	    DecimalFormat df = new DecimalFormat();
	    df.setGroupingUsed(false);
	    df.setMaximumFractionDigits(precision);
	    df.setMinimumFractionDigits(precision);
	    
	    dataStr = df.format(value);
	}
	
	catch(NumberFormatException ex)
	{
	    dataStr = strValue;
	}
	// Can use wildcard for target names, only for Proxy at moment
	mt.AddDataset(Utility.combineWildcards(__ID, proxyID), Utility.combineWildcards(__Namespace, proxyNS), dataStr);
	TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }
    
    private void HandleSplitList(String NS, String ID, String strInpValue)
    {
	String parts[] = strInpValue.split(__splitToken);
	
	if (parts.length < 2)
	{
	    LOGGER.warning("GenerateDatapoint: SplitList received data that was not a list: " + strInpValue);
	    return;
	}
	
	int index = 0;
	
	for (String strValue : parts)
	{
	    MarvinTask mt = new MarvinTask();
	    String dataStr;
	    try
	    {
		float value;
		value = Float.parseFloat(strValue);
		value *= __Scale;
		int precision = __precision;
		
		if (-1 == precision)
		{ // use precision of value received
		    int integerPlaces = strValue.indexOf('.');
		    if (integerPlaces > 0)
		    {
			precision = strValue.length() - integerPlaces - 1;
		    }
		    else
		    {
			precision = 0;
		    }
		    
		}
		DecimalFormat df = new DecimalFormat();
		df.setGroupingUsed(false);
		df.setMaximumFractionDigits(precision);
		df.setMinimumFractionDigits(precision);
		
		dataStr = df.format(value);
		
		index++;
	    }
	    
	    catch(NumberFormatException ex)
	    {
		dataStr = strValue;
	    }
	    // Can use wildcard for target names, only for Proxy at moment
	    // mt.AddDataset(Utility.combineWildcards(__ID, proxyID),
	    // Utility.combineWildcards(__Namespace, proxyNS), dataStr);
	    
	    String newID = Utility.combineWildcards(__ID, ID) + "." + Integer.toString(index);
	    String newNS = Utility.combineWildcards(__Namespace, NS);
	    mt.AddDataset(newID, newNS, dataStr);
	    TaskManager.getTaskManager().AddDeferredTaskObject(mt);
	}
	MarvinTask mt = new MarvinTask();
	
	String newID = Utility.combineWildcards(__ID, ID) + ".count";
	String newNS = Utility.combineWildcards(__Namespace, NS);
	mt.AddDataset(newID, newNS, Integer.toString(parts.length));
	TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }
    
    private void CheckForUpdate(String NS, String ID)
    {
	if (_Method == GenerateMethod.PROXY)
	{
	    LOGGER.severe("Entered CheckForUpdate() for proxied value - should NEVER occur.  Notify Patrick");
	    return;
	}
	
	float Total = 0;
	boolean forceUpdate = false;
	int sourcPrecision = 0;
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
		// Value contains boolean, if false, then it isn't ready to be processed
		// so don't worry about it, and return.
		if (__dirtyMap.get(key).getValue() || forceUpdate)
		{
		    String strValue = __dirtyMap.get(key).getKey();
		    Total += Float.parseFloat(strValue);
		    int integerPlaces = strValue.indexOf('.');
		    int precision;
		    if (integerPlaces < 0)
		    {
			precision = 0;
		    }
			
		    else
		    {
			precision= strValue.length() - integerPlaces - 1;
		    }
		    if (precision > sourcPrecision)
		    {
			sourcPrecision = precision; // use source precision, in case not specified
		    }
		}
		else
		{
		    return; // not all of them had been updated
		}
	    }
	    if (_Method == GenerateMethod.ADD || Total == 0.0)
	    {
	    }
	    else
	    {
		Total /= __dirtyMap.size();
	    }
	    Total *= __Scale;
	    // this likely needs to be a postponted task - otherwise can have endless loop
	    // of a mobius strip (maybe a marvindata task...
	    // DataManager.getDataManager().ChangeValue(__ID, __Namespace,
	    // Float.toString(Total));
	    
	    for (String key : __dirtyMap.keySet())
	    { // go flip the dirty bit - can't do this in main loop because not all could have
	      // been dirty
		Pair<String, Boolean> entry = new Pair<>(__dirtyMap.get(key).getKey(), false);
		__dirtyMap.put(key, entry);
	    }
	}
	
	MarvinTask mt = new MarvinTask();
	DecimalFormat df = new DecimalFormat();
	df.setGroupingUsed(false);
	int precision = __precision;
	
	if (-1 == precision)
	{ // use precision of value received
	    precision = sourcPrecision;
	}
	
	df.setMaximumFractionDigits(precision);
	df.setMinimumFractionDigits(precision);
	String newID = Utility.combineWildcards(__ID, ID);
	String newNS = Utility.combineWildcards(__Namespace, NS);
	mt.AddDataset(newID, newNS, df.format(Total));

//	mt.AddDataset(__ID, __Namespace, df.format(Total));
	TaskManager.getTaskManager().AddDeferredTaskObject(mt);
	__lastUpdate = System.currentTimeMillis();
    }
    
    public void BuildDatapoint(String inputNamespace, String inputID)
    {
	// LOGGER.info(String.format("Adding Input of %s:%s to Build Data point
	// %s:%s",inputNamespace,inputID,__Namespace,__ID));
	ChangeListener<Object> objListener = new ChangeListener<Object>()
	{
	    @Override
	    public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
	    {
		if (__csvEntry > -1)
		{
		    String parts[] = newVal.toString().split(__splitToken);
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
		    if (GenerateMethod.PROXY == _Method)
		    {
			HandleProxiedValue(inputNamespace, inputID, newVal.toString());
		    }
		    else if (GenerateMethod.SPLIT_LIST == _Method)
		    {
			HandleSplitList(inputNamespace, inputID, newVal.toString());
		    }
		    else
		    {
			Float.parseFloat(newVal.toString()); // only allow numeric values for non proxy
			Pair<String, Boolean> entry = new Pair<>(newVal.toString(), true);
			__dirtyMap.put(Utility.generateKey(inputNamespace, inputID), entry);
			CheckForUpdate(inputNamespace, inputID);
		    }
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
	synchronized (this)
	{
	    
	    _listenerMap.put(inputNamespace.toUpperCase() + inputID.toLowerCase(), objListener);
	}
    }
    /*
     * public void DumpPatterns() { for (Pair<String,String> entry :
     * __includeCriterea) { String ns = entry.getKey(); String ID = entry.getKey();
     * if (ns == ID) {
     * 
     * } ID = ns; } }
     */
}
