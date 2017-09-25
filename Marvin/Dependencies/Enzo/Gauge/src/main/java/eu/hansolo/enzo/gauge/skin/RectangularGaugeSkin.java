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
import eu.hansolo.enzo.gauge.RectangularGauge;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
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
 * Created by hansolo on 10.12.15.
 */
public class RectangularGaugeSkin extends SkinBase<RectangularGauge> implements Skin<RectangularGauge> {
    private static final double          PREFERRED_WIDTH  = 310;
    private static final double          PREFERRED_HEIGHT = 260;
    private static final double          MINIMUM_WIDTH    = 31;
    private static final double          MINIMUM_HEIGHT   = 26;
    private static final double          MAXIMUM_WIDTH    = 1024;
    private static final double          MAXIMUM_HEIGHT   = 858;
    private              double          aspectRatio      = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private              double          oldValue;
    private              double          width;
    private              double          height;
    private              Pane            pane;
    private              Region          foreground;
    private              Canvas          ticksAndSectionsCanvas;
    private              GraphicsContext ticksAndSections;
    private              Region          ledFrame;
    private              Region          ledMain;
    private              Region          ledHl;
    private              Region          needle;
    private              Region          needleHighlight;
    private              Rotate          needleRotate;
    private              Group           shadowGroup;
    private              InnerShadow     lightEffect;
    private              DropShadow      dropShadow;
    private              InnerShadow     innerShadow;
    private              DropShadow      glow;
    private              Text            titleText;
    private              Text            unitText;
    private              Label           lcdText;
    private              double          angleStep;
    private              Timeline        timeline;
    private              String          limitString;
    private              Instant         lastCall;
    private              boolean         withinSpeedLimit;


    // ******************** Constructors **************************************
    public RectangularGaugeSkin(RectangularGauge gauge) {
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

        if (getSkinnable().getPrefWidth() != PREFERRED_WIDTH || getSkinnable().getPrefHeight() != PREFERRED_HEIGHT) {
            aspectRatio = getSkinnable().getPrefHeight() / getSkinnable().getPrefWidth();
        }
    }

