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
 * @author Patrick Kutch
 */
public class Configuration {

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static Configuration _Config = null;

    public static Configuration getConfig() {
        return _Config;
    }

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
    //    private double _topOffset, _bottomOffset;
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
    private boolean _DoNotReportAliasErrors;
    private boolean _ImmediateRefreshRequsted;

    public Configuration() {
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
        _DoNotReportAliasErrors = false;
    }

    void AddAbout() {
        Menu objMenu = new Menu("About");
        MenuItem item = new MenuItem("About");
        fAboutCreated = true;

        item.setOnAction(t -> {
            AboutBox.ShowAboutBox();
        });
        objMenu.getItems().add(item);
        _MenuBar.getMenus().add(objMenu);
    }

    public boolean DoNotReportAliasErrors() {
        return _DoNotReportAliasErrors;
    }

    public void SetDoNotReportAliasErrors(boolean newVal) {
        _DoNotReportAliasErrors = newVal;
    }

    public void addOscarBullhornEntry(String address, int Port, String Key) {
        OscarBullhorn objBH = new OscarBullhorn(address, Port, Key);
        _OscarBullhornList.add(objBH);
        TaskManager TASKMAN = TaskManager.getTaskManager();
        if (false == TASKMAN.TaskExists("OscarBullhornTask")) // go create a task on startup to send the announcements
        {
            TASKMAN.AddOnStartupTask("OscarBullhornTask", new OscarBullhornTask());
        }
    }

    public void DetermineMemorex() {
        double timeCompare = 10000; // 10 seconds
        double currTime = System.currentTimeMillis();
        boolean Live = false;
        boolean Recorded = false;
        if (_LastLiveDataReceived + timeCompare > currTime) {
            Live = true;
        }
        if (_LastRecordedDataReceived + timeCompare > currTime) {
            Recorded = true;
        }
        if (Live && Recorded) {
            TitleSuffix = " {Live and Recorded}";
        } else if (Live) {
            TitleSuffix = " {Live}";
        } else if (Recorded) {
            TitleSuffix = " {Recorded}";
        } else {
            TitleSuffix = "";
        }
    }

    public String getAddress() {
        return _Address;
    }

    public boolean getAllowTasks() {
        return _AllowTasks;
    }

    /*
     * public double getBottomOffset() { return 0.0;//return _bottomOffset; }
     *
     * public void setBottomOffset(double _bottomOffset) { this._bottomOffset =
     * _bottomOffset; }
     *
     * public double getTopOffset() { return 0.0; //return _topOffset; }
     *
     * public void setTopOffset(double _topOffset) { this._topOffset = _topOffset; }
     */
    public double getAppBorderWidth() {
        return _AppBorderWidth;
    }

    public String GetApplicationID() {
        return _ApplicationID;
    }

    public Scene getAppScene() {
        return _appScene;
    }

    public Stage getAppStage() {
        return _AppStage;
    }

    public String getAppTitle() {
        return _AppTitle;
    }

    public int getCanvasHeight() {
        return _CanvasHeight;
    }

    public int getCanvasWidth() {
        return _CanvasWidth;
    }

    public int getCreationHeight() {
        return _CreationHeight;
    }

    public int getCreationWidth() {
        return _CreationWidth;
    }

    public String getCSSFile() {
        return _CSSFile;
    }

    public SimpleDoubleProperty getCurrentHeightProperty() {
        return _CurrHeightProperty;
    }

    public SimpleDoubleProperty getCurrentWidthProperty() {
        return _CurrWidthProperty;
    }

    public boolean getEnableScrollBars() {
        return _EnableScrollBars;
    }

    public boolean getEnforceMediaSupport() {
        return _EnforceMediaSupport;
    }

    public int getHeartbeatInterval() {
        return HeartbeatInterval;
    }

    public int getHeight() {
        return _Height;
    }

    public boolean getIgnoreWebCerts() {
        return _IgnoreWebCerts;
    }

