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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.SeriesDataSet;
import kutch.biff.marvin.utility.Utility;

/**
 * @author Patrick Kutch
 */
public class LineChartWidget_MS extends BaseChartWidget {

    public LineChartWidget_MS() {

    }

    @SuppressWarnings("unchecked")
    protected void _CreateChart() {
        CreateChart();

        for (SeriesDataSet ds : getSeries()) {
            ((LineChart<?, ?>) (getChart())).getData().add(ds.getSeries());
        }
    }

    /**
     * @param pane
     * @param dataMgr
     * @return
     */
    protected boolean _CreateMSChart(GridPane pane, DataManager dataMgr) {
        _CreateChart();
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        ConfigureSynchronizationForMultiSource();

        pane.add(getChart(), getColumn(), getRow(), getColumnSpan(), getRowSpan());
        if (0 == _SeriesOrder.size()) {
            for (SeriesDataSet ds : getSeries()) {
                dataMgr.AddListener(ds.getID(), ds.getNamespace(), (o, oldVal, newVal) -> {
                    if (IsPaused()) {
                        return;
                    }

                    String strVal = newVal.toString();
                    double newValue = 0;
                    try {
                        newValue = Double.parseDouble(strVal);
                        newValue *= ds.getScaleValue();
                        HandleSteppedRange(newValue);
                        strVal = Double.toString(newValue);
                    } catch (Exception ex) {
                        LOGGER.severe("Invalid data for Chart received: " + strVal);
                        return;
                    }
                    OnDataArrived(ds, strVal);
                });
            }
        } else {
            SetupSeriesSets(dataMgr);
        }
        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        return _CreateMSChart(pane, dataMgr);

    }

    @Override
    protected void CreateAxisObjects() {
        super.CreateAxisObjects();
        if (0 != _SeriesOrder.size()) {
            _xAxis = new CategoryAxis();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Chart CreateChartObject() {
        return new LineChart<Number, Number>(getxAxis(), getyAxis());
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return ((LineChart<?, ?>) (getChart()));
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return ((LineChart<?, ?>) (getChart())).getStylesheets();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if (true == HandleChartSpecificAppSettings(node)) {
            return true;
        }

        if (node.getNodeName().equalsIgnoreCase("Series")) {
            String ID, Namespace, Label;
            if (node.hasAttribute("Label")) {
                Label = node.getAttribute("Label");
            } else {
                Label = "";
            }
            for (FrameworkNode newNode : node.getChildNodes()) {
                if (newNode.getNodeName().equalsIgnoreCase("MinionSrc")) {
                    Utility.ValidateAttributes(new String[]{"ID", "Namespace", "Scale"}, newNode);
                    if (newNode.hasAttribute("ID")) {
                        ID = newNode.getAttribute("ID");
                    } else {
                        LOGGER.severe("Series defined with invalid MinionSrc - no ID");
                        return false;
                    }
                    if (newNode.hasAttribute("Namespace")) {
                        Namespace = newNode.getAttribute("Namespace");
                    } else {
                        LOGGER.severe("Series defined with invalid MinionSrc - no Namespace");
                        return false;
                    }
                    SeriesDataSet objDS = new SeriesDataSet(Label, ID, Namespace);
                    if (newNode.hasAttribute("Scale")) {
                        double scaleVal = newNode.getDoubleAttribute("Scale", 0);
                        if (scaleVal <= 0) {
                            LOGGER.severe("Series defined with invalid scale value" + newNode.getAttribute("Scale"));
                            return false;
                        }
                        objDS.setScaleValue(scaleVal);
                    }

                    getSeries().add(objDS);
                }
            }
            return true;
        }

        return false;
    }
}
