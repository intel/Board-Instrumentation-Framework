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

import eu.hansolo.enzo.common.Section;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;


/**
 * Created by hansolo on 02.12.15.
 */
public class DoubleRadialGaugeBuilder<B extends DoubleRadialGaugeBuilder<B>> {
    private HashMap<String, Property> properties = new HashMap<>();


    // ******************** Constructors **************************************
    protected DoubleRadialGaugeBuilder() {}


    // ******************** Methods *******************************************
    public static final DoubleRadialGaugeBuilder create() {
        return new DoubleRadialGaugeBuilder();
    }
    
    public final B animated(final boolean ANIMATED) {
        properties.put("animated", new SimpleBooleanProperty(ANIMATED));
        return (B)this;
    }

    public final B animationDuration(final double ANIMATION_DURATION) {
        properties.put("animationDuration", new SimpleDoubleProperty(ANIMATION_DURATION));
        return (B)this;
    }

    public final B style(final String STYLE) {
        properties.put("style", new SimpleStringProperty(STYLE));
        return (B)this;
    }

    public final B styleClass(final String... STYLES) {
        properties.put("styleClass", new SimpleObjectProperty<>(STYLES));
        return (B)this;
    }

    public final B dropShadowEnabled(final boolean DROP_SHADOW_ENABLED) {
        properties.put("dropShadowEnabled", new SimpleBooleanProperty(DROP_SHADOW_ENABLED));
        return (B)this;
    }

    public final B valueOne(final double VALUE) {
        properties.put("valueOne", new SimpleDoubleProperty(VALUE));
        return (B) this;
    }

    public final B minValueOne(final double MIN_VALUE) {
        properties.put("minValueOne", new SimpleDoubleProperty(MIN_VALUE));
        return (B) this;
    }

    public final B maxValueOne(final double MAX_VALUE) {
        properties.put("maxValueOne", new SimpleDoubleProperty(MAX_VALUE));
        return (B) this;
    }

    public final B decimalsOne(final int DECIMALS) {
        properties.put("decimalsOne", new SimpleIntegerProperty(DECIMALS));
        return (B) this;
    }

    public final B titleOne(final String TITLE) {
        properties.put("titleOne", new SimpleStringProperty(TITLE));
        return (B)this;
    }

    public final B unitOne(final String UNIT) {
        properties.put("unitOne", new SimpleStringProperty(UNIT));
        return (B)this;
    }
    
    public final B autoScaleOne(final boolean AUTO_SCALE) {
        properties.put("autoScaleOne", new SimpleBooleanProperty(AUTO_SCALE));
        return (B)this;
    }

    public final B needleColorOne(final Color NEEDLE_COLOR) {
        properties.put("needleColorOne", new SimpleObjectProperty<>(NEEDLE_COLOR));
        return (B)this;
    }

    public final B tickLabelOrientationOne(final RadialGauge.TickLabelOrientation TICK_LABEL_ORIENTATION) {
        properties.put("tickLabelOrientationOne", new SimpleObjectProperty<>(TICK_LABEL_ORIENTATION));
        return (B)this;
    }

    public final B numberFormatOne(final RadialGauge.NumberFormat NUMBER_FORMAT) {
        properties.put("numberFormatOne", new SimpleObjectProperty<>(NUMBER_FORMAT));
        return (B)this;
    }

    public final B majorTickSpaceOne(final double MAJOR_TICK_SPACE) {
        properties.put("majorTickSpaceOne", new SimpleDoubleProperty(MAJOR_TICK_SPACE));
        return (B)this;
    }

    public final B minorTickSpaceOne(final double MINOR_TICK_SPACE) {
        properties.put("minorTickSpaceOne", new SimpleDoubleProperty(MINOR_TICK_SPACE));
        return (B)this;
    }

    public final B tickLabelFillOne(final Color TICK_LABEL_FILL) {
        properties.put("tickLabelFillOne", new SimpleObjectProperty<>(TICK_LABEL_FILL));
        return (B)this;
    }

