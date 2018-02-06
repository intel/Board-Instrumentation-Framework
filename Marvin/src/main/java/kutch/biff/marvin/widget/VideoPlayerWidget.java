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

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import static kutch.biff.marvin.widget.BaseWidget.CONFIG;

/**
 *
 * @author Patrick Kutch
 */
public class VideoPlayerWidget extends MediaPlayerWidget
{
    private final MediaView _mediaView;
    private boolean _RetainAspectRatio;
    private static boolean _HasBeenVerified = false;
    private static boolean _IsValid = true;

    @Override
    public boolean HasBeenVerified()
    {
        return VideoPlayerWidget._HasBeenVerified;
    }

    @Override
    public void setHasBeenVerified(boolean _HasBeenVerified)
    {
        VideoPlayerWidget._HasBeenVerified = _HasBeenVerified;
    }
    
    @Override
    public boolean IsValid()
    {
        return _IsValid;
    }
    
    @Override
    public void SetIsValid(boolean flag)
    {
        _IsValid = flag;
    }
    
    public static void VideoPlayerWidget(boolean flag)
    {
        _IsValid = flag;
    }
    
    /**
     *
     */
    public VideoPlayerWidget()
    {
        super("VideoPlayerWidget");
        _mediaView = new MediaView();
        _RetainAspectRatio = true;
    }

    @Override
    protected boolean VerifyMedia(Media objMedia)
    {
        return true;
    }
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        return HandleWidgetSpecificSettings(node,"Video");
    }
        @Override
        
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        ConfigureDimentions();
        ConfigureAlignment();
        
        SetupPeekaboo(dataMgr);
        _mediaView.setPreserveRatio(_RetainAspectRatio);
        
        if (!Create(dataMgr))
        {
            return false;
        }
        pane.add(_mediaView, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        
        SetupTaskAction();
        return true;
    }
    @Override
    public EventHandler<MouseEvent> SetupTaskAction()
    {
        if (false == isMouseHasBeenSetup()) // quick hack, as I call this from MOST widgets, but now want it from all.  Will eventually remove from individual widgets.
        {
            BaseWidget objWidget = this;
            if (_TaskMap.size() > 0 || CONFIG.isDebugMode()) // only do if a task to setup, or if debug mode
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
                        else if (true == CONFIG.getAllowTasks() && _TaskMap.containsKey(_CurrentMediaID.toLowerCase()))
                        {
                            TASKMAN.PerformTask(_TaskMap.get(_CurrentMediaID.toLowerCase()));
                        }
                        else if (null != getTaskID() && true == CONFIG.getAllowTasks())
                        {
                            TASKMAN.PerformTask(getTaskID());
                        }
                    }
                };
                getStylableObject().setOnMouseClicked(eh);
                setMouseHasBeenSetup(true);
                return eh;
            }
        }
        return null;
    }
    
    @Override
    protected boolean OnNewMedia(MediaPlayer objMediaPlayer)
    {
        _mediaView.setMediaPlayer(objMediaPlayer);
        
        return true;
    }

    public boolean getRetainAspectRatio()
    {
        return _RetainAspectRatio;
    }

    public void setRetainAspectRatio(boolean _RetainAspectRatio)
    {
        this._RetainAspectRatio = _RetainAspectRatio;
    }
    
    @Override
    public ObservableList<String> getStylesheets()
    {
        return _mediaView.getStyleClass();
    }

    @Override
    public Node getStylableObject()
    {
        return _mediaView;
    }
    
    @Override
    protected void ConfigureDimentions()
    {
        if (getHeight() > 0)
        {
            _mediaView.setFitHeight(getHeight());
        }
        if (getWidth() > 0)
        {
            _mediaView.setFitWidth(getWidth());
        }
    }
}