    public int getInsetBottom() {
        return _insetBottom;
    }

    public int getInsetLeft() {
        return _insetLeft;
    }

    public int getInsetRight() {
        return _insetRight;
    }

    public int getInsetTop() {
        return _insetTop;
    }

    public boolean getKioskMode() {
        return _KioskMode;
    }

    public boolean getLegacyInsetMode() {
        return _legacyInsetMode;
    }

    public boolean getMarvinLocalDatafeed() {
        return _MarvinLocalDatafeed;
    }

    public int getMaxPacketSize() {
        return _MaxPacketSize;
    }

    public MenuBar getMenuBar() {
        if (null != _MenuBar && false == fAboutCreated) {
            AddAbout();
        }
        return _MenuBar;
    }

    public ArrayList<OscarBullhorn> getOscarBullhornList() {
        return _OscarBullhornList;
    }

    public TabPane getPane() {
        return _Pane;
    }

    public int getPort() {
        return _port;
    }

    public Screen getPrimaryScreen() {
        return _PrimaryScreen;
    }

    public double getScaleFactor() {
        return _ScaleProperty.getValue();
    }

    public DoubleProperty getScaleProperty() {
        return _ScaleProperty;
    }

    public boolean getShowMenuBar() {
        return _ShowMenuBar;
    }

    public Side getSide() {
        return _Side;
    }

    public long getTimerInterval() {
        return _GuiTimerUpdateInterval;
    }

    public int getWidth() {
        return _Width;
    }

    public boolean isAutoScale() {
        return _AutoScale;
    }

    public boolean isDebugMode() {
        return _DebugMode;
    }

    public boolean isPrimaryScreenDetermined() {
        return _PrimaryScreenDetermined;
    }

    public boolean isRunInDebugger() {
        return _RunInDebugger;
    }

    public void OnLiveDataReceived() {
        _LastLiveDataReceived = System.currentTimeMillis();
    }

    public void OnRecordedDataReceived() {
        _LastRecordedDataReceived = System.currentTimeMillis();
    }

    public boolean refreshRequested() {
        if (_ImmediateRefreshRequsted) {
            _ImmediateRefreshRequsted = false;
            return true;
        }
        return false;
    }

    public void requestImmediateRefresh() {
        _ImmediateRefreshRequsted = true;
    }

    public void restoreCursor() {
        _BusyCursorRequestCount--;
        if (_BusyCursorRequestCount < 1) {
            getAppScene().setCursor(_prevCursor);
            _prevCursor = null;
        }
    }

    public void setAddress(String _Address) {
        this._Address = _Address;
    }

    public void setAllowTasks(boolean _AllowTasks) {
        this._AllowTasks = _AllowTasks;
    }

    public void setAppBorderWidth(double _AppBorderWidth) {
        this._AppBorderWidth = _AppBorderWidth;
    }

    public void SetApplicationID(String newID) {
        _ApplicationID = newID;
    }

    public void setAppScene(Scene scene) {
        if (null == scene) {
            LOGGER.severe("setScene received a NULL argument");
            return;
        }
        _appScene = scene;
    }

    public void setAppStage(Stage _AppStage) {
        this._AppStage = _AppStage;
    }

    public void setAppTitle(String _AppTitle) {
        this._AppTitle = _AppTitle;
    }

    public void setAutoScale(boolean _AutoScale) {
        this._AutoScale = _AutoScale;
    }

    public void setCanvasHeight(int _CanvasHeight) {
        this._CanvasHeight = _CanvasHeight;
    }

    public void setCanvasWidth(int _CanvasWidth) {
        this._CanvasWidth = _CanvasWidth;
    }

    public void setCreationHeight(int _CreationHeight) {
        this._CreationHeight = _CreationHeight;
    }

    public void setCreationWidth(int _CreationWidth) {
        this._CreationWidth = _CreationWidth;
    }

    public void setCSSFile(String _CSSFie) {
        this._CSSFile = _CSSFie;
    }

