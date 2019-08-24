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
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public class PieChartWidget extends BaseWidget
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private PieChart _Chart;
    private ArrayList<String> _Slices;

    public PieChartWidget()
    {
        _Chart = new PieChart();
        _Slices = new ArrayList<>();
    }

    public void AddSlide(String slice)
    {

        _Slices.add(slice);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        SetupPieChart();
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(_Chart, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
            {
                if (IsPaused())  
                {
                    return;
                }
                String[] strList = newVal.toString().split(","); // expecting comma separated data
                int iIndex = 0;
                for (String strValue : strList)
                {
                    double newValue;
                    try
                    {
                        newValue = Double.parseDouble(strValue);
                    }
                    catch (NumberFormatException ex)
                    {
                        LOGGER.severe("Invalid data for Line Chart received: " + strValue);
                        return;
                    }

                    if (iIndex < _Slices.size())
                    {
                        _Chart.getData().get(iIndex).setPieValue(newValue);
                    }
                    else
                    {
                        LOGGER.severe("Received More datapoints for Pie Chart than was defined in application definition file");
                        return;
                    }
                    iIndex++;
                }
            }
        });

        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return _Chart;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return _Chart.getStylesheets();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        if (node.getNodeName().equalsIgnoreCase("Slice"))
        {
            if (node.hasAttribute("Label"))
            {
                AddSlide(node.getAttribute("Label"));
            }
            else
            {
                LOGGER.severe("Pie Chart Widget has a slice with to 'Label' attribute.");
            }
            return true;
        }

        return false;
    }

    private void SetupPieChart()
    {
        for (String slice : _Slices)
        {
            _Chart.getData().add(new PieChart.Data(slice, 100.0 / _Slices.size()));
        }
        _Chart.setTitle(getTitle());
    }   
    @Override
    public boolean SupportsSteppedRanges()
    {
        return false;
    }

    @Override
    public void UpdateTitle(String strTitle)
    {
        _Chart.setTitle(strTitle);
    }
}
