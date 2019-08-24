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

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.SimpleGauge;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public class SteelSimpleGaugeWidget extends BaseWidget
{
    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private List<Section> Sections;
    private List<Pair<Double, Double>> SectionPercentages = null;

    private SimpleGauge _Gauge;
    private double _InitialValue = 0;
    
    public SteelSimpleGaugeWidget()
    {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        Sections = null;
        _Gauge = new SimpleGauge();
        _Gauge.setAnimationDuration(400);
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
        
        SetupPeekaboo(dataMgr);
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
                    HandleSteppedRange(newDialValue);
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid data for Simple Gauge received: " + strVal);
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

    protected void HandleSteppedRange(double newValue)
    {
        if (SupportsSteppedRanges())
        {
            if (getExceededMaxSteppedRange(newValue))
            {
                MaxValue = getNextMaxSteppedRange(newValue);
                UpdateValueRange();
            }
            else if (getExceededMinSteppedRange(newValue))
            {
                MinValue = getNextMinSteppedRange(newValue);
                UpdateValueRange();
            }
        }
    }
    
    /**
     * Sets range for widget - not valid for all widgets
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

    private void makeNewGauge()
    {
        SimpleGauge oldGauge = _Gauge;
        _Gauge = new SimpleGauge();
        _Gauge.setVisible(oldGauge.isVisible());

        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        if (false == SetupGauge())
        {
            LOGGER.severe("Tried to re-create SteelGaugeWidget for Stepped Range, but something bad happened.");
            _Gauge = oldGauge;
            return;
        }
        pane.add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        ApplyCSS();
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
            LOGGER.severe("Invalid Default Value data for 180  Gauge: " + value);
        }
    }

    public void setMaxValue(double MaxValue)
    {
        this.MaxValue = MaxValue;
    }

    public void setMinValue(double MinValue)
    {
        this.MinValue = MinValue;
    }

    public void setPercentageSections(List<Pair<Double, Double>> Sections)
    {
        this.SectionPercentages = Sections;
    }
    public void setSections(List<Section> objSections)
    {
        this.Sections = objSections;
    }
    private void setSectionsFromPercentages()
    {
        if (null == SectionPercentages)
        {
            return;
        }
        List<Section> sections = new ArrayList<>();
        double range = abs(MaxValue - MinValue);
        for (Pair<Double, Double> sect : SectionPercentages)
        {
            double start, end;
            start = MinValue + sect.getKey() / 100 * range;
            end = MinValue + sect.getValue() / 100 * range;
            sections.add(new Section(start, end));
        }
        setSections(sections);
    }
    public void setUnitText(String UnitText)
    {
        this.UnitText = UnitText;
    }
    private boolean SetupGauge()
    {
        _Gauge.setMinValue(MinValue);
        _Gauge.setMaxValue(MaxValue);
        if (null != getUnitsOverride())
        {
            _Gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        }
        else if (UnitText.length() > 0)
        {
            _Gauge.setUnit(UnitText);
        }
        _Gauge.setAnimationDuration(800); // for some reason default is 3000, and that is too long, causes issues.

        if (getTitle().length() > 0)
        {
            _Gauge.setTitle(getTitle());
        }
        if (getHeight() > 0)
        {
            _Gauge.setPrefHeight(getHeight());
            _Gauge.setMaxHeight(getHeight());
        }
        if (getWidth() > 0)
        {
            _Gauge.setPrefHeight(getWidth());
            _Gauge.setMaxWidth(getWidth());
        }
        setSectionsFromPercentages();
        if (null != Sections)
        {
            _Gauge.setSections(Sections);
        }
        _Gauge.setDecimals(getDecimalPlaces());
        ConfigureDimentions();
        ConfigureAlignment();
        SetupTaskAction();

        return false != ApplyCSS();
    }

    @Override
    public boolean SupportsSteppedRanges()
    {
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle)
    {
        _Gauge.setTitle(strTitle);
    }
    @Override
    public void UpdateValueRange()
    {
        makeNewGauge();
    }  
}
