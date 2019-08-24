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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import eu.hansolo.enzo.common.Section;
import javafx.util.Pair;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.SteelSimpleGaugeWidget;

/**
 *
 * @author Patrick Kutch
 */
public class SteelSimpleGaugeBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static SteelSimpleGaugeWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        SteelSimpleGaugeWidget sg = new SteelSimpleGaugeWidget();
        
        for (FrameworkNode node :masterNode.getChildNodes())
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
                    LOGGER.severe("Invalid MaxValue in SteelSimpleGauge Widget Definition File");
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
                    LOGGER.severe("Invalid Decimals in SteelSimpleGauge Widget Definition File");
                    return null;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("UnitText"))
            {
                String str = node.getTextContent();
                sg.setUnitText(str);
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
               LOGGER.severe("Invalid SteelSimpleGauge Widget Definition File.  Unknown Tag: " + node.getNodeName());
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
    private static ArrayList<Section> ProcessSections(FrameworkNode sections)
    {
        ArrayList<Section> sectList = new ArrayList<Section>();
        for (FrameworkNode node :sections.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text"))
            {
                continue;
            }
            else if (node.getNodeName().equalsIgnoreCase("Section"))
            {
                Utility.ValidateAttributes(new String[] {"start","end"},node);      
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
                        LOGGER.severe("Invalid <Sections> in SteelSimpleGauge Widget Definition File.");
                        return null;
                    }
                }
                else
                {
                    LOGGER.severe("Invalid <Sections> in SteelSimpleGauge Widget Definition File.");
                    return null;
                }
            }
            else
            {
                LOGGER.severe("Invalid <Sections> in SteelSimpleGauge Widget Definition File. Unknown tag: " + node.getNodeName());
                return null;
            }
        }
        return sectList;
    }
    
}
