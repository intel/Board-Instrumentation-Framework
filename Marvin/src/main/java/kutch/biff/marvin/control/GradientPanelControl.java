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
package kutch.biff.marvin.control;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;

public class GradientPanelControl extends Region {
    private float minValue;
    private float maxValue;
    private float value;
    private String label;
    private boolean showLabel;
    private boolean showValue;
    private List<GradientColor> gradientColors;
    private Pane panel;
    private Text labelText;
    private Text valueText;

    public GradientPanelControl() {
        this.minValue = 0.0f;
        this.maxValue = 100.0f;
        this.value = minValue;
        this.label = "";
        this.showLabel = true;
        this.showValue = true;
        this.gradientColors = new ArrayList<>();
        this.gradientColors.add(new GradientColor(Color.BLUE, 0.5f)); // Default colors with equal weight
        this.gradientColors.add(new GradientColor(Color.RED, 0.5f));
        this.panel = createPanel();
        getChildren().add(panel); // Add the panel to the region
    }

    private Pane createPanel() {
        VBox vbox = new VBox();

        // Apply CSS class for alignment and spacing
        vbox.getStyleClass().add("panel-content");

        // Optional label text
        labelText = new Text(label);
        labelText.getStyleClass().add("label-text");

        // Current value text
        valueText = new Text(String.format("Value: %.2f", value));
        valueText.getStyleClass().add("value-text");

        // Adding components to VBox based on the flags
        if (showLabel) {
            vbox.getChildren().add(labelText);
        }
        if (showValue) {
            vbox.getChildren().add(valueText);
        }

        // Creating panel and applying the gradient background and CSS class
        Pane widgetPanel = new Pane(vbox);
        widgetPanel.setPrefSize(200, 200);
        widgetPanel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
        widgetPanel.getStyleClass().add("gradient-panel");

                // Ensure VBox resizes with the Pane
        vbox.setFillWidth(true);
        vbox.prefWidthProperty().bind(widgetPanel.widthProperty());
        vbox.prefHeightProperty().bind(widgetPanel.heightProperty());

        return widgetPanel;
    }

    public void updateValue(float newValue) {
        this.value = newValue;
        valueText.setText(String.format("Value: %.2f", value));
        panel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
    }

    public void setLabel(String label) {
        this.label = label;
        labelText.setText(label);
        refreshPanel();
    }

    public String getLabel() {
        return this.label;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        refreshPanel();
    }

    public boolean isShowLabel() {
        return this.showLabel;
    }

    public void setShowValue(boolean showValue) {
        this.showValue = showValue;
        refreshPanel();
    }

    public boolean isShowValue() {
        return this.showValue;
    }

    public void setValue(float value) {
        updateValue(value);
    }

    public float getValue() {
        return this.value;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        updateValue(this.value); // Recalculate the color based on the new min value
    }

    public float getMinValue() {
        return this.minValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        updateValue(this.value); // Recalculate the color based on the new max value
    }

    public float getMaxValue() {
        return this.maxValue;
    }
    
    public void setGradientColors(List<GradientColor> gradientColors) {
        if (gradientColors == null || gradientColors.size() < 2) {
            throw new IllegalArgumentException("At least two colors with weights are required.");
        }
        this.gradientColors = new ArrayList<>(gradientColors);
        panel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
    }

    public List<GradientColor> getGradientColors() {
        return new ArrayList<>(this.gradientColors);
    }

    private void refreshPanel() {
//        getChildren().clear();
//        this.panel = createPanel();
//        getChildren().add(panel);
    }

    private Color getColorForValue(float value) {
        float ratio = (value - minValue) / (maxValue - minValue);
        float accumulatedWeight = 0;
        Color lowerColor = null;
        Color upperColor = null;
        float lowerBound = 0;
        float upperBound = 0;

        for (int i = 0; i < gradientColors.size(); i++) {
            GradientColor gc = gradientColors.get(i);
            upperBound = accumulatedWeight + gc.getWeight();

            if (ratio >= lowerBound && ratio <= upperBound) {
                lowerColor = gc.getColor();
                if (i == gradientColors.size() - 1) {
                    upperColor = lowerColor;
                } else {
                    upperColor = gradientColors.get(i + 1).getColor();
                }
                break;
            }

            accumulatedWeight = upperBound;
            lowerBound = upperBound;
        }

        if (lowerColor == null || upperColor == null) {
            return gradientColors.get(0).getColor(); // Fallback in case of any issue
        }

        float sectionRatio = (ratio - lowerBound) / (upperBound - lowerBound);
        return lowerColor.interpolate(upperColor, sectionRatio);
    }

    private String toHex(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
