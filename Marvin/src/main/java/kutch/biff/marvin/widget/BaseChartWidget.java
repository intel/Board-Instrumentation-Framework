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
import java.util.HashMap;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.SeriesDataSet;
import kutch.biff.marvin.utility.SeriesSet;
import kutch.biff.marvin.utility.Utility;

/**
 * @author Patrick Kutch
 */
abstract public class BaseChartWidget extends BaseWidget {

    private Chart _chart;
    // private AreaChart<Number,Number> _chart;
    private final ArrayList<SeriesDataSet> _Series;
    @SuppressWarnings("rawtypes")
    protected Axis _xAxis;
    @SuppressWarnings("rawtypes")
    protected Axis _yAxis;
    protected String xAxisLabel, yAxisLabel;
    protected double xAxisMaxCount, yAxisMaxCount;
    protected double yAxisMaxValue, yAxisMinValue, yAxisMaxValue_Initial, yAxisMinValue_Initial;
    private boolean _Animated;
    protected int xAxisMinorTick, yAxisMinorTick;
    protected double xAxisMajorTick, yAxisMajorTick;
    protected boolean xAxisTickVisible, yAxisTickVisible;
    protected HashMap<SeriesDataSet, String> SyncronizeDataSetsMap;
    protected boolean _SynchronizeMulitSourceData;
    protected int MaxSynchronizeMulitSoureDataWait;
    protected long MaxSynchronizeMultiSourceLastUpdate;
    protected HashMap<String, SeriesSet> _SeriesMap;
    protected ArrayList<String> _SeriesOrder;
    protected boolean _HorizontalChart;
    protected double yAxisMajorTickCount, xAxisMajorTickCount;
    protected double yAxisMinorTickCount, xAxisMinorTickCount;
    protected boolean _isStackedChart;

    public BaseChartWidget() {
        xAxisLabel = "";
        yAxisLabel = "";
        xAxisMaxCount = 20;
        yAxisMaxCount = 0;
        yAxisMaxValue = 100;
        yAxisMinValue = 0;
        _Animated = false;
        yAxisMajorTick = 0;
        yAxisMinorTick = 0;
        xAxisMajorTick = 0;
        xAxisMinorTick = 0;
        yAxisMajorTickCount = 0;
        xAxisMajorTickCount = 0;
        yAxisMinorTickCount = 0;
        xAxisMinorTickCount = 0;
        xAxisTickVisible = true;
        yAxisTickVisible = true;

        _Series = new ArrayList<>();
        _chart = null;
        setDefaultIsSquare(false);
        SyncronizeDataSetsMap = null;
        _SynchronizeMulitSourceData = true;
        MaxSynchronizeMulitSoureDataWait = 0;
        MaxSynchronizeMultiSourceLastUpdate = 0;
        _SeriesMap = new HashMap<>();
        _SeriesOrder = new ArrayList<>();
        _HorizontalChart = false;
        _isStackedChart = false;
    }

    @SuppressWarnings("unchecked")
    protected void AddNewData(SeriesDataSet ds, String strNewValue) {
        double newValue = 0;
        try {
            newValue = Double.parseDouble(strNewValue);
        } catch (Exception ex) {
            LOGGER.severe("Invalid data for Chart received: " + strNewValue);
            return;
        }

        ShiftSeries(ds.getSeries(), getxAxisMaxCount());
        ds.getSeries().getData().add(new XYChart.Data<>(ds.getSeries().getData().size(), newValue));
    }

    protected boolean AllDataSetsArrived() {
        for (SeriesDataSet ds : getSeries()) // create an entry for all
        {
            if (SyncronizeDataSetsMap.get(ds) == null) {
                return false;
            }
        }
        return true;
    }

    protected void ClearSynchronizationForMultiSource() {
        for (SeriesDataSet ds : getSeries()) // create an entry for all
        {
            SyncronizeDataSetsMap.put(ds, null);
        }
    }

    protected void ConfigureSynchronizationForMultiSource() {
        if (true == _SynchronizeMulitSourceData) {
            SyncronizeDataSetsMap = new HashMap<>();
            ClearSynchronizationForMultiSource();
        }
    }

