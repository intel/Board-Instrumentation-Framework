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
package kutch.biff.marvin.widget;

import java.util.HashMap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import static kutch.biff.marvin.widget.BaseWidget.CONFIG;
import static kutch.biff.marvin.widget.BaseWidget.LOGGER;
import kutch.biff.marvin.widget.dynamicgrid.DynamicGrid;
import kutch.biff.marvin.widget.dynamicgrid.DynamicTransition;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;
import static kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.ReadGridInfo;

/**
 *
 * @author Patrick Kutch
 */
public class DynamicGridWidget extends GridWidget
{
    private final HashMap<String, DynamicGrid> _GridMap;
    private final HashMap<String, String> _TaskMap;
    private CircularList<String> _ListID;
    private String _CurrentKey;
    private boolean _AutoAdvance;
    private boolean _AutoLoopWithAdvance;
    private int _AutoAdvanceInterval;
    private static int _AutoAdvanceGridNumber = 0;
    private GridPane _TransitionPane;
    private DynamicTransition _latestTransition;

    public DynamicGridWidget()
    {
        _GridMap = new HashMap<>();
        _TaskMap = new HashMap<>();
        _ListID = new CircularList<>();
        _CurrentKey = null;
        _AutoAdvance = false;
        _AutoLoopWithAdvance = false;
        _AutoAdvanceInterval = 0;
        _TransitionPane = null;
        _latestTransition = null;
    }

    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr)
    {
        if (super.Create(parentPane, dataMgr))
        {
            _TransitionPane = getGridPane(); // _ParentPane is used in 
            for (Widget objWidget : _Widgets)
            {
                if (GridWidget.class.isInstance(objWidget))
                {   // make all the grids invisible
                    objWidget.getStylableObject().setVisible(false);
                }
            }
            
            if (_GridMap.isEmpty())
            {
                LOGGER.warning("Dynamic Grid has no Grids.  Ignoring.");
                return true;
            }

            if (null != _CurrentKey)
            {
                if (!_GridMap.containsKey(_CurrentKey))
                {
                    LOGGER.severe("Initial ID for Dynamic Grid: " + _CurrentKey + " is invalid.");
                    return false;
                }

                GridWidget objGrid = _GridMap.get(_ListID.get(_CurrentKey));
                setAlignment(objGrid.getAlignment());
                getGridPane().setAlignment(getPosition());
                objGrid.getStylableObject().setVisible(true);
            }

            if (_AutoAdvance)
            {
                if (null == getMinionID() || null == getNamespace())
                {
                    String ID = Integer.toBinaryString(DynamicGridWidget._AutoAdvanceGridNumber);
                    DynamicGridWidget._AutoAdvanceGridNumber++;

                    if (null == getMinionID())
                    {
                        setMinionID(ID);
                    }
                    if (null == getNamespace())
                    {
                        setNamespace(ID);
                    }
                }
                MarvinTask mt = new MarvinTask();
                mt.AddDataset(getMinionID(), getNamespace(), "Next");
                TASKMAN.AddPostponedTask(mt, _AutoAdvanceInterval);
            }

            dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
            {
                @Override
                public void changed(ObservableValue o, Object oldVal, Object newVal)
                {
                    if (IsPaused())
                    {
                        return;
                    }

                    String strVal = newVal.toString().replaceAll("(\\r|\\n)", "");
                    String key;

                    if (strVal.equalsIgnoreCase("Next")) // go to next image in the list
                    {
                        key = _ListID.GetNext();

                    }
                    else if (strVal.equalsIgnoreCase("Previous")) // go to previous image in the list
                    {
                        key = _ListID.GetPrevious();
                    }
                    else
                    {
                        key = strVal; // expecting an ID
                        _ListID.get(key); // just to keep next/prev alignment
                    }
                    ActivateGrid(key);
                }
            });

            return true;
        }
        return false;
    }
    private void ActivateGrid(String key)
    {
        String prevKey = _CurrentKey;
        key = key.toLowerCase();

        if (_GridMap.containsKey(key)) // specified grid ID is valid, so let's proceed
        {
            DynamicGrid objGridCurrent, objGridNext;
            objGridCurrent = null;
            if (prevKey != null && !prevKey.equalsIgnoreCase(key))
            {
                objGridCurrent = _GridMap.get(prevKey);
                if (null != objGridCurrent)
                {
                    //objGridCurrent.getStylableObject().setVisible(false);
                }
            }
            objGridNext = _GridMap.get(key);
            if (null != objGridNext)
            {
                setAlignment(objGridNext.getAlignment());
                getGridPane().setAlignment(getPosition());
                if (null == objGridCurrent )
                {
                    objGridNext.getStylableObject().setVisible(true);
                }
                else 
                {
                    if (null != _latestTransition && _latestTransition.stillPlaying())
                    {
                        _latestTransition.stopTransition(); // current transition still playing, so stop it
                    }
                    _latestTransition = objGridNext.getTransition(objGridCurrent, _TransitionPane);
                }
                _CurrentKey = key;
            }

            if (_TaskMap.containsKey(key))  // Grid now active - is there a task associated with it?
            {
                TASKMAN.PerformTask(_TaskMap.get(key)); // yup, go run it!
            }
        }
        else
        {
            LOGGER.warning("Received unknown ID: [" + key +"] for DynamicGrid #" + getName() +": [" + getNamespace() + ":" + getMinionID() + "]");
            return;
        }
        if (_AutoAdvance)
        {
            if (!_AutoLoopWithAdvance && _ListID.IsLast(key))
            {
                _AutoAdvance = false;
                return;
            }
            MarvinTask mt = new MarvinTask();
            mt.AddDataset(getMinionID(), getNamespace(), "Next");
            TASKMAN.AddPostponedTask(mt, _AutoAdvanceInterval);
        }
    }

    @Override
    public boolean PerformPostCreateActions(GridWidget objParentGrid)
    {
        _WidgetParentGridWidget = objParentGrid;
        if (CONFIG.isDebugMode())
        {
            _ToolTip = this.toString();
        }
        if (_ToolTip != null)
        {
            HandleToolTipInit();
            for (String key : _GridMap.keySet())
            {
                DynamicGrid objGrid = _GridMap.get(key); 
                Tooltip.install(objGrid.getStylableObject(), _objToolTip);
            }
        }
        super.PerformPostCreateActions(objParentGrid);

        return handlePercentageDimentions();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        String Id = "";
        String FileName;

        if (super.HandleWidgetSpecificSettings(node)) // padding override etc.
        {
            return true;
        }
        if (node.getNodeName().equalsIgnoreCase("AutoAdvance"))
        {
            /*        
             <AutoAdvance Frequency='1000' Loop='False'/>
             */
            if (node.hasAttribute("Frequency"))
            {
                _AutoAdvanceInterval = node.getIntegerAttribute("Frequency", -1);
                if (_AutoAdvanceInterval < 100)
                {
                    LOGGER.severe("Frequency specified for DynamicGrid is invalid: " + node.getAttribute("Frequency"));
                    return false;
                }

                if (node.hasAttribute("Loop"))
                {
                    _AutoLoopWithAdvance = node.getBooleanAttribute("Loop");
                }
                _AutoAdvance = true;
                return true;
            }
            return false;
        }
        if (node.getNodeName().equalsIgnoreCase("Initial"))
        {
            Utility.ValidateAttributes(new String[]
            {
                "ID"
            }, node);
            if (node.hasAttribute("ID"))
            {
                _CurrentKey = node.getAttribute("ID").toLowerCase();
                return true;
            }
            LOGGER.severe("Dynamic Grid Widget incorrectly defined Initial Grid, no ID.");
            return false;
        }

        if (node.getNodeName().equalsIgnoreCase("GridFile"))
        {
            if (node.hasAttribute("Source"))
            {
                FileName = node.getAttribute("Source");
            }
            else
            {
                LOGGER.severe("Dynamic Grid Widget has no Source for Grid");
                return false;
            }
            if (node.hasAttribute("ID"))
            {
                Id = node.getAttribute("ID");

                if (true == _GridMap.containsKey(Id.toLowerCase()))
                {
                    LOGGER.severe("Dynamic Grid Widget has repeated Grid ID: " + Id);
                    return false;
                }
                Id = Id.toLowerCase();
            }
            else
            {
                LOGGER.severe("Dynamic Grid Widget has no ID for Grid");
                return false;
            }
            if (node.hasAttribute("TaskOnActivate"))
            {
                String Task = node.getAttribute("TaskOnActivate");
                _TaskMap.put(Id, Task); // task to run on activate
            }

            DynamicGrid objGrid = (DynamicGrid) BuildGrid(node);
            
            if (null == objGrid)
            {
                return false;
            }
            if ( null == objGrid.ReadTransitionInformation(node))
            {
                return false;
            }
            
            objGrid.ConfigureAlignment();
            _Widgets.add(objGrid);
            _GridMap.put(Id, objGrid);
            _ListID.add(Id);
        }
        return true;
    }

    private GridWidget BuildGrid(FrameworkNode node)
    {
        GridWidget retWidget = new DynamicGrid(); // DynamicGrid is a superset, so can do this
        if (true == node.hasAttribute("Source"))
        {
            AliasMgr.getAliasMgr().PushAliasList(true);
            AliasMgr.getAliasMgr().AddAliasFromAttibuteList(node, new String[]
            {
                "hgap", "vgap", "Align", "Source", "ID"
            });
            if (false == AliasMgr.ReadAliasFromExternalFile(node.getAttribute("Source")))
            {
                AliasMgr.getAliasMgr().PopAliasList();
                return null;
            }
            FrameworkNode GridNode = WidgetBuilder.OpenDefinitionFile(node.getAttribute("Source"), "Grid");

            if (null == GridNode)
            {
                return null;
            }
            retWidget = ReadGridInfo(GridNode, retWidget, ""); // read grid from external file
            if (null == retWidget)
            {
                return null;
            }
            if (!ConfigurationReader.ReadTasksFromExternalFile(node.getAttribute("Source"))) // could also be tasks defined in external file
            {
                return null;
            }
            if (node.hasAttribute("hgap"))
            {
                try
                {
                    retWidget.sethGap(Integer.parseInt(node.getAttribute("hgap")));
                    LOGGER.config("Setting hGap for DynamicGrid :" + node.getAttribute("hgap"));
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.warning("hgap for DynamicGrid  invalid: " + node.getAttribute("hgap") + ".  Ignoring");
                }
            }
            if (node.hasAttribute("vgap"))
            {
                try
                {
                    retWidget.setvGap(Integer.parseInt(node.getAttribute("vgap")));
                    LOGGER.config("Setting vGap for DynamicGrid :" + node.getAttribute("vgap"));
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.warning("vgap for DynamicGrid invalid: " + node.getAttribute("vgap") + ".  Ignoring");
                }
            }
            if (true == node.hasAttribute("Align"))
            {
                String str = node.getAttribute("Align");
                retWidget.setAlignment(str);
            }
            else
            {
                retWidget.setAlignment(getAlignment()); // if one wasn't specifice for the grid file, use whatever the master for the widget is.
            }

            if (node.hasAttribute("Task"))
            {
                retWidget.setTaskID(node.getAttribute("Task"));
            }
            AliasMgr.getAliasMgr().PopAliasList();
        }
        return retWidget;
    }

}
