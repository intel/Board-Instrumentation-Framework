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

import eu.hansolo.enzo.lcd.Lcd;
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
public class SteelLCDWidget extends BaseWidget
{
    private static double aspectRatio = 2.75;
    private Lcd _LCD = null;
    private String UnitText;
    private boolean ShowMeasuredMax;
    private boolean ShowMeasuredMin;
    private double MinValue;
    private double MaxValue;
    private boolean KeepAspectRatio;
    private boolean _TextMode;
    private String _InitialValue = "";
    
    public SteelLCDWidget()
    {
	_LCD = new Lcd();
	KeepAspectRatio = true;
	_TextMode = false;
	
	UnitText = "";
	_LCD.setAnimationDuration(300);
    }
    
    @Override
    protected void ConfigureDimentions()
    {
	if (KeepAspectRatio)
	{
	    if (getWidth() > 0)
	    {
		setHeight(getWidth() / aspectRatio);
	    }
	    else if (getHeight() > 0)
	    {
		setWidth(getHeight() * aspectRatio);
	    }
	}
	super.ConfigureDimentions();
    }
    
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
	SetParent(pane);
	if (false == SetupLCD())
	{
	    return false;
	}
	if (_InitialValue.length() > 0)
	{
	    SetValue(_InitialValue);
	}
	
	ConfigureAlignment();
	ConfigureDimentions();
	
	pane.add(_LCD, getColumn(), getRow(), getColumnSpan(), getRowSpan());
	SetupPeekaboo(dataMgr);
	
	dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
	{
	    @Override
	    public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
	    {
		if (IsPaused())
		{
		    return;
		}
		SetValue(newVal.toString());
	    }
	});
	SetupTaskAction();
	
	return ApplyCSS();
    }
    
    public double getMaxValue()
    {
	return MaxValue;
    }
    
    public double getMinValue()
    {
	return MinValue;
    }
    
    @Override
    public javafx.scene.Node getStylableObject()
    {
	return _LCD;
    }
    
    @Override
    public ObservableList<String> getStylesheets()
    {
	return _LCD.getStylesheets();
    }
    
    public boolean getTextMode()
    {
	return _TextMode;
    }
    
    public String getUnitText()
    {
	return UnitText;
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
	return true;
    }
    
    public boolean isKeepAspectRatio()
    {
	return KeepAspectRatio;
    }
    
    public boolean isShowMeasuredMax()
    {
	return ShowMeasuredMax;
    }
    
    public boolean isShowMeasuredMin()
    {
	return ShowMeasuredMin;
    }
    
    @Override
    public void SetInitialValue(String value)
    {
	_InitialValue = value;
    }
    
    public void setKeepAspectRatio(boolean KeepAspectRation)
    {
	this.KeepAspectRatio = KeepAspectRation;
    }
    
    public void setMaxValue(double MaxValue)
    {
	this.MaxValue = MaxValue;
    }
    
    public void setMinValue(double MinValue)
    {
	this.MinValue = MinValue;
    }
    
    public void setShowMeasuredMax(boolean ShowMeasuredMax)
    {
	this.ShowMeasuredMax = ShowMeasuredMax;
    }
    
    public void setShowMeasuredMin(boolean ShowMeasuredMin)
    {
	this.ShowMeasuredMin = ShowMeasuredMin;
    }
    
    public void setTextMode(boolean newMode)
    {
	_TextMode = newMode;
	_LCD.setTextMode(newMode);
	if (newMode)
	{
	    _LCD.setDecimals(0);
	}
	else
	{
	    _LCD.setDecimals(getDecimalPlaces());
	}
    }
    
    public void setUnitText(String UnitText)
    {
	this.UnitText = UnitText;
    }
    
    private boolean SetupLCD()
    {
	if (!_TextMode)
	{
	    _LCD.setMinMeasuredValueVisible(ShowMeasuredMin);
	    _LCD.setMaxMeasuredValueVisible(ShowMeasuredMax);
	    _LCD.setMaxValue(getMaxValue());
	    _LCD.setKeepAspect(KeepAspectRatio);
	    _LCD.setDecimals(getDecimalPlaces());
	}
	
	if (getTitle().length() > 0)
	{
	    _LCD.setTitle(getTitle());
	}
	if (null != getUnitsOverride())
	{
	    _LCD.setUnit(getUnitsOverride());
	    LOGGER.config("Overriding Widget Units Text to " + getUnitsOverride());
	}
	else if (UnitText.length() > 0)
	{
	    _LCD.setUnit(UnitText);
	}
	
	_LCD.setCrystalOverlayVisible(true); // 'rgainy, LCD like overlay, very subtle
	return true;
    }
    
    public void SetValue(String newVal)
    {
//        if (true || !_TextMode)
	if (!_TextMode)
	{
	    double newDialValue;
	    String strVal = newVal;
	    try
	    {
		newDialValue = Double.parseDouble(strVal);
		setTextMode(false);
	    }
	    catch(NumberFormatException ex)
	    {
		_LCD.setText(strVal);
		return;
	    }
	    _LCD.setValue(newDialValue);
	}
	else
	{
	    _LCD.setText(newVal);
	}
    }
    
    @Override
    public void UpdateTitle(String strTitle)
    {
	_LCD.setTitle(strTitle);
    }
    
}
