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
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.TranslationCalculator;
import static kutch.biff.marvin.widget.BaseWidget.convertToFileURL;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;
import static kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.HandlePeekaboo;

/**
 *
 * @author Patrick Kutch
 */
public class TabWidget extends GridWidget
{

    private Tab _tab;
    private GridPane _BaseGridPane; // throw one down in tab to put all the goodies in
    private boolean _IsVisible;
    private int _TabIndex;
    ScrollPane _ScrollPane;
    private boolean _UseScrollBars;
    private Pane basePane;
    private StackPane _stackReference;

    public TabWidget(String tabID)
    {
        super();
        setMinionID(tabID); // isn't really a minion, just re-using field
        _BaseGridPane = new GridPane(); // can't rely on super class
        _tab = new Tab();
        setBaseCSSFilename("TabDefault.css");
        _IsVisible = true;

        basePane = new Pane();

        _UseScrollBars = CONFIG.getEnableScrollBars();

        if (_UseScrollBars)
        {
            _ScrollPane = new ScrollPane();
            _ScrollPane.setPannable(true);
            _ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            _ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        }
        _DefinitionFile = "Tab {" + tabID + "}";
        _WidgetType = "Tab";
    }

    /**
     * Create the tab
     *
     * @param tabPane
     * @param dataMgr
     * @param iIndex
     * @return
     */
    public boolean Create(TabPane tabPane, DataManager dataMgr, int iIndex)
    {
        _TabIndex = iIndex;

        _BaseGridPane.setPadding(new Insets(getInsetTop(), getInsetRight(), getInsetBottom(), getInsetLeft()));

        boolean fSuccess = super.Create(_BaseGridPane, dataMgr);
        if (true == fSuccess)
        {
            _tab.setText(getTitle());
            _tab.setClosable(false);

            //basePane.setStyle("-fx-background-color:yellow");
            _stackReference = new StackPane(); // for back filler when translating
//            _stackReference.setStyle("-fx-background-color:red");

            getGridPane().setAlignment(getPosition());

            //stackReference.getChildren().add(this.getGridPane());
            basePane.getChildren().add(_stackReference);
            basePane.getChildren().add(_BaseGridPane);

            _stackReference.prefWidthProperty().bind(CONFIG.getCurrentWidthProperty());
            _stackReference.prefHeightProperty().bind(CONFIG.getCurrentHeightProperty());

            if (_UseScrollBars)
            {
                _ScrollPane.setContent(basePane);

                _tab.setContent(_ScrollPane);
            }
            else
            {
                basePane.prefWidthProperty().bind(CONFIG.getCurrentWidthProperty());
                basePane.prefHeightProperty().bind(CONFIG.getCurrentHeightProperty());

                _tab.setContent(basePane);

            }

            new TranslationCalculator(_stackReference, _BaseGridPane, CONFIG.getScaleProperty(), getPosition()); // handles all the resizing/scaling

            tabPane.getTabs().add(_tab);
            SetupPeekaboo(dataMgr);

            ApplyCSS();
            return true;
        }

        return false;
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return basePane;
//        return _BaseGridPane;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return basePane.getStylesheets();
//        return _BaseGridPane.getStylesheets();
    }

    @Override
    protected String GetCSS_File()
    {
        String strFile = "";
        if (null != getBaseCSSFilename())
        {
            //strFile = getBaseCSSFilename();
            strFile = strFile + getDefinintionFileDirectory() + File.separatorChar + getBaseCSSFilename();

            File file = new File(strFile);
            if (false == file.exists())
            {
                LOGGER.severe("Unable to locate Tab Stylesheet: " + strFile + " : " + getBaseCSSFilename());
                return null;
            }
            return convertToFileURL(strFile);
        }
        return null;
    }

    public boolean LoadConfiguration(FrameworkNode doc)
    {
        if (doc.getChildNodes().isEmpty())
        {
            LOGGER.severe("No Widgets Defined for Tab: " + getTitle());
            return false;
        }
        for (FrameworkNode node : doc.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#Comment"))
            {
                continue;
            }

            if (node.getNodeName().equalsIgnoreCase("Title"))
            {
                setTitle(node.getTextContent());
            }
            else if (node.getNodeName().equalsIgnoreCase("Peekaboo"))
            {
                if (!HandlePeekaboo(this, node))
                {
                    return false;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("StyleOverride"))
            {
                if (false == WidgetBuilder.HandleStyleOverride(this, node))
                {
                    return false;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent"))
            {
                SetClickThroughTransparentRegion(node.getBooleanValue());
                if (node.hasAttribute("Propagate") && node.getBooleanAttribute("Propagate"))
                {
                    setExplicitPropagate(true);
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("Widget") || node.getNodeName().equalsIgnoreCase("Grid") || node.getNodeName().equalsIgnoreCase("DynamicGrid"))
            {
                Widget widget = WidgetBuilder.Build(node);

                if (null != widget)
                {
                    _Widgets.add(widget);
                }
                else
                {
                    return false;
                }
            }
            else if (node.getNodeName().equalsIgnoreCase("GridMacro") || node.getNodeName().equalsIgnoreCase("MacroGrid"))
            {
                if (!WidgetBuilder.ReadGridMacro(node))
                {
                    return false;
                }
            }

            else if (node.getNodeName().equalsIgnoreCase("For"))
            {
                List<Widget> repeatList = WidgetBuilder.BuildRepeatList(node);
                if (null == repeatList)
                {
                    return false;
                }
                _Widgets.addAll(repeatList);
            }

            else if (node.getNodeName().equalsIgnoreCase("TaskList"))
            {
                ConfigurationReader.ReadTaskList(node);
            }
            else if (node.getNodeName().equalsIgnoreCase("Prompt"))
            {
                ConfigurationReader.ReadPrompt(node);
            }

            else if (node.getNodeName().equalsIgnoreCase("AliasList"))
            {
                // already deal with someplace else
            }
            else if (false == HandleWidgetSpecificSettings(node))
            {
                LOGGER.warning("Unknown Entry: " + node.getNodeName() + " in Tab ID= " + getMinionID());
            }
        }

        return true;
    }

    public boolean isVisible()
    {
        return _IsVisible;
    }

    public void setVisible(boolean _IsVisible)
    {
        this._IsVisible = _IsVisible;
    }

    public int getTabIndex()
    {
        return _TabIndex;
    }

    public Tab getTabControl()
    {
        return _tab;
    }

    @Override
    protected GridPane getGridPane()
    {
        return _BaseGridPane;
    }
}