    public final B tickMarkFillOne(final Color TICK_MARKER_FILL) {
        properties.put("tickMarkFillOne", new SimpleObjectProperty<>(TICK_MARKER_FILL));
        return (B)this;
    }
    
    public final B ledColorOne(final Color LED_COLOR) {
        properties.put("ledColorOne", new SimpleObjectProperty<>(LED_COLOR));
        return (B)this;
    }

    public final B ledVisibleOne(final boolean LED_VISIBLE) {
        properties.put("ledVisibleOne", new SimpleBooleanProperty(LED_VISIBLE));
        return (B) this;
    }

    public final B valueVisibleOne(final boolean VALUE_VISIBLE) {
        properties.put("valueVisibleOne", new SimpleBooleanProperty(VALUE_VISIBLE));
        return (B) this;
    }

    public final B sectionsOne(final Section... SECTIONS) {
        properties.put("sectionsArrayOne", new SimpleObjectProperty<>(SECTIONS));
        return (B)this;
    }

    public final B sectionsOne(final List<Section> SECTIONS) {
        properties.put("sectionsListOne", new SimpleObjectProperty<>(SECTIONS));
        return (B)this;
    }

    public final B areasOne(final Section... AREAS) {
        properties.put("areasArrayOne", new SimpleObjectProperty<>(AREAS));
        return (B)this;
    }

    public final B areasOne(final List<Section> AREAS) {
        properties.put("areasListOne", new SimpleObjectProperty<>(AREAS));
        return (B)this;
    }

    public final B sectionsVisibleOne(final boolean SECTIONS_VISIBLE) {
        properties.put("sectionsVisibleOne", new SimpleBooleanProperty(SECTIONS_VISIBLE));
        return (B)this;
    }

    public final B areasVisibleOne(final boolean AREAS_VISIBLE) {
        properties.put("areasVisibleOne", new SimpleBooleanProperty(AREAS_VISIBLE));
        return (B)this;
    }

    public final B sectionFill0One(final Color SECTION_0_FILL) {
        properties.put("sectionFill0One", new SimpleObjectProperty<>(SECTION_0_FILL));
        return (B)this;
    }

    public final B sectionFill1One(final Color SECTION_1_FILL) {
        properties.put("sectionFill1One", new SimpleObjectProperty<>(SECTION_1_FILL));
        return (B)this;
    }

    public final B sectionFill2One(final Color SECTION_2_FILL) {
        properties.put("sectionFill2One", new SimpleObjectProperty<>(SECTION_2_FILL));
        return (B)this;
    }

    public final B sectionFill3One(final Color SECTION_3_FILL) {
        properties.put("sectionFill3One", new SimpleObjectProperty<>(SECTION_3_FILL));
        return (B)this;
    }

    public final B sectionFill4One(final Color SECTION_4_FILL) {
        properties.put("sectionFill4One", new SimpleObjectProperty<>(SECTION_4_FILL));
        return (B)this;
    }
    
    public final B areaFill0One(final Color AREA_0_FILL) {
        properties.put("areaFill0One", new SimpleObjectProperty<>(AREA_0_FILL));
        return (B)this;
    }

    public final B areaFill1One(final Color AREA_1_FILL) {
        properties.put("areaFill1One", new SimpleObjectProperty<>(AREA_1_FILL));
        return (B)this;
    }

    public final B areaFill2One(final Color AREA_2_FILL) {
        properties.put("areaFill2One", new SimpleObjectProperty<>(AREA_2_FILL));
        return (B)this;
    }

    public final B areaFill3One(final Color AREA_3_FILL) {
        properties.put("areaFill3One", new SimpleObjectProperty<>(AREA_3_FILL));
        return (B)this;
    }

    public final B areaFill4One(final Color AREA_4_FILL) {
        properties.put("areaFill4One", new SimpleObjectProperty<>(AREA_4_FILL));
        return (B)this;
    }


