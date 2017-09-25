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

package eu.hansolo.enzo.clock.skin;

import eu.hansolo.enzo.clock.Clock;
import eu.hansolo.enzo.fonts.Fonts;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;


/**
 * User: hansolo
 * Date: 31.10.12
 * Time: 14:18
 */
public class ClockSkin extends SkinBase<Clock> implements Skin<Clock> {
    private static final int    SHORT_INTERVAL   = 20;
    private static final int    LONG_INTERVAL    = 1000;
    private static final double PREFERRED_WIDTH  = 200;
    private static final double PREFERRED_HEIGHT = 200;
    private static final double MINIMUM_WIDTH    = 50;
    private static final double MINIMUM_HEIGHT   = 50;
    private static final double MAXIMUM_WIDTH    = 1024;
    private static final double MAXIMUM_HEIGHT   = 1024;
    private Pane            pane;
    private String          nightDayStyleClass;
    private Region          background;
    private Canvas          logoLayer;
    private GraphicsContext ctx;
    private Region          minutePointer;
    private Region          minutePointerFlour;
    private Region          hourPointer;
    private Region          hourPointerFlour;
    private Region          secondPointer;
    private Region          centerKnob;
    private Region          foreground;
    private double          size;
    private double          hourPointerWidthFactor;
    private double          hourPointerHeightFactor;
    private double          minutePointerWidthFactor;
    private double          minutePointerHeightFactor;
    private double          secondPointerWidthFactor;
    private double          secondPointerHeightFactor;
    private double          majorTickWidthFactor;
    private double          majorTickHeightFactor;
    private double          minorTickWidthFactor;
    private double          minorTickHeightFactor;
    private double          majorTickOffset;
    private double          minorTickOffset;
    private Rotate          hourRotate;
    private Rotate          minuteRotate;    
    private Rotate          secondRotate;
    private List<Region>    ticks;
    private List<Text>      tickLabels;
    private Group           tickMarkGroup;
    private Group           tickLabelGroup;
    private Group           pointerGroup;
    private Group           secondPointerGroup;
    private Font            tickLabelFont;
    private DoubleProperty  currentMinuteAngle;
    private DoubleProperty  minuteAngle;
    private Timeline        timeline;


