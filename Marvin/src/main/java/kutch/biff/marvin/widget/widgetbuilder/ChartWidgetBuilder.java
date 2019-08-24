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
package kutch.biff.marvin.widget.widgetbuilder;

import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.AreaChartWidget;
import kutch.biff.marvin.widget.AreaChartWidget_MS;
import kutch.biff.marvin.widget.BarChartWidget;
import kutch.biff.marvin.widget.BaseChartWidget;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.LineChartWidget;
import kutch.biff.marvin.widget.LineChartWidget_MS;
import kutch.biff.marvin.widget.StackedAreaChartWidget;
import kutch.biff.marvin.widget.StackedAreaChartWidget_MS;
import kutch.biff.marvin.widget.StackedBarChartWidget;

/**
 *
 * @author Patrick Kutch
 */
public class ChartWidgetBuilder
{
    
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static AreaChartWidget BuildAreaChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	AreaChartWidget objWidget = new AreaChartWidget();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static BarChartWidget BuildBarChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	BarChartWidget objWidget = new BarChartWidget(false);
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static BarChartWidget BuildHorizontalBarChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	BarChartWidget objWidget = new BarChartWidget(true);
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static LineChartWidget BuildLineChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	LineChartWidget objWidget = new LineChartWidget();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static AreaChartWidget_MS BuildMultiSourceAreaChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	AreaChartWidget_MS objWidget = new AreaChartWidget_MS();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static LineChartWidget_MS BuildMultiSourceLineChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	LineChartWidget_MS objWidget = new LineChartWidget_MS();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static StackedAreaChartWidget_MS BuildMultiSourceStackedAreaChart(FrameworkNode masterNode,
	    String widgetDefFilename)
    {
	StackedAreaChartWidget_MS objWidget = new StackedAreaChartWidget_MS();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static StackedAreaChartWidget BuildStackedAreaChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	StackedAreaChartWidget objWidget = new StackedAreaChartWidget();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    public static BarChartWidget BuildStackedlBarChart(FrameworkNode masterNode, String widgetDefFilename)
    {
	StackedBarChartWidget objWidget = new StackedBarChartWidget();
	if (false == ChartWidgetBuilder.HandleCommonChartWidgetDefinition(masterNode, objWidget, widgetDefFilename))
	{
	    return null;
	}
	
	return objWidget;
    }
    
    private static boolean HandleCommonChartWidgetDefinition(FrameworkNode masterNode, BaseChartWidget chart,
	    String widgetDefFilename)
    {
	for (FrameworkNode node : masterNode.getChildNodes())
	{
	    if (BaseWidget.HandleCommonDefinitionFileConfig(chart, node))
	    {
		continue;
	    }
	    if (node.getNodeName().equalsIgnoreCase("Animated"))
	    {
		String str = node.getTextContent();
		if (0 == str.compareToIgnoreCase("True"))
		{
		    chart.setAnimated(true);
		}
		else if (0 == str.compareToIgnoreCase("False"))
		{
		    chart.setAnimated(false);
		}
		else
		{
		    LOGGER.severe(
			    "Invalid Chart Widget Definition File.  Animated should be True or False, not:" + str);
		    return false;
		}
	    }
	    else if (node.getNodeName().equalsIgnoreCase("Synchronized"))
	    {
		Utility.ValidateAttributes(new String[] { "MaxSyncWait" }, node);
		String str = node.getTextContent();
		boolean flag;
		int interval = 2500;
		
		if (0 == str.compareToIgnoreCase("True"))
		{
		    flag = true;
		}
		else if (0 == str.compareToIgnoreCase("False"))
		{
		    flag = false;
		}
		else
		{
		    LOGGER.severe(
			    "Invalid Chart Widget Definition File.  Synchronized should be True or False, not:" + str);
		    return false;
		}
		if (node.hasAttribute("MaxSyncWait"))
		{
		    try
		    {
			interval = Integer.parseInt(node.getAttribute("MaxSyncWait"));
		    }
		    catch(Exception ex)
		    {
			LOGGER.severe(
				"Invalid Chart Widget definition MaxSyncWait: " + node.getAttribute("MaxSyncWait"));
			return false;
		    }
		}
		chart.SetSynchronizeInformation(flag, interval);
		
	    }
	    
	    else if (node.getNodeName().equalsIgnoreCase("xAxis"))
	    {
		Utility.ValidateAttributes(new String[] { "MajorTickInterval", "MinorTickInterval", "TickLabelVisible",
			"MajorTickCount", "MinorTickCount" }, node);
		if (node.hasAttribute("MajorTickInterval"))
		{
		    if (node.hasAttribute("MajorTickCount"))
		    {
			LOGGER.severe(
				"Chart Widget definition has MajorTickInterval and MajorTickCount.  Ignoreing MajorTickInterval");
		    }
		    else
		    {
			try
			{
			    chart.setxAxisMajorTick(Double.parseDouble(node.getAttribute("MajorTickInterval")));
			}
			catch(Exception ex)
			{
			    LOGGER.severe("Invalid Chart Widget definition MajorTickInterval: "
				    + node.getAttribute("MajorTickInterval"));
			    return false;
			}
		    }
		}
		if (node.hasAttribute("MajorTickCount"))
		{
		    try
		    {
			chart.setxAxisMajorTickCount(Double.parseDouble(node.getAttribute("MajorTickInterval")));
		    }
		    catch(Exception ex)
		    {
			LOGGER.severe("Invalid Chart Widget definition MajorTickCount: "
				+ node.getAttribute("MajorTickCount"));
			return false;
		    }
		}
		if (node.hasAttribute("MinorTickInterval"))
		{
		    if (node.hasAttribute("MinortTickCount"))
		    {
			LOGGER.severe(
				"Chart Widget definition has MinorTickInterval and MinorTickCount.  Ignoreing MinorTickInterval");
		    }
		    else
		    {
			try
			{
			    chart.setxAxisMinorTick(Integer.parseInt(node.getAttribute("MinorTickInterval")));
			}
			catch(Exception ex)
			{
			    LOGGER.severe("Invalid Chart Widget definition MinorTickInterval: "
				    + node.getAttribute("MinorTickInterval"));
			    return false;
			}
		    }
		}
		if (node.hasAttribute("MinorTickCount"))
		{
		    try
		    {
			chart.setxAxisMinorTickCount(Double.parseDouble(node.getAttribute("MinorTickCount")));
		    }
		    catch(Exception ex)
		    {
			LOGGER.severe("Invalid Chart Widget definition inorTickCount: "
				+ node.getAttribute("MinorTickCount"));
			return false;
		    }
		}
		if (node.hasAttribute("TickLabelVisible"))
		{
		    String str = node.getAttribute("TickLabelVisible");
		    if (0 == str.compareToIgnoreCase("True"))
		    {
			chart.setxAxisTickVisible(true);
		    }
		    else if (0 == str.compareToIgnoreCase("False"))
		    {
			chart.setxAxisTickVisible(false);
		    }
		    else
		    {
			LOGGER.severe(
				"Invalid Chart Widget Definition File.  TickLable Visible should be True or False, not:"
					+ str);
			return false;
		    }
		}
	    }
	    else if (node.getNodeName().equalsIgnoreCase("yAxis"))
	    {
		Utility.ValidateAttributes(new String[] { "MajorTickInterval", "MinorTickInterval", "TickLabelVisible",
			"MajorTickCount", "MinorTickCount" }, node);
		if (node.hasAttribute("MajorTickInterval"))
		{
		    if (node.hasAttribute("MajorTickCount"))
		    {
			LOGGER.severe(
				"Chart Widget definition has MajorTickInterval and MajorTickCount.  Ignoreing MajorTickInterval");
		    }
		    else
		    {
			try
			{
			    chart.setyAxisMajorTick(Double.parseDouble(node.getAttribute("MajorTickInterval")));
			}
			catch(Exception ex)
			{
			    LOGGER.severe("Invalid Chart Widget definition MajorTickInterval: "
				    + node.getAttribute("MajorTickInterval"));
			    return false;
			}
		    }
		}
		if (node.hasAttribute("MajorTickCount"))
		{
		    try
		    {
			chart.setyAxisMajorTickCount(Double.parseDouble(node.getAttribute("MajorTickCount")));
		    }
		    catch(Exception ex)
		    {
			LOGGER.severe("Invalid Chart Widget definition MajorTickCount: "
				+ node.getAttribute("MajorTickCount"));
			return false;
		    }
		}
		if (node.hasAttribute("MinorTickInterval"))
		{
		    if (node.hasAttribute("MinortTickCount"))
		    {
			LOGGER.severe(
				"Chart Widget definition has MinorTickInterval and MinorTickCount.  Ignoreing MinorTickInterval");
		    }
		    else
		    {
			try
			{
			    chart.setyAxisMinorTick(Integer.parseInt(node.getAttribute("MinorTickInterval")));
			}
			catch(Exception ex)
			{
			    LOGGER.severe("Invalid Chart Widget definition MinorTickInterval: "
				    + node.getAttribute("MinorTickInterval"));
			    return false;
			}
		    }
		}
		if (node.hasAttribute("MinorTickCount"))
		{
		    try
		    {
			chart.setyAxisMinorTickCount(Double.parseDouble(node.getAttribute("MinorTickCount")));
		    }
		    catch(Exception ex)
		    {
			LOGGER.severe("Invalid Chart Widget definition inorTickCount: "
				+ node.getAttribute("MinorTickCount"));
			return false;
		    }
		}
		if (node.hasAttribute("TickLabelVisible"))
		{
		    String str = node.getAttribute("TickLabelVisible");
		    if (0 == str.compareToIgnoreCase("True"))
		    {
			chart.setyAxisTickVisible(true);
		    }
		    else if (0 == str.compareToIgnoreCase("False"))
		    {
			chart.setyAxisTickVisible(false);
		    }
		    else
		    {
			LOGGER.severe(
				"Invalid Chart Widget Definition File.  TickLable Visible should be True or False, not:"
					+ str);
			return false;
		    }
		}
	    }
	    
	}
	
	return true;
    }
    
}
