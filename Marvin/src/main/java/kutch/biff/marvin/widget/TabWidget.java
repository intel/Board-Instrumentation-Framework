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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.TranslationCalculator;
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
    private String _TaskOnActivate;
    private boolean _IgnoreFirstSelect;
    private boolean _CreatedOnDemand;
    private String _OnDemandSortStr;

    public TabWidget(String tabID)
    {
        super();
        setMinionID(tabID); // isn't really a minion, just re-using field
        _BaseGridPane = new GridPane(); // can't rely on super class
        _tab = new Tab();
        setBaseCSSFilename("TabDefault.css");
        _IsVisible = true;
        _TaskOnActivate = null;
        _IgnoreFirstSelect = false;
        _CreatedOnDemand = false;
        _OnDemandSortStr = null;
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
        setWidth(CONFIG.getCanvasWidth());
        setHeight(CONFIG.getCanvasHeight());

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
            _stackReference.prefHeightProperty().bind(basePane.heightProperty());

            if (_UseScrollBars)
            {
                _ScrollPane.setContent(_BaseGridPane);
                _ScrollPane.setFitToWidth(true);
                _ScrollPane.setFitToHeight(true);

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

    public void setOnDemandSortBy(String sortStr)
    {
        _OnDemandSortStr = sortStr;
    }

    public String getOnDemandSortBy()
    {
        return _OnDemandSortStr;
    }

    public boolean Reindex(Tab compare, int newIndex)
    {
        if (compare == _tab)
        {
            _TabIndex = newIndex;
            return true;
        }
        return false;
    }

    private static void sortTabs(TabPane tabPane)
    {
        List<TabWidget> tabs = ConfigurationReader.GetConfigReader().getTabs();
        Collections.sort(tabs, new Comparator<TabWidget>()
                 {
                     @Override
                     public int compare(TabWidget o1, TabWidget o2)
                     {
                         if (!o1.getCreatedOnDemand() && o2.getCreatedOnDemand())
                         {
                             return -1;
                         }
                         if (o1.getCreatedOnDemand() && !o2.getCreatedOnDemand())
                         {

                             return 0;
                         }
                         if (!o1.getCreatedOnDemand() && !o2.getCreatedOnDemand())
                         {
                             return 0;
                         }
                         if (null == o1.getOnDemandSortBy() && null == o2.getOnDemandSortBy())
                         {
                             return 0;
                         }
                         if (null == o1.getOnDemandSortBy() && null != o2.getOnDemandSortBy())
                         {
                             return 1;
                         }
                         if (null != o1.getOnDemandSortBy() && null == o2.getOnDemandSortBy())
                         {
                             return -1;
                         }
                         return o1.getOnDemandSortBy().compareToIgnoreCase(o2.getOnDemandSortBy());
                         //return nc.compare(o1.getTitle(),o2.getTitle());
                     }
                 }
        );
        int index = 0;
        for (TabWidget tabWidget : tabs)
        {
            boolean selected = false;
            Tab objTab = tabWidget.getTabControl();
            if (objTab.isSelected())
            {
                selected = true;
            }
            tabPane.getTabs().remove(objTab);
            tabPane.getTabs().add(index, objTab);
            if (selected)
            {
                SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
                selectionModel.select(index);
            }
            index++;
        }
    }

    public static void ReIndexTabs(TabPane tabPane)
    {
        TabWidget.sortTabs(tabPane);

        String Titles = "";
        int tabIndex = 0;
        for (Tab tab : tabPane.getTabs())
        {
            for (TabWidget tabWidget : ConfigurationReader.GetConfigReader().getTabs())
            {
                if (tabWidget.Reindex(tab, tabIndex))
                {
                    break;
                }
            }
            tabIndex++;
        }
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return basePane;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return basePane.getStylesheets();
    }

    public void setOnActivateTask(String taskID)
    {
        _TaskOnActivate = taskID;
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
            else if (node.getNodeName().equalsIgnoreCase("GenerateDataPoint"))
            {
                if (!ConfigurationReader.ReadGenerateDataPoints(node))
                {
                    //return null;
                }
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

    @Override
    public boolean PerformPostCreateActions(GridWidget parentGrid, boolean updateToolTipOnly)
    {
        if (getHeight() == 0 && this.getHeightPercentOfParentGrid() == 0)
        {
            setHeightPercentOfParentGrid(100);
        }
        if (null != _TaskOnActivate)
        {
            if (_tab.isSelected())
            {
                _IgnoreFirstSelect = true; // 1st tab will get the selection changed notification on startup, ignore it
            }
            _tab.setOnSelectionChanged(e
                    -> 
                    {
                        if (_tab.isSelected())
                        {
                            if (!_IgnoreFirstSelect)
                            {
                                TASKMAN.PerformTask(_TaskOnActivate);
                            }
                        }
                        _IgnoreFirstSelect = false;
            }
            );
        }

        return super.PerformPostCreateActions(parentGrid, updateToolTipOnly);
    }

    public void setCreatedOnDemand()
    {
        _CreatedOnDemand = true;
    }

    public boolean getCreatedOnDemand()
    {
        return _CreatedOnDemand;
    }
   
}
