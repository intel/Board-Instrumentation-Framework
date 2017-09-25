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

package eu.hansolo.enzo.gauge.skin;

import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.fonts.Fonts;
import eu.hansolo.enzo.gauge.DoubleRadialGauge;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
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
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.time.Instant;
import java.util.Locale;
import java.util.stream.IntStream;


/**
 * Created by hansolo on 01.12.15.
 */
public class DoubleRadialGaugeSkin extends SkinBase<DoubleRadialGauge> implements Skin<DoubleRadialGauge> {
    private static final double      PREFERRED_WIDTH  = 250;
    private static final double      PREFERRED_HEIGHT = 250;
    private static final double      MINIMUM_WIDTH    = 50;
    private static final double      MINIMUM_HEIGHT   = 50;
    private static final double      MAXIMUM_WIDTH    = 1024;
    private static final double      MAXIMUM_HEIGHT   = 1024;
    private double          oldValueOne;
    private double          oldValueTwo;
    private double          size;
    private Pane            pane;
    private Region          background;
    private Canvas          ticksAndSectionsCanvas;
    private GraphicsContext ticksAndSections;
    private Region          ledFrameOne;
    private Region          ledMainOne;
    private Region          ledHlOne;
    private Region          ledFrameTwo;
    private Region          ledMainTwo;
    private Region          ledHlTwo;
    private Region          needleOne;
    private Region          needleOneHighlight;
    private Rotate          needleOneRotate;
    private Region          needleTwo;
    private Region          needleTwoHighlight;
    private Rotate          needleTwoRotate;
    private Region          knob;
    private Group       shadowGroup;
    private DropShadow  dropShadow;
    private InnerShadow innerShadow;
    private DropShadow  glow;
    private Text     titleTextOne;
    private Text     titleTextTwo;
    private Text     unitTextOne;
    private Text     unitTextTwo;
    private Text     valueTextOne;
    private Text     valueTextTwo;
    private double   angleStepOne;
    private double   angleStepTwo;
    private Timeline timelineOne;
    private Timeline timelineTwo;
    private String   limitString;
    private Instant  lastCallOne;
    private Instant  lastCallTwo;
    private boolean  withinSpeedLimitOne;
    private boolean  withinSpeedLimitTwo;



