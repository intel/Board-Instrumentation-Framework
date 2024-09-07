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
import javafx.scene.paint.Color;
/**
 * Represents a gradient color with a specific color and weight.
 */
public class GradientColor {
    private final Color color;
    private final float weight;

    public GradientColor(Color color, float weight) {
        this.color = color;
        this.weight = weight;
    }

    public Color getColor() {
        return color;
    }

    public float getWeight() {
        return weight;
    }
}
