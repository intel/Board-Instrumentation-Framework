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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.splash.MySplash;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.GridMacroMgr;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.DynamicGridWidget;
import kutch.biff.marvin.widget.GridWidget;
import kutch.biff.marvin.widget.OnDemandGridWidget;
import kutch.biff.marvin.widget.Widget;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Patrick Kutch
 */
public class WidgetBuilder
{

    private final static Configuration CONFIG = Configuration.getConfig();
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final static MySplash SPLASH = MySplash.getSplash();
    private static int FileDepth = 1;
    private static String FileTree = "";

    public static Widget Build(FrameworkNode node)
    {
        if (node.getNodeName().equalsIgnoreCase("Widget"))
        {
            return BuildWidget(node);
        }
        else if (node.getNodeName().equalsIgnoreCase("Grid"))
        {
            Widget objGrid = BuildGrid(node, false);
            if (node.hasAttribute("OnDemandTask") && null != objGrid)
            {
                ((GridWidget) (objGrid)).setOnDemandTask(node.getAttribute("OnDemandTask"));
            }

            return objGrid;
        }
        else if (node.getNodeName().equalsIgnoreCase("ListGrid"))
        {
            Widget objGrid = BuildGrid(node, false);
            if (node.hasAttribute("OnDemandTask") && null != objGrid)
            {
                ((GridWidget) (objGrid)).setOnDemandTask(node.getAttribute("OnDemandTask"));
            }

            return objGrid;
        }
        else if (node.getNodeName().equalsIgnoreCase("DynamicGrid"))
        {
            Widget objDynaGrid = BuildDynamicGrid(node);
            if (node.hasAttribute("OnDemandTask") && null != objDynaGrid)
            {
                ((GridWidget) (objDynaGrid)).setOnDemandTask(node.getAttribute("OnDemandTask"));
            }

            return objDynaGrid;
        }

        LOGGER.severe("Unknown Widget type: " + node.getNodeName());
        return null;
    }

