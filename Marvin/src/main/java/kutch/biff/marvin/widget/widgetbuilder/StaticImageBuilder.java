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
import kutch.biff.marvin.widget.StaticImageWidget;

/**
 *
 * @author Patrick Kutch
 */
public class StaticImageBuilder
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    /**
     *
     * @param masterNode
     * @return
     */
    public static StaticImageWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        StaticImageWidget _widget = new StaticImageWidget();
        return ParseXML(_widget, masterNode);
    }

    /**
     *
     * @param _widget
     * @param masterNode
     * @return
     */
    protected static StaticImageWidget ParseXML(StaticImageWidget _widget, FrameworkNode masterNode)
    {

        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget, node))
            {
            }

            else if (node.getNodeName().equalsIgnoreCase("PreserveRatio"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    _widget.setPreserveRatio(true);
                }

                else if (0 == str.compareToIgnoreCase("False"))
                {
                    _widget.setPreserveRatio(false);
                }
                else
                {
                    LOGGER.severe("Invalid PreserveRatio in Image  Definition File.  Should be true or false, not " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ScaleToFit"))
            {
                String str = node.getTextContent();
                if (0 == str.compareToIgnoreCase("True"))
                {
                    _widget.setScaleToFit(true);
                }

                else if (0 == str.compareToIgnoreCase("False"))
                {
                    _widget.setScaleToFit(false);
                }
                else
                {
                    LOGGER.severe("Invalid ScaleToFit in Image  Definition File.  Should be true or false, not " + str);
                    return null;
                }
            }
        }
        return _widget;
    }
}
