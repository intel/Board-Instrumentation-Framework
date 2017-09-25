/*
 * Copyright (c) 2015 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.enzo.gauge;

import javafx.beans.property.*;
import javafx.geometry.Dimension2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import java.util.HashMap;


/**
 * Created by hansolo on 19.10.15.
 */
public class FlatGaugeBuilder<B extends FlatGaugeBuilder<B>> {
    private HashMap<String, Property> properties = new HashMap<>();
    private Stop[] stops;


    // ******************** Constructors **************************************
    protected FlatGaugeBuilder() {}


    // ******************** Methods *******************************************
    public static final FlatGaugeBuilder create() {
        return new FlatGaugeBuilder();
    }

    public final B value(final double VALUE) {
        properties.put("value", new SimpleDoubleProperty(VALUE));
        return (B) this;
    }

    public final B minValue(final double MIN_VALUE) {
        properties.put("minValue", new SimpleDoubleProperty(MIN_VALUE));
        return (B) this;
    }

    public final B maxValue(final double MAX_VALUE) {
        properties.put("maxValue", new SimpleDoubleProperty(MAX_VALUE));
        return (B) this;
    }

    public final B decimals(final int DECIMALS) {
        properties.put("decimals", new SimpleIntegerProperty(DECIMALS));
        return (B) this;
    }

    public final B title(final String TITLE) {
        properties.put("title", new SimpleStringProperty(TITLE));
        return (B)this;
    }

    public final B unit(final String UNIT) {
        properties.put("unit", new SimpleStringProperty(UNIT));
        return (B)this;
    }

    public final B animationDuration(final int ANIMATION_DURATION) {
        properties.put("animationDuration", new SimpleIntegerProperty(ANIMATION_DURATION));
        return (B)this;
    }

