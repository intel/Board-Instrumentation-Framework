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

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */

/*
 * <Widget file="$(MenuDir)/MenuButton.xml" row="$(CurrentColumnAlias)"
 * column="$(CurrentColumnAlias)" Align="NW"> <Title>Slide</Title> <MenuItem
 * Text="Cover" Task="Slide.Cover"/> <MenuItem Text="Legal" Task="Slide.Legal"/>
 * <MenuItem Text="Setup" Task="Slide.Setup"/> <MenuItem Text="ML Overview"
 * Task="Slide.ML_Overview"/> <MenuItem Text="Call To Action"
 * Task="Slide.CallToAction"/> <MinionSrc Namespace="TestNS" ID="ButtonTest"
 * Task="OneTaskForAllOptions" /> </Widget>
 * 
 */
public class MenuButtonWidget extends ButtonWidget
{
    private MenuButton _Button;
    private FrameworkNode __CommonTaskNode;
    private boolean __UpdateTitleFromSelection;
    private String __LastValue;
    
    public MenuButtonWidget()
    {
	_Button = new MenuButton();
	// to get around styling bug in Java 8
	_Button.getStyleClass().add("kutch");
	__CommonTaskNode = null;
	__UpdateTitleFromSelection = true; // can turn off laster
	__LastValue="";
    }
    
    @Override
    protected ButtonBase getButton()
    {
	return _Button;
    }
    
    @Override
    public boolean HandleWidgetSpecificConfiguration(FrameworkNode widgetNode)
    {
	if (widgetNode.hasChild("MinionSrc"))
	{
	    FrameworkNode minionSrcNode = widgetNode.getChild("MinionSrc");
	    if (minionSrcNode.hasChild("MenuItem"))
	    {
		FrameworkNode commonTaskNode = minionSrcNode.getChild("MenuItem");
		setCommonTaskNode(commonTaskNode);
	    }
	    else
	    {
		LOGGER.severe("MenuButtonWidget with <MinionSrc> is invalid, needs <MenuItem> in <MinionSrc>");
		return false;
	    }
	}
	
	if (widgetNode.hasChild("Title") && widgetNode.getChild("Title").hasAttribute("UpdateTitleFromSelection"))
	{
	    __UpdateTitleFromSelection= widgetNode.getChild("Title").getBooleanAttribute("UpdateTitleFromSelection");
	}
	else
	{
	    __UpdateTitleFromSelection = false;
	}
	
	return true;
    }
    
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetNode)
    {
	if (super.HandleWidgetSpecificSettings(widgetNode))
	{
	    return true;
	}
	
	if (widgetNode.getNodeName().equalsIgnoreCase("MenuItem"))
	{
	    ConfigurationReader rdr = ConfigurationReader.GetConfigReader();
	    
	    MenuItem objItem = rdr.ReadMenuItem(widgetNode,_Button.getItems().size());
	    if (null != objItem)
	    {
		_Button.getItems().add(objItem);
		setupChangeTitleListener(objItem);
		return true;
	    }
	}
	
	return false;
    }
    
    @Override
    protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal)
    {
	String entriesList = newVal.toString();
	if (entriesList.length() < 1 || __LastValue.equals(entriesList))
	{
	    return;
	}
	__LastValue = entriesList;
	_Button.getItems().clear();
	String[] newEntries = newVal.toString().split(",");
	ConfigurationReader rdr = ConfigurationReader.GetConfigReader();

	for (String entry : newEntries)
	{
	    FrameworkNode menuNode = new FrameworkNode(__CommonTaskNode);
	    
	    menuNode.AddAttibute("Text", entry);
	    MenuItem objItem = null;
	    try
	    {
		objItem = rdr.ReadMenuItem(menuNode, _Button.getItems().size());
		setupChangeTitleListener(objItem);
	    }
	    catch(Exception ex)
	    {
		LOGGER.severe(ex.toString());
	    }
	    if (null != objItem)
	    {
		try
		{
		    _Button.getItems().add(objItem);
		}
		catch(Exception ex)
		{
		    LOGGER.severe(ex.toString());
		}
	    }
	}
	ApplyCSS();
    }
    
    public void setCommonTaskNode(FrameworkNode commonTask)
    {
	__CommonTaskNode = new FrameworkNode(commonTask);  // make new instance, to get alias resolved now
	//__CommonTaskNode.ResolveAlias();
    }
    
    private void setupChangeTitleListener(MenuItem objItem)
    {
	if (__UpdateTitleFromSelection)
	{
	    EventHandler<ActionEvent> eventHandler = new EventHandler<ActionEvent>()
	    {
		@Override
		public void handle(ActionEvent event)
		{
		    if (__UpdateTitleFromSelection) // sometimes flag is read AFTER handler setup
		    {
			_Button.setText(objItem.getText());
		    }
		}
	    };
	    objItem.addEventHandler(ActionEvent.ACTION, eventHandler);
	}
    }
    
    @Override
    public EventHandler<MouseEvent> SetupTaskAction()
    {
	// Tasks for Menu Button are setup down in Configuration:ReadMenuItem
	return null;
    }
}
