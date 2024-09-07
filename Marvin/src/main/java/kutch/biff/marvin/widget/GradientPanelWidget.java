/*
 * ##############################################################################
 * #  Copyright (c) 2024 Intel Corporation
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

import java.util.List;
import java.util.logging.Level;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.control.GradientColor;
import kutch.biff.marvin.control.GradientPanelControl;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.widgetbuilder.GradientPanelWidgetBuilder;

/**
 *
 * @author Patrick
 */
public class GradientPanelWidget extends BaseWidget {

    private final GradientPanelControl _gradientPanel;

    public GradientPanelWidget() {
        _gradientPanel = new GradientPanelControl();
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        SetupPeekaboo(dataMgr);
        pane.add(_gradientPanel, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        _gradientPanel.setLabel(getTitle());
        _gradientPanel.setDecimalPlaces(getDecimalPlaces());
        setDefaultIsSquare(false);
        SetParent(pane);
        ConfigureDimentions();
        ConfigureAlignment();
        _gradientPanel.setLabel(getTitle());

        dataMgr.AddListener(getMinionID(), getNamespace(), (o, oldVal, newVal) -> {
            if (IsPaused()) {
                return;
            }

            String strVal = newVal.toString();
            try {
                float newValue = Float.parseFloat(strVal);
                _gradientPanel.setValue(newValue, getStyleOverride());
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.SEVERE, "Invalid data for GradientPanel received: {0}", strVal);
            }
        });
        boolean retVal = ApplyCSS();
        _gradientPanel.setValue(_gradientPanel.getMinValue(), getStyleOverride());
        return retVal;
    }

    @Override
    public boolean HandleValueRange(FrameworkNode rangeNode) {
        double Min = -1234.5678;
        double Max = -1234.5678;
        if (rangeNode.hasAttribute("Min")) {
            Min = rangeNode.getDoubleAttribute("Min", Min);
            if (Min == -1234.5678) {
                return false;
            }
            _gradientPanel.setMinValue((float) Min);
        }
        if (rangeNode.hasAttribute("Max")) {
            Max = rangeNode.getDoubleAttribute("Max", Max);
            if (Max == -1234.5678) {
                return false;
            }
            _gradientPanel.setMaxValue((float) Max);
        }
        return true;
    }

    @Override
    public Node getStylableObject() {
        return _gradientPanel.getStylableObject();
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _gradientPanel.getStylesheets();
    }

    @Override
    public void UpdateTitle(String newTitle) {
        _gradientPanel.setLabel(newTitle);
    }

    public void setMinValue(float newVal) {
        _gradientPanel.setMinValue(newVal);
    }

    public void setMaxValue(float newVal) {
        _gradientPanel.setMaxValue(newVal);
    }

    public void setGradientColors(List<GradientColor> gradientColors) {
        _gradientPanel.setGradientColors(gradientColors);
    }

    public void setShowValue(boolean newVal) {
        _gradientPanel.setShowValue(newVal);
    }

    public void setShowLabel(boolean newVal) {
        _gradientPanel.setShowLabel(newVal);
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetNode) {
        if (widgetNode.getNodeName().equalsIgnoreCase("Colors")) {
            List<GradientColor> colors = GradientPanelWidgetBuilder.readColors(widgetNode);
            if (colors != null && colors.size() > 1) {
                _gradientPanel.setGradientColors(colors);
                return true;
            }
        } else if (widgetNode.getNodeName().equalsIgnoreCase("ShowValue")) {
            boolean show = widgetNode.getBooleanValue();
            _gradientPanel.setShowValue(show);
            return true;
        } else if (widgetNode.getNodeName().equalsIgnoreCase("ShowTitle")) {
            boolean show = widgetNode.getBooleanValue();
            _gradientPanel.setShowLabel(show);
            return true;
        }

        return false;
    }
}
