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
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.MediaPlayerWidget;

/**
 *
 * @author Patrick Kutch
 */
public class MediaPlayerWidgetBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static boolean ParseDefinitionFile(MediaPlayerWidget objWidget, FrameworkNode node)
    {
	if (BaseWidget.HandleCommonDefinitionFileConfig(objWidget, node))
	{
	    
	}
	else if (node.getNodeName().equalsIgnoreCase("#comment"))
	{
	    
	}
	else if (node.getNodeName().equalsIgnoreCase("AutoStart"))
	{
	    objWidget.setAutoStart(node.getBooleanValue());
	    
	}
	else if (node.getNodeName().equalsIgnoreCase("Repeat"))
	{
	    boolean bVal = node.getBooleanValue();
	    
	    if (bVal)
	    {
		if (node.hasAttribute("Mode"))
		{
		    if (node.getAttribute("Mode").equalsIgnoreCase("LoopList"))
		    {
			objWidget.setRepeatList(true);
			objWidget.setRepeatSingleMedia(false);
		    }
		    else if (node.getAttribute("Mode").equalsIgnoreCase("Single"))
		    {
			objWidget.setRepeatList(false);
			objWidget.setRepeatSingleMedia(true);
		    }
		    else
		    {
			LOGGER.severe(objWidget.getWidgetType()
				+ " definition file has tag invalid <Repeat> Mide Attribute tag, expecting either LoopList or Single, got "
				+ node.getAttribute("Mode"));
			return false;
		    }
		}
	    }
	    return true;
	}
	else if (node.getNodeName().equalsIgnoreCase("InitialVolume"))
	{
	    String strVal = node.getTextContent();
	    try
	    {
		objWidget.setVolumeLevel(Double.parseDouble(strVal));
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.severe(objWidget.getWidgetType() + " definition file has tag invalid <InitialVolume> tag, got "
			+ node.getTextContent());
		return false;
	    }
	    return true;
	}
	
	else
	{
	    return false;
	}
	
	return true;
    }
    
}
