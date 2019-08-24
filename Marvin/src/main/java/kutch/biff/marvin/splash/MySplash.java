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
package kutch.biff.marvin.splash;

import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.version.Version;

/**
 *
 * @author Patrick Kutch
 */
public class MySplash
{
    private static MySplash _Splash;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static int SPLASH_WIDTH = 676;
    static public MySplash getSplash()
    {
        return _Splash;
    }
    private Pane splashLayout;
    private ProgressBar loadProgress;
    private Label progressText;
    private final boolean _Show;
    Stage _Stage;
    AnimationTimer _splashAnimationTimer;
    double startTimerTime;
    private String strAltSplash;
    double TimerInterval = 3000;
    public boolean _appVisible;
    Rectangle2D AppVisualBounds = null;

    private boolean _SplashClosed = true;

    public MySplash(boolean show, String alternateSplashImage)
    {
        _Splash = this;
        _Show = show;
        _appVisible = false;
        
        strAltSplash = alternateSplashImage;
        init();
    }
    public void appVisible()
    {
        _appVisible = true;
    }

    private Image getAlternateSplashImage()
    {
        Image retImage = null;
        if (null != strAltSplash)
        {
            String fname = convertToFileOSSpecific(strAltSplash);
            File file = new File(fname);
            if (file.exists())
            {
                String fn = "file:" + fname ;
                retImage = new Image(fn);
                LOGGER.info("Using alternate splash image: " + strAltSplash);
            }
            else
            {
                LOGGER.warning("Specified alternate splash image: " + strAltSplash + ". Does not exist.  Using default.");
            }
        }
        return retImage;
    }
    
    public void init()
    {
        if (false == _Show)
        {
            return;
        }
        Image splashImg;
        splashImg = getAlternateSplashImage();
        if (null == splashImg)
        {
            URL resource = MySplash.class.getResource("Logo.png");
            splashImg = new Image(resource.toString());
        }

        ImageView splash = new ImageView(splashImg);

        SPLASH_WIDTH = (int) splashImg.getWidth();

        loadProgress = new ProgressBar();
        loadProgress.setPrefWidth(SPLASH_WIDTH);
        
        progressText = new Label(Version.getVersion());
        progressText.setAlignment(Pos.CENTER);
        
        progressText.setStyle("-fx-content-display:center");
        
        splashLayout = new VBox();
        ((VBox)(splashLayout)).setAlignment(Pos.CENTER);

        splashLayout.getChildren().addAll(splash, loadProgress, progressText);
        
        splashLayout.setStyle("-fx-padding: 5; -fx-background-color: darkgray; -fx-border-width:5; -fx-border-color: darkslategray;");
        splashLayout.setEffect(new DropShadow());
    }

    public boolean isSplashClosed()
    {
        return _SplashClosed;
    }
    private void showSplash(Stage initStage)
    {
        Scene splashScene = new Scene(splashLayout);
        initStage.initStyle(StageStyle.UNDECORATED);

        initStage.setScene(splashScene);

        initStage.centerOnScreen();
        initStage.setAlwaysOnTop(true);
        initStage.show();
    }

    public void start(Stage parentStage)
    {
        if (false == _Show)
        {
            return;
        }

        _Stage = new Stage();
        
        _Stage.setTitle("Marvin");
        _Stage.initStyle(StageStyle.UNDECORATED);
        _Stage.toFront();

        showSplash(_Stage);
        
       _Stage.show();
       

        startTimerTime = 0;

        _splashAnimationTimer = new AnimationTimer() // can't update the Widgets outside of GUI thread, so this is a little worker to do so
        {
            @Override
            public void handle(long now)
            {
                if (null == AppVisualBounds && null != Configuration.getConfig())
                {
                    if (Configuration.getConfig().isPrimaryScreenDetermined())
                    {
                       AppVisualBounds = Configuration.getConfig().getPrimaryScreen().getVisualBounds();
                       /*
                       double aboutBoxW = _Stage.getWidth()/2;
                       double configW =Configuration.getConfig().getWidth()/2;
                       double visM = AppVisualBounds.getMinX();
                       double visWid = AppVisualBounds.getMaxX();
                       */
                       double visWidth = Math.abs(AppVisualBounds.getMaxX()) - Math.abs(AppVisualBounds.getMinX());
                       double visHeight = Math.abs(AppVisualBounds.getMaxY()) - Math.abs(AppVisualBounds.getMinY());
                       
                       
                       double X = visWidth /2  - _Stage.getWidth()/2;
                       double Y = visHeight /2 - _Stage.getHeight()/2;
                       
                       _Stage.setX(X);
                       _Stage.setY(Y);
                       //_Stage.setX(AppVisualBounds.getMinX() + Configuration.getConfig().getWidth()/2 - _Stage.getWidth()/2);
                       //_Stage.setY(AppVisualBounds.getMinY() + Configuration.getConfig().getHeight()/2- _Stage.getHeight()/2);
                    }
                    return;
                }
                   
                if (0 == startTimerTime)
                {
                    startTimerTime = System.currentTimeMillis();
                    Thread.currentThread().setName("Splash Screen Animation Timer Thread");
                    return;
                }
                
                if (System.currentTimeMillis() >= startTimerTime + TimerInterval)
                {
                    _splashAnimationTimer.stop();
                    stopSplash();
                    
                    if (true == Configuration.getConfig().getKioskMode())
                    {
                    }
                    return;
                }
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex)
                {
                    
                }
            }
        };
        _SplashClosed = false;
       //startTimerTime = System.currentTimeMillis();
        
        _splashAnimationTimer.start();
    }
    
    public void stopSplash()
    {
        if (false == _Show)
        {
            return;
        }        
        _SplashClosed = true;
        TimerInterval = 0;
       _Stage.close();
    }
}
