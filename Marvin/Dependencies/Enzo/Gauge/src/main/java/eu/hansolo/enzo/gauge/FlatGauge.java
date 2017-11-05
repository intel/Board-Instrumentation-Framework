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

import eu.hansolo.enzo.common.GradientLookup;
import eu.hansolo.enzo.fonts.Fonts;
import eu.hansolo.enzo.gauge.skin.FlatGaugeSkin;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * Created by hansolo on 19.10.15.
 */
public class FlatGauge extends Control {
    private static final double   PREFERRED_WIDTH          = 300;
    private static final double   PREFERRED_HEIGHT         = 300;
    private static final double   MINIMUM_WIDTH            = 50;
    private static final double   MINIMUM_HEIGHT           = 50;
    private static final double   MAXIMUM_WIDTH            = 1024;
    private static final double   MAXIMUM_HEIGHT           = 1024;

    private static final Color    DEFAULT_BAR_COLOR        = Color.web("#00ffff");
    private static final Color    DEFAULT_BACKGROUND_COLOR = Color.web("#ffffff");
    private static final Color    DEFAULT_TITLE_TEXT_COLOR = Color.web("#333333");
    private static final Color    DEFAULT_VALUE_TEXT_COLOR = Color.web("#333333");
    private static final Color    DEFAULT_UNIT_TEXT_COLOR  = Color.web("#333333");
    private static final Color    DEFAULT_SEPARATOR_COLOR  = Color.web("#d0d0d0");

    private double                size;
    private double                width;
    private double                height;
    private Circle                colorRing;
    private Arc                   bar;
    private Line                  separator;
    private Circle                background;
    private Text                  titleText;
    private Text                  valueText;
    private Text                  unitText;
    private Pane                  pane;
    private double                range;
    private double                angleStep;
    private int                   decimals;
    private int                   animationDurationInMs;
    private boolean               multiColor;
    private DoubleProperty        minValue;
    private DoubleProperty        maxValue;
    private DoubleProperty        value;
    private DoubleProperty        currentValue;
    private StringProperty        title;
    private StringProperty        unit;
    private BooleanProperty       separatorVisible;
    private BooleanProperty       colorRingVisible;
    private ObjectProperty<Paint> barColor;
    private ObjectProperty<Paint> backgroundColor;
    private ObjectProperty<Paint> valueTextColor;
    private ObjectProperty<Paint> titleTextColor;
    private ObjectProperty<Paint> unitTextColor;
    private ObjectProperty<Paint> separatorColor;
    private Timeline              barTimeline;
    private GradientLookup        gradientLookup;


