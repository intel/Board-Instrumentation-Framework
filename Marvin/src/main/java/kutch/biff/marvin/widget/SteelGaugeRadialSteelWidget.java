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

import eu.hansolo.enzo.gauge.RadialGauge;
import eu.hansolo.enzo.gauge.RadialGauge.TickLabelOrientation;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class SteelGaugeRadialSteelWidget extends BaseWidget {
    private String UnitText;
    private double MinValue;
    private double MaxValue;
    private int DialStartAngle;
    private int DialRangeAngle;
    private double MajorTick;
    private double MinorTick;
    private double MajorTickCount, MinorTickCount;
    private RadialGauge.TickLabelOrientation eOrientation;
    @SuppressWarnings("unused")
    private boolean EnhancedRateText;
    private boolean Shadowed;
    private RadialGauge _Gauge;
    private double _InitialValue;

    public SteelGaugeRadialSteelWidget() {
        UnitText = "";
        MinValue = 0;
        MaxValue = 0;
        DialStartAngle = 270;
        DialRangeAngle = 270;
        MajorTick = 0;
        MinorTick = 0;
        MajorTickCount = 0;
        MinorTickCount = 0;
        eOrientation = RadialGauge.TickLabelOrientation.HORIZONTAL;
        EnhancedRateText = true;
        Shadowed = true;
        _Gauge = new RadialGauge();
//        _Gauge.setAnimationDuration(400);
        _Gauge.setPrefWidth(200);
        _Gauge.setPrefHeight(200);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        setDefaultIsSquare(true);
        SetParent(pane);
        if (false == SetupGauge()) {
            return false;
        }

        _Gauge.setValue(_InitialValue);
        initialSteppedRangeSetup(MinValue, MaxValue);
        getParentPane().add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        SetupPeekaboo(DataManager.getDataManager());
        dataMgr.AddListener(getMinionID(), getNamespace(), (o, oldVal, newVal) -> {
            if (IsPaused()) {
                return;
            }

            double newDialValue = 0;
            String strVal = newVal.toString();
            try {
                newDialValue = Double.parseDouble(strVal);
                HandleSteppedRange(newDialValue);

                // oldDialValue = Double.parseDouble(oldVal.toString());
            } catch (Exception ex) {
                LOGGER.severe("Invalid data for Gauge received: " + strVal);
                return;
            }

            _Gauge.setValue(newDialValue);
        });

        return ApplyCSS();
    }

    public double getMajorTickCount() {
        return MajorTickCount;
    }

    public double getMinorTickCount() {
        return MinorTickCount;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return _Gauge;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _Gauge.getStylesheets();
    }

    protected void HandleSteppedRange(double newValue) {
        if (SupportsSteppedRanges()) {
            if (getExceededMaxSteppedRange(newValue)) {
                MaxValue = getNextMaxSteppedRange(newValue);
                UpdateValueRange();
            } else if (getExceededMinSteppedRange(newValue)) {
                MinValue = getNextMinSteppedRange(newValue);
                UpdateValueRange();
            }
        }
    }

    /**
     * Sets range for widget - not valid for all widgets
     *
     * @param rangeNode
     * @return
     */
    @Override
    public boolean HandleValueRange(FrameworkNode rangeNode) {
        double Min = -1234.5678;
        double Max = -1234.5678;
        if (rangeNode.hasAttribute("Min")) {
            Min = rangeNode.getDoubleAttribute("Min", Min);
            if (Min == -1234.5678) {
                return false;
            }
            this.MinValue = Min;
        }
        if (rangeNode.hasAttribute("Max")) {
            Max = rangeNode.getDoubleAttribute("Max", Max);
            if (Max == -1234.5678) {
                return false;
            }
            this.MaxValue = Max;
        }
        for (FrameworkNode node : rangeNode.getChildNodes()) {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment")) {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("TickCount")) {
                double MajorTickVal = -1234;
                double MinorTickVal = -1234;

                if (node.hasAttribute("Major")) {
                    MajorTickVal = node.getDoubleAttribute("Major", MajorTickVal);
                    if (MajorTickVal != -1234) {
                        setMajorTick(MajorTickVal);
                    } else {
                        LOGGER.severe("Invalid TickCount:Major ->" + node.getAttribute("Major"));
                        return false;
                    }
                }
                if (node.hasAttribute("Minor")) {
                    MinorTickVal = node.getDoubleAttribute("Minor", MinorTickVal);
                    if (MinorTickVal != -1234) {
                        setMinorTick(MinorTickVal);
                    } else {
                        LOGGER.severe("Invalid TickCount:Minor ->" + node.getAttribute("Minor"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void makeNewGauge() {
        RadialGauge oldGauge = _Gauge;
        _Gauge = new RadialGauge();
        _Gauge.setVisible(oldGauge.isVisible());

        if (false == SetupGauge()) {
            LOGGER.severe("Tried to re-create SteelGaugeWidget for Stepped Range, but something bad happened.");
            _Gauge = oldGauge;
            return;
        }
        GridPane pane = getParentPane();
        pane.getChildren().remove(oldGauge);

        pane.add(_Gauge, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        ApplyCSS();
    }

    public void setDialRangeAngle(int DialRangeAngle) {
        this.DialRangeAngle = DialRangeAngle;
    }

    public void setDialStartAngle(int DialStartAngle) {
        this.DialStartAngle = DialStartAngle;
    }

    public void setEnhancedRateText(boolean EnhancedRateText) {
        this.EnhancedRateText = EnhancedRateText;
    }

    @Override
    public void SetInitialValue(String value) {
        try {
            _InitialValue = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Default Value data for SteelGaugeRadialSteel  Gauge: " + value);
        }
    }

    public void setMajorTick(double MajorTick) {
        this.MajorTick = MajorTick;
    }

    public void setMajorTickCount(double MajorTickCount) {
        this.MajorTickCount = MajorTickCount;
    }

    public void setMaxValue(double MaxValue) {
        this.MaxValue = MaxValue;
    }

    public void setMinorTick(double MinorTick) {
        this.MinorTick = MinorTick;
    }

    public void setMinorTickCount(double MinorTickCount) {
        this.MinorTickCount = MinorTickCount;
    }

    public void setMinValue(double MinValue) {
        this.MinValue = MinValue;
    }

    public void setOrientation(TickLabelOrientation eOrientation) {
        this.eOrientation = eOrientation;
    }

    public void setRangeAngle(int DialEndAngle) {
        this.DialRangeAngle = DialEndAngle;
    }

    public void setShadowed(boolean Shadowed) {
        this.Shadowed = Shadowed;
    }

    public void setUnitText(String UnitText) {
        this.UnitText = UnitText;
    }

    private boolean SetupGauge() {
        _Gauge.setMinValue(MinValue);
        _Gauge.setMaxValue(MaxValue);
        _Gauge.setStartAngle(DialStartAngle);
        _Gauge.setAngleRange(DialRangeAngle);
        // _Gauge.setAngleRange(180);
        _Gauge.setTickLabelOrientation(eOrientation);
        _Gauge.setDropShadowEnabled(Shadowed);

        if (getTitle().length() > 0) {
            _Gauge.setTitle(getTitle());
        }
        SetupTicksFromTickCount();

        if (MajorTick > 0) {
            _Gauge.setMajorTickSpace(MajorTick);
        }
        if (MinorTick > 0) {
            _Gauge.setMinorTickSpace(MinorTick);
        }
        if (null != getUnitsOverride()) {
            _Gauge.setUnit(getUnitsOverride());
            LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
        } else if (UnitText.length() > 0) {
            _Gauge.setUnit(UnitText);
        }

        _Gauge.setDecimals(getDecimalPlaces());

        ConfigureDimentions();

        ConfigureAlignment();
        SetupTaskAction();

        // LOGGER.config(DumpDimensions("SteelGauge take 2", _Gauge));
        return true;
    }

    private void SetupTicksFromTickCount() {
        double range = abs(this.MaxValue - this.MinValue);
        if (range == 0) {
            return;
        }
        if (MajorTickCount > 0) {
            MajorTick = range / MajorTickCount;
            if (MinorTickCount > 0) {
                MinorTick = MajorTick / MinorTickCount;
            }
        }
    }

    @Override
    public boolean SupportsSteppedRanges() {
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        _Gauge.setTitle(getTitle());
    }

    @Override
    public void UpdateValueRange() {
        _Gauge.setMinValue(MinValue);
        _Gauge.setMaxValue(MaxValue);
        makeNewGauge();
    }

}
