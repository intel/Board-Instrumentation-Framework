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

import eu.hansolo.enzo.gauge.Radial;
import eu.hansolo.enzo.gauge.Radial.TickLabelOrientation;
import static java.lang.Math.abs;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public class SteelGaugeRadialWidget extends BaseWidget
{

    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private int DialStartAngle;
    private int DialRangeAngle;
    private double MajorTick;
    private double MinorTick;
    private TickLabelOrientation eOrientation;
    private boolean EnhancedRateText;
    private Radial _Gauge;
    private GridPane _ParentGridPane;
    private double _InitialValue = 0;
    private double MajorTickCount = 0;
    private double MinorTickCount = 0;

    public SteelGaugeRadialWidget()
    {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        DialStartAngle = 330;
        DialRangeAngle = 300;
        MajorTickCount = 0;
        MinorTickCount = 0;

        MajorTick = 0;
        MinorTick = 0;
        eOrientation = TickLabelOrientation.HORIZONTAL;
        EnhancedRateText = true;
        _Gauge = new Radial();
        _Gauge.setAnimationDuration(400);
        //_Gauge.setAnimated(false);

    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        _ParentGridPane = pane;
        if (false == SetupGauge())
        {
            return false;
        }
        _Gauge.setValue(_InitialValue);
        SetupPeekaboo(dataMgr);

        pane.add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
                    {
                        @Override
                        public void changed(ObservableValue o, Object oldVal, Object newVal)
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
                                LOGGER.severe("Invalid data for Gauge received: " + strVal);
                                return;
                            }

                            _Gauge.setValue(newDialValue);
                        }
                    });

        return ApplyCSS();
    }

    private boolean SetupGauge()
    {
        _Gauge.setMinValue(MinValue);
        _Gauge.setMaxValue(MaxValue);
        // For some reason there is a bug in this gauge, so going to IGNORE this field for now
//        _Gauge.setStartAngle(DialStartAngle);
//        _Gauge.setAngleRange(DialRangeAngle);
        _Gauge.setTickLabelOrientation(eOrientation);

        if (getTitle().length() > 0)
        {
            _Gauge.setTitle(getTitle());
        }
        SetupTicksFromTickCount();
        if (MajorTick > 0)
        {
            _Gauge.setMajorTickSpace(MajorTick);
        }
        if (MinorTick > 0)
        {
            _Gauge.setMinorTickSpace(MinorTick);
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
//        if (null != Sections)
//        {
//            _Gauge.setSections(Sections);
//        }

        _Gauge.setDecimals(getDecimalPlaces());
        ConfigureDimentions();
        SetupTaskAction();
        ConfigureAlignment();

//        _Gauge.setMouseTransparent(true);
        //LOGGER.config(DumpDimensions("SteelGauge take 2", _Gauge));
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
            LOGGER.severe("Invalid Default Value data for SteelGaugeRadial  Gauge: " + value);
        }
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return _Gauge;
    }

    public void setUnitText(String UnitText)
    {
        this.UnitText = UnitText;
    }

    public void setDialRangeAngle(int DialRangeAngle)
    {
        this.DialRangeAngle = DialRangeAngle;
    }

    public void setOrientation(TickLabelOrientation eOrientation)
    {
        this.eOrientation = eOrientation;
    }

    public void setMinValue(double MinValue)
    {
        this.MinValue = MinValue;
    }

    public void setMaxValue(double MaxValue)
    {
        this.MaxValue = MaxValue;
    }

    public void setDialStartAngle(int DialStartAngle)
    {
        this.DialStartAngle = DialStartAngle;
    }

    public void setRangeAngle(int DialEndAngle)
    {
        this.DialRangeAngle = DialEndAngle;
    }

    public void setMajorTick(double MajorTick)
    {
        this.MajorTick = MajorTick;
    }

    public void setMinorTick(double MinorTick)
    {
        this.MinorTick = MinorTick;
    }

    public void setEnhancedRateText(boolean EnhancedRateText)
    {
        this.EnhancedRateText = EnhancedRateText;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return _Gauge.getStylesheets();
    }

    protected static String DumpDimensions(String ID, Radial objGauge)
    {
        String prefWidth = "Pref Width: " + objGauge.getPrefWidth() + " ";
        String prefHeight = "Pref Height: " + objGauge.getPrefHeight() + " ";
        String maxWidth = "MAX Width: " + objGauge.getMaxWidth() + " ";
        String maxHeight = "MAX Width: " + objGauge.getMaxWidth() + " ";
        String minWidth = "MIN Width: " + objGauge.getMinWidth() + " ";
        String minHeight = "MIN Height: " + objGauge.getMinHeight() + " ";
        String currWidth = "Curr Width: " + objGauge.getWidth() + " ";
        String currHeight = "Curr Height: " + objGauge.getHeight() + " ";

        String retString = "Dimensions for " + ID + " Widget\n";
        retString += "Current";
        retString += "\t" + currWidth + "\t" + currHeight;
        retString += "\nPreferred";
        retString += "\t" + prefWidth + "\t" + prefHeight;
        retString += "\nMaximum";
        retString += "\t" + maxWidth + "\t" + maxHeight;
        retString += "\nMinimum";
        retString += "\t" + minWidth + "\t" + minHeight;

        return retString;

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
        for (FrameworkNode node : rangeNode.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("TickCount"))
            {
                double MajorTickVal = -1234;
                double MinorTickVal = -1234;

                if (node.hasAttribute("Major"))
                {
                    MajorTickVal = node.getDoubleAttribute("Major", MajorTickVal);
                    if (MajorTickVal != -1234)
                    {
                        setMajorTickCount(MajorTickVal);
                    }
                    else
                    {
                        LOGGER.severe("Invalid TickCount:Major ->" + node.getAttribute("Major"));
                        return false;
                    }
                }
                if (node.hasAttribute("Minor"))
                {
                    MinorTickVal = node.getDoubleAttribute("Minor", MinorTickVal);
                    if (MinorTickVal != -1234)
                    {
                        setMinorTickCount(MinorTickVal);
                    }
                    else
                    {
                        LOGGER.severe("Invalid TickCount:Minor ->" + node.getAttribute("Minor"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle)
    {
        _Gauge.setTitle(getTitle());
    }

    @Override
    public void UpdateValueRange()
    {
        makeNewGauge();
    }

    private void makeNewGauge()
    {
        Radial oldGauge = _Gauge;
        _Gauge = new Radial();
        _Gauge.setVisible(oldGauge.isVisible());

        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        if (false == SetupGauge())
        {
            LOGGER.severe("Tried to re-create Radial for Stepped Range, but something bad happened.");
            _Gauge = oldGauge;
            return;
        }
        pane.add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        ApplyCSS();
    }

    public double getMajorTickCount()
    {
        return MajorTickCount;
    }

    public void setMajorTickCount(double MajorTickCount)
    {
        this.MajorTickCount = MajorTickCount;
    }

    public double getMinorTickCount()
    {
        return MinorTickCount;
    }

    public void setMinorTickCount(double MinorTickCount)
    {
        this.MinorTickCount = MinorTickCount;
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

    @Override
    public boolean SupportsSteppedRanges()
    {
        return true;
    }

    private void SetupTicksFromTickCount()
    {
        double range = abs(this.MaxValue - this.MinValue);
        if (range == 0)
        {
            return;
        }
        if (MajorTickCount > 0)
        {
            MajorTick = range / MajorTickCount;
            if (MinorTickCount > 0)
            {
                MinorTick = MajorTick / MinorTickCount;
            }
        }
    }

}