    private static Widget BuildWidget(FrameworkNode widgetNode)
    {
        BaseWidget retWidget = null;
        String rowSpan = "1";
        String colSpan = "1";

        if (false == widgetNode.hasAttribute("File"))
        {
            LOGGER.severe("Widget with no file definition");
            return null;
        }

        if (false == widgetNode.hasAttribute("row"))
        {
            LOGGER.severe("Widget with no row");
            return null;
        }

        if (false == widgetNode.hasAttribute("column"))
        {
            LOGGER.severe("Widget with no column");
            return null;
        }

        if (widgetNode.hasAttribute("rowSpan"))
        {
            rowSpan = widgetNode.getAttribute("rowSpan");
        }
        if (widgetNode.hasAttribute("colSpan"))
        {
            colSpan = widgetNode.getAttribute("colSpan");
        }
        if (widgetNode.hasAttribute("columnSpan"))
        {
            colSpan = widgetNode.getAttribute("columnspan");
        }

        String fileName = widgetNode.getAttribute("File");
        fileName = BaseWidget.convertToFileOSSpecific(fileName);
        String fileNameWPath;
        File fCheck = new File(fileName);

        if (null == fCheck)
        {
            return null;
        }

        if (fCheck.exists())
        {
            fileNameWPath = fileName;  //fully qualified path provided (likely custom widget)
        }
        else
        {
            fileNameWPath = BaseWidget.DefaultWidgetDirectory + File.separatorChar + fileName; // find widget in widget dir
        }

        String strRow = widgetNode.getAttribute("row");
        String strColumn = widgetNode.getAttribute("column");

        retWidget = ParseWidgetDefinitionFile(fileNameWPath);
        if (null == retWidget)
        {
            return null;
        }
        Utility.ValidateAttributes(new String[]
        {
            "File", "row", "column", "rowSpan", "colSpan", "columnSpan", "Align", "Height", "Width", "Task", "Enabled","ToggleTask"
        }, retWidget.GetCustomAttributes(), widgetNode);

        retWidget.HandleWidgetSpecificAttributes(widgetNode);

        if (true == widgetNode.hasAttribute("Enabled"))
        {
            String str = widgetNode.getAttribute("Enabled");
            boolean Enabled = true;
            if (str.equalsIgnoreCase("True"))
            {
                Enabled = true;
            }
            else if (str.equalsIgnoreCase("False"))
            {
                Enabled = false;
            }
            else
            {
                LOGGER.severe("Invalid option for Enabled attribute for widget, only True or False is valid, not: " + str);
                return null;
            }

            if (retWidget.SupportsEnableDisable())
            {
                retWidget.setInitiallyEnabled(Enabled);
            }
            else
            {
                LOGGER.warning("Widget set Enabled attribute, but it is not supported by the widget.  Ignoring");
            }
        }

        if (widgetNode.hasAttribute("Task"))
        {
            retWidget.setTaskID(widgetNode.getAttribute("Task"));
        }

        if (true == widgetNode.hasAttribute("Height"))
        {
            if (!retWidget.parseHeight(widgetNode))
            {
                return null;
            }
        }
        if (true == widgetNode.hasAttribute("Width"))
        {
            String str = widgetNode.getAttribute("Width");
            if (false == retWidget.parseWidth(widgetNode))
            {
                return null;
            }
        }
        // Continue reading widget definition from Application.xml
        try
        {
            retWidget.setRow(Integer.parseInt(strRow));
            AliasMgr.getAliasMgr().UpdateCurrentRow(Integer.parseInt(strRow));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid Widget row: " + strRow + " declaration");
            return null;
        }

        try
        {
            retWidget.setColumn(Integer.parseInt(strColumn));
            AliasMgr.getAliasMgr().UpdateCurrentColumn(Integer.parseInt(strColumn));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid Widget column: " + strColumn + " declaration");
            return null;
        }
        try
        {
            retWidget.setRowSpan(Integer.parseInt(rowSpan));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid Widget rowSpan: " + rowSpan + " declaration");
            return null;
        }
        try
        {
            retWidget.setColumnSpan(Integer.parseInt(colSpan));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid Widget colSpan: " + colSpan + " declaration");
            return null;
        }

        if (true == widgetNode.hasAttribute("Align"))
        {
            String str = widgetNode.getAttribute("Align");
            retWidget.setAlignment(str);
        }
//        ArrayList<MyNode> Children = widgetNode.getChildNodes();
        for (FrameworkNode node : widgetNode.getChildNodes(true))
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            

            if (node.getNodeName().equalsIgnoreCase("Title"))
            {
                retWidget.setTitle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("Decimals"))
            {
                try
                {
                    retWidget.setDecimalPlaces(node.getIntegerContent());
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invalid Decimals option: " + node.getTextContent() +". Ignoring" );
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("StyleOverride"))
            {
                HandleStyleOverride(retWidget, node);
            }
            else if (node.getNodeName().equalsIgnoreCase("UnitsOverride"))
            {
                retWidget.setUnitsOverride(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("InitialValue"))
            {
                retWidget.SetInitialValue(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("ValueRange"))
            {
                if (false == retWidget.HandleValueRange(node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ToolTip"))
            {
                if (false == retWidget.HandleToolTipConfig(node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("SelectedStyle"))
            {
                if (false == retWidget.HandleSelectionConfig(node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent"))
            {
                retWidget.SetClickThroughTransparentRegion(node.getBooleanValue());
            }

            else if (node.getNodeName().equalsIgnoreCase("MinionSrc"))
            {
                if (node.hasAttributes())
                {
                    Utility.ValidateAttributes(new String[]
                    {
                        "Namespace", "ID"
                    }, node);
                    if (node.hasAttribute("ID"))
                    {
                        retWidget.setMinionID(node.getAttribute("ID"));
                    }
                    else
                    {
                        LOGGER.severe("Malformed Widget MinionSrc Definition - ID not found");
                        return null;
                    }
                    if (node.hasAttribute("ID") && node.hasAttribute("Namespace"))
                    {
                        retWidget.setNamespace(node.getAttribute("Namespace"));
                    }
                    else
                    {
                        LOGGER.severe("Malformed Widget MinionSrc Definition - Namespace not found");
                        return null;
                    }
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Peekaboo"))
            {
                if (!HandlePeekaboo(retWidget, node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Size"))
            {
                if (false == HandleSizeSection(node, retWidget))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("MaxSteppedRange"))
            {
                if (!retWidget.HandleMaxSteppedRange(node))
                {
                    return null;
                }

            }
            else if (node.getNodeName().equalsIgnoreCase("MinSteppedRange"))
            {
                if (!retWidget.HandleMinSteppedRange(node))
                {
                    return null;
                }
            }
            else if (false == retWidget.HandleWidgetSpecificSettings(node))
            {
                LOGGER.severe("Unknown/Invalid tag in Widget Definition portion of Application.xml : " + node.getNodeName());
                return null;
            }
        }

        return retWidget;
    }

    public static boolean HandlePeekaboo(BaseWidget widget, FrameworkNode widgetNode)
    {
        if (widgetNode.getNodeName().equalsIgnoreCase("Peekaboo"))
        {
            if (widgetNode.hasAttributes())
            {
                Utility.ValidateAttributes(new String[]
                {
                    "Namespace", "ID", "Hide", "Show", "Default"
                }, widgetNode);
                if (widgetNode.hasAttribute("ID") && widgetNode.hasAttribute("Namespace"))
                {
                    widget.addPeekaboo(widgetNode.getAttribute("Namespace"), widgetNode.getAttribute("ID"));
                }
                else
                {
                    LOGGER.severe("Malformed Widget Peekaboo Definition in Application.XML");
                    return false;
                }
                if (widgetNode.hasAttribute("Hide"))
                {
                    widget.setPeekabooHideStr(widgetNode.getAttribute("Hide"));
                }
                if (widgetNode.hasAttribute("Show"))
                {
                    widget.setPeekabooShowStr(widgetNode.getAttribute("Show"));
                }
                if (widgetNode.hasAttribute("Default"))
                {
                    String strDef = widgetNode.getAttribute("Default");
                    if (0 == strDef.compareToIgnoreCase("Show") || 0 == strDef.compareToIgnoreCase(widget.getPeekabooShowStr()))
                    {
                        widget.setPeekabooShowDefault(true);
                    }
                    else if (0 == strDef.compareToIgnoreCase("Hide") || 0 == strDef.compareToIgnoreCase(widget.getPeekabooHideStr()))
                    {
                        widget.setPeekabooShowDefault(false);
                    }
                    else
                    {
                        String validOpts[] =
                        {
                            "Pause", "Resume", "Remove", "Insert", "Disable", "Enable", "Select", "Deselected"
                        };
                        boolean found = false;

                        for (String opt : validOpts)
                        {
                            if (opt.equalsIgnoreCase(strDef))
                            {
                                found = true;
                                widget.SetDefaultPeekabooAction(strDef);
                                break;
                            }
                        }
                        if (!found)
                        {
                            if (!strDef.substring(0, "Marvin:".length()).equalsIgnoreCase("Marvin:"))
                            {
                                LOGGER.severe("Malformed Widget Peekaboo Definition in Application.XML.  Default is unknown : " + strDef);
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static boolean HandleStyleOverride(BaseWidget widget, FrameworkNode styleNode)
    {
        Utility.ValidateAttributes(new String[]
        {
            "File", "ID", "ScaleToShape"
        }, styleNode);
        if (styleNode.hasAttribute("File"))
        {
            widget.setBaseCSSFilename(styleNode.getAttribute("File"));
        }
        if (styleNode.hasAttribute("ID"))
        {
            widget.setStyleID(styleNode.getAttribute("ID"));

        }
        widget.HandleCustomStyleOverride(styleNode);

        for (FrameworkNode node : styleNode.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("Item"))
            {
                widget.AddAdditionalStyleOverride(node.getTextContent());
            }
            else
            {
                LOGGER.severe("Unknown Tag under <StyleOverride>: " + node.getNodeName());
                return false;
            }
        }
        return true;
    }

    private static boolean HandleSizeSection(FrameworkNode sizeNode, BaseWidget retWidget)
    {
        LOGGER.warning("The <Size> Tag has been deprecated - and will be removed in future release");
        return true;

//        for (FrameworkNode node : sizeNode.getChildNodes())
//        {
//            if (node.getNodeType() == Node.ELEMENT_NODE)
//            {
//                int iVal;
//                if (node.getNodeName().equalsIgnoreCase("Minimum"))
//                {
//                    iVal = GetIntAttribute(node, "Width", false);
//                    if (iVal != -1)
//                    {
//                        retWidget.setMinWidth(iVal);
//                    }
//                    else if (iVal == -2)
//                    {
//                        return false;
//                    }
//
//                    iVal = GetIntAttribute(node, "Height", false);
//                    if (iVal != -1)
//                    {
//                        retWidget.setMinHeight(iVal);
//                    }
//                    else if (iVal == -2)
//                    {
//                        return false;
//                    }
//                }
//                else if (node.getNodeName().equalsIgnoreCase("Maximum"))
//                {
//                    iVal = GetIntAttribute(node, "Width", false);
//                    if (iVal != -1)
//                    {
//                        retWidget.setMaxWidth(iVal);
//                    }
//                    else if (iVal == -2)
//                    {
//                        return false;
//                    }
//                    iVal = GetIntAttribute(node, "Height", false);
//                    if (iVal != -1)
//                    {
//                        retWidget.setMaxHeight(iVal);
//                    }
//                    else if (iVal == -2)
//                    {
//                        return false;
//                    }
//                }
//                else
//                {
//                    LOGGER.severe("Unknown tag in <Size> section Widget Definition portion of Application.xml : " + node.getNodeName());
//                    return false;
//                }
//            }
//        }
//
//        return true;
    }

    private static int GetIntAttribute(FrameworkNode node, String Attribute, boolean fRequired)
    {
        if (node.hasAttribute(Attribute))
        {
            try
            {
                return Integer.parseInt(node.getAttribute(Attribute));
            }
            catch (Exception ex)
            {
                LOGGER.severe("Invalid attribute for Widget in Application.xml: " + node.getNodeName());
                return -1;
            }
        }
        if (true == fRequired)
        {
            LOGGER.severe("Missing attribute [" + Attribute + "] Widget in Application.xml: " + node.getNodeName());
            return -1;
        }
        return -2;
    }

    private static BaseWidget ParseWidgetDefinitionFile(String Filename)
    {
        BaseWidget retWidget = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try
        {
            db = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }

        File file = new File(Filename);
        if (file.exists())
        {
            Document docWidget;
            try
            {
                docWidget = db.parse(file);
            }
            catch (SAXException | IOException ex)
            {
                LOGGER.severe(ex.toString());
                return null;
            }
            AliasMgr.getAliasMgr().PushAliasList(true);
            if (!AliasMgr.ReadAliasFromRootDocument(docWidget)) // let's see if there are any aliases in the widget file!
            {
                return null;
            }

            FrameworkNode baseNode = new FrameworkNode(docWidget.getElementsByTagName("Widget").item(0));

            if (false == baseNode.hasAttributes())
            {
                LOGGER.severe("Invalid Widget definition in " + Filename);
                return null;
            }

            String strWidget = baseNode.getAttribute("Type");
            if (null == strWidget)
            {
                LOGGER.severe("Invalid Widget definition in " + Filename);
                return null;
            }

            if (strWidget.equalsIgnoreCase("SteelGauge"))
            {
                retWidget = SteelGaugeBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("SteelGaugeRadial"))
            {
                retWidget = SteelGaugeRadialBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("SteelGaugeRadialSteel"))
            {
                retWidget = SteelGaugeRadialSteelBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("Steel180Gauge"))
            {
                retWidget = SteelGauge180Builder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("SteelSimpleGauge"))
            {
                retWidget = SteelSimpleGaugeBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("SteelLCD"))
            {
                retWidget = SteelLCDWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("LedBargraph"))
            {
                retWidget = SteelLedBarGraphBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("StaticImage"))
            {
                retWidget = StaticImageBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("DynamicImage"))
            {
                retWidget = DynamicImageBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("Text"))
            {
                retWidget = TextBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("ProgressBar"))
            {
                retWidget = ProgressBarBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("ProgressIndicator"))
            {
                retWidget = ProgressIndicatorBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("Button"))
            {
                retWidget = ButtonWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("MenuButton"))
            {
                retWidget = ButtonWidgetBuilder.BuildMenuButton(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("ToggleButton"))
            {
                retWidget = ButtonWidgetBuilder.BuildToggleButton(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("MultiSourceLineChart"))
            {
                retWidget = ChartWidgetBuilder.BuildMultiSourceLineChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("LineChart"))
            {
                retWidget = ChartWidgetBuilder.BuildLineChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("AreaChart"))
            {
                retWidget = ChartWidgetBuilder.BuildAreaChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("MultiSourceAreaChart"))
            {
                retWidget = ChartWidgetBuilder.BuildMultiSourceAreaChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("MultiSourceStackedAreaChart"))
            {
                retWidget = ChartWidgetBuilder.BuildMultiSourceStackedAreaChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("StackedAreaChart"))
            {
                retWidget = ChartWidgetBuilder.BuildStackedAreaChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("PieChart"))
            {
                retWidget = PieChartWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("BarChart"))
            {
                retWidget = ChartWidgetBuilder.BuildBarChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("StackedBarChart"))
            {
                retWidget = ChartWidgetBuilder.BuildStackedlBarChart(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("HorizontalBarChart"))
            {
                retWidget = ChartWidgetBuilder.BuildHorizontalBarChart(baseNode, Filename);
            }

            else if (strWidget.equalsIgnoreCase("FlipPanel"))
            {
                retWidget = FlipPanelWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("FileWriter"))
            {
                retWidget = FileWriterWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("Spacer"))
            {
                retWidget = SpacerWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("SVG"))
            {
                retWidget = SVG_WidgetBuilder.Build(baseNode, Filename);
            }

            else if (strWidget.equalsIgnoreCase("PDF_Reader"))
            {
                LOGGER.severe("PDF Reader not currently supported");
                retWidget = null;
                //retWidget = PDF_ReaderWidgetBuilder.Build(baseNode, Filename);
            }

            else if (strWidget.equalsIgnoreCase("AudioPlayer"))
            {
                retWidget = AudioPlayerWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("VideoPlayer"))
            {
                retWidget = VideoPlayerWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("Web"))
            {
                retWidget = WebWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("QuickView"))
            {
                retWidget = QuickViewWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("QuickViewLCD"))
            {
                retWidget = QuickViewLCDWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("DoubleBarGauge"))
            {
                retWidget = DoubleBarGaugeWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("BarGauge"))
            {
                retWidget = BarGaugeWidgetBuilder.Build(baseNode, Filename);
            }
            else if (strWidget.equalsIgnoreCase("ListBoxText"))
            {
                retWidget = TextBuilder.ListBoxText_Build(baseNode, Filename);
            }

            else
            {
                LOGGER.severe("Unknown Widget type : " + strWidget + " in :" + Filename);
            }
            if (null != retWidget)
            {
                retWidget.setWidgetInformation(file.getParent(), Filename, strWidget);
            }
        }
        else
        {
            LOGGER.severe("Widget Definition file does not exist: " + Filename);
            return null;
        }

        AliasMgr.getAliasMgr().PopAliasList();
        return retWidget;
    }

    public static boolean ReadGridAttributes(GridWidget objGridWidget, FrameworkNode gridNode, boolean isFlipPanelGrid)
    {
        String WhatIsIt = "Grid";
        if (true == isFlipPanelGrid)
        {
            WhatIsIt = "FlipPanel";
        }
        if (gridNode.hasAttribute("rowSpan"))
        {
            objGridWidget.setRowSpan(gridNode.getIntegerAttribute("rowspan", 1));
        }
        if (gridNode.hasAttribute("colSpan"))
        {
            objGridWidget.setColumnSpan(gridNode.getIntegerAttribute("colspan", 1));
        }
        else if (gridNode.hasAttribute("columnSpan"))
        {
            objGridWidget.setColumnSpan(gridNode.getIntegerAttribute("columnspan", 1));
        }
        if (true == gridNode.hasAttribute("Height"))
        {
            if (!objGridWidget.parseHeight(gridNode))
            {
                return false;
            }
        }
        if (true == gridNode.hasAttribute("Width"))
        {
            if (!objGridWidget.parseWidth(gridNode))
            {
                return false;
            }
        }

        if (gridNode.hasAttribute("hgap"))
        {
            if (objGridWidget.parsehGapValue(gridNode))
            {
                LOGGER.config("Setting hGap for DynamicGrid :" + gridNode.getAttribute("hgap"));
            }
            else
            {
                LOGGER.warning("hgap for DynamicGrid  invalid: " + gridNode.getAttribute("hgap") + ".  Ignoring");
                return false;
            }
        }
        if (gridNode.hasAttribute("vgap"))
        {
            if (objGridWidget.parsevGapValue(gridNode))
            {
                LOGGER.config("Setting vGap for Grid :" + gridNode.getAttribute("vgap"));
            }
            else
            {
                LOGGER.warning("vgap for Grid invalid: " + gridNode.getAttribute("vgap") + ".  Ignoring");
                return false;
            }
        }

        if (true == gridNode.hasAttribute("Align"))
        {
            String str = gridNode.getAttribute("Align");
            if (false == objGridWidget.setAlignment(str))
            {
                return false;
            }
        }

        if (gridNode.hasAttribute("Task"))
        {
            objGridWidget.setTaskID(gridNode.getAttribute("Task"));
        }
        return true;
    }

    public static GridWidget BuildGrid(FrameworkNode gridNode, boolean isFlipPanelGrid)
    {
        GridWidget retWidget = new GridWidget();
        String rowSpan = "1";
        String colSpan = "1";
        String strRow = "0";
        String strColumn = "0";
        String WhatIsIt = "Grid";
        boolean isOnDemand = false;
        DynamicItemInfoContainer info = null;

        if (true == isFlipPanelGrid)
        {
            WhatIsIt = "FlipPanel";
        }

        if (false == gridNode.hasAttribute("row") && false == isFlipPanelGrid)
        {
            LOGGER.severe("Grid with no row");
            return null;
        }

        if (false == gridNode.hasAttribute("column") && false == isFlipPanelGrid)
        {
            LOGGER.severe("Grid with no column");
            return null;
        }
        if (gridNode.hasAttribute("rowSpan"))
        {
            rowSpan = gridNode.getAttribute("rowSpan");
        }
        if (gridNode.hasAttribute("colSpan"))
        {
            colSpan = gridNode.getAttribute("colSpan");
        }
        if (gridNode.hasAttribute("columnspan"))
        {
            colSpan = gridNode.getAttribute("columnspan");
        }

        if (gridNode.hasChild("OnDemand"))
        {
            FrameworkNode demandNode = gridNode.getChild("OnDemand");
            info = ConfigurationReader.ReadOnDemandInfo(demandNode);
            OnDemandGridWidget objWidget = new OnDemandGridWidget(info);
            if (demandNode.hasChild("Growth"))
            {
                objWidget.ReadGrowthInfo(demandNode.getChild("Growth"));
            }

            // get hgap and all those good things
            ReadGridAttributes(objWidget, gridNode, false);
            retWidget = objWidget;
            WhatIsIt = "OnDemand Grid";
            isOnDemand = true;
            gridNode.DeleteChildNodes("OnDemand"); // delete the ondemand section, not needed anymore

//            int row = gridNode.getIntegerAttribute("row", 0);
//            int col = gridNode.getIntegerAttribute("column", 0);
//            gridNode.AddAttibute("GRID_COLUMN_ODD", col % 2 == 0 ? "FALSE" : "TRUE");
//            gridNode.AddAttibute("GRID_ROW_ODD", row % 2 == 0 ? "FALSE" : "TRUE");
            info.setNode(gridNode);
        }

        if (true == gridNode.hasAttribute("Height"))
        {
            if (!retWidget.parseHeight(gridNode))
            {
                return null;
            }
        }
        if (true == gridNode.hasAttribute("Width"))
        {
            if (!retWidget.parseWidth(gridNode))
            {
                return null;
            }
        }

        if (false == isFlipPanelGrid)
        {
            strRow = gridNode.getAttribute("row");
            strColumn = gridNode.getAttribute("column");
            if (strRow == null)
            {
                LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. no row defined");
                return null;
            }
            if (strColumn == null)
            {
                LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. No column defined");
                return null;
            }
        }

        try
        {
            retWidget.setRow(Integer.parseInt(strRow));
            retWidget.setColumn(Integer.parseInt(strColumn));
            retWidget.setRowSpan(Integer.parseInt(rowSpan));
            retWidget.setColumnSpan(Integer.parseInt(colSpan));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. " + ex.toString());
            return null;
        }
        AliasMgr.getAliasMgr().UpdateCurrentColumn(Integer.parseInt(strColumn));
        AliasMgr.getAliasMgr().UpdateCurrentRow(Integer.parseInt(strRow));

        if (isOnDemand)
        {
            // need to nuke these, otherwise the on-demand grid will suck them up, and that will be a problem.
            gridNode.DeleteAttribute("rowspan");
            gridNode.DeleteAttribute("colspan");
            gridNode.DeleteAttribute("columnpan");
            gridNode.DeleteAttribute("hgap");
            gridNode.DeleteAttribute("vgap");
            gridNode.DeleteAttribute("align");
            gridNode.DeleteAttribute("height");
            gridNode.DeleteAttribute("width");
            AliasMgr.getAliasMgr().PushAliasList(false);
            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(gridNode, new String[]
                                                    {
                                                        "row", "column", "rowSpan", "colSpan", "columnSpan", "hgap", "vgap", "Align", "File", "Height", "Width"
            });
            info.TakeAliasSnapshot(); // have to do this way down here, after the other stuff
            AliasMgr.getAliasMgr().PopAliasList();
            return retWidget;
        }
        AliasMgr.getAliasMgr().AddUpdateAlias("GRID_ROW_ODD", AliasMgr.getAliasMgr().GetAlias("CurrentRowIsOddAlias"));
        AliasMgr.getAliasMgr().AddUpdateAlias("GRID_COLUMN_ODD", AliasMgr.getAliasMgr().GetAlias("CurrentColumnIsOddAlias"));

        AliasMgr.getAliasMgr().PushAliasList(true);

        if (true == gridNode.hasAttribute("File"))
        {
            String strFileName = gridNode.getAttribute("File");
            StartReadingExternalFile(gridNode);
            AliasMgr.getAliasMgr().PushAliasList(true);
            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(gridNode, new String[]
                                                    {
                                                        "row", "column", "rowSpan", "colSpan", "columnSpan", "hgap", "vgap", "Align", "File", "Height", "Width"
            });
            if (false == AliasMgr.ReadAliasFromExternalFile(strFileName))
            {
                AliasMgr.getAliasMgr().PopAliasList();
                return null;
            }
            FrameworkNode GridNode = WidgetBuilder.OpenDefinitionFile(gridNode.getAttribute("File"), "Grid");
            if (null == GridNode)
            {
                LOGGER.severe("Invalid file: " + strFileName + " no <Grid> found.");
                return null;
            }
            retWidget = ReadGridInfo(GridNode, retWidget, strFileName); // read grid from external file
            if (null == retWidget)
            {
                return null;
            }
            if (!ConfigurationReader.ReadTasksFromExternalFile(strFileName)) // could also be tasks defined in external file
            {
                return null;
            }
            AliasMgr.getAliasMgr().PopAliasList();
            DoneReadingExternalFile();
        }

        if (gridNode.hasAttribute("Macro"))
        {
            if (gridNode.hasAttribute("File"))
            {
                LOGGER.severe("Grid cannot have both file and Macro.");
                return null;
            }
            String strMacro = gridNode.getAttribute("Macro");
            FrameworkNode nodeMacro = GridMacroMgr.getGridMacroMgr().getGridMacro(strMacro);
            if (null == nodeMacro)
            {
                LOGGER.severe("Grid Macro specified [" + strMacro + "] does not defined.");
                return null;
            }
            // need to get alias from the grid macro is in
            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(gridNode, new String[]
                                                    {
                                                        "rowSpan", "colSpan", "columnSpan", "hgap", "vgap", "Align", "Height", "Width"
            });

            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(nodeMacro, new String[]
                                                    {
                                                        "rowSpan", "colSpan", "columnSpan", "hgap", "vgap", "Align", "Height", "Width"
            });
            retWidget = ReadGridInfo(nodeMacro, retWidget, null);
            if (null == retWidget)
            {
                return null;
            }
        }
        // go read the grid contents - note that this could be a continuation from stuff already read in via file
        // so you can define most of grid in external file, but keep adding
        retWidget = ReadGridInfo(gridNode, retWidget, "");

        AliasMgr.getAliasMgr().PopAliasList();
        return retWidget;
    }

    public static DynamicGridWidget BuildDynamicGrid(FrameworkNode dynaGridNode)
    {
        DynamicGridWidget retWidget = new DynamicGridWidget();
        String rowSpan = "1";
        String colSpan = "1";
        String strRow = "0";
        String strColumn = "0";
        String WhatIsIt = "DynamicGrid";

        if (false == dynaGridNode.hasAttribute("row"))
        {
            LOGGER.severe("DynamicGrid with no row");
            return null;
        }

        if (dynaGridNode.hasAttribute("Task"))
        {
            retWidget.setTaskID(dynaGridNode.getAttribute("Task"));
        }

        if (false == dynaGridNode.hasAttribute("column"))
        {
            LOGGER.severe("Grid with no column");
            return null;
        }
        if (dynaGridNode.hasAttribute("rowSpan"))
        {
            rowSpan = dynaGridNode.getAttribute("rowSpan");
        }
        if (dynaGridNode.hasAttribute("colSpan"))
        {
            colSpan = dynaGridNode.getAttribute("colSpan");
        }
        if (dynaGridNode.hasAttribute("columnspan"))
        {
            colSpan = dynaGridNode.getAttribute("columnspan");
        }
        if (true == dynaGridNode.hasAttribute("Height"))
        {
            if (!retWidget.parseHeight(dynaGridNode))
            {
                LOGGER.severe("Invalid Height for Grid in Application.xml");
                return null;
            }
        }
        if (true == dynaGridNode.hasAttribute("Width"))
        {
            if (!retWidget.parseWidth(dynaGridNode))
            {
                LOGGER.severe("Invalid Width for Grid in Application.xml");
                return null;
            }
        }

        strRow = dynaGridNode.getAttribute("row");
        strColumn = dynaGridNode.getAttribute("column");
        if (strRow == null)
        {
            LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. no row defined");
            return null;
        }
        if (strColumn == null)
        {
            LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. no row defined");
            return null;
        }

        try
        {
            retWidget.setRow(Integer.parseInt(strRow));
            retWidget.setColumn(Integer.parseInt(strColumn));
            retWidget.setRowSpan(Integer.parseInt(rowSpan));
            retWidget.setColumnSpan(Integer.parseInt(colSpan));
            AliasMgr.getAliasMgr().UpdateCurrentColumn(Integer.parseInt(strColumn));
            AliasMgr.getAliasMgr().UpdateCurrentRow(Integer.parseInt(strRow));
            AliasMgr.getAliasMgr().PushAliasList(true);
        }
        catch (NumberFormatException ex)
        {
            LOGGER.severe("Invalid " + WhatIsIt + " definition in Configuration file. ");
            return null;
        }
        if (dynaGridNode.hasAttribute("hgap"))
        {
            try
            {
                retWidget.sethGap(Integer.parseInt(dynaGridNode.getAttribute("hgap")));
                LOGGER.config("Setting hGap for " + WhatIsIt + " :" + dynaGridNode.getAttribute("hgap"));
            }
            catch (NumberFormatException ex)
            {
                LOGGER.warning("hgap for " + WhatIsIt + "  invalid: " + dynaGridNode.getAttribute("hgap") + ".  Ignoring");
            }
        }
        if (dynaGridNode.hasAttribute("vgap"))
        {
            try
            {
                retWidget.setvGap(Integer.parseInt(dynaGridNode.getAttribute("vgap")));
                LOGGER.config("Setting vGap for " + WhatIsIt + " :" + dynaGridNode.getAttribute("vgap"));
            }
            catch (NumberFormatException ex)
            {
                LOGGER.warning("vgap for " + WhatIsIt + " invalid: " + dynaGridNode.getAttribute("vgap") + ".  Ignoring");
            }
        }
        if (true == dynaGridNode.hasAttribute("Align"))
        {
            String str = dynaGridNode.getAttribute("Align");
            retWidget.setAlignment(str);
        }

        else
        { // if not an external declaration, check for known options
            Utility.ValidateAttributes(new String[]
            {
                "row", "column", "rowSpan", "colSpan", "columnSpan", "hgap", "vgap", "Align", "Height", "Width"
            }, dynaGridNode);

        }

        // go read the grid contents - note that this could be a continuation from stuff already read in via file
        // so you can define most of grid in external file, but keep adding
        retWidget = ReadGridInfo(dynaGridNode, retWidget);
        AliasMgr.getAliasMgr().PopAliasList();
        return retWidget;
    }

    public static DynamicGridWidget ReadGridInfo(FrameworkNode gridNode, DynamicGridWidget retWidget)
    {
        if (gridNode.getChildNodes().isEmpty())
        {
            return retWidget;
        }
        for (FrameworkNode node : gridNode.getChildNodes(true))
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }

            if (node.getNodeName().equalsIgnoreCase("PaddingOverride") || node.getNodeName().equalsIgnoreCase("Padding"))
            {
                retWidget.HandleWidgetSpecificSettings(node);
            }
            else if (node.getNodeName().equalsIgnoreCase("Widget") || node.getNodeName().equalsIgnoreCase("Grid"))
            {
                Widget subWidget = WidgetBuilder.Build(node);
                if (null != subWidget)
                {
                    retWidget.AddWidget(subWidget);
                }
                else
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("GridMacro") || node.getNodeName().equalsIgnoreCase("MacroGrid"))
            {
                if (!ReadGridMacro(node))
                {
                    return null;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("For"))
            {
                List<Widget> repeatList = WidgetBuilder.BuildRepeatList(node);
                if (null == repeatList)
                {
                    return null;
                }
                for (Widget objWidget : repeatList)
                {
                    retWidget.AddWidget(objWidget);
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("MinionSrc"))
            {
                if (node.hasAttributes())
                {
                    Utility.ValidateAttributes(new String[]
                    {
                        "Namespace", "ID"
                    }, node);
                    if (node.hasAttribute("ID") && node.hasAttribute("Namespace"))
                    {
                        retWidget.setMinionID(node.getAttribute("ID"));
                        retWidget.setNamespace(node.getAttribute("Namespace"));
                        continue;
                    }
                }
                LOGGER.severe("Malformed DynamicGrid Widget MinionSrc Definition in Application.XML");
                return null;
            }

            else if (node.getNodeName().equalsIgnoreCase("StyleOverride"))
            {
                HandleStyleOverride(retWidget, node);
            }
            else if (node.getNodeName().equalsIgnoreCase("Peekaboo")) // for external grid files
            {
                if (!HandlePeekaboo(retWidget, node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent"))
            {
                retWidget.SetClickThroughTransparentRegion(node.getBooleanValue());
                if (node.hasAttribute("Propagate") && node.getBooleanAttribute("Propagate"))
                {
                    retWidget.setExplicitPropagate(true);
                }
            }

            else if (!retWidget.HandleWidgetSpecificSettings(node))
            {
                LOGGER.severe("Unknown DynamicGrid Item in Config file: " + node.getNodeName());
                return null;
            }
        }
        return retWidget;
    }

    public static GridWidget ReadGridInfo(FrameworkNode gridNode, GridWidget retWidget, String filename)
    {
        if (!ReadGridAttributes(retWidget, gridNode, false))
        {
            return null;
        }

        if (gridNode.getChildNodes().isEmpty())
        {
            return retWidget;
        }
        if (null != filename)
        {
            LOGGER.config("Processing file [" + filename + "]");
        }

        for (FrameworkNode node : gridNode.getChildNodes())
        {
            String name = node.getNodeName();
            if (name.equalsIgnoreCase("#Text") || name.equalsIgnoreCase("#comment"))
            {
                continue;
            }

            if (name.equalsIgnoreCase("PaddingOverride") || name.equalsIgnoreCase("Padding"))
            {
                if (false == retWidget.HandleWidgetSpecificSettings(node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("Tasklist"))
            {
                if (!ConfigurationReader.ReadTaskList(node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("GenerateDataPoint"))
            {
                if (!ConfigurationReader.ReadGenerateDataPoints(node))
                {
                    return null;
                }
            }
            else if (name.equalsIgnoreCase("Widget") || name.equalsIgnoreCase("Grid") || name.equalsIgnoreCase("DynamicGrid"))
            {
                Widget subWidget = WidgetBuilder.Build(node);
                if (null != subWidget)
                {
                    retWidget.AddWidget(subWidget);
                }
                else
                {
                    return null;
                }
            }
            else if (name.equalsIgnoreCase("GridMacro") || name.equalsIgnoreCase("MacroGrid"))
            {
                if (!ReadGridMacro(node))
                {
                    return null;
                }
            }
            else if (name.equalsIgnoreCase("ListView"))
            {
                retWidget.setUseListView(true);
                if (node.hasChild("StyleOverride"))
                {
                    FrameworkNode styleNode = node.getChild("StyleOverride");
                    Utility.ValidateAttributes(new String[]
                    {
                        "File", "ID", "ScaleToShape"
                    }, styleNode);
                    if (styleNode.hasAttribute("File"))
                    {
                        retWidget.setListViewFileCSS(styleNode.getAttribute("File"));
                    }
                    if (styleNode.hasAttribute("ID"))
                    {
                        retWidget.setStyleID(styleNode.getAttribute("ID"));
                    }
                    List<String> itemList = new ArrayList<>();
                    for (FrameworkNode sNode : styleNode.getChildNodes())
                    {
                        if (sNode.getNodeName().equalsIgnoreCase("#Text") || sNode.getNodeName().equalsIgnoreCase("#comment"))
                        {
                            continue;
                        }
                        if (sNode.getNodeName().equalsIgnoreCase("Item"))
                        {
                            itemList.add(sNode.getTextContent());
                        }
                        else
                        {
                            LOGGER.severe("Unknown Tag under <StyleOverride>: " + sNode.getNodeName());
                        }
                    }
                    retWidget.setListStyleOverride(itemList);
                }

            }

            else if (node.getNodeName().equalsIgnoreCase("For"))
            {
                List<Widget> repeatList = WidgetBuilder.BuildRepeatList(node);
                if (null == repeatList)
                {
                    return null;
                }
                for (Widget objWidget : repeatList)
                {
                    retWidget.AddWidget(objWidget);
                }
            }

            else if (name.equalsIgnoreCase("StyleOverride"))
            {
                HandleStyleOverride(retWidget, node);
            }
            else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent"))
            {
                retWidget.SetClickThroughTransparentRegion(node.getBooleanValue());
                if (node.hasAttribute("Propagate") && node.getBooleanAttribute("Propagate"))
                {
                    retWidget.setExplicitPropagate(true);
                }
            }

            else if (name.equalsIgnoreCase("Peekaboo")) // for external grid files
            {
                if (!HandlePeekaboo(retWidget, node))
                {
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ToolTip"))
            {
                if (false == retWidget.HandleToolTipConfig(node))
                {
                    return null;
                }
            }

            else if (name.equalsIgnoreCase("Size"))
            {
                if (false == HandleSizeSection(node, retWidget))
                {
                    return null;
                }
            }
            else if (name.equalsIgnoreCase("AliasList")) // TODO, should specifically search for this 1st, as might want to place this anywhere in grid
            {
                AliasMgr.HandleAliasNode(node);
            }
            else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent"))
            {
                retWidget.SetClickThroughTransparentRegion(node.getBooleanValue());
            }

            else
            {
                LOGGER.severe("Unknown Grid Item [" + name + "] in Config file: " + filename);
                return null;
            }
        }
        return retWidget;
    }

    public static boolean ReadGridMacro(FrameworkNode node)
    {
        if (!node.hasAttribute("Name"))
        {
            LOGGER.severe("GridMacro must hvae a Name attribute");
            return true;
        }
        String strName = node.getAttribute("Name");
        GridMacroMgr.getGridMacroMgr().AddGridMacro(strName, node);
        return true;
    }

    public static FrameworkNode OpenDefinitionFile(String inputFilename, String DesiredNode)
    {
        LOGGER.config("Opening [" + DesiredNode + "] file: " + inputFilename);

        Document doc = ConfigurationReader.OpenXMLFile(inputFilename);
        if (null == doc)
        {
            return null;
        }
        boolean foundRequiredRoot = false;
        NodeList nodes = doc.getChildNodes();
        for (int iLoop = 0; iLoop < nodes.getLength(); iLoop++)
        {
            if (nodes.item(iLoop).getNodeName().equalsIgnoreCase("MarvinExternalFile"))
            {
                nodes = nodes.item(iLoop).getChildNodes();
                foundRequiredRoot = true;
                break;
            }
        }

        if (false == foundRequiredRoot)
        {
            LOGGER.severe("External defintion file [" + inputFilename + "] did not have root of <MarvinExternalFile>");
            return null;
        }

        for (int iLoop = 0; iLoop < nodes.getLength(); iLoop++)
        {
            Node node = nodes.item(iLoop);
            if (null != DesiredNode)
            {
                if (node.getNodeName().equalsIgnoreCase(DesiredNode))
                {
                    return new FrameworkNode(node);
                }
            }
            else if (node.hasChildNodes()) // only root should have children, so should be good
            {
                return new FrameworkNode(node);
            }
        }
        return null;
    }

    public static List<Widget> BuildRepeatList(FrameworkNode repeatNode)
    {
        ArrayList<Widget> objWidgetList = new ArrayList<>();
        int count, start;
        String strCountAlias = "";
        String strValueAlias = "";

        AliasMgr.getAliasMgr().PushAliasList(false);
        AliasMgr.getAliasMgr().AddAliasFromAttibuteList(repeatNode, new String[] // can define an alias list in <repeat>
                                                {
                                                    "Count", "startvlaue", "currentCountAlias", "currentvalueAlias"
        });

        if (!repeatNode.hasAttribute("Count"))
        {
            LOGGER.severe("For did not have Count attribute.");
            return null;
        }
        count = repeatNode.getIntegerAttribute("Count", -1);
        if (count < 1)
        {
            LOGGER.warning("For Count value invalid: " + repeatNode.getAttribute("Count"));
            return objWidgetList;
        }
        start = repeatNode.getIntegerAttribute("StartValue", 0);

        if (start < 0)
        {
            LOGGER.severe("For Start value invalid: " + repeatNode.getAttribute("startValue"));
            return null;
        }
        if (repeatNode.hasAttribute("CurrentCountAlias"))
        {
            strCountAlias = repeatNode.getAttribute("CurrentCountAlias");
        }

        if (repeatNode.hasAttribute("CurrentValueAlias"))
        {
            strValueAlias = repeatNode.getAttribute("CurrentValueAlias");
        }

        for (int iLoop = 0; iLoop < count; iLoop++)
        {
            AliasMgr.getAliasMgr().PushAliasList(false);
            if (!strCountAlias.isEmpty())
            {
                AliasMgr.getAliasMgr().AddAlias(strCountAlias, Integer.toString(iLoop));
            }
            if (!strValueAlias.isEmpty())
            {
                AliasMgr.getAliasMgr().AddAlias(strValueAlias, Integer.toString(iLoop + start));
            }
            // Always have these aliases
            AliasMgr.getAliasMgr().AddAlias("CurrentValueAlias", Integer.toString(iLoop + start));
            AliasMgr.getAliasMgr().AddAlias("CurrentCountAlias", Integer.toString(iLoop));

            for (FrameworkNode node : repeatNode.getChildNodes())
            {
                if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
                {
                    continue;
                }
                if (node.getNodeName().equalsIgnoreCase("Widget") || node.getNodeName().equalsIgnoreCase("Grid") || node.getNodeName().equalsIgnoreCase("DynamicGrid"))
                {
                    Widget widget = WidgetBuilder.Build(node);

                    if (null != widget)
                    {
                        objWidgetList.add(widget);
                    }
                    else
                    {
                        LOGGER.severe("Error creating " + node.getNodeName() + " in <Repeat>");
                        return null;
                    }
                }
                else if (node.getNodeName().equalsIgnoreCase("GridMacro") || node.getNodeName().equalsIgnoreCase("MacroGrid"))
                {
                    ReadGridMacro(node);
                }

                else if (node.getNodeName().equalsIgnoreCase("For")) // embedded <Repeat>s - kewl!
                {
                    objWidgetList.addAll(BuildRepeatList(node));
                }
                else
                {
                    LOGGER.warning("Unknown item in <For> :" + node.getNodeName() + " ignoring");
                }
            }
            AliasMgr.getAliasMgr().PopAliasList();
        }

        AliasMgr.getAliasMgr().PopAliasList();
        return objWidgetList;
    }

    public static void StartReadingExternalFile(FrameworkNode node)
    {
        String strPrint = Integer.toString(FileDepth) + ": ";
        for (int iLoop = 0; iLoop < FileDepth; iLoop++)
        {
            strPrint += "....";
        }

        strPrint += node.getAttributeList() + "\n";
        FileTree += strPrint;
        FileDepth++;
    }

    public static void DoneReadingExternalFile()
    {
        FileDepth--;
    }

    public static String GetFileTree()
    {
        return FileTree;
    }
}
