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
import javafx.geometry.Orientation;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.PanelSideInfo;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.FlipPanelWidget;

/**
 *
 * @author Patrick Kutch
 */
public class FlipPanelWidgetBuilder
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static FlipPanelWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        FlipPanelWidget _panel = new FlipPanelWidget();
        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_panel, node))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("AnimationDuration"))
            {
                String str = node.getTextContent();
                try
                {
                    _panel.setAnimationDuration(Double.parseDouble(str));
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Invlid value for <AnimationDuration> tag for FlipPanel Widget");
                    return null;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("RotationDirection"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("Horizontal"))
                {
                    _panel.setOrientation(Orientation.HORIZONTAL);
                }
                else if (0 == str.compareToIgnoreCase("Vertical"))
                {
                    _panel.setOrientation(Orientation.VERTICAL);
                }
                else
                {
                    LOGGER.severe("Invalid Orientation in FlipPanel Widget Definition File. Should be Horizontal or Vertical, not : " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("FrontButton") || node.getNodeName().equalsIgnoreCase("BackButton"))
            {
                String BtnText = null;
                String StyleFile = null;
                String StyleID = null;
                String Location = null;

                if (node.hasAttribute("Text"))
                {
                    BtnText = node.getAttribute("Text");
                }
                if (node.hasAttribute("Position"))
                {
                    Location = node.getAttribute("Position");
                }
                else
                {
                    LOGGER.severe("No Position set for FlipPanel Button");
                    return null;
                }
                for (FrameworkNode iNode : node.getChildNodes())
                {
                    if (iNode.getNodeName().equalsIgnoreCase("#text") || iNode.getNodeName().equalsIgnoreCase("#comment"))
                    {
                        continue;
                    }
                    if (iNode.getNodeName().equalsIgnoreCase("Style"))
                    {
                        StyleFile = iNode.getTextContent();
                        if (iNode.hasAttribute("ID"))
                        {
                            StyleID = iNode.getAttribute("ID");
                        }
                    }
                }
                if (null == Location || null == StyleFile)
                {
                    LOGGER.severe("Invalid Flip Panel side definition :" + node.getNodeName());
                }

                PanelSideInfo panel = new PanelSideInfo(Location, BtnText, StyleID, StyleFile);
                if (node.getNodeName().equalsIgnoreCase("FrontButton"))
                {
                    _panel.setFrontInfo(panel);
                }
                else
                {
                    _panel.setBackInfo(panel);
                }
            }

            else
            {
                LOGGER.severe("Invalid FlipPanel Widget Definition File.  Unknown Tag: " + node.getNodeName());
                return null;
            }
        }
        return _panel;
    }

}