    protected void CreateAxisObjects() {
        if (xAxisMajorTickCount > 0) {
            _xAxis = new NumberAxis(0d, xAxisMaxCount - 1, xAxisMaxCount / xAxisMajorTickCount);
        } else {
            _xAxis = new NumberAxis(0d, xAxisMaxCount - 1, xAxisMajorTick);
        }

        if (yAxisMajorTickCount > 0) {
            _yAxis = new NumberAxis(yAxisMinValue, yAxisMaxValue, yAxisMaxValue / yAxisMajorTickCount);
        } else {
            _yAxis = new NumberAxis(yAxisMinValue, yAxisMaxValue, yAxisMajorTick);
        }
        setupMinorTicks();
    }

    protected void CreateChart() {
        if (-1 != getCurrentMaxSteppedRange()) {
            yAxisMaxValue = getCurrentMaxSteppedRange();
        }
        if (-1 != getCurrentMinSteppedRange()) {
            yAxisMinValue = getCurrentMinSteppedRange();
        }
        CreateAxisObjects();
        // _xAxis = new NumberAxis(0d,xAxisMaxCount-1,xAxisMajorTick);
        // _yAxis = new NumberAxis(yAxisMinValue,yAxisMaxValue,yAxisMajorTick);

        _xAxis.setTickLabelsVisible(xAxisTickVisible);
        _yAxis.setTickLabelsVisible(yAxisTickVisible);

        _chart = CreateChartObject();

        if (getTitle().length() > 0) {
            _chart.setTitle(getTitle());
        }

        _xAxis.setLabel(xAxisLabel);
        _yAxis.setLabel(yAxisLabel);
        _chart.setAnimated(_Animated);
        yAxisMaxValue_Initial = yAxisMaxValue;
        yAxisMinValue_Initial = yAxisMinValue;
        initialSteppedRangeSetup(yAxisMinValue, yAxisMaxValue);
    }

    abstract Chart CreateChartObject();

    protected Chart getChart() {
        if (null == _chart) {
            LOGGER.severe("Accessing chart object before created");
        }
        return _chart;
    }

    public ArrayList<SeriesDataSet> getSeries() {
        return _Series;
    }

    @SuppressWarnings("rawtypes")
    protected Axis getxAxis() {
        return _xAxis;
    }

    public String getxAxisLabel() {
        return xAxisLabel;
    }

    public double getxAxisMajorTick() {
        return xAxisMajorTick;
    }

    public int getxAxisMaxCount() {
        return (int) xAxisMaxCount;
    }

    public int getxAxisMinorTick() {
        return xAxisMinorTick;
    }

    @SuppressWarnings("rawtypes")
    protected Axis getyAxis() {
        return _yAxis;
    }

    public String getyAxisLabel() {
        return yAxisLabel;
    }

    public double getyAxisMajorTick() {
        return yAxisMajorTick;
    }

    public int getyAxisMaxCount() {
        return (int) yAxisMaxCount;
    }

    public double getyAxisMaxValue() {
        return yAxisMaxValue;
    }

    public int getyAxisMinorTick() {
        return yAxisMinorTick;
    }

