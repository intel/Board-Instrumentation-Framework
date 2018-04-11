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
package kutch.biff.marvin.task;

import java.util.logging.Logger;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class Parameter
{
    private String _value;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    public Parameter()
    {
       
    }
    public Parameter(String val)
    {
        _value = val;
    }
    
    @Override
    public String toString()
    {
        if (_value.charAt(0)=='@' && null != PromptManager.getPromptManager().getPrompt(_value.substring(1)))
        {
            BasePrompt objPrompt = PromptManager.getPromptManager().getPrompt(_value.substring(1));
            if (null == objPrompt)
            {
                return null;
            }
            if (objPrompt.PerformPrompt())
            {
                return objPrompt.GetPromptedValue();
            }
            LOGGER.info("Prompted value was cancelled");
            return null;
        }
        
        return CheckForDataSrcParameter(_value);
    }
    
    public static String CheckForDataSrcParameter(String strCheck)
    {
        return Parameter.HandleDataSrc(strCheck);
    }
    
    public static String GetDataSrcVal(String strAlias)
    {
        String parts[] = strAlias.split(",");
        String strVal = null;
        
        if (parts.length==2)
        {
            // ID + Namespace
            strVal = DataManager.getDataManager().GetValue(parts[0], parts[1]);
            if (null == strVal) // Try it as Namespace and ID too
            {
                strVal = DataManager.getDataManager().GetValue(parts[1], parts[0]);
            }
        }
        return strVal;
    }
    /***
     * Routine to see if there is an MinionSrc 'alias' embedded within the XML node string
     * Supports an alias within an alias.  Is a reentrant routine
     * @param strData the raw string
     * @return string with expanded replacement
     */
    public static String HandleDataSrc(String strData)
    {
        String retString="";
        if (false == strData.contains("%("))
        {
            return strData;
        }
        int OutterIndex = strData.indexOf("%(");
        int CloseParenIndex = strData.indexOf(")",OutterIndex);
        
        if (CloseParenIndex > 0)
        {
                String pair = strData.substring(OutterIndex+2, CloseParenIndex);
                String strVal = GetDataSrcVal(pair);
                
                retString = strData.substring(0, OutterIndex);
                if (null != strVal)
                {
                    retString += strVal;
                    retString += strData.substring(CloseParenIndex+1);
                }
                else // didn't find a match, so skip and move to next
                {
                    retString = strData.substring(0,CloseParenIndex+1);
                    retString += HandleDataSrc(strData.substring(CloseParenIndex+1));
                    return retString;
                }
        }
        else 
        {
            LOGGER.warning("Something looks like an MinionSrc Parameter but is not correctly formed: " + strData);
            return strData;
        }
        
        return HandleDataSrc(retString);
    }    
}