    public final B valueTwo(final double VALUE) {
        properties.put("valueTwo", new SimpleDoubleProperty(VALUE));
        return (B) this;
    }

    public final B minValueTwo(final double MIN_VALUE) {
        properties.put("minValueTwo", new SimpleDoubleProperty(MIN_VALUE));
        return (B) this;
    }

    public final B maxValueTwo(final double MAX_VALUE) {
        properties.put("maxValueTwo", new SimpleDoubleProperty(MAX_VALUE));
        return (B) this;
    }

    public final B decimalsTwo(final int DECIMALS) {
        properties.put("decimalsTwo", new SimpleIntegerProperty(DECIMALS));
        return (B) this;
    }

    public final B titleTwo(final String TITLE) {
        properties.put("titleTwo", new SimpleStringProperty(TITLE));
        return (B)this;
    }

    public final B unitTwo(final String UNIT) {
        properties.put("unitTwo", new SimpleStringProperty(UNIT));
        return (B)this;
    }

    public final B autoScaleTwo(final boolean AUTO_SCALE) {
        properties.put("autoScaleTwo", new SimpleBooleanProperty(AUTO_SCALE));
        return (B)this;
    }

    public final B needleColorTwo(final Color NEEDLE_COLOR) {
        properties.put("needleColorTwo", new SimpleObjectProperty<>(NEEDLE_COLOR));
        return (B)this;
    }

    public final B tickLabelOrientationTwo(final RadialGauge.TickLabelOrientation TICK_LABEL_ORIENTATION) {
        properties.put("tickLabelOrientationTwo", new SimpleObjectProperty<>(TICK_LABEL_ORIENTATION));
        return (B)this;
    }

    public final B numberFormatTwo(final RadialGauge.NumberFormat NUMBER_FORMAT) {
        properties.put("numberFormatTwo", new SimpleObjectProperty<>(NUMBER_FORMAT));
        return (B)this;
    }

    public final B majorTickSpaceTwo(final double MAJOR_TICK_SPACE) {
        properties.put("majorTickSpaceTwo", new SimpleDoubleProperty(MAJOR_TICK_SPACE));
        return (B)this;
    }

    public final B minorTickSpaceTwo(final double MINOR_TICK_SPACE) {
        properties.put("minorTickSpaceTwo", new SimpleDoubleProperty(MINOR_TICK_SPACE));
        return (B)this;
    }

    public final B tickLabelFillTwo(final Color TICK_LABEL_FILL) {
        properties.put("tickLabelFillTwo", new SimpleObjectProperty<>(TICK_LABEL_FILL));
        return (B)this;
    }

    public final B tickMarkFillTwo(final Color TICK_MARKER_FILL) {
        properties.put("tickMarkFillTwo", new SimpleObjectProperty<>(TICK_MARKER_FILL));
        return (B)this;
    }

    public final B ledColorTwo(final Color LED_COLOR) {
        properties.put("ledColorTwo", new SimpleObjectProperty<>(LED_COLOR));
        return (B)this;
    }

    public final B ledVisibleTwo(final boolean LED_VISIBLE) {
        properties.put("ledVisibleTwo", new SimpleBooleanProperty(LED_VISIBLE));
        return (B) this;
    }

    public final B valueVisibleTwo(final boolean VALUE_VISIBLE) {
        properties.put("valueVisibleTwo", new SimpleBooleanProperty(VALUE_VISIBLE));
        return (B) this;
    }

    public final B sectionsTwo(final Section... SECTIONS) {
        properties.put("sectionsArrayTwo", new SimpleObjectProperty<>(SECTIONS));
        return (B)this;
    }

    public final B sectionsTwo(final List<Section> SECTIONS) {
        properties.put("sectionsListTwo", new SimpleObjectProperty<>(SECTIONS));
        return (B)this;
    }

