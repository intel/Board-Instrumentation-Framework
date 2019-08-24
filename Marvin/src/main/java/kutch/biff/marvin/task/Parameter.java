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
	if (_value.charAt(0) == '@' && null != PromptManager.getPromptManager().getPrompt(_value.substring(1)))
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
	
	if (parts.length > 1 && parts.length < 5)
	{
	    // ID + Namespace
	    strVal = DataManager.getDataManager().GetValue(parts[0], parts[1]);
	    if (null == strVal) // Try it as Namespace and ID too
	    {
		strVal = DataManager.getDataManager().GetValue(parts[1], parts[0]);
	    }
	    if (null != strVal && parts.length > 2)
	    {
		int iIndex;
		try
		{
		    iIndex = Integer.parseInt(parts[2]);
		}
		catch(Exception ex)
		{
		    LOGGER.severe("Received invalid Index for GetDataSrcValue: " + parts[2]);
		    return strAlias;
		}
		String strToken = ",";
		if (parts.length > 3)
		{
		    strToken = parts[3];
		}
		String[] parts2 = strVal.split(strToken);
		if (null == parts2 || parts2.length < iIndex)
		{
		    LOGGER.severe("Received invalid Index for GetDataSrcValue: " + parts[2]);
		    return strAlias;
		}
		strVal = parts2[iIndex];
	    }
	}
	return strVal;
    }
    
    public static int findClosingParen(String text, int openPos)
    {
	int closePos = openPos;
	int counter = 1;
	while (counter > 0)
	{
	    if (++closePos >= text.length())
	    {
		return -1;
	    }
	    char c = text.charAt(closePos);
	    if (c == '(')
	    {
		counter++;
	    }
	    else if (c == ')')
	    {
		counter--;
	    }
	}
	return closePos;
    }
    
    /***
     * Routine to see if there is an MinionSrc 'alias' embedded within the XML node
     * string Supports an alias within an alias. Is a reentrant routine
     * 
     * @param strData the raw string
     * @return string with expanded replacement
     */
    public static String HandleDataSrc(String strData)
    {
	// ID="%($(DemoNS),AVAIL_PMU_RDT_ID_LIST,%($(DemoNS),SELECTED_HIST_INDEX))
	String retString = strData;
	if (false == strData.contains("%("))
	{
	    return strData;
	}
	
	int OutterIndex = strData.lastIndexOf("%(");
	int CloseParenIndex = findClosingParen(strData,OutterIndex+1);
	
	if (CloseParenIndex > 0)
	{
	    String pair = strData.substring(OutterIndex + 2, CloseParenIndex);
	    String strVal = GetDataSrcVal(pair);
	    
	    //= strData.substring(0, OutterIndex);
	    if (null != strVal)
	    {
		retString = strData.replace(strData.substring(OutterIndex, CloseParenIndex+1),strVal);

		String nextLoop = HandleDataSrc(retString);
		while (!nextLoop.equals(retString))
		{
		    retString = nextLoop;
		    nextLoop =  HandleDataSrc(retString);
		}
	    }
	    else // didn't find a match, so skip and move to next
	    {
		//retString = strData.substring(0, CloseParenIndex + 1);
		//retString += HandleDataSrc(strData.substring(CloseParenIndex + 1));
		//return retString;
	    }
	}
	else
	{
	    LOGGER.warning("Something looks like an MinionSrc Parameter but is not correctly formed: " + strData);
	    return strData;
	}
	
	return retString;
    }
}
