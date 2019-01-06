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
import kutch.biff.marvin.widget.SteelGauge180Widget;

/**
 *
 * @author Patrick Kutch
 */
public class SteelGauge180Builder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static SteelGauge180Widget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        SteelGauge180Widget sg = new SteelGauge180Widget();
        
        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(sg, node))
            {
                continue;
            }
            else if (node.getNodeName().equalsIgnoreCase("MinValue"))
            {
                String str = node.getTextContent();
                try
                {
                    sg.setMinValue(Double.parseDouble(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid MinValue in Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("MaxValue"))
            {
                String str = node.getTextContent();
                try
                {
                    sg.setMaxValue(Double.parseDouble(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid MaxValue in SteelGauge180 Widget Definition File");
                    return null;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("UnitText"))
            {
                String str = node.getTextContent();
                sg.setUnitText(str);
            }
            else 
            {
               LOGGER.severe("Invalid SteelGauge180 Widget Definition File.  Unknown Tag: " + node.getNodeName());
               return null;                
            }
        }
        return sg;
    }
   
}
