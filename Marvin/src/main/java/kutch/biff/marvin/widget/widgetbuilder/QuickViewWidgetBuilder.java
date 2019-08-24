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
import kutch.biff.marvin.widget.QuickViewWidget;

/**
 *
 * @author Patrick Kutch
 */
public class QuickViewWidgetBuilder
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static QuickViewWidget Build(FrameworkNode masterNode, String widgetDefFilename)
    {
        QuickViewWidget _widget = new QuickViewWidget();
        for (FrameworkNode node :masterNode.getChildNodes())
        {
            if (BaseWidget.HandleCommonDefinitionFileConfig(_widget,node))
            {
            }
            else if (node.getNodeName().equalsIgnoreCase("#comment"))
            {
            }
            else if (node.getNodeName().equalsIgnoreCase("RowWidth"))
            {
                String str = node.getTextContent();
                try
                {
                    _widget.setRowWidth(Integer.parseInt(str));
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.severe("Invalid <RowWidth> in QuickViewWidget Widget Definition File : " + str);
                    return null;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("EvenBackgroundStyle"))
            {
                _widget.setEvenBackgroundStyle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("EvenIDStyle"))
            {
                _widget.setEvenIDStyle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("EvenDataStyle"))
            {
                _widget.setEvenDataStyle(node.getTextContent());
            }
                
            else if (node.getNodeName().equalsIgnoreCase("OddBackgroundStyle"))
            {
                _widget.setOddBackgroundStyle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("OddIDStyle"))
            {
                _widget.setOddIDStyle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("OddDataStyle"))
            {
                _widget.setOddDataStyle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("Order"))
            {
                String strVal = node.getTextContent();
                if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Ascending.toString()))
                {
                    _widget.setSortMode(QuickViewWidget.SortMode.Ascending);
                }
                else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Descending.toString()))
                {
                    _widget.setSortMode(QuickViewWidget.SortMode.Descending);
                }
                else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.None.toString()))
                {
                    _widget.setSortMode(QuickViewWidget.SortMode.None);
                }
                else
                {
                    LOGGER.severe("Invalid <Order> Tag in QuickViewWidget Widget Definition File. " + strVal);
                    return null;
                }
            }
            else 
            {
               LOGGER.severe("Invalid QuickViewWidget Widget Definition File.  Unknown Tag: " + node.getNodeName());
               return null;                
            }
            
        }
        return _widget;
    }
        
    
}
