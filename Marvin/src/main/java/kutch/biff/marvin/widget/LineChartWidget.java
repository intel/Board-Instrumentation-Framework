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
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.SeriesDataSet;

/**
 *
 * @author Patrick Kutch
 */
public class LineChartWidget extends LineChartWidget_MS
{
    @Override
    @SuppressWarnings("unchecked")
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        if (getSeries().isEmpty())
        {
            if (getyAxisMaxCount() > 0)
            {
                for (int iLoop = 0; iLoop < getyAxisMaxCount(); iLoop++)
                {
                    SeriesDataSet objDS = new SeriesDataSet(Integer.toString(iLoop), "", "");
                    getSeries().add(objDS);
                }
            }
            else
            {
                LOGGER.severe("Chart created with no series defined and no count defined for yAxis");
                return false;
            }
        }

        _CreateChart();
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(getChart(), getColumn(), getRow(), getColumnSpan(), getRowSpan());
        //hmm, only get called if different, that could be a problem for a chart

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
        {
            @Override
            @SuppressWarnings("unchecked")
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                if (IsPaused())
                {
                    return;
                }

                String[] strList = newVal.toString().split(",");
                int iIndex = 0;
                for (String strValue : strList)
                {
                    double newValue;
                    try
                    {
                        newValue = Double.parseDouble(strValue);
                        HandleSteppedRange(newValue);
                    }
                    catch (NumberFormatException ex)
                    {
                        LOGGER.severe("Invalid data for Line Chart received: " + strValue);
                        return;
                    }

                    if (iIndex < getSeries().size())
                    {
                        SeriesDataSet ds = getSeries().get(iIndex++);
                        ShiftSeries(ds.getSeries(), getxAxisMaxCount());
                        ds.getSeries().getData().add(new XYChart.Data<>(ds.getSeries().getData().size(), newValue));
                    }
                    else
                    {
                        LOGGER.severe("Received More datapoints for Line Chart than was defined in application definition file. Received " + Integer.toString(strList.length) + " expecting " + Integer.toString(getSeries().size()));
                        return;
                    }
                }
            }
        });

        SetupTaskAction();
        return ApplyCSS();
    }

    /**
     *
     * @param node
     * @return
     */
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        if (true == HandleChartSpecificAppSettings(node))
        {
            return true;
        }

        if (node.getNodeName().equalsIgnoreCase("Series"))
        {
            String Label;
            if (node.hasAttribute("Label"))
            {
                Label = node.getAttribute("Label");
            }
            else
            {
                Label = "";
            }
            SeriesDataSet objDS = new SeriesDataSet(Label, "", "");
            getSeries().add(objDS);
            return true;
        }

        return false;
    }

}
