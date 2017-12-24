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

import eu.hansolo.enzo.flippanel.FlipPanel;
import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.PanelSideInfo;
import static kutch.biff.marvin.widget.BaseWidget.CONFIG;
import static kutch.biff.marvin.widget.BaseWidget.LOGGER;
import kutch.biff.marvin.widget.dynamicgrid.DynamicGrid;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 *
 * @author Patrick Kutch
 */
public class FlipPanelWidget extends BaseWidget
{

    private GridWidget _BackGrid;
    private GridWidget _FrontGrid;
    private FlipPanel _Panel;
    private GridPane _FrontBaseGridPane;
    private GridPane _BackBaseGridPane;
    private Orientation _Orientation;
    private boolean _Front2Back = true;
    private PanelSideInfo _FrontInfo;
    private PanelSideInfo _BackInfo;
    private VBox _vFront;
    private VBox _vBack;
    private Button _frontBtn, _backBtn;
    private double _AnimationDuration;

    public FlipPanelWidget()
    {
        _BackGrid = null;
        _FrontGrid = null;
        _Panel = null;
        _frontBtn = null;
        _backBtn = null;
        _Orientation = Orientation.HORIZONTAL;
        _AnimationDuration = 700;
        setDefaultIsSquare(false);
    }

    public double getAnimationDuration()
    {
        return _AnimationDuration;
    }

    public void setAnimationDuration(double _AnimationDuration)
    {
        this._AnimationDuration = _AnimationDuration;
    }

