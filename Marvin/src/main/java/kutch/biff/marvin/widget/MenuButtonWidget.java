/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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
package kutch.biff.marvin.widget;

import javafx.event.EventHandler;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class MenuButtonWidget extends ButtonWidget
{
    private MenuButton _Button;
    public MenuButtonWidget()
    {
        _Button = new MenuButton();
        _Button.getStyleClass().add("kutch");
    }
    
    @Override
    protected ButtonBase getButton()
    {
        return _Button;
    }    
    
    @Override
    public EventHandler<MouseEvent> SetupTaskAction()
    {
        return null;
    }
    
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetNode)
    {
        if (super.HandleWidgetSpecificSettings(widgetNode))
        {
            return true;
        }
            
        ConfigurationReader rdr = ConfigurationReader.GetConfigReader();
        MenuItem objItem = rdr.ReadMenuItem(widgetNode);
        if (null != objItem)
        {
            _Button.getItems().add(objItem);
            return true;
             
        }
        return false;
    }    
}
