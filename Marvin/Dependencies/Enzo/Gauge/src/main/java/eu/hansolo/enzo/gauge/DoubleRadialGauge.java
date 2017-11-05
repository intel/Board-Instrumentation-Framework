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
import eu.hansolo.enzo.gauge.skin.DoubleRadialGaugeSkin;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;


/**
 * Created by hansolo on 01.12.15.
 */
public class DoubleRadialGauge extends Control {
    public enum NeedleType {
        STANDARD_ONE("needle-standard-one"),
        STANDARD_TWO("needle-standard-two");

        public final String STYLE_CLASS;

        NeedleType(final String STYLE_CLASS) {
            this.STYLE_CLASS = STYLE_CLASS;
        }
    }
    public enum TickLabelOrientation {
        ORTHOGONAL,
        HORIZONTAL,
        TANGENT
    }
    public enum NumberFormat {
        AUTO("0"),
        STANDARD("0"),
        FRACTIONAL("0.0#"),
        SCIENTIFIC("0.##E0"),
        PERCENTAGE("##0.0%");

        private final DecimalFormat DF;

        NumberFormat(final String FORMAT_STRING) {
            Locale.setDefault(new Locale("en", "US"));

            DF = new DecimalFormat(FORMAT_STRING);
        }

        public String format(final Number NUMBER) {
            return DF.format(NUMBER);
        }
    }

    public static final String       STYLE_CLASS_NEEDLE_ONE_STANDARD = NeedleType.STANDARD_ONE.STYLE_CLASS;
    public static final String       STYLE_CLASS_NEEDLE_TWO_STANDARD = NeedleType.STANDARD_TWO.STYLE_CLASS;
    private static final PseudoClass LED_ON_ONE_PSEUDO_CLASS         = PseudoClass.getPseudoClass("led-on-one");
    private static final PseudoClass LED_ON_TWO_PSEUDO_CLASS         = PseudoClass.getPseudoClass("led-on-two");
    private static final long        LED_BLINK_INTERVAL              = 500_000_000l;

