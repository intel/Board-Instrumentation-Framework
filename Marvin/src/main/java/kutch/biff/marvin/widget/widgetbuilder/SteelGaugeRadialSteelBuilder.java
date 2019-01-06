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

import eu.hansolo.enzo.gauge.RadialGauge.TickLabelOrientation;
import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.SteelGaugeRadialSteelWidget;

/**
 *
 * @author Patrick Kutch
 */
public class SteelGaugeRadialSteelBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    @SuppressWarnings(
    {
        "BroadCatchBlock", "TooBroadCatch"
    })
    public static SteelGaugeRadialSteelWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        SteelGaugeRadialSteelWidget objWidget = new SteelGaugeRadialSteelWidget();
        
        for (FrameworkNode node :masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(objWidget, node))
            {
                continue;
            }
            else if (node.getNodeName().equalsIgnoreCase("MinValue"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setMinValue(Double.parseDouble(str));
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
                    objWidget.setMaxValue(Double.parseDouble(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid MaxValue in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Decimals"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setDecimalPlaces(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid Decimals in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("DialStartAngle"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setDialStartAngle(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid DialStartAngle in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("DialRangeAngle"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setRangeAngle(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid DialRangeAngle in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("TickCount"))
            {
                double MajorTickVal = -1234;
                double MinorTickVal = -1234;
                
                if (node.hasAttribute("Major"))
                {
                    MajorTickVal = node.getDoubleAttribute("Major", MajorTickVal);
                    if (MajorTickVal != -1234)
                    {
                        objWidget.setMajorTickCount(MajorTickVal);
                    }
                    else
                    {
                        LOGGER.severe("Invalid TickCount ->" + node.getAttribute("Major"));
                        return null;
                    }
                }
                if (node.hasAttribute("Minor"))
                {
                    MinorTickVal = node.getDoubleAttribute("Minor", MinorTickVal);
                    if (MinorTickVal != -1234)
                    {
                        objWidget.setMinorTickCount(MinorTickVal);
                    }
                    else
                    {
                        LOGGER.severe("Invalid TickCount:Minor ->" + node.getAttribute("Minor"));
                        return null;
                    }
                }                
            }            
            else if (node.getNodeName().equalsIgnoreCase("MajorTicksSpace"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setMajorTick(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid MajorTicksSpace in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("MinorTicksSpace"))
            {
                String str = node.getTextContent();
                try
                {
                    objWidget.setMinorTick(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid MinorTicksSpace in SteelGauge Widget Definition File");
                    return null;
                }
                
            }
            else if (node.getNodeName().equalsIgnoreCase("TickLableOrientation"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("Horizontal"))
                {
                    objWidget.setOrientation(TickLabelOrientation.HORIZONTAL);
                }
                else if (0==str.compareToIgnoreCase("Orthogonal"))
                {
                    objWidget.setOrientation(TickLabelOrientation.ORTHOGONAL);
                }
                else if (0==str.compareToIgnoreCase("Tangent"))
                {
                    objWidget.setOrientation(TickLabelOrientation.TANGENT);
                }
                else
                {
                    LOGGER.severe("Invalid TickLableOrientation in SteelGauge Widget Definition File. Should be Horizontal, Orthogonal or Tangent");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("EnhancedRateText"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    objWidget.setEnhancedRateText(true);
                }
                else if (0==str.compareToIgnoreCase("False"))
                {
                    objWidget.setEnhancedRateText(false);
                }
                else
                {
                    LOGGER.severe("Invalid EnhancedRateText in SteelGauge Widget Definition File.  Should be true or false");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("UnitText"))
            {
                String str = node.getTextContent();
                objWidget.setUnitText(str);
            }
            else 
            {
               LOGGER.severe("Invalid SteelGauge Widget Definition File.  Unknown Tag: " + node.getNodeName());
               return null;                
            }
        }
        return objWidget;
    }
}