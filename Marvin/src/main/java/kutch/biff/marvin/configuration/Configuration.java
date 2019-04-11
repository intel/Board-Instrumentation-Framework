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
package kutch.biff.marvin.configuration;

import java.util.ArrayList;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kutch.biff.marvin.AboutBox;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.network.OscarBullhorn;
import kutch.biff.marvin.task.OscarBullhornTask;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick Kutch
 */
public class Configuration
{

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static Configuration _Config = null;
    private boolean _DebugMode = false;
    private boolean _KioskMode = false;
    private String _Address = null;
    private int _port = 0;
    private String _AppTitle = null;
    private String _CSSFile = null;
    private int _insetTop, _insetBottom, _insetLeft, _insetRight;
    private boolean _legacyInsetMode;
    private Scene _appScene;
    private SimpleDoubleProperty _ScaleProperty;
    private SimpleDoubleProperty _CurrWidthProperty;
    private SimpleDoubleProperty _CurrHeightProperty;
    private double _topOffset, _bottomOffset;
    private boolean _AutoScale;
    private int _Width, _Height;
    private int _CreationWidth, _CreationHeight;
    public String TitleSuffix;
    private MenuBar _MenuBar;
    boolean _AllowTasks;
    boolean _ShowMenuBar;
    boolean fAboutCreated;
    private int HeartbeatInterval;
    private TabPane _Pane;
    private long _GuiTimerUpdateInterval;
    private Stage _AppStage;
    private double _AppBorderWidth;
    private double _LastLiveDataReceived;
    private double _LastRecordedDataReceived;
    private String _ApplicationID;
    private Side _Side;
    private boolean _IgnoreWebCerts;
    private int _MaxPacketSize;
    private boolean _EnableScrollBars;
    private ArrayList<OscarBullhorn> _OscarBullhornList;
    private boolean _MarvinLocalDatafeed;
    private boolean _ShuttingDown;
    private Screen _PrimaryScreen;
    private boolean _PrimaryScreenDetermined;
    private int _CanvasWidth, _CanvasHeight;
    private boolean _RunInDebugger;
    private boolean _EnforceMediaSupport;
    private Cursor _prevCursor = null;
    private int _BusyCursorRequestCount;
    private boolean _ImmediateRefreshRequsted;

    public Configuration()
    {
        _insetTop = 0;
        _insetBottom = 0;
        _insetLeft = 0;
        _insetRight = 0;
        _legacyInsetMode = false;
        _AutoScale = false;
        _Width = 0;
        _Height = 0;
        _CreationWidth = 0;
        _CreationHeight = 0;
        _CanvasWidth = _CanvasHeight = 0;
        _AllowTasks = true;
        _ShowMenuBar = false;
        TitleSuffix = "";
        fAboutCreated = false;
        HeartbeatInterval = 5; // 5 secs
        _AppBorderWidth = 8;
        _topOffset = 0;
        _bottomOffset = 0;
        _GuiTimerUpdateInterval = 350;
        _ScaleProperty = new SimpleDoubleProperty(1.0);
        _CurrWidthProperty = new SimpleDoubleProperty();
        _CurrHeightProperty = new SimpleDoubleProperty();
        Configuration._Config = this;
        _IgnoreWebCerts = false;
        _ApplicationID = "";
        _Side = Side.TOP;
        _PrimaryScreen = Screen.getPrimary();
        _PrimaryScreenDetermined = false;
//       __DynamicTabList = new ArrayList<>();
        _ShuttingDown = false;

        _LastLiveDataReceived = 0;
        _LastRecordedDataReceived = 0;
        _MaxPacketSize = 16 * 1024;
        _EnableScrollBars = false;
        _OscarBullhornList = new ArrayList<>();
        _MarvinLocalDatafeed = false;
        _RunInDebugger = false;
        _appScene = null;
        _BusyCursorRequestCount = 0;
        _ImmediateRefreshRequsted = false;

    }