    // ******************** Constructors **************************************
    public ClockSkin(final Clock CONTROL) {
        super(CONTROL);
        nightDayStyleClass        = getSkinnable().isNightMode() ? "night-mode" : "day-mode";

        hourPointerWidthFactor    = 0.04;
        hourPointerHeightFactor   = 0.55;
        minutePointerWidthFactor  = 0.04;
        minutePointerHeightFactor = 0.4;
        secondPointerWidthFactor  = 0.075;
        secondPointerHeightFactor = 0.46;

        majorTickWidthFactor      = 0.04;
        majorTickHeightFactor     = 0.12;
        minorTickWidthFactor      = 0.01;
        minorTickHeightFactor     = 0.05;

        majorTickOffset           = 0.018;
        minorTickOffset           = 0.05;

        tickLabelFont             = Fonts.bebasNeue(12);        
        minuteAngle               = new SimpleDoubleProperty(0);
        currentMinuteAngle        = new SimpleDoubleProperty(0);

        minuteRotate              = new Rotate();
        hourRotate                = new Rotate();
        secondRotate              = new Rotate();

        ticks                     = new ArrayList<>(60);
        tickLabels                = new ArrayList<>(12);

        timeline                  = new Timeline();

        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getSkinnable().getWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getHeight(), 0.0) <= 0) {
            if (getSkinnable().getPrefWidth() > 0 && getSkinnable().getPrefHeight() > 0) {
                getSkinnable().setPrefSize(getSkinnable().getPrefWidth(), getSkinnable().getPrefHeight());
            } else {
                getSkinnable().setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(), 0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <= 0) {
            getSkinnable().setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        pane    = new Pane();

        background = new Region();
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            background.getStyleClass().setAll("background-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            background.getStyleClass().setAll("background-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            background.getStyleClass().setAll("background-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            background.getStyleClass().setAll("background-bosch");
        }

        logoLayer = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctx       = logoLayer.getGraphicsContext2D();

        String majorTickStyleClass;
        String minorTickStyleClass;
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            majorTickStyleClass = "major-tick-ios6";
            minorTickStyleClass = "minor-tick-ios6";
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            majorTickStyleClass = "major-tick-db";
            minorTickStyleClass = "minor-tick-db";
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            majorTickStyleClass = "major-tick-bosch";
            minorTickStyleClass = "minor-tick-bosch";
        } else {
            majorTickStyleClass = "major-tick-braun";
            minorTickStyleClass = "minor-tick-braun";
        }

        int tickLabelCounter = 1;
        for (double angle = 0 ; angle < 360 ; angle += 6) {
            Region tick = new Region();
            if (angle % 30 == 0) {
                tick.getStyleClass().setAll(majorTickStyleClass);
                Text tickLabel = new Text(Integer.toString(tickLabelCounter));
                tickLabel.getStyleClass().setAll("tick-label-braun");
                tickLabels.add(tickLabel);
                tickLabelCounter++;
            } else {
                tick.getStyleClass().setAll(minorTickStyleClass);
            }
            ticks.add(tick);
        }

        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.65));
        dropShadow.setRadius(1.5);
        dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
        dropShadow.setOffsetY(1);

        tickMarkGroup = new Group();
        tickMarkGroup.setEffect(dropShadow);
        tickMarkGroup.getChildren().setAll(ticks);

        tickLabelGroup = new Group();
        tickLabelGroup.setEffect(dropShadow);
        tickLabelGroup.getChildren().setAll(tickLabels);
        tickLabelGroup.setOpacity(Clock.Design.BRAUN == getSkinnable().getDesign() ? 1 : 0);

        minutePointer = new Region();
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            minutePointer.getStyleClass().setAll("hour-pointer-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            minutePointer.getStyleClass().setAll("hour-pointer-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            minutePointer.getStyleClass().setAll("hour-pointer-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            minutePointer.getStyleClass().setAll("hour-pointer-bosch");
        }
        minutePointer.getTransforms().setAll(minuteRotate);

        minutePointerFlour = new Region();
        minutePointerFlour.getStyleClass().setAll("hour-pointer-braun-flour");
        if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            minutePointerFlour.setOpacity(1);
        } else {
            minutePointerFlour.setOpacity(0);
        }
        minutePointerFlour.getTransforms().setAll(minuteRotate);

        hourPointer = new Region();
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            hourPointer.getStyleClass().setAll("minute-pointer-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            hourPointer.getStyleClass().setAll("minute-pointer-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            hourPointer.getStyleClass().setAll("minute-pointer-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            hourPointer.getStyleClass().setAll("minute-pointer-bosch");
        }
        hourPointer.getTransforms().setAll(hourRotate);

        hourPointerFlour = new Region();
        hourPointerFlour.getStyleClass().setAll("minute-pointer-braun-flour");
        if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            hourPointerFlour.setOpacity(1);
        } else {
            hourPointerFlour.setOpacity(0);
        }
        hourPointerFlour.getTransforms().setAll(hourRotate);

        DropShadow pointerShadow = new DropShadow();
        pointerShadow.setColor(Color.rgb(0, 0, 0, 0.45));
        pointerShadow.setRadius(12);
        pointerShadow.setBlurType(BlurType.TWO_PASS_BOX);
        pointerShadow.setOffsetY(6);

        pointerGroup = new Group();
        pointerGroup.setEffect(pointerShadow);
        pointerGroup.getChildren().setAll(hourPointerFlour, hourPointer, minutePointerFlour, minutePointer);

        secondPointer = new Region();
        secondPointer.setOpacity(1);
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            secondPointer.getStyleClass().setAll("second-pointer-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            secondPointer.getStyleClass().setAll("second-pointer-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            secondPointer.getStyleClass().setAll("second-pointer-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            secondPointer.setOpacity(0);
        }
        secondPointer.getTransforms().setAll(secondRotate);

        InnerShadow secondPointerInnerShadow = new InnerShadow();
        secondPointerInnerShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        secondPointerInnerShadow.setRadius(1);
        secondPointerInnerShadow.setBlurType(BlurType.TWO_PASS_BOX);
        secondPointerInnerShadow.setOffsetY(-1);

        InnerShadow secondPointerInnerHighlight = new InnerShadow();
        secondPointerInnerHighlight.setColor(Color.rgb(255, 255, 255, 0.3));
        secondPointerInnerHighlight.setRadius(1);
        secondPointerInnerHighlight.setBlurType(BlurType.TWO_PASS_BOX);
        secondPointerInnerHighlight.setOffsetY(1);
        secondPointerInnerHighlight.setInput(secondPointerInnerShadow);

        DropShadow secondPointerShadow = new DropShadow();
        secondPointerShadow.setColor(Color.rgb(0, 0, 0, 0.45));
        secondPointerShadow.setRadius(12);
        secondPointerShadow.setBlurType(BlurType.TWO_PASS_BOX);
        secondPointerShadow.setOffsetY(6);
        secondPointerShadow.setInput(secondPointerInnerHighlight);

        secondPointerGroup = new Group();
        secondPointerGroup.setEffect(secondPointerShadow);
        secondPointerGroup.getChildren().setAll(secondPointer);
        secondPointerGroup.setOpacity(getSkinnable().isSecondPointerVisible() ? 1 : 0);

        centerKnob = new Region();
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            centerKnob.getStyleClass().setAll("center-knob-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            centerKnob.getStyleClass().setAll("center-knob-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            centerKnob.getStyleClass().setAll("center-knob-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            centerKnob.getStyleClass().setAll("center-knob-bosch");
        }

        foreground = new Region();
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
            foreground.getStyleClass().setAll("foreground-ios6");
        } else if (Clock.Design.DB == getSkinnable().getDesign()) {
            foreground.getStyleClass().setAll("foreground-db");
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            foreground.getStyleClass().setAll("foreground-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            foreground.getStyleClass().setAll("foreground-bosch");
        }
        foreground.setOpacity(getSkinnable().isHighlightVisible() ? 1 : 0);

        pane.getChildren().setAll(background, logoLayer, tickMarkGroup, tickLabelGroup, pointerGroup, secondPointerGroup, centerKnob, foreground);

        getChildren().setAll(pane);

        updateDesign();
    }

    private void registerListeners() {
        minuteRotate.angleProperty().bind(currentMinuteAngle);
        minuteAngle.addListener(observable -> moveMinutePointer(minuteAngle.get()));        
        
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().secondPointerVisibleProperty().addListener(observable -> handleControlPropertyChanged("SECOND_POINTER_VISIBLE"));
        getSkinnable().nightModeProperty().addListener(observable -> handleControlPropertyChanged("DESIGN"));
        getSkinnable().designProperty().addListener(observable -> handleControlPropertyChanged("DESIGN"));
        getSkinnable().highlightVisibleProperty().addListener(observable -> handleControlPropertyChanged("DESIGN"));
        getSkinnable().timeProperty().addListener(observable -> handleControlPropertyChanged("TIME"));
        getSkinnable().runningProperty().addListener(observable -> handleControlPropertyChanged("RUNNING"));        
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("DESIGN".equals(PROPERTY)) {
            updateDesign();
        } else if ("SECOND_POINTER_VISIBLE".equals(PROPERTY)) {
            secondPointerGroup.setOpacity(getSkinnable().isSecondPointerVisible() ? 1 : 0);
        } else if ("TIME".equals(PROPERTY)) {
            updateTime();
        }
    }
    
    private void updateTime() {
        LocalTime TIME = getSkinnable().getTime();
        // Seconds
        if (getSkinnable().isDiscreteSecond()) {
            secondRotate.setAngle(TIME.getSecond() * 6);
        } else {
            secondRotate.setAngle(TIME.getSecond() * 6 + TIME.get(ChronoField.MILLI_OF_SECOND) * 0.006);
        }
        // Minutes
        minuteAngle.set(TIME.getMinute() * 6);
        // Hours                
        hourRotate.setAngle(0.5 * (60 * TIME.getHour() + TIME.getMinute()));
        
        if (getSkinnable().isAutoNightMode()) checkForNight(TIME);
    }

    private void checkForNight(final LocalTime TIME) {
        int hour   = TIME.getHour();
        int minute = TIME.getMinute();

        if (0 <= hour && minute >= 0 && hour <= 5 && minute <= 59|| 17 <= hour && minute <= 59 && hour <= 23 && minute <= 59) {
            getSkinnable().setNightMode(true);
        } else {
            getSkinnable().setNightMode(false);
        }
    }

    private void moveMinutePointer(final double ANGLE) {
        /*
        final KeyValue kv = new KeyValue(currentMinuteAngle, ANGLE, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
        final KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
        timeline = new Timeline();
        timeline.getKeyFrames().add(kf);
        timeline.play();
        */
        KeyValue kv = new KeyValue(currentMinuteAngle, ANGLE, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
        if ((int) currentMinuteAngle.get() == 354 && ANGLE == 0) {
            kv = new KeyValue(currentMinuteAngle, 360, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
        } else if ((int) currentMinuteAngle.get() == 0 && ANGLE == 354) {
            kv = new KeyValue(currentMinuteAngle, -6, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
        }        
        final KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
        timeline = new Timeline();
        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(event -> {
            if ((int) currentMinuteAngle.get() == 360) {
                currentMinuteAngle.set(0);
            } else if ((int) currentMinuteAngle.get() == -6) {
                currentMinuteAngle.set(354);
            }
        });
        timeline.play();
    }
        

    // ******************** Drawing related ***********************************    
    private void drawLogoLayer() {
        ctx.clearRect(0, 0, size, size);
        if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            ctx.setFill(getSkinnable().isNightMode() ? Color.rgb(240, 240, 240) : Color.rgb(10, 10, 10));
            ctx.fillRect(size * 0.5 - 1, size * 0.18, 2, size * 0.27);
            ctx.fillRect(size * 0.5 - 1, size * 0.55, 2, size * 0.27);
            ctx.fillRect(size * 0.18, size * 0.5 - 1, size * 0.27, 2);
            ctx.fillRect(size * 0.55, size * 0.5 - 1, size * 0.27, 2);
        }
        if (getSkinnable().getText().isEmpty()) return;
        ctx.setFill(getSkinnable().isNightMode() ? Color.WHITE : Color.BLACK);
        ctx.setFont(Fonts.robotoMedium(size * 0.05));
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setTextAlign(TextAlignment.CENTER);
        ctx.fillText(getSkinnable().getText(), size * 0.5, size * 0.675, size * 0.8);
    }

    private void updateDesign() {
        boolean wasRunning = getSkinnable().isRunning();
        if (wasRunning) { getSkinnable().setRunning(false); }
                
        // Set day or night mode
        nightDayStyleClass = getSkinnable().isNightMode() ? "night-mode" : "day-mode";
        // Set Styles for each component
        if (Clock.Design.IOS6 == getSkinnable().getDesign()) {          
            background.getStyleClass().setAll(nightDayStyleClass, "background-ios6");
            int index = 0;
            for (double angle = 0 ; angle < 360 ; angle += 6) {
                Region tick = ticks.get(index);
                if (angle % 30 == 0) {
                    tick.getStyleClass().setAll(nightDayStyleClass, "major-tick-ios6");
                } else {
                    tick.getStyleClass().setAll(nightDayStyleClass, "minor-tick-ios6");
                }
                ticks.add(tick);
                index++;
            }
            minutePointer.getStyleClass().setAll(nightDayStyleClass, "hour-pointer-ios6");
            hourPointer.getStyleClass().setAll(nightDayStyleClass, "minute-pointer-ios6");
            secondPointer.getStyleClass().setAll(nightDayStyleClass, "second-pointer-ios6");
            centerKnob.getStyleClass().setAll(nightDayStyleClass, "center-knob-ios6");
            foreground.getStyleClass().setAll(nightDayStyleClass, "foreground-ios6");            
        } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
            nightDayStyleClass = getSkinnable().isNightMode() ? "night-mode-braun" : "day-mode-braun";
            background.getStyleClass().setAll(nightDayStyleClass, "background-braun");
            int index = 0;
            for (double angle = 0 ; angle < 360 ; angle += 6) {
                if (angle % 30 == 0) {
                    ticks.get(index).getStyleClass().setAll(nightDayStyleClass, "major-tick-braun");
                } else {
                    ticks.get(index).getStyleClass().setAll(nightDayStyleClass, "minor-tick-braun");
                }
                index++;
            }
            for (index = 0 ; index < 12 ; index++) {
                tickLabels.get(index).getStyleClass().setAll(nightDayStyleClass, "tick-label-braun");
            }
            minutePointer.getStyleClass().setAll(nightDayStyleClass, "hour-pointer-braun");
            hourPointer.getStyleClass().setAll(nightDayStyleClass, "minute-pointer-braun");
            secondPointer.getStyleClass().setAll(nightDayStyleClass, "second-pointer-braun");
            centerKnob.getStyleClass().setAll(nightDayStyleClass, "center-knob-braun");
            foreground.getStyleClass().setAll(nightDayStyleClass, "foreground-braun");
        } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
            nightDayStyleClass = getSkinnable().isNightMode() ? "night-mode-bosch" : "day-mode-bosch";
            background.getStyleClass().setAll(nightDayStyleClass, "background-bosch");
            int index = 0;
            for (double angle = 0 ; angle < 360 ; angle += 6) {
                Region tick = ticks.get(index);
                if (angle % 30 == 0) {
                    tick.getStyleClass().setAll(nightDayStyleClass, "major-tick-bosch");
                } else {
                    tick.getStyleClass().setAll(nightDayStyleClass, "minor-tick-bosch");
                }
                ticks.add(tick);
                index++;
            }
            minutePointer.getStyleClass().setAll(nightDayStyleClass, "hour-pointer-bosch");
            hourPointer.getStyleClass().setAll(nightDayStyleClass, "minute-pointer-bosch");
            secondPointer.getStyleClass().setAll(nightDayStyleClass, "second-pointer-bosch");
            centerKnob.getStyleClass().setAll(nightDayStyleClass, "center-knob-bosch");
            foreground.getStyleClass().setAll(nightDayStyleClass, "foreground-bosch");
        } else {
            background.getStyleClass().setAll(nightDayStyleClass, "background-db");
            int index = 0;
            for (double angle = 0 ; angle < 360 ; angle += 6) {
                Region tick = ticks.get(index);
                if (angle % 30 == 0) {
                    tick.getStyleClass().setAll(nightDayStyleClass, "major-tick-db");
                } else {
                    tick.getStyleClass().setAll(nightDayStyleClass, "minor-tick-db");
                }
                ticks.add(tick);
                index++;
            }
            minutePointer.getStyleClass().setAll(nightDayStyleClass, "hour-pointer-db");
            hourPointer.getStyleClass().setAll(nightDayStyleClass, "minute-pointer-db");
            secondPointer.getStyleClass().setAll(nightDayStyleClass, "second-pointer-db");
            centerKnob.getStyleClass().setAll(nightDayStyleClass, "center-knob-db");
            foreground.getStyleClass().setAll(nightDayStyleClass, "foreground-db");
        }
        tickLabelGroup.setOpacity(Clock.Design.BRAUN == getSkinnable().getDesign() ? 1 : 0);
        foreground.setOpacity(getSkinnable().isHighlightVisible() ? 1 : 0);
        resize();
        
        if (wasRunning) { getSkinnable().setRunning(true); }
    }    
    
    private void resize() {
        size = getSkinnable().getWidth() < getSkinnable().getHeight() ? getSkinnable().getWidth() : getSkinnable().getHeight();

        logoLayer.setWidth(size);
        logoLayer.setHeight(size);

        if (size > 0) {
            pane.setMaxSize(size, size);
            
            background.setPrefSize(size, size);
            
            if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
                hourPointerWidthFactor    = 0.04;
                hourPointerHeightFactor   = 0.55;
                minutePointerWidthFactor  = 0.04;
                minutePointerHeightFactor = 0.4;
                secondPointerWidthFactor  = 0.075;
                secondPointerHeightFactor = 0.46;
                majorTickWidthFactor      = 0.04;
                majorTickHeightFactor     = 0.12;
                minorTickWidthFactor      = 0.01;
                minorTickHeightFactor     = 0.05;
                majorTickOffset           = 0.018;
                minorTickOffset           = 0.05;
                minuteRotate.setPivotX(size * 0.5 * hourPointerWidthFactor);
                minuteRotate.setPivotY(size * 0.76 * hourPointerHeightFactor);
                hourRotate.setPivotX(size * 0.5 * minutePointerWidthFactor);
                hourRotate.setPivotY(size * 0.66 * minutePointerHeightFactor);
                secondRotate.setPivotX(size * 0.5 * secondPointerWidthFactor);
                secondRotate.setPivotY(size * 0.7341040462 * secondPointerHeightFactor);
            } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
                hourPointerWidthFactor    = 0.105;
                hourPointerHeightFactor   = 0.485;
                minutePointerWidthFactor  = 0.105;
                minutePointerHeightFactor = 0.4;
                secondPointerWidthFactor  = 0.09;
                secondPointerHeightFactor = 0.53;
                majorTickWidthFactor      = 0.015;
                majorTickHeightFactor     = 0.045;
                minorTickWidthFactor      = 0.0075;
                minorTickHeightFactor     = 0.0225;
                majorTickOffset           = 0.012;
                minorTickOffset           = 0.02;
                minuteRotate.setPivotX(size * 0.5 * hourPointerWidthFactor);
                minuteRotate.setPivotY(size * 0.895 * hourPointerHeightFactor);
                hourRotate.setPivotX(size * 0.5 * minutePointerWidthFactor);
                hourRotate.setPivotY(size * 0.87 * minutePointerHeightFactor);
                secondRotate.setPivotX(size * 0.5 * secondPointerWidthFactor);
                secondRotate.setPivotY(size * 0.8125 * secondPointerHeightFactor);
            } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
                hourPointerWidthFactor    = 0.04;
                hourPointerHeightFactor   = 0.54;
                minutePointerWidthFactor  = 0.04;
                minutePointerHeightFactor = 0.38;
                secondPointerWidthFactor  = 0.09;
                secondPointerHeightFactor = 0.53;
                majorTickWidthFactor      = 0.02;
                majorTickHeightFactor     = 0.145;
                minorTickWidthFactor      = 0.006;
                minorTickHeightFactor     = 0.07;
                majorTickOffset           = 0.005;
                minorTickOffset           = 0.04;
                minuteRotate.setPivotX(size * 0.5 * hourPointerWidthFactor);
                minuteRotate.setPivotY(size * 0.8240740741 * hourPointerHeightFactor);
                hourRotate.setPivotX(size * 0.5 * minutePointerWidthFactor);
                hourRotate.setPivotY(size * 0.75 * minutePointerHeightFactor);
                secondRotate.setPivotX(size * 0.5 * secondPointerWidthFactor);
                secondRotate.setPivotY(size * 0.8125 * secondPointerHeightFactor);
            } else {
                hourPointerWidthFactor    = 0.04;
                hourPointerHeightFactor   = 0.47;
                minutePointerWidthFactor  = 0.055;
                minutePointerHeightFactor = 0.33;
                secondPointerWidthFactor  = 0.1;
                secondPointerHeightFactor = 0.455;
                majorTickWidthFactor      = 0.04;
                majorTickHeightFactor     = 0.12;
                minorTickWidthFactor      = 0.025;
                minorTickHeightFactor     = 0.04;
                majorTickOffset           = 0.018;
                minorTickOffset           = 0.06;
                minuteRotate.setPivotX(size * 0.5 * hourPointerWidthFactor);
                minuteRotate.setPivotY(size * hourPointerHeightFactor);
                hourRotate.setPivotX(size * 0.5 * minutePointerWidthFactor);
                hourRotate.setPivotY(size * minutePointerHeightFactor);
                secondRotate.setPivotX(size * 0.5 * secondPointerWidthFactor);
                secondRotate.setPivotY(size * secondPointerHeightFactor);
            }

            drawLogoLayer();

            double radius = 0.4;
            double sinValue;
            double cosValue;
            int index = 0;
            for (double angle = 0 ; angle < 360 ; angle += 6) {
                sinValue = Math.sin(Math.toRadians(angle));
                cosValue = Math.cos(Math.toRadians(angle));
                Region tick = ticks.get(index);
                if (angle % 30 == 0) {
                    tick.setPrefWidth(size * majorTickWidthFactor);
                    tick.setPrefHeight(size * majorTickHeightFactor);                    
                    tick.relocate(size * 0.5 + ((size * (radius + majorTickOffset) * sinValue) - (size * (majorTickWidthFactor) * 0.5)),
                                  size * 0.5 + ((size * (radius + majorTickOffset) * cosValue) - (size * (majorTickHeightFactor) * 0.5)));
                } else {
                    tick.setPrefWidth(size * minorTickWidthFactor);
                    tick.setPrefHeight(size * minorTickHeightFactor);                    
                    tick.relocate(size * 0.5 + ((size * (radius + minorTickOffset) * sinValue) - (size * (minorTickWidthFactor) * 0.5)),
                                  size * 0.5 + ((size * (radius + minorTickOffset) * cosValue) - (size * (minorTickHeightFactor) * 0.5)));
                }
                tick.setRotate(-angle);
                index++;
            }

            if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
                int tickLabelCounter = 0;
                //tickLabelFont = Font.loadFont(getClass().getResourceAsStream("/eu/hansolo/enzo/fonts/helvetica.ttf"), (0.075 * size));
                tickLabelFont = Font.font("Bebas Neue", FontWeight.THIN, FontPosture.REGULAR, 0.09 * size);
                for (double angle = 0 ; angle < 360 ; angle += 30.0) {
                    double x = 0.34 * size * Math.sin(Math.toRadians(150 - angle));
                    double y = 0.34 * size * Math.cos(Math.toRadians(150 - angle));
                    tickLabels.get(tickLabelCounter).setFont(tickLabelFont);
                    tickLabels.get(tickLabelCounter).setX(size * 0.5 + x - tickLabels.get(tickLabelCounter).getLayoutBounds().getWidth() * 0.5);
                    tickLabels.get(tickLabelCounter).setY(size * 0.5 + y);
                    tickLabels.get(tickLabelCounter).setTextOrigin(VPos.CENTER);
                    tickLabels.get(tickLabelCounter).setTextAlignment(TextAlignment.CENTER);
                    tickLabelCounter++;
                }
            }

            minutePointer.setPrefSize(size * hourPointerWidthFactor, size * hourPointerHeightFactor);
            if (Clock.Design.IOS6 == getSkinnable().getDesign()) {                
                minutePointer.relocate(size * 0.5 - (minutePointer.getPrefWidth() * 0.5), size * 0.5 - (minutePointer.getPrefHeight()) + (minutePointer.getPrefHeight() * 0.24));
            } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {                
                minutePointer.relocate(size * 0.5 - (minutePointer.getPrefWidth() * 0.5), size * 0.5 - (minutePointer.getPrefHeight()) + (minutePointer.getPrefHeight() * 0.108));
                minutePointerFlour.setPrefSize(size * hourPointerWidthFactor, size * hourPointerHeightFactor);                
                minutePointerFlour.relocate(size * 0.5 - (minutePointer.getPrefWidth() * 0.5), size * 0.5 - (minutePointer.getPrefHeight()) + (minutePointer.getPrefHeight() * 0.108));
            } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {                
                minutePointer.relocate(size * 0.5 - (minutePointer.getPrefWidth() * 0.5), size * 0.5 - (minutePointer.getPrefHeight()) + (minutePointer.getPrefHeight() * 0.1759259259));
            } else {                
                minutePointer.relocate(size * 0.5 - (minutePointer.getPrefWidth() * 0.5), size * 0.5 - minutePointer.getPrefHeight());
            }

            hourPointer.setPrefSize(size * minutePointerWidthFactor, size * minutePointerHeightFactor);
            if (Clock.Design.IOS6 == getSkinnable().getDesign()) {                
                hourPointer.relocate(size * 0.5 - (hourPointer.getPrefWidth() * 0.5), size * 0.5 - (hourPointer.getPrefHeight()) + (hourPointer.getPrefHeight() * 0.34));
            } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
                hourPointer.relocate(size * 0.5 - (hourPointer.getPrefWidth() * 0.5), size * 0.5 - (hourPointer.getPrefHeight()) + (hourPointer.getPrefHeight() * 0.128));
                hourPointerFlour.setPrefSize(size * minutePointerWidthFactor, size * minutePointerHeightFactor);
                hourPointerFlour.relocate(size * 0.5 - (hourPointer.getPrefWidth() * 0.5), size * 0.5 - (hourPointer.getPrefHeight()) + (hourPointer.getPrefHeight() * 0.128));
            } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
                hourPointer.relocate(size * 0.5 - (hourPointer.getPrefWidth() * 0.5), size * 0.5 - (hourPointer.getPrefHeight()) + (hourPointer.getPrefHeight() * 0.25));
            } else {
                hourPointer.relocate(size * 0.5 - (hourPointer.getPrefWidth() * 0.5), size * 0.5 - hourPointer.getPrefHeight());
            }

            secondPointer.setPrefSize(size * secondPointerWidthFactor, size * secondPointerHeightFactor);
            if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
                secondPointer.relocate(size * 0.5 - (secondPointer.getPrefWidth() * 0.5), size * 0.5 - (secondPointer.getPrefHeight()) + (secondPointer.getPrefHeight() * 0.2658959538));
            } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
                secondPointer.relocate(size * 0.5 - (secondPointer.getPrefWidth() * 0.5), size * 0.5 - secondPointer.getPrefHeight() + (secondPointer.getPrefHeight() * 0.189));
            } else {
                secondPointer.relocate(size * 0.5 - (secondPointer.getPrefWidth() * 0.5), size * 0.5 - secondPointer.getPrefHeight());
            }

            if (Clock.Design.IOS6 == getSkinnable().getDesign()) {
                centerKnob.setPrefSize(size * 0.015, size * 0.015);
            } else if (Clock.Design.BRAUN == getSkinnable().getDesign()) {
                centerKnob.setPrefSize(size * 0.085, size * 0.085);
            } else if (Clock.Design.BOSCH == getSkinnable().getDesign()) {
                centerKnob.setPrefSize(size * 0.035, size * 0.035);
            } else {
                centerKnob.setPrefSize(size * 0.1, size * 0.1);
            }
            centerKnob.relocate(size * 0.5 - (centerKnob.getPrefWidth() * 0.5), size * 0.5 - (centerKnob.getPrefHeight() * 0.5));

            foreground.setPrefSize(size * 0.955, size * 0.495);
            foreground.relocate(size * 0.5 - (foreground.getPrefWidth() * 0.5), size * 0.01);
        }
    }    
}
