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

import java.io.File;
import java.util.HashMap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import static kutch.biff.marvin.widget.BaseWidget.CONFIG;
import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;
import kutch.biff.marvin.widget.dynamicgrid.DynamicTransition;

/**
 * Displays an image, but it can be changed. Is a list of images each with a
 * 'nam' associated with it. DataPoint comes in with a name and is displayed.
 *
 * @author Patrick Kutch
 */
public class DynamicImageWidget extends StaticImageWidget
{

    private HashMap<String, String> _ImageFilenames;
    private HashMap<String, DynamicTransition> _TransitionMap;
    private HashMap<String, ImageView> _ImageViewMap;
    private HashMap<String,Long> _MontorMap;
    //private ArrayList<String> _ImageFileNames;
    private CircularList<String> _ListID;
    private String _CurrentKey;
    private boolean _AutoAdvance;
    private boolean _AutoLoopWithAdvance;
    private int _AutoAdvanceInterval;
    private static int _AutoAdvanceImageNumber = 0;
    private ImageView _ActiveView;
    private GridPane basePane;
    private int _MonitorInterval=500;

    public DynamicImageWidget()
    {
        _ImageFilenames = new HashMap<>();
        _TransitionMap = new HashMap<>();
        _ImageViewMap = new HashMap<>();
        _MontorMap = new HashMap<>();
//        _ImageFileNames = new ArrayList<>();
        _CurrentKey = null;
        _ListID = new CircularList<>();
        setDefaultIsSquare(false);
        _AutoAdvance = false;
        _AutoLoopWithAdvance = false;
        _AutoAdvanceInterval = 0;
        _ActiveView = null;
        _ImageView = null;
        basePane = new GridPane();

    }

