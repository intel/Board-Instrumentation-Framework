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

import java.text.NumberFormat;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class TextWidget extends BaseWidget {
    private String _TextString;
    private Label _TextControl;
    protected Group _Group;
    private boolean _ScaleToFitBounderies;
    private boolean _NumericFormat = false;
    private boolean _MonetaryFormat = false;
    private boolean _PercentFormat = false;
    private String _Suffix = "";
    boolean DecimalsSet = false;

    public TextWidget() {
        _TextString = null;
        _TextControl = new Label();
        _TextControl.setAlignment(Pos.CENTER);
        _ScaleToFitBounderies = false;
        setDefaultIsSquare(false);
    }

    @Override
    public void ConfigureAlignment() {
        super.ConfigureAlignment();
        _TextControl.setAlignment(getPosition());
        GridPane.setValignment(_Group, getVerticalPosition());
        GridPane.setHalignment(_Group, getHorizontalPosition());
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (null != _TextString) {
            _TextControl.setText(_TextString);
        }

        ConfigureDimentions();
        _TextControl.setScaleShape(getScaleToFitBounderies());

        // By adding it to a group, the underlying java framework will properly
        // clip and resize bounderies if rotated
        _Group = new Group(_TextControl);
        ConfigureAlignment();
        SetupPeekaboo(dataMgr);

        pane.add(_Group, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> o, Object oldVal, Object newVal) {
                if (IsPaused()) {
                    return;
                }

                _TextString = newVal.toString();
                if (DecimalsSet) {
                    try {
                        String fmtString = "%." + Integer.toString(getDecimalPlaces()) + "f";
                        float fVal = Float.parseFloat(_TextString);
                        _TextString = String.format(fmtString, fVal);
                    } catch (Exception ex) {

                    }
                }
                if (true == _NumericFormat) {
                    try {
                        if (_TextString.contains(".")) // good bet it's a float
                        {
                            _TextString = NumberFormat.getNumberInstance().format(Float.parseFloat(_TextString));
                        } else {
                            _TextString = NumberFormat.getNumberInstance().format(Long.parseLong(_TextString));
                        }
                    } catch (Exception Ex) {
                        // System.out.println(Ex.toString());
                    }
                } else if (true == _MonetaryFormat) {
                    if (_TextString.contains(".")) // good bet it's a float
                    {
                        _TextString = NumberFormat.getCurrencyInstance().format(Float.parseFloat(_TextString));
                    } else {
                        _TextString = NumberFormat.getCurrencyInstance().format(Long.parseLong(_TextString));

                    }
                } else if (true == _PercentFormat) {
                    if (_TextString.contains(".")) // good bet it's a float
                    {
                        _TextString = NumberFormat.getPercentInstance().format(Float.parseFloat(_TextString));
                    } else {
                        _TextString = NumberFormat.getPercentInstance().format(Long.parseLong(_TextString));

                    }
                }
                _TextString += _Suffix;

                if (_TextString.length() < 2) // seems a single character won't display - bug in Java
                {
                    _TextString += " ";
                }
                _TextControl.setText(_TextString);

            }
        });

        SetupTaskAction();
        return ApplyCSS();
    }

    @Override
    public javafx.scene.Node getRemovableNode() {
        return _Group;
    }

    public boolean getScaleToFitBounderies() {
        return _ScaleToFitBounderies;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return _TextControl;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _TextControl.getStylesheets();
    }

    @Override
    public void HandleCustomStyleOverride(FrameworkNode styleNode) {
        if (styleNode.hasAttribute("ScaleToShape")) {
            String str = styleNode.getAttribute("ScaleToShape");
            if (0 == str.compareToIgnoreCase("True")) {
                setScaleToFitBounderies(true);
            } else if (0 == str.compareToIgnoreCase("False")) {
                setScaleToFitBounderies(false);
            } else {
                LOGGER.severe(
                        "Invalid StyleOVerride Elment ScaleToShape for Text .  ScaleToShape should be True or False, not:"
                                + str);
            }
        }
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if (node.getNodeName().equalsIgnoreCase("Format")) {
            if (node.hasAttribute("Type")) {
                String Type = node.getAttribute("Type");
                if (Type.equalsIgnoreCase("Number")) {
                    _NumericFormat = true;
                } else if (Type.equalsIgnoreCase("Money")) {
                    _MonetaryFormat = true;
                } else if (Type.equalsIgnoreCase("Percent")) {
                    _PercentFormat = true;
                } else {
                    LOGGER.warning("Unknown Text Widget Format type: " + Type);
                }
            } else {
                LOGGER.severe("Text widget Format option has no Type");
                return true;
            }
            if (node.hasAttribute("Suffix")) {
                _Suffix = node.getAttribute("Suffix");
            }
            return true;
        }
        return false;
    }

    @Override
    public void setDecimalPlaces(int _DecimalPlaces) {
        super.setDecimalPlaces(_DecimalPlaces);
        this.DecimalsSet = true;
    }

    @Override
    public void SetInitialValue(String value) {
        _TextString = value;
    }

    public void setScaleToFitBounderies(boolean _ScaleToFitBounderies) {
        this._ScaleToFitBounderies = _ScaleToFitBounderies;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a TextWidget to " + strTitle);
    }

}
