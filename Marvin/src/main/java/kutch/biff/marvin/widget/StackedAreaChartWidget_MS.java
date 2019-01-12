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

import javafx.collections.ObservableList;
import javafx.scene.chart.Chart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.SeriesDataSet;

/**
 *
 * @author Patrick Kutch
 */
public class StackedAreaChartWidget_MS extends AreaChartWidget_MS
{
    public StackedAreaChartWidget_MS()
    {
        _isStackedChart = true;
    }
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        return _CreateMSChart(pane, dataMgr);
    }
  @Override
    protected Chart CreateChartObject()
    {
        return new StackedAreaChart<>(getxAxis(), getyAxis());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void _CreateChart()
    {
        CreateChart();

        for (SeriesDataSet ds : getSeries())
        {
            ds.getSeries().getData().add(new XYChart.Data<>(0,0)); // Stacked Charts will crash if you don't have any data in a series.
           ((StackedAreaChart) (getChart())).getData().add(ds.getSeries());
        }
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return ((StackedAreaChart) (getChart()));
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return ((StackedAreaChart) (getChart())).getStylesheets();
    }    
    
}
