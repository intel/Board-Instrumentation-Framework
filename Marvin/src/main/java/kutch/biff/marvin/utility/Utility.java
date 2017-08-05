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
package kutch.biff.marvin.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

/**
 *
 * @author Patrick Kutch
 */
public class Utility
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public static boolean ValidateAttributes(String ValidAttributes[],FrameworkNode node)
    {
        boolean retVal = true;
        
        if (node.hasAttributes())
        {
            NamedNodeMap attrs = node.GetNode().getAttributes();  

            for (int oLoop = 0; oLoop<attrs.getLength();oLoop++)
            {
                boolean found = false;
                Attr attribute = (Attr)attrs.item(oLoop); 
                for (int iLoop = 0; iLoop < ValidAttributes.length;iLoop++) // compare to list of valid
                {
                    if (0 == ValidAttributes[iLoop].compareToIgnoreCase(attribute.getName()) ) // 1st check case independent just for fun
                    {
                            found = true;
                        break;
                    }
                }
                if (false == found)
                {
                    LOGGER.warning("Unknown XML Attribute for " + node.getNodeName() +" found: " + attribute.getName() + ". Ignoring.");
                }
            }
        }
        
        return retVal;
    }
    
    public static boolean ValidateAttributes(String ValidAttributes[],String[] MoreAttributes, FrameworkNode node)
    {
        if (null == MoreAttributes)
        {
            return ValidateAttributes(ValidAttributes,node);
        }
        ArrayList<String> Attributes = new ArrayList<>(Arrays.asList(ValidAttributes));
        Attributes.addAll(Arrays.asList(MoreAttributes));
        
        return ValidateAttributes(Attributes.toArray(new String[Attributes.size()]),node);
    }
    
}
