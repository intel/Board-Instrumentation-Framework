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

package eu.hansolo.enzo.led.skin;

import eu.hansolo.enzo.led.Led;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;


/**
 * Created by
 * User: hansolo
 * Date: 17.04.13
 * Time: 09:01
 */
public class LedSkin extends SkinBase<Led> implements Skin<Led> {
    private static final double PREFERRED_SIZE = 16;
    private static final double MINIMUM_SIZE   = 8;
    private static final double MAXIMUM_SIZE   = 1024;
    private double              size;
    private Region              frame;
    private Region              led;
    private Region              highlight;
    private InnerShadow         innerShadow;
    private DropShadow          glow;


    // ******************** Constructors **************************************
    public LedSkin(final Led CONTROL) {
        super(CONTROL);
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
                getSkinnable().setPrefSize(PREFERRED_SIZE, PREFERRED_SIZE);
            }
        }

        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(), 0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_SIZE, MINIMUM_SIZE);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <= 0) {
            getSkinnable().setMaxSize(MAXIMUM_SIZE, MAXIMUM_SIZE);
        }
    }

    private void initGraphics() {
        frame = new Region();
        frame.setOpacity(getSkinnable().isFrameVisible() ? 1 : 0);

        led = new Region();                
        led.setStyle("-led-color: " + colorToCss((Color) getSkinnable().getLedColor()) + ";");

        innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 8, 0d, 0d, 0d);

        glow = new DropShadow(BlurType.TWO_PASS_BOX, (Color) getSkinnable().getLedColor(), 20, 0d, 0d, 0d);
        glow.setInput(innerShadow);

        highlight = new Region();

        // Set the appropriate style classes
        changeStyle();
                         
        getChildren().setAll(frame, led, highlight);
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE") );
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE") );
        getSkinnable().ledColorProperty().addListener(observable -> handleControlPropertyChanged("COLOR") );
        getSkinnable().ledTypeProperty().addListener(observable -> handleControlPropertyChanged("STYLE") );
        getSkinnable().onProperty().addListener(observable -> handleControlPropertyChanged("ON") );
        getSkinnable().frameVisibleProperty().addListener(observable -> handleControlPropertyChanged("FRAME_VISIBLE") );
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("COLOR".equals(PROPERTY)) {
            led.setStyle("-led-color: " + colorToCss((Color) getSkinnable().getLedColor()) + ";");
            changeStyle();
        } else if ("STYLE".equals(PROPERTY)) {
            changeStyle();
        } else if ("ON".equals(PROPERTY)) {
            led.setEffect(getSkinnable().isOn() ? glow : innerShadow);
        } else if ("FRAME_VISIBLE".equals(PROPERTY)) {
            frame.setOpacity(getSkinnable().isFrameVisible() ? 1.0 : 0.0);
            frame.setManaged(getSkinnable().isFrameVisible());
        }
    }
    
    
    // ******************** Private Methods ***********************************
    private static String colorToCss(final Color COLOR) {
        return COLOR.toString().replace("0x", "#");
    }
    
    private void changeStyle() {
        switch(getSkinnable().getLedType()) {
            case HORIZONTAL:
                frame.getStyleClass().setAll("horizontal-frame");
                led.getStyleClass().setAll("horizontal");
                highlight.getStyleClass().setAll("horizontal-highlight");
                break;
            case VERTICAL:
                frame.getStyleClass().setAll("vertical-frame");
                led.getStyleClass().setAll("vertical");
                highlight.getStyleClass().setAll("vertical-highlight");
                break;
            case SQUARE:
                frame.getStyleClass().setAll("square-frame");
                led.getStyleClass().setAll("square");
                highlight.getStyleClass().setAll("square-highlight");
                break;            
            case ROUND:
            default:
                frame.getStyleClass().setAll("round-frame");
                led.getStyleClass().setAll("round");
                highlight.getStyleClass().setAll("round-highlight");
                break;
        }

        glow.setColor((Color) getSkinnable().getLedColor());
        led.setEffect(getSkinnable().isOn() ? glow : innerShadow);

        resize();
    }

    private void resize() {
        size = getSkinnable().getWidth() < getSkinnable().getHeight() ? getSkinnable().getWidth() : getSkinnable().getHeight();        
        if (size > 0) {                                   
            innerShadow.setRadius(0.07 * size);
            glow.setRadius(0.36 * size);

            switch(getSkinnable().getLedType()) {
                case HORIZONTAL:
                    frame.setMaxSize(size, 0.56 * size);
                    led.setMaxSize(0.72 * size, 0.28 * size);                                                            
                    highlight.setMaxSize(0.68 * size, 0.12 * size);
                    highlight.setTranslateY(-0.06 * size);
                    break;
                case VERTICAL:
                    frame.setMaxSize(0.56 * size, size);
                    led.setMaxSize(0.28 * size, 0.72 * size);
                    highlight.setMaxSize(0.24 * size, 0.23 * size);
                    highlight.setTranslateY(-0.22 * size);
                    break;
                case SQUARE:
                    frame.setMaxSize(size, size);
                    led.setMaxSize(0.72 * size, 0.72 * size);
                    highlight.setMaxSize(0.66 * size, 0.66 * size);                     
                    break;                
                case ROUND:
                default:
                    frame.setMaxSize(size, size);
                    led.setMaxSize(0.72 * size, 0.72 * size);
                    highlight.setMaxSize(0.58 * size, 0.58 * size);
                    break;
            }
        }
    }
}
