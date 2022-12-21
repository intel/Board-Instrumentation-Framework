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

import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Chart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.SeriesDataSet;
import kutch.biff.marvin.utility.SeriesSet;

/**
 * @author Patrick Kutch
 */
public class StackedBarChartWidget extends BarChartWidget {

    public StackedBarChartWidget() {
        super(false);
        _isStackedChart = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Chart CreateChartObject() {
        ArrayList<String> list = new ArrayList<>();

        for (String key : _SeriesOrder) {
            if (null == _SeriesMap.get(key)) {
                LOGGER.severe("Unexpected probelm in CreateChartObject");
                break;
            }

            ArrayList<SeriesDataSet> setList = _SeriesMap.get(key).getSeriesList();
            if (null == setList) {
                LOGGER.severe("Unexpected probelm in CreateChartObject");
                break;
            }

            for (SeriesDataSet set : setList) {
                list.add(set.getTitle());
            }
            break;
        }

        getAxis_X().setCategories(FXCollections.observableArrayList(list));
        return new StackedBarChart<String, Number>(getAxis_X(), getyAxis());
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return ((getChart()));
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return ((StackedBarChart<?, ?>) (getChart())).getStylesheets();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void setupListeners(DataManager dataMgr) {
        for (String key : _SeriesOrder) {
            SeriesSet objSeriesSet = _SeriesMap.get(key);
            if (null == objSeriesSet) {
                LOGGER.severe("Invalid Key in setupListeners");
                return;
            }
            XYChart.Series objSeries = new XYChart.Series<>();
            String strTitle = objSeriesSet.getTitle();
            if (null == strTitle) {
                strTitle = "untitled";
            }

            objSeries.setName(strTitle);

            for (SeriesDataSet objDs : objSeriesSet.getSeriesList()) {
                XYChart.Data objData;
                if (isHorizontal()) {
                    objData = new XYChart.Data(0, objDs.getTitle());
                } else {
                    objData = new XYChart.Data(objDs.getTitle(), 0);
                }

                objSeries.getData().add(objData);

                dataMgr.AddListener(objDs.getID(), objDs.getNamespace(), (o, oldVal, newVal) -> {
                    if (IsPaused()) {
                        return;
                    }
                    String strVal = newVal.toString();
                    double newValue;
                    try {
                        newValue = Double.parseDouble(strVal);
                        HandleSteppedRange(newValue);
                    } catch (Exception ex) {
                        LOGGER.severe("Invalid data for Line Chart received: " + strVal);
                        return;
                    }
                    if (isHorizontal()) {
                        objData.XValueProperty().set(newValue);
                    } else {
                        objData.YValueProperty().set(newValue);
                    }
                });

            }
            ((StackedBarChart) getChart()).getData().add(objSeries);

        }
    }

}
