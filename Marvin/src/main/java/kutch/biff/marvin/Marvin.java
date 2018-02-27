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
package kutch.biff.marvin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.network.Server;
import kutch.biff.marvin.splash.MySplash;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.Heartbeat;
import kutch.biff.marvin.utility.JVMversion;
import kutch.biff.marvin.version.Version;
import kutch.biff.marvin.widget.BaseWidget;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import kutch.biff.marvin.utility.MarvinLocalData;
import java.util.Set;
import static java.lang.Math.abs;
import kutch.biff.marvin.widget.Widget;

/**
 *
 * @author Patrick
 */
public class Marvin extends Application
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private DataManager _DataMgr;
    private ConfigurationReader _Config;
    private static TabPane _objTabPane = null;
    private static final int noOfNodes = 0;
    private Configuration appConfig = null;
    private Server _receiveServer;
    private AnimationTimer _animationTimer;
    private Heartbeat _Heartbeat;
    private long lastTimerCall;
    private TabPane _TestPane;

    private long TimerInterval = 2500; //nanoseconds 1ms = 1000000 ns
    private long MemoryUsageReportingInterval = 10000; // 10 secs
    private long LastMemoryUsageReportingTime = 0;
    private boolean ReportMemoryUsage = false;
    private String strOldSuffix = "dummy";
    private int SplashWait = 5000;
    private int NoSplashWait = 800;

    private Stage _stage;
    private final TaskManager TASKMAN = TaskManager.getTaskManager();
    private String ConfigFilename = "Application.xml";
    private String LogFileName = "MarvinLog.html";
    private boolean ShowHelp = false;
    private boolean ShowVersion = false;
    private boolean ShowSplash = true;
    private boolean dumpAlias = false;
    private boolean dumpWidgetInfo = false;
    private String altSplash = null;
    private MarvinLocalData objLocalMarvinData = null;
    private MySplash _Splash;
    private boolean _CheckForSizeProblems = true;
    private boolean _SizeCheckWindowShowing = false;

    // returns the base tab pane - used for dynamic tabs in debug mode
    public static TabPane GetBaseTabPane()
    {
        return _objTabPane;
    }

    private void DisableWebCerts()
    {
        LOGGER.info("Disabling Web Certificates.");
        // Completely disable, by trusting everything!
        TrustManager[] myTM = new TrustManager[]
        {
            new X509TrustManager()
            {
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType)
                {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType)
                {
                }

            }
        };

        try
        {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, myTM, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        }
        catch (Exception ex)
        {
            LOGGER.severe("Error Disabling Web Certificates: " + ex.toString());
        }
    }

    private void CheckForLogFileName()
    {
        Parameters params = getParameters();
        List<String> parameters = params.getRaw();

        for (int iIndex = 0; iIndex < parameters.size(); iIndex++)
        {
            String param = parameters.get(iIndex);
            if (param.equalsIgnoreCase("-log"))
            {
                if (iIndex + 1 < parameters.size())
                {
                    LogFileName = parameters.get(++iIndex);
                }
                else
                {
                    System.out.println("-log command line option given, but no filename provided.  Defaulting to MarvinLog.html");
                }
                return;
            }
        }
    }

    private void ParseCommandLineArgs()
    {
        Parameters params = getParameters();
        List<String> parameters = params.getRaw();
        int verboseLevel = 0;
        String AliasFileCompare = "-aliasfile=";

        for (int iIndex = 0; iIndex < parameters.size(); iIndex++)
        {
            String param = parameters.get(iIndex);
            if (param.equalsIgnoreCase("-i"))
            {
                if (iIndex + 1 < parameters.size())
                {
                    ConfigFilename = parameters.get(++iIndex);
                }
                else
                {
                    LOGGER.severe("-i command line option given, but no filename provided.  Defaulting to Application.xml");
                }
            }
            else if (param.equalsIgnoreCase("-log")) // already handled elsewhere, but do it again for fun
            {
                if (iIndex + 1 < parameters.size())
                {
                    LogFileName = parameters.get(++iIndex);
                }
                else
                {
                    LOGGER.severe("-log command line option given, but no filename provided.  Defaulting to MarvinLog.html");
                }
            }
            else if (param.equalsIgnoreCase("-AltSplash")) // already handled elsewhere, but do it again for fun
            {
                if (iIndex + 1 < parameters.size())
                {
                    altSplash = parameters.get(++iIndex);
                }
                else
                {
                    LOGGER.severe("-AltSplash command line option given, but no filename provided.");
                }
            }
            else if (param.equalsIgnoreCase("-v"))
            {
                verboseLevel = 1;
            }
            else if (param.equalsIgnoreCase("-vv"))
            {
                verboseLevel = 2;
            }
            else if (param.equalsIgnoreCase("-vvv"))
            {
                verboseLevel = 3;
            }
            else if (param.equalsIgnoreCase("-vvvv"))
            {
                verboseLevel = 4;
                //ReportMemoryUsage = true; // super verbose mode, show memory usage
            }
            else if (param.equalsIgnoreCase("-dumpalias"))
            {
                dumpAlias = true;
            }
            else if (param.equalsIgnoreCase("-dumpwidgetinfo"))
            {
                dumpWidgetInfo = true;
            }

            else if (param.length() > AliasFileCompare.length() && param.substring(0, AliasFileCompare.length()).equalsIgnoreCase(AliasFileCompare))
            {
                AliasMgr.getAliasMgr().LoadAliasFile(param);
            }
            else if (param.equalsIgnoreCase("-?"))
            {
                ShowHelp = true;
            }
            else if (param.equalsIgnoreCase("-help"))
            {
                ShowHelp = true;
            }
            else if (param.equalsIgnoreCase("-version"))
            {
                ShowVersion = true;
            }
            else if (param.equalsIgnoreCase("-ns")) // don't show splash
            {
                ShowSplash = false;
            }
            else if (param.equalsIgnoreCase("-nosplash")) // don't show splash
            {
                ShowSplash = false;
            }
            else
            {
                LOGGER.severe("Unknown command line parameter: " + param);
            }
        }

        MarvinLogger.setDebugLevel(Level.ALL);
        LOGGER.config("--- BIFF GUI [Marvin]  " + Version.getVersion());
        MarvinLogger.setDebugLevel(Level.SEVERE);

        if (0 == verboseLevel)
        {
            MarvinLogger.setDebugLevel(Level.SEVERE);
            _CheckForSizeProblems = false;
        }

        switch (verboseLevel)
        {
            case 1:
                MarvinLogger.setDebugLevel(Level.WARNING);
                _CheckForSizeProblems = false;
                break;
            case 2:
                MarvinLogger.setDebugLevel(Level.INFO);
                _CheckForSizeProblems = false;
                break;
            case 3:
                MarvinLogger.setDebugLevel(Level.CONFIG);
                break;
            case 4:
                MarvinLogger.setDebugLevel(Level.ALL);
                break;
            default:
                break;
        }
    }

    private void DisplayHelp()
    {
        String help = "-? | -help \t\t: Display this help\n";
        help += "-i application.xml file [default Application.xml\n";
        help += "-log application.log file [default MarvinLog.html\n";
        help += "-aliasfile externalFile aliases you want to define outside of your xml\n";
        help += "-altSplash image file to use for splash screen\n";
        help += "-v | -vv |-vvv |-vvvv \t: - logging level\n";
//        help += "-version \t\t: - show version information\n";
        help += "-dumpalias - dumps top level alias to log\n";
        help += "-dumpWidgetInfo - dumps info on all widgets\n";
        help += "-ns - supresses splash screen\n";
        System.out.println(help);
        JOptionPane.showMessageDialog(null, help, "Command Line Options", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void init()
    {
        CheckForLogFileName();
        try
        {
            MarvinLogger.setup(LogFileName);
            MarvinLogger.setDebugLevel(Level.SEVERE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        ParseCommandLineArgs();
        if (true == ShowHelp)
        {
            return;
        }

        _Splash = new MySplash(ShowSplash, altSplash);
    }

    private long BeginLoadProcess()
    {
        long start = System.currentTimeMillis();
        _DataMgr = new DataManager();
        if (null == _Config)
        {
            _Config = new ConfigurationReader();
        }

        if (null == _Config)
        {
            return 0;
        }

        SimpleDoubleProperty complete = new SimpleDoubleProperty();
        appConfig = _Config.ReadAppConfigFile(ConfigFilename, complete);

        if (null != appConfig)
        {
            if (dumpAlias)
            {
                AliasMgr.getAliasMgr().DumpTop();
            }

            TASKMAN.setDataMgr(_DataMgr); // kludgy I know, I know.  I hang my head in shame
            _receiveServer = new Server(_DataMgr);
        }
        return System.currentTimeMillis() - start;
    }

    private boolean SetupGoodies(TabPane pane)
    {
        boolean RetVal = true;
        long startTime = System.currentTimeMillis();
        if (null == _Config.getTabs())
        {
            return false;
        }

        for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++)
        {
            if (false == _Config.getTabs().get(iIndex).Create(pane, _DataMgr, iIndex))
            {
                RetVal = false;
                break;
            }
        }
        if (true == RetVal)
        {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++)
            {
                if (false == _Config.getTabs().get(iIndex).PerformPostCreateActions(null, false))
                {
                    RetVal = false;
                    break;
                }
            }

        }

        // check if all have been setup with parent pane (a bit of a hack for this added peekabooo feature_
        /*
        for (BaseWidget objWidget : BaseWidget.getWidgetList())
        {
            if (!objWidget.ProperlySetup())
            {
                RetVal = false;
                LOGGER.severe(objWidget.getClass().toString() + " didn't register with SetParent");
            }
        }        
         */
        if (TaskManager.getTaskManager().VerifyTasks())
        {
            LOGGER.info("Verified all referenced tasks are defined.");
        }

        _Config.getConfiguration().setPane(pane);
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Time taken to initialize all widgets: " + Long.toString(elapsed) + "ms.");

        return RetVal;
    }

    private void SetupDebugToolTips()
    {
        if (_Config.getConfiguration().isDebugMode())
        {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++)
            {
                _Config.getTabs().get(iIndex).PerformPostCreateActions(null, true);
            }
        }

        //check if a widget is bigger than it's parent grid - not working yet
        if (_CheckForSizeProblems)
        {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++)
            {
                _Config.getTabs().get(iIndex).CheckSizingBounds(1);
            }
        }
    }

    private void StopWidgets()
    {
        if (null != _Config && null != _Config.getTabs())
        {
            for (Widget tab : _Config.getTabs())
            {
                tab.PrepareForAppShutdown();
            }
        }
    }

    private void DumpAllWidgetsInformation()
    {
        if (_Config.getConfiguration().isDebugMode())
        {
            for (BaseWidget objWidget : BaseWidget.getWidgetList())
            {
                if (dumpWidgetInfo)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Widget Information: ");
                    sb.append(objWidget.toString(false));
                    LOGGER.config(sb.toString());
                }
                objWidget.SetupTaskAction(); // setup shift and ctrl click actions for debug
                if (null != objWidget.getRegionObject())
                {
                    // objWidget.getRegionObject().requestLayout();
                }
            }
            if (dumpWidgetInfo)
            {
                LOGGER.config("External Grid/Tab usage file from " + ConfigFilename + "\n" + kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.GetFileTree());
            }
        }
    }

    private boolean SetAppStyle(ObservableList<String> StyleSheets)
    {
        if (null != _Config.getConfiguration().getCSSFile())
        {
            String osIndepFN = BaseWidget.convertToFileOSSpecific(_Config.getConfiguration().getCSSFile());

            if (null == osIndepFN)
            {
                return true;
            }
            String strCSS = BaseWidget.convertToFileURL(osIndepFN);

            if (null != strCSS)
            {
                try
                {
                    if (false == StyleSheets.add(strCSS))
                    {
                        LOGGER.severe("Problems with application stylesheet: " + _Config.getConfiguration().getCSSFile());
                        return false;
                    }
                }
                catch (Exception ex)
                {
                    LOGGER.severe("Problems with application stylesheet: " + _Config.getConfiguration().getCSSFile());
                    return false;
                }
            }
        }

        return true;
    }

    private void checkSize(Stage stage, Scene scene, GridPane objGridPane)
    {

        //if (1 == 1) return;
//        stage.setMaximized(true);
        stage.centerOnScreen();
        double a = stage.getWidth();
        double BorderWidth = abs((scene.getWidth() - stage.getWidth()) / 2);
        _Config.getConfiguration().setAppBorderWidth(BorderWidth);

        _Config.getConfiguration().setBottomOffset(_TestPane.getHeight());
        double height = _TestPane.getHeight(); // tab + borders
        if (null != _Config.getConfiguration().getMenuBar() && true == _Config.getConfiguration().getShowMenuBar())
        {
            height = _TestPane.getHeight() + _Config.getConfiguration().getMenuBar().getHeight(); //menu + borders + tab
        }
        _Config.getConfiguration().setTopOffset(height);
        objGridPane.getChildren().remove(_TestPane);
    }

    /**
     * Creates a dummy tab pane, so I can measure the height of the tab portion
     * for other calcualations
     *
     * @param basePlane
     */
    private void SetupSizeCheckPane(GridPane basePlane)
    {
        _TestPane = new TabPane();
        Tab T = new Tab();
        T.setText("Test");
        _TestPane.getTabs().add(T);
        _TestPane.setVisible(false);

        basePlane.add(_TestPane, 2, 2);
    }

    private void DoSizeTest(Stage stage)
    {
        _Config = new ConfigurationReader();
        if (null == _Config)
        {
            return;
        }

        Configuration config = _Config.ReadStartupInfo(ConfigFilename);
        if (null == config)
        {
            return;
        }
        _objTabPane = new TabPane();
        _objTabPane.setSide(config.getSide());

        GridPane sceneGrid = new GridPane();

        GridPane.setHalignment(_Config.getConfiguration().getMenuBar(), HPos.LEFT);
        GridPane.setValignment(_Config.getConfiguration().getMenuBar(), VPos.TOP);

        sceneGrid.add(_Config.getConfiguration().getMenuBar(), 0, 0);
        sceneGrid.add(_objTabPane, 0, 1);

        Scene scene = null;
        Rectangle2D visualBounds = _Config.getConfiguration().getPrimaryScreen().getVisualBounds();
        int appWidth = (int) visualBounds.getWidth();
        int appHeight = (int) visualBounds.getHeight();

        if (config.getWidth() > 0)
        {
            appWidth = config.getWidth();
        }
        else
        {
            config.setWidth(appWidth);
        }
        if (config.getHeight() > 0)
        {
            appHeight = config.getHeight();
        }
        else
        {
            config.setHeight(appHeight);
        }

        scene = new Scene(sceneGrid);
        SetAppStyle(scene.getStylesheets());
        stage.setScene(scene);
        stage.setX(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinX());
        stage.setY(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinY());

        stage.setMaximized(true);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>()
                      {
                          @Override
                          public void handle(WindowEvent window)
                          {
                              FinishLoad(stage);
                          }
                      });

        while (false && !_SizeCheckWindowShowing)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Marvin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        FinishLoad(stage);

    }

    @Override
    public void start(Stage stage) throws Exception
    {
        if (true == ShowHelp)
        {
            DisplayHelp();
            Platform.exit();
            return;
        }

        DoSizeTest(stage);
    }

    public void FinishLoad(Stage stage)
    {

        MySplash.getSplash().start(stage);

        long elapsedTime = BeginLoadProcess();
        LOGGER.info("Time taken to load Configuration: " + Long.toString(elapsedTime) + "ms.");

        if (!ShowSplash)
        {
            //ShowSplash = true;
            MySplash.getSplash().stopSplash();
        }

        if (null == this.appConfig)
        {
            Platform.exit();
            return;
        }
        _Config.getConfiguration().setAppStage(stage);

        if (null == _objTabPane)
        {
            _objTabPane = new TabPane();
            _objTabPane.setSide(_Config.getConfiguration().getSide());
        }
        GridPane sceneGrid = new GridPane();

        Scene scene = null;
        Rectangle2D visualBounds = _Config.getConfiguration().getPrimaryScreen().getVisualBounds();
        int appWidth = (int) visualBounds.getWidth();
        int appHeight = (int) visualBounds.getHeight();

        if (appConfig.getWidth() > 0)
        {
            appWidth = appConfig.getWidth();
        }
        else
        {
            appConfig.setWidth(appWidth);
        }
        if (appConfig.getHeight() > 0)
        {
            appHeight = appConfig.getHeight();
        }
        else
        {
            appConfig.setHeight(appHeight);
        }

        sceneGrid.add(_objTabPane, 0, 1);
        //sceneGrid.setStyle("-fx-background-color:red;");
        SetupSizeCheckPane(sceneGrid);
        //sceneGrid.setMaxHeight(340);
        if (null != _Config.getConfiguration().getMenuBar() && true == _Config.getConfiguration().getShowMenuBar())
        {
            //vbox.getChildren().add(_Config.getConfiguration().getMenuBar());
            GridPane.setHalignment(_Config.getConfiguration().getMenuBar(), HPos.LEFT);
            GridPane.setValignment(_Config.getConfiguration().getMenuBar(), VPos.TOP);

            sceneGrid.add(_Config.getConfiguration().getMenuBar(), 0, 0);
        }

        scene = new Scene(sceneGrid);

        _Config.getConfiguration().getCurrentHeightProperty().bind(scene.heightProperty());
        _Config.getConfiguration().getCurrentWidthProperty().bind(scene.widthProperty());
        _objTabPane.prefWidthProperty().bind(scene.widthProperty());
        //_objTabPane.setPrefWidth(800);
        _objTabPane.prefHeightProperty().bind(scene.heightProperty());

        // Wait for gui to render before starting up network connection
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>()
                      {
                          @Override
                          public void handle(WindowEvent window)
                          {
                              Platform.runLater(new Runnable()
                              {
                                  @Override
                                  public void run()
                                  {
                                      BeginServerEtc();
                                      _Splash.appVisible();
                                  }
                              });
                          }
                      });

        SetAppStyle(scene.getStylesheets());

        if (false == SetupGoodies(_objTabPane))
        {
            JOptionPane.showMessageDialog(null, "Error loading Configuation. \nCheck log file.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
            Platform.exit();
            return;
        }

        if (false == _receiveServer.Setup(_Config.getConfiguration().getAddress(), _Config.getConfiguration().getPort()))
        {
            JOptionPane.showMessageDialog(null, "Error setting up Network Configuation. \nCheck log file.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
            Platform.exit();
            return;
        }
        checkSize(stage, scene, sceneGrid); // go resize based upon scaling

        stage.setTitle(_Config.getConfiguration().getAppTitle());
        stage.setScene(scene);
        stage.setHeight(appHeight);
        stage.setWidth(appWidth);

        if (_Config.getConfiguration().getKioskMode())
        {
            stage.initStyle(StageStyle.UNDECORATED);
        }

        if (true == ShowHelp)
        {
            DisplayHelp();
        }

        if (_Config.getConfiguration().getIgnoreWebCerts())
        {
            DisableWebCerts();
        }

        _stage = stage;

        TimerInterval = _Config.getConfiguration().getTimerInterval();
        lastTimerCall = System.currentTimeMillis() + TimerInterval;
        LastMemoryUsageReportingTime = lastTimerCall;

        strOldSuffix = "dummy";

        _animationTimer = new AnimationTimer() // can't update the Widgets outside of GUI thread, so this is a little worker to do so
        {
            boolean Showing = false;

            @Override
            public void handle(long now)
            {
                if (Configuration.getConfig().terminating())
                {
                    return;
                }

                if (!Showing)
                {
                    try
                    {
                        Showing = true;
                        stage.show();
                        Thread.currentThread().setName("Animation Timer Thread");
                        SetupDebugToolTips();
                    }
                    catch (Exception e)
                    {
                        StringWriter strWriter = new StringWriter();
                        PrintWriter pntWriter = new PrintWriter(strWriter);
                        e.printStackTrace(pntWriter);
                        LOGGER.severe(strWriter.toString());
                        JOptionPane.showMessageDialog(null, "Error trying to launch application. \nCheck log file.", "Error", JOptionPane.ERROR_MESSAGE);
                        Platform.exit();
                    }
                    Showing = true;
                    return;
                }

                if (System.currentTimeMillis() > lastTimerCall + TimerInterval)
                {
                    _DataMgr.PerformUpdates();
                    _Config.getConfiguration().DetermineMemorex();
                    if (!strOldSuffix.equals(_Config.getConfiguration().TitleSuffix)) // title could be 'recorded' 'lived'
                    {
                        _stage.setTitle(_Config.getConfiguration().getAppTitle() + _Config.getConfiguration().TitleSuffix);
                        strOldSuffix = _Config.getConfiguration().TitleSuffix;
                    }
                    // for remote marvin admin updates, can't update gui outside of gui thread
                    TaskManager.getTaskManager().PerformDeferredTasks();
                    lastTimerCall = System.currentTimeMillis();
                }

                else if (ReportMemoryUsage && System.currentTimeMillis() > LastMemoryUsageReportingTime + MemoryUsageReportingInterval)
                {
                    LastMemoryUsageReportingTime = System.currentTimeMillis();
                    long freeMem = Runtime.getRuntime().freeMemory();
                    long totalMem = Runtime.getRuntime().maxMemory();
                    long usedMem = totalMem - freeMem;
                    usedMem /= 1024.0;
                    String MBMemStr = NumberFormat.getNumberInstance(Locale.US).format(usedMem / 1024);
                    //String BytesStr = NumberFormat.getNumberInstance(Locale.US).format(usedMem);
                    LOGGER.info("Used Memory: " + MBMemStr + " MB.");
                }
            }
        };

        int waitBeforeRun = ShowSplash ? SplashWait : NoSplashWait;
        new java.util.Timer().schedule(// Start goodies in a few seconds
                new java.util.TimerTask()
        {
            @Override
            public void run()
            {
                _animationTimer.start();
                this.cancel();
            }
        },
                waitBeforeRun
        );

        DumpAllWidgetsInformation();

        stage.setX(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinX());
        stage.setY(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinY());

        stage.setMaximized(true);
    }

    private void BeginServerEtc()
    {
        LOGGER.info("Starting Server");
        _receiveServer.Start();

        TaskManager.getTaskManager().PerformOnStartupTasks(); // perform any tasks designated to be run on startup
        _Heartbeat = new Heartbeat(_Config.getConfiguration().getHeartbeatInterval()); // every n seconds  TODO: make configurable
        _Heartbeat.Start();
        if (_Config.getConfiguration().getMarvinLocalDatafeed())
        {
            objLocalMarvinData = new MarvinLocalData(1);
        }
    }

    public static void DumpThreads(boolean showStack)
    {
        showStack = false;
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);

        LOGGER.info("******* Dumping " + Integer.toString(threadArray.length) + " Threads from thread: " + Thread.currentThread().getName() + " ****************");

        String dumpString = "";
        for (Thread entry : threadArray)
        {
            // info is name,priority,threadgroup
            dumpString += "\t" + entry.toString() + " -- " + entry.getState().toString() + "\n";
            if (true == showStack)
            {
                for (StackTraceElement element : entry.getStackTrace())
                {
                    dumpString += "\t\t" + element.toString() + "\n";
                }

            }

        }
        LOGGER.info(dumpString);

    }

    @Override
    public void stop()
    {
        if (null != Configuration.getConfig())
        {
            Configuration.getConfig().setTerminating();
            StopWidgets();
        }

        if (null != objLocalMarvinData)
        {
            objLocalMarvinData.Shutdown();
        }
        if (null != _receiveServer)
        {
            _receiveServer.Stop();
        }

        if (null != _Heartbeat)
        {
            _Heartbeat.Stop();
        }
        /*
        if (null != _animationTimer)
        {
            _animationTimer.stop();
        }
        LOGGER.info("Animation Timer Stopped");
         */
        //Marvin.DumpThreads(true);
    }

    public static void main(final String[] args)
    {
        if (!JVMversion.meetsMinimumVersion())
        {
            System.out.println("Not valid JVM version.  Requires 1." + JVMversion.MINIMUM_MAJOR_VERSION + " build " + JVMversion.MINIMUM_BUILD_VERSION + " or newer");
            return;
        }
        try
        {
            Thread.currentThread().setName("Main Application Thread");
            Application.launch(args);
            System.exit(0);
        }
        catch (OutOfMemoryError ex)
        {
            JOptionPane.showMessageDialog(null, "Insufficient Memory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            LOGGER.severe(sw.toString());
            LOGGER.severe(ex.toString());
        }
        System.exit(1);
    }
}
