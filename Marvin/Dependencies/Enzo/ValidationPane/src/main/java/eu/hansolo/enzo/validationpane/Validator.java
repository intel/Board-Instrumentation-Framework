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

package eu.hansolo.enzo.validationpane;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.image.Image;


/**
 * Created by
 * User: hansolo
 * Date: 10.04.13
 * Time: 09:25
 */
public class Validator {
    public static enum State {
        VALID("Valid", new Image(ValidationPane.class.getResource("valid.png").toExternalForm(), IMG_SIZE, IMG_SIZE, true, false)),
        INVALID("Invalid", new Image(ValidationPane.class.getResource("invalid.png").toExternalForm(), IMG_SIZE, IMG_SIZE, true, false)),
        INFO("Information", new Image(ValidationPane.class.getResource("info.png").toExternalForm(), IMG_SIZE, IMG_SIZE, true, false)),
        OPTIONAL("Option", new Image(ValidationPane.class.getResource("optional.png").toExternalForm(), IMG_SIZE, IMG_SIZE, true, false)),
        CLEAR("", null);

        public final String TEXT;
        public final Image  IMAGE;


        private State(final String TEXT, final Image IMAGE) {
            this.TEXT  = TEXT;
            this.IMAGE = IMAGE;
        }
    }
    public static final int IMG_SIZE   = 12;
    public static final int IMG_OFFSET = 6;
    private DoubleProperty  alpha;
    private State           state;
    private String          _infoText;
    private StringProperty  infoText;
    private Pos             validatorPosition;
    private double          iconLocationX;
    private double          iconLocationY;


    // ******************** Constructors **********************************
    public Validator(final State STATE) {
        this(STATE, Pos.TOP_LEFT, "");
    }
    public Validator(final State STATE, final Pos POSITION) {
        this(STATE, POSITION, "");
    }
    public Validator(final State STATE, final Pos POSITION, final String INFO_TEXT) {
        alpha             = new SimpleDoubleProperty(1.0);
        state             = STATE;
        validatorPosition = POSITION;
        _infoText         = INFO_TEXT;
        iconLocationX     = 0;
        iconLocationY     = 0;
    }


    // ******************** Methods ***************************************
    public State getState() {
        return state;
    }
    public void setState(final State STATE) {
        state = STATE;
    }

    /**
     * @return the Pos object that contains the rough location of the icon
     */
    public Pos getValidatorPosition() {
        return validatorPosition;
    }
    public void setValidatorPosition(final Pos VALIDATOR_POSITION) {
        validatorPosition = VALIDATOR_POSITION;
    }

    public double getIconLocationX() {
        return iconLocationX;
    }
    public void setIconLocationX(final double X) {
        iconLocationX = X;
    }

    public double getIconLocationY() {
        return iconLocationY;
    }
    public void setIconLocationY(final double Y) {
        iconLocationY = Y;
    }

    public double[] getIconLocation() {
        return new double[] {iconLocationX, iconLocationY};
    }
    public void setIconLocation(final double X, final double Y) {
        iconLocationX = X;
        iconLocationY = Y;
    }

    public Image getIcon() {
        return state.IMAGE;
    }

    public String getInfoText() {
        return null == infoText ? _infoText : infoText.get();
    }
    public void setInfoText(final String INFO_TEXT) {
        if (null == infoText) {
            _infoText = INFO_TEXT;
        } else {
            infoText.set(INFO_TEXT);
        }
    }
    public StringProperty infoTextProperty() {
        if (null == infoText) {
            infoText = new SimpleStringProperty(this, "infoText", _infoText);
        }
        return infoText;
    }

    public double getAlpha() {
        return alpha.get();
    }
    public void setAlpha(final double ALPHA) {
        alpha.set(ALPHA);
    }
    public DoubleProperty alphaProperty() {
        return alpha;
    }
}
