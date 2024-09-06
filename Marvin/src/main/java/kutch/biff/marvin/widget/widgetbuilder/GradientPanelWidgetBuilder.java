/*
 * ##############################################################################
 * #  Copyright (c) 2024 Intel Corporation
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.GradientPanelWidget;
import kutch.biff.marvin.control.GradientColor;

/**
 *
 * @author Patrick Kutch
 */
public class GradientPanelWidgetBuilder {

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static GradientPanelWidget Build(FrameworkNode masterNode, String widgetDefFilename) {
        kutch.biff.marvin.widget.GradientPanelWidget _widget = new kutch.biff.marvin.widget.GradientPanelWidget();

        for (FrameworkNode node : masterNode.getChildNodes()) {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget, node)) {
            } else if (node.getNodeName().equalsIgnoreCase("MinValue")) {
                String str = node.getTextContent();
                try {
                    _widget.setMinValue(Float.parseFloat(str));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid MinValue in GradientPanel Widget Definition File");
                    return null;
                }
            } else if (node.getNodeName().equalsIgnoreCase("MaxValue")) {
                String str = node.getTextContent();
                try {
                    _widget.setMaxValue(Float.parseFloat(str));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid MaxValue in GradientPanel Widget Definition File");
                    return null;
                }
            } else if (node.getNodeName().equalsIgnoreCase("Colors")) {
                List<GradientColor> gradientColors = GradientPanelWidgetBuilder.readColors(node);
                if (null == gradientColors) {
                    return null;
                }
                _widget.setGradientColors(gradientColors);
            } else if (node.getNodeName().equalsIgnoreCase("ShowValue")) {
                boolean show = node.getBooleanValue();
                _widget.setShowValue(show);
            } else if (node.getNodeName().equalsIgnoreCase("ShowTitle")) {
                boolean show = node.getBooleanValue();
                _widget.setShowLabel(show);
            } else if (node.getNodeName().equalsIgnoreCase("Decimals")) {
                String str = node.getTextContent();
                try {
                    _widget.setDecimalPlaces(Integer.parseInt(str));
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Invalid Decimals in GradientPane Widget Definition File");
                    return null;
                }
            } else {
                LOGGER.log(Level.SEVERE, "Unknown GradientPanelWidget setting in Widget Definition file: {0}", node.getNodeName());
                return null;
            }
        }

        return _widget;
    }

    public static List<GradientColor> readColors(FrameworkNode node) {
        List<GradientColor> gradientColors = new ArrayList<>();
        float totalWeight = 0;
        for (FrameworkNode colorNode : node.getChildNodes()) {
            if (!colorNode.hasChild("Hex")) {
                LOGGER.log(Level.SEVERE, "GradientPanelWidget Color setting requires 'Hex'}");
                return null;
            }
            if (!colorNode.hasChild("Weight")) {
                LOGGER.log(Level.SEVERE, "GradientPanelWidget Color setting requires 'Weight'}");
                return null;
            }
            String hex = colorNode.getChild("Hex").getTextContent();
            try {
                Color color = Color.web(hex);

                float weight = (float) colorNode.getChild("Weight").getDoubleContent();
                totalWeight += weight;
                gradientColors.add(new GradientColor(color, weight));
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, "Invalid Color value {0} in GradientPanel Widget", hex);
                return null;
            }
        }

        if (gradientColors.size() < 2) {
            LOGGER.log(Level.SEVERE, "GradientPanelWidget requires at least 2 Color settings}");
            return null;
        }
        if (totalWeight != 1.0) {
            LOGGER.log(Level.SEVERE, "GradientPanelWidget weights must add up to 1");
            return null;
        }
        return gradientColors;
    }

}
