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

import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.Gauge.TickLabelOrientation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.util.Pair;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.SteelGaugeWidget;

/**
 *
 * @author Patrick Kutch
 */
public class SteelGaugeBuilder
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static SteelGaugeWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        SteelGaugeWidget sg = new SteelGaugeWidget();

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
                    LOGGER.severe("Invalid MinValue [" + str + "] in Widget Definition File: " + widgetDefFilename);
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
                    LOGGER.severe("Invalid MaxValue [" + str + "] in SteelGauge Widget Definition File:" + widgetDefFilename);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Decimals"))
            {
                String str = node.getTextContent();
                try
                {
                    sg.setDecimalPlaces(Integer.parseInt(str));
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
                    sg.setDialStartAngle(Integer.parseInt(str));
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
                    sg.setRangeAngle(Integer.parseInt(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid DialRangeAngle in SteelGauge Widget Definition File");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("MajorTicksSpace"))
            {
                String str = node.getTextContent();
                try
                {
                    sg.setMajorTick(Integer.parseInt(str));
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
                    sg.setMinorTick(Integer.parseInt(str));
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
                    sg.setOrientation(TickLabelOrientation.HORIZONTAL);
                }
                else if (0 == str.compareToIgnoreCase("Orthogonal"))
                {
                    sg.setOrientation(TickLabelOrientation.ORTHOGONAL);
                }
                else if (0 == str.compareToIgnoreCase("Tangent"))
                {
                    sg.setOrientation(TickLabelOrientation.TANGENT);
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
                    sg.setEnhancedRateText(true);
                }
                else if (0 == str.compareToIgnoreCase("False"))
                {
                    sg.setEnhancedRateText(false);
                }
                else
                {
                    LOGGER.severe("Invalid EnhancedRateText in SteelGauge Widget Definition File.  Should be true or false");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Shadowed"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    sg.setShadowed(true);
                }
                else if (0 == str.compareToIgnoreCase("False"))
                {
                    sg.setShadowed(false);
                }
                else
                {
                    LOGGER.severe("Invalid Shadowed in SteelGauge Widget Definition File.  Should be true or false");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("UnitText"))
            {
                String str = node.getTextContent();
                sg.setUnitText(str);
            }
            else if (node.getNodeName().equalsIgnoreCase("ShowMaxMeasuredValue"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    sg.setShowMeasuredMax(true);
                }
                else if (0 == str.compareToIgnoreCase("False"))
                {
                    sg.setShowMeasuredMax(false);
                }
                else
                {
                    LOGGER.severe("Invalid ShowMaxMeasuredValue in SteelGauge Widget Definition File.  Should be true or false");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ShowMinMeasuredValue"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    sg.setShowMeasuredMin(true);
                }
                else if (0 == str.compareToIgnoreCase("False"))
                {
                    sg.setShowMeasuredMin(false);
                }
                else
                {
                    LOGGER.severe("Invalid ShowMinMeasuredValue in SteelGauge Widget Definition File.  Should be true or false");
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Sections"))
            {
                List<Pair<Double, Double>> percentSections = ProcessPercentageSections(node);
                if (null != percentSections)
                {
                    sg.setPercentageSections(percentSections);
                }
                else
                {
                    List<Section> sectList = ProcessSections(node);
                    if (null != sectList)
                    {
                        sg.setSections(sectList);
                    }
                    else
                    {
                        return null;
                    }
                }
            }
            else
            {
                LOGGER.severe("Invalid SteelGauge Widget Definition File.  Unknown Tag: " + node.getNodeName());
                return null;
            }
        }
        return sg;
    }

    protected static List<Pair<Double, Double>> ProcessPercentageSections(FrameworkNode sections)
    {
        ArrayList<Pair<Double, Double>> sectList = new ArrayList<>();

        for (FrameworkNode node : sections.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text"))
            {

            }
            else if (node.getNodeName().equalsIgnoreCase("Section"))
            {
                Utility.ValidateAttributes(new String[]
                {
                    "start", "end"
                }, node);
                if (node.hasAttribute("start") && node.hasAttribute("end"))
                {
                    try
                    {
                        double start, end;
                        String str = node.getAttribute("start");
                        if (str.contains("%"))
                        {
                            str = str.replace("%", "");
                        }
                        else
                        {
                            return null;
                        }
                        start = Double.parseDouble(str);
                        str = node.getAttribute("end");
                        if (str.contains("%"))
                        {
                            str = str.replace("%", "");
                        }
                        else
                        {
                            return null;
                        }
                        end = Double.parseDouble(str);
                        sectList.add(new Pair<Double, Double>(start, end));
                    }
                    catch (Exception ex)
                    {
                        //LOGGER.severe("Invalid <Sections> in SteelGauge Widget Definition File.");
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
        return sectList;

    }

    protected static List<Section> ProcessSections(FrameworkNode sections)
    {
        ArrayList<Section> sectList = new ArrayList<>();

        for (FrameworkNode node : sections.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text"))
            {

            }
            else if (node.getNodeName().equalsIgnoreCase("Section"))
            {
                Utility.ValidateAttributes(new String[]
                {
                    "start", "end"
                }, node);
                if (node.hasAttribute("start") && node.hasAttribute("end"))
                {
                    Section objSect = new Section();
                    try
                    {
                        String str = node.getAttribute("start");
                        objSect.setStart(Double.parseDouble(str));
                        str = node.getAttribute("end");
                        objSect.setStop(Double.parseDouble(str));
                        sectList.add(objSect);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.severe("Invalid <Sections> in SteelGauge Widget Definition File.");
                        return null;
                    }
                }
                else
                {
                    LOGGER.severe("Invalid <Sections> in SteelGauge Widget Definition File.");
                    return null;
                }
            }
            else
            {
                LOGGER.severe("Invalid <Sections> in SteelGauge Widget Definition File. Unknown tag: " + node.getNodeName());
                return null;
            }
        }
        return sectList;
    }
}