    public boolean refreshRequested()
    {
        if (_ImmediateRefreshRequsted)
        {
            _ImmediateRefreshRequsted = false;
            return true;
        }
        return false;
    }
    
    public void requestImmediateRefresh()
    {
        _ImmediateRefreshRequsted = true;
    }
    
    public boolean isRunInDebugger()
    {
        return _RunInDebugger;
    }

    public void setRunInDebugger(boolean _RunInDebugger)
    {
        this._RunInDebugger = _RunInDebugger;
    }

    public void setAppScene(Scene scene)
    {
        if (null == scene)
        {
            LOGGER.severe("setScene received a NULL argument");
            return;
        }
        _appScene = scene;
    }

    public Scene getAppScene()
    {
        return _appScene;
    }

    public int getCanvasWidth()
    {
        return _CanvasWidth;
    }

    public void setCursorToWait()
    {
        if (_BusyCursorRequestCount++ == 0)
        {
            _prevCursor = getAppScene().getCursor();
            getAppScene().setCursor(Cursor.WAIT);
        }
    }

    public void restoreCursor()
    {
        _BusyCursorRequestCount--;
        if (_BusyCursorRequestCount < 1)
        {
            getAppScene().setCursor(_prevCursor);
            _prevCursor = null;
        }
    }

    public void setCanvasWidth(int _CanvasWidth)
    {
        this._CanvasWidth = _CanvasWidth;
    }

    public int getCanvasHeight()
    {
        return _CanvasHeight;
    }

    public void setCanvasHeight(int _CanvasHeight)
    {
        this._CanvasHeight = _CanvasHeight;
    }

    public boolean isPrimaryScreenDetermined()
    {
        return _PrimaryScreenDetermined;
    }

    public void setPrimaryScreenDetermined(boolean _PrimaryScreenDetermined)
    {
        this._PrimaryScreenDetermined = _PrimaryScreenDetermined;
    }

    public Screen getPrimaryScreen()
    {
        return _PrimaryScreen;
    }

    public void setPrimaryScreen(Screen _PrimaryScreen)
    {
        this._PrimaryScreen = _PrimaryScreen;
        setPrimaryScreenDetermined(true);
    }

    public boolean terminating()
    {
        return _ShuttingDown;
    }

    public void setTerminating()
    {
        _ShuttingDown = true;
    }

    public boolean getMarvinLocalDatafeed()
    {
        return _MarvinLocalDatafeed;
    }

    public void setMarvinLocalDatafeed(boolean _MarvinLocalDatafeed)
    {
        this._MarvinLocalDatafeed = _MarvinLocalDatafeed;
    }

    public boolean getEnableScrollBars()
    {
        return _EnableScrollBars;
    }

    public void setEnableScrollBars(boolean _EnableScrollBars)
    {
        this._EnableScrollBars = _EnableScrollBars;
    }

    public void SetApplicationID(String newID)
    {
        _ApplicationID = newID;
    }

    public String GetApplicationID()
    {
        return _ApplicationID;
    }

    public void OnLiveDataReceived()
    {
        _LastLiveDataReceived = System.currentTimeMillis();
    }

    public void OnRecordedDataReceived()
    {
        _LastRecordedDataReceived = System.currentTimeMillis();
    }

    public void DetermineMemorex()
    {
        double timeCompare = 10000; // 10 seconds
        double currTime = System.currentTimeMillis();
        boolean Live = false;
        boolean Recorded = false;
        if (_LastLiveDataReceived + timeCompare > currTime)
        {
            Live = true;
        }
        if (_LastRecordedDataReceived + timeCompare > currTime)
        {
            Recorded = true;
        }
        if (Live && Recorded)
        {
            TitleSuffix = " {Live and Recorded}";
        }
        else if (Live)
        {
            TitleSuffix = " {Live}";
        }
        else if (Recorded)
        {
            TitleSuffix = " {Recorded}";
        }
        else
        {
            TitleSuffix = "";
        }
    }