    // ******************** Constructors **************************************
    public FlatGauge() {
        getStylesheets().add(FlatGauge.class.getResource("flat-gauge.css").toExternalForm());
        getStyleClass().add("flat-gauge");

        range                 = 100d;
        angleStep             = 360d / range;
        decimals              = 0;
        animationDurationInMs = 500;
        minValue              = new DoublePropertyBase(0) {
            @Override public void set(final double VALUE) {
                if (VALUE > maxValue.get()) maxValue.set(VALUE + 0.1);
                double v  = clamp(-Double.MAX_VALUE, maxValue.get() - 0.1, VALUE);
                super.set(v);
            }
            @Override public Object getBean() { return FlatGauge.this;}
            @Override public String getName() { return "minValue"; }
        };
        maxValue              = new DoublePropertyBase(100) {
            @Override public void set(final double VALUE) {
                if (VALUE < minValue.get()) minValue.set(VALUE - 0.1);
                double v  = clamp(minValue.get() + 0.1, Double.MAX_VALUE, VALUE);
                //range     = v - minValue.get();
                //angleStep = 360d / (range + minValue.get());
                super.set(v);
            }
            @Override public Object getBean() { return FlatGauge.this;}
            @Override public String getName() { return "maxBarValue"; }
        };
        value                 = new DoublePropertyBase(minValue.get()) {
            @Override public void set(final double VALUE) { super.set(clamp(minValue.get(), maxValue.get(), VALUE));}
            @Override public Object getBean() { return FlatGauge.this;}
            @Override public String getName() { return "value"; }
        };
        currentValue          = new SimpleDoubleProperty(this, "currentValue", 0d);
        title                 = new SimpleStringProperty(this, "title", "");
        unit                  = new SimpleStringProperty(this, "unit", "");
        separatorVisible      = new SimpleBooleanProperty(this, "separatorVisible", true);
        colorRingVisible      = new SimpleBooleanProperty(this, "colorRingVisible", true);
        barColor              = new StyleableObjectProperty<Paint>(DEFAULT_BAR_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.BAR_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "barColor"; }
        };
        backgroundColor       = new StyleableObjectProperty<Paint>(DEFAULT_BACKGROUND_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.BACKGROUND_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "backgroundColor"; }
        };
        titleTextColor        = new StyleableObjectProperty<Paint>(DEFAULT_TITLE_TEXT_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.TITLE_TEXT_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "titleTextColor"; }
        };
        valueTextColor        = new StyleableObjectProperty<Paint>(DEFAULT_VALUE_TEXT_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.VALUE_TEXT_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "valueTextColor"; }
        };
        unitTextColor         = new StyleableObjectProperty<Paint>(DEFAULT_UNIT_TEXT_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.UNIT_TEXT_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "unitTextColor"; }
        };
        separatorColor        = new StyleableObjectProperty<Paint>(DEFAULT_SEPARATOR_COLOR) {
            @Override public CssMetaData getCssMetaData() { return StyleableProperties.SEPARATOR_COLOR; }
            @Override public Object getBean() { return FlatGauge.this; }
            @Override public String getName() { return "separatorColor"; }
        };
        barTimeline           = new Timeline();
        multiColor            = false;
        gradientLookup        = new GradientLookup(new Stop(0.00, Color.BLUE),
                                                   new Stop(0.20, Color.CYAN),
                                                   new Stop(0.40, Color.LIME),
                                                   new Stop(0.60, Color.YELLOW),
                                                   new Stop(0.80, Color.ORANGE),
                                                   new Stop(1.00, Color.RED));
        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        if (Double.compare(getMinWidth(), 0.0) <= 0 || Double.compare(getMinHeight(), 0.0) <= 0) {
            setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getMaxWidth(), 0.0) <= 0 || Double.compare(getMaxHeight(), 0.0) <= 0) {
            setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        colorRing = new Circle(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.5);
        colorRing.setFill(Color.TRANSPARENT);
        colorRing.setStrokeWidth(PREFERRED_WIDTH * 0.0075);
        colorRing.setStroke(DEFAULT_BAR_COLOR);
        colorRing.setVisible(colorRingVisible.get());

        bar = new Arc(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.468, PREFERRED_HEIGHT * 0.468, 90, 0);
        bar.setType(ArcType.OPEN);
        bar.setStroke(DEFAULT_BAR_COLOR);
        bar.setStrokeWidth(PREFERRED_WIDTH * 0.15);
        bar.setStrokeLineCap(StrokeLineCap.BUTT);
        bar.setFill(null);

        separator = new Line(PREFERRED_WIDTH * 0.5, 1, PREFERRED_WIDTH * 0.5, 0.16667 * PREFERRED_HEIGHT);
        separator.setStroke(DEFAULT_SEPARATOR_COLOR);
        separator.setFill(Color.TRANSPARENT);
        separator.setVisible(separatorVisible.get());

        background = new Circle(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.363);
        background.setFill(DEFAULT_BACKGROUND_COLOR);

        titleText = new Text("");
        titleText.setFont(Fonts.robotoLight(PREFERRED_WIDTH * 0.08));
        titleText.setFill(DEFAULT_TITLE_TEXT_COLOR);

        valueText = new Text(String.format(Locale.US, "%." + decimals + "f", getValue()));
        valueText.setFont(Fonts.robotoRegular(PREFERRED_WIDTH * 0.27333));
        valueText.setFill(DEFAULT_VALUE_TEXT_COLOR);

        unitText = new Text(unit.get());
        unitText.setFont(Fonts.robotoLight(PREFERRED_WIDTH * 0.08));
        unitText.setFill(DEFAULT_UNIT_TEXT_COLOR);

        pane = new Pane(colorRing, bar, separator, background, titleText, valueText, unitText);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        value.addListener(o -> handleControlPropertyChanged("VALUE"));
        currentValue.addListener(o -> handleControlPropertyChanged("CURRENT_VALUE"));
        title.addListener(o -> handleControlPropertyChanged("TITLE"));
        unit.addListener(o -> handleControlPropertyChanged("UNIT"));
        barColor.addListener(o -> handleControlPropertyChanged("BAR_COLOR"));
        backgroundColor.addListener(o -> handleControlPropertyChanged("BACKGROUND_COLOR"));
        valueTextColor.addListener(o -> handleControlPropertyChanged("VALUE_TEXT_COLOR"));
        unitTextColor.addListener(o -> handleControlPropertyChanged("UNIT_TEXT_COLOR"));
        titleTextColor.addListener(o -> handleControlPropertyChanged("TITLE_TEXT_COLOR"));
        separatorColor.addListener(o -> handleControlPropertyChanged("SEPARATOR_COLOR"));
        separatorVisible.addListener(o -> handleControlPropertyChanged("SEPARATOR_VISIBLE"));
        colorRingVisible.addListener(o -> handleControlPropertyChanged("COLOR_RING_VISIBLE"));
    }


    // ******************** Methods *******************************************
    private void handleControlPropertyChanged(final String PROPERTY) {
        if ("VALUE".equals(PROPERTY)) {
            barTimeline.stop();
            range     = maxValue.get() - minValue.get();
            angleStep = 360d / range;
            currentValue.set(clamp(minValue.get(), maxValue.get(), currentValue.get()));
            final KeyValue KV_START = new KeyValue(currentValue, currentValue.get(), Interpolator.EASE_BOTH);
            final KeyValue KV_STOP  = new KeyValue(currentValue, value.get(), Interpolator.EASE_BOTH);
            final KeyFrame KF_START = new KeyFrame(Duration.ZERO, KV_START);
            final KeyFrame KF_STOP  = new KeyFrame(Duration.millis(animationDurationInMs), KV_STOP);
            barTimeline.getKeyFrames().setAll(KF_START, KF_STOP);
            barTimeline.play();
        } else if ("CURRENT_VALUE".equals(PROPERTY)) {
            setBar(currentValue.get());
        } else if ("TITLE".equals(PROPERTY)) {
            titleText.setText(title.get());
            resizeTitleText();
        } else if ("UNIT".equals(PROPERTY)) {
            unitText.setText(unit.get());
            resizeUnitText();
        } else if ("BAR_COLOR".equals(PROPERTY)) {
            bar.setStroke(barColor.get());
            colorRing.setStroke(barColor.get());
        } else if ("BACKGROUND_COLOR".equals(PROPERTY)) {
            background.setFill(backgroundColor.get());
        } else if ("VALUE_TEXT_COLOR".equals(PROPERTY)) {
            valueText.setFill(valueTextColor.get());
        } else if ("UNIT_TEXT_COLOR".equals(PROPERTY)) {
            unitText.setFill(unitTextColor.get());
        } else if ("TITLE_TEXT_COLOR".equals(PROPERTY)) {
            titleText.setFill(titleTextColor.get());
        } else if ("SEPARATOR_COLOR".equals(PROPERTY)) {
            separator.setStroke(separatorColor.get());
        } else if ("SEPARATOR_VISIBLE".equals(PROPERTY)) {
            separator.setVisible(separatorVisible.get());
        } else if ("COLOR_RING_VISIBLE".equals(PROPERTY)) {
            colorRing.setVisible(colorRingVisible.get());
        }
    }

    public double getMinValue() { return minValue.get(); }
    public void setMinValue(final double MIN_VALUE) { minValue.set(MIN_VALUE); }
    public DoubleProperty minValueProperty() { return minValue; }

    public double getMaxValue() { return maxValue.get(); }
    public void setMaxValue(final double MAX_VALUE) { maxValue.set(MAX_VALUE); }
    public DoubleProperty maxValueProperty() { return maxValue; }

    public double getValue() { return value.get(); }
    public void setValue(final double VALUE) { value.set(VALUE); }
    public DoubleProperty valueProperty() { return value; }

    public ReadOnlyDoubleProperty currentValueProperty() { return currentValue; }

    public String getTitle() { return title.get(); }
    public void setTitle(final String TITLE) { title.set(TITLE); }
    public StringProperty titleProperty() { return title; }

    public String getUnit() { return unit.get(); }
    public void setUnit(final String UNIT) { unit.set(UNIT); }
    public StringProperty unitProperty() { return unit; }

    public Paint getBarColor() { return barColor.get(); }
    public void setBarColor(final Color BAR_COLOR) {
        if (multiColor) return;
        barColor.set(BAR_COLOR);
    }
    public ObjectProperty<Paint> barColorProperty() { return barColor; }

    public Paint getBackgroundColor() { return backgroundColor.get(); }
    public void setBackgroundColor(final Color BACKGROUND_COLOR) { backgroundColor.set(BACKGROUND_COLOR); }
    public ObjectProperty<Paint> backgroundColorProperty() { return backgroundColor; }

    public Paint getValueTextColor() { return valueTextColor.get(); }
    public void setValueTextColor(final Color VALUE_TEXT_COLOR) { valueTextColor.set(VALUE_TEXT_COLOR); }
    public ObjectProperty<Paint> valueTextColorProperty() { return valueTextColor; }

    public Paint getTitleTextColor() { return titleTextColor.get(); }
    public void setTitleTextColor(final Color TITLE_TEXT_COLOR) { titleTextColor.set(TITLE_TEXT_COLOR); }
    public ObjectProperty<Paint> titleTextColorProperty() { return titleTextColor; }

    public Paint getUnitTextColor() { return unitTextColor.get(); }
    public void setUnitTextColor(final Color UNIT_TEXT_COLOR) { unitTextColor.set(UNIT_TEXT_COLOR); }
    public ObjectProperty<Paint> unitTextColorProperty() { return unitTextColor; }

    public Paint getSeparatorColor() { return separatorColor.get(); }
    public void setSeparatorColor(final Color SEPARATOR_COLOR) { separatorColor.set(SEPARATOR_COLOR); }
    public ObjectProperty<Paint> separatorColorProperty() { return separatorColor; }

    public boolean isSeparatorVisible() { return separatorVisible.get(); }
    public void setSeparatorVisible(final boolean SEPARATOR_VISIBLE) { separatorVisible.set(SEPARATOR_VISIBLE); }
    public BooleanProperty separatorVisibleProperty() { return separatorVisible; }

    public boolean isColorRingVisible() { return colorRingVisible.get(); }
    public void setColorRingVisible(final boolean COLOR_RING_VISIBLE) { colorRingVisible.set(COLOR_RING_VISIBLE); }
    public BooleanProperty colorRingVisibleProperty() { return colorRingVisible; }

    public void setGradient(final Stop... STOPS) { gradientLookup.setStops(STOPS); }

    public void setDecimals(final int DECIMALS) {
        decimals = clamp(0, 3, DECIMALS);
        resizeValueText();
    }

    public int getAnimationDurationInMs() { return animationDurationInMs; }
    public void setAnimationDurationInMs(final int ANIMATION_DURATION_IN_MS) { animationDurationInMs = clamp(1, 10000, ANIMATION_DURATION_IN_MS); }

    public boolean isMultiColor()  { return multiColor; }
    public void setMultiColor(final boolean MULTI_COLOR) {
        multiColor = MULTI_COLOR;
        if (multiColor) return;
        bar.setFill(barColor.get());
    }

    private void setBar(final double VALUE) {
        if (multiColor) { bar.setFill(gradientLookup.getColorAt((VALUE - minValue.get()) / range)); }
        if (minValue.get() > 0) {
            bar.setLength(((VALUE - minValue.get()) * (-1)) * angleStep);
        } else {
            if (VALUE < 0) {
                bar.setLength((-VALUE + minValue.get()) * angleStep);
            } else {
                bar.setLength(((minValue.get() - VALUE) * angleStep));
            }
        }
        valueText.setText(String.format(Locale.US, "%." + decimals + "f", VALUE));
        resizeValueText();
    }


    // ******************** Utility methods ***********************************
    private String colorToCss(final Color COLOR) { return COLOR.toString().replace("0x", "#"); }

    private double clamp(final double MIN_VALUE, final double MAX_VALUE, final double VALUE) {
        if (VALUE < MIN_VALUE) return MIN_VALUE;
        if (VALUE > MAX_VALUE) return MAX_VALUE;
        return VALUE;
    }
    private int clamp(final int MIN_VALUE, final int MAX_VALUE, final int VALUE) {
        if (VALUE < MIN_VALUE) return MIN_VALUE;
        if (VALUE > MAX_VALUE) return MAX_VALUE;
        return VALUE;
    }


    // ******************** CSS Meta Data *************************************
    @Override protected Skin createDefaultSkin() { return new FlatGaugeSkin(this); }

    @Override public String getUserAgentStylesheet() { return getClass().getResource("flat-gauge.css").toExternalForm(); }

    private static class StyleableProperties {
        private static final CssMetaData<FlatGauge, Paint> BAR_COLOR =
            new CssMetaData<FlatGauge, Paint>("-bar-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_BAR_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.barColor || !node.barColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.barColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getBarColor();}
            };

        private static final CssMetaData<FlatGauge, Paint> BACKGROUND_COLOR =
            new CssMetaData<FlatGauge, Paint>("-background-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_BACKGROUND_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.backgroundColor || !node.backgroundColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.backgroundColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getBackgroundColor();}
            };

        private static final CssMetaData<FlatGauge, Paint> TITLE_TEXT_COLOR =
            new CssMetaData<FlatGauge, Paint>("-title-text-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_TITLE_TEXT_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.titleTextColor || !node.titleTextColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.titleTextColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getTitleTextColor();}
            };

        private static final CssMetaData<FlatGauge, Paint> VALUE_TEXT_COLOR =
            new CssMetaData<FlatGauge, Paint>("-value-text-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_VALUE_TEXT_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.valueTextColor || !node.valueTextColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.valueTextColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getValueTextColor();}
            };

        private static final CssMetaData<FlatGauge, Paint> UNIT_TEXT_COLOR =
            new CssMetaData<FlatGauge, Paint>("-unit-text-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_UNIT_TEXT_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.unitTextColor || !node.unitTextColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.unitTextColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getUnitTextColor();}
            };

        private static final CssMetaData<FlatGauge, Paint> SEPARATOR_COLOR =
            new CssMetaData<FlatGauge, Paint>("-separator-color", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), DEFAULT_SEPARATOR_COLOR) {
                @Override public boolean isSettable(FlatGauge node) {
                    return null == node.separatorColor || !node.separatorColor.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(FlatGauge node) {
                    return (StyleableProperty) node.separatorColorProperty();
                }
                @Override public Paint getInitialValue(FlatGauge node) { return node.getSeparatorColor();}
            };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            Collections.addAll(styleables,
                               BAR_COLOR,
                               BACKGROUND_COLOR,
                               TITLE_TEXT_COLOR,
                               VALUE_TEXT_COLOR,
                               UNIT_TEXT_COLOR,
                               SEPARATOR_COLOR);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() { return StyleableProperties.STYLEABLES; }

    @Override public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() { return getClassCssMetaData(); }


    // ******************** Resizing ******************************************
    private void resizeTitleText() {
        titleText.setFont(Fonts.robotoLight(size * 0.08));
        double decrement = 0d;
        while (titleText.getLayoutBounds().getWidth() > 0.56667 * size && titleText.getFont().getSize() > 0) {
            titleText.setFont(Fonts.robotoLight(size * (0.08 - decrement)));
            decrement += 0.01;
        }
        titleText.relocate((size - titleText.getLayoutBounds().getWidth()) * 0.5, size * 0.225);
    }
    private void resizeValueText() {
        valueText.setFont(Fonts.robotoRegular(size * 0.3));
        double decrement = 0d;
        while (valueText.getLayoutBounds().getWidth() > 0.5 * size && valueText.getFont().getSize() > 0) {
            valueText.setFont(Fonts.robotoRegular(size * (0.3 - decrement)));
            decrement += 0.01;
        }
        valueText.relocate((size - valueText.getLayoutBounds().getWidth()) * 0.5, (size - valueText.getLayoutBounds().getHeight()) * 0.5);
    }
    private void resizeUnitText() {
        unitText.setFont(Fonts.robotoLight(size * 0.08));
        double decrement = 0d;
        while (unitText.getLayoutBounds().getWidth() > 0.56667 * size && unitText.getFont().getSize() > 0) {
            unitText.setFont(Fonts.robotoLight(size * (0.08 - decrement)));
            decrement += 0.01;
        }
        unitText.relocate((size - unitText.getLayoutBounds().getWidth()) * 0.5, size * 0.66);
    }

    private void resize() {
        width  = getWidth();
        height = getHeight();
        size   = width < height ? width : height;

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.relocate((width - size) * 0.5, (height - size) * 0.5);

            colorRing.setCenterX(size * 0.5);
            colorRing.setCenterY(size * 0.5);
            colorRing.setRadius(size * 0.5);
            colorRing.setStrokeWidth(size * 0.0075);

            bar.setCenterX(size * 0.5);
            bar.setCenterY(size * 0.5);
            bar.setRadiusX(size * 0.421);
            bar.setRadiusY(size * 0.421);
            bar.setStrokeWidth(size * 0.12);

            separator.setStartX(size * 0.5);
            separator.setStartY(size * 0.02);
            separator.setEndX(size * 0.5);
            separator.setEndY(size * 0.138);

            background.setCenterX(size * 0.5);
            background.setCenterY(size * 0.5);
            background.setRadius(size * 0.363);

            resizeTitleText();
            resizeValueText();
            resizeUnitText();
        }
    }
}
