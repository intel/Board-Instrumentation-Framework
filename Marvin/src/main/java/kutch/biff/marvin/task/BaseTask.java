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

import java.util.ArrayList;
import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;
import static kutch.biff.marvin.task.Parameter.CheckForDataSrcParameter;

/**
 *
 * @author Patrick Kutch
 */
abstract public class BaseTask implements ITask
{
    private ArrayList<Parameter> _Params;
    private long _PostponePeriod;
    private static int _TaskCount = 0;
    protected String _ID;
    
    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    protected final TaskManager TASKMAN = TaskManager.getTaskManager();
    
    
    public BaseTask()
    {
        _ID = null;
        _TaskCount++;
    }
    public BaseTask(String taskID)
    {
        _Params = null;
        _PostponePeriod = 0;
        _TaskCount++;
        _ID=taskID;
    }
    
    public String getTaskID_ForVerification()
    {
        return _ID;
    }
    
    public String getTaskID()
    {
        if (null == _ID)
        {
            return this.toString();
        }
        return _ID;
    }
    
    public void setTaskID(String taskID)
    {
        _ID = taskID;
    }
    @Override
    public boolean getMustBeInGUIThread()
    {
        return true;
    }
    public static int getTaskCount()
    {
        return _TaskCount;
    }

    public ArrayList<Parameter> getParams()
    {
        return _Params;
    }

    public void setParams(ArrayList<Parameter> _Params)
    {
        this._Params = _Params;
    }
    
    /**
     * Helper routine to see if there is a prompt associated with the piece of data
     * @param strData
     * @return 
     */
    public String getDataValue(String strData)
    {
        if (strData==null || strData.length()<1)
        {
            return strData;
        }
        
        if (strData.charAt(0) == '@') // could be a prompt
        {
            BasePrompt objPrompt = PromptManager.getPromptManager().getPrompt(strData.substring(1));
            if (null != objPrompt)
            {
                if (objPrompt.PerformPrompt())
                {
                    strData = objPrompt.GetPromptedValue();
                }
                else
                {
                    strData = null; // was cancelled
                }
            }
        }
        
        return CheckForDataSrcParameter(strData);
    }

    @Override
    public long getPostponePeriod()
    {
        return _PostponePeriod;
    }

    @Override
    public void setPostponePeriod(long _PostponePeriod)
    {
        this._PostponePeriod = _PostponePeriod;
    }
    
}