    private void initGraphics() {
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
        needle.getStyleClass().setAll(RectangularGauge.STYLE_CLASS_NEEDLE_STANDARD);
        needleRotate = new Rotate(180 - getSkinnable().getStartAngle());
        needleRotate.setAngle(needleRotate.getAngle() + (getSkinnable().getValue() - oldValue - getSkinnable().getMinValue()) * angleStep);
        needle.getTransforms().setAll(needleRotate);

        needleHighlight = new Region();
        needleHighlight.setMouseTransparent(true);
        needleHighlight.getStyleClass().setAll("needle-highlight");
        needleHighlight.getTransforms().setAll(needleRotate);

        dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.25));
        dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
        dropShadow.setRadius(0.015 * PREFERRED_WIDTH);
        dropShadow.setOffsetY(0.015 * PREFERRED_WIDTH);

        shadowGroup = new Group(needle, needleHighlight);
        shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);

        titleText = new Text(getSkinnable().getTitle());
        titleText.setTextOrigin(VPos.CENTER);
        titleText.getStyleClass().setAll("title");

        unitText = new Text(getSkinnable().getUnit());
        unitText.setMouseTransparent(true);
        unitText.setTextOrigin(VPos.CENTER);
        unitText.getStyleClass().setAll("unit");

        lcdText = new Label(String.format(Locale.US, "%." + getSkinnable().getDecimals() + "f", getSkinnable().getValue()));
        lcdText.getStyleClass().setAll("lcd-text");

        // Set initial value
        angleStep          = getSkinnable().getAngleRange() / (getSkinnable().getMaxValue() - getSkinnable().getMinValue());
        double targetAngle = 180 - getSkinnable().getStartAngle() + (getSkinnable().getValue() - getSkinnable().getMinValue()) * angleStep;
        targetAngle        = clamp(180 - getSkinnable().getStartAngle(), 180 - getSkinnable().getStartAngle() + getSkinnable().getAngleRange(), targetAngle);
        needleRotate.setAngle(targetAngle);

        lightEffect = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.65), 2, 0d, 0d, 2d);

        foreground = new Region();
        foreground.getStyleClass().setAll("foreground");
        foreground.setEffect(lightEffect);

        // Add all nodes
        pane = new Pane();
        pane.getChildren().setAll(ticksAndSectionsCanvas,
                                  ledFrame,
                                  ledMain,
                                  ledHl,
                                  unitText,
                                  lcdText,
                                  shadowGroup,
                                  foreground,
                                  titleText);

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
            lcdText.setText((limitString + String.format(Locale.US, "%." + getSkinnable().getDecimals() + "f", currentValue)));
        } else if ("DROP_SHADOW".equals(PROPERTY)) {
            shadowGroup.setEffect(getSkinnable().isDropShadowEnabled() ? dropShadow : null);
        } else if ("CANVAS_REFRESH".equals(PROPERTY)) {
            ticksAndSections.clearRect(0, 0, width, height);
            drawSections(ticksAndSections);
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
        double  orthText     = RectangularGauge.TickLabelOrientation.ORTHOGONAL == getSkinnable().getTickLabelOrientation() ? 0.51 : 0.52;
        Point2D center       = new Point2D(width * 0.5, height * 0.77);
        boolean smallRange   = getSkinnable().getMaxValue() - getSkinnable().getMinValue() < 10;
        double  tmpMinValue  = smallRange ? getSkinnable().getMinValue() * 10 : getSkinnable().getMinValue();
        double  tmpMaxValue  = smallRange ? getSkinnable().getMaxValue() * 10 : getSkinnable().getMaxValue();
        double  tmpAngleStep = smallRange ? angleStep / 10 : angleStep;
        for (double angle = 0, counter = tmpMinValue ; Double.compare(counter, tmpMaxValue) <= 0 ; angle -= tmpAngleStep, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            Point2D innerPoint       = new Point2D(center.getX() + width * 0.41987097 * sinValue, center.getY() + width * 0.41987097 * cosValue);
            Point2D outerMinorPoint  = new Point2D(center.getX() + width * 0.45387097 * sinValue, center.getY() + width * 0.45387097 * cosValue);
            Point2D outerMediumPoint = new Point2D(center.getX() + width * 0.46387097 * sinValue, center.getY() + width * 0.46387097 * cosValue);
            Point2D outerPoint       = new Point2D(center.getX() + width * 0.48387097 * sinValue, center.getY() + width * 0.48387097 * cosValue);
            Point2D textPoint        = new Point2D(center.getX() + width * orthText * sinValue, center.getY() + width * orthText * cosValue);

            CTX.setStroke(getSkinnable().getTickMarkFill());
            if (counter % getSkinnable().getMajorTickSpace() == 0) {
                // Draw major tickmark
                CTX.setLineWidth(height * 0.0055);
                CTX.strokeLine(innerPoint.getX(), innerPoint.getY(), outerPoint.getX(), outerPoint.getY());

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
                CTX.setFont(Font.font("Verdana", FontWeight.LIGHT, 0.04 * height));
                CTX.setTextAlign(TextAlignment.CENTER);
                CTX.setTextBaseline(VPos.CENTER);
                CTX.setFill(getSkinnable().getTickLabelFill());
                CTX.fillText(Integer.toString((int) (smallRange ? counter / 10 : counter)), 0, 0);
                CTX.restore();
            } else if (getSkinnable().getMinorTickSpace() % 2 != 0 && counter % 5 == 0) {
                CTX.setLineWidth(height * 0.0035);
                CTX.strokeLine(innerPoint.getX(), innerPoint.getY(), outerMediumPoint.getX(), outerMediumPoint.getY());
            } else if (counter % getSkinnable().getMinorTickSpace() == 0) {
                CTX.setLineWidth(height * 0.00225);
                CTX.strokeLine(innerPoint.getX(), innerPoint.getY(), outerMinorPoint.getX(), outerMinorPoint.getY());
            }
        }
    }

    private final void drawSections(final GraphicsContext CTX) {
        final double x         = width * 0.06;
        final double y         = width * 0.21;
        final double w         = width * 0.88;
        final double h         = height * 1.05;
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
                    CTX.setLineWidth(height * 0.0415);
                    CTX.setLineCap(StrokeLineCap.BUTT);
                    CTX.strokeArc(x, y, w, h, -(OFFSET + SECTION_START_ANGLE), -SECTION_ANGLE_EXTEND, ArcType.OPEN);
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
        titleText.setFont(Fonts.robotoMedium(height * 0.11));
        titleText.setTranslateX((width - titleText.getLayoutBounds().getWidth()) * 0.5);
        titleText.setTranslateY(height * 0.76);

        unitText.setFont(Fonts.robotoMedium(height * 0.1));
        unitText.setTranslateX((width - unitText.getLayoutBounds().getWidth()) * 0.5);
        unitText.setTranslateY(height * 0.37);

        lcdText.setStyle("-fx-background-radius: " + (0.0125 * height) + ";");
        lcdText.setFont(Fonts.digitalReadoutBold(height * 0.09));
        lcdText.setPrefSize(0.3 * width, 0.014 * height);
        lcdText.relocate((width - lcdText.getPrefWidth()) * 0.5, 0.44 * height);

    }

    private void resize() {
        width  = getSkinnable().getWidth();
        height = getSkinnable().getHeight();


        if (aspectRatio * width > height) {
            width = 1 / (aspectRatio / height);
        } else if (1 / (aspectRatio / height) > width) {
            height = aspectRatio * width;
        }

        if (width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            pane.relocate((getSkinnable().getWidth() - width) * 0.5, (getSkinnable().getHeight() - height) * 0.5);

            dropShadow.setRadius(0.01 * height);
            dropShadow.setOffsetY(0.01 * height);

            ticksAndSectionsCanvas.setWidth(width);
            ticksAndSectionsCanvas.setHeight(height);
            ticksAndSections.clearRect(0, 0, width, height);
            ticksAndSections.setFill(new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE,
                                                        new Stop(0.0, Color.TRANSPARENT),
                                                        new Stop(0.07692308, Color.rgb(180,180,180)),
                                                        new Stop(0.08461538, Color.rgb(235,235,235)),
                                                        new Stop(0.56923077, Color.rgb(255,255,255)),
                                                        new Stop(0.57692308, Color.rgb(180,180,180)),
                                                        new Stop(1.0, Color.TRANSPARENT)));

            ticksAndSections.fillRect(0, 0, width, height);
            if (getSkinnable().isSectionsVisible()) drawSections(ticksAndSections);
            drawTickMarks(ticksAndSections);
            ticksAndSectionsCanvas.setCache(true);
            ticksAndSectionsCanvas.setCacheHint(CacheHint.QUALITY);

            innerShadow.setRadius(0.01 * height);
            glow.setRadius(0.05 * height);
            glow.setColor(getSkinnable().getLedColor());

            ledFrame.setPrefSize(0.06 * height, 0.06 * height);
            ledFrame.relocate(0.11 * width, 0.10 * height);

            ledMain.setPrefSize(0.0432 * height, 0.0432 * height);
            ledMain.relocate(0.11 * width + 0.0084 * width, 0.10 * height + 0.0084 * width);

            ledHl.setPrefSize(0.0348 * height, 0.0348 * height);
            ledHl.relocate(0.11 * width + 0.0126 * width, 0.10 * height + 0.0126 * width);

            boolean ledVisible = getSkinnable().isLedVisible();
            ledFrame.setManaged(ledVisible);
            ledFrame.setVisible(ledVisible);
            ledMain.setManaged(ledVisible);
            ledMain.setVisible(ledVisible);
            ledHl.setManaged(ledVisible);
            ledHl.setVisible(ledVisible);

            needle.setPrefSize(width * 0.02, height * 0.58);
            needle.relocate((width - needle.getPrefWidth()) * 0.5, height * 0.77 - needle.getPrefHeight());
            needleRotate.setPivotX(needle.getPrefWidth() * 0.5);
            needleRotate.setPivotY(needle.getPrefHeight());

            needleHighlight.setPrefSize(width * 0.02, height * 0.58);
            needleHighlight.setTranslateX((width - needle.getPrefWidth()) * 0.5);
            needleHighlight.setTranslateY(height * 0.77 - needle.getPrefHeight());


            lightEffect.setRadius(0.00769231 * height);
            lightEffect.setOffsetY(0.00769231 * height);
            foreground.setPrefSize(width, height);

            resizeText();
        }
    }
}
