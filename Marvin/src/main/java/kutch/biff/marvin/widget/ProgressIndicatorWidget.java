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
package kutch.biff.marvin.widget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;

/**
 * @author Patrick Kutch
 */
public class ProgressIndicatorWidget extends BaseWidget {

    private javafx.scene.control.ProgressIndicator _Indicator;

    public ProgressIndicatorWidget() {
        _Indicator = new javafx.scene.control.ProgressIndicator();
        _Indicator.setProgress(0);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        pane.add(_Indicator, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> o, Object oldVal, Object newVal) {
                if (IsPaused()) {
                    return;
                }
                double newDialValue = 0;
                String strVal = newVal.toString();
                try {
                    newDialValue = Double.parseDouble(strVal);
                } catch (Exception ex) {
                    LOGGER.severe("Invalid data for Progress Indicator received: " + strVal);
                    return;
                }

                if (0.0 == newDialValue) {
                    _Indicator.setProgress(newDialValue);
                } else {
                    _Indicator.setProgress(newDialValue / 100);
                }
            }
        });
        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return _Indicator;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _Indicator.getStylesheets();
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a Progress Indicator to " + strTitle);
    }

}
