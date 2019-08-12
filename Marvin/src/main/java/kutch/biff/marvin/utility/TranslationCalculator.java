/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * # 
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * # 
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * # 
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract: 
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class TranslationCalculator
{

    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    protected final static Configuration CONFIG = Configuration.getConfig();

    private final DoubleProperty _Scale;
//    private final Pane _ReferencePane;
    private final Pane _WorkingPane;
    //private final Rectangle clip = new Rectangle();
    private final Pos _Position;
    private final Translate _Translate;

    public TranslationCalculator(Pane objBasePane, Pane objWorkingPane, DoubleProperty objScaleProperty, Pos position)
    {
        _Scale = objScaleProperty;

  //      _ReferencePane = objBasePane;
        _WorkingPane = objWorkingPane;
        _Position = position;
        _Translate = new Translate();
        Scale scaleTransform = new Scale();
        scaleTransform.xProperty().bind(_Scale);
        scaleTransform.yProperty().bind(_Scale);

        objWorkingPane.getTransforms().addAll(scaleTransform, _Translate);
        SetupListeners();
    }

    private void Calculate()
    {
        double tX = CalcTranslationX();
        double tY = CalcTranslationY();
        // Don't think I need this anymor
        _Translate.setX(tX);
        _Translate.setY(tY);
    }

    private void SetupListeners()
    {
        _WorkingPane.widthProperty().addListener(new ChangeListener<Number>() // when things are resized
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number number, Number oldNumber)
            {
                Calculate();
            }
        });
        _WorkingPane.heightProperty().addListener(new ChangeListener<Number>() // when things are resized
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number number, Number oldNumber)
            {
                Calculate();
            }
        });
        /*
        _ReferencePane.layoutBoundsProperty().addListener(new ChangeListener<Bounds>() // when things are resized
        {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldBounds, Bounds bounds)
            {
                clip.setWidth(bounds.getWidth());
                clip.setHeight(bounds.getHeight());
                _ReferencePane.setClip(clip);
                Calculate();
            }
        });
        */
        _Scale.addListener(new ChangeListener<Number>() // when the scale changes
        {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2)
            {
                Calculate();
            }
        });
    }

    private double CalcTranslationX()
    {
        double refWidth, paneWidth;
        double tx, scale;

        scale = _Scale.getValue();

        //refWidth = _ReferencePane.getWidth();
        refWidth = CONFIG.getCanvasWidth();
        paneWidth = _WorkingPane.getWidth();

        if (_Position == Pos.CENTER_RIGHT || _Position == Pos.TOP_RIGHT || _Position == Pos.BOTTOM_RIGHT)
        {
            tx = (refWidth - (paneWidth * scale)) / scale; // right aligned
        }
        else if (_Position == Pos.CENTER_LEFT || _Position == Pos.TOP_LEFT || _Position == Pos.BOTTOM_LEFT)
        {
            tx = 0; // left aligned
        }
        else if (_Position == Pos.CENTER || _Position == Pos.TOP_CENTER || _Position == Pos.BOTTOM_CENTER)
        {
            tx = ((refWidth - (paneWidth * scale)) / scale) / 2; // centered
        }
        else
        {
            tx = 0;
        }
        if (tx < 0)
        {
            tx = 0;
        }

        return tx;
    }

    private double CalcTranslationY()
    {
        double refHeight, paneHeight;
        double ty, scale;

        scale = _Scale.getValue();

        //refHeight = _ReferencePane.getHeight();
        refHeight = CONFIG.getCanvasHeight();
        paneHeight = _WorkingPane.getHeight();
        if (paneHeight == 0 || refHeight == 0)
        {
            return 0;
        }
        if (_Position == Pos.BOTTOM_CENTER || _Position == Pos.BOTTOM_RIGHT || _Position == Pos.BOTTOM_LEFT)
        {
            ty = (refHeight - (paneHeight * scale)) / scale; // Bottom aligned
        }
        else if (_Position == Pos.TOP_CENTER || _Position == Pos.TOP_LEFT || _Position == Pos.TOP_RIGHT)
        {
            ty = 0; // left aligned
        }
        else if (_Position == Pos.CENTER || _Position == Pos.CENTER_LEFT || _Position == Pos.CENTER_RIGHT)
        {
            ty = ((refHeight - (paneHeight * scale)) / scale) / 2; // centered
        }
        else
        {
            ty = 0;
        }
        /*
        if (ty < CONFIG.getTopOffset() * -1)
        {
            ty = CONFIG.getTopOffset() * -1;
        }
        */
        return ty;
    }
}
