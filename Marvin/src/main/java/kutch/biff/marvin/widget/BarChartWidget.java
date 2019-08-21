/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.datamanager.MarvinChangeListener;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public class BarChartWidget extends LineChartWidget
{
    private final CategoryAxis _xAxis;
    private XYChart.Series<String, Number> _objSeries;

    public BarChartWidget(boolean Horizontal)
    {
        _xAxis = new CategoryAxis();
        _HorizontalChart = Horizontal;
        _objSeries = new XYChart.Series<>();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        CreateBarChart();
        ConfigureDimentions();
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(getChart(), getColumn(), getRow(), getColumnSpan(), getRowSpan());
        setupListeners(dataMgr);

        SetupTaskAction();
        return ApplyCSS();
    }

    private void CreateBarChart()
    {
        CreateChart();
        _xAxis.setLabel(getxAxisLabel());
        
        _xAxis.setAnimated(false);  // for some reason for this chart, it defaults to true!
    }

    protected void setupListeners(DataManager dataMgr)
    {
        if (0 == _SeriesOrder.size())
        {
            setupListenersForSingleSource(dataMgr);
            return;
        }
        SetupSeriesSets(dataMgr);
    }
    
    protected void setupAxis()
    {
	_objSeries.getData().clear();	


        for (int iLoop = 0; iLoop < getxAxisMaxCount(); iLoop++)
        {
	    Data<String, Number> objData = new XYChart.Data<>(Integer.toString(iLoop), 0);
	    _objSeries.getData().add(objData);
	    //displayLabelForData(objData);
        }
    }
    
    protected void resizeAxis(int newSize)
    {
	int currSize = _objSeries.getData().size();
	if (currSize < newSize)
	{
	    while (currSize < newSize)
	    {
		Data<String, Number> objData = new XYChart.Data<>(Integer.toString(currSize), 0);

		_objSeries.getData().add(objData);
		      // displayLabelForData(objData);
		currSize++;
	    }
	}
	else while (currSize > newSize)
	{
	    _objSeries.getData().remove(--currSize);
	}
    }
    
 
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupListenersForSingleSource(DataManager dataMgr)
    {
	setupAxis();
        ((BarChart) getChart()).getData().add(_objSeries);	
        dataMgr.AddListener(getMinionID(), getNamespace(), new MarvinChangeListener(get__dataIndex(),get__dataIndexToken())
        {
            @Override
            public void onChanged(String newVal)
            {
                if (IsPaused())
                {
                    return;
                }

                String[] strList = newVal.split(",");
                if (strList.length != getxAxisMaxCount())
                {
                    LOGGER.info("Received " + Integer.toString(strList.length) + " items for a Bar Chart, so changing chart.");
                    setxAxisMaxCount(strList.length);
                    //setupAxis();
                    resizeAxis(getxAxisMaxCount());
                }
                
                int index = 0;
                
                for (String strValue : strList)
                {
                    double newValue;
                    try
                    {
                        newValue = Double.parseDouble(strValue);
                        newValue *= getValueScale();
                        HandleSteppedRange(newValue);
                    }
                    catch (NumberFormatException ex)
                    {
                        LOGGER.severe("Invalid data for Bar Chart received: " + strValue);
                        return;
                    }

                    XYChart.Data objData = _objSeries.getData().get(index++);
                    objData.setYValue(newValue);
                }
            }
        });

    }

    @SuppressWarnings("rawtypes")
    @Override
    public javafx.scene.Node getStylableObject()
    {
        return ((BarChart) (getChart()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ObservableList<String> getStylesheets()
    {
        return ((BarChart) (getChart())).getStylesheets();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Chart CreateChartObject()
    {
        if (isHorizontal())
        {
            getyAxis().setTickLabelRotation(90);
            return new BarChart<Number, String>(getyAxis(), _xAxis);
        }

        return new BarChart<String, Number>(_xAxis, getyAxis());
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        if (true == HandleChartSpecificAppSettings(node))
        {
            return true;
        }

        return false;
    }

    protected CategoryAxis getAxis_X()
    {
        return this._xAxis;
    }
    
    protected void setupMinorTicks()
    {
        ((NumberAxis) (_yAxis)).setMinorTickCount((int)yAxisMinorTick+1);
        // xAxis is not a number axis
    }
    private void displayLabelForData(XYChart.Data<String, Number> data) {
	  final Node node = data.getNode();
	  final Text dataText = new Text(data.getYValue() + "");
	  node.parentProperty().addListener(new ChangeListener<Parent>() {
	    @Override public void changed(ObservableValue<? extends Parent> ov, Parent oldParent, Parent parent) {
	      Group parentGroup = (Group) parent;
	      parentGroup.getChildren().add(dataText);
	    }
	  });
	 
	  node.boundsInParentProperty().addListener((ChangeListener<? super Bounds>) new ChangeListener<Bounds>() {
	    public void changed(ObservableValue<? extends Bounds> ov, Bounds oldBounds, Bounds bounds) {
	      dataText.setLayoutX(
	        Math.round(
	          bounds.getMinX() + bounds.getWidth() / 2 - dataText.prefWidth(-1) / 2
	        )
	      );
	      dataText.setLayoutY(
	        Math.round(
	          bounds.getMinY() - dataText.prefHeight(-1) * 0.5
	        )
	      );
	    }
	  });
	}    
}
