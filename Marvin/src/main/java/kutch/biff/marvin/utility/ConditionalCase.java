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
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick Kutch
 *
 * 
 *                 <Conditional Type="CASE">
 *                               <MinionSrc Namespace="$(Namespace)" ID="$(Status).background.warp.core" />
 *                               <Case Value="0">MyTask</Value>
 *                               <Case Value="1">MyTask1</Value>
 *                               <Case Value="2">MyTask2</Value>
 *                               <Default>myDefTask</Default>
 *               </Conditional>
 */

public class ConditionalCase extends Conditional
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final TaskManager TASKMAN = TaskManager.getTaskManager();

    private final ArrayList<String> _CaseValues;
    private final HashMap<String, String> _Tasks;
    private String _DefaultTask;

    public ConditionalCase(Type type)
    {
        super(type,true);
        _CaseValues = new ArrayList<>();
        _Tasks = new HashMap<>();
        _DefaultTask = null;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this._CaseValues);
        hash = 23 * hash + Objects.hashCode(this._Tasks);
        hash = 23 * hash + Objects.hashCode(this._DefaultTask);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ConditionalCase other = (ConditionalCase) obj;
        if (!Objects.equals(this._DefaultTask, other._DefaultTask))
        {
            return false;
        }
        if (!Objects.equals(this._CaseValues, other._CaseValues))
        {
            return false;
        }
        if (!Objects.equals(this._Tasks, other._Tasks))
        {
            return false;
        }
        return true;
    }

    protected boolean AddNewCaseStatement(String CompareValue, String Task)
    {
        String strValue;
        if (isCaseSensitive())
        {
            strValue = CompareValue;
        }
        else
        {
            strValue = CompareValue.toUpperCase();
        }
        if (_Tasks.containsKey(strValue))
        {
            LOGGER.severe("Conditional CASE has duplicate <Case> value.");
            return false;
        }
        _Tasks.put(strValue, Task);
        _CaseValues.add(strValue);

        return true;
    }

    public boolean SetDefaultTask(String strTask)
    {
        if (null == _DefaultTask)
        {
            _DefaultTask = strTask;
            return true;
        }
        LOGGER.severe("Conditional CASE can't have more than one Default Task.");
        return false;
    }

    @Override
    protected void Perform(String rawValue)
    {
        String strValue = rawValue;
        if (!isCaseSensitive())
        {
            strValue = rawValue.toUpperCase();
        }

        for (String strCompare : _CaseValues)
        {
            if (strCompare.equals(strValue))
            {
                String strTask = _Tasks.get(strCompare);
                TASKMAN.AddDeferredTask(strTask);
                return;
            }
        }
        if (null != _DefaultTask)
        {
            TASKMAN.AddDeferredTask(_DefaultTask); // no match, so do defautl
        }
    }

    public static ConditionalCase BuildConditionalCase(FrameworkNode condNode)
    {
        ConditionalCase objCase = new ConditionalCase(Type.CASE);
        if (objCase.ReadMinionSrc(condNode))
        {
            String strValue = null;
            String strTask = null;
            for (FrameworkNode node : condNode.getChildNodes())
            {
                if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#Comment"))
                {
                    continue;
                }
                if (node.getNodeName().equalsIgnoreCase("MinionSrc"))
                {
                    continue;
                }

                if (node.getNodeName().equalsIgnoreCase("Case"))
                {
                    if (node.hasAttribute("Value"))
                    {
                        strValue = node.getAttribute("Value");
                    }
                    else
                    {
                        LOGGER.severe("Conditional CASE has <Case> item without a Value defined");
                        objCase = null;
                        break;
                    }
                    strTask = node.getTextContent();
                    if (!objCase.AddNewCaseStatement(strValue, strTask))
                    {
                        objCase = null;
                        break;
                    }
                }
                else if (node.getNodeName().equalsIgnoreCase("Default"))
                {
                    if (!objCase.SetDefaultTask(node.getTextContent()))
                    {
                        objCase = null;
                        break;
                    }
                }
                else
                {
                    LOGGER.config("Unknown Conditional Case item: " + node.getNodeName() + " - ignoring.");
                }
            }
        }
        else
        {
            objCase = null;
        }
        return objCase;
    }
}