    // ******************** Constructors **************************************
    public DoubleRadialGaugeSkin(DoubleRadialGauge gauge) {
        super(gauge);
        angleStepOne        = gauge.getAngleRangeOne() / (gauge.getMaxValueOne() - gauge.getMinValueOne());
        angleStepTwo        = -gauge.getAngleRangeTwo() / (gauge.getMaxValueTwo() - gauge.getMinValueTwo());
        timelineOne         = new Timeline();
        timelineTwo         = new Timeline();
        oldValueOne         = getSkinnable().getValueOne();
        oldValueTwo         = getSkinnable().getValueTwo();
        limitString         = "";
        lastCallOne         = Instant.now();
        lastCallTwo         = Instant.now();
        withinSpeedLimitOne = true;
        withinSpeedLimitTwo = true;

        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getSkinnable().getWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getHeight(), 0.0) <= 0) {
            if (getSkinnable().getPrefWidth() < 0 && getSkinnable().getPrefHeight() < 0) {
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
        background = new Region();
        background.getStyleClass().setAll("background");

        ticksAndSectionsCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ticksAndSections = ticksAndSectionsCanvas.getGraphicsContext2D();

        innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 8, 0d, 0d, 0d);
        glow = new DropShadow(BlurType.TWO_PASS_BOX, getSkinnable().getLedColorOne(), 20, 0d, 0d, 0d);
        glow.setInput(innerShadow);

        ledFrameOne = new Region();
        ledFrameOne.getStyleClass().setAll("led-frame");

        ledMainOne = new Region();
        ledMainOne.getStyleClass().setAll("led-main-one");
        ledMainOne.setStyle("-led-color-one: " + (colorToCss(getSkinnable().getLedColorOne())) + ";");

        ledHlOne = new Region();
        ledHlOne.getStyleClass().setAll("led-hl");

        ledFrameTwo = new Region();
        ledFrameTwo.getStyleClass().setAll("led-frame");

        ledMainTwo = new Region();
        ledMainTwo.getStyleClass().setAll("led-main-two");
        ledMainTwo.setStyle("-led-color-two: " + (colorToCss(getSkinnable().getLedColorTwo())) + ";");

        ledHlTwo = new Region();
        ledHlTwo.getStyleClass().setAll("led-hl");

        angleStepOne          = getSkinnable().getAngleRangeOne() / (getSkinnable().getMaxValueOne() - getSkinnable().getMinValueOne());
        double targetAngleOne = 180 - getSkinnable().getStartAngleOne() + (getSkinnable().getValueOne() - getSkinnable().getMinValueOne()) * angleStepOne;
        targetAngleOne        = clamp(180 - getSkinnable().getStartAngleOne(), 180 - getSkinnable().getStartAngleOne() + getSkinnable().getAngleRangeOne(), targetAngleOne);
        needleOneRotate       = new Rotate(targetAngleOne);

        needleOne = new Region();
        needleOne.getStyleClass().setAll(DoubleRadialGauge.STYLE_CLASS_NEEDLE_ONE_STANDARD);
        needleOne.getTransforms().setAll(needleOneRotate);

        needleOneHighlight = new Region();
        needleOneHighlight.setMouseTransparent(true);
        needleOneHighlight.getStyleClass().setAll("needle-highlight");
        needleOneHighlight.getTransforms().setAll(needleOneRotate);

        angleStepTwo          = -getSkinnable().getAngleRangeTwo() / (getSkinnable().getMaxValueTwo() - getSkinnable().getMinValueTwo());
        double targetAngleTwo = 180 - getSkinnable().getStartAngleTwo() + (getSkinnable().getValueTwo() - getSkinnable().getMinValueTwo()) * angleStepTwo;
        targetAngleTwo        = clamp(180 - getSkinnable().getStartAngleTwo() - getSkinnable().getAngleRangeTwo(), 180 - getSkinnable().getStartAngleTwo(), targetAngleTwo);
        needleTwoRotate       = new Rotate(targetAngleTwo);

        needleTwo = new Region();
        needleTwo.getStyleClass().setAll(DoubleRadialGauge.STYLE_CLASS_NEEDLE_TWO_STANDARD);
        needleTwo.getTransforms().setAll(needleTwoRotate);

        needleTwoHighlight = new Region();
        needleTwoHighlight.setMouseTransparent(true);
        needleTwoHighlight.getStyleClass().setAll("needle-highlight");
        needleTwoHighlight.getTransforms().setAll(needleTwoRotate);

        knob = new Region();
        knob.setPickOnBounds(false);
        knob.getStyleClass().setAll("knob");

        dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.25));
        dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
        dropShadow.setRadius(0.015 * PREFERRED_WIDTH);
        dropShadow.setOffsetY(0.015 * PREFERRED_WIDTH);

        shadowGroup = new Group(needleOne, needleOneHighlight, needleTwo, needleTwoHighlight, knob);
        shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);

        titleTextOne = new Text(getSkinnable().getTitleOne());
        titleTextOne.setTextOrigin(VPos.CENTER);
        titleTextOne.getStyleClass().setAll("title");

        unitTextOne = new Text(getSkinnable().getUnitOne());
        unitTextOne.setMouseTransparent(true);
        unitTextOne.setTextOrigin(VPos.CENTER);
        unitTextOne.getStyleClass().setAll("unit");

        valueTextOne = new Text(String.format(Locale.US, "%." + getSkinnable().getDecimalsOne() + "f", getSkinnable().getValueOne()));
        valueTextOne.setMouseTransparent(true);
        valueTextOne.setTextOrigin(VPos.CENTER);
        valueTextOne.getStyleClass().setAll("value");

        titleTextTwo = new Text(getSkinnable().getTitleTwo());
        titleTextTwo.setTextOrigin(VPos.CENTER);
        titleTextTwo.getStyleClass().setAll("title");

        unitTextTwo = new Text(getSkinnable().getUnitTwo());
        unitTextTwo.setMouseTransparent(true);
        unitTextTwo.setTextOrigin(VPos.CENTER);
        unitTextTwo.getStyleClass().setAll("unit");

        valueTextTwo = new Text(String.format(Locale.US, "%." + getSkinnable().getDecimalsTwo() + "f", getSkinnable().getValueTwo()));
        valueTextTwo.setMouseTransparent(true);
        valueTextTwo.setTextOrigin(VPos.CENTER);
        valueTextTwo.getStyleClass().setAll("value");

        // Add all nodes
        pane = new Pane();
        pane.getChildren().setAll(background,
                                  ticksAndSectionsCanvas,
                                  titleTextOne,
                                  titleTextTwo,
                                  ledFrameOne,
                                  ledMainOne,
                                  ledHlOne,
                                  ledFrameTwo,
                                  ledMainTwo,
                                  ledHlTwo,
                                  unitTextOne,
                                  valueTextOne,
                                  unitTextTwo,
                                  valueTextTwo,
                                  shadowGroup);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().ledOnOneProperty().addListener(observable -> ledMainOne.setEffect(getSkinnable().getLedOnOne() ? glow : innerShadow));
        getSkinnable().ledColorOneProperty().addListener(observable -> handleControlPropertyChanged("LED_COLOR_ONE"));
        getSkinnable().ledVisibleOneProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().valueVisibleOneProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().tickLabelOrientationOneProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickLabelFillOneProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickMarkFillOneProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().needleColorOneProperty().addListener(observable -> handleControlPropertyChanged("NEEDLE_COLOR_ONE"));
        getSkinnable().numberFormatOneProperty().addListener(observable -> handleControlPropertyChanged("RECALC"));
        getSkinnable().getSectionsOne().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().getAreasOne().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));

        getSkinnable().ledOnTwoProperty().addListener(observable -> ledMainTwo.setEffect(getSkinnable().getLedOnTwo() ? glow : innerShadow));
        getSkinnable().ledColorTwoProperty().addListener(observable -> handleControlPropertyChanged("LED_COLOR_TWO"));
        getSkinnable().ledVisibleTwoProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().valueVisibleTwoProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().tickLabelOrientationTwoProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickLabelFillTwoProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickMarkFillTwoProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().needleColorTwoProperty().addListener(observable -> handleControlPropertyChanged("NEEDLE_COLOR_TWO"));
        getSkinnable().numberFormatTwoProperty().addListener(observable -> handleControlPropertyChanged("RECALC"));
        getSkinnable().getSectionsTwo().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().getAreasTwo().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));
        
        getSkinnable().dropShadowEnabledProperty().addListener(observable -> handleControlPropertyChanged("DROP_SHADOW"));
        getSkinnable().animatedProperty().addListener(observable -> handleControlPropertyChanged("ANIMATED"));

        getSkinnable().valueOneProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            withinSpeedLimitOne = !(Instant.now().minusMillis((long) getSkinnable().getAnimationDuration()).isBefore(lastCallOne));
            lastCallOne         = Instant.now();
            oldValueOne         = OLD_VALUE.doubleValue();
            rotateNeedleOne();
        });

        getSkinnable().minValueOneProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStepOne = getSkinnable().getAngleRangeOne() / (getSkinnable().getMaxValueOne() - NEW_VALUE.doubleValue());
            needleOneRotate.setAngle((180 - getSkinnable().getStartAngleOne()) + (getSkinnable().getValueOne() - NEW_VALUE.doubleValue()) * angleStepOne);
            if (getSkinnable().getValueOne() < NEW_VALUE.doubleValue()) {
                getSkinnable().setValueOne(NEW_VALUE.doubleValue());
                oldValueOne = NEW_VALUE.doubleValue();
            }
        });
        getSkinnable().maxValueOneProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStepOne = getSkinnable().getAngleRangeOne() / (NEW_VALUE.doubleValue() - getSkinnable().getMinValueOne());
            needleOneRotate.setAngle((180 - getSkinnable().getStartAngleOne()) + (getSkinnable().getValueOne() - getSkinnable().getMinValueOne()) * angleStepOne);
            if (getSkinnable().getValueOne() > NEW_VALUE.doubleValue()) {
                getSkinnable().setValueOne(NEW_VALUE.doubleValue());
                oldValueOne = NEW_VALUE.doubleValue();
            }
        });

        getSkinnable().valueTwoProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            withinSpeedLimitTwo = !(Instant.now().minusMillis((long) getSkinnable().getAnimationDuration()).isBefore(lastCallTwo));
            lastCallTwo         = Instant.now();
            oldValueTwo         = OLD_VALUE.doubleValue();
            rotateNeedleTwo();
        });

        getSkinnable().minValueTwoProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStepTwo = getSkinnable().getAngleRangeTwo() / (getSkinnable().getMaxValueTwo() - NEW_VALUE.doubleValue());
            needleTwoRotate.setAngle((180 - getSkinnable().getStartAngleTwo()) + (getSkinnable().getValueTwo() - NEW_VALUE.doubleValue()) * angleStepTwo);
            if (getSkinnable().getValueTwo() < NEW_VALUE.doubleValue()) {
                getSkinnable().setValueTwo(NEW_VALUE.doubleValue());
                oldValueTwo = NEW_VALUE.doubleValue();
            }
        });
        getSkinnable().maxValueTwoProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStepTwo = getSkinnable().getAngleRangeTwo() / (NEW_VALUE.doubleValue() - getSkinnable().getMinValueTwo());
            needleTwoRotate.setAngle((180 - getSkinnable().getStartAngleTwo()) + (getSkinnable().getValueTwo() - getSkinnable().getMinValueTwo()) * angleStepTwo);
            if (getSkinnable().getValueTwo() > NEW_VALUE.doubleValue()) {
                getSkinnable().setValueTwo(NEW_VALUE.doubleValue());
                oldValueTwo = NEW_VALUE.doubleValue();
            }
        });

        needleOneRotate.angleProperty().addListener(observable -> handleControlPropertyChanged("ANGLE_ONE"));
        needleTwoRotate.angleProperty().addListener(observable -> handleControlPropertyChanged("ANGLE_TWO"));
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("ANGLE_ONE".equals(PROPERTY)) {
            double currentValueOne = (needleOneRotate.getAngle() + getSkinnable().getStartAngleOne() - 180) / angleStepOne + getSkinnable().getMinValueOne();
            valueTextOne.setText(limitString + String.format(Locale.US, "%." + getSkinnable().getDecimalsOne() + "f", currentValueOne));
            valueTextOne.setTranslateX((size - valueTextOne.getLayoutBounds().getWidth()) * 0.35);
        } else if ("ANGLE_TWO".equals(PROPERTY)) {
            double currentValueTwo = (needleTwoRotate.getAngle() + getSkinnable().getStartAngleTwo() - 180) / angleStepTwo + getSkinnable().getMinValueTwo();
            valueTextTwo.setText(limitString + String.format(Locale.US, "%." + getSkinnable().getDecimalsTwo() + "f", currentValueTwo));
            valueTextTwo.setTranslateX((size - valueTextTwo.getLayoutBounds().getWidth()) * 0.65);
        } else if ("DROP_SHADOW".equals(PROPERTY)) {
            shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);
        } else if ("CANVAS_REFRESH".equals(PROPERTY)) {
            ticksAndSections.clearRect(0, 0, size, size);
            drawSections(ticksAndSections);
            drawAreas(ticksAndSections);
            drawTickMarks(ticksAndSections);
        } else if ("NEEDLE_COLOR_ONE".equals(PROPERTY)) {
            needleOne.setStyle("-needle-color-one: " + (colorToCss(getSkinnable().getNeedleColorOne())) + ";");
            resize();
        } else if ("LED_COLOR".equals(PROPERTY)) {
            ledMainOne.setStyle("-led-color-one: " + (colorToCss(getSkinnable().getLedColorOne())));
            resize();
        } else if ("NEEDLE_COLOR_TWO".equals(PROPERTY)) {
            needleTwo.setStyle("-needle-color-two: " + (colorToCss(getSkinnable().getNeedleColorTwo())) + ";");
            resize();
        } else if ("LED_COLOR_TWO".equals(PROPERTY)) {
            ledMainTwo.setStyle("-led-color-two: " + (colorToCss(getSkinnable().getLedColorTwo())));
            resize();
        }
    }


    // ******************** Utility methods ***********************************
    private String colorToCss(final Color COLOR) {
        return COLOR.toString().replace("0x", "#");
    }


    // ******************** Private Methods ***********************************
    private void rotateNeedleOne() {
        timelineOne.stop();
        angleStepOne       = getSkinnable().getAngleRangeOne() / (getSkinnable().getMaxValueOne() - getSkinnable().getMinValueOne());
        double targetAngle = 180 - getSkinnable().getStartAngleOne() + (getSkinnable().getValueOne() - getSkinnable().getMinValueOne()) * angleStepOne;
        targetAngle        = clamp(180 - getSkinnable().getStartAngleOne(), 180 - getSkinnable().getStartAngleOne() + getSkinnable().getAngleRangeOne(), targetAngle);
        if (withinSpeedLimitOne && getSkinnable().isAnimated()) {
            //double animationDuration = (getSkinnable().getAnimationDuration() / (getSkinnable().getMaxValueOne() - getSkinnable().getMinValueOne())) * Math.abs(getSkinnable().getValueOne() - getSkinnable().getOldValue());
            final KeyValue KEY_VALUE = new KeyValue(needleOneRotate.angleProperty(), targetAngle, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
            final KeyFrame KEY_FRAME = new KeyFrame(Duration.millis(getSkinnable().getAnimationDuration()), KEY_VALUE);
            timelineOne.getKeyFrames().setAll(KEY_FRAME);
            timelineOne.play();
        } else {
            needleOneRotate.setAngle(targetAngle);
        }
    }

    private void rotateNeedleTwo() {
        timelineTwo.stop();
        angleStepTwo       = -getSkinnable().getAngleRangeTwo() / (getSkinnable().getMaxValueTwo() - getSkinnable().getMinValueTwo());
        double targetAngle = 180 - getSkinnable().getStartAngleTwo() + (getSkinnable().getValueTwo() - getSkinnable().getMinValueTwo()) * angleStepTwo;
        targetAngle        = clamp(180 - getSkinnable().getStartAngleTwo() - getSkinnable().getAngleRangeTwo(), 180 - getSkinnable().getStartAngleTwo(), targetAngle);

        if (withinSpeedLimitTwo && getSkinnable().isAnimated()) {
            //double animationDuration = (getSkinnable().getAnimationDuration() / (getSkinnable().getMaxValueTwo() - getSkinnable().getMinValueTwo())) * Math.abs(getSkinnable().getValueTwo() - getSkinnable().getOldValue());
            final KeyValue KEY_VALUE = new KeyValue(needleTwoRotate.angleProperty(), targetAngle, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
            final KeyFrame KEY_FRAME = new KeyFrame(Duration.millis(getSkinnable().getAnimationDuration()), KEY_VALUE);
            timelineTwo.getKeyFrames().setAll(KEY_FRAME);
            timelineTwo.play();
        } else {
            needleTwoRotate.setAngle(targetAngle);
        }
    }

    private void drawTickMarks(final GraphicsContext CTX) {
        double  sinValue;
        double  cosValue;
        double  startAngle   = getSkinnable().getStartAngleOne();
        double  orthText     = DoubleRadialGauge.TickLabelOrientation.ORTHOGONAL == getSkinnable().getTickLabelOrientationOne() ? 0.35 : 0.33;
        Point2D center       = new Point2D(size * 0.5, size * 0.5);
        boolean smallRange   = getSkinnable().getMaxValueOne() - getSkinnable().getMinValueOne() < 10;
        double  tmpMinValue  = smallRange ? getSkinnable().getMinValueOne() * 10 : getSkinnable().getMinValueOne();
        double  tmpMaxValue  = smallRange ? getSkinnable().getMaxValueOne() * 10 : getSkinnable().getMaxValueOne();
        double  tmpAngleStep = smallRange ? angleStepOne / 10 : angleStepOne;
        
        // Scale One
        for (double angle = 0, counter = tmpMinValue; Double.compare(counter, tmpMaxValue) <= 0 ; angle -= tmpAngleStep, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            Point2D innerMainPoint   = new Point2D(center.getX() + size * 0.368 * sinValue, center.getY() + size * 0.368 * cosValue);
            Point2D innerMediumPoint = new Point2D(center.getX() + size * 0.388 * sinValue, center.getY() + size * 0.388 * cosValue);
            Point2D innerMinorPoint  = new Point2D(center.getX() + size * 0.3975 * sinValue, center.getY() + size * 0.3975 * cosValue);
            Point2D outerPoint       = new Point2D(center.getX() + size * 0.432 * sinValue, center.getY() + size * 0.432 * cosValue);
            Point2D textPoint        = new Point2D(center.getX() + size * orthText * sinValue, center.getY() + size * orthText * cosValue);

            CTX.setStroke(getSkinnable().getTickMarkFillOne());
            if (counter % getSkinnable().getMajorTickSpaceOne() == 0) {
                // Draw major tickmark
                CTX.setLineWidth(size * 0.0055);
                CTX.strokeLine(innerMainPoint.getX(), innerMainPoint.getY(), outerPoint.getX(), outerPoint.getY());

                // Draw text
                CTX.save();
                CTX.translate(textPoint.getX(), textPoint.getY());
                switch(getSkinnable().getTickLabelOrientationOne()) {
                    case ORTHOGONAL:
                        if ((360 - startAngle - angle) % 360 > 90 && (360 - startAngle - angle) % 360 < 270) {
                            CTX.rotate((180 - startAngle - angle) % 360);
                        } else {
                            CTX.rotate((360 - startAngle - angle) % 360);
                        }
                        break;
                    case TANGENT:
                        if ((360 - startAngle - angle - 90) % 360 > 90 && (360 - startAngle - angle - 90) % 360 < 270) {
                            CTX.rotate((90 - startAngle - angle) % 360);
                        } else {
                            CTX.rotate((270 - startAngle - angle) % 360);
                        }
                        break;
                    case HORIZONTAL:
                    default:
                        break;
                }
                CTX.setFont(Font.font("Verdana", FontWeight.NORMAL, 0.035 * size));
                CTX.setTextAlign(TextAlignment.CENTER);
                CTX.setTextBaseline(VPos.CENTER);
                CTX.setFill(getSkinnable().getTickLabelFillOne());
                CTX.fillText(Integer.toString((int) (smallRange ? counter / 10 : counter)), 0, 0);
                CTX.restore();
            } else if (getSkinnable().getMinorTickSpaceOne() % 2 != 0 && counter % 5 == 0) {
                CTX.setLineWidth(size * 0.0035);
                CTX.strokeLine(innerMediumPoint.getX(), innerMediumPoint.getY(), outerPoint.getX(), outerPoint.getY());
            } else if (counter % getSkinnable().getMinorTickSpaceOne() == 0) {
                CTX.setLineWidth(size * 0.00225);
                CTX.strokeLine(innerMinorPoint.getX(), innerMinorPoint.getY(), outerPoint.getX(), outerPoint.getY());
            }
        }

        // Scale Two
        startAngle   = getSkinnable().getStartAngleTwo();
        smallRange   = getSkinnable().getMaxValueTwo() - getSkinnable().getMinValueTwo() < 10;
        tmpMinValue  = smallRange ? getSkinnable().getMinValueTwo() * 10 : getSkinnable().getMinValueTwo();
        tmpMaxValue  = smallRange ? getSkinnable().getMaxValueTwo() * 10 : getSkinnable().getMaxValueTwo();
        tmpAngleStep = smallRange ? angleStepTwo / 10 : angleStepTwo;
        for (double angle = 0, counter = tmpMinValue; Double.compare(counter, tmpMaxValue) <= 0 ; angle -= tmpAngleStep, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            Point2D innerMainPoint   = new Point2D(center.getX() + size * 0.368 * sinValue, center.getY() + size * 0.368 * cosValue);
            Point2D innerMediumPoint = new Point2D(center.getX() + size * 0.388 * sinValue, center.getY() + size * 0.388 * cosValue);
            Point2D innerMinorPoint  = new Point2D(center.getX() + size * 0.3975 * sinValue, center.getY() + size * 0.3975 * cosValue);
            Point2D outerPoint       = new Point2D(center.getX() + size * 0.432 * sinValue, center.getY() + size * 0.432 * cosValue);
            Point2D textPoint        = new Point2D(center.getX() + size * orthText * sinValue, center.getY() + size * orthText * cosValue);

            CTX.setStroke(getSkinnable().getTickMarkFillTwo());
            if (counter % getSkinnable().getMajorTickSpaceTwo() == 0) {
                // Draw major tickmark
                CTX.setLineWidth(size * 0.0055);
                CTX.strokeLine(innerMainPoint.getX(), innerMainPoint.getY(), outerPoint.getX(), outerPoint.getY());

                // Draw text
                CTX.save();
                CTX.translate(textPoint.getX(), textPoint.getY());
                switch(getSkinnable().getTickLabelOrientationTwo()) {
                    case ORTHOGONAL:
                        if ((360 - startAngle - angle) % 360 > 90 && (360 - startAngle - angle) % 360 < 270) {
                            CTX.rotate((180 - startAngle - angle) % 360);
                        } else {
                            CTX.rotate((360 - startAngle - angle) % 360);
                        }
                        break;
                    case TANGENT:
                        if ((360 - startAngle - angle - 90) % 360 > 90 && (360 - startAngle - angle - 90) % 360 < 270) {
                            CTX.rotate((90 - startAngle - angle) % 360);
                        } else {
                            CTX.rotate((270 - startAngle - angle) % 360);
                        }
                        break;
                    case HORIZONTAL:
                    default:
                        break;
                }
                CTX.setFont(Font.font("Verdana", FontWeight.NORMAL, 0.035 * size));
                CTX.setTextAlign(TextAlignment.CENTER);
                CTX.setTextBaseline(VPos.CENTER);
                CTX.setFill(getSkinnable().getTickLabelFillTwo());
                CTX.fillText(Integer.toString((int) (smallRange ? counter / 10 : counter)), 0, 0);
                CTX.restore();
            } else if (getSkinnable().getMinorTickSpaceTwo() % 2 != 0 && counter % 5 == 0) {
                CTX.setLineWidth(size * 0.0035);
                CTX.strokeLine(innerMediumPoint.getX(), innerMediumPoint.getY(), outerPoint.getX(), outerPoint.getY());
            } else if (counter % getSkinnable().getMinorTickSpaceTwo() == 0) {
                CTX.setLineWidth(size * 0.00225);
                CTX.strokeLine(innerMinorPoint.getX(), innerMinorPoint.getY(), outerPoint.getX(), outerPoint.getY());
            }
        }
    }

    private final void drawSections(final GraphicsContext CTX) {
        final double xy            = (size - 0.83425 * size) / 2;
        final double wh            = size * 0.83425;
        final double MIN_VALUE_ONE = getSkinnable().getMinValueOne();
        final double MAX_VALUE_ONE = getSkinnable().getMaxValueOne();
        final double OFFSET_ONE    = 90 - getSkinnable().getStartAngleOne();

        IntStream.range(0, getSkinnable().getSectionsOne().size()).parallel().forEachOrdered(
            i -> {
                final Section SECTION = getSkinnable().getSectionsOne().get(i);
                final double  SECTION_START_ANGLE;
                if (Double.compare(SECTION.getStart(), MAX_VALUE_ONE) <= 0 && Double.compare(SECTION.getStop(), MIN_VALUE_ONE) >= 0) {
                    if (SECTION.getStart() < MIN_VALUE_ONE && SECTION.getStop() < MAX_VALUE_ONE) {
                        SECTION_START_ANGLE = MIN_VALUE_ONE * angleStepOne;
                    } else {
                        SECTION_START_ANGLE = (SECTION.getStart() - MIN_VALUE_ONE) * angleStepOne;
                    }
                    final double SECTION_ANGLE_EXTEND;
                    if (SECTION.getStop() > MAX_VALUE_ONE) {
                        SECTION_ANGLE_EXTEND = (MAX_VALUE_ONE - SECTION.getStart()) * angleStepOne;
                    } else {
                        SECTION_ANGLE_EXTEND = (SECTION.getStop() - SECTION.getStart()) * angleStepOne;
                    }

                    CTX.save();
                    switch (i) {
                        case 0: CTX.setStroke(getSkinnable().getSectionFill0One()); break;
                        case 1: CTX.setStroke(getSkinnable().getSectionFill1One()); break;
                        case 2: CTX.setStroke(getSkinnable().getSectionFill2One()); break;
                        case 3: CTX.setStroke(getSkinnable().getSectionFill3One()); break;
                        case 4: CTX.setStroke(getSkinnable().getSectionFill4One()); break;
                    }
                    CTX.setLineWidth(size * 0.0415);
                    CTX.setLineCap(StrokeLineCap.BUTT);
                    CTX.strokeArc(xy, xy, wh, wh, -(OFFSET_ONE + SECTION_START_ANGLE), -SECTION_ANGLE_EXTEND, ArcType.OPEN);
                    CTX.restore();
                }
            });


        final double MIN_VALUE_TWO = getSkinnable().getMinValueTwo();
        final double MAX_VALUE_TWO = getSkinnable().getMaxValueTwo();
        final double OFFSET_TWO    = 90 - getSkinnable().getStartAngleTwo();

        IntStream.range(0, getSkinnable().getSectionsTwo().size()).parallel().forEachOrdered(
            i -> {
                final Section SECTION = getSkinnable().getSectionsTwo().get(i);
                final double  SECTION_START_ANGLE;
                if (Double.compare(SECTION.getStart(), MAX_VALUE_TWO) <= 0 && Double.compare(SECTION.getStop(), MIN_VALUE_TWO) >= 0) {
                    if (SECTION.getStart() < MIN_VALUE_TWO && SECTION.getStop() < MAX_VALUE_TWO) {
                        SECTION_START_ANGLE = MIN_VALUE_TWO * angleStepTwo;
                    } else {
                        SECTION_START_ANGLE = (SECTION.getStart() - MIN_VALUE_TWO) * angleStepTwo;
                    }
                    final double SECTION_ANGLE_EXTEND;
                    if (SECTION.getStop() > MAX_VALUE_TWO) {
                        SECTION_ANGLE_EXTEND = (MAX_VALUE_TWO - SECTION.getStart()) * angleStepTwo;
                    } else {
                        SECTION_ANGLE_EXTEND = (SECTION.getStop() - SECTION.getStart()) * angleStepTwo;
                    }

                    CTX.save();
                    switch (i) {
                        case 0: CTX.setStroke(getSkinnable().getSectionFill0Two()); break;
                        case 1: CTX.setStroke(getSkinnable().getSectionFill1Two()); break;
                        case 2: CTX.setStroke(getSkinnable().getSectionFill2Two()); break;
                        case 3: CTX.setStroke(getSkinnable().getSectionFill3Two()); break;
                        case 4: CTX.setStroke(getSkinnable().getSectionFill4Two()); break;
                    }
                    CTX.setLineWidth(size * 0.0415);
                    CTX.setLineCap(StrokeLineCap.BUTT);
                    CTX.strokeArc(xy, xy, wh, wh, -(OFFSET_TWO + SECTION_START_ANGLE), -SECTION_ANGLE_EXTEND, ArcType.OPEN);
                    CTX.restore();
                }
            });
    }

    private final void drawAreas(final GraphicsContext CTX) {
        final double xy            = (size - 0.7925 * size) / 2;
        final double wh            = size * 0.7925;
        final double MIN_VALUE_ONE = getSkinnable().getMinValueOne();
        final double MAX_VALUE_ONE = getSkinnable().getMaxValueOne();
        final double OFFSET_ONE    = 90 - getSkinnable().getStartAngleOne();

        IntStream.range(0, getSkinnable().getAreasOne().size()).parallel().forEachOrdered(
            i -> {
                final Section AREA = getSkinnable().getAreasOne().get(i);
                final double AREA_START_ANGLE;
                if (Double.compare(AREA.getStart(), MAX_VALUE_ONE) <= 0 && Double.compare(AREA.getStop(), MIN_VALUE_ONE) >= 0) {
                    if (AREA.getStart() < MIN_VALUE_ONE && AREA.getStop() < MAX_VALUE_ONE) {
                        AREA_START_ANGLE = MIN_VALUE_ONE * angleStepOne;
                    } else {
                        AREA_START_ANGLE = (AREA.getStart() - MIN_VALUE_ONE) * angleStepOne;
                    }
                    final double AREA_ANGLE_EXTEND;
                    if (AREA.getStop() > MAX_VALUE_ONE) {
                        AREA_ANGLE_EXTEND = (MAX_VALUE_ONE - AREA.getStart()) * angleStepOne;
                    } else {
                        AREA_ANGLE_EXTEND = (AREA.getStop() - AREA.getStart()) * angleStepOne;
                    }

                    CTX.save();
                    switch(i) {
                        case 0: CTX.setFill(getSkinnable().getAreaFill0One()); break;
                        case 1: CTX.setFill(getSkinnable().getAreaFill1One()); break;
                        case 2: CTX.setFill(getSkinnable().getAreaFill2One()); break;
                        case 3: CTX.setFill(getSkinnable().getAreaFill3One()); break;
                        case 4: CTX.setFill(getSkinnable().getAreaFill4One()); break;
                    }
                    CTX.fillArc(xy, xy, wh, wh, -(OFFSET_ONE + AREA_START_ANGLE), - AREA_ANGLE_EXTEND, ArcType.ROUND);
                    CTX.restore();
                }
            });

        
        final double MIN_VALUE_TWO = getSkinnable().getMinValueTwo();
        final double MAX_VALUE_TWO = getSkinnable().getMaxValueTwo();
        final double OFFSET_TWO    = 90 - getSkinnable().getStartAngleTwo();
        
        IntStream.range(0, getSkinnable().getAreasTwo().size()).parallel().forEachOrdered(
            i -> {
                final Section AREA = getSkinnable().getAreasTwo().get(i);
                final double AREA_START_ANGLE;
                if (Double.compare(AREA.getStart(), MAX_VALUE_TWO) <= 0 && Double.compare(AREA.getStop(), MIN_VALUE_TWO) >= 0) {
                    if (AREA.getStart() < MIN_VALUE_TWO && AREA.getStop() < MAX_VALUE_TWO) {
                        AREA_START_ANGLE = MIN_VALUE_TWO * angleStepTwo;
                    } else {
                        AREA_START_ANGLE = (AREA.getStart() - MIN_VALUE_TWO) * angleStepTwo;
                    }
                    final double AREA_ANGLE_EXTEND;
                    if (AREA.getStop() > MAX_VALUE_TWO) {
                        AREA_ANGLE_EXTEND = (MAX_VALUE_TWO - AREA.getStart()) * angleStepTwo;
                    } else {
                        AREA_ANGLE_EXTEND = (AREA.getStop() - AREA.getStart()) * angleStepTwo;
                    }

                    CTX.save();
                    switch(i) {
                        case 0: CTX.setFill(getSkinnable().getAreaFill0Two()); break;
                        case 1: CTX.setFill(getSkinnable().getAreaFill1Two()); break;
                        case 2: CTX.setFill(getSkinnable().getAreaFill2Two()); break;
                        case 3: CTX.setFill(getSkinnable().getAreaFill3Two()); break;
                        case 4: CTX.setFill(getSkinnable().getAreaFill4Two()); break;
                    }
                    CTX.fillArc(xy, xy, wh, wh, -(OFFSET_TWO + AREA_START_ANGLE), - AREA_ANGLE_EXTEND, ArcType.ROUND);
                    CTX.restore();
                }
            });
    }

    private double clamp(final double MIN_VALUE, final double MAX_VALUE, final double VALUE) {
        if (VALUE < MIN_VALUE) return MIN_VALUE;
        if (VALUE > MAX_VALUE) return MAX_VALUE;
        return VALUE;
    }

    private void resizeText() {
        titleTextOne.setFont(Fonts.robotoMedium(size * 0.04));
        titleTextOne.setTranslateX((size - titleTextOne.getLayoutBounds().getWidth()) * 0.35);
        titleTextOne.setTranslateY(size * 0.35);

        unitTextOne.setFont(Fonts.robotoMedium(size * 0.04));
        unitTextOne.setTranslateX((size - unitTextOne.getLayoutBounds().getWidth()) * 0.35);
        unitTextOne.setTranslateY(size * 0.84);
        unitTextOne.setRotate(-60);

        valueTextOne.setFont(Fonts.robotoBold(size * 0.04));
        valueTextOne.setTranslateX((size - valueTextOne.getLayoutBounds().getWidth()) * 0.35);
        valueTextOne.setTranslateY(size * 0.65);
        valueTextOne.setVisible(getSkinnable().isValueVisibleOne());

        titleTextTwo.setFont(Fonts.robotoMedium(size * 0.04));
        titleTextTwo.setTranslateX((size - titleTextTwo.getLayoutBounds().getWidth()) * 0.65);
        titleTextTwo.setTranslateY(size * 0.35);

        unitTextTwo.setFont(Fonts.robotoMedium(size * 0.04));
        unitTextTwo.setTranslateX((size - unitTextTwo.getLayoutBounds().getWidth()) * 0.65);
        unitTextTwo.setTranslateY(size * 0.84);
        unitTextTwo.setRotate(60);

        valueTextTwo.setFont(Fonts.robotoBold(size * 0.04));
        valueTextTwo.setTranslateX((size - valueTextTwo.getLayoutBounds().getWidth()) * 0.65);
        valueTextTwo.setTranslateY(size * 0.65);
        valueTextTwo.setVisible(getSkinnable().isValueVisibleTwo());
    }

    private void resize() {
        size = getSkinnable().getWidth() < getSkinnable().getHeight() ? getSkinnable().getWidth() : getSkinnable().getHeight();

        if (size > 0) {
            pane.setMaxSize(size, size);
            pane.relocate((getSkinnable().getWidth() - size) * 0.5, (getSkinnable().getHeight() - size) * 0.5);

            dropShadow.setRadius(0.01 * size);
            dropShadow.setOffsetY(0.01 * size);

            background.setPrefSize(size, size);
            // TODO: Remove this CSS inlining workaround by using % in -fx-background-insets
            background.setStyle("-fx-background-insets: 0, " + 0.02 * size + ", " + 0.022 * size + ", " + 0.038 * size + ";");

            ticksAndSectionsCanvas.setWidth(size);
            ticksAndSectionsCanvas.setHeight(size);
            ticksAndSections.clearRect(0, 0, size, size);
            if (getSkinnable().getSectionsVisibleOne()) drawSections(ticksAndSections);
            if (getSkinnable().getAreasVisibleOne()) drawAreas(ticksAndSections);
            drawTickMarks(ticksAndSections);
            ticksAndSectionsCanvas.setCache(true);
            ticksAndSectionsCanvas.setCacheHint(CacheHint.QUALITY);

            innerShadow.setRadius(0.01 * size);
            glow.setRadius(0.05 * size);
            glow.setColor(getSkinnable().getLedColorOne());

            ledFrameOne.setPrefSize(0.06 * size, 0.06 * size);
            ledFrameOne.relocate(0.33 * size, 0.11 * size);

            ledMainOne.setPrefSize(0.0432 * size, 0.0432 * size);
            ledMainOne.relocate(0.33 * size + 0.0084 * size, 0.11 * size + 0.0084 * size);

            ledHlOne.setPrefSize(0.0348 * size, 0.0348 * size);
            ledHlOne.relocate(0.33 * size + 0.0126 * size, 0.11 * size + 0.0126 * size);

            ledFrameTwo.setPrefSize(0.06 * size, 0.06 * size);
            ledFrameTwo.relocate(0.61 * size, 0.11 * size);

            ledMainTwo.setPrefSize(0.0432 * size, 0.0432 * size);
            ledMainTwo.relocate(0.61 * size + 0.0084 * size, 0.11 * size + 0.0084 * size);

            ledHlTwo.setPrefSize(0.0348 * size, 0.0348 * size);
            ledHlTwo.relocate(0.61 * size + 0.0126 * size, 0.11 * size + 0.0126 * size);

            boolean ledOneVisible = getSkinnable().getLedVisibleOne();
            ledFrameOne.setManaged(ledOneVisible);
            ledFrameOne.setVisible(ledOneVisible);
            ledMainOne.setManaged(ledOneVisible);
            ledMainOne.setVisible(ledOneVisible);
            ledHlOne.setManaged(ledOneVisible);
            ledHlOne.setVisible(ledOneVisible);

            boolean ledTwoVisible = getSkinnable().getLedVisibleTwo();
            ledFrameTwo.setManaged(ledTwoVisible);
            ledFrameTwo.setVisible(ledTwoVisible);
            ledMainTwo.setManaged(ledTwoVisible);
            ledMainTwo.setVisible(ledTwoVisible);
            ledHlTwo.setManaged(ledTwoVisible);
            ledHlTwo.setVisible(ledTwoVisible);

            needleOne.setStyle("-needle-color-one: " + (colorToCss(getSkinnable().getNeedleColorOne())) + ";");

            needleOne.setPrefSize(size * 0.04, size * 0.425);
            needleOne.relocate((size - needleOne.getPrefWidth()) * 0.5, size * 0.5 - needleOne.getPrefHeight());
            needleOneRotate.setPivotX(needleOne.getPrefWidth() * 0.5);
            needleOneRotate.setPivotY(needleOne.getPrefHeight());

            needleOneHighlight.setPrefSize(size * 0.04, size * 0.425);
            needleOneHighlight.setTranslateX((size - needleOne.getPrefWidth()) * 0.5);
            needleOneHighlight.setTranslateY(size * 0.5 - needleOne.getPrefHeight());

            needleTwo.setStyle("-needle-color-two: " + (colorToCss(getSkinnable().getNeedleColorTwo())) + ";");

            needleTwo.setPrefSize(size * 0.04, size * 0.425);
            needleTwo.relocate((size - needleTwo.getPrefWidth()) * 0.5, size * 0.5 - needleTwo.getPrefHeight());
            needleTwoRotate.setPivotX(needleTwo.getPrefWidth() * 0.5);
            needleTwoRotate.setPivotY(needleTwo.getPrefHeight());

            needleTwoHighlight.setPrefSize(size * 0.04, size * 0.425);
            needleTwoHighlight.setTranslateX((size - needleTwo.getPrefWidth()) * 0.5);
            needleTwoHighlight.setTranslateY(size * 0.5 - needleTwo.getPrefHeight());

            knob.setPrefSize(size * 0.15, size * 0.15);
            knob.setTranslateX((size - knob.getPrefWidth()) * 0.5);
            knob.setTranslateY((size - knob.getPrefHeight()) * 0.5);

            resizeText();
        }
    }
}
