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
import eu.hansolo.enzo.gauge.RadialGauge;
import javafx.animation.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.*;
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
 * Created by hansolo on 21.07.14.
 */
public class RadialGaugeSkin extends SkinBase<RadialGauge> implements Skin<RadialGauge> {
    private static final double      PREFERRED_WIDTH  = 250;
    private static final double      PREFERRED_HEIGHT = 250;
    private static final double      MINIMUM_WIDTH    = 50;
    private static final double      MINIMUM_HEIGHT   = 50;
    private static final double      MAXIMUM_WIDTH    = 1024;
    private static final double      MAXIMUM_HEIGHT   = 1024;
    private double                   oldValue;
    private double                   size;
    private Pane                     pane;
    private Region                   background;
    private Canvas                   ticksAndSectionsCanvas;
    private GraphicsContext          ticksAndSections;
    private Region                   ledFrame;
    private Region                   ledMain;
    private Region                   ledHl;
    private Region                   needle;
    private Region                   needleHighlight;
    private Rotate                   needleRotate;
    private Region                   knob;
    private Group                    shadowGroup;
    private DropShadow               dropShadow;
    private InnerShadow              innerShadow;
    private DropShadow               glow;
    private Text                     titleText;
    private Text                     unitText;
    private Text                     valueText;
    private double                   angleStep;
    private Timeline                 timeline;
    private String                   limitString;
    private Instant                  lastCall;
    private boolean                  withinSpeedLimit;
    private double                   interactiveAngle;


