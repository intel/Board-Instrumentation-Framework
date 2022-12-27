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

/**
 * @author Patrick.Kutch@gmail.com
 */

package kutch.biff.marvin.utility;

import java.text.DecimalFormat;
import java.time.temporal.ValueRange;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class GenerateDatapointInfo {
    public enum GenerateMethod {
        ADD, AVERAGE, PROXY, SPLIT_LIST, MAKE_LIST, MAKE_NAMESPACE_LIST, MAKE_ID_LIST, GET_LIST_SIZE, MAKE_INDEX_LIST,
        INVALID
    }

    public enum ListSortMethod {
        ASCENDING, DESCENDING, NONE;
    }

    public enum RefreshPolicy {
        REMOVE, REUSE, ZERO_OUT, INVALD
    }

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final List<Pair<String, String>> __includeCriterea;
    private final List<Pair<String, String>> __excludeCriterea;
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
    private boolean __useCSV;
    private String __splitToken;

    private Map<String, String> __mapOfListData;
    ;

    private ListSortMethod _sortMethod;

    private String __cachedValue;
    ;

    private GenerateMethod _Method;
    private RefreshPolicy _Policy;
    private ValueRange __dataIndexRange;
    private String __dataIndexToken;
    private boolean __ProcessRanges;

    public GenerateDatapointInfo(String namespace, String id, List<Pair<String, String>> includeList,
                                 List<Pair<String, String>> excludeList, ValueRange valRange, String tokenCharForValue) {
        _Method = GenerateMethod.INVALID;
        _Policy = RefreshPolicy.INVALD;
        _sortMethod = ListSortMethod.ASCENDING;
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
        __splitToken = ",";
        __mapOfListData = new HashMap<>();
        __dataIndexRange = valRange;
        __dataIndexToken = tokenCharForValue;
        __useCSV = false;
        if (__dataIndexRange.getMinimum() > -1) {
            __ProcessRanges = true;
        } else {
            __ProcessRanges = false;
        }
        __cachedValue = "";
    }

    public void BuildDatapoint(String inputNamespace, String inputID) {
        // LOGGER.info(String.format("Adding Input of %s:%s to Build Data point
        // %s:%s",inputNamespace,inputID,__Namespace,__ID));
        ChangeListener<Object> objListener = new ChangeListener<Object>()
                // MarvinChangeListener objListener = new
                // MarvinChangeListener(__dataIndexRange,__dataIndexToken)
        {
            @Override
            public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
            // public void onChanged(String newVal)
            {
                if (__useCSV && __csvEntry > -1) {
                    String parts[] = newVal.toString().split(__splitToken);
                    if (parts.length < __csvEntry) {
                        LOGGER.severe(String.format(
                                "GenerateDatapoint indicated invalid ListEntry, data does not match. [%d,%s] does not meet this.",
                                __csvEntry, newVal.toString()));
                        return;
                    }
                    newVal = parts[__csvEntry]; // make newVal then let the rest parse it
                } else if (__useCSV) {
                    return;
                }
                try {
                    if (GenerateMethod.PROXY == _Method) {
                        HandleProxiedValue(inputNamespace, inputID, newVal.toString());
                    } else if (GenerateMethod.SPLIT_LIST == _Method) {
                        HandleSplitList(inputNamespace, inputID, newVal.toString());
                    } else if (GenerateMethod.MAKE_LIST == _Method) {
                        HandleMakeList(inputNamespace, inputID, newVal.toString());
                    } else if (GenerateMethod.MAKE_NAMESPACE_LIST == _Method) {
                        HandleNamespaceList(inputNamespace);
                    } else if (GenerateMethod.MAKE_ID_LIST == _Method) {
                        HandleIDList(inputNamespace, inputID);
                    } else if (GenerateMethod.GET_LIST_SIZE == _Method) {
                        HandleGetListSize(newVal.toString());
                    } else if (GenerateMethod.MAKE_INDEX_LIST == _Method) {
                        HandleMakeListIndexList(newVal.toString());
                    } else {
                        Float.parseFloat(newVal.toString()); // only allow numeric values for non proxy
                        Pair<String, Boolean> entry = new Pair<>(newVal.toString(), true);
                        __dirtyMap.put(Utility.generateKey(inputNamespace, inputID), entry);
                        CheckForUpdate(inputNamespace, inputID);
                    }
                } catch (NumberFormatException ex) {
                    LOGGER.severe(String.format(
                            "GenerateDatapoint can only accept inputs that are numeric. [%s,%s] does not meet this.",
                            inputNamespace, inputID));
                }
            }
        };
        DataManager.getDataManager().AddListener(inputID, inputNamespace, objListener);
        synchronized (this) {

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

    private void CheckForUpdate(String NS, String ID) {
        if (_Method == GenerateMethod.PROXY) {
            LOGGER.severe("Entered CheckForUpdate() for proxied value - should NEVER occur.  Notify Patrick");
            return;
        }

        float Total = 0;
        boolean forceUpdate = false;
        int sourcePrecision = 0;
        synchronized (__dirtyMap) {
            if (__minFrequency > 0) {
                if (__lastUpdate + __minFrequency < System.currentTimeMillis()) {
                    if (_Policy == RefreshPolicy.ZERO_OUT) {
                        ZeroOutStaleEntries();
                    } else if (_Policy == RefreshPolicy.REUSE) {
                        forceUpdate = true;
                    } else if (_Policy == RefreshPolicy.REMOVE) {
                        RemoveOutStaleEntries();
                    }
                }
            }
            for (String key : __dirtyMap.keySet()) {
                // Value contains boolean, if false, then it isn't ready to be processed
                // so don't worry about it, and return.
                if (__dirtyMap.get(key).getValue() || forceUpdate) {
                    String strValue = __dirtyMap.get(key).getKey();
                    Total += Float.parseFloat(strValue);
                    int integerPlaces = strValue.indexOf('.');
                    int precision;
                    if (integerPlaces < 0) {
                        precision = 0;
                    } else {
                        precision = strValue.length() - integerPlaces - 1;
                    }
                    if (precision > sourcePrecision) {
                        sourcePrecision = precision; // use source precision, in case not specified
                    }
                } else {
                    return; // not all of them had been updated
                }
            }

            if (_Method == GenerateMethod.ADD || Total == 0.0) {
            } else if (_Method == GenerateMethod.AVERAGE) {
                Total /= __dirtyMap.size();
            } else {
                LOGGER.severe("Unknown GenerateDataPoint method: " + _Method);
                return;
            }
            Total *= __Scale;
            // this likely needs to be a postponted task - otherwise can have endless loop
            // of a mobius strip (maybe a marvindata task...
            // DataManager.getDataManager().ChangeValue(__ID, __Namespace,
            // Float.toString(Total));

            for (String key : __dirtyMap.keySet()) { // go flip the dirty bit - can't do this in main loop because not all could have
                // been dirty
                Pair<String, Boolean> entry = new Pair<>(__dirtyMap.get(key).getKey(), false);
                __dirtyMap.put(key, entry);
            }
        }

        MarvinTask mt = new MarvinTask();
        DecimalFormat df = new DecimalFormat();
        df.setGroupingUsed(false);
        int precision = __precision;

        if (-1 == precision) { // use precision of value received
            precision = sourcePrecision;
            if (0 == precision && Total < 1) {
                precision = 2;
            }
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

    public int getListEntry() {
        return __csvEntry;
    }

    public int getMinFrequency() {
        return __minFrequency;
    }

    public RefreshPolicy getPolicy() {
        return _Policy;
    }

    public int getPrecision() {
        return __precision;
    }

    public String getProxyID() {
        return __ProxyID;
    }

    public double getScale() {
        return __Scale;
    }

    private void HandleGetListSize(String strInpValue) {
        if (null == strInpValue) {
            return;
        }
        String strData;
        strData = Integer.toString(strInpValue.split(__splitToken).length);

        MarvinTask mt = new MarvinTask();
        mt.AddDataset(__ID, __Namespace, strData);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }

    private void HandleIDList(String NS, String ID) {
        String Key = NS + ID;
        Key = Key.toUpperCase();
        if (!__mapOfListData.containsKey(Key)) {
            if (__ProcessRanges) {
                ID = BaseWidget.ProcessIndexDataRequest(__dataIndexRange, __dataIndexToken, ID);
                if (null == ID) {
                    return;
                }
            }
            __mapOfListData.put(Key, ID);
            String strData = null;
            String[] entries = new String[__mapOfListData.size()];
            int index = 0;
            for (String key : __mapOfListData.keySet()) {
                entries[index++] = __mapOfListData.get(key);
            }

            if (ListSortMethod.ASCENDING == _sortMethod) {
                java.util.Arrays.sort(entries);

            } else if (ListSortMethod.DESCENDING == _sortMethod) {
                java.util.Arrays.sort(entries, Collections.reverseOrder());
            }
            for (String item : entries) {
                if (null == strData) {
                    strData = item;
                } else {
                    strData += "," + item;
                }
            }
            __cachedValue = strData;
        }
        MarvinTask mt = new MarvinTask();
        mt.AddDataset(__ID, __Namespace, __cachedValue);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }

    private void HandleMakeList(String NS, String ID, String strInpValue) {
        String Key = NS + ID;
        Key = Key.toUpperCase();
        if (!__mapOfListData.containsKey(Key)) {
            __mapOfListData.put(Key, strInpValue);
            String strData = null;
            String[] entries = new String[__mapOfListData.size()];
            int index = 0;
            for (String key : __mapOfListData.keySet()) {
                entries[index++] = __mapOfListData.get(key);
            }

            if (ListSortMethod.ASCENDING == _sortMethod) {
                java.util.Arrays.sort(entries);

            } else if (ListSortMethod.DESCENDING == _sortMethod) {
                java.util.Arrays.sort(entries, Collections.reverseOrder());
            }
            for (String item : entries) {
                if (null == strData) {
                    strData = item;
                } else {
                    strData += "," + item;
                }
            }
            MarvinTask mt = new MarvinTask();

            mt.AddDataset(__ID, __Namespace, strData);
            TaskManager.getTaskManager().AddDeferredTaskObject(mt);
        }
    }

    private void HandleMakeListIndexList(String strInpValue) {
        if (null == strInpValue) {
            return;
        }
        String strData = null;

        int len = strInpValue.split(__splitToken).length;

        for (int iIndex = 0; iIndex < len; iIndex++) {
            if (null == strData) {
                strData = Integer.toString(iIndex);
            } else {
                strData += __splitToken + Integer.toString(iIndex);
            }
        }
        MarvinTask mt = new MarvinTask();
        mt.AddDataset(__ID, __Namespace, strData);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }

    private void HandleNamespaceList(String strListItem) {
        if (!__mapOfListData.containsKey(strListItem.toUpperCase())) {
            String strNamespace;
            if (__ProcessRanges) {
                strNamespace = BaseWidget.ProcessIndexDataRequest(__dataIndexRange, __dataIndexToken, strListItem);
                if (null == strNamespace) {
                    return;
                }
            } else {
                strNamespace = strListItem;
            }
            __mapOfListData.put(strListItem.toUpperCase(), strNamespace);

            String strData = null;
            String[] entries = __mapOfListData.keySet().toArray(new String[__mapOfListData.size()]);
            if (ListSortMethod.DESCENDING == _sortMethod) {
                java.util.Arrays.sort(entries);

            } else if (ListSortMethod.ASCENDING == _sortMethod) {
                java.util.Arrays.sort(entries, Collections.reverseOrder());
            }
            for (String item : entries) {
                if (null == strData) {
                    strData = item;
                } else {
                    strData += "," + item;
                }
            }
            MarvinTask mt = new MarvinTask();

            mt.AddDataset(__ID, __Namespace, strData);
            TaskManager.getTaskManager().AddDeferredTaskObject(mt);
        }
    }

    private void HandleProxiedValue(String proxyNS, String proxyID, String strValue) {
        MarvinTask mt = new MarvinTask();
        String dataStr;

        try {
            float value;
            value = Float.parseFloat(strValue);
            value *= __Scale;
            int precision = __precision;

            if (-1 == precision) { // use precision of value received
                int integerPlaces = strValue.indexOf('.');
                if (integerPlaces > 0) {
                    precision = strValue.length() - integerPlaces - 1;
                } else {
                    precision = 0;
                }
            }

            DecimalFormat df = new DecimalFormat();
            df.setGroupingUsed(false);
            df.setMaximumFractionDigits(precision);
            df.setMinimumFractionDigits(precision);

            dataStr = df.format(value);
        } catch (NumberFormatException ex) {
            dataStr = strValue;
        }

        // Can use wildcard for target names, only for Proxy at moment
        mt.AddDataset(Utility.combineWildcards(__ID, proxyID), Utility.combineWildcards(__Namespace, proxyNS), dataStr);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
    }

    private void HandleSplitList(String NS, String ID, String strInpValue) {
        String parts[] = strInpValue.split(__splitToken);

        if (parts.length < 2) {
            LOGGER.warning("GenerateDatapoint: SplitList received data that was not a list: " + strInpValue);
            return;
        }

        int index = -1;

        for (String strValue : parts) {
            MarvinTask mt = new MarvinTask();
            String dataStr;
            try {
                float value;
                value = Float.parseFloat(strValue);
                value *= __Scale;
                int precision = __precision;

                if (-1 == precision) { // use precision of value received
                    int integerPlaces = strValue.indexOf('.');
                    if (integerPlaces > 0) {
                        precision = strValue.length() - integerPlaces - 1;
                    } else {
                        precision = 0;
                    }
                }

                DecimalFormat df = new DecimalFormat();
                df.setGroupingUsed(false);
                df.setMaximumFractionDigits(precision);
                df.setMinimumFractionDigits(precision);

                dataStr = df.format(value);

                index++;
            } catch (NumberFormatException ex) {
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

    public boolean Matches(String checkNamespace, String checkID) {
        String namespace = checkNamespace.toUpperCase();
        String id = checkID.toUpperCase();

        // if already checked, no need to do it again
        if (__PreviouslyChecked.containsKey(namespace + id)) {
            return false;
        }
        if (Matches(namespace, id, __includeCriterea) && !Matches(namespace, id, __excludeCriterea)) {

            __PreviouslyChecked.put(namespace + id, true);
            return true;
        } else {
            __PreviouslyChecked.put(namespace + id, false);
        }
        return false;
    }

    private boolean Matches(String checkNamespace, String checkID, List<Pair<String, String>> criterea) {
        // for (Pair<String, String> matchPattern : criterea)
        for (int index = 0; index < criterea.size(); index++) {
            Pair<String, String> matchPattern = criterea.get(index);
            // ID is null when only checking NS
            if ((null == matchPattern.getKey() || Glob.check(matchPattern.getKey(), checkNamespace))
                    && (null == matchPattern.getValue() || Glob.check(matchPattern.getValue(), checkID))) {
                return true;
            }
        }
        return false;
    }

    public void ProxyReset(String newNamespaceFilter, String newIDFilter, String newListEntry) {
        synchronized (__dirtyMap) {
            removeAllListeners();
            __dirtyMap.clear();

            Pair<String, String> current = __includeCriterea.get(0);
            if (null == newNamespaceFilter) {
                newNamespaceFilter = current.getKey();
            }
            if (null == newIDFilter) {
                newIDFilter = current.getValue();
            }
            if (null != newNamespaceFilter && null != newIDFilter) {
                __includeCriterea.clear(); // only 1 criteria for proxy
                __includeCriterea.add(new Pair<String, String>(newNamespaceFilter, newIDFilter));
            }
            if (null != newListEntry) {
                try {
                    __csvEntry = Integer.parseInt(newListEntry);
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Attempt to set proxy ListEntry failed:" + newListEntry);
                }
            }
        }
    }

    private void removeAllListeners() {
        synchronized (this) {

            for (String key : _listenerMap.keySet()) {
                // go through all listeners for this data and nuke it.
                ChangeListener<?> objListener = _listenerMap.get(key);
                // very inefficient - this should be re-worked.
                DataManager.getDataManager().RemoveListener(objListener);
            }
            _listenerMap.clear();
            __PreviouslyChecked.clear();
        }
    }

    private void RemoveOutStaleEntries() {
        Map<String, Pair<String, Boolean>> newDirtyMap = new HashMap<>();

        for (String key : __dirtyMap.keySet()) {
            if (__dirtyMap.get(key).getValue()) // have received an update recently, so keep
            {
                newDirtyMap.put(key, __dirtyMap.get(key));
            } else {
                ChangeListener<?> objListener = _listenerMap.get(key);
                _listenerMap.remove(key);
                LOGGER.info("Removing Stale GenerateDataPoing Input: " + key);
                // very inefficient - this should be re-woredk.
                DataManager.getDataManager().RemoveListener(objListener);
                __PreviouslyChecked.remove(key); // in case it comes back
            }
        }
        __dirtyMap.clear();
        __dirtyMap.putAll(newDirtyMap);
    }

    public boolean setListEntry(int newVal) {
        /*
         * if (newVal < 0) { return false; }
         */
        __csvEntry = newVal;
        __useCSV = true;
        return true;
    }

    public void setMethod(GenerateMethod method) {
        _Method = method;
        if (GenerateMethod.PROXY == _Method) {
            setPrecision(-1);
        }
    }

    public void setMinFrequency(int __minFrequency) {
        this.__minFrequency = __minFrequency;
    }

    public void setPolicy(RefreshPolicy _Policy) {
        this._Policy = _Policy;
    }

    public void setPrecision(int __precision) {
        this.__precision = __precision;
    }

    public void setProxyID(String id) {
        __ProxyID = id;
    }

    public void setScale(double __Scale) {
        this.__Scale = __Scale;
    }

    public void SetSortMethod(ListSortMethod newMethod) {
        _sortMethod = newMethod;
    }

    public void setSplitToken(String newToken) {
        __splitToken = newToken;
    }

    private void ZeroOutStaleEntries() {
        for (String key : __dirtyMap.keySet()) {
            if (!__dirtyMap.get(key).getValue()) {
                Pair<String, Boolean> entry = new Pair<>("0.0", false);
                __dirtyMap.put(key, entry);
            }
        }
    }
}
