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

package kutch.biff.marvin.datamanager;

import java.time.temporal.ValueRange;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.widget.BaseWidget;

public abstract class MarvinChangeListener implements ChangeListener<Object> {
    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private ValueRange __valueRange;
    private boolean __ProcessRanges;
    private String __tokenChar;

    public MarvinChangeListener(ValueRange objRange, String tokenChar) {
        __valueRange = objRange;
        __tokenChar = tokenChar;
        if (__valueRange.getMinimum() > -1) {
            __ProcessRanges = true;
        } else {
            __ProcessRanges = false;
        }
    }

    @Override
    public void changed(ObservableValue<?> arg0, Object arg1, Object arg2) {
        if (!__ProcessRanges) {
            onChanged(arg2.toString());
        } else {
            String retStr = BaseWidget.ProcessIndexDataRequest(__valueRange, __tokenChar, arg2.toString());
            if (null != retStr) {
                onChanged(retStr);
            } else {
                // send it anyway?
                // onChanged(arg2.toString());
            }
        }
    }

    public abstract void onChanged(String newValue);
}
