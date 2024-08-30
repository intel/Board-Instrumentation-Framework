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

public class GradientPanelControl extends Region {

    private static final float MIN_VALUE = 0.0f;
    private static final float MAX_VALUE = 100.0f;

    private float value;
    private String label;
    private boolean showLabel;
    private boolean showValue;
    private Color color1;
    private Color color2;
    private Pane panel;
    private Text labelText;
    private Text valueText;

    public GradientPanelControl() {
        this.value = MIN_VALUE;
        this.label = "";
        this.showLabel = true;
        this.showValue = true;
        this.color1 = Color.BLUE;
        this.color2 = Color.RED;
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
        Pane panel = new Pane(vbox);
        panel.setPrefSize(200, 200);
        panel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
        panel.getStyleClass().add("gradient-panel");

        return panel;
    }

    public void updateValue(float newValue) {
        this.value = newValue;
        valueText.setText(String.format("Value: %.2f", value));
        panel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
    }

    public void setLabel(String label) {
        this.label = label;
        labelText.setText(label);
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

    public void setColors(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
        panel.setStyle("-fx-background-color: " + toHex(getColorForValue(value)) + ";");
    }

    public Color getColor1() {
        return this.color1;
    }

    public Color getColor2() {
        return this.color2;
    }

    private void refreshPanel() {
        getChildren().clear();
        this.panel = createPanel();
        getChildren().add(panel);
    }

    private Color getColorForValue(float value) {
        float ratio = (value - MIN_VALUE) / (MAX_VALUE - MIN_VALUE);
        return color1.interpolate(color2, ratio);
    }

    private String toHex(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