    // Default section colors
    private static final Color                     DEFAULT_SECTION_FILL_0 = Color.rgb(0, 0, 178, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_1 = Color.rgb(0, 128, 255, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_2 = Color.rgb(  0, 255, 255, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_3 = Color.rgb(  0, 255,  64, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_4 = Color.rgb(128, 255,   0, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_5 = Color.rgb(255, 255,   0, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_6 = Color.rgb(255, 191,   0, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_7 = Color.rgb(255, 128,   0, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_8 = Color.rgb(255,  64,   0, 0.5);
    private static final Color                     DEFAULT_SECTION_FILL_9 = Color.rgb(255,   0,   0, 0.5);

    // Default area colors
    private static final Color                     DEFAULT_AREA_FILL_0    = Color.rgb(0, 0, 178, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_1    = Color.rgb(0, 128, 255, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_2    = Color.rgb(  0, 255, 255, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_3    = Color.rgb(  0, 255,  64, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_4    = Color.rgb(128, 255,   0, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_5    = Color.rgb(255, 255,   0, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_6    = Color.rgb(255, 191,   0, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_7    = Color.rgb(255, 128,   0, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_8    = Color.rgb(255,  64,   0, 0.5);
    private static final Color                     DEFAULT_AREA_FILL_9    = Color.rgb(255,   0,   0, 0.5);

    private DoubleProperty                         valueOne;
    private DoubleProperty                         valueTwo;
    private DoubleProperty                         minValueOne;
    private DoubleProperty                         minValueTwo;
    private double                                 exactMinValueOne;
    private double                                 exactMinValueTwo;
    private DoubleProperty                         maxValueOne;
    private DoubleProperty                         maxValueTwo;
    private double                                 exactMaxValueOne;
    private double                                 exactMaxValueTwo;
    private int                                    _decimalsOne;
    private IntegerProperty                        decimalsOne;
    private int                                    _decimalsTwo;
    private IntegerProperty                        decimalsTwo;
    private String                                 _titleOne;
    private StringProperty                         titleOne;
    private String                                 _titleTwo;
    private StringProperty                         titleTwo;
    private String                                 _unitOne;
    private StringProperty                         unitOne;
    private String                                 _unitTwo;
    private StringProperty                         unitTwo;
    private boolean                                _animated;
    private BooleanProperty                        animated;
    private double                                 animationDuration;
    private double                                 startAngleOne;
    private double                                 startAngleTwo;
    private double                                 angleRangeOne;
    private double                                 angleRangeTwo;
    private boolean                                _autoScaleOne;
    private BooleanProperty                        autoScaleOne;
    private boolean                                _autoScaleTwo;
    private BooleanProperty                        autoScaleTwo;
    private ObjectProperty<Color>                  needleColorOne;
    private ObjectProperty<Color>                  needleColorTwo;
    private BooleanProperty                        ledOnOne;
    private BooleanProperty                        ledOnTwo;
    private BooleanProperty                        blinkingOne;
    private BooleanProperty                        blinkingTwo;
    private ObjectProperty<Color>                  ledColorOne;
    private ObjectProperty<Color>                  ledColorTwo;
    private BooleanProperty                        ledVisibleOne;
    private BooleanProperty                        ledVisibleTwo;
    private DoubleRadialGauge.TickLabelOrientation _tickLabelOrientationOne;
    private ObjectProperty<TickLabelOrientation>   tickLabelOrientationOne;
    private DoubleRadialGauge.TickLabelOrientation _tickLabelOrientationTwo;
    private ObjectProperty<TickLabelOrientation>   tickLabelOrientationTwo;
    private DoubleRadialGauge.NumberFormat         _numberFormatOne;
    private ObjectProperty<NumberFormat>           numberFormatOne;
    private DoubleRadialGauge.NumberFormat         _numberFormatTwo;
    private ObjectProperty<NumberFormat>           numberFormatTwo;
    private double                                 _majorTickSpaceOne;
    private DoubleProperty                         majorTickSpaceOne;
    private double                                 _majorTickSpaceTwo;
    private DoubleProperty                         majorTickSpaceTwo;
    private double                                 _minorTickSpaceOne;
    private DoubleProperty                         minorTickSpaceOne;
    private double                                 _minorTickSpaceTwo;
    private DoubleProperty                         minorTickSpaceTwo;
    private boolean                                _dropShadowEnabled;
    private BooleanProperty                        dropShadowEnabled;
    private BooleanProperty                        valueVisibleOne;
    private BooleanProperty                        valueVisibleTwo;
    private ObservableList<Section>                sectionsOne;
    private ObservableList<Section>                sectionsTwo;
    private ObservableList<Section>                areasOne;
    private ObservableList<Section>                areasTwo;
    private BooleanProperty                        sectionsVisibleOne;
    private BooleanProperty                        sectionsVisibleTwo;
    private BooleanProperty                        areasVisibleOne;
    private BooleanProperty                        areasVisibleTwo;
    private ObjectProperty<Color>                  sectionFill0One;
    private ObjectProperty<Color>                  sectionFill1One;
    private ObjectProperty<Color>                  sectionFill2One;
    private ObjectProperty<Color>                  sectionFill3One;
    private ObjectProperty<Color>                  sectionFill4One;
    private ObjectProperty<Color>                  sectionFill0Two;
    private ObjectProperty<Color>                  sectionFill1Two;
    private ObjectProperty<Color>                  sectionFill2Two;
    private ObjectProperty<Color>                  sectionFill3Two;
    private ObjectProperty<Color>                  sectionFill4Two;
    private ObjectProperty<Color>                  areaFill0One;
    private ObjectProperty<Color>                  areaFill1One;
    private ObjectProperty<Color>                  areaFill2One;
    private ObjectProperty<Color>                  areaFill3One;
    private ObjectProperty<Color>                  areaFill4One;
    private ObjectProperty<Color>                  areaFill0Two;
    private ObjectProperty<Color>                  areaFill1Two;
    private ObjectProperty<Color>                  areaFill2Two;
    private ObjectProperty<Color>                  areaFill3Two;
    private ObjectProperty<Color>                  areaFill4Two;

    // CSS styleable properties
    private ObjectProperty<Paint>                 tickMarkFillOne;
    private ObjectProperty<Paint>                 tickLabelFillOne;
    private ObjectProperty<Paint>                 tickMarkFillTwo;
    private ObjectProperty<Paint>                 tickLabelFillTwo;

    private long                                  lastTimerCallOne;
    private AnimationTimer                        timerOne;
    private long                                  lastTimerCallTwo;
    private AnimationTimer                        timerTwo;


    // ******************** Constructors **************************************
    public DoubleRadialGauge() {
        getStyleClass().add("double-radial-gauge");
        minValueOne              = new SimpleDoubleProperty(this, "minValueOne", 0);
        maxValueOne              = new SimpleDoubleProperty(this, "maxValueOne", 100);
        minValueTwo              = new SimpleDoubleProperty(this, "minValueTwo", 0);
        maxValueTwo              = new SimpleDoubleProperty(this, "maxValueTwo", 100);
        valueOne                 = new DoublePropertyBase(minValueOne.get()) {
            @Override public void set(final double VALUE) {
                super.set(clamp(getMinValueOne(), getMaxValueOne(), VALUE));
            }
            @Override public Object getBean() { return this; }
            @Override public String getName() { return "valueOne"; }
        };
        valueTwo                 = new DoublePropertyBase(minValueTwo.get()) {
            @Override public void set(final double VALUE) {
                super.set(clamp(getMinValueTwo(), getMaxValueTwo(), VALUE));
            }
            @Override public Object getBean() { return this; }
            @Override public String getName() { return "valueTwo"; }
        };
        valueVisibleOne          = new SimpleBooleanProperty(this, "valueVisibleOne", true);
        valueVisibleTwo          = new SimpleBooleanProperty(this, "valueVisibleTwo", true);
        _decimalsOne             = 1;
        _decimalsTwo             = 1;
        _titleOne                = "";
        _titleTwo                = "";
        _unitOne                 = "";
        _unitTwo                 = "";
        _animated                = true;
        startAngleOne            = 330;
        startAngleTwo            = 30;
        angleRangeOne            = 120;
        angleRangeTwo            = 120;
        _autoScaleOne            = false;
        _autoScaleTwo            = false;
        _tickLabelOrientationOne = TickLabelOrientation.HORIZONTAL;
        _tickLabelOrientationTwo = TickLabelOrientation.HORIZONTAL;
        _numberFormatOne         = NumberFormat.STANDARD;
        _numberFormatTwo         = NumberFormat.STANDARD;
        _majorTickSpaceOne       = 10;
        _minorTickSpaceOne       = 1;
        _majorTickSpaceTwo       = 10;
        _minorTickSpaceTwo       = 1;
        animationDuration        = 800;
        _dropShadowEnabled       = true;
        sectionsOne              = FXCollections.observableArrayList();
        sectionsTwo              = FXCollections.observableArrayList();
        areasOne                 = FXCollections.observableArrayList();
        areasTwo                 = FXCollections.observableArrayList();
        lastTimerCallOne         = System.nanoTime();
        timerOne                 = new AnimationTimer() {
            @Override public void handle(final long NOW) {
                if (NOW > lastTimerCallOne + LED_BLINK_INTERVAL) {
                    setLedOnOne(blinkingOne.get() ? !getLedOnOne() : false);
                    lastTimerCallOne = NOW;
                }
            }
        };
        lastTimerCallTwo         = System.nanoTime();
        timerTwo                 = new AnimationTimer() {
            @Override public void handle(final long NOW) {
                if (NOW > lastTimerCallTwo + LED_BLINK_INTERVAL) {
                    setLedOnTwo(blinkingTwo.get() ? !getLedOnTwo() : false);
                    lastTimerCallTwo = NOW;
                }
            }
        };
        registerListeners();
    }

    private void registerListeners() {
        sectionsOne.addListener((ListChangeListener<Section>) c -> IntStream.range(0, sectionsOne.size()).parallel().forEachOrdered(
            i -> {
                Section section = sectionsOne.get(i);
                switch(i) {
                    case 0: setSectionFill0One(section.getColor()); break;
                    case 1: setSectionFill1One(section.getColor()); break;
                    case 2: setSectionFill2One(section.getColor()); break;
                    case 3: setSectionFill3One(section.getColor()); break;
                    case 4: setSectionFill4One(section.getColor()); break;
               }
            }));
        areasOne.addListener((ListChangeListener<Section>) c -> IntStream.range(0, areasOne.size()).parallel().forEachOrdered(
            i -> {
                Section area = areasOne.get(i);
                switch(i) {
                    case 0: setAreaFill0One(area.getColor()); break;
                    case 1: setAreaFill1One(area.getColor()); break;
                    case 2: setAreaFill2One(area.getColor()); break;
                    case 3: setAreaFill3One(area.getColor()); break;
                    case 4: setAreaFill4One(area.getColor()); break;
                }
            }));
        sectionsTwo.addListener((ListChangeListener<Section>) c -> IntStream.range(0, sectionsTwo.size()).parallel().forEachOrdered(
            i -> {
                Section section = sectionsTwo.get(i);
                switch(i) {
                    case 0: setSectionFill0Two(section.getColor()); break;
                    case 1: setSectionFill1Two(section.getColor()); break;
                    case 2: setSectionFill2Two(section.getColor()); break;
                    case 3: setSectionFill3Two(section.getColor()); break;
                    case 4: setSectionFill4Two(section.getColor()); break;
                }
            }));
        areasTwo.addListener((ListChangeListener<Section>) c -> IntStream.range(0, areasTwo.size()).parallel().forEachOrdered(
            i -> {
                Section area = areasTwo.get(i);
                switch(i) {
                    case 0: setAreaFill0Two(area.getColor()); break;
                    case 1: setAreaFill1Two(area.getColor()); break;
                    case 2: setAreaFill2Two(area.getColor()); break;
                    case 3: setAreaFill3Two(area.getColor()); break;
                    case 4: setAreaFill4Two(area.getColor()); break;
                }
            }));
    }


    // ******************** Methods Gauge One *********************************
    public final double getValueOne() { return valueOne.get(); }
    public final void setValueOne(final double VALUE) { valueOne.set(VALUE); }
    public final DoubleProperty valueOneProperty() { return valueOne; }

    public final double getMinValueOne() { return minValueOne.get(); }
    public final void setMinValueOne(final double MIN_VALUE) { minValueOne.set(MIN_VALUE); }
    public final DoubleProperty minValueOneProperty() { return minValueOne; }

    public final double getMaxValueOne() { return maxValueOne.get(); }
    public final void setMaxValueOne(final double MAX_VALUE) { maxValueOne.set(MAX_VALUE); }
    public final DoubleProperty maxValueOneProperty() { return maxValueOne; }

    public final int getDecimalsOne() { return null == decimalsOne ? _decimalsOne : decimalsOne.get(); }
    public final void setDecimalsOne(final int DECIMALS) {
        if (null == decimalsOne) {
            _decimalsOne = clamp(0, 3, DECIMALS);
        } else {
            decimalsOne.set(clamp(0, 3, DECIMALS));
        }
    }
    public final IntegerProperty decimalsOneProperty() {
        if (null == decimalsOne) {
            decimalsOne = new SimpleIntegerProperty(this, "decimalsOne", _decimalsOne);
        }
        return decimalsOne;
    }

    public final String getTitleOne() { return null == titleOne ? _titleOne : titleOne.get(); }
    public final void setTitleOne(final String TITLE) {
        if (null == titleOne) {
            _titleOne = TITLE;
        } else {
            titleOne.set(TITLE);
        }
    }
    public final StringProperty titleOneProperty() {
        if (null == titleOne) {
            titleOne = new SimpleStringProperty(this, "titleOne", _titleOne);
        }
        return titleOne;
    }

    public final String getUnitOne() { return null == unitOne ? _unitOne : unitOne.get(); }
    public final void setUnitOne(final String UNIT) {
        if (null == unitOne) {
            _unitOne = UNIT;
        } else {
            unitOne.set(UNIT);
        }
    }
    public final StringProperty unitOneProperty() {
        if (null == unitOne) {
            unitOne = new SimpleStringProperty(this, "unitOne", _unitOne);
        }
        return unitOne;
    }
    
    public double getStartAngleOne() { return startAngleOne; }
    
    public final double getAngleRangeOne() { return angleRangeOne; }

    public final boolean getAutoScaleOne() {
        return null == autoScaleOne ? _autoScaleOne : autoScaleOne.get();
    }
    public final void setAutoScaleOne(final boolean AUTO_SCALE) {
        if (null == autoScaleOne) {
            _autoScaleOne = AUTO_SCALE;
        } else {
            autoScaleOne.set(AUTO_SCALE);
        }
    }
    public final BooleanProperty autoScaleOneProperty() {
        if (null == autoScaleOne) {
            autoScaleOne = new BooleanPropertyBase(_autoScaleOne) {
                @Override public void set(final boolean AUTO_SCALE) {
                    if (get()) {
                        exactMinValueOne = getMinValueOne();
                        exactMaxValueOne = getMaxValueOne();
                    } else {
                        setMinValueOne(exactMinValueOne);
                        setMaxValueOne(exactMaxValueOne);
                    }
                    super.set(AUTO_SCALE);
                }
                @Override public Object getBean() { return this; }
                @Override public String getName() { return "autoScaleOne"; }
            };
        }
        return autoScaleOne;
    }

    public final Color getNeedleColorOne() { return needleColorOneProperty().get(); }
    public final void setNeedleColorOne(final Color NEEDLE_COLOR) { needleColorOneProperty().set(NEEDLE_COLOR); }
    public final ObjectProperty<Color> needleColorOneProperty() {
        if (null == needleColorOne) {
            needleColorOne = new SimpleObjectProperty<>(this, "needleColorOne", Color.RED);
        }
        return needleColorOne;
    }

    public final boolean getLedOnOne() { return null == ledOnOne ? false : ledOnOne.get(); }
    public final void setLedOnOne(final boolean LED_ON) { ledOnOneProperty().set(LED_ON); }
    public final BooleanProperty ledOnOneProperty() {
        if (null == ledOnOne) {
            ledOnOne = new BooleanPropertyBase(false) {
                @Override protected void invalidated() { pseudoClassStateChanged(LED_ON_ONE_PSEUDO_CLASS, get()); }
                @Override public Object getBean() { return this; }
                @Override public String getName() { return "oneOn"; }
            };
        }
        return ledOnOne;
    }

    public final boolean getBlinkingOne() { return null == blinkingOne ? false : blinkingOne.get(); }
    public final void setBlinkingOne(final boolean BLINKING) { blinkingOneProperty().set(BLINKING); }
    public final BooleanProperty blinkingOneProperty() {
        if (null == blinkingOne) {
            blinkingOne = new BooleanPropertyBase() {
                @Override public void set(final boolean BLINKING) {
                    super.set(BLINKING);
                    if (BLINKING) {
                        timerOne.start();
                    } else {
                        timerOne.stop();
                        setLedOnOne(false);
                    }
                }
                @Override public Object getBean() {
                    return DoubleRadialGauge.this;
                }
                @Override public String getName() {
                    return "blinkingOne";
                }
            };
        }
        return blinkingOne;
    }

    public final Color getLedColorOne() { return ledColorOneProperty().get(); }
    public final void setLedColorOne(final Color LED_COLOR) { ledColorOneProperty().set(LED_COLOR); }
    public final ObjectProperty<Color> ledColorOneProperty() {
        if (null == ledColorOne) {
            ledColorOne = new SimpleObjectProperty<>(this, "ledColorOne", Color.RED);
        }
        return ledColorOne;
    }

    public final boolean getLedVisibleOne() { return null == ledVisibleOne ? false : ledVisibleOne.get(); }
    public final void setLedVisibleOne(final boolean LED_VISIBLE) { ledVisibleOneProperty().set(LED_VISIBLE); }
    public final BooleanProperty ledVisibleOneProperty() {
        if (null == ledVisibleOne) {
            ledVisibleOne = new SimpleBooleanProperty(this, "ledVisibleOne", false);
        }
        return ledVisibleOne;
    }

    public final boolean isValueVisibleOne() { return valueVisibleOne.get(); }
    public final void setValueVisibleOne(final boolean VALUE_VISIBLE) { valueVisibleOne.set(VALUE_VISIBLE); }
    public final BooleanProperty valueVisibleOneProperty() { return valueVisibleOne; }

    public final DoubleRadialGauge.TickLabelOrientation getTickLabelOrientationOne() {
        return null == tickLabelOrientationOne ? _tickLabelOrientationOne : tickLabelOrientationOne.get();
    }
    public final void setTickLabelOrientationOne(final DoubleRadialGauge.TickLabelOrientation TICK_LABEL_ORIENTATION) {
        if (null == tickLabelOrientationOne) {
            _tickLabelOrientationOne = TICK_LABEL_ORIENTATION;
        } else {
            tickLabelOrientationOne.set(TICK_LABEL_ORIENTATION);
        }
    }
    public final ObjectProperty<TickLabelOrientation> tickLabelOrientationOneProperty() {
        if (null == tickLabelOrientationOne) {
            tickLabelOrientationOne = new SimpleObjectProperty<>(this, "tickLabelOrientationOne", _tickLabelOrientationOne);
        }
        return tickLabelOrientationOne;
    }

    public final DoubleRadialGauge.NumberFormat getNumberFormatOne() {
        return null == numberFormatOne ? _numberFormatOne : numberFormatOne.get();
    }
    public final void setNumberFormatOne(final DoubleRadialGauge.NumberFormat NUMBER_FORMAT) {
        if (null == numberFormatOne) {
            _numberFormatOne = NUMBER_FORMAT;
        } else {
            numberFormatOne.set(NUMBER_FORMAT);
        }
    }
    public final ObjectProperty<NumberFormat> numberFormatOneProperty() {
        if (null == numberFormatOne) {
            numberFormatOne = new SimpleObjectProperty<>(this, "numberFormatOne", _numberFormatOne);
        }
        return numberFormatOne;
    }

    public final double getMajorTickSpaceOne() {
        return null == majorTickSpaceOne ? _majorTickSpaceOne : majorTickSpaceOne.get();
    }
    public final void setMajorTickSpaceOne(final double MAJOR_TICK_SPACE) {
        if (null == majorTickSpaceOne) {
            _majorTickSpaceOne = MAJOR_TICK_SPACE;
        } else {
            majorTickSpaceOne.set(MAJOR_TICK_SPACE);
        }
    }
    public final DoubleProperty majorTickSpaceOneProperty() {
        if (null == majorTickSpaceOne) {
            majorTickSpaceOne = new SimpleDoubleProperty(this, "majorTickSpaceOne", _majorTickSpaceOne);
        }
        return majorTickSpaceOne;
    }

    public final double getMinorTickSpaceOne() {
        return null == minorTickSpaceOne ? _minorTickSpaceOne : minorTickSpaceOne.get();
    }
    public final void setMinorTickSpaceOne(final double MINOR_TICK_SPACE) {
        if (null == minorTickSpaceOne) {
            _minorTickSpaceOne = MINOR_TICK_SPACE;
        } else {
            minorTickSpaceOne.set(MINOR_TICK_SPACE);
        }
    }
    public final DoubleProperty minorTickSpaceOneProperty() {
        if (null == minorTickSpaceOne) {
            minorTickSpaceOne = new SimpleDoubleProperty(this, "minorTickSpaceOne", _minorTickSpaceOne);
        }
        return minorTickSpaceOne;
    }

    public final ObservableList<Section> getSectionsOne() { return sectionsOne; }
    public final void setSectionsOne(final List<Section> SECTIONS) { sectionsOne.setAll(SECTIONS); }
    public final void setSectionsOne(final Section... SECTIONS) { setSectionsOne(Arrays.asList(SECTIONS)); }
    public final void addSectionOne(final Section SECTION) { if (!sectionsOne.contains(SECTION)) sectionsOne.add(SECTION); }
    public final void removeSectionOne(final Section SECTION) { if (sectionsOne.contains(SECTION)) sectionsOne.remove(SECTION); }

    public final ObservableList<Section> getAreasOne() { return areasOne; }
    public final void setAreasOne(final List<Section> AREAS) { areasOne.setAll(AREAS); }
    public final void setAreasOne(final Section... AREAS) { setAreasOne(Arrays.asList(AREAS)); }
    public final void addAreaOne(final Section AREA) { if (!areasOne.contains(AREA)) areasOne.add(AREA); }
    public final void removeAreaOne(final Section AREA) { if (areasOne.contains(AREA)) areasOne.remove(AREA); }

    public final boolean getSectionsVisibleOne() { return null == sectionsVisibleOne ? true : sectionsVisibleOne.get(); }
    public final void setSectionsVisibleOne(final boolean SECTIONS_VISIBLE) { sectionsVisibleOneProperty().set(SECTIONS_VISIBLE); }
    public final BooleanProperty sectionsVisibleOneProperty() {
        if (null == sectionsVisibleOne) {
            sectionsVisibleOne = new SimpleBooleanProperty(this, "sectionsVisibleOne", true);
        }
        return sectionsVisibleOne;
    }

    public final boolean getAreasVisibleOne() { return null == areasVisibleOne ? true : areasVisibleOne.get(); }
    public final void setAreasVisibleOne(final boolean AREAS_VISIBLE) { areasVisibleOneProperty().set(AREAS_VISIBLE); }
    public final BooleanProperty areasVisibleOneProperty() {
        if (null == areasVisibleOne) {
            areasVisibleOne = new SimpleBooleanProperty(this, "areasVisibleOne", true);
        }
        return areasVisibleOne;
    }

    public final Color getSectionFill0One() {
        return null == sectionFill0One ? DEFAULT_SECTION_FILL_0 : sectionFill0One.get();
    }
    public final void setSectionFill0One(Color value) {
        sectionFill0OneProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill0OneProperty() {
        if (null == sectionFill0One) {
            sectionFill0One = new SimpleObjectProperty<>(this, "sectionFill0One", DEFAULT_SECTION_FILL_0);
        }
        return sectionFill0One;
    }

    public final Color getSectionFill1One() {
        return null == sectionFill1One ? DEFAULT_SECTION_FILL_1 : sectionFill1One.get();
    }
    public final void setSectionFill1One(Color value) {
        sectionFill1OneProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill1OneProperty() {
        if (null == sectionFill1One) {
            sectionFill1One = new SimpleObjectProperty<>(this, "sectionFill1One", DEFAULT_SECTION_FILL_1);
        }
        return sectionFill1One;
    }

    public final Color getSectionFill2One() {
        return null == sectionFill2One ? DEFAULT_SECTION_FILL_2 : sectionFill2One.get();
    }
    public final void setSectionFill2One(Color value) {
        sectionFill2OneProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill2OneProperty() {
        if (null == sectionFill2One) {
            sectionFill2One = new SimpleObjectProperty<>(this, "sectionFill2One", DEFAULT_SECTION_FILL_2);
        }
        return sectionFill2One;
    }

    public final Color getSectionFill3One() {
        return null == sectionFill3One ? DEFAULT_SECTION_FILL_3 : sectionFill3One.get();
    }
    public final void setSectionFill3One(Color value) {
        sectionFill3OneProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill3OneProperty() {
        if (null == sectionFill3One) {
            sectionFill3One = new SimpleObjectProperty<>(this, "sectionFill3One", DEFAULT_SECTION_FILL_3);
        }
        return sectionFill3One;
    }

    public final Color getSectionFill4One() {
        return null == sectionFill4One ? DEFAULT_SECTION_FILL_4 : sectionFill4One.get();
    }
    public final void setSectionFill4One(Color value) {
        sectionFill4OneProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill4OneProperty() {
        if (null == sectionFill4One) {
            sectionFill4One = new SimpleObjectProperty<>(this, "sectionFill4One", DEFAULT_SECTION_FILL_4);
        }
        return sectionFill4One;
    }

    public final Color getAreaFill0One() {
        return null == areaFill0One ? DEFAULT_AREA_FILL_0 : areaFill0One.get();
    }
    public final void setAreaFill0One(Color value) {
        areaFill0OneProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill0OneProperty() {
        if (null == areaFill0One) {
            areaFill0One = new SimpleObjectProperty<>(this, "areaFill0One", DEFAULT_SECTION_FILL_0);
        }
        return areaFill0One;
    }

    public final Color getAreaFill1One() {
        return null == areaFill1One ? DEFAULT_AREA_FILL_1 : areaFill1One.get();
    }
    public final void setAreaFill1One(Color value) {
        areaFill1OneProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill1OneProperty() {
        if (null == areaFill1One) {
            areaFill1One = new SimpleObjectProperty<>(this, "areaFill1One", DEFAULT_SECTION_FILL_1);
        }
        return areaFill1One;
    }

    public final Color getAreaFill2One() {
        return null == areaFill2One ? DEFAULT_AREA_FILL_2 : areaFill2One.get();
    }
    public final void setAreaFill2One(Color value) {
        areaFill2OneProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill2OneProperty() {
        if (null == areaFill2One) {
            areaFill2One = new SimpleObjectProperty<>(this, "areaFill2One", DEFAULT_SECTION_FILL_2);
        }
        return areaFill2One;
    }

    public final Color getAreaFill3One() {
        return null == areaFill3One ? DEFAULT_AREA_FILL_3 : areaFill3One.get();
    }
    public final void setAreaFill3One(Color value) {
        areaFill3OneProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill3OneProperty() {
        if (null == areaFill3One) {
            areaFill3One = new SimpleObjectProperty<>(this, "areaFill3One", DEFAULT_SECTION_FILL_3);
        }
        return areaFill3One;
    }

    public final Color getAreaFill4One() {
        return null == areaFill4One ? DEFAULT_AREA_FILL_4 : areaFill4One.get();
    }
    public final void setAreaFill4One(Color value) {
        areaFill4OneProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill4OneProperty() {
        if (null == areaFill4One) {
            areaFill4One = new SimpleObjectProperty<>(this, "areaFill4One", DEFAULT_SECTION_FILL_4);
        }
        return areaFill4One;
    }



    // ******************** Methods Gauge Two *********************************
    public final double getValueTwo() { return valueTwo.get(); }
    public final void setValueTwo(final double VALUE) { valueTwo.set(VALUE); }
    public final DoubleProperty valueTwoProperty() { return valueTwo; }

    public final double getMinValueTwo() { return minValueTwo.get(); }
    public final void setMinValueTwo(final double MIN_VALUE) { minValueTwo.set(MIN_VALUE); }
    public final DoubleProperty minValueTwoProperty() { return minValueTwo; }

    public final double getMaxValueTwo() { return maxValueTwo.get(); }
    public final void setMaxValueTwo(final double MAX_VALUE) { maxValueTwo.set(MAX_VALUE); }
    public final DoubleProperty maxValueTwoProperty() { return maxValueTwo; }

    public final int getDecimalsTwo() { return null == decimalsTwo ? _decimalsTwo : decimalsTwo.get(); }
    public final void setDecimalsTwo(final int DECIMALS) {
        if (null == decimalsTwo) {
            _decimalsTwo = clamp(0, 3, DECIMALS);
        } else {
            decimalsTwo.set(clamp(0, 3, DECIMALS));
        }
    }
    public final IntegerProperty decimalsTwoProperty() {
        if (null == decimalsTwo) {
            decimalsTwo = new SimpleIntegerProperty(this, "decimalsTwo", _decimalsTwo);
        }
        return decimalsTwo;
    }

    public final String getTitleTwo() { return null == titleTwo ? _titleTwo : titleTwo.get(); }
    public final void setTitleTwo(final String TITLE) {
        if (null == titleTwo) {
            _titleTwo = TITLE;
        } else {
            titleTwo.set(TITLE);
        }
    }
    public final StringProperty titleTwoProperty() {
        if (null == titleTwo) {
            titleTwo = new SimpleStringProperty(this, "titleTwo", _titleTwo);
        }
        return titleTwo;
    }

    public final String getUnitTwo() { return null == unitTwo ? _unitTwo : unitTwo.get(); }
    public final void setUnitTwo(final String UNIT) {
        if (null == unitTwo) {
            _unitTwo = UNIT;
        } else {
            unitTwo.set(UNIT);
        }
    }
    public final StringProperty unitTwoProperty() {
        if (null == unitTwo) {
            unitTwo = new SimpleStringProperty(this, "unitTwo", _unitTwo);
        }
        return unitTwo;
    }

    public double getStartAngleTwo() { return startAngleTwo; }

    public final double getAngleRangeTwo() { return angleRangeTwo; }

    public final boolean getAutoScaleTwo() { return null == autoScaleTwo ? _autoScaleTwo : autoScaleTwo.get(); }
    public final void setAutoScaleTwo(final boolean AUTO_SCALE) {
        if (null == autoScaleTwo) {
            _autoScaleTwo = AUTO_SCALE;
        } else {
            autoScaleTwo.set(AUTO_SCALE);
        }
    }
    public final BooleanProperty autoScaleTwoProperty() {
        if (null == autoScaleTwo) {
            autoScaleTwo = new BooleanPropertyBase(_autoScaleTwo) {
                @Override public void set(final boolean AUTO_SCALE) {
                    if (get()) {
                        exactMinValueTwo = getMinValueTwo();
                        exactMaxValueTwo = getMaxValueTwo();
                    } else {
                        setMinValueTwo(exactMinValueTwo);
                        setMaxValueTwo(exactMaxValueTwo);
                    }
                    super.set(AUTO_SCALE);
                }
                @Override public Object getBean() { return this; }
                @Override public String getName() { return "autoScaleTwo"; }
            };
        }
        return autoScaleTwo;
    }

    public final Color getNeedleColorTwo() { return needleColorTwoProperty().get(); }
    public final void setNeedleColorTwo(final Color NEEDLE_COLOR) { needleColorTwoProperty().set(NEEDLE_COLOR); }
    public final ObjectProperty<Color> needleColorTwoProperty() {
        if (null == needleColorTwo) {
            needleColorTwo = new SimpleObjectProperty<>(this, "needleColorTwo", Color.RED);
        }
        return needleColorTwo;
    }

    public final boolean getLedOnTwo() { return ledOnTwoProperty().get(); }
    public final void setLedOnTwo(final boolean LED_ON) { ledOnTwoProperty().set(LED_ON); }
    public final BooleanProperty ledOnTwoProperty() {
        if (null == ledOnTwo) {
            ledOnTwo = new BooleanPropertyBase(false) {
                @Override protected void invalidated() { pseudoClassStateChanged(LED_ON_TWO_PSEUDO_CLASS, get()); }
                @Override public Object getBean() { return this; }
                @Override public String getName() { return "twoOn"; }
            };
        }
        return ledOnTwo;
    }

    public final boolean getBlinkingTwo() { return null == blinkingTwo ? false : blinkingTwo.get(); }
    public final void setBlinkingTwo(final boolean BLINKING) { blinkingTwoProperty().set(BLINKING); }
    public final BooleanProperty blinkingTwoProperty() {
        if (null == blinkingTwo) {
            blinkingTwo = new BooleanPropertyBase() {
                @Override public void set(final boolean BLINKING) {
                    super.set(BLINKING);
                    if (BLINKING) {
                        timerTwo.start();
                    } else {
                        timerTwo.stop();
                        setLedOnTwo(false);
                    }
                }
                @Override public Object getBean() {
                    return DoubleRadialGauge.this;
                }
                @Override public String getName() {
                    return "blinkingTwo";
                }
            };
        }
        return blinkingTwo;
    }

    public final Color getLedColorTwo() { return null == ledColorTwo ? Color.RED : ledColorTwo.get(); }
    public final void setLedColorTwo(final Color LED_COLOR) { ledColorTwoProperty().set(LED_COLOR); }
    public final ObjectProperty<Color> ledColorTwoProperty() {
        if (null == ledColorTwo) {
            ledColorTwo = new SimpleObjectProperty<>(this, "ledColorTwo", Color.RED);
        }
        return ledColorTwo;
    }

    public final boolean getLedVisibleTwo() { return null == ledVisibleTwo ? false : ledVisibleTwo.get(); }
    public final void setLedVisibleTwo(final boolean LED_VISIBLE) { ledVisibleTwoProperty().set(LED_VISIBLE); }
    public final BooleanProperty ledVisibleTwoProperty() {
        if (null == ledVisibleTwo) {
            ledVisibleTwo = new SimpleBooleanProperty(this, "ledVisibleTwo", false);
        }
        return ledVisibleTwo;
    }

    public final boolean isValueVisibleTwo() { return valueVisibleTwo.get(); }
    public final void setValueVisibleTwo(final boolean VALUE_VISIBLE) { valueVisibleTwo.set(VALUE_VISIBLE); }
    public final BooleanProperty valueVisibleTwoProperty() { return valueVisibleTwo; }

    public final DoubleRadialGauge.TickLabelOrientation getTickLabelOrientationTwo() {
        return null == tickLabelOrientationTwo ? _tickLabelOrientationTwo : tickLabelOrientationTwo.get();
    }
    public final void setTickLabelOrientationTwo(final DoubleRadialGauge.TickLabelOrientation TICK_LABEL_ORIENTATION) {
        if (null == tickLabelOrientationTwo) {
            _tickLabelOrientationTwo = TICK_LABEL_ORIENTATION;
        } else {
            tickLabelOrientationTwo.set(TICK_LABEL_ORIENTATION);
        }
    }
    public final ObjectProperty<TickLabelOrientation> tickLabelOrientationTwoProperty() {
        if (null == tickLabelOrientationTwo) {
            tickLabelOrientationTwo = new SimpleObjectProperty<>(this, "tickLabelOrientationTwo", _tickLabelOrientationTwo);
        }
        return tickLabelOrientationTwo;
    }

    public final DoubleRadialGauge.NumberFormat getNumberFormatTwo() {
        return null == numberFormatTwo ? _numberFormatTwo : numberFormatTwo.get();
    }
    public final void setNumberFormatTwo(final DoubleRadialGauge.NumberFormat NUMBER_FORMAT) {
        if (null == numberFormatTwo) {
            _numberFormatTwo = NUMBER_FORMAT;
        } else {
            numberFormatTwo.set(NUMBER_FORMAT);
        }
    }
    public final ObjectProperty<NumberFormat> numberFormatTwoProperty() {
        if (null == numberFormatTwo) {
            numberFormatTwo = new SimpleObjectProperty<>(this, "numberFormatTwo", _numberFormatTwo);
        }
        return numberFormatTwo;
    }

    public final double getMajorTickSpaceTwo() {
        return null == majorTickSpaceTwo ? _majorTickSpaceTwo : majorTickSpaceTwo.get();
    }
    public final void setMajorTickSpaceTwo(final double MAJOR_TICK_SPACE) {
        if (null == majorTickSpaceTwo) {
            _majorTickSpaceTwo = MAJOR_TICK_SPACE;
        } else {
            majorTickSpaceTwo.set(MAJOR_TICK_SPACE);
        }
    }
    public final DoubleProperty majorTickSpaceTwoProperty() {
        if (null == majorTickSpaceTwo) {
            majorTickSpaceTwo = new SimpleDoubleProperty(this, "majorTickSpaceTwo", _majorTickSpaceTwo);
        }
        return majorTickSpaceTwo;
    }

    public final double getMinorTickSpaceTwo() {
        return null == minorTickSpaceTwo ? _minorTickSpaceTwo : minorTickSpaceTwo.get();
    }
    public final void setMinorTickSpaceTwo(final double MINOR_TICK_SPACE) {
        if (null == minorTickSpaceTwo) {
            _minorTickSpaceTwo = MINOR_TICK_SPACE;
        } else {
            minorTickSpaceTwo.set(MINOR_TICK_SPACE);
        }
    }
    public final DoubleProperty minorTickSpaceTwoProperty() {
        if (null == minorTickSpaceTwo) {
            minorTickSpaceTwo = new SimpleDoubleProperty(this, "minorTickSpaceTwo", _minorTickSpaceTwo);
        }
        return minorTickSpaceTwo;
    }
    
    public final ObservableList<Section> getSectionsTwo() { return sectionsTwo; }
    public final void setSectionsTwo(final List<Section> SECTIONS) { sectionsTwo.setAll(SECTIONS); }
    public final void setSectionsTwo(final Section... SECTIONS) { setSectionsTwo(Arrays.asList(SECTIONS)); }
    public final void addSectionTwo(final Section SECTION) { if (!sectionsTwo.contains(SECTION)) sectionsTwo.add(SECTION); }
    public final void removeSectionTwo(final Section SECTION) { if (sectionsTwo.contains(SECTION)) sectionsTwo.remove(SECTION); }

    public final ObservableList<Section> getAreasTwo() { return areasTwo; }
    public final void setAreasTwo(final List<Section> AREAS) { areasTwo.setAll(AREAS); }
    public final void setAreasTwo(final Section... AREAS) { setAreasTwo(Arrays.asList(AREAS)); }
    public final void addAreaTwo(final Section AREA) { if (!areasTwo.contains(AREA)) areasTwo.add(AREA); }
    public final void removeAreaTwo(final Section AREA) { if (areasTwo.contains(AREA)) areasTwo.remove(AREA); }
    
    public final boolean getSectionsVisibleTwo() { return null == sectionsVisibleTwo ? true : sectionsVisibleTwo.get(); }
    public final void setSectionsVisibleTwo(final boolean SECTIONS_VISIBLE) { sectionsVisibleTwoProperty().set(SECTIONS_VISIBLE); }
    public final BooleanProperty sectionsVisibleTwoProperty() {
        if (null == sectionsVisibleTwo) {
            sectionsVisibleTwo = new SimpleBooleanProperty(this, "sectionsVisibleTwo", true);
        }
        return sectionsVisibleTwo;
    }

    public final boolean getAreasVisibleTwo() { return null == areasVisibleTwo ? true : areasVisibleTwo.get(); }
    public final void setAreasVisibleTwo(final boolean AREAS_VISIBLE) { areasVisibleTwoProperty().set(AREAS_VISIBLE); }
    public final BooleanProperty areasVisibleTwoProperty() {
        if (null == areasVisibleTwo) {
            areasVisibleTwo = new SimpleBooleanProperty(this, "areasVisibleTwo", true);
        }
        return areasVisibleTwo;
    }
    
    public final Color getSectionFill0Two() {
        return null == sectionFill0Two ? DEFAULT_SECTION_FILL_5 : sectionFill0Two.get();
    }
    public final void setSectionFill0Two(Color value) {
        sectionFill0TwoProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill0TwoProperty() {
        if (null == sectionFill0Two) {
            sectionFill0Two = new SimpleObjectProperty<>(this, "sectionFill0Two", DEFAULT_SECTION_FILL_5);
        }
        return sectionFill0Two;
    }

    public final Color getSectionFill1Two() {
        return null == sectionFill1Two ? DEFAULT_SECTION_FILL_6 : sectionFill1Two.get();
    }
    public final void setSectionFill1Two(Color value) {
        sectionFill1TwoProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill1TwoProperty() {
        if (null == sectionFill1Two) {
            sectionFill1Two = new SimpleObjectProperty<>(this, "sectionFill1Two", DEFAULT_SECTION_FILL_6);
        }
        return sectionFill1Two;
    }

    public final Color getSectionFill2Two() {
        return null == sectionFill2Two ? DEFAULT_SECTION_FILL_7 : sectionFill2Two.get();
    }
    public final void setSectionFill2Two(Color value) {
        sectionFill2TwoProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill2TwoProperty() {
        if (null == sectionFill2Two) {
            sectionFill2Two = new SimpleObjectProperty<>(this, "sectionFill2Two", DEFAULT_SECTION_FILL_7);
        }
        return sectionFill2Two;
    }

    public final Color getSectionFill3Two() {
        return null == sectionFill3Two ? DEFAULT_SECTION_FILL_8 : sectionFill3Two.get();
    }
    public final void setSectionFill3Two(Color value) {
        sectionFill3TwoProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill3TwoProperty() {
        if (null == sectionFill3Two) {
            sectionFill3Two = new SimpleObjectProperty<>(this, "sectionFill3Two", DEFAULT_SECTION_FILL_8);
        }
        return sectionFill3Two;
    }

    public final Color getSectionFill4Two() {
        return null == sectionFill4Two ? DEFAULT_SECTION_FILL_9 : sectionFill4Two.get();
    }
    public final void setSectionFill4Two(Color value) {
        sectionFill4TwoProperty().set(value);
    }
    public final ObjectProperty<Color> sectionFill4TwoProperty() {
        if (null == sectionFill4Two) {
            sectionFill4Two = new SimpleObjectProperty<>(this, "sectionFill4Two", DEFAULT_SECTION_FILL_9);
        }
        return sectionFill4Two;
    }
    
    public final Color getAreaFill0Two() {
        return null == areaFill0Two ? DEFAULT_AREA_FILL_5 : areaFill0Two.get();
    }
    public final void setAreaFill0Two(Color value) {
        areaFill0TwoProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill0TwoProperty() {
        if (null == areaFill0Two) {
            areaFill0Two = new SimpleObjectProperty<>(this, "areaFill0Two", DEFAULT_SECTION_FILL_5);
        }
        return areaFill0Two;
    }

    public final Color getAreaFill1Two() {
        return null == areaFill1Two ? DEFAULT_AREA_FILL_6 : areaFill1Two.get();
    }
    public final void setAreaFill1Two(Color value) {
        areaFill1TwoProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill1TwoProperty() {
        if (null == areaFill1Two) {
            areaFill1Two = new SimpleObjectProperty<>(this, "areaFill1Two", DEFAULT_SECTION_FILL_6);
        }
        return areaFill1Two;
    }

    public final Color getAreaFill2Two() {
        return null == areaFill2Two ? DEFAULT_AREA_FILL_7 : areaFill2Two.get();
    }
    public final void setAreaFill2Two(Color value) {
        areaFill2TwoProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill2TwoProperty() {
        if (null == areaFill2Two) {
            areaFill2Two = new SimpleObjectProperty<>(this, "areaFill2Two", DEFAULT_SECTION_FILL_7);
        }
        return areaFill2Two;
    }

    public final Color getAreaFill3Two() {
        return null == areaFill3Two ? DEFAULT_AREA_FILL_8 : areaFill3Two.get();
    }
    public final void setAreaFill3Two(Color value) {
        areaFill3TwoProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill3TwoProperty() {
        if (null == areaFill3Two) {
            areaFill3Two = new SimpleObjectProperty<>(this, "areaFill3Two", DEFAULT_SECTION_FILL_8);
        }
        return areaFill3Two;
    }

    public final Color getAreaFill4Two() {
        return null == areaFill4Two ? DEFAULT_AREA_FILL_9 : areaFill4Two.get();
    }
    public final void setAreaFill4Two(Color value) {
        areaFill4TwoProperty().set(value);
    }
    public final ObjectProperty<Color> areaFill4TwoProperty() {
        if (null == areaFill4Two) {
            areaFill4Two = new SimpleObjectProperty<>(this, "areaFill4Two", DEFAULT_SECTION_FILL_9);
        }
        return areaFill4Two;
    }


    // ******************** Methods *******************************************
    public final boolean isDropShadowEnabled() {
        return null == dropShadowEnabled ? _dropShadowEnabled : dropShadowEnabled.get();
    }
    public final void setDropShadowEnabled(final boolean DROP_SHADOW_ENABLED) {
        if (null == dropShadowEnabled) {
            _dropShadowEnabled = DROP_SHADOW_ENABLED;
        } else {
            dropShadowEnabled.set(DROP_SHADOW_ENABLED);
        }
    }
    public final BooleanProperty dropShadowEnabledProperty() {
        if (null == dropShadowEnabled) {
            dropShadowEnabled = new SimpleBooleanProperty(this, "dropShadowEnabled", _dropShadowEnabled);
        }
        return dropShadowEnabled;
    }
    
    public final double getAnimationDuration() {
        return animationDuration;
    }
    public final void setAnimationDuration(final double ANIMATION_DURATION) {
        animationDuration = clamp(20, 5000, ANIMATION_DURATION);
    }
    
    public final boolean isAnimated() {
        return null == animated ? _animated : animated.get();
    }
    public final void setAnimated(final boolean ANIMATED) {
        if (null == animated) {
            _animated = ANIMATED;
        } else {
            animated.set(ANIMATED);
        }
    }
    public final BooleanProperty animatedProperty() {
        if (null == animated) {
            animated = new SimpleBooleanProperty(this, "animated", _animated);
        }
        return animated;
    }
    
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
    private Duration clamp(final Duration MIN_VALUE, final Duration MAX_VALUE, final Duration VALUE) {
        if (VALUE.lessThan(MIN_VALUE)) return MIN_VALUE;
        if (VALUE.greaterThan(MAX_VALUE)) return MAX_VALUE;
        return VALUE;
    }

    public void calcAutoScale() {
        if (getAutoScaleOne()) {
            double maxNoOfMajorTicks = 10;
            double maxNoOfMinorTicks = 10;
            double niceMinValue;
            double niceMaxValue;
            double niceRange;
            niceRange = (calcNiceNumber((getMaxValueOne() - getMinValueOne()), false));
            majorTickSpaceOne.set(calcNiceNumber(niceRange / (maxNoOfMajorTicks - 1), true));
            niceMinValue = (Math.floor(getMinValueOne() / majorTickSpaceOne.doubleValue()) * majorTickSpaceOne.doubleValue());
            niceMaxValue = (Math.ceil(getMaxValueOne() / majorTickSpaceOne.doubleValue()) * majorTickSpaceOne.doubleValue());
            minorTickSpaceOne.set(calcNiceNumber(majorTickSpaceOne.doubleValue() / (maxNoOfMinorTicks - 1), true));
            setMinValueOne(niceMinValue);
            setMaxValueOne(niceMaxValue);
        }
    }

    /**
     * Returns a "niceScaling" number approximately equal to the range.
     * Rounds the number if ROUND == true.
     * Takes the ceiling if ROUND = false.
     *
     * @param RANGE the valueOne range (maxValueOne - minValueOne)
     * @param ROUND whether to round the result or ceil
     * @return a "niceScaling" number to be used for the valueOne range
     */
    private double calcNiceNumber(final double RANGE, final boolean ROUND) {
        final double EXPONENT = Math.floor(Math.log10(RANGE));   // exponent of range
        final double FRACTION = RANGE / Math.pow(10, EXPONENT);  // fractional part of range
        //final double MOD      = FRACTION % 0.5;                  // needed for large number scale
        double niceFraction;

        // niceScaling
        /*
        if (isLargeNumberScale()) {
            if (MOD != 0) {
                niceFraction = FRACTION - MOD;
                niceFraction += 0.5;
            } else {
                niceFraction = FRACTION;
            }
        } else {
        */

        if (ROUND) {
            if (FRACTION < 1.5) {
                niceFraction = 1;
            } else if (FRACTION < 3) {
                niceFraction = 2;
            } else if (FRACTION < 7) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        } else {
            if (Double.compare(FRACTION, 1) <= 0) {
                niceFraction = 1;
            } else if (Double.compare(FRACTION, 2) <= 0) {
                niceFraction = 2;
            } else if (Double.compare(FRACTION, 5) <= 0) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        }
        //}
        return niceFraction * Math.pow(10, EXPONENT);
    }

    public void validate() {
        if (getValueOne() < getMinValueOne()) setValueOne(getMinValueOne());
        if (getValueOne() > getMaxValueOne()) setValueOne(getMaxValueOne());
    }


    // ******************** CSS Stylable Properties ***************************
    public final Paint getTickMarkFillOne() {
        return null == tickMarkFillOne ? Color.BLACK : tickMarkFillOne.get();
    }
    public final void setTickMarkFillOne(Paint value) {
        tickMarkFillOneProperty().set(value);
    }
    public final ObjectProperty<Paint> tickMarkFillOneProperty() {
        if (null == tickMarkFillOne) {
            tickMarkFillOne = new StyleableObjectProperty<Paint>(Color.BLACK) {

                @Override public CssMetaData getCssMetaData() { return StyleableProperties.TICK_MARK_FILL_ONE; }

                @Override public Object getBean() { return DoubleRadialGauge.this; }

                @Override public String getName() { return "tickMarkFillOne"; }
            };
        }
        return tickMarkFillOne;
    }

    public final Paint getTickLabelFillOne() {
        return null == tickLabelFillOne ? Color.BLACK : tickLabelFillOne.get();
    }
    public final void setTickLabelFillOne(Paint value) {
        tickLabelFillOneProperty().set(value);
    }
    public final ObjectProperty<Paint> tickLabelFillOneProperty() {
        if (null == tickLabelFillOne) {
            tickLabelFillOne = new StyleableObjectProperty<Paint>(Color.BLACK) {
                @Override public CssMetaData getCssMetaData() { return StyleableProperties.TICK_LABEL_FILL_ONE; }
                @Override public Object getBean() { return DoubleRadialGauge.this; }
                @Override public String getName() { return "tickLabelFillOne"; }
            };
        }
        return tickLabelFillOne;
    }


    public final Paint getTickMarkFillTwo() { return null == tickMarkFillTwo ? Color.BLACK : tickMarkFillTwo.get(); }
    public final void setTickMarkFillTwo(Paint value) { tickMarkFillTwoProperty().set(value); }
    public final ObjectProperty<Paint> tickMarkFillTwoProperty() {
        if (null == tickMarkFillTwo) {
            tickMarkFillTwo = new StyleableObjectProperty<Paint>(Color.BLACK) {
                @Override public CssMetaData getCssMetaData() { return StyleableProperties.TICK_MARK_FILL_ONE; }
                @Override public Object getBean() { return DoubleRadialGauge.this; }
                @Override public String getName() { return "tickMarkFillTwo"; }
            };
        }
        return tickMarkFillTwo;
    }

    public final Paint getTickLabelFillTwo() { return null == tickLabelFillTwo ? Color.BLACK : tickLabelFillTwo.get(); }
    public final void setTickLabelFillTwo(Paint value) { tickLabelFillTwoProperty().set(value); }
    public final ObjectProperty<Paint> tickLabelFillTwoProperty() {
        if (null == tickLabelFillTwo) {
            tickLabelFillTwo = new StyleableObjectProperty<Paint>(Color.BLACK) {
                @Override public CssMetaData getCssMetaData() { return StyleableProperties.TICK_LABEL_FILL_ONE; }
                @Override public Object getBean() { return DoubleRadialGauge.this; }
                @Override public String getName() { return "tickLabelFillTwo"; }
            };
        }
        return tickLabelFillTwo;
    }
    

    // ******************** Style related *************************************
    @Override protected Skin createDefaultSkin() { return new DoubleRadialGaugeSkin(this); }

    @Override public String getUserAgentStylesheet() {
        return getClass().getResource("double-radial-gauge.css").toExternalForm();
    }

    private static class StyleableProperties {
        private static final CssMetaData<DoubleRadialGauge, Paint> TICK_MARK_FILL_ONE =
            new CssMetaData<DoubleRadialGauge, Paint>("-tick-mark-fill-one", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), Color.BLACK) {

                @Override public boolean isSettable(DoubleRadialGauge gauge) {
                    return null == gauge.tickMarkFillOne || !gauge.tickMarkFillOne.isBound();
                }

                @Override public StyleableProperty<Paint> getStyleableProperty(DoubleRadialGauge gauge) {
                    return (StyleableProperty) gauge.tickMarkFillOneProperty();
                }
            };

        private static final CssMetaData<DoubleRadialGauge, Paint> TICK_LABEL_FILL_ONE =
            new CssMetaData<DoubleRadialGauge, Paint>("-tick-label-fill-one", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), Color.BLACK) {

                @Override public boolean isSettable(DoubleRadialGauge gauge) {
                    return null == gauge.tickLabelFillOne || !gauge.tickLabelFillOne.isBound();
                }

                @Override public StyleableProperty<Paint> getStyleableProperty(DoubleRadialGauge gauge) {
                    return (StyleableProperty) gauge.tickLabelFillOneProperty();
                }
            };

        private static final CssMetaData<DoubleRadialGauge, Paint> TICK_MARK_FILL_TWO =
            new CssMetaData<DoubleRadialGauge, Paint>("-tick-mark-fill-two", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), Color.BLACK) {
                @Override public boolean isSettable(DoubleRadialGauge gauge) {
                    return null == gauge.tickMarkFillTwo || !gauge.tickMarkFillTwo.isBound();
                }
                @Override public StyleableProperty<Paint> getStyleableProperty(DoubleRadialGauge gauge) {
                    return (StyleableProperty) gauge.tickMarkFillTwoProperty();
                }
            };

        private static final CssMetaData<DoubleRadialGauge, Paint> TICK_LABEL_FILL_TWO =
            new CssMetaData<DoubleRadialGauge, Paint>("-tick-label-fill-two", (StyleConverter<?, Paint>) StyleConverter.getPaintConverter(), Color.BLACK) {

                @Override public boolean isSettable(DoubleRadialGauge gauge) {
                    return null == gauge.tickLabelFillTwo || !gauge.tickLabelFillTwo.isBound();
                }

                @Override public StyleableProperty<Paint> getStyleableProperty(DoubleRadialGauge gauge) {
                    return (StyleableProperty) gauge.tickLabelFillTwoProperty();
                }
            };
        
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            Collections.addAll(styleables,
                               TICK_MARK_FILL_ONE,
                               TICK_LABEL_FILL_ONE,
                               TICK_MARK_FILL_TWO,
                               TICK_LABEL_FILL_TWO);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }
}

