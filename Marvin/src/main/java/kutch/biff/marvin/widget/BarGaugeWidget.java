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

import java.util.logging.Logger;

import eu.hansolo.enzo.gauge.FlatGauge;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public class BarGaugeWidget extends BaseWidget
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private double MinValue;
    private double MaxValue;
    private FlatGauge _Gauge;
    private double _InitialValue;    
    private String UnitText;

    public BarGaugeWidget()
    {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        _Gauge = new FlatGauge();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        if (false == SetupGauge())
        {
            return false;
        }
        _Gauge.setValue(_InitialValue);

        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        ConfigureDimentions();

        pane.add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
            {
                if (IsPaused())
                {
                    return;
                }

                double newDialValue = 0;
                String strVal = newVal.toString();
                try
                {
                    newDialValue = Double.parseDouble(strVal);
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.info("Ignoring invalid data for BarGauge received: " + strVal);
                    return;
                }
                _Gauge.setValue(newDialValue);
            }
        });


        return true;
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return _Gauge;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return _Gauge.getStylesheets();
    }

    /**
     * Sets range for widget - not valid for all widgets
     *
     * @param rangeNode
     * @return
     */

    @Override
    public boolean HandleValueRange(FrameworkNode rangeNode)
    {
        double Min = -1234.5678;
        double Max = -1234.5678;
        if (rangeNode.hasAttribute("Min"))
        {
            Min = rangeNode.getDoubleAttribute("Min", Min);
            if (Min == -1234.5678)
            {
                return false;
            }
            this.MinValue = Min;
        }
        if (rangeNode.hasAttribute("Max"))
        {
            Max = rangeNode.getDoubleAttribute("Max", Max);
            if (Max == -1234.5678)
            {
                return false;
            }
            this.MaxValue = Max;
        }
        return true;
    }

    @Override
    public void SetInitialValue(String value)
    {
        try
        {
            _InitialValue = Double.parseDouble(value);
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid Default Value data for BarGauge: " + value);
        }
    }

    public void setMaxValue(double MaxValue)
    {
        this.MaxValue = MaxValue;
    }

    public void setMinValue(double MinValue)
    {
        _InitialValue = MinValue;
        this.MinValue = MinValue;
    }

    public void setUnitText(String UnitText)
    {
        this.UnitText = UnitText;
    }

    private boolean SetupGauge()
    {
        _Gauge.setMinValue(MinValue);
        _Gauge.setMaxValue(MaxValue);

        if (getTitle().length() > 0)
        {
            _Gauge.setTitle(getTitle());
        }
        if (null != getUnitsOverride())
        {
            _Gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        }
        else if (UnitText.length() > 0)
        {
            _Gauge.setUnit(UnitText);
        }

        _Gauge.setDecimals(getDecimalPlaces());

        SetupTaskAction();

        return false != ApplyCSS();
    }
    @Override
    public void UpdateTitle(String strTitle)
    {
        _Gauge.setTitle(getTitle());
    }
}
