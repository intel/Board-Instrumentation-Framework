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
import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;

/**
 *
 * @author Patrick Kutch
 */
public class MySplash
{
    private static MySplash _Splash;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private Pane splashLayout;
    private ProgressBar loadProgress;
    private Label progressText;
    private static int SPLASH_WIDTH = 676;
    private static final int SPLASH_HEIGHT = 227;
    private final boolean _Show;
    Stage _Stage;
    AnimationTimer _splashAnimationTimer;
    double startTimerTime;
    private String strAltSplash;
    double TimerInterval = 5000;
    public boolean _appVisible;
    Rectangle2D AppVisualBounds = null;

    static public MySplash getSplash()
    {
        return _Splash;
    }

    public MySplash(boolean show, String alternateSplashImage)
    {
        _Splash = this;
        _Show = show;
        _appVisible = false;
        
        strAltSplash = alternateSplashImage;
        init();
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

    public void appVisible()
    {
        _appVisible = true;
    }
    public void start(Stage parentStage)
    {
        if (false == _Show)
        {
            return;
        }
        //return;
        //parentStage.setIconified(true);

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
                        _Stage.setX(AppVisualBounds.getMinX() + Configuration.getConfig().getWidth()/2 - _Stage.getWidth()/2);
                        _Stage.setY(AppVisualBounds.getMinY() + Configuration.getConfig().getHeight()/2- _Stage.getHeight()/2);
                    }
                    
                }
                if (0 == startTimerTime)
                {
                    startTimerTime = System.currentTimeMillis();
                    Thread.currentThread().setName("Splash Screen Animation Timer Thread");
                }
                if (_appVisible && System.currentTimeMillis() >= startTimerTime + TimerInterval)
                {
                    stopSplash();
            //        parentStage.setIconified(false);
                    _splashAnimationTimer.stop();

                    if (true == Configuration.getConfig().getKioskMode())
                    {
          //              parentStage.setResizable(false);
                    }
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

        _splashAnimationTimer.start();
    }

    private void showSplash(Stage initStage)
    {
        Scene splashScene = new Scene(splashLayout);
        initStage.initStyle(StageStyle.UNDECORATED);

        initStage.setScene(splashScene);

        initStage.centerOnScreen();
        initStage.show();
        //LOGGER.info("****************************************************************");
    }
    
    public void stopSplash()
    {
        if (false == _Show)
        {
            return;
        }        
        TimerInterval = 0;
       _Stage.close();
    }
//
//    public int CalculateLoadItems(String filename)
//    {
//        Node doc = ConfigurationReader.OpenXMLFileQuietly(filename);
//        WidgetCount = DetermineNumberOfNodes(doc);
//        return WidgetCount;
//    }
//
//    private int DetermineNumberOfNodes(Node topNode)
//    {
//        if (null == topNode)
//        {
//            return 0;
//        }
//        NodeList Children = topNode.getChildNodes();
//        int iCount = 0;
//
//        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++)
//        {
//            Node node = Children.item(iLoop);
//
//            if (node.getNodeName().equalsIgnoreCase("Widget")) // root
//            {
//                if (false == isFlip(node))
//                {
//                    iCount++;
//                }
//                else
//                {
//                    iCount += DetermineNumberOfNodes(node);
//                }
//            }
//            else if (node.getNodeName().equalsIgnoreCase("Grid") || node.getNodeName().equalsIgnoreCase("Tab"))
//            {
//                Element elem = (Element) node;
//                if (elem.hasAttribute("File"))
//                {
//                    iCount += CalculateLoadItems(elem.getAttribute("File"));
//                }
//                iCount += DetermineNumberOfNodes(node);
//            }
//            else if (node.hasChildNodes())
//            {
//                iCount += DetermineNumberOfNodes(node);
//            }
//        }
//        return iCount;
//    }

//    private boolean isFlip(Node topNode)
//    {
//        NodeList Children = topNode.getChildNodes();
//        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++)
//        {
//            Node node = Children.item(iLoop);
//            if (node.getNodeName().equalsIgnoreCase("Front")) // root
//            {
//                return true;
//            }
//        }
//        return false;
//    }
}