    public SimpleDoubleProperty getCurrentWidthProperty()
    {
        return _CurrWidthProperty;
    }

    public SimpleDoubleProperty getCurrentHeightProperty()
    {
        return _CurrHeightProperty;
    }

    public Stage getAppStage()
    {
        return _AppStage;
    }

    public void setAppStage(Stage _AppStage)
    {
        this._AppStage = _AppStage;
    }

    public DoubleProperty getScaleProperty()
    {
        return _ScaleProperty;
    }

    public long getTimerInterval()
    {
        return _GuiTimerUpdateInterval;
    }

    public void setTimerInterval(long newVal)
    {
        _GuiTimerUpdateInterval = newVal;
    }

    public int getHeartbeatInterval()
    {
        return HeartbeatInterval;
    }

    public void setHeartbeatInterval(int HeartbeatInterval)
    {
        this.HeartbeatInterval = HeartbeatInterval;
    }

    public TabPane getPane()
    {
        return _Pane;
    }

    public void setPane(TabPane _Pane)
    {
        this._Pane = _Pane;
    }

    public int getWidth()
    {
        return _Width;
    }

    public void setWidth(int _Width)
    {
        this._Width = _Width;
    }

    public int getHeight()
    {
        return _Height;
    }

    public void setHeight(int _Height)
    {
        this._Height = _Height;
    }

    public boolean getAllowTasks()
    {
        return _AllowTasks;
    }

    public boolean getShowMenuBar()
    {
        return _ShowMenuBar;
    }

    public void setShowMenuBar(boolean _ShowMenuBar)
    {
        this._ShowMenuBar = _ShowMenuBar;
    }

    public void setAllowTasks(boolean _AllowTasks)
    {
        this._AllowTasks = _AllowTasks;
    }

    public double getScaleFactor()
    {
        return _ScaleProperty.getValue();
    }

    public void setScaleFactor(double _ScaleFactor)
    {
        LOGGER.config("Setting Application Scale Factor to: " + Double.toString(_ScaleFactor));

        _ScaleProperty.setValue(_ScaleFactor);
    }

    public int getInsetTop()
    {
        return _insetTop;
    }

    public void setInsetTop(int insetTop)
    {
        if (insetTop >= 0)
        {
            LOGGER.config("Setting application insetTop to: " + Integer.toString(insetTop));
            this._insetTop = insetTop;
        }
    }

    public int getInsetBottom()
    {
        return _insetBottom;
    }

    public void setInsetBottom(int insetBottom)
    {
        if (insetBottom >= 0)
        {
            LOGGER.config("Setting application insetBottom to: " + Integer.toString(insetBottom));
            this._insetBottom = insetBottom;
        }
    }

    public boolean getLegacyInsetMode()
    {
        return _legacyInsetMode;
    }

    public void setLegacyInsetMode(boolean fVal)
    {
        _legacyInsetMode = fVal;
    }

    public int getInsetLeft()
    {
        return _insetLeft;
    }

    public void setInsetLeft(int insetLeft)
    {
        if (insetLeft >= 0)
        {
            LOGGER.config("Setting application insetLeft to: " + Integer.toString(insetLeft));
            this._insetLeft = insetLeft;
        }
    }

    public int getInsetRight()
    {
        return _insetRight;
    }

    public void setInsetRight(int insetRight)
    {
        if (insetRight >= 0)
        {
            LOGGER.config("Setting application insetRight to: " + Integer.toString(insetRight));
            this._insetRight = insetRight;
        }
    }

    public static Configuration getConfig()
    {
        return _Config;
    }

    public String getAddress()
    {
        return _Address;
    }

    public void setAddress(String _Address)
    {
        this._Address = _Address;
    }

    public int getPort()
    {
        return _port;
    }

    public void setPort(int _port)
    {
        this._port = _port;
    }

    public String getAppTitle()
    {
        return _AppTitle;
    }