    // ******************** Constructors **************************************
    public RadialGaugeSkin(RadialGauge gauge) {
        super(gauge);
        angleStep        = gauge.getAngleRange() / (gauge.getMaxValue() - gauge.getMinValue());
        timeline         = new Timeline();
        oldValue         = getSkinnable().getValue();
        limitString      = "";
        lastCall         = Instant.now();
        withinSpeedLimit = true;

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
        glow = new DropShadow(BlurType.TWO_PASS_BOX, getSkinnable().getLedColor(), 20, 0d, 0d, 0d);
        glow.setInput(innerShadow);

        ledFrame = new Region();
        ledFrame.getStyleClass().setAll("led-frame");

        ledMain = new Region();
        ledMain.getStyleClass().setAll("led-main");
        ledMain.setStyle("-led-color: " + (colorToCss(getSkinnable().getLedColor())) + ";");

        ledHl = new Region();
        ledHl.getStyleClass().setAll("led-hl");

        needle = new Region();
        needle.getStyleClass().setAll(RadialGauge.STYLE_CLASS_NEEDLE_STANDARD);
        needleRotate = new Rotate(180 - getSkinnable().getStartAngle());
        needleRotate.setAngle(needleRotate.getAngle() + (getSkinnable().getValue() - oldValue - getSkinnable().getMinValue()) * angleStep);
        needle.getTransforms().setAll(needleRotate);

        needleHighlight = new Region();
        needleHighlight.setMouseTransparent(true);
        needleHighlight.getStyleClass().setAll("needle-highlight");
        needleHighlight.getTransforms().setAll(needleRotate);

        knob = new Region();
        knob.setPickOnBounds(false);
        knob.getStyleClass().setAll("knob");

        dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.25));
        dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
        dropShadow.setRadius(0.015 * PREFERRED_WIDTH);
        dropShadow.setOffsetY(0.015 * PREFERRED_WIDTH);

        shadowGroup = new Group(needle, needleHighlight, knob);
        shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);

        titleText = new Text(getSkinnable().getTitle());
        titleText.setTextOrigin(VPos.CENTER);
        titleText.getStyleClass().setAll("title");

        unitText = new Text(getSkinnable().getUnit());
        unitText.setMouseTransparent(true);
        unitText.setTextOrigin(VPos.CENTER);
        unitText.getStyleClass().setAll("unit");

        valueText = new Text(String.format(Locale.US, "%." + getSkinnable().getDecimals() + "f", getSkinnable().getValue()));
        valueText.setMouseTransparent(true);
        valueText.setTextOrigin(VPos.CENTER);
        valueText.getStyleClass().setAll("value");

        // Set initial value
        angleStep          = getSkinnable().getAngleRange() / (getSkinnable().getMaxValue() - getSkinnable().getMinValue());
        double targetAngle = 180 - getSkinnable().getStartAngle() + (getSkinnable().getValue() - getSkinnable().getMinValue()) * angleStep;
        targetAngle        = clamp(180 - getSkinnable().getStartAngle(), 180 - getSkinnable().getStartAngle() + getSkinnable().getAngleRange(), targetAngle);
        needleRotate.setAngle(targetAngle);

        // Add all nodes
        pane = new Pane();
        pane.getChildren().setAll(background,
                                  ticksAndSectionsCanvas,
                                  titleText,
                                  ledFrame,
                                  ledMain,
                                  ledHl,
                                  unitText,
                                  valueText,
                                  shadowGroup);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().ledOnProperty().addListener(observable -> ledMain.setEffect(getSkinnable().isLedOn() ? glow : innerShadow));
        getSkinnable().ledColorProperty().addListener(observable -> handleControlPropertyChanged("LED_COLOR"));
        getSkinnable().ledVisibleProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().tickLabelOrientationProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickLabelFillProperty().addListener(observable -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().tickMarkFillProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().needleColorProperty().addListener(observable -> handleControlPropertyChanged("NEEDLE_COLOR"));
        getSkinnable().animatedProperty().addListener(observable -> handleControlPropertyChanged("ANIMATED"));
        getSkinnable().angleRangeProperty().addListener(observable -> handleControlPropertyChanged("ANGLE_RANGE"));
        getSkinnable().numberFormatProperty().addListener(observable -> handleControlPropertyChanged("RECALC"));
        getSkinnable().dropShadowEnabledProperty().addListener(observable -> handleControlPropertyChanged("DROP_SHADOW"));
        getSkinnable().getSections().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));
        getSkinnable().getAreas().addListener((ListChangeListener<Section>) change -> handleControlPropertyChanged("CANVAS_REFRESH"));


        getSkinnable().valueProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            withinSpeedLimit = !(Instant.now().minusMillis((long) getSkinnable().getAnimationDuration()).isBefore(lastCall));
            lastCall         = Instant.now();
            oldValue         = OLD_VALUE.doubleValue();
            rotateNeedle();
        });

        getSkinnable().minValueProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStep = getSkinnable().getAngleRange() / (getSkinnable().getMaxValue() - NEW_VALUE.doubleValue());
            needleRotate.setAngle((180 - getSkinnable().getStartAngle()) + (getSkinnable().getValue() - NEW_VALUE.doubleValue()) * angleStep);
            if (getSkinnable().getValue() < NEW_VALUE.doubleValue()) {
                getSkinnable().setValue(NEW_VALUE.doubleValue());
                oldValue = NEW_VALUE.doubleValue();
            }
        });
        getSkinnable().maxValueProperty().addListener((OV, OLD_VALUE, NEW_VALUE) -> {
            angleStep = getSkinnable().getAngleRange() / (NEW_VALUE.doubleValue() - getSkinnable().getMinValue());
            needleRotate.setAngle((180 - getSkinnable().getStartAngle()) + (getSkinnable().getValue() - getSkinnable().getMinValue()) * angleStep);
            if (getSkinnable().getValue() > NEW_VALUE.doubleValue()) {
                getSkinnable().setValue(NEW_VALUE.doubleValue());
                oldValue = NEW_VALUE.doubleValue();
            }
        });

        needleRotate.angleProperty().addListener(observable -> handleControlPropertyChanged("ANGLE"));
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("ANGLE".equals(PROPERTY)) {
            double currentValue = (needleRotate.getAngle() + getSkinnable().getStartAngle() - 180) / angleStep + getSkinnable().getMinValue();
            valueText.setText(limitString + String.format(Locale.US, "%." + getSkinnable().getDecimals() + "f", currentValue));
            valueText.setTranslateX((size - valueText.getLayoutBounds().getWidth()) * 0.5);
        } else if ("DROP_SHADOW".equals(PROPERTY)) {
            shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);
        } else if ("CANVAS_REFRESH".equals(PROPERTY)) {
            ticksAndSections.clearRect(0, 0, size, size);
            drawSections(ticksAndSections);
            drawAreas(ticksAndSections);
            drawTickMarks(ticksAndSections);
        } else if ("NEEDLE_COLOR".equals(PROPERTY)) {
            needle.setStyle("-needle-color: " + (colorToCss(getSkinnable().getNeedleColor())) + ";");
        } else if ("LED_COLOR".equals(PROPERTY)) {
            ledMain.setStyle("-led-color: " + (colorToCss(getSkinnable().getLedColor())));
            resize();
        }
    }


    // ******************** Utility methods ***********************************
    private String colorToCss(final Color COLOR) {
        return COLOR.toString().replace("0x", "#");
    }


    // ******************** Private Methods ***********************************
    private void rotateNeedle() {
        timeline.stop();
        angleStep          = getSkinnable().getAngleRange() / (getSkinnable().getMaxValue() - getSkinnable().getMinValue());
        double targetAngle = 180 - getSkinnable().getStartAngle() + (getSkinnable().getValue() - getSkinnable().getMinValue()) * angleStep;
        targetAngle        = clamp(180 - getSkinnable().getStartAngle(), 180 - getSkinnable().getStartAngle() + getSkinnable().getAngleRange(), targetAngle);
        if (withinSpeedLimit && getSkinnable().isAnimated()) {
            //double animationDuration = (getSkinnable().getAnimationDuration() / (getSkinnable().getMaxValueOne() - getSkinnable().getMinValueOne())) * Math.abs(getSkinnable().getValueOne() - getSkinnable().getOldValue());
            final KeyValue KEY_VALUE = new KeyValue(needleRotate.angleProperty(), targetAngle, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
            final KeyFrame KEY_FRAME = new KeyFrame(Duration.millis(getSkinnable().getAnimationDuration()), KEY_VALUE);
            timeline.getKeyFrames().setAll(KEY_FRAME);
            timeline.play();
        } else {
            needleRotate.setAngle(targetAngle);
        }
    }

    private void drawTickMarks(final GraphicsContext CTX) {
        double  sinValue;
        double  cosValue;
        double  startAngle   = getSkinnable().getStartAngle();
        double  orthText     = RadialGauge.TickLabelOrientation.ORTHOGONAL == getSkinnable().getTickLabelOrientation() ? 0.33 : 0.31;
        Point2D center       = new Point2D(size * 0.5, size * 0.5);
        boolean smallRange   = getSkinnable().getMaxValue() - getSkinnable().getMinValue() < 10;
        double  tmpMinValue  = smallRange ? getSkinnable().getMinValue() * 10 : getSkinnable().getMinValue();
        double  tmpMaxValue  = smallRange ? getSkinnable().getMaxValue() * 10 : getSkinnable().getMaxValue();
        double  tmpAngleStep = smallRange ? angleStep / 10 : angleStep;

        for (double angle = 0, counter = tmpMinValue ; Double.compare(counter, tmpMaxValue) <= 0 ; angle -= tmpAngleStep, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            Point2D innerMainPoint   = new Point2D(center.getX() + size * 0.368 * sinValue, center.getY() + size * 0.368 * cosValue);
            Point2D innerMediumPoint = new Point2D(center.getX() + size * 0.388 * sinValue, center.getY() + size * 0.388 * cosValue);
            Point2D innerMinorPoint  = new Point2D(center.getX() + size * 0.3975 * sinValue, center.getY() + size * 0.3975 * cosValue);
            Point2D outerPoint       = new Point2D(center.getX() + size * 0.432 * sinValue, center.getY() + size * 0.432 * cosValue);
            Point2D textPoint        = new Point2D(center.getX() + size * orthText * sinValue, center.getY() + size * orthText * cosValue);

            CTX.setStroke(getSkinnable().getTickMarkFill());
            if (counter % getSkinnable().getMajorTickSpace() == 0) {
                // Draw major tickmark
                CTX.setLineWidth(size * 0.0055);
                CTX.strokeLine(innerMainPoint.getX(), innerMainPoint.getY(), outerPoint.getX(), outerPoint.getY());

                // Draw text
                CTX.save();
                CTX.translate(textPoint.getX(), textPoint.getY());
                switch(getSkinnable().getTickLabelOrientation()) {
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
                CTX.setFont(Font.font("Verdana", FontWeight.NORMAL, 0.045 * size));
                CTX.setTextAlign(TextAlignment.CENTER);
                CTX.setTextBaseline(VPos.CENTER);
                CTX.setFill(getSkinnable().getTickLabelFill());
                CTX.fillText(Integer.toString((int) (smallRange ? counter / 10 : counter)), 0, 0);
                CTX.restore();
            } else if (getSkinnable().getMinorTickSpace() % 2 != 0 && counter % 5 == 0) {
                CTX.setLineWidth(size * 0.0035);
                CTX.strokeLine(innerMediumPoint.getX(), innerMediumPoint.getY(), outerPoint.getX(), outerPoint.getY());
            } else if (counter % getSkinnable().getMinorTickSpace() == 0) {
                CTX.setLineWidth(size * 0.00225);
                CTX.strokeLine(innerMinorPoint.getX(), innerMinorPoint.getY(), outerPoint.getX(), outerPoint.getY());
            }
        }
    }

    private final void drawSections(final GraphicsContext CTX) {
        final double xy        = (size - 0.83425 * size) / 2;
        final double wh        = size * 0.83425;
        final double MIN_VALUE = getSkinnable().getMinValue();
        final double MAX_VALUE = getSkinnable().getMaxValue();
        final double OFFSET    = 90 - getSkinnable().getStartAngle();

        IntStream.range(0, getSkinnable().getSections().size()).parallel().forEachOrdered(
            i -> {
                final Section SECTION = getSkinnable().getSections().get(i);
                final double SECTION_START_ANGLE;
                if (Double.compare(SECTION.getStart(), MAX_VALUE) <= 0 && Double.compare(SECTION.getStop(), MIN_VALUE) >= 0) {
                    if (SECTION.getStart() < MIN_VALUE && SECTION.getStop() < MAX_VALUE) {
                        SECTION_START_ANGLE = MIN_VALUE * angleStep;
                    } else {
                        SECTION_START_ANGLE = (SECTION.getStart() - MIN_VALUE) * angleStep;
                    }
                    final double SECTION_ANGLE_EXTEND;
                    if (SECTION.getStop() > MAX_VALUE) {
                        SECTION_ANGLE_EXTEND = (MAX_VALUE - SECTION.getStart()) * angleStep;
                    } else {
                        SECTION_ANGLE_EXTEND = (SECTION.getStop() - SECTION.getStart()) * angleStep;
                    }

                    CTX.save();
                    switch(i) {
                        case 0: CTX.setStroke(getSkinnable().getSectionFill0()); break;
                        case 1: CTX.setStroke(getSkinnable().getSectionFill1()); break;
                        case 2: CTX.setStroke(getSkinnable().getSectionFill2()); break;
                        case 3: CTX.setStroke(getSkinnable().getSectionFill3()); break;
                        case 4: CTX.setStroke(getSkinnable().getSectionFill4()); break;
                        case 5: CTX.setStroke(getSkinnable().getSectionFill5()); break;
                        case 6: CTX.setStroke(getSkinnable().getSectionFill6()); break;
                        case 7: CTX.setStroke(getSkinnable().getSectionFill7()); break;
                        case 8: CTX.setStroke(getSkinnable().getSectionFill8()); break;
                        case 9: CTX.setStroke(getSkinnable().getSectionFill9()); break;
                    }
                    CTX.setLineWidth(size * 0.0415);
                    CTX.setLineCap(StrokeLineCap.BUTT);
                    CTX.strokeArc(xy, xy, wh, wh, -(OFFSET + SECTION_START_ANGLE), -SECTION_ANGLE_EXTEND, ArcType.OPEN);
                    CTX.restore();
                }
            }
                                                                                         );
    }

    private final void drawAreas(final GraphicsContext CTX) {
        final double xy        = (size - 0.7925 * size) / 2;
        final double wh        = size * 0.7925;
        final double MIN_VALUE = getSkinnable().getMinValue();
        final double MAX_VALUE = getSkinnable().getMaxValue();
        final double OFFSET    = 90 - getSkinnable().getStartAngle();

        IntStream.range(0, getSkinnable().getAreas().size()).parallel().forEachOrdered(
            i -> {
                final Section AREA = getSkinnable().getAreas().get(i);
                final double AREA_START_ANGLE;
                if (Double.compare(AREA.getStart(), MAX_VALUE) <= 0 && Double.compare(AREA.getStop(), MIN_VALUE) >= 0) {
                    if (AREA.getStart() < MIN_VALUE && AREA.getStop() < MAX_VALUE) {
                        AREA_START_ANGLE = MIN_VALUE * angleStep;
                    } else {
                        AREA_START_ANGLE = (AREA.getStart() - MIN_VALUE) * angleStep;
                    }
                    final double AREA_ANGLE_EXTEND;
                    if (AREA.getStop() > MAX_VALUE) {
                        AREA_ANGLE_EXTEND = (MAX_VALUE - AREA.getStart()) * angleStep;
                    } else {
                        AREA_ANGLE_EXTEND = (AREA.getStop() - AREA.getStart()) * angleStep;
                    }

                    CTX.save();
                    switch(i) {
                        case 0: CTX.setFill(getSkinnable().getAreaFill0()); break;
                        case 1: CTX.setFill(getSkinnable().getAreaFill1()); break;
                        case 2: CTX.setFill(getSkinnable().getAreaFill2()); break;
                        case 3: CTX.setFill(getSkinnable().getAreaFill3()); break;
                        case 4: CTX.setFill(getSkinnable().getAreaFill4()); break;
                        case 5: CTX.setFill(getSkinnable().getAreaFill5()); break;
                        case 6: CTX.setFill(getSkinnable().getAreaFill6()); break;
                        case 7: CTX.setFill(getSkinnable().getAreaFill7()); break;
                        case 8: CTX.setFill(getSkinnable().getAreaFill8()); break;
                        case 9: CTX.setFill(getSkinnable().getAreaFill9()); break;
                    }
                    CTX.fillArc(xy, xy, wh, wh, -(OFFSET + AREA_START_ANGLE), - AREA_ANGLE_EXTEND, ArcType.ROUND);
                    CTX.restore();
                }
            }
                                                                                      );
    }

    private double clamp(final double MIN_VALUE, final double MAX_VALUE, final double VALUE) {
        if (VALUE < MIN_VALUE) return MIN_VALUE;
        if (VALUE > MAX_VALUE) return MAX_VALUE;
        return VALUE;
    }

    private void resizeText() {
        titleText.setFont(Fonts.robotoMedium(size * 0.06));
        titleText.setTranslateX((size - titleText.getLayoutBounds().getWidth()) * 0.5);
        titleText.setTranslateY(size * 0.35);

        unitText.setFont(Fonts.robotoMedium(size * 0.06));
        unitText.setTranslateX((size - unitText.getLayoutBounds().getWidth()) * 0.5);
        unitText.setTranslateY(size * 0.65);

        valueText.setFont(Fonts.robotoBold(size * 0.1));
        valueText.setTranslateX((size - valueText.getLayoutBounds().getWidth()) * 0.5);
        valueText.setTranslateY(size * 0.85);
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
            if (getSkinnable().isSectionsVisible()) drawSections(ticksAndSections);
            if (getSkinnable().isAreasVisible()) drawAreas(ticksAndSections);
            drawTickMarks(ticksAndSections);
            ticksAndSectionsCanvas.setCache(true);
            ticksAndSectionsCanvas.setCacheHint(CacheHint.QUALITY);

            innerShadow.setRadius(0.01 * size);
            glow.setRadius(0.05 * size);
            glow.setColor(getSkinnable().getLedColor());

            ledFrame.setPrefSize(0.06 * size, 0.06 * size);
            ledFrame.relocate(0.61 * size, 0.40 * size);

            ledMain.setPrefSize(0.0432 * size, 0.0432 * size);
            ledMain.relocate(0.61 * size + 0.0084 * size, 0.40 * size + 0.0084 * size);

            ledHl.setPrefSize(0.0348 * size, 0.0348 * size);
            ledHl.relocate(0.61 * size + 0.0126 * size, 0.40 * size + 0.0126 * size);

            boolean ledVisible = getSkinnable().isLedVisible();
            ledFrame.setManaged(ledVisible);
            ledFrame.setVisible(ledVisible);
            ledMain.setManaged(ledVisible);
            ledMain.setVisible(ledVisible);
            ledHl.setManaged(ledVisible);
            ledHl.setVisible(ledVisible);

            needle.setPrefSize(size * 0.04, size * 0.425);
            needle.relocate((size - needle.getPrefWidth()) * 0.5, size * 0.5 - needle.getPrefHeight());
            needleRotate.setPivotX(needle.getPrefWidth() * 0.5);
            needleRotate.setPivotY(needle.getPrefHeight());

            needleHighlight.setPrefSize(size * 0.04, size * 0.425);
            needleHighlight.setTranslateX((size - needle.getPrefWidth()) * 0.5);
            needleHighlight.setTranslateY(size * 0.5 - needle.getPrefHeight());

            knob.setPrefSize(size * 0.15, size * 0.15);
            knob.setTranslateX((size - knob.getPrefWidth()) * 0.5);
            knob.setTranslateY((size - knob.getPrefHeight()) * 0.5);

            resizeText();
        }
    }
}
