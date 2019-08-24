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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.utility.DynamicItemInfoContainer.SortMethod;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandGridBuilder;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class OnDemandGridWidget extends GridWidget
{
    private String __strPrimaryGrowth = "HZ";
    private String __strSecondaryGrowth = "VT";
    private int __NewLineCount = 1;
    @SuppressWarnings("unused")
    private int __currentLineCount = 0;
    private int __nextPositionX = 0;
    private int __nextPositionY = 0;
    private DynamicItemInfoContainer __criterea;
    private List<Pair<BaseWidget, String>> __AddedGridList;

    public OnDemandGridWidget(DynamicItemInfoContainer onDemandInfo)
    {
        __criterea = onDemandInfo;
        __AddedGridList = new ArrayList<>();
    }

    public boolean AddOnDemandWidget(BaseWidget objWidget, String sortStr)
    {
        Pair<Integer, Integer> position = getNextPosition();

        objWidget.setColumn(position.getKey());
        objWidget.setRow(position.getValue());
        objWidget.setWidth(getWidth());
        objWidget.setHeight(getHeight());

        if (objWidget.Create(getGridPane(), DataManager.getDataManager()))
        {
            if (objWidget.PerformPostCreateActions(this, false))
            {
                __AddedGridList.add(new Pair<>(objWidget, sortStr));
                if (__criterea.getSortByMethod() != SortMethod.NONE)
                {
                    resortWidgets(); // OddEven Style applied in here
                }
                else
                {   
                    __criterea.ApplyOddEvenStyle(objWidget,__AddedGridList.size());
                }
                return true;
            }
        }

        return false;
    }
    
    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr)
    {
        super.Create(parentPane, dataMgr);
        setupPositioning();
        OnDemandGridBuilder objBuilder = new OnDemandGridBuilder(this);
        dataMgr.AddOnDemandWidgetCriterea(__criterea, objBuilder);
        // grab ALL aliases to use when contained widgets created
        return true;
    }

    public DynamicItemInfoContainer getCriterea()
    {
        return __criterea;
    }

    private Pair<Integer, Integer> getNextPosition()
    {
        Pair<Integer, Integer> retObj = null;
        if ("HZ".equals(__strPrimaryGrowth))
        {
            __nextPositionX++;
            if (__nextPositionX >= __NewLineCount)
            {
                __currentLineCount++;
                __nextPositionX = 0;
                if ("VT".equals(__strSecondaryGrowth))
                {
                    __nextPositionY++;
                }
                else
                {
                    __nextPositionY--;
                }
            }
        }
        else if ("ZH".equals(__strPrimaryGrowth))
        {
            __nextPositionX--;
            if (__nextPositionX < 0)
            {
                __currentLineCount++;
                __nextPositionX = __NewLineCount - 1;
                if ("VT".equals(__strSecondaryGrowth))
                {
                    __nextPositionY++;
                }
                else
                {
                    __nextPositionY--;
                }
            }
        }

        else if ("VT".equals(__strPrimaryGrowth))
        {
            __nextPositionY++;
            if (__nextPositionY >= __NewLineCount)
            {
                __currentLineCount++;
                __nextPositionY = 0;
                if ("HZ".equals(__strSecondaryGrowth))
                {
                    __nextPositionX++;
                }
                else
                {
                    __nextPositionX--;
                }
            }
        }
        else if ("TV".equals(__strPrimaryGrowth))
        {
            __nextPositionY--;
            if (__nextPositionY < 0)
            {
                __currentLineCount++;
                __nextPositionY = __NewLineCount - 1;
                if ("HZ".equals(__strSecondaryGrowth))
                {
                    __nextPositionX++;
                }
                else
                {
                    __nextPositionX--;
                }
            }
        }

        retObj = new Pair<>(__nextPositionX, __nextPositionY);
        return retObj;
    }

    public boolean ReadGrowthInfo(FrameworkNode growthNode)
    {
        String strPrimary = growthNode.getAttribute("Primary");
        String strSecondary = growthNode.getAttribute("Secondary");
        __NewLineCount = growthNode.getIntegerAttribute("NewLineCount", 1);
        boolean primaryIsHorizontal = false;
        boolean secondaryIsHorizontal = false;

        if (null == strPrimary)
        {
            strPrimary = "HZ";
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
            if (primaryIsHorizontal)
            {
                strSecondary = "VT";
                secondaryIsHorizontal = false;
            }
            else
            {
                __strSecondaryGrowth = "HZ";
                secondaryIsHorizontal = true;
            }
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

    private void resortWidgets()
    {
//        getGridPane().getChildren().removeAll(getGridPane().getChildren());
        getGridPane().getChildren().clear();
        sortGrids();
        
        setupPositioning();
        int widgetNum = 0;
        for (Pair<BaseWidget, String> tuple : __AddedGridList)
        {
            widgetNum++;
            BaseWidget objWidget = tuple.getKey();
            Pair<Integer, Integer> position = getNextPosition();
            getGridPane().add(objWidget.getStylableObject(), position.getKey(), position.getValue());
            __criterea.ApplyOddEvenStyle(objWidget,widgetNum);
        }
    }

    private void setupPositioning()
    {
       if ("HZ".equals(__strPrimaryGrowth))
        {
            __nextPositionX = -1;
            if ("VT".equals(__strSecondaryGrowth))
            {
                __nextPositionY = 0;
            }
            else
            {
                __nextPositionY = 125;
            }
        }
        else if ("ZH".equals(__strPrimaryGrowth))
        {
            __nextPositionX = __NewLineCount - 1;
            if ("VT".equals(__strSecondaryGrowth))
            {
                __nextPositionY = 0;
            }
            else
            {
                __nextPositionY = 125;
            }
        }
        else if ("VT".equals(__strPrimaryGrowth))
        {
            __nextPositionY = -1;
            if ("HZ".equals(__strSecondaryGrowth))
            {
                __nextPositionX = -1;
            }
            else
            {
                __nextPositionX = 125;
            }
        }
        else if ("TV".equals(__strPrimaryGrowth))
        {
            __nextPositionY = __NewLineCount - 1;
            if ("HZ".equals(__strSecondaryGrowth))
            {
                __nextPositionX = -1;
            }
            else
            {
                __nextPositionX = 125;
            }
        }        
    }

    
    private void sortGrids()
    {
        Collections.sort(__AddedGridList, new Comparator<Pair<BaseWidget, String>>()
                 {
                     @Override
                     public int compare(Pair<BaseWidget, String> o1, Pair<BaseWidget, String> o2)
                     {
                         return o1.getValue().compareTo(o2.getValue());
                     }
                 }
        );
    }
}