    public void setAppTitle(String _AppTitle)
    {
        this._AppTitle = _AppTitle;
    }

    public String getCSSFile()
    {
        return _CSSFile;
    }

    public void setCSSFile(String _CSSFie)
    {
        this._CSSFile = _CSSFie;
    }

    public boolean isDebugMode()
    {
        return _DebugMode;
    }

    public void setDebugMode(boolean DebugMode)
    {
        this._DebugMode = DebugMode;
    }

    public MenuBar getMenuBar()
    {
        if (null != _MenuBar && false == fAboutCreated)
        {
            AddAbout();
        }
        return _MenuBar;
    }

    public void setMenuBar(MenuBar _MenuBar)
    {
        this._MenuBar = _MenuBar;
    }

    public boolean getKioskMode()
    {
        return _KioskMode;
    }

    public void setKioskMode(boolean _KioskMode)
    {
        this._KioskMode = _KioskMode;
    }

    void AddAbout()
    {
        Menu objMenu = new Menu("About");
        MenuItem item = new MenuItem("About");
        fAboutCreated = true;

        item.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent t)
            {
                AboutBox.ShowAboutBox();
            }
        });
        objMenu.getItems().add(item);
        _MenuBar.getMenus().add(objMenu);
    }

    public boolean isAutoScale()
    {
        return _AutoScale;
    }

    public void setAutoScale(boolean _AutoScale)
    {
        this._AutoScale = _AutoScale;
    }

    public int getCreationWidth()
    {
        return _CreationWidth;
    }

    public void setCreationWidth(int _CreationWidth)
    {
        this._CreationWidth = _CreationWidth;
    }

    public int getCreationHeight()
    {
        return _CreationHeight;
    }

    public void setCreationHeight(int _CreationHeight)
    {
        this._CreationHeight = _CreationHeight;
    }

    /*
    public double getBottomOffset()
    {
        return 0.0;//return _bottomOffset;
    }

    public void setBottomOffset(double _bottomOffset)
    {
        this._bottomOffset = _bottomOffset;
    }

    public double getTopOffset()
    {
        return 0.0; //return _topOffset;
    }

    public void setTopOffset(double _topOffset)
    {
        this._topOffset = _topOffset;
    }
     */
    public double getAppBorderWidth()
    {
        return _AppBorderWidth;
    }

    public void setAppBorderWidth(double _AppBorderWidth)
    {
        this._AppBorderWidth = _AppBorderWidth;
    }

    public Side getSide()
    {
        return _Side;
    }

    public void setSide(Side _Side)
    {
        this._Side = _Side;
    }

    public boolean getIgnoreWebCerts()
    {
        return _IgnoreWebCerts;
    }

    public void setIgnoreWebCerts(boolean _IgnoreWebCerts)
    {
        this._IgnoreWebCerts = _IgnoreWebCerts;
    }

    public int getMaxPacketSize()
    {
        return _MaxPacketSize;
    }

    public void setMaxPacketSize(int _MaxPacketSize)
    {
        this._MaxPacketSize = _MaxPacketSize;
    }

    public void addOscarBullhornEntry(String address, int Port, String Key)
    {
        OscarBullhorn objBH = new OscarBullhorn(address, Port, Key);
        _OscarBullhornList.add(objBH);
        TaskManager TASKMAN = TaskManager.getTaskManager();
        if (false == TASKMAN.TaskExists("OscarBullhornTask")) // go create a task on startup to send the announcements
        {
            TASKMAN.AddOnStartupTask("OscarBullhornTask", new OscarBullhornTask());
        }
    }

    public ArrayList<OscarBullhorn> getOscarBullhornList()
    {
        return _OscarBullhornList;
    }
    public boolean getEnforceMediaSupport()
    {
        return _EnforceMediaSupport;
    }

    public void setEnforceMediaSupport(boolean _EnforceMediaSupport)
    {
        this._EnforceMediaSupport = _EnforceMediaSupport;
    }

}
