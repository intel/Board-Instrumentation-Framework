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

import eu.hansolo.enzo.led.Led.LedType;
import java.util.logging.Logger;
import javafx.geometry.Orientation;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.SteelLedBarGraphWidget;

/**
 *
 * @author Patrick Kutch
 */
public class SteelLedBarGraphBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static SteelLedBarGraphWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        SteelLedBarGraphWidget led = new SteelLedBarGraphWidget();
        
        for (FrameworkNode node :masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(led, node))
            {
                continue;
            }
         
            else if (node.getNodeName().equalsIgnoreCase("NumberOfLeds"))
            {
                String str = node.getTextContent();
                try
                {
                    led.setNumberOfLeds(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid NumberOfLeds in LedBarGraph Widget Definition File : " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("SizeOfLeds"))
            {
                String str = node.getTextContent();
                try
                {
                    led.setLedSize(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid SizeOfLeds in LedBarGraph Widget Definition File : " + str);
                    return null;
                }
            }            
            
            else if (node.getNodeName().equalsIgnoreCase("Orientation"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("Horizontal"))
                {
                    led.setOrientation(Orientation.HORIZONTAL);
                }
                else if (0==str.compareToIgnoreCase("Vertical"))
                {
                    led.setOrientation(Orientation.VERTICAL);
                }
                else
                {
                    LOGGER.severe("Invalid Orientation in LedBarGraph Widget Definition File. Should be Horizontal or Vertical, not : " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("LedType"))
            {
                String str = node.getTextContent();
                
                if (0 == str.compareToIgnoreCase("Horizontal"))
                {
                    led.setLedType(LedType.HORIZONTAL);
                }
                else if (0==str.compareToIgnoreCase("Vertical"))
                {
                    led.setLedType(LedType.VERTICAL);
                }
                else if (0==str.compareToIgnoreCase("Round"))
                {
                    led.setLedType(LedType.ROUND);
                }
                else if (0==str.compareToIgnoreCase("Square"))
                {
                    led.setLedType(LedType.SQUARE);
                }
                else
                {
                    LOGGER.severe("Invalid Orientation in LedBarGraph Widget Definition File. Should be Horizontal or Vertical, not : " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ShowPeakValue"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    led.setShowPeakValue(true);
                }
                else if (0==str.compareToIgnoreCase("False"))
                {
                    led.setShowPeakValue(false);
                }
                else
                {
                    LOGGER.severe("Invalid ShowPeakValue in LedBarGraph  Definition File.  Should be true or false, not " + str);
                    return null;
                }
            }
            else 
            {
               LOGGER.severe("Invalid LedBarGraph Widget Definition File.  Unknown Tag: " + node.getNodeName());
               return null;                
            }
        }
        return led;
    }
}
