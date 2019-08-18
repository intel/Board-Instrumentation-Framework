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
package kutch.biff.marvin.datamanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.util.Pair;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.DynamicDebugWidgetTask;
import kutch.biff.marvin.task.LateCreateTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.GenerateDatapointInfo;
import kutch.biff.marvin.utility.Glob;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandTabBuilder;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandWidgetBuilder;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class DataManager
{
    private static DataManager _DataManager = null;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    private ConcurrentHashMap<String, DataSet> _DataMap;
    private Map<String, List<GenerateDatapointInfo>> _ProxyIDMap;
    private ConcurrentHashMap<String, List<WildcardListItem>> _WildcardDataMap;
    private final Queue<Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder>> _OnDemandQueue; // a queue solves some of
											       // my concurency issues
    private final Queue<GenerateDatapointInfo> __GenerateDatapointList;
    private long _UpdateCount;
    private long _UnassignedDataPoints;
    private boolean __DynamicTabRegistered = false;
    
    public DataManager()
    {
	_DataMap = new ConcurrentHashMap<>();
	_WildcardDataMap = new ConcurrentHashMap<>();
	_DataManager = this;
	_UpdateCount = 0;
	_UnassignedDataPoints = 0;
	_OnDemandQueue = new ConcurrentLinkedQueue<>();
	__GenerateDatapointList = new ConcurrentLinkedQueue<>();
	_ProxyIDMap = new HashMap<>();
    }
    
    public boolean DynamicTabRegistered()
    {
	return __DynamicTabRegistered;
    }
    
    public boolean AddGenerateDatapointInfo(GenerateDatapointInfo genInfo)
    {
	String key;
	if (null == genInfo.getProxyID())
	{
	    key = this.toString();
	}
	else
	{
	    key = genInfo.getProxyID().toUpperCase();
	    
	}
	if (!_ProxyIDMap.containsKey(key))
	{
	    _ProxyIDMap.put(key, new ArrayList<GenerateDatapointInfo>());
	}
	
	_ProxyIDMap.get(key).add(genInfo); // add to list
	__GenerateDatapointList.add(genInfo);
	return true;
    }
    
    public void UpdateGenerateDatapointProxy(String proxyID, String newNamespaceCriteria, String newIDCriterea, String newListEntry)
    {
	if (!_ProxyIDMap.containsKey(proxyID.toUpperCase()))
	{
	    LOGGER.warning("Unknown ProxyID:" + proxyID);
	    return;
	}
	
	LOGGER.info("Updating proxy [" + proxyID + "] to [" + newNamespaceCriteria + ":" + newIDCriterea + "]");
	for (GenerateDatapointInfo genInfo : _ProxyIDMap.get(proxyID.toUpperCase()))
	{
	    genInfo.ProxyReset(newNamespaceCriteria, newIDCriterea,newListEntry);
	}
    }
    
    public Queue<GenerateDatapointInfo> getGenerateDatapointList()
    {
	return __GenerateDatapointList;
    }
    
    public void AddOnDemandWidgetCriterea(DynamicItemInfoContainer criterea, OnDemandWidgetBuilder objBuilder)
    {
	_OnDemandQueue.add(new Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder>(criterea, objBuilder));
	
	if (objBuilder instanceof OnDemandTabBuilder)
	{
	    __DynamicTabRegistered = true; // flag so can know if something is registered for startup check for any tabs
	}
    }
    
    public Queue<Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder>> getOnDemandList()
    {
	return _OnDemandQueue;
    }
    
    public static DataManager getDataManager()
    {
	return _DataManager;
    }
    
    public int NumberOfRegisteredDatapoints()
    {
	return _DataMap.size();
    }
    
    public int getQueuedSize()
    {
	int tSize = 0;
	for (Map.Entry<String, DataSet> entry : _DataMap.entrySet())
	{
	    DataSet objData = entry.getValue();
	    tSize += objData.getSize();
	}
	return tSize;
    }
    
    public long getUpdateCount()
    {
	return _UpdateCount;
    }
    
    public long getUnassignedCount()
    {
	return _UnassignedDataPoints;
    }
    
    public void RemoveListener(String ID, String Namespace, ChangeListener<?> listener)
    {
	if (null == ID || null == Namespace)
	{
	    return;
	}
	
	String Key = Utility.generateKey(Namespace, ID);
	
	if (_DataMap.containsKey(Key))
	{
	    synchronized (this)
	    {
		_DataMap.get(Key).removeListener(listener);
	    }
	}
    }
    
    public void RemoveListener(ChangeListener<?> listener)
    {
	// super inefficient.....
	for (String key : _DataMap.keySet())
	{
	    _DataMap.get(key).removeListener(listener);
	}
    }
    
    public void AddListener(String ID, String Namespace, ChangeListener<?> listener)
    {
	if (null == ID || null == Namespace)
	{
	    return;
	}
	
	String Key = Utility.generateKey(Namespace, ID);
	
	if (false == _DataMap.containsKey(Key))
	{
	    _DataMap.put(Key, new DataSet());
	}
	
	_DataMap.get(Key).addListener(listener);
    }
    
    public void AddWildcardListener(String ID, String Namespace, ChangeListener<?> listener)
    {
	if (null == ID || null == Namespace)
	{
	    LOGGER.severe("Wildcard listener has no Namespace or RegEx pattern");
	    return;
	}
	
	// LOGGER.info("Adding Wildcard Listener for [" + Namespace + "] ID: " + ID);
	String Key = Namespace.toUpperCase();
	
	if (false == _WildcardDataMap.containsKey(Key))
	{
	    WildcardListItem item = new WildcardListItem(ID);
	    List<WildcardListItem> list = new ArrayList<>();
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
	boolean OnDemandItemFound = false;
	boolean OnDemandTabFound = false;
	synchronized (this)
	{
	    for (Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder> entry : _OnDemandQueue)
	    {
		if (entry.getKey().Matches(Namespace, ID, Value))
		{
		    LateCreateTask objTask = new LateCreateTask(entry.getValue(), Namespace, ID, Value,
			    entry.getKey().getLastMatchedSortStr());
		    TaskManager.getTaskManager().AddDeferredTaskObject(objTask);
		    OnDemandItemFound = true;
		    if (entry.getValue() instanceof OnDemandTabBuilder)
		    {
			OnDemandTabFound = true;
		    }
		}
	    }
	    if (OnDemandItemFound)
	    {
		Configuration.getConfig().setCursorToWait();
	    }
	    // go and handle any GenerateDatapoint stuff
	    for (GenerateDatapointInfo info : __GenerateDatapointList)
	    {
		if (info.Matches(Namespace, ID))
		{
		    info.BuildDatapoint(Namespace, ID);
		}
	    }
	}
	
	synchronized (this)
	{
	    String Key = Utility.generateKey(Namespace, ID);
	    
	    _UpdateCount++;
	    
	    boolean inWildcard = HandleWildcardChangeValue(ID, Namespace, Value);
	    
	    if (false == _DataMap.containsKey(Key))
	    {
		_DataMap.put(Key, new DataSet());
		
		if (false == inWildcard)
		{
		    _UnassignedDataPoints++;
		    
		    // LOGGER.info("Received Data update not associated with a widget: " + Namespace
		    // + " : " + ID + " [" + Value + "]");
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
	if (true == OnDemandTabFound)
	{
//            ApplyOnDemandTabStyle objTask = new ApplyOnDemandTabStyle();
//            TaskManager.getTaskManager().AddPostponedTask(objTask, 1000);
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
	    
	    String Key = Utility.generateKey(Namespace, ID);
	    
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
	    
	    String Key = Utility.generateKey(Namespace, ID);
	    
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
    
    public int PulseDataPoint(String namespaceCriterea, String idCriterea)
    {
	int count = 0;
	if (null == namespaceCriterea || null == idCriterea)
	{
	    return count;
	}
	
	String strCompare = Utility.generateKey(namespaceCriterea, idCriterea);
	
	for (String Key : _DataMap.keySet())
	{
	    if (Glob.check(strCompare, Key))
	    {
		String parts[] = Utility.splitKey(Key);
		if (parts.length != 2)
		{
		    LOGGER.severe("Unknown problem trying to perform PulseDataPoint. Key=" + Key);
		}
		else
		{
		    String Namespace = parts[0];
		    String ID = parts[1];
		    String Value = _DataMap.get(Key).getLatestValue();
		    ChangeValue(ID, Namespace, Value);
		    count++;
		}
	    }
	}
	
	return count;
    }
}
