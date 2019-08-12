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
import kutch.biff.marvin.widget.ButtonWidget;
import kutch.biff.marvin.widget.MenuButtonWidget;
import kutch.biff.marvin.widget.ToggleButtonWidget;

/**
 *
 * @author Patrick Kutch
 */
public class ButtonWidgetBuilder
{
    @SuppressWarnings("unused")
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static ButtonWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        ButtonWidget btnWidget = new ButtonWidget();
        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(btnWidget, node))
            {
                continue;
            }
        }
        return btnWidget;
    }
    public static MenuButtonWidget BuildMenuButton(FrameworkNode masterNode, String widgetDefFilename)
    {
        MenuButtonWidget btnWidget = new MenuButtonWidget();
        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(btnWidget, node))
            {
                continue;
            }
        }
        return btnWidget;
    }
    
    public static ToggleButtonWidget BuildToggleButton(FrameworkNode masterNode, String widgetDefFilename)
    {
        ToggleButtonWidget btnWidget = new ToggleButtonWidget();
        for (FrameworkNode node : masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(btnWidget, node))
            {
                continue;
            }
        }
        return btnWidget;
    }
}
