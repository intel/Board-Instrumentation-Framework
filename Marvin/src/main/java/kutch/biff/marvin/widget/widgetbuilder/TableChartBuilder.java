package kutch.biff.marvin.widget.widgetbuilder;

import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.TableChartWidget;

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
/**
*
* @author Patrick.Kutch@gmail.com
*/
public class TableChartBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public final static TableChartWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
	TableChartWidget objTableChart =  new TableChartWidget();
	for (FrameworkNode node : masterNode.getChildNodes())
	{
	    if (BaseWidget.HandleCommonDefinitionFileConfig(objTableChart, node))
	    {
		continue;
	    }
	    else
	    {
		LOGGER.warning("Unknown section in " + widgetDefFilename +": " + node.getNodeName());
	    }
	}
	return objTableChart;
    }
}
