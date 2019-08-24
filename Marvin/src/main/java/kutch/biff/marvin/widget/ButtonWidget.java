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

import java.io.File;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;

/**
 *
 * @author Patrick Kutch
 */
public class ButtonWidget extends BaseWidget
{
    private Button _Button;
    protected String _ImageFileName;
    protected double _ImageWidthConstraint, _ImageHeightConstraint;
    
    public ButtonWidget()
    {
	_Button = new Button();
	_ImageFileName = null;
	_ImageWidthConstraint = 0;
	_ImageHeightConstraint = 0;
    }
    
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
	SetParent(pane);
	getButton().disableProperty().set(!_InitiallyEnabled);
	
	ConfigureDimentions();
	
	ConfigureAlignment();
	SetupPeekaboo(dataMgr);
	
	getButton().setText(getTitle());
	if (false == SetupImage())
	{
	    return false;
	}
	pane.add(getButton(), getColumn(), getRow(), getColumnSpan(), getRowSpan());
	dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
	{
	    @Override
	    public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
	    {
		onChange(o, oldVal, newVal);
	    }
	});
	
	SetupTaskAction();
	return ApplyCSS();
    }
    
    protected ButtonBase getButton()
    {
	return _Button;
    }
    
    @Override
    public javafx.scene.Node getStylableObject()
    {
	return getButton();
    }
    
    @Override
    public ObservableList<String> getStylesheets()
    {
	return getButton().getStylesheets();
    }
    
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetNode)
    {
	if (widgetNode.getNodeName().equalsIgnoreCase("Image"))
	{
	    setImageFileName(widgetNode.getTextContent());
	    
	    Utility.ValidateAttributes(new String[] { "Height", "Width" }, widgetNode);
	    if (widgetNode.hasAttribute("Width"))
	    {
		try
		{
		    _ImageWidthConstraint = Double.parseDouble(widgetNode.getAttribute("Width"));
		}
		catch(Exception ex)
		{
		    LOGGER.severe("Button Image has invalid Width specified: " + widgetNode.getAttribute("Width"));
		    return false;
		}
	    }
	    if (widgetNode.hasAttribute("Height"))
	    {
		try
		{
		    _ImageHeightConstraint = Double.parseDouble(widgetNode.getAttribute("Height"));
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe("Button Image has invalid Height specified: " + widgetNode.getAttribute("Height"));
		    return false;
		}
	    }
	    
	    return true;
	}
	return false;
    }
    
    protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal)
    {
	String strVal = newVal.toString();
	getButton().setText(strVal);
    }
    
    @Override
    public void SetEnabled(boolean enabled)
    {
	getButton().disableProperty().set(!enabled);
    }
    
    public void setImageFileName(String _ImageFileName)
    {
	this._ImageFileName = _ImageFileName;
    }
    
    private boolean SetupImage()
    {
	if (null != _ImageFileName)
	{
	    String fname = convertToFileOSSpecific(_ImageFileName);
	    File file = new File(fname);
	    if (file.exists())
	    {
		String fn = "file:" + fname;
		Image img = new Image(fn);
		ImageView view = new ImageView(img);
		
		if (_ImageHeightConstraint > 0)
		{
		    view.setFitHeight(_ImageHeightConstraint);
		}
		if (_ImageWidthConstraint > 0)
		{
		    view.setFitWidth(_ImageWidthConstraint);
		}
		
		getButton().setGraphic(view);
		
		LOGGER.config("Adding Image to Button - " + _ImageFileName);
		return true;
	    }
	    else
	    {
		LOGGER.severe("Invalid Image File specified for Button: " + _ImageFileName);
		return false;
	    }
	}
	return true;
    }
    
    @Override
    public boolean SupportsEnableDisable()
    {
	return true;
    }
    
    @Override
    public void UpdateTitle(String strTitle)
    {
	getButton().setText(strTitle);
    }
    
}
