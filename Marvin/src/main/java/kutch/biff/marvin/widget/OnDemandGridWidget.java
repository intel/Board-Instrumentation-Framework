/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
 * ###
*/
package kutch.biff.marvin.widget;

import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandGridBuilder;


/**
 *
 * @author Patrick
 */
public class OnDemandGridWidget extends GridWidget
{
    private String __strPrimaryGrowth="HZ";
    private String __strSecondaryGrowth="VT";
    private int __NewLineCount=1;
    private int __currentLineCount=0;
    private int __nextPositionX=0;
    private int __nextPositionY=0;
    private DynamicItemInfoContainer __criterea;
    
    public OnDemandGridWidget(DynamicItemInfoContainer onDemandInfo)
    {
        __criterea = onDemandInfo;
    }
    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr)
    {
        super.Create(parentPane, dataMgr);
        OnDemandGridBuilder objBuilder = new OnDemandGridBuilder(this);
        dataMgr.AddOnDemandWidgetCriterea(__criterea, objBuilder);
        return true;
    }
    
    public DynamicItemInfoContainer getCriterea()
    {
        return __criterea;
    }
    
    private Pair<Integer,Integer> getNextPosition()
    {
        Pair<Integer,Integer> retObj = new Pair<>(__nextPositionX,__nextPositionY);
        __nextPositionY++;
        return retObj;
    }
    public boolean AddOnDemandWidget(BaseWidget objWidget)
    {
        Pair<Integer,Integer> position = getNextPosition();
        
        objWidget.setColumn(position.getKey());
        objWidget.setRow(position.getValue());
        
        objWidget.Create(getGridPane(), DataManager.getDataManager());
        objWidget.PerformPostCreateActions(this, false);
        //this.getGridPane().add(_WidgetParentPane, 0,0);
        return true;
    }
    
    public boolean ReadGrowthInfo(FrameworkNode growthNode)
    {
        String strPrimary = growthNode.getAttribute("Primary");
        String strSecondary = growthNode.getAttribute("Secondary");
        int NewLineCount= growthNode.getIntegerAttribute("NewLineCount", 1);
        boolean primaryIsHorizontal = false;
        boolean secondaryIsHorizontal = false;
        
        if (null == strPrimary)
        {
            strPrimary="HZ";
            primaryIsHorizontal = true;
        }
        else if ("Horizontal".equalsIgnoreCase(strPrimary) || "HZ".equalsIgnoreCase(strPrimary))
        {
            __strPrimaryGrowth = "HZ";
            primaryIsHorizontal = true;
        }
        else if ("latnoziroh".equalsIgnoreCase(strPrimary) || "ZH".equalsIgnoreCase(strPrimary))
        {
            __strPrimaryGrowth = "ZH";
            primaryIsHorizontal = true;
        }
        else if ("Vertical".equalsIgnoreCase(strPrimary) || "VT".equalsIgnoreCase(strPrimary))
        {
            __strPrimaryGrowth = "VT";
        }
        else if ("lacitrev".equalsIgnoreCase(strPrimary) || "TV".equalsIgnoreCase(strPrimary))
        {
            __strPrimaryGrowth = "TV";
        }
        else
        {
            LOGGER.severe("Unknown primary Growth Direction for OnDemand grid: " + strPrimary);
            return false;
        }
        
        if (null == strSecondary)
        {
            strSecondary="HZ";
            secondaryIsHorizontal = true;
        }
        else if ("Horizontal".equalsIgnoreCase(strSecondary) || "HZ".equalsIgnoreCase(strSecondary))
        {
            __strSecondaryGrowth = "HZ";
            secondaryIsHorizontal = true;
        }
        else if ("latnoziroh".equalsIgnoreCase(strSecondary) || "ZH".equalsIgnoreCase(strSecondary))
        {
            __strSecondaryGrowth = "ZH";
            secondaryIsHorizontal = true;
        }
        else if ("Vertical".equalsIgnoreCase(strSecondary) || "VT".equalsIgnoreCase(strSecondary))
        {
            __strSecondaryGrowth = "VT";
        }
        else if ("lacitrev".equalsIgnoreCase(strSecondary) || "TV".equalsIgnoreCase(strSecondary))
        {
            __strSecondaryGrowth = "TV";
        }
        else
        {
            LOGGER.severe("Unknown secondary Growth Direction for OnDemand grid: " + strSecondary);
            return false;
        }
        
        if (primaryIsHorizontal == secondaryIsHorizontal)
        {
            LOGGER.severe("Primary and secondary Growth Direction for OnDemand grid must differ, one must be horizontal, the other vertical");
            return false;
        }
            
        return true;
    }
}