    public final B barColor(final Color COLOR) {
        properties.put("barColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B backgroundColor(final Color COLOR) {
        properties.put("backgroundColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B titleTextColor(final Color COLOR) {
        properties.put("titleTextColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B valueTextColor(final Color COLOR) {
        properties.put("valueTextColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B unitTextColor(final Color COLOR) {
        properties.put("innerValueTextColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B separatorColor(final Color COLOR) {
        properties.put("separatorColor", new SimpleObjectProperty<>(COLOR));
        return (B) this;
    }

    public final B separatorVisible(final boolean SEPARATOR_VISIBLE) {
        properties.put("separatorVisible", new SimpleBooleanProperty(SEPARATOR_VISIBLE));
        return (B) this;
    }

    public final B colorRingVisible(final boolean COLOR_RING_VISIBLE) {
        properties.put("colorRingVisible", new SimpleBooleanProperty(COLOR_RING_VISIBLE));
        return (B) this;
    }

    public final B multiColor(final boolean MULTI_COLOR) {
        properties.put("multiColor", new SimpleBooleanProperty(MULTI_COLOR));
        return (B) this;
    }

    public final B gradient(final Stop... STOPS) {
        stops = STOPS;
        return (B) this;
    }

    public final B prefSize(final double WIDTH, final double HEIGHT) {
        properties.put("prefSize", new SimpleObjectProperty<>(new Dimension2D(WIDTH, HEIGHT)));
        return (B)this;
    }
    public final B minSize(final double WIDTH, final double HEIGHT) {
        properties.put("minSize", new SimpleObjectProperty<>(new Dimension2D(WIDTH, HEIGHT)));
        return (B)this;
    }
    public final B maxSize(final double WIDTH, final double HEIGHT) {
        properties.put("maxSize", new SimpleObjectProperty<>(new Dimension2D(WIDTH, HEIGHT)));
        return (B)this;
    }

    public final B prefWidth(final double PREF_WIDTH) {
        properties.put("prefWidth", new SimpleDoubleProperty(PREF_WIDTH));
        return (B)this;
    }
    public final B prefHeight(final double PREF_HEIGHT) {
        properties.put("prefHeight", new SimpleDoubleProperty(PREF_HEIGHT));
        return (B)this;
    }

    public final B minWidth(final double MIN_WIDTH) {
        properties.put("minWidth", new SimpleDoubleProperty(MIN_WIDTH));
        return (B)this;
    }
    public final B minHeight(final double MIN_HEIGHT) {
        properties.put("minHeight", new SimpleDoubleProperty(MIN_HEIGHT));
        return (B)this;
    }

    public final B maxWidth(final double MAX_WIDTH) {
        properties.put("maxWidth", new SimpleDoubleProperty(MAX_WIDTH));
        return (B)this;
    }
    public final B maxHeight(final double MAX_HEIGHT) {
        properties.put("maxHeight", new SimpleDoubleProperty(MAX_HEIGHT));
        return (B)this;
    }

    public final B scaleX(final double SCALE_X) {
        properties.put("scaleX", new SimpleDoubleProperty(SCALE_X));
        return (B)this;
    }
    public final B scaleY(final double SCALE_Y) {
        properties.put("scaleY", new SimpleDoubleProperty(SCALE_Y));
        return (B)this;
    }

    public final B layoutX(final double LAYOUT_X) {
        properties.put("layoutX", new SimpleDoubleProperty(LAYOUT_X));
        return (B)this;
    }
    public final B layoutY(final double LAYOUT_Y) {
        properties.put("layoutY", new SimpleDoubleProperty(LAYOUT_Y));
        return (B)this;
    }

    public final B translateX(final double TRANSLATE_X) {
        properties.put("translateX", new SimpleDoubleProperty(TRANSLATE_X));
        return (B)this;
    }
    public final B translateY(final double TRANSLATE_Y) {
        properties.put("translateY", new SimpleDoubleProperty(TRANSLATE_Y));
        return (B)this;
    }

    public final B opacity(final double OPACITY) {
        properties.put("opacity", new SimpleDoubleProperty(clamp(0, 1, OPACITY)));
        return (B) this;
    }

    private double clamp(final double MIN, final double MAX, final double VALUE) {
        if (VALUE < MIN) return MIN;
        if (VALUE > MAX) return MAX;
        return VALUE;
    }


    public final FlatGauge build() {
        final FlatGauge CONTROL = new FlatGauge();
        properties.forEach((key, property) -> {
            if ("prefSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                CONTROL.setPrefSize(dim.getWidth(), dim.getHeight());
            } else if("minSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                CONTROL.setPrefSize(dim.getWidth(), dim.getHeight());
            } else if("maxSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                CONTROL.setPrefSize(dim.getWidth(), dim.getHeight());
            } else if("prefWidth".equals(key)) {
                CONTROL.setPrefWidth(((DoubleProperty) properties.get(key)).get());
            } else if("prefHeight".equals(key)) {
                CONTROL.setPrefHeight(((DoubleProperty) properties.get(key)).get());
            } else if("minWidth".equals(key)) {
                CONTROL.setMinWidth(((DoubleProperty) properties.get(key)).get());
            } else if("minHeight".equals(key)) {
                CONTROL.setMinHeight(((DoubleProperty) properties.get(key)).get());
            } else if("maxWidth".equals(key)) {
                CONTROL.setMaxWidth(((DoubleProperty) properties.get(key)).get());
            } else if("maxHeight".equals(key)) {
                CONTROL.setMaxHeight(((DoubleProperty) properties.get(key)).get());
            } else if("scaleX".equals(key)) {
                CONTROL.setScaleX(((DoubleProperty) properties.get(key)).get());
            } else if("scaleY".equals(key)) {
                CONTROL.setScaleY(((DoubleProperty) properties.get(key)).get());
            } else if ("layoutX".equals(key)) {
                CONTROL.setLayoutX(((DoubleProperty) properties.get(key)).get());
            } else if ("layoutY".equals(key)) {
                CONTROL.setLayoutY(((DoubleProperty) properties.get(key)).get());
            } else if ("translateX".equals(key)) {
                CONTROL.setTranslateX(((DoubleProperty) properties.get(key)).get());
            } else if ("translateY".equals(key)) {
                CONTROL.setTranslateY(((DoubleProperty) properties.get(key)).get());
            } else if ("opacity".equals(key)) {
                CONTROL.setOpacity(((DoubleProperty) properties.get(key)).get());
            } else if("styleClass".equals(key)) {
                CONTROL.getStyleClass().setAll("flat-gauge");
                CONTROL.getStyleClass().addAll(((ObjectProperty<String[]>) properties.get(key)).get());
            } else if("minValue".equals(key)) {
                CONTROL.setMinValue(((DoubleProperty) properties.get(key)).get());
            } else if("maxValue".equals(key)) {
                CONTROL.setMaxValue(((DoubleProperty) properties.get(key)).get());
            } else if("value".equals(key)) {
                CONTROL.setValue(((DoubleProperty) properties.get(key)).get());
            } else if("decimals".equals(key)) {
                CONTROL.setDecimals(((IntegerProperty) properties.get(key)).get());
            } else if("title".equals(key)) {
                CONTROL.setTitle(((StringProperty) properties.get(key)).get());
            } else if("unit".equals(key)) {
                CONTROL.setUnit(((StringProperty) properties.get(key)).get());
            } else if ("animationDuration".equals(key)) {
                CONTROL.setAnimationDurationInMs(((IntegerProperty) properties.get(key)).get());
            } else if("barColor".equals(key)) {
                CONTROL.setBarColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("backgroundColor".equals(key)) {
                CONTROL.setBackgroundColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("titleTextColor".equals(key)) {
                CONTROL.setTitleTextColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("valueTextColor".equals(key)) {
                CONTROL.setValueTextColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("unitTextColor".equals(key)) {
                CONTROL.setUnitTextColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("separatorColor".equals(key)) {
                CONTROL.setSeparatorColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("multiColor".equals(key)) {
                CONTROL.setMultiColor(((BooleanProperty) properties.get(key)).get());
            } else if ("separatorVisible".equals(key)) {
                CONTROL.setSeparatorVisible(((BooleanProperty) properties.get(key)).get());
            } else if ("colorRingVisible".equals(key)) {
                CONTROL.setColorRingVisible(((BooleanProperty) properties.get(key)).get());
            }
        });
        if (null != stops) CONTROL.setGradient(stops);

        return CONTROL;
    }
}
