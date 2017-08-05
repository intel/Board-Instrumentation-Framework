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

import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import kutch.biff.marvin.datamanager.DataManager;
import static kutch.biff.marvin.widget.BaseWidget.LOGGER;

/**
 *
 * @author Patrick Kutch
 */
public class WebWidget extends BaseWidget
{
    private WebView _Browser;
    private boolean _ReverseContent;
    private String  _CurrentContent;
    private String  _HackedFile;
    
    public WebWidget()
    {
        _Browser = new WebView();
        _ReverseContent = false;
        _CurrentContent="";
        _HackedFile = null;
    }
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        ConfigureDimentions();
        if (_ReverseContent)
        {
            _Browser.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }
        
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);
        SetContent(_CurrentContent);
        
        pane.add(_Browser, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                if (IsPaused())
                {
                    return;
                }
                _HackedFile = null;
                SetContent(newVal.toString());
            }
        });

        // check status of loading
        _Browser.getEngine().getLoadWorker().stateProperty().addListener(
                new ChangeListener<State>()
                {
                    @Override
                    public void changed(ObservableValue ov, State oldState, State newState)
                    {
                        if (newState == State.FAILED)
                        {
                            // browser requires absolute path, so let's try to provide that, in case a relative one was provided
                            if ("file:".equalsIgnoreCase(_CurrentContent.substring(0,5)))
                            {
                                if (_HackedFile == null)
                                {
                                    Path currentRelativePath = Paths.get("");
                                    String workDir = currentRelativePath.toAbsolutePath().toString() + java.io.File.separator;
                                    
                                    _HackedFile = _CurrentContent;
                                    _CurrentContent = "file:" + workDir + _CurrentContent.substring(5);
                                    SetContent(_CurrentContent);
                                    return;        
                                }
                                _CurrentContent = _HackedFile;
                            }
                            
                            LOGGER.info("Error loading web widget content: " + _CurrentContent);
                        }
                    }
                });


        SetupTaskAction();
        return ApplyCSS();
    }
    
    private void BadContent(String strContent)
    {
        LOGGER.warning("Received bad WebWidget content: " + strContent);
    }
    private void SetContent(String strContent)
    {
        if (strContent.length() < 10)
        {
            BadContent(strContent);
            return;
        }
        
        if ("http".equalsIgnoreCase(strContent.substring(0,4)))
        {
            _Browser.getEngine().load(strContent);
        }
        else if ("file:".equalsIgnoreCase(strContent.substring(0,5)))
        {
            _Browser.getEngine().load(strContent);
        }
        else
        {
            _Browser.getEngine().loadContent(strContent);
        }
        _CurrentContent = strContent;
    }
    

    @Override
    public ObservableList<String> getStylesheets()
    {

        return _Browser.getStylesheets();
    }

    @Override
    public Node getStylableObject()
    {
        return _Browser;
    }
    
    public void SetReverseContent(boolean newVal)
    {
        _ReverseContent = newVal;
    }

    @Override
    public void SetInitialValue(String value)
    {
        _CurrentContent = value;
    }

    @Override
    protected void ConfigureDimentions()
    {
        if (getHeight() > 0)
        {
            _Browser.setPrefHeight(getHeight());
        }
        
        if (getWidth() > 0)
        {
            _Browser.setPrefWidth(getWidth());
        }
    }
    @Override
    public void UpdateTitle(String strTitle)
    {
        LOGGER.warning("Tried to update Title of a WebWidget to " + strTitle);
    }

}
