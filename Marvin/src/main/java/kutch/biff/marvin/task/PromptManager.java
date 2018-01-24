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
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick Kutch
 */
public class PromptManager
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static PromptManager _PromptManager = null;
    private ArrayList<BasePrompt> _Prompts = null;
    
    public PromptManager()
    {
        _Prompts = new ArrayList<>();
    }

    public static PromptManager getPromptManager()
    {
        if (null == _PromptManager)
        {
            _PromptManager = new PromptManager();
        }
        return _PromptManager;
    }

    public BasePrompt getPrompt(String ID)
    {
        for (BasePrompt prompt : _Prompts)
        {
            if (ID.equalsIgnoreCase(prompt.toString()))
            {
                return prompt;
            }
        }
        return null;
    }
    
    public boolean addPrompt(String ID, BasePrompt prompt)
    {
        if (null == getPrompt(ID))
        {
            _Prompts.add(prompt);
            
        }
        else
        {
            LOGGER.warning("Tried to add duplicate Prompt: " + ID + ". Ignoring.");
        }
        return true;
    }
    
    public boolean CreatePromptObject(String ID, FrameworkNode taskNode)
    {
        String strType = null;
        String strTitle = null;
        String strMessage = null;
        BasePrompt objPrompt = null;
        
        if (taskNode.hasAttribute("Type"))
        {
            strType = taskNode.getAttribute("Type");
        }
        else
        {
            LOGGER.severe("Invalid Prompt, no Type ID=" + ID);
            return false;
        }
        
        if (strType.equalsIgnoreCase("ListBox"))
        {
            objPrompt = new Prompt_ListBox(ID);
        }
        else if (strType.equalsIgnoreCase("InputBox"))
        {
            objPrompt = new Prompt_InputBox(ID);
        }
        else
        {
            LOGGER.severe("Invalid Prompt Object, Type[" + strType + "] is unknown ID=" + ID);
            return false;
        }
        
        if (taskNode.hasAttribute("Width"))
        {
            objPrompt.setWidth(BaseWidget.parsePercentWidth(taskNode, "Width"));
        }
        if (taskNode.hasAttribute("Height"))
        {
            objPrompt.setHeight(BaseWidget.parsePercentWidth(taskNode, "Height"));
        }
        
        for (FrameworkNode node : taskNode.getChildNodes(true))
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#Comment"))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("StyleSheet"))
            {
                objPrompt.setCssFile(node.getTextContent());
            }
            
            else if (node.getNodeName().equalsIgnoreCase("StyleOverride"))
            {
                String styleList = "";
                
                for (FrameworkNode itemNode : node.getChildNodes())
                {
                    if (itemNode.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
                    {
                        continue;
                    }
                    if (itemNode.getNodeName().equalsIgnoreCase("Item"))
                    {
                        styleList += node.getTextContent() + ";";
                    }
                    else
                    {
                        LOGGER.severe("Unknown Tag under <Prompt> <StyleOverride>: " + itemNode.getNodeName());
                        return false;
                    }
                }
                objPrompt.setStyleOverride(styleList);
            }
            
            else if (node.getNodeName().equalsIgnoreCase("Title"))
            {
                strTitle = node.getTextContent();
            }
            else if (node.getNodeName().equalsIgnoreCase("Message"))
            {
                strMessage = node.getTextContent();
            }
            else if (objPrompt.HandlePromptSpecificConfig(node))
            {
                continue;
            }
            else
            {
                LOGGER.config("Unknown tag in <Prompt> ID[" + ID + "] :" + node.getNodeName());
            }
        }
        if (null == strTitle)
        {
            LOGGER.severe("Prompt ID[+" + ID + "] has no title.");
            return false;
        }
        if (null == strMessage)
        {
            LOGGER.severe("Prompt ID[+" + ID + "] has no message.");
            return false;
        }
        objPrompt.setDlgTitle(strTitle);
        objPrompt.setMessage(strMessage);
        return addPrompt(ID, objPrompt);
    }
    
}
