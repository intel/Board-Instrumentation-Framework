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

package eu.hansolo.enzo.ledbargraph.skin;

import eu.hansolo.enzo.led.Led;
import eu.hansolo.enzo.led.LedBuilder;
import eu.hansolo.enzo.ledbargraph.LedBargraph;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


/**
 * Created by
 * User: hansolo
 * Date: 16.02.12
 * Time: 11:30
 */
public class LedBargraphSkin extends SkinBase<LedBargraph> implements Skin<LedBargraph> {
    private static final double PREFERRED_SIZE = 16;
    private static final double MINIMUM_SIZE   = 8;
    private static final double MAXIMUM_SIZE   = 1024;
    public static final long    PEAK_TIMEOUT   = 1_500_000_000l;
    private Pane                bargraph;
    private List<Led>           ledList;
    private long                lastTimerCall;
    private DoubleProperty      stepSize;
    private int                 peakLedIndex;
    private AnimationTimer      timer;


    // ******************** Constructors **************************************
    public LedBargraphSkin(final LedBargraph CONTROL) {
        super(CONTROL);
        ledList         = new ArrayList<>(getSkinnable().getNoOfLeds());
        stepSize        = new SimpleDoubleProperty(1.0 / getSkinnable().getNoOfLeds());
        lastTimerCall   = 0l;
        peakLedIndex    = 0;
        timer           = new AnimationTimer() {
            @Override public void handle(final long NOW) {
                if (NOW > lastTimerCall + PEAK_TIMEOUT) {
                    if (Double.compare(peakLedIndex * stepSize.doubleValue(), getSkinnable().getValue()) > 0) {
                        ledList.get(peakLedIndex).setOn(false);
                        peakLedIndex = 0;
                        timer.stop();
                    }                                            
                }
            }
        };

        init();
        initGraphics();
        registerListeners();
        setLedSizes();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(), 0.0) <= 0 ||
            getSkinnable().getWidth() <= 0 || getSkinnable().getHeight() <= 0) {
            getSkinnable().setPrefSize(PREFERRED_SIZE, PREFERRED_SIZE);
        }

        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(), 0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_SIZE, MINIMUM_SIZE);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <=0) {
            getSkinnable().setMaxSize(MAXIMUM_SIZE, MAXIMUM_SIZE);
        }

        for(int i = 0 ; i < getSkinnable().getNoOfLeds() ; i++) {
            Led led = LedBuilder.create()
                                .frameVisible(getSkinnable().isFrameVisible())
                                .prefWidth(getSkinnable().getLedSize())
                                .prefHeight(getSkinnable().getLedSize())
                                .build();
            ledList.add(led);
            if (getSkinnable().getValue() > 0) {
                if (Double.compare(i * stepSize.doubleValue(), getSkinnable().getValue()) <= 0) {
                    ledList.get(i).setOn(true);
                } else {
                    ledList.get(i).setOn(false);
                }
            }
        }

        setLedColors();
        setLedTypes();
    }

    private void initGraphics() {
        final int NO_OF_LEDS = getSkinnable().getNoOfLeds();
        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            bargraph = new VBox();            
            ((VBox) bargraph).setSpacing(0);
            //bargraph.setPadding(new Insets(0, 0, 0, 0));
            IntStream.range(0, NO_OF_LEDS).parallel().forEachOrdered(
                i -> bargraph.getChildren().add(i, ledList.get(NO_OF_LEDS - 1 - i))
            );            
        } else {
            bargraph = new HBox();            
            ((HBox) bargraph).setSpacing(0);
            //bargraph.setPadding(new Insets(0, 0, 0, 0));
            IntStream.range(0, NO_OF_LEDS).parallel().forEachOrdered(
                i -> bargraph.getChildren().add(i, ledList.get(i))
            );            
        }
        getChildren().setAll(bargraph);
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE") );
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE") );
        getSkinnable().valueProperty().addListener(observable -> handleControlPropertyChanged("VALUE") );
        getSkinnable().ledTypeProperty().addListener(observable -> handleControlPropertyChanged("LED_TYPE") );
        getSkinnable().frameVisibleProperty().addListener(observable -> handleControlPropertyChanged("FRAME_VISIBLE") );
        getSkinnable().ledSizeProperty().addListener(observable -> handleControlPropertyChanged("LED_SIZE") );
        getSkinnable().orientationProperty().addListener(observable -> handleControlPropertyChanged("ORIENTATION") );
        getSkinnable().noOfLedsProperty().addListener(observable -> handleControlPropertyChanged("LED_NUMBER") );
        getSkinnable().ledColorsProperty().addListener((ListChangeListener<Color>) change -> handleControlPropertyChanged("LED_COLOR"));        
    }


    // ******************** Methods *******************************************
    @Override protected void layoutChildren(double x, double y, double w, double h) {        
        super.layoutChildren(x, y, w, h);
        getSkinnable().getParent().requestLayout();
    }
    
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("VALUE".equals(PROPERTY)) {
            int currentLedPeakIndex = 0;            
            for (int i = 0 ; i < getSkinnable().getNoOfLeds() ; i++) {
                if (Double.compare(getSkinnable().getValue(), 0) == 0) {
                    ledList.get(i).setOn(false);                    
                } else if (Double.compare(i * stepSize.doubleValue(), getSkinnable().getValue()) < 0) {
                    ledList.get(i).setOn(true);
                    currentLedPeakIndex = i;                    
                } else {
                    ledList.get(i).setOn(false);
                }
                if (getSkinnable().getValue() > 0) ledList.get(peakLedIndex).setOn(true);
            }
            // PeakValue
            if (getSkinnable().isPeakValueVisible()) {
                if (currentLedPeakIndex > peakLedIndex) {
                    peakLedIndex = currentLedPeakIndex;
                    timer.stop();
                    lastTimerCall = System.nanoTime();
                    timer.start();
                }
            }
        } else if ("FRAME_VISIBLE".equals(PROPERTY)) {
            for (Led led : ledList) {
                led.setFrameVisible(getSkinnable().isFrameVisible());
            }
        } else if ("LED_SIZE".equals(PROPERTY)) {
            setLedSizes();
            setLedColors();
        } else if ("ORIENTATION".equals(PROPERTY)) {
            initGraphics();
        } else if ("LED_NUMBER".equals(PROPERTY)) {
            stepSize.set(1.0 / getSkinnable().getNoOfLeds());
        } else if ("LED_COLOR".equals(PROPERTY)) {
            setLedColors();
        } else if ("LED_TYPE".equals(PROPERTY)) {
            setLedTypes();
        } else if ("RESIZE".equals(PROPERTY)) {                                    
            setLedSizes();                        
        }
    }


    // ******************** Private Methods ***********************************
    private final void setLedSizes() {        
        ledList.forEach(led -> led.setPrefSize(getSkinnable().getLedSize(), getSkinnable().getLedSize()));        
        if (Orientation.HORIZONTAL == getSkinnable().getOrientation()) {
            getSkinnable().setMinSize(MINIMUM_SIZE * getSkinnable().getNoOfLeds(), MINIMUM_SIZE);
            getSkinnable().setPrefSize(getSkinnable().getLedSize() * getSkinnable().getNoOfLeds(), getSkinnable().getLedSize());
            bargraph.setMaxSize(getSkinnable().getLedSize() * getSkinnable().getNoOfLeds(), getSkinnable().getLedSize());
        } else {
            getSkinnable().setMinSize(MINIMUM_SIZE, MINIMUM_SIZE * getSkinnable().getNoOfLeds());
            getSkinnable().setPrefSize(getSkinnable().getLedSize(), getSkinnable().getLedSize() * getSkinnable().getNoOfLeds());
            bargraph.setMaxSize(getSkinnable().getLedSize(), getSkinnable().getLedSize() * getSkinnable().getNoOfLeds());
        }
        bargraph.relocate((getSkinnable().getWidth() - bargraph.getLayoutBounds().getWidth()) * 0.5,
                          (getSkinnable().getHeight() - bargraph.getLayoutBounds().getHeight()) * 0.5);
    }

    private final void setLedColors() {
        IntStream.range(0, getSkinnable().getNoOfLeds()).parallel().forEachOrdered(
            i -> ledList.get(i).setLedColor(getSkinnable().getLedColor(i))
        );        
    }

    private final void setLedTypes() {
        ledList.forEach(led -> led.setLedType(getSkinnable().getLedType()));        
    }
}
