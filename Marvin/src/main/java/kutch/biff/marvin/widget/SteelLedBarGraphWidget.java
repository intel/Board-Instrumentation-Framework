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

import eu.hansolo.enzo.led.Led;
import eu.hansolo.enzo.ledbargraph.LedBargraph;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import kutch.biff.marvin.datamanager.DataManager;

/**
 *
 * @author Patrick Kutch
 */
public class SteelLedBarGraphWidget extends BaseWidget
{
    
    private Orientation _Orientation;
    private int _NumberOfLeds;
    private int _LedSize;
    private Led.LedType _LedType;
    private boolean _ShowPeakValue;
    private Pane _Pane;
    
    private LedBargraph _BarGraph;
    
    public SteelLedBarGraphWidget()
    {
	_Orientation = Orientation.VERTICAL;
	_NumberOfLeds = 10;
	_LedSize = 25;
	_LedType = Led.LedType.ROUND;
	_ShowPeakValue = true;
	_Pane = new Pane();
	
	_BarGraph = new LedBargraph();
	_BarGraph.setValue(0);
	setDefaultIsSquare(false);
    }
    
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
	SetParent(pane);
	if (false == SetupBarGraph())
	{
	    return false;
	}
	_Pane.getChildren().add(_BarGraph);
	ConfigureAlignment();
	SetupPeekaboo(dataMgr);
	pane.add(_BarGraph, getColumn(), getRow(), getColumnSpan(), getRowSpan());
	// pane.add(_Pane, getColumn(), getRow(),getRowSpan(),getColumnSpan());
	
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
		    
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe("Invalid data for LED received: " + strVal);
		    return;
		}
		
		if (0.0 == newDialValue)
		{
		    _BarGraph.setValue(newDialValue);
		}
		else
		{
		    _BarGraph.setValue(newDialValue / 100);
		}
	    }
	});
	SetupTaskAction();
	
	return ApplyCSS();
    }
    
    public int getLedSize()
    {
	return _LedSize;
    }
    
    public Led.LedType getLedType()
    {
	return _LedType;
    }
    
    public int getNumberOfLeds()
    {
	return _NumberOfLeds;
    }
    
    public Orientation getOrientation()
    {
	return _Orientation;
    }
    
    @Override
    public javafx.scene.Node getStylableObject()
    {
	// return _Pane;
	return _BarGraph;
    }
    
    @Override
    public ObservableList<String> getStylesheets()
    {
	// return _Pane.getStylesheets();
	return _BarGraph.getStylesheets();
    }
    
    public boolean isShowPeakValue()
    {
	return _ShowPeakValue;
    }
    
    public void setLedSize(int _LedSize)
    {
	this._LedSize = _LedSize;
    }
    
    public void setLedType(Led.LedType _LedType)
    {
	this._LedType = _LedType;
    }
    
    public void setNumberOfLeds(int _NumberOfLeds)
    {
	this._NumberOfLeds = _NumberOfLeds;
    }
    
    public void setOrientation(Orientation _Orientation)
    {
	this._Orientation = _Orientation;
    }
    
    public void setShowPeakValue(boolean _ShowPeakValue)
    {
	this._ShowPeakValue = _ShowPeakValue;
    }
    
    private boolean SetupBarGraph()
    {
	_BarGraph.setOrientation(_Orientation);
	_BarGraph.setNoOfLeds(_NumberOfLeds);
	_BarGraph.setLedSize(_LedSize);
	_BarGraph.setLedType(_LedType);
	_BarGraph.setPeakValueVisible(_ShowPeakValue);
	
	ConfigureDimentions();
	
	double dimension = getWidth();
	if (_Orientation == Orientation.VERTICAL)
	{
	    dimension = getHeight();
	}
	
	double newSize = dimension / (getNumberOfLeds());
	_BarGraph.setLedSize(newSize);
	
	return true;
    }
    
    @Override
    public void UpdateTitle(String strTitle)
    {
	LOGGER.warning("Tried to update Title of a SteelLEDBarGraphWidget to " + strTitle);
    }
    
}