    public boolean HandleChartSpecificAppSettings(FrameworkNode node) {
        if (node.getNodeName().equalsIgnoreCase("xAxis")) {
            if (node.hasAttribute("Label")) {
                setxAxisLabel(node.getAttribute("Label"));
            }
            if (node.hasAttribute("MaxEntries")) {
                String strVal = node.getAttribute("MaxEntries");
                try {
                    setxAxisMaxCount(Integer.parseInt(strVal));
                } catch (NumberFormatException ex) {
                    try {
                        setxAxisMaxCount((int) Double.parseDouble(strVal));
                    } catch (NumberFormatException ex1) {
                        LOGGER.severe("Invalid value for chart MaxEntires: " + strVal + " - ignoring.");
                        return false;
                    }
                }

            }
            // For a bar chart, isn't max entries, is count. Is same thing, but gramatically
            // Count is better
            if (node.hasAttribute("Count")) {
                String strVal = node.getAttribute("Count");
                try {
                    setxAxisMaxCount(Integer.parseInt(strVal));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid value for chart Count: " + strVal + " - ignoring.");
                    return false;
                }
            }
            return true;
        }
        if (node.getNodeName().equalsIgnoreCase("Series")) {
            String Label;
            String ID;
            if (node.hasAttribute("Label")) {
                Label = node.getAttribute("Label");
            } else {
                Label = "";
            }
            if (node.hasAttribute("ID")) {
                ID = node.getAttribute("ID");
            } else {
                // LOGGER.warning("Series declaration for Chart Widget requires an ID");
                return false;
            }
            if (_SeriesMap.containsKey(ID.toUpperCase())) {
                LOGGER.severe("Seried ID must be unique per Bar Chart, repeat found: " + ID);
                return false;
            }
            _SeriesMap.put(ID.toUpperCase(), new SeriesSet(Label));
            _SeriesOrder.add(ID.toUpperCase());

            return true;
        } else if (node.getNodeName().equalsIgnoreCase("SeriesSet")) {
            return ReadSeriesSet(node);
        }

        if (node.getNodeName().equalsIgnoreCase("yAxis")) {
            if (node.hasAttribute("Label")) {
                yAxisLabel = node.getAttribute("Label");
            }
            if (node.hasAttribute("MaxValue")) {
                String strVal = node.getAttribute("MaxValue");
                try {
                    yAxisMaxValue = Double.parseDouble(strVal);
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid value for chart MaxValue: " + strVal + " - ignoring.");
                    return false;
                }
            }
            if (node.hasAttribute("MinValue")) {
                String strVal = node.getAttribute("MinValue");
                try {
                    yAxisMinValue = Double.parseDouble(strVal);
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid value for chart MinValue: " + strVal + " - ignoring.");
                    return false;
                }
            }
            if (node.hasAttribute("Count")) {
                String strVal = node.getAttribute("Count");
                try {
                    setyAxisMaxCount(Integer.parseInt(strVal));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid value for chart Count: " + strVal + " - ignoring.");
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    protected void HandleSteppedRange(double newValue) {
        if (SupportsSteppedRanges()) {
            if (getExceededMaxSteppedRange(newValue)) {
                double newMax = getNextMaxSteppedRange(newValue);
                setyAxisMaxValue(newMax);
                UpdateValueRange();
            } else if (getExceededMinSteppedRange(newValue)) {
                double newMin = getNextMinSteppedRange(newValue);
                yAxisMinValue = newMin;
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
            this.yAxisMinValue = Min;
        }
        if (rangeNode.hasAttribute("Max")) {
            Max = rangeNode.getDoubleAttribute("Max", Max);
            if (Max == -1234.5678) {
                return false;
            }
            this.yAxisMaxValue = Max;
        }
        return true;
    }

    public boolean isAnimated() {
        return _Animated;
    }

    public boolean isHorizontal() {
        return _HorizontalChart;
    }

    public boolean isxAxisTickVisible() {
        return xAxisTickVisible;
    }

    public boolean isyAxisTickVisible() {
        return yAxisTickVisible;
    }

    protected void OnDataArrived(SeriesDataSet ds, String strNewValue) {
        if (null == SyncronizeDataSetsMap) {
            AddNewData(ds, strNewValue);
            return;
        }
        SyncronizeDataSetsMap.put(ds, strNewValue);
        if (0 == MaxSynchronizeMultiSourceLastUpdate) {
            MaxSynchronizeMultiSourceLastUpdate = System.currentTimeMillis();
        } else if (AllDataSetsArrived() || (MaxSynchronizeMulitSoureDataWait > 0
                && System.currentTimeMillis() - MaxSynchronizeMultiSourceLastUpdate > MaxSynchronizeMulitSoureDataWait)) {
            PushSynchronizedData();
            MaxSynchronizeMultiSourceLastUpdate = System.currentTimeMillis();
        }
    }

    protected void PushSynchronizedData() {
        String strVal;
        double tVal = 0.0;
        for (SeriesDataSet ds : getSeries()) // create an entry for all
        {
            strVal = SyncronizeDataSetsMap.put(ds, null);
            if (null != strVal) {
                AddNewData(ds, strVal);
                if (_isStackedChart = true) { // for stacked charts, stepped range must use the aggregate value, not an
                    // individual
                    tVal += Double.parseDouble(strVal);
                }
            }
        }
        if (_isStackedChart = true) {
            HandleSteppedRange(tVal);
        }
    }

    protected boolean ReadSeriesSet(FrameworkNode setNode) {
        String title = "";
        if (setNode.hasAttributes()) {
            Utility.ValidateAttributes(new String[]{"Title"}, setNode);
            if (setNode.hasAttribute("Title")) {
                title = setNode.getAttribute("Title");
            }
        }
        for (FrameworkNode node : setNode.getChildNodes()) {
            if (node.getNodeName().equalsIgnoreCase("MinionSrc")) {
                Utility.ValidateAttributes(new String[]{"ID", "Namespace", "SeriesID", "Scale"}, node);

                String ID, Namespace, SeriesID;
                if (node.hasAttribute("ID")) {
                    ID = node.getAttribute("ID");
                } else {
                    LOGGER.severe("Chart SeriesSet defined with invalid MinionSrc - no ID");
                    return false;
                }
                if (node.hasAttribute("Namespace")) {
                    Namespace = node.getAttribute("Namespace");
                } else {
                    LOGGER.severe("Chart SeriesSet defined with invalid MinionSrc - no Namespace");
                    return false;
                }
                if (node.hasAttribute("SeriesID")) {
                    SeriesID = node.getAttribute("SeriesID");
                } else {
                    LOGGER.severe("Chart SeriesSet defined with invalid MinionSrc - no SeriesID");
                    return false;
                }
                if (false == _SeriesMap.containsKey(SeriesID.toUpperCase())) {
                    LOGGER.severe(
                            "Chart SeriesSet defined with invalid MinionSrc - the Series ID has not been defined in a <Series> section. SeriesID="
                                    + SeriesID);
                    return false;
                }
                SeriesDataSet objDS = new SeriesDataSet(title, ID, Namespace);

                if (node.hasAttribute("Scale")) {
                    double scaleVal = node.getDoubleAttribute("Scale", 0);
                    if (scaleVal <= 0) {
                        LOGGER.severe("Chart SeriesSet defined with invalid scale value" + node.getAttribute("Scale"));
                        return false;
                    }
                    objDS.setScaleValue(scaleVal);
                }

                _SeriesMap.get(SeriesID.toUpperCase()).AddSeries(objDS);
            }
        }

        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void resetState(String param) {
        @SuppressWarnings("unused")
        int val = 0;
        if (null != param) {
            try {
                val = Integer.parseInt(param);
            } catch (NumberFormatException ex) {
            }
        }
        yAxisMaxValue = yAxisMaxValue_Initial;
        yAxisMinValue = yAxisMinValue_Initial;
        resetSteppedRange();
        setyAxisMaxValue(yAxisMaxValue_Initial);
        // setyAxisMinValue(yAxisMinValue_Initial);
        initialSteppedRangeSetup(yAxisMinValue, yAxisMaxValue);
        UpdateValueRange();

        // now go and set all to zero or the specified value
        @SuppressWarnings("unchecked")
        ObservableList<XYChart.Series<String, Number>> dList = ((XYChart) getChart()).getData();

        for (XYChart.Series<String, Number> objSeriesEntry : dList) {
            objSeriesEntry.getData().clear();
        }

    }

    public void setAnimated(boolean _Animated) {
        this._Animated = _Animated;
    }

    public void SetSynchronizeInformation(boolean flag, int timeout) {
        _SynchronizeMulitSourceData = flag;
        MaxSynchronizeMulitSoureDataWait = timeout;
    }

    protected void setupMinorTicks() {
        ((NumberAxis) (_yAxis)).setMinorTickCount((int) yAxisMinorTick + 1);
        ((NumberAxis) (_xAxis)).setMinorTickCount((int) xAxisMinorTick + 1);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void SetupSeriesSets(DataManager dataMgr) {
        for (String key : _SeriesOrder) {
            SeriesSet objSeriesSet = _SeriesMap.get(key);
            if (null == objSeriesSet) {
                return;
            }
            XYChart.Series objSeries = new XYChart.Series();
            String strTitle = objSeriesSet.getTitle();
            if (null == strTitle) {
                return;
            }
            objSeries.setName(strTitle);

            for (SeriesDataSet objDs : objSeriesSet.getSeriesList()) {
                XYChart.Data objChartDataSet;
                if (isHorizontal()) {
                    objChartDataSet = new XYChart.Data<>(0, objDs.getTitle());
                } else {
                    objChartDataSet = new XYChart.Data<>(objDs.getTitle(), 0);
                }

                objSeries.getData().add(objChartDataSet);

                dataMgr.AddListener(objDs.getID(), objDs.getNamespace(), new ChangeListener<Object>() {
                    @Override
                    public void changed(ObservableValue<?> o, Object oldVal, Object newVal) {
                        if (IsPaused()) {
                            return;
                        }
                        String strVal = newVal.toString();
                        double newValue;
                        try {
                            newValue = Double.parseDouble(strVal);
                            newValue *= objDs.getScaleValue();
                            HandleSteppedRange(newValue);
                        } catch (NumberFormatException ex) {
                            LOGGER.severe("Invalid data for Line Chart received: " + strVal);
                            return;
                        }
                        if (isHorizontal()) {
                            objChartDataSet.XValueProperty().set(newValue);
                        } else {
                            objChartDataSet.YValueProperty().set(newValue);
                        }
                    }
                });

            }
            @SuppressWarnings({"unused"})
            boolean fReturn = ((XYChart) getChart()).getData().add(objSeries);
        }
    }

    public void setxAxis(NumberAxis _xAxis) {
        this._xAxis = _xAxis;
    }

    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }

    public void setxAxisMajorTick(double xAxisMajorTick) {
        this.xAxisMajorTick = xAxisMajorTick;
    }

    public void setxAxisMajorTickCount(double count) {
        xAxisMajorTickCount = count;
    }

    public void setxAxisMaxCount(int xAxisMaxCount) {
        this.xAxisMaxCount = xAxisMaxCount;
    }

    public void setxAxisMinorTick(int xAxisMinorTick) {
        this.xAxisMinorTick = xAxisMinorTick;
    }

    public void setxAxisMinorTickCount(double count) {
        xAxisMinorTickCount = count;
    }

    public void setxAxisTickVisible(boolean xAxisTickVisible) {
        this.xAxisTickVisible = xAxisTickVisible;
    }

    public void setyAxis(NumberAxis _yAxis) {
        this._yAxis = _yAxis;
    }

    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
    }

    public void setyAxisMajorTick(double yAxisMajorTick) {
        this.yAxisMajorTick = yAxisMajorTick;
    }

    public void setyAxisMajorTickCount(double count) {
        yAxisMajorTickCount = count;
    }

    public void setyAxisMaxCount(int yAxisMaxCount) {
        this.yAxisMaxCount = yAxisMaxCount;
    }

    public void setyAxisMaxValue(double yAxisMaxValue) {
        this.yAxisMaxValue = yAxisMaxValue;
    }

    public void setyAxisMinorTick(int yAxisMinorTick) {
        this.yAxisMinorTick = yAxisMinorTick;
    }

    public void setyAxisMinorTickCount(double count) {
        yAxisMinorTickCount = count;
    }

    public void setyAxisTickVisible(boolean yAxisTickVisible) {
        this.yAxisTickVisible = yAxisTickVisible;
    }

    /**
     * Routine nukes 1st entry, shifts everything left
     *
     * @param series
     * @param Max
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void ShiftSeries(XYChart.Series series, int Max) {
        if (series.getData().size() < Max) {
            return;
        }
        for (int iLoop = 0; iLoop < series.getData().size() - 1; iLoop++) {
            XYChart.Data point = (XYChart.Data) series.getData().get(iLoop + 1);
            point.setXValue(iLoop);
        }
        series.getData().remove(0);
    }

    @Override
    public boolean SupportsSteppedRanges() {
        return true;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        _chart.setTitle(strTitle);
    }

    @Override
    public void UpdateValueRange() {
        // if ranges changed, then change ticks
        double currRange = abs(((NumberAxis) (_yAxis)).getUpperBound() - ((NumberAxis) (_yAxis)).getLowerBound());
        double newRange = abs(yAxisMaxValue - yAxisMinValue);
        double currTickCount = currRange / ((NumberAxis) (_yAxis)).getTickUnit();
        double newTickUnit = newRange / currTickCount;

        ((NumberAxis) (_yAxis)).setUpperBound(yAxisMaxValue);
        ((NumberAxis) (_yAxis)).setLowerBound(yAxisMinValue);
        ((NumberAxis) (_yAxis)).setTickUnit(newTickUnit);

        setupMinorTicks();
    }
}