    public void setCursorToWait() {
        if (_BusyCursorRequestCount++ == 0) {
            _prevCursor = getAppScene().getCursor();
            getAppScene().setCursor(Cursor.WAIT);
        }
    }

    public void setDebugMode(boolean DebugMode) {
        this._DebugMode = DebugMode;
    }

    public void setEnableScrollBars(boolean _EnableScrollBars) {
        this._EnableScrollBars = _EnableScrollBars;
    }

    public void setEnforceMediaSupport(boolean _EnforceMediaSupport) {
        this._EnforceMediaSupport = _EnforceMediaSupport;
    }

    public void setHeartbeatInterval(int HeartbeatInterval) {
        this.HeartbeatInterval = HeartbeatInterval;
    }

    public void setHeight(int _Height) {
        this._Height = _Height;
    }

    public void setIgnoreWebCerts(boolean _IgnoreWebCerts) {
        this._IgnoreWebCerts = _IgnoreWebCerts;
    }

    public void setInsetBottom(int insetBottom) {
        if (insetBottom >= 0) {
            LOGGER.config("Setting application insetBottom to: " + Integer.toString(insetBottom));
            this._insetBottom = insetBottom;
        }
    }

    public void setInsetLeft(int insetLeft) {
        if (insetLeft >= 0) {
            LOGGER.config("Setting application insetLeft to: " + Integer.toString(insetLeft));
            this._insetLeft = insetLeft;
        }
    }

    public void setInsetRight(int insetRight) {
        if (insetRight >= 0) {
            LOGGER.config("Setting application insetRight to: " + Integer.toString(insetRight));
            this._insetRight = insetRight;
        }
    }

    public void setInsetTop(int insetTop) {
        if (insetTop >= 0) {
            LOGGER.config("Setting application insetTop to: " + Integer.toString(insetTop));
            this._insetTop = insetTop;
        }
    }

    public void setKioskMode(boolean _KioskMode) {
        this._KioskMode = _KioskMode;
    }

    public void setLegacyInsetMode(boolean fVal) {
        _legacyInsetMode = fVal;
    }

    public void setMarvinLocalDatafeed(boolean _MarvinLocalDatafeed) {
        this._MarvinLocalDatafeed = _MarvinLocalDatafeed;
    }

    public void setMaxPacketSize(int _MaxPacketSize) {
        this._MaxPacketSize = _MaxPacketSize;
    }

    public void setMenuBar(MenuBar _MenuBar) {
        this._MenuBar = _MenuBar;
    }

    public void setPane(TabPane _Pane) {
        this._Pane = _Pane;
    }

    public void setPort(int _port) {
        this._port = _port;
    }

    public void setPrimaryScreen(Screen _PrimaryScreen) {
        this._PrimaryScreen = _PrimaryScreen;
        setPrimaryScreenDetermined(true);
    }

    public void setPrimaryScreenDetermined(boolean _PrimaryScreenDetermined) {
        this._PrimaryScreenDetermined = _PrimaryScreenDetermined;
    }

    public void setRunInDebugger(boolean _RunInDebugger) {
        this._RunInDebugger = _RunInDebugger;
    }

    public void setScaleFactor(double _ScaleFactor) {
        LOGGER.config("Setting Application Scale Factor to: " + Double.toString(_ScaleFactor));

        _ScaleProperty.setValue(_ScaleFactor);
    }

    public void setShowMenuBar(boolean _ShowMenuBar) {
        this._ShowMenuBar = _ShowMenuBar;
    }

    public void setSide(Side _Side) {
        this._Side = _Side;
    }

    public void setTerminating() {
        _ShuttingDown = true;
    }

    public void setTimerInterval(long newVal) {
        _GuiTimerUpdateInterval = newVal;
    }

    public void setWidth(int _Width) {
        this._Width = _Width;
    }

    public boolean terminating() {
        return _ShuttingDown;
    }

}