    public final B areasTwo(final Section... AREAS) {
        properties.put("areasArrayTwo", new SimpleObjectProperty<>(AREAS));
        return (B)this;
    }

    public final B areasTwo(final List<Section> AREAS) {
        properties.put("areasListTwo", new SimpleObjectProperty<>(AREAS));
        return (B)this;
    }

    public final B sectionsVisibleTwo(final boolean SECTIONS_VISIBLE) {
        properties.put("sectionsVisibleTwo", new SimpleBooleanProperty(SECTIONS_VISIBLE));
        return (B)this;
    }

    public final B areasVisibleTwo(final boolean AREAS_VISIBLE) {
        properties.put("areasVisibleTwo", new SimpleBooleanProperty(AREAS_VISIBLE));
        return (B)this;
    }

    public final B sectionFill0Two(final Color SECTION_0_FILL) {
        properties.put("sectionFill0Two", new SimpleObjectProperty<>(SECTION_0_FILL));
        return (B)this;
    }

    public final B sectionFill1Two(final Color SECTION_1_FILL) {
        properties.put("sectionFill1Two", new SimpleObjectProperty<>(SECTION_1_FILL));
        return (B)this;
    }

    public final B sectionFill2Two(final Color SECTION_2_FILL) {
        properties.put("sectionFill2Two", new SimpleObjectProperty<>(SECTION_2_FILL));
        return (B)this;
    }

    public final B sectionFill3Two(final Color SECTION_3_FILL) {
        properties.put("sectionFill3Two", new SimpleObjectProperty<>(SECTION_3_FILL));
        return (B)this;
    }

    public final B sectionFill4Two(final Color SECTION_4_FILL) {
        properties.put("sectionFill4Two", new SimpleObjectProperty<>(SECTION_4_FILL));
        return (B)this;
    }

    public final B areaFill0Two(final Color AREA_0_FILL) {
        properties.put("areaFill0Two", new SimpleObjectProperty<>(AREA_0_FILL));
        return (B)this;
    }

    public final B areaFill1Two(final Color AREA_1_FILL) {
        properties.put("areaFill1Two", new SimpleObjectProperty<>(AREA_1_FILL));
        return (B)this;
    }

    public final B areaFill2Two(final Color AREA_2_FILL) {
        properties.put("areaFill2Two", new SimpleObjectProperty<>(AREA_2_FILL));
        return (B)this;
    }

    public final B areaFill3Two(final Color AREA_3_FILL) {
        properties.put("areaFill3Two", new SimpleObjectProperty<>(AREA_3_FILL));
        return (B)this;
    }

