/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kutch.biff.marvin.widget;

import java.util.List;
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
