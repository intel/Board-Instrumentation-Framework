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
import kutch.biff.marvin.widget.BarGaugeWidget;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick Kutch
 */
public class BarGaugeWidgetBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static BarGaugeWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
	BarGaugeWidget gauge = new BarGaugeWidget();
	for (FrameworkNode node : masterNode.getChildNodes())
	{
	    if (BaseWidget.HandleCommonDefinitionFileConfig(gauge, node))
	    {
		continue;
	    }
	    else if (node.getNodeName().equalsIgnoreCase("MinValue"))
	    {
		String str = node.getTextContent();
		try
		{
		    gauge.setMinValue(Double.parseDouble(str));
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe("Invalid MinValue in BarGauge Widget Definition File");
		    return null;
		}
	    }
	    else if (node.getNodeName().equalsIgnoreCase("MaxValue"))
	    {
		String str = node.getTextContent();
		try
		{
		    gauge.setMaxValue(Double.parseDouble(str));
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe("Invalid MaxValue in BarGauge Widget Definition File");
		    return null;
		}
	    }
	    else if (node.getNodeName().equalsIgnoreCase("Decimals"))
	    {
		String str = node.getTextContent();
		try
		{
		    gauge.setDecimalPlaces(Integer.parseInt(str));
		}
		catch(NumberFormatException ex)
		{
		    LOGGER.severe("Invalid Decimals in Bareauge Widget Definition File");
		    return null;
		}
	    }
	    else if (node.getNodeName().equalsIgnoreCase("UnitText"))
	    {
		String str = node.getTextContent();
		gauge.setUnitText(str);
	    }
	    else
	    {
		LOGGER.severe("Invalid BarGauge Widget Definition File.  Unknown Tag: " + node.getNodeName());
		return null;
	    }
	    
	}
	return gauge;
    }
}