    @Override
    public boolean Create(GridPane parentPane, DataManager dataMgr)
    {
        SetParent(parentPane);
        if (null == _BackGrid || null == _FrontGrid)
        {
            LOGGER.severe("Flip Panel needs both front and back definition.");
            return false;
        }
        if (false == SetupPanel(dataMgr))
        {
            return false;
        }

        ConfigureDimentions();
        parentPane.add(_Panel, getColumn(), getRow(), getColumnSpan(), getRowSpan()); // is a cycle since this is the parent of tab

        SetupPeekaboo(dataMgr);

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                String strVal = newVal.toString();
                Orientation orientation = getRequestedOrientation(strVal);
                if (strVal.equalsIgnoreCase("Flip"))
                {
                    DoFlip();
                }
                else if (strVal.equalsIgnoreCase("Front"))
                {
                    DoFlip(false, getOrientation());
                }
                else if (strVal.equalsIgnoreCase("Back"))
                {
                    DoFlip(true, getOrientation());
                }
                else if (strVal.length() > 4 && strVal.substring(0, 5).equalsIgnoreCase("Flip:")) // Flip, but with a direction
                {
                    if (null != orientation)
                    {
                        DoFlip(_Front2Back, orientation);
                    }

                }
                else if (strVal.length() > 4 && strVal.substring(0, 5).equalsIgnoreCase("Front:")) // Flip, but with a direction
                {
                    if (null != orientation)
                    {
                        DoFlip(false, orientation);
                    }

                }
                else if (strVal.length() > 4 && strVal.substring(0, 5).equalsIgnoreCase("Back:")) // Flip, but with a direction
                {
                    if (null != orientation)
                    {
                        DoFlip(true, orientation);
                    }
                }
                else
                {

                }

            }
        });

        return ApplyCSS();
    }

    private Orientation getRequestedOrientation(String strRequest)
    {
        String[] parts = strRequest.split(":");
        if (parts.length > 1)
        {
            if (parts[1].equalsIgnoreCase("Horizontal"))
            {
                return Orientation.HORIZONTAL;
            }
            if (parts[1].equalsIgnoreCase("Vertical"))
            {
                return Orientation.VERTICAL;
            }
            LOGGER.warning("Received invalid action for Flip Panel: " + strRequest);
        }
        return null;
    }

    private boolean SetupPanel(DataManager dataMgr)
    {
        boolean RetVal = true;
        _Panel = new FlipPanel(_Orientation);

        _Panel.setFlipTime(getAnimationDuration());
        _vFront = new VBox();
        _vBack = new VBox();

        _FrontBaseGridPane = new GridPane();
        _BackBaseGridPane = new GridPane();

        if (false == _FrontGrid.Create(_FrontBaseGridPane, dataMgr) || ! _FrontGrid.PerformPostCreateActions(getFrontGrid()))
        {
            return false;
        }
        if (false == _BackGrid.Create(_BackBaseGridPane, dataMgr) || ! _BackGrid.PerformPostCreateActions(_BackGrid))
        {
            return false;
        }
        _FrontBaseGridPane.setAlignment(_FrontGrid.getPosition());
        _BackBaseGridPane.setAlignment(_BackGrid.getPosition());
        _frontBtn = SetupPanelButton(_FrontInfo);
        _backBtn = SetupPanelButton(_BackInfo);

        _vFront.getChildren().add(_FrontBaseGridPane);
        _vBack.getChildren().add(_BackBaseGridPane);
        if (null != _frontBtn)
        {
            if (_FrontInfo.IsButtonOnTop())
            {
                _vFront.getChildren().add(0, _frontBtn);
            }
            else
            {
                _vFront.getChildren().add(1, _frontBtn);
            }
        }
        if (null != _backBtn)
        {
            if (_BackInfo.IsButtonOnTop())
            {
                _vBack.getChildren().add(0, _backBtn);
            }
            else
            {
                _vBack.getChildren().add(1, _backBtn);
            }
        }

        if (null != _FrontInfo)
        {
            _vFront.setAlignment(_FrontInfo.GetButtonAlignment());
        }
        if (null != _BackInfo)
        {
            _vBack.setAlignment(_BackInfo.GetButtonAlignment());
        }

        _Panel.getFront().getChildren().add(_vFront);
        _Panel.getBack().getChildren().add(_vBack);

        return RetVal;
    }

    private Button SetupPanelButton(PanelSideInfo info)
    {
        if (null == info)
        {
            return null;
        }
        Button _button = new Button(info.getButtonText());
        String strFile = "";

        strFile = strFile + getDefinintionFileDirectory() + File.separatorChar + info.getCSSFile();
        File file = new File(strFile);

        if (false == file.exists())
        {
            LOGGER.severe("Unable to locate FlipPanel Stylesheet: " + strFile);
        }
        else
        {
            LOGGER.config("Applying Stylesheet: " + GetCSS_File() + " to Widget.");
            String url = BaseWidget.convertToFileURL(strFile);
            if (false == _button.getStylesheets().add(url))
            {
                //       return null;
            }
            if (null != info.getStyleID())
            {
                _button.setId(info.getStyleID());
            }
        }
        _button.addEventHandler(MouseEvent.MOUSE_CLICKED, EVENT -> this.DoFlip());

        return _button;
    }

    private void DoFlip()
    {
        try
        {
            _Panel.setFlipDirection(getOrientation());
            if (_Front2Back)
            {
                _Panel.flipToBack();
            }
            else
            {
                _Panel.flipToFront();
            }
            _Front2Back = !_Front2Back;
        }
        catch (Exception ex)  // latest Enzo build works, but sometimes has an exeption in here
        {
        }
    }

    private void DoFlip(boolean toBack, Orientation orientation)
    {
        if (null != orientation) // am on one side and want to go to other
        {
            _Panel.setFlipDirection(orientation);
            if (_Front2Back)
            {
                _Panel.flipToBack();
            }
            else
            {
                _Panel.flipToFront();
            }
            _Front2Back = !_Front2Back;
        }
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        if (super.HandleWidgetSpecificSettings(node)) // see if anything in GridWidget (like padding override)
        {
            return true;
        }
        if (node.getNodeName().equalsIgnoreCase("Front"))
        {
            //_FrontGrid = WidgetBuilder.ReadGridInfo(node, new GridWidget());
            _FrontGrid = WidgetBuilder.BuildGrid(node, true);
            return _FrontGrid != null;
        }
        if (node.getNodeName().equalsIgnoreCase("Back"))
        {
            //_BackGrid =  WidgetBuilder.ReadGridInfo(node, new GridWidget());
            _BackGrid = WidgetBuilder.BuildGrid(node, true);
            return _BackGrid != null;
        }
        if (node.getNodeName().equalsIgnoreCase("AnimationDuration"))
        {
            String str = node.getTextContent();
            try
            {
                setAnimationDuration(Double.parseDouble(str));
                return true;
            }
            catch (NumberFormatException ex)
            {
                LOGGER.severe("Invlid value for <AnimationDuration> tag for FlipPanel Widget");
            }
        }
        if (node.getNodeName().equalsIgnoreCase("RotationOverride"))
        {
            String str = node.getTextContent();
            if (0 == str.compareToIgnoreCase("Horizontal"))
            {
                setOrientation(Orientation.HORIZONTAL);
            }
            else if (0 == str.compareToIgnoreCase("Vertical"))
            {
                setOrientation(Orientation.VERTICAL);
            }
            else
            {
                LOGGER.severe("Invalid Orientation in FlipPanel Orientation overvide. Should be Horizontal or Vertical, not : " + str);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public javafx.scene.Node getStylableObject()
    {
        return _Panel;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return _Panel.getStylesheets();
    }

    public Orientation getOrientation()
    {
        return _Orientation;
    }

    public void setOrientation(Orientation _Orientation)
    {
        this._Orientation = _Orientation;
    }

    public GridWidget getFrontGrid()
    {
        return _FrontGrid;
    }

    public void setFrontGrid(GridWidget _FrontGrid)
    {
        this._FrontGrid = _FrontGrid;
    }

    public PanelSideInfo getFrontInfo()
    {
        return _FrontInfo;
    }

    public void setFrontInfo(PanelSideInfo _FrontInfo)
    {
        this._FrontInfo = _FrontInfo;
    }

    public PanelSideInfo getBackInfo()
    {
        return _BackInfo;
    }

    public void setBackInfo(PanelSideInfo _BackInfo)
    {
        this._BackInfo = _BackInfo;
    }

    @Override
    protected boolean ApplyCSS()
    {
        //super.ApplyCSS(); // can do CSS for entire widget here, and below is for individual sides
        _BackGrid.setWidgetInformation(getDefinintionFileDirectory(),null,"FlipPanel");
        _FrontGrid.setWidgetInformation(getDefinintionFileDirectory(),null,"FlipPanel");
        if (null == _FrontGrid.GetCSS_File())
        {
            _FrontGrid.setBaseCSSFilename(getBaseCSSFilename());
        }
        if (null == _BackGrid.GetCSS_File())
        {
            _BackGrid.setBaseCSSFilename(getBaseCSSFilename());
        }

        _FrontGrid.ApplyCSS();
        _BackGrid.ApplyCSS();

        _vFront.getStylesheets().clear();
        _vBack.getStylesheets().clear();

        if (null != getStyleID())
        {
            _vFront.setId(getStyleID());
            _vBack.setId(getStyleID());
        }
        if (null != GetCSS_File())
        {
            _vFront.getStylesheets().add(GetCSS_File());
            _vBack.getStylesheets().add(GetCSS_File());
        }
        ApplyStyleOverrides(_vFront, getStyleOverride());
        ApplyStyleOverrides(_vBack, getStyleOverride());
        ApplyStyleOverrides(_vFront, _FrontGrid.getStyleOverride());
        ApplyStyleOverrides(_vBack, _BackGrid.getStyleOverride());
        return true;
    }
    @Override
    public void UpdateTitle(String strTitle)
    {
        LOGGER.warning("Tried to update Title of a FlipPanel Widget to " + strTitle);
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
            Tooltip.install(_vFront, _objToolTip);
            Tooltip.install(_vBack, _objToolTip);
        }

        return true;
    }    
}
