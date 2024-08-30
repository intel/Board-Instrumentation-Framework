/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package kutch.biff.marvin.widget.widgetbuilder;
import java.util.logging.Level;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.GradientPanelWidget;

/**
 *
 * @author Patrick
 */
public class GradientPanelWidgetBuilder {
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static GradientPanelWidget Build(FrameworkNode masterNode, String widgetDefFilename) {
        kutch.biff.marvin.widget.GradientPanelWidget _widget = new kutch.biff.marvin.widget.GradientPanelWidget();
        for (FrameworkNode node : masterNode.getChildNodes()) {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget, node)) {
                continue;
            } else {
                LOGGER.log(Level.SEVERE, "Unknown GradientPanelWidget setting in Widget Definition file: {0}", node.getNodeName());
                return null;
            }
        }

        return _widget;

    }
    
}