    public final B areaFill4Two(final Color AREA_4_FILL) {
        properties.put("areaFill4Two", new SimpleObjectProperty<>(AREA_4_FILL));
        return (B)this;
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

    public final DoubleRadialGauge build() {
        final DoubleRadialGauge CONTROL = new DoubleRadialGauge();

        // Make sure that sections and markers will be added first
        if (properties.keySet().contains("sectionsArrayOne")) {
            CONTROL.setSectionsOne(((ObjectProperty<Section[]>) properties.get("sectionsArrayOne")).get());
        }
        if(properties.keySet().contains("sectionsListOne")) {
            CONTROL.setSectionsOne(((ObjectProperty<List<Section>>) properties.get("sectionsListOne")).get());
        }
        if (properties.keySet().contains("areasArrayOne")) {
            CONTROL.setAreasOne(((ObjectProperty<Section[]>) properties.get("areasArrayOne")).get());
        }
        if(properties.keySet().contains("areasListOne")) {
            CONTROL.setAreasOne(((ObjectProperty<List<Section>>) properties.get("areasListOne")).get());
        }

        if (properties.keySet().contains("sectionsArrayTwo")) {
            CONTROL.setSectionsTwo(((ObjectProperty<Section[]>) properties.get("sectionsArrayTwo")).get());
        }
        if(properties.keySet().contains("sectionsListTwo")) {
            CONTROL.setSectionsTwo(((ObjectProperty<List<Section>>) properties.get("sectionsListTwo")).get());
        }
        if (properties.keySet().contains("areasArrayTwo")) {
            CONTROL.setAreasTwo(((ObjectProperty<Section[]>) properties.get("areasArrayTwo")).get());
        }
        if(properties.keySet().contains("areasListTwo")) {
            CONTROL.setAreasTwo(((ObjectProperty<List<Section>>) properties.get("areasListTwo")).get());
        }

        for (String key : properties.keySet()) {
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
            } else if("styleClass".equals(key)) {
                CONTROL.getStyleClass().setAll("gauge");
                CONTROL.getStyleClass().addAll(((ObjectProperty<String[]>) properties.get(key)).get());
            } else if("animated".equals(key)) {
                CONTROL.setAnimated(((BooleanProperty) properties.get(key)).get());
            } else if("dropShadowEnabled".equals(key)) {
                CONTROL.setDropShadowEnabled(((BooleanProperty) properties.get(key)).get());
            } else if("animationDuration".equals(key)) {
                CONTROL.setAnimationDuration(((DoubleProperty) properties.get(key)).get());
            } else if ("style".equals(key)) {
                CONTROL.setStyle(((StringProperty) properties.get(key)).get());
            } else if("minValueOne".equals(key)) {
                CONTROL.setMinValueOne(((DoubleProperty) properties.get(key)).get());
            } else if("maxValueOne".equals(key)) {
                CONTROL.setMaxValueOne(((DoubleProperty) properties.get(key)).get());
            } else if("valueOne".equals(key)) {
                CONTROL.setValueOne(((DoubleProperty) properties.get(key)).get());
            } else if("decimalsOne".equals(key)) {
                CONTROL.setDecimalsOne(((IntegerProperty) properties.get(key)).get());
            } else if("titleOne".equals(key)) {
                CONTROL.setTitleOne(((StringProperty) properties.get(key)).get());
            } else if("unitOne".equals(key)) {
                CONTROL.setUnitOne(((StringProperty) properties.get(key)).get());
            } else if ("autoScaleOne".equals(key)) {
                CONTROL.setAutoScaleOne(((BooleanProperty) properties.get(key)).get());
            } else if("needleColorOne".equals(key)) {
                CONTROL.setNeedleColorOne(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("tickLabelOrientationOne".equals(key)) {
                CONTROL.setTickLabelOrientationOne(((ObjectProperty<DoubleRadialGauge.TickLabelOrientation>) properties.get(key)).get());
            } else if("numberFormatOne".equals(key)) {
                CONTROL.setNumberFormatOne(((ObjectProperty<DoubleRadialGauge.NumberFormat>) properties.get(key)).get());
            } else if("majorTickSpaceOne".equals(key)) {
                CONTROL.setMajorTickSpaceOne(((DoubleProperty) properties.get(key)).get());
            } else if("minorTickSpaceOne".equals(key)) {
                CONTROL.setMinorTickSpaceOne(((DoubleProperty) properties.get(key)).get());
            } else if("tickLabelFill".equals(key)) {
                CONTROL.setTickLabelFillOne(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("tickMarkFillOne".equals(key)) {
                CONTROL.setTickMarkFillOne(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("ledColorOne".equals(key)) {
                CONTROL.setLedColorOne(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("ledVisibleOne".equals(key)) {
                CONTROL.setLedVisibleOne(((BooleanProperty) properties.get(key)).get());
            } else if ("valueVisibleOne".equals(key)) {
                CONTROL.setValueVisibleOne(((BooleanProperty) properties.get(key)).get());
            } else if("sectionFill0One".equals(key)) {
                CONTROL.setSectionFill0One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill1One".equals(key)) {
                CONTROL.setSectionFill1One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill2One".equals(key)) {
                CONTROL.setSectionFill2One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill3One".equals(key)) {
                CONTROL.setSectionFill3One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill4One".equals(key)) {
                CONTROL.setSectionFill4One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill0One".equals(key)) {
                CONTROL.setAreaFill0One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill1One".equals(key)) {
                CONTROL.setAreaFill1One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill2One".equals(key)) {
                CONTROL.setAreaFill2One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill3One".equals(key)) {
                CONTROL.setAreaFill3One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill4One".equals(key)) {
                CONTROL.setAreaFill4One(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("minValueTwo".equals(key)) {
                CONTROL.setMinValueTwo(((DoubleProperty) properties.get(key)).get());
            } else if("maxValueTwo".equals(key)) {
                CONTROL.setMaxValueTwo(((DoubleProperty) properties.get(key)).get());
            } else if("valueTwo".equals(key)) {
                CONTROL.setValueTwo(((DoubleProperty) properties.get(key)).get());
            } else if("decimalsTwo".equals(key)) {
                CONTROL.setDecimalsTwo(((IntegerProperty) properties.get(key)).get());
            } else if("titleTwo".equals(key)) {
                CONTROL.setTitleTwo(((StringProperty) properties.get(key)).get());
            } else if("unitTwo".equals(key)) {
                CONTROL.setUnitTwo(((StringProperty) properties.get(key)).get());
            } else if ("autoScaleTwo".equals(key)) {
                CONTROL.setAutoScaleTwo(((BooleanProperty) properties.get(key)).get());
            } else if("needleColorTwo".equals(key)) {
                CONTROL.setNeedleColorTwo(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("tickLabelOrientationTwo".equals(key)) {
                CONTROL.setTickLabelOrientationTwo(((ObjectProperty<DoubleRadialGauge.TickLabelOrientation>) properties.get(key)).get());
            } else if("numberFormatTwo".equals(key)) {
                CONTROL.setNumberFormatTwo(((ObjectProperty<DoubleRadialGauge.NumberFormat>) properties.get(key)).get());
            } else if("majorTickSpaceTwo".equals(key)) {
                CONTROL.setMajorTickSpaceTwo(((DoubleProperty) properties.get(key)).get());
            } else if("minorTickSpaceTwo".equals(key)) {
                CONTROL.setMinorTickSpaceTwo(((DoubleProperty) properties.get(key)).get());
            } else if("tickLabelFill".equals(key)) {
                CONTROL.setTickLabelFillTwo(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("tickMarkFillTwo".equals(key)) {
                CONTROL.setTickMarkFillTwo(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("ledColorTwo".equals(key)) {
                CONTROL.setLedColorTwo(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("ledVisibleTwo".equals(key)) {
                CONTROL.setLedVisibleTwo(((BooleanProperty) properties.get(key)).get());
            } else if ("valueVisibleTwo".equals(key)) {
                CONTROL.setValueVisibleTwo(((BooleanProperty) properties.get(key)).get());
            } else if("sectionFill0Two".equals(key)) {
                CONTROL.setSectionFill0Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill1Two".equals(key)) {
                CONTROL.setSectionFill1Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill2Two".equals(key)) {
                CONTROL.setSectionFill2Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill3Two".equals(key)) {
                CONTROL.setSectionFill3Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("sectionFill4Two".equals(key)) {
                CONTROL.setSectionFill4Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill0Two".equals(key)) {
                CONTROL.setAreaFill0Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill1Two".equals(key)) {
                CONTROL.setAreaFill1Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill2Two".equals(key)) {
                CONTROL.setAreaFill2Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill3Two".equals(key)) {
                CONTROL.setAreaFill3Two(((ObjectProperty<Color>) properties.get(key)).get());
            } else if("areaFill4Two".equals(key)) {
                CONTROL.setAreaFill4Two(((ObjectProperty<Color>) properties.get(key)).get());
            }
        }
        return CONTROL;
    }
}
