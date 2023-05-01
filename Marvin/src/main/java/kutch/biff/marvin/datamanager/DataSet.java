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

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class DataSet {
    SimpleObjectProperty<DataObject> _ObjectProperty;
    ArrayList<DataObject> _DataUpdates;
    String _LatestValue;
    String _LatestValueForMath;

    public DataSet() {
        _ObjectProperty = new SimpleObjectProperty<>();
        _DataUpdates = new ArrayList<>(); // is an arraylist because could receive back 2 back updates, before 1st
        // update handled, 2nd one overwrites
        _LatestValue = "";
        _LatestValueForMath = "";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addListener(ChangeListener listener) {
        if (null != listener && null != _ObjectProperty) {
            _ObjectProperty.addListener(listener);
        }
    }

    public String getLatestValue() {
        synchronized (this) {
            return _LatestValue;
        }
    }

    public String getLatestValueForMath() {
        synchronized (this) {
            return _LatestValueForMath;
        }
    }

    public int getSize() {
        return _DataUpdates.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void removeListener(ChangeListener listener) {
        if (null != listener && null != _ObjectProperty) {
            _ObjectProperty.removeListener(listener);
        }
    }

    public void setLatestValue(String newValue) {
        DataObject newObj = new DataObject(newValue);
        synchronized (this) {
            _DataUpdates.add(newObj);
            _LatestValueForMath = newValue;
        }
    }

    public int Update() {
        int retVal;
        synchronized (this) {
            retVal = _DataUpdates.size();
            for (DataObject newValue : _DataUpdates) {
                _ObjectProperty.setValue(newValue);
                _LatestValue = newValue.getData();
            }
            _DataUpdates.clear();
        }
        return retVal;
    }
}
