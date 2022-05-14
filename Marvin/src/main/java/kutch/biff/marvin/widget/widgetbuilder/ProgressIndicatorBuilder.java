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
import kutch.biff.marvin.widget.ProgressIndicatorWidget;

/**
 * @author Patrick Kutch
 */
public class ProgressIndicatorBuilder {
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static ProgressIndicatorWidget Build(FrameworkNode masterNode, String widgetDefFilename) {
        kutch.biff.marvin.widget.ProgressIndicatorWidget _widget = new kutch.biff.marvin.widget.ProgressIndicatorWidget();
        for (FrameworkNode node : masterNode.getChildNodes()) {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget, node)) {
                continue;
            } else {
                LOGGER.severe("Unknown Progress Bar setting in Widget Definition file: " + node.getNodeName());
                return null;
            }
        }

        return _widget;
    }

}