    /**
     *
     * @param pane
     * @param dataMgr
     * @return
     */
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        if (setupImages())
        {
            ConfigureAlignment();
            for (String key : this._ImageFilenames.keySet())
            {
                ImageView obj = _ImageViewMap.get(key);
                basePane.add(obj, 0, 0);
            }
            pane.add(basePane, getColumn(), getRow(), getColumnSpan(), getRowSpan());
            SetupPeekaboo(dataMgr);

            if (_AutoAdvance)
            {
                if (null == getMinionID() || null == getNamespace())
                {
                    String ID = Integer.toBinaryString(DynamicImageWidget._AutoAdvanceImageNumber);
                    DynamicImageWidget._AutoAdvanceImageNumber++;

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
            if (!_MontorMap.isEmpty())
            {
                MarvinTask mt = new MarvinTask();
                mt.AddDataset(getMinionID(), getNamespace(), "Monitor");
                TASKMAN.AddPostponedTask(mt, _MonitorInterval);
            }

            DynamicImageWidget objDynaImg = this;

            dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
                        {
                            @Override
                            public void changed(ObservableValue o, Object oldVal, Object newVal)
                            {
                                boolean ChangeOcurred = false;
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
                                else if (strVal.equalsIgnoreCase("Monitor"))
                                {
                                    ChangeOcurred = MonitorForFilechange();
                                    if (ChangeOcurred)
                                    {
                                    key = _CurrentKey;
                                        
                                    }
                                    key = _CurrentKey;
                                    
                                    MarvinTask mt = new MarvinTask();
                                    mt.AddDataset(getMinionID(), getNamespace(), "Monitor");
                                    TASKMAN.AddPostponedTask(mt, _MonitorInterval);
                                }
                                else
                                {
                                    key = strVal; // expecting an ID
                                    _ListID.get(key); // just to keep next/prev alignment
                                }
                                key = key.toLowerCase();
                                if (_ImageFilenames.containsKey(key))
                                {
                                    if (!key.equalsIgnoreCase(_CurrentKey) || ChangeOcurred) // no reason to re-load if it is already loaded
                                    {
                                        DynamicTransition objTransition = null;

                                        ImageView startView = null;
                                        ImageView nextView = null;

                                        if (_TransitionMap.containsKey(_CurrentKey))
                                        {
                                            DynamicTransition objCurrentTransition = _TransitionMap.get(_CurrentKey);
                                            if (objCurrentTransition.stillPlaying())
                                            {
                                                objCurrentTransition.stopTransition();
                                            }
                                        }

                                        if (_TransitionMap.containsKey(key))
                                        {
                                            objTransition = _TransitionMap.get(key);
                                            startView = _ImageViewMap.get(_CurrentKey);
                                        }
                                        else
                                        {
                                            _ImageViewMap.get(_CurrentKey).setVisible(false);
                                        }

                                        _CurrentKey = key;

                                        if (null != objTransition && null != startView)
                                        {
                                            nextView = _ImageViewMap.get(_CurrentKey);
                                            objTransition.Transition(objDynaImg, startView, nextView);
                                        }
                                        else
                                        {
                                            _ImageViewMap.get(_CurrentKey).setVisible(true);
                                        }
                                    }
                                }
                                else
                                {
                                    LOGGER.warning("Received unknown ID: [" + strVal + "] for dynamic Image#" + getName() + ": [" + getNamespace() + ":" + getMinionID() + "]");
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
                        });
            SetupTaskAction();

            return ApplyCSS();
        }
        return false;
    }

    public GridPane GetContainerPane()
    {
        return basePane;
    }

    @Override
    public EventHandler<MouseEvent> SetupTaskAction()
    {
        BaseWidget objWidget = this;
        if (null != getTaskID() || CONFIG.isDebugMode()) // only do if a task to setup, or if debug mode
        {
            EventHandler<MouseEvent> eh = new EventHandler<MouseEvent>()
            {
                @Override
                public void handle(MouseEvent event)
                {
                    if (event.isShiftDown() && CONFIG.isDebugMode())
                    {
                        LOGGER.info(objWidget.toString(true));
                    }
                    else if (null != getTaskID() && true == CONFIG.getAllowTasks())
                    {
                        TASKMAN.PerformTask(getTaskID());
                    }
                }
            };
            for (String key : _ImageFilenames.keySet())
            {
                _ImageViewMap.get(key).setOnMouseClicked(eh);
            }

            return eh;
        }
        return null;
    }

@Override
    public boolean PerformPostCreateActions(GridWidget objParentGrid)
    {
        if (CONFIG.isDebugMode())
        {
            _ToolTip = this.toString();
        }
        if (_ToolTip != null)
        {
            HandleToolTipInit();
            for (String key : _ImageFilenames.keySet())
            {
                ImageView objView = _ImageViewMap.get(key);
                Tooltip.install(objView, _objToolTip);
            }
        }

        return true;
    }
    
    private boolean MonitorForFilechange()
    {
        boolean retVal = false;
        for (String Id : _MontorMap.keySet())
        {
            File fp = new File(_ImageFilenames.get(Id).substring("file:".length())); // is stored as url, so skip the 1st part
            if (fp.lastModified() != _MontorMap.get(Id))
            {
                _MontorMap.put(Id, fp.lastModified());
                LOGGER.info("Monitoring " + _ImageFilenames.get(Id) +" updated");
                ImageView objImageView = new ImageView(_ImageFilenames.get(Id));
                objImageView.setPreserveRatio(getPreserveRatio());
                objImageView.setSmooth(true);
                objImageView.setPickOnBounds(!GetClickThroughTransparentRegion());
                objImageView.setVisible(_ImageViewMap.get(Id).isVisible());
                objImageView.setFitWidth(_ImageViewMap.get(Id).getFitWidth());
                objImageView.setFitHeight(_ImageViewMap.get(Id).getFitHeight());
                basePane.getChildren().remove(_ImageViewMap.get(Id));

                _ImageViewMap.put(Id, objImageView);
                basePane.add(objImageView, 0, 0);

                retVal = true;
            }
        }
        return retVal;
    }
    
    private boolean setupImages()
    {
        for (String key : _ImageFilenames.keySet())
        {
            ImageView objImageView = new ImageView(_ImageFilenames.get(key));
            objImageView.setPreserveRatio(getPreserveRatio());
            objImageView.setSmooth(true);
            objImageView.setPickOnBounds(!GetClickThroughTransparentRegion());
            objImageView.setVisible(false);
            _ImageViewMap.put(key, objImageView);
        }

        if (_CurrentKey == null)
        {
            LOGGER.severe("No Initial Image setup for Dynamic Image Widget.");
            return false;
        }
        else if (_ImageFilenames.containsKey(_CurrentKey))
        {
            _ActiveView = _ImageViewMap.get(_CurrentKey);
            _ActiveView.setVisible(true);
        }
        else
        {
            LOGGER.severe("Initial key not valid for dynamic image widget: " + _CurrentKey);
            return false;
        }
        ConfigureDimentions();
        return true;
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        String Id = null;
        String FileName = null;

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
            else
            {
                LOGGER.severe("Dynamic Image Widget incorrectly defined Initial Image, no ID.");
                return false;
            }
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
                    LOGGER.severe("Frequency specified for DynamicImage is invalid: " + node.getAttribute("Frequency"));
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

        if (node.getNodeName().equalsIgnoreCase("Image"))
        {
            Utility.ValidateAttributes(new String[]
            {
                "Source", "ID","Monitor"
            }, node);
            if (node.hasAttribute("Source"))
            {
                FileName = node.getAttribute("Source");
            }
            else
            {
                LOGGER.severe("Dynamic Image Widget has no Source for Image");
                return false;
            }
            if (node.hasAttribute("ID"))
            {
                Id = node.getAttribute("ID");

                if (true == _ImageFilenames.containsKey(Id.toLowerCase()))
                {
                    LOGGER.severe("Dynamic Image Widget has repeated Image ID: " + Id);
                    return false;
                }
                Id = Id.toLowerCase();
            }
            else
            {
                LOGGER.severe("Dynamic Image Widget has no ID for Image");
                return false;
            }
            String fname = convertToFileOSSpecific(FileName);
            if (null == fname)
            {
                return false;
            }
            File file = new File(fname);
            
            if (file.exists())
            {
                String fn = "file:" + fname;
                //Image img = new Image(fn);
                _ImageFilenames.put(Id, fn);
                _ListID.add(Id);
                if (node.hasAttribute("Monitor"))
                {
                    if (node.getBooleanAttribute("Monitor"))
                    {
                        _MontorMap.put(Id, file.lastModified());
                    }
                }
                
            }

            else
            {
                LOGGER.severe("Dynamic Image Widget - missing Image file: " + FileName);
                return false;
            }
            if (node.hasChild("Transition"))
            {
                DynamicTransition objTransition = DynamicTransition.ReadTransitionInformation(node);
                if (null != objTransition)
                {
                    this._TransitionMap.put(Id, objTransition);
                }
            }
        }

        return true;
    }

    @Override
    protected void ConfigureDimentions()
    {
        for (String key : _ImageFilenames.keySet())
        {

            if (getHeight() > 0)
            {
                _ImageViewMap.get(key).setFitHeight(getHeight());
            }

            if (getWidth() > 0)
            {
                _ImageViewMap.get(key).setFitWidth(getWidth());

            }
        }
    }

    @Override
    public void ConfigureAlignment()
    {
        for (String key : _ImageFilenames.keySet())
        {
            Node objStylable = _ImageViewMap.get(key);
            if (objStylable != null)
            {
                GridPane.setValignment(objStylable, getVerticalPosition());
                GridPane.setHalignment(objStylable, getHorizontalPosition());
            }
        }
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
//        return _Pane;
        return null;
    }

    @Override
    protected boolean ApplyCSS()
    {
        boolean fRet = true;
        if (null != GetCSS_File())
        {
            //getStylesheets().clear();

            LOGGER.config("Applying Stylesheet: " + GetCSS_File() + " to Widget [" + _DefinitionFile + "]");
            // This was a .add(), but changed to Sett all as there was kind of
            // memory leak when I changed style via Minion or MarvinTasks...
            fRet = getStylesheets().setAll(GetCSS_File());
            if (false == fRet)
            {
                LOGGER.severe("Failed to apply Stylesheet " + GetCSS_File());
                return false;
            }
        }
        if (null != getStyleID())
        {
            getStylableObject().setId(getStyleID());
        }

        for (String key : _ImageFilenames.keySet())
        {
            Node objStylable = _ImageViewMap.get(key);
            fRet = ApplyStyleOverrides(objStylable, getStyleOverride());
        }
        return fRet;
    }

}
