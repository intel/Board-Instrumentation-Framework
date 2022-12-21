/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
package kutch.biff.marvin.widget;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.ObservableList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.Pair;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.DefaultPeekabooTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 * @author Patrick.Kutch@gmail.com
 */
abstract public class BaseWidget implements Widget {
    public static String DefaultWidgetDirectory = "Widget";
    private static int _WidgetCount = 0;
    private static final ArrayList<BaseWidget> _WidgetList = new ArrayList<>();
    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    protected static Configuration CONFIG = Configuration.getConfig();
    protected static CircularList<String> DebugStyles = null;

    static public boolean ApplyStyleOverrides(javafx.scene.Node widget, List<String> Styles) {
        // return true;
        if (null == widget || null == Styles || Styles.size() < 1) {
            return true; // audio widget has no
        }
        String StyleString;// = widget.getStyle();
        StyleString = "";
        for (String Style : Styles) {
            StyleString += Style + ";";
        }

        widget.setStyle(StyleString);

        return true;
    }

    public static String convertToFileOSSpecific(String filename) {
        String path = filename;
        if (null == filename) {
            return null;
        }

        if (File.separatorChar == '/') // linux
        {
            path = path.replace('\\', File.separatorChar);
        } else // windows box
        {
            path = path.replace('/', File.separatorChar);
        }
        return path;
    }

    public static String convertToFileURL(String filename) {
        String path = filename;

        if (File.separatorChar == '/') {
        } else {
            path = path.replace(File.separatorChar, '/');
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String retVal = "file:." + path;

        return retVal;
    }

    public static int getWidgetCount() {
        return _WidgetCount;
    }

    public static ArrayList<BaseWidget> getWidgetList() {
        return _WidgetList;
    }

    public static boolean HandleCommonDefinitionFileConfig(BaseWidget widget, FrameworkNode node) {
        if (null == widget) {
            return false;
        }

        if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment")) {
            return true;
        }

        if (node.getNodeName().equalsIgnoreCase("Widget")) // root
        {
            return true;
        }

        if (node.getNodeName().equalsIgnoreCase("AliasList")) // handled elsewhere
        {
            return true;
        }

        if (node.getNodeName().equalsIgnoreCase("Style")) {
            String str = node.getTextContent();
            widget.setBaseCSSFilename(str);

            Utility.ValidateAttributes(new String[]{"ID"}, node);
            if (node.hasAttribute("ID"))// get the style ID if ther is one in the wiget defintion file
            {
                // LOGGER.config("Wiget has CSS ID of [" + node.getAttribute("ID") + "] defined
                // in widget definition file");
                widget.setStyleID(node.getAttribute("ID"));
            }
            return true;
        } else if (node.getNodeName().equalsIgnoreCase("UnitsOverride")) {
            String str = node.getTextContent();
            widget.setUnitsOverride(str);
            return true;
        } else if (node.getNodeName().equalsIgnoreCase("ClickThroughTransparent")) {
            widget.SetClickThroughTransparentRegion(node.getBooleanValue());
            return true;
        }
        if (node.getNodeName().equalsIgnoreCase("MaxSteppedRange")) {
            return widget.HandleMaxSteppedRange(node);
        }
        if (node.getNodeName().equalsIgnoreCase("MinSteppedRange")) {
            return widget.HandleMinSteppedRange(node);
        }
        return false;
    }

    public static int parsePercentWidth(FrameworkNode widgetNode) {
        String str = widgetNode.getAttribute("Width");
        try {
            if (str.contains("%A") || str.contains("%a") || str.contains("%")) {
                str = str.replace("%a", "");
                str = str.replace("%A", "");
                str = str.replace("%", "");
                double percentVal = Double.parseDouble(str);
                double screenWidth = CONFIG.getCanvasWidth();
                if (0 == screenWidth) {
                    screenWidth = CONFIG.getCreationWidth();
                }
                return ((int) (screenWidth * (percentVal / 100.0)));
            } else {
                return ((int) Double.parseDouble(str));
            }
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Width specified " + str);
        }
        return 0;
    }

    public static int parsePercentWidth(FrameworkNode widgetNode, String strAttribute) {
        String str = widgetNode.getAttribute(strAttribute);
        try {
            if (str.contains("%A") || str.contains("%a") || str.contains("%")) {
                str = str.replace("%a", "");
                str = str.replace("%A", "");
                str = str.replace("%", "");
                double percentVal = Double.parseDouble(str);
                double screenWidth = CONFIG.getCanvasHeight();
                if (0 == screenWidth) {
                    screenWidth = CONFIG.getCreationWidth();
                }
                return ((int) (screenWidth * (percentVal / 100.0)));
            } else {
                return ((int) Double.parseDouble(str));
            }
        } catch (NumberFormatException ex) {
            LOGGER.severe("Invalid Width specified " + str);
        }
        return 0;
    }

    public static String ProcessIndexDataRequest(ValueRange valueRange, String splitToken, String strValue) {
        String[] parts = strValue.split(splitToken);
        if (parts.length == 1) {
            return null; // wasn't split
        }
        if (valueRange.getMinimum() < parts.length && valueRange.getMaximum() < parts.length) {
            String retStr = null;

            for (long index = valueRange.getMinimum(); index <= valueRange.getMaximum(); index++) {
                if (null == retStr) {
                    retStr = parts[(int) index];
                } else {
                    retStr += "," + parts[(int) index];
                }
            }
            return retStr;
        } else {
            LOGGER.warning("DataIndex looking for index that is not in data: " + strValue);
        }
        return null;
    }

    protected TaskManager TASKMAN = TaskManager.getTaskManager();
    private final int _WidgetNumber;
    private double _Height;
    private double _Width;
    private int _Row, _RowSpan;
    private int _Column, _ColumnSpan;
    private int _DecimalPlaces;
    private String _FileCSS;
    private String _MinionID;
    private String _Namespace;
    private String _Title;

    private String _UnitsOverride;

    private String _StyleID;
    private List<String> _StyleOverride;
    private HPos _HorizontalPosition;
    private VPos _VerticalPosition;
    protected Pos _Position;
    private ArrayList<Pair<String, String>> _Peekaboos;
    private boolean _ClickThroughTransparentRegion = false;
    protected double _WidthPercentOfParentGrid;
    protected double _HeightPercentOfParentGrid;
    private double _ValueScale;
    private String _PeekabooHideStr;
    private String _PeekabooShowStr;
    private Boolean _PeekabooShowDefault;
    private String _TaskID;
    protected boolean _InitiallyEnabled;
    private String _DefinintionFileDirectory;
    private boolean _Paused;
    private boolean _removed;
    private boolean _DefaultIsSquare;
    private boolean _MouseHasBeenSetup;
    protected String _strAlignment;
    protected GridPane _WidgetParentPane;
    protected GridWidget _WidgetParentGridWidget;
    protected String _DefaultPeekabooAction;
    protected String _DefinitionFile;
    protected String _WidgetType;
    protected ArrayList<String> _RemoteStyleOverrideList;
    protected int _MaxRemoteStyleOverrideToRetain = 5;
    protected boolean StyleUpdatesFromConfigFinished = false;
    protected String _ToolTip = null;
    protected Tooltip _objToolTip = null;
    private List<String> _ToolTipStyle;

    protected boolean _Selected = false;

    protected ArrayList<String> _SelectedStyle;
    protected String _SelectedStyleCSS = null;

    protected String _SelectedStyleID = null;

    private List<Double> _SteppedMaxRanges = null;

    private List<Double> _SteppedMinRanges = null;

    @SuppressWarnings("unused")
    private boolean _widthEqualsHeight = false;

    @SuppressWarnings("unused")
    private boolean _heightEqualsWidth = false;

    private int _SteppedMaxRangeIndex;

    private int _SteppedMinRangeIndex;

    private ValueRange __dataIndexRange;

    private String __dataIndexToken;

    public BaseWidget() {
        _WidgetParentPane = null;
        BaseWidget.CONFIG = Configuration.getConfig();
        BaseWidget._WidgetCount++;
        _WidgetNumber = BaseWidget.getWidgetCount();
        _Height = 0;
        _Width = 0;
        _Row = 0;
        _RowSpan = 1;
        _Column = 0;
        _ColumnSpan = 1;
        _DecimalPlaces = 0;
        _FileCSS = null;
        _MinionID = null;
        _Namespace = null;
        _UnitsOverride = null;
        _TaskID = null;
        _Title = "";
        _StyleID = null;
        _StyleOverride = new ArrayList<>();
        _HorizontalPosition = HPos.CENTER;
        _VerticalPosition = VPos.CENTER;
        _Peekaboos = new ArrayList<>();
        _PeekabooShowDefault = true;
        _DefinintionFileDirectory = DefaultWidgetDirectory;
        _PeekabooHideStr = "Hide";
        _PeekabooShowStr = "Show";
        _InitiallyEnabled = true;
        _Position = Pos.CENTER;
        _Paused = false;
        _DefaultIsSquare = true;
        _WidgetList.add(this);
        _MouseHasBeenSetup = false;
        _strAlignment = "Center";
        _removed = false;
        _DefaultPeekabooAction = null;
        _DefinitionFile = "Not Defined";
        _WidgetType = "Not Defined";
        _RemoteStyleOverrideList = new ArrayList<>();
        _ToolTipStyle = null;
        _SelectedStyle = null;
        _ClickThroughTransparentRegion = false;
        _WidgetParentGridWidget = null;
        _WidthPercentOfParentGrid = 0;
        _HeightPercentOfParentGrid = 0;
        _ValueScale = 1.0;
        _SteppedMaxRangeIndex = 0;
        _SteppedMinRangeIndex = 0;

        __dataIndexRange = ValueRange.of(-1, -1);
        __dataIndexToken = ",";

        if (CONFIG.isDebugMode()) {
            AddAdditionalStyleOverride(AliasMgr.getAliasMgr().GetAlias("DEBUG_STYLE"));
            if (null == DebugStyles) {
                DebugStyles = new CircularList<>();
                DebugStyles.add("-fx-background-color:yellow;-fx-border-color:black;-fx-border-style: solid");
                DebugStyles.add("-fx-background-color:lightblue;-fx-border-color:dimgrey;-fx-border-style: dotted");
                DebugStyles.add("-fx-background-color:darkviolet;-fx-border-color:yellow;-fx-border-style: dashed");
                DebugStyles.add("-fx-background-color:lime;-fx-border-color:deeppink;-fx-border-style: solid");
            }
        }
    }

    public void AddAdditionalStyleOverride(String newOverride) {
        if (false == StyleUpdatesFromConfigFinished) {
            _StyleOverride.add(newOverride);
        } else {
            _RemoteStyleOverrideList.add(newOverride);
            if (_RemoteStyleOverrideList.size() > _MaxRemoteStyleOverrideToRetain) {
                _RemoteStyleOverrideList.remove(0);
            }
        }
    }

    public void addOnDemandStyle(List<String> newStyles) {
        _RemoteStyleOverrideList.clear();
        for (String style : newStyles) {
            AddAdditionalStyleOverride(style);
        }
        ApplyCSS();
    }

    public void addPeekaboo(String Namespace, String ID) {
        Pair<String, String> newPeekaboo = new Pair<>(Namespace, ID);
        _Peekaboos.add(newPeekaboo);
    }

    protected boolean ApplyCSS() {
        String cssFile = GetCSS_File();
        if (null != cssFile) {
            // getStylesheets().clear();

            boolean fRet = true;
            LOGGER.config("Applying Stylesheet: " + cssFile + " to Widget [" + _DefinitionFile + "]");
            // This was a .add(), but changed to Sett all as there was kind of
            // memory leak when I changed style via Minion or MarvinTasks...

            InputStream inputStream;
            BufferedInputStream bis;
            DataInputStream inp;
            String myCssFile = GetCSS_FileWithPath();
            String result;
            ObservableList<String> data;
            try {
                inputStream = new FileInputStream(myCssFile);
                bis = new BufferedInputStream(inputStream);

                result = new String(bis.readAllBytes());
                getStylesheets().clear();
                fRet = getStylesheets().add(result);

            } catch (Exception e) {
                LOGGER.severe("Invalid Playback File: " + cssFile);
            }
            fRet = getStylesheets().setAll(cssFile);
            ObservableList<String> foo = getStylesheets();

            if (false == fRet) {
                LOGGER.severe("Failed to apply Stylesheet " + cssFile);
                return false;
            }
        }
        if (null != getStyleID()) {
            getStylableObject().setId(getStyleID());
        }

        return ApplyStyleOverrides(getStylableObject(), getStyleOverride());
    }

    // nukes old styles string and replaces with these. Used for dynamic data
    // widgets
    public boolean ApplyOverrides() {
        if (null == getStylableObject()) {
            return true; // audio widget has no
        }
        String StyleString = "";
        for (String Style : getStyleOverride()) {
            StyleString += Style + ";";
        }
        getStylableObject().setStyle(StyleString);

        return true;
    }

    protected boolean ApplySelectedCSS() {
        if (null != GetSelectedCSS_File()) {
            boolean fRet = true;
            fRet = getStylesheets().setAll(GetSelectedCSS_File());

            if (false == fRet) {
                LOGGER.severe("Failed to apply Selected Stylesheet " + GetCSS_File());
                return false;
            }
        }
        if (null != _SelectedStyleID) {
            getStylableObject().setId(_SelectedStyleID);
        }

        return ApplyStyleOverrides(getStylableObject(), _SelectedStyle);
    }

    public void ConfigureAlignment() {
        Node objStylable = getStylableObject();
        if (objStylable != null) {
            GridPane.setValignment(getStylableObject(), getVerticalPosition());
            GridPane.setHalignment(getStylableObject(), getHorizontalPosition());
        }
    }

    protected void ConfigureDimentions() {
        Region regionNode = getRegionObject();

        if (null == regionNode) {
            LOGGER.severe(getName() + " : Should NOT BE here, NULL Widget pass to Config Dimensions");
            return;
        }
        PreConfigDimensions(regionNode);
        if (getWidth() > 0) {
            regionNode.setPrefWidth(getWidth());
            regionNode.setMinWidth(getWidth());
            regionNode.setMaxWidth(getWidth());
        }
        if (getHeight() > 0) {
            regionNode.setPrefHeight(getHeight());
            regionNode.setMinHeight(getHeight());
            regionNode.setMaxHeight(getHeight());
        }
    }

    protected void FireDefaultPeekaboo() {
        if (null != _DefaultPeekabooAction) {
            /*
             * Some default action for peekaboo, don't want it to be general for all widgets
             * with same peekabook id/namespace, so simply call a worker function with that
             * peekaboo string at a later time (let eveyrthing load)
             */
            SendDefaultPeekabooAction(100);
        }
    }

    public ValueRange get__dataIndex() {
        return __dataIndexRange;
    }

    public String get__dataIndexToken() {
        return __dataIndexToken;
    }

    public String getAlignment() {
        return _strAlignment;
    }

    protected double getAncestrialWidth() {
        double width = getWidth() * CONFIG.getScaleFactor();
        GridWidget currParent = _WidgetParentGridWidget;

        if (width < 1 && currParent != null) {
            while (width == 0 && null != currParent) {
                currParent = currParent.getParentGridWidget();
                if (null != currParent) {
                    width = currParent.getWidth() * CONFIG.getScaleFactor();
                }
            }
        }
        return width;
    }

    public String getBaseCSSFilename() {
        return _FileCSS;
    }

    public boolean GetClickThroughTransparentRegion() {
        return _ClickThroughTransparentRegion;
    }

    public int getColumn() {
        return _Column;
    }

    public int getColumnSpan() {
        return _ColumnSpan;
    }

    public Dimension getConfiguredDimensions() {
        Dimension objDimension = new Dimension();
        objDimension.setSize(getWidth() * CONFIG.getScaleFactor(), getHeight() * CONFIG.getScaleFactor());
        return objDimension;
    }

    protected String GetCSS_File() {
        String strFile = getBaseCSSFilename();
        if (null != strFile) {
            File file = new File(strFile); // first look for fully qualified path

            if (false == file.exists()) { // if didn't find, look in same directory that widget was defined in
                strFile = getDefinintionFileDirectory() + File.separatorChar + strFile;
                file = new File(strFile);

                if (false == file.exists()) {
                    LOGGER.severe("Unable to locate Stylesheet: " + strFile);
                    return null;
                }
            }

            return convertToFileURL(strFile);
        }
        return null;
    }

    protected String GetCSS_FileWithPath() {
        String strFile = getBaseCSSFilename();
        if (null != strFile) {
            File file = new File(strFile); // first look for fully qualified path

            if (false == file.exists()) { // if didn't find, look in same directory that widget was defined in
                strFile = getDefinintionFileDirectory() + File.separatorChar + strFile;
                file = new File(strFile);

                if (false == file.exists()) {
                    LOGGER.severe("Unable to locate Stylesheet: " + strFile);
                    return null;
                }
            }

            return strFile;
        }
        return null;
    }


    public double getCurrentMaxSteppedRange() {
        if (null == _SteppedMaxRanges) {
            return -1;
        }
        return _SteppedMaxRanges.get(_SteppedMaxRangeIndex);

    }

    public double getCurrentMinSteppedRange() {
        if (null == _SteppedMinRanges) {
            return -1;
        }
        return _SteppedMinRanges.get(_SteppedMinRangeIndex);

    }

    public String[] GetCustomAttributes() {
        return null;
    }

    public int getDecimalPlaces() {
        return _DecimalPlaces;
    }

    public boolean getDefaultIsSquare() {
        return _DefaultIsSquare;
    }

    public String getDefinintionFileDirectory() {
        return _DefinintionFileDirectory;
    }

    public boolean getExceededMaxSteppedRange(double compareVal) {
        if (null == _SteppedMaxRanges) {
            return false;
        }
        if (_SteppedMaxRangeIndex >= _SteppedMaxRanges.size() - 1) // on last one
        {
            return false;
        }
        return (compareVal > _SteppedMaxRanges.get(_SteppedMaxRangeIndex));
    }

    public boolean getExceededMinSteppedRange(double compareVal) {
        if (null == _SteppedMinRanges) {
            return false;
        }
        if (_SteppedMinRangeIndex >= _SteppedMinRanges.size() - 1) // on last one
        {
            return false;
        }
        return (compareVal < _SteppedMinRanges.get(_SteppedMinRangeIndex));
    }

    public double getHeight() {
        return _Height;
    }

    public double getHeightPercentOfParentGrid() {
        return _HeightPercentOfParentGrid;
    }

    public HPos getHorizontalPosition() {
        return _HorizontalPosition;
    }

    public String getMinionID() {
        return _MinionID;
    }

    public String getName() {
        String strList[] = this.getClass().toString().split("\\."); // Need the \\ as delimeter for period
        String retStr = "Something baaaad happened";
        if (strList.length > 1) {
            retStr = strList[strList.length - 1] + " [#" + Integer.toString(getWidgetNumber()) + "]";
        }
        return retStr;
    }

    public String getNamespace() {
        return _Namespace;
    }

    public double getNextMaxSteppedRange(double currVal) {
        if (null == _SteppedMaxRanges) {
            LOGGER.severe("getNextMaxSteppedRange() called without having been configured.");
            return 0.0;
        }
        while (getExceededMaxSteppedRange(currVal)) {
            _SteppedMaxRangeIndex++;
        }

        return _SteppedMaxRanges.get(_SteppedMaxRangeIndex);
    }

    public double getNextMinSteppedRange(double currVal) {
        if (null == _SteppedMinRanges) {
            LOGGER.severe("getNextMinSteppedRange() called without having been configured.");
            return 0.0;
        }
        while (getExceededMinSteppedRange(currVal)) {
            _SteppedMinRangeIndex++;
        }
        return _SteppedMinRanges.get(_SteppedMinRangeIndex);
    }

    public GridWidget getParentGridWidget() {
        return _WidgetParentGridWidget;
    }

    public GridPane getParentPane() {
        return _WidgetParentPane;
    }

    public String getPeekabooHideStr() {
        return _PeekabooHideStr;
    }

    public String getPeekabooShowStr() {
        return _PeekabooShowStr;
    }

    public Pos getPosition() {
        return _Position;
    }

    public Dimension getRealDimensions() {
        Double Height = 0.0, Width = 0.0;

        Dimension objDimension = new Dimension();
        if (null != getRegionObject()) {
            Width = getRegionObject().getWidth();
            Height = getRegionObject().getHeight();
        } else if (null != getStylableObject()) {
            Width = getStylableObject().getBoundsInParent().getWidth();
            Height = getStylableObject().getBoundsInParent().getHeight();
        }
        objDimension.setSize(Width, Height);
        return objDimension;
    }

    @Override
    public Region getRegionObject() {
        try {
            return (Region) getStylableObject();
        } catch (Exception ex) {
            return null;
        }
    }

    public javafx.scene.Node getRemovableNode() {
        return getStylableObject();
    }

    public int getRow() {
        return _Row;
    }

    public int getRowSpan() {
        return _RowSpan;
    }

    private String GetSelectedCSS_File() {
        String strFile = getBaseCSSFilename();
        if (null != _SelectedStyleCSS) {
            File file = new File(strFile); // first look for fully qualified path

            if (false == file.exists()) { // if didn't find, look in same directory that widget was defined in
                strFile = getDefinintionFileDirectory() + File.separatorChar + _SelectedStyleCSS;
                file = new File(strFile);

                if (false == file.exists()) {
                    LOGGER.severe("Unable to locate Selection Stylesheet: " + _SelectedStyleCSS);
                    return null;
                }
            }

            return convertToFileURL(strFile);
        }
        return null;
    }

    public String getStyleID() {
        return _StyleID;
    }

    public List<String> getStyleOverride() {
        if (false == StyleUpdatesFromConfigFinished) {
            return _StyleOverride;
        }
        ArrayList<String> styleList = new ArrayList<>(_StyleOverride);
        styleList.addAll(_RemoteStyleOverrideList);
        return styleList;
    }

    public String getTaskID() {
        return _TaskID;
    }

    public String getTitle() {
        return _Title;
    }

    public String getUnitsOverride() {
        return _UnitsOverride;
    }

    public double getValueScale() {
        return _ValueScale;
    }

    public VPos getVerticalPosition() {
        return _VerticalPosition;
    }

    public int getWidgetNumber() {
        return _WidgetNumber;
    }

    public double getWidth() {
        return _Width;
    }

    public double getWidthPercentOfParentGrid() {
        return _WidthPercentOfParentGrid;
    }

    @Override
    public void HandleCustomStyleOverride(FrameworkNode styleNode) {

    }

    public void HandleDefaultPeekaboo() {
        try {
            if (!HandlePeekabooMessage(_DefaultPeekabooAction)) {
                LOGGER.warning("Attempting to perfrom default Peekaboo action :" + _DefaultPeekabooAction + " again.");
                // SendDefaultPeekabooAction(250);
            }
        } catch (Exception ex) {
            LOGGER.warning(ex.toString());
        }

    }

    private void HandleMarvinPeekaboo(String strRequest) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;
        FrameworkNode baseNode;

        // Can't pass XML within XML, so for this, change < for [ and ] for >
        String strMassaged = strRequest.substring("Marvin:".length()).replace('[', '<').replace(']', '>');
        // strMassaged = toEscaped(strMassaged);
        try {
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                LOGGER.severe(ex.toString());
                return;
            }

            doc = db.parse(new ByteArrayInputStream(strMassaged.getBytes()));
            NodeList appStuff = doc.getChildNodes();
            baseNode = new FrameworkNode(appStuff.item(0));
        } catch (SAXException | IOException ex) {
            LOGGER.severe(ex.toString());
            LOGGER.severe("Invalid Peekaboo Marvin data received: " + strRequest);
            LOGGER.severe(strMassaged);
            return;
        }

        if (baseNode.getNodeName().equalsIgnoreCase("StyleOverride")) {
            HandleRemoteStyleOverride(baseNode);
        } else if (baseNode.getNodeName().equalsIgnoreCase("Title")) {
            HandleRemoteTitleUpdate(baseNode);
        } else if (baseNode.getNodeName().equalsIgnoreCase("ValueRange")) {
            HandleRemoteValueRangeUpdate(baseNode);
        } else {
            LOGGER.warning("Received unknown Peekaboo Marvin data: " + strRequest);
        }
    }

    public boolean HandleMaxSteppedRange(FrameworkNode node) {
        List<Double> rangeList = ReadRange(node, true);
        if (null == rangeList) {
            return false;
        }
        setMaxSteppedRange(rangeList);
        return true;
    }

    public boolean HandleMinSteppedRange(FrameworkNode node) {
        List<Double> rangeList = ReadRange(node, false);
        if (null == rangeList) {
            return false;
        }
        setMinSteppedRange(rangeList);
        return true;
    }

    protected boolean HandlePeekabooMessage(String strPeek) {
        if (strPeek.equalsIgnoreCase(getPeekabooHideStr()) || (strPeek.equalsIgnoreCase("Hide"))) {
            getStylableObject().setVisible(false);
        } else if (0 == strPeek.compareToIgnoreCase(getPeekabooShowStr()) || (strPeek.equalsIgnoreCase("Show"))) {
            getStylableObject().setVisible(true);
        }
        // Some widgets (Buttons) can be enable and disable too, so let's override this.
        else if (true == SupportsEnableDisable() && strPeek.equalsIgnoreCase("Enable")
                || strPeek.equalsIgnoreCase("Disable")) {
            if (strPeek.equalsIgnoreCase("Enable")) {
                SetEnabled(true);
            } else {
                SetEnabled(false);
            }
        } else if (strPeek.equalsIgnoreCase("Pause")) {
            if (false == _Paused) {
                _Paused = true;
                OnPaused();
            }
        } else if (strPeek.equalsIgnoreCase("Resume")) {
            if (true == _Paused) {
                _Paused = false;
                OnResumed();
            }
        } else if (strPeek.equalsIgnoreCase("Select")) {
            HandleSelectionState(true);
        } else if (strPeek.equalsIgnoreCase("DeSelect")) {
            HandleSelectionState(false);
        } else if (strPeek.equalsIgnoreCase("Remove")) {
            if (!ProperlySetup()) {
                LOGGER.severe(getClass().toString()
                        + " didn't register with SetParent, so can't use Peekaboo Remove/Insert.  Programming error, report it to Patrick.");
                return false;
            }
            if (!_removed) {
                if (_WidgetParentPane.getChildren().remove(getRemovableNode())) {
                    _removed = true;
                    LOGGER.info("Removing Widget from Grid due to Peekaboo Remove command");
                } else {
                    LOGGER.warning(
                            "Attempt to remove Widget from Grid due to Peekaboo Remove command failed for some reason.");
                    return false;
                }
            }
        } else if (strPeek.equalsIgnoreCase("Insert")) {
            if (!ProperlySetup()) {
                LOGGER.severe(getClass().toString()
                        + " didn't register with SetParent, so can't use Peekaboo Remove/Insert.  Programming error, report it to Patrick.");
                return false;
            }
            if (_removed) {
                _removed = false;
                _WidgetParentPane.getChildren().add(getStylableObject());
                LOGGER.info("Re-Inserting Widget int Grid due to Peekaboo Insert command");
            }
        } else if (strPeek.length() > "Marvin:".length()
                && strPeek.substring(0, "Marvin:".length()).equalsIgnoreCase("Marvin:")) {
            HandleMarvinPeekaboo(strPeek);
        } else if (strPeek.length() >= "Reset".length()
                && strPeek.substring(0, "Reset".length()).equalsIgnoreCase("Reset")) {
            String strParam = null;
            if (strPeek.length() > "Reset:".length()
                    && strPeek.substring(0, "Reset:".length()).equalsIgnoreCase("Reset:")) {
                strParam = strPeek.substring("Reset:".length());
            }
            resetState(strParam);
        } else {
            LOGGER.severe("Received invalid Peekaboo option: " + strPeek);
            return false;
        }
        return true;
    }

    public boolean handlePercentageDimentions() {
        boolean changed = false;
        if (getWidthPercentOfParentGrid() > 0) {
            double parentWidth = _WidgetParentGridWidget.getWidth();
            GridWidget currParent = _WidgetParentGridWidget;

            while (parentWidth == 0) {
                currParent = currParent.getParentGridWidget();
                if (null != currParent) {
                    parentWidth = currParent.getWidth();
                } else {
                    break;
                }
            }
            if (parentWidth == 0) {
                LOGGER.severe("Widget [" + getName()
                        + "] Width specified as percentage of parent grid - but parent grid width not specified.");
                return false;
            }

            double width = parentWidth * (getWidthPercentOfParentGrid() / 100);
            setWidth(width);
            changed = true;
        }
        if (getHeightPercentOfParentGrid() > 0) {
            double parentHeight = _WidgetParentGridWidget.getHeight();
            GridWidget currParent = _WidgetParentGridWidget;

            while (parentHeight == 0) {
                currParent = currParent.getParentGridWidget();
                if (null != currParent) {
                    parentHeight = currParent.getHeight();
                } else {
                    break;
                }
            }
            if (parentHeight == 0) {
                LOGGER.severe("Widget [" + getName()
                        + "] Height specified as percentage of parent grid - but parent grid width not specified.");
                return false;
            }

            double Height = parentHeight * (getHeightPercentOfParentGrid() / 100);
            setHeight(Height);
            changed = true;
        }
        if (changed) {
            ConfigureDimentions();
        }

        return true;
    }

    private void HandleRemoteStyleOverride(FrameworkNode node) {
        if (false == StyleUpdatesFromConfigFinished) {
            StyleUpdatesFromConfigFinished = true;
        }

        if (WidgetBuilder.HandleStyleOverride(this, node)) {
            ApplyCSS();
        }
    }

    private void HandleRemoteTitleUpdate(FrameworkNode node) {
        String strTitle = node.getTextContent();
        if (strTitle.length() > 0) {
            LOGGER.info("Updating Widget Tilte via Peekaboo RemoteTitleUpdate to: " + strTitle);
            UpdateTitle(strTitle);
        } else {
            LOGGER.warning("Received Peekaboo Marvin request for new Title, but no String Title Given");
        }
    }

    private void HandleRemoteValueRangeUpdate(FrameworkNode node) {
        Utility.ValidateAttributes(new String[]{"Min", "Max"}, node);

        String strMin = "";
        if (node.hasAttribute("Min")) {
            strMin = node.getAttribute("Min");
        }
        String strMax = "";
        if (node.hasAttribute("Max")) {
            strMax = node.getAttribute("Max");
        }

        if (strMin.length() > 0 || strMax.length() > 0) {
            LOGGER.info("Updating Widget ValueRange via Peekaboo");
            if (HandleValueRange(node)) {
                UpdateValueRange();
            }
        } else {
            LOGGER.warning("Received Invalid Peekaboo Marvin request for new ValueRange: " + node.toString());
        }
    }

    public boolean HandleSelectionConfig(FrameworkNode styleNode) {
        /*
         * <SelectedStyle> <Item>-fx-background-color:yellow</Item> </SelectedStyle>
         */
        _SelectedStyle = new ArrayList<>();

        Utility.ValidateAttributes(new String[]{"File", "ID"}, styleNode);
        if (styleNode.hasAttribute("File")) {
            _SelectedStyleCSS = styleNode.getAttribute("File");
        }
        if (styleNode.hasAttribute("ID")) {
            _SelectedStyleID = styleNode.getAttribute("ID");
        }

        for (FrameworkNode node : styleNode.getChildNodes()) {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment")) {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("Item")) {
                _SelectedStyle.add(node.getTextContent());
            } else {
                LOGGER.severe("Unknown Tag under Selected : " + node.getNodeName());
                return false;
            }
        }
        return true;
    }

    private void HandleSelectionState(boolean fSelected) {
        if (fSelected == _Selected) {
            return; // already in this state
        }
        if (fSelected && null == _SelectedStyle) {
            LOGGER.warning("Tried to select Widget [" + getName() + "] that has no <SelectedStyle> configure.");
            return;
        }
        _Selected = fSelected;
        if (_Selected) {
            // ApplyStyleOverrides(getStylableObject(), _SelectedStyle);
            ApplySelectedCSS();
            LOGGER.info("Selecting " + getName());
        } else {
            getStylableObject().setStyle("");
            ApplyCSS();
            // ApplyStyleOverrides(getStylableObject(), getStyleOverride());
            LOGGER.info("Deselecting " + getName());
        }
    }

    public boolean HandleToolTipConfig(FrameworkNode baseNode) {
        /*
         * <ToolTip>My Tool Tip</ToolTop> or <ToolTip> <DisplayString>My Tool
         * Tip</DisplayString>
         * <StyleOverride><Item>-fx-background-color:blue</Item></StyleOverride>
         * </ToolTip>
         */
        String strDisplay;

        if (baseNode.getChildNodes().size() == 0) {
            strDisplay = baseNode.getTextContent();
            if (strDisplay.length() > 0) {
                SetToolTip(strDisplay);
                return true;
            }
            LOGGER.severe("Invalid ToolTop specified.");
            return false;
        }
        if (baseNode.hasChild("DisplayString")) {
            strDisplay = baseNode.getChild("DisplayString").getTextContent();
            if (strDisplay.length() > 0) {
                SetToolTip(strDisplay);
            } else {
                LOGGER.severe("Invalid ToolTop specified.");
                return false;
            }
        }
        if (baseNode.hasChild("StyleOverride")) {
            FrameworkNode styleNode = baseNode.getChild("StyleOverride");
            _ToolTipStyle = new ArrayList<>();

            for (FrameworkNode node : styleNode.getChildNodes()) {
                if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment")) {
                    continue;
                }
                if (node.getNodeName().equalsIgnoreCase("Item")) {
                    _ToolTipStyle.add(node.getTextContent());
                } else {
                    LOGGER.severe("Unknown Tag under <ToolTip> <StyleOverride>: " + node.getNodeName());
                    return false;
                }
            }
        }

        return true;
    }

    protected void HandleToolTipInit() {
        if (null != _ToolTip) {
            _objToolTip = new Tooltip(_ToolTip);
            if (null != _ToolTipStyle && _ToolTipStyle.size() > 0) {
                String StyleString = "";
                for (String Style : _ToolTipStyle) {
                    StyleString += Style + ";";
                }
                _objToolTip.setStyle(StyleString);
            }
        }
    }

    /**
     * Sets range for widget - not valid for all widgets
     *
     * @param rangeNode
     * @return
     */
    public boolean HandleValueRange(FrameworkNode rangeNode) {
        LOGGER.severe(getName() + " does not use the <Value Range> tag");
        return false;
    }

    public void HandleWidgetSpecificAttributes(FrameworkNode widgetNode) {

    }

    // handles any specific 'sub tags' in a widget
    public boolean HandleWidgetSpecificConfiguration(FrameworkNode widgetNode) {
        return true;
    }

    // handles unknown tags within widget app settings
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetTag) {
        return false;
    }

    public void initialSteppedRangeSetup(double min, double max) {
        if (!SupportsSteppedRanges()) {
            return;
        }
        if (null != _SteppedMaxRanges) {
            while (getExceededMaxSteppedRange(max) && _SteppedMaxRangeIndex < _SteppedMaxRanges.size()) {
                _SteppedMaxRangeIndex++;
            }
            if (_SteppedMaxRanges.isEmpty()) {
                _SteppedMaxRanges = null;
            }
        }

        if (null != _SteppedMinRanges) {
            while (getExceededMinSteppedRange(max) && _SteppedMinRangeIndex < _SteppedMinRanges.size()) {
                _SteppedMinRangeIndex++;
            }
            if (_SteppedMinRanges.isEmpty()) {
                _SteppedMinRanges = null;
            }
        }

    }

    public boolean isMouseHasBeenSetup() {
        return _MouseHasBeenSetup;
    }

    public boolean isOutsideConfiguredSize() {
        double Height, Width;

        Dimension visDim = getRealDimensions();
        Height = visDim.height;
        Width = visDim.width;

        double configHeight = getAncestrialWidth();

        double wDelta = Width - (configHeight);
        double hDelta = Height - getHeight() * CONFIG.getScaleFactor();

        if (Width > configHeight) {
            System.out.println("# " + Integer.toString(this.getWidgetNumber()) + " " + Double.toString(wDelta));
        }
        if (getWidth() > 0 && wDelta > 2) {
            return true;
        } else if (getHeight() > 0 && hDelta > 2) {
            return true;
        }
        return false;
    }

    public boolean IsPaused() {
        return _Paused;

    }

    public Boolean isPeekabooShowDefault() {
        return _PeekabooShowDefault;
    }

    public void mouseHandler(MouseEvent event) {
        BaseWidget objWidget = this;

        if (CONFIG.isDebugMode() && event.isShiftDown()) {
            LOGGER.info(objWidget.toString(true));
        } else if (CONFIG.isDebugMode() && event.isControlDown()) {
            if (null != getStylableObject()) {
                AddAdditionalStyleOverride(DebugStyles.GetNext());
                ApplyCSS();
            }
        } else if (null != getTaskID() && true == CONFIG.getAllowTasks()) {
            TASKMAN.PerformTask(getTaskID());
        }
    }

    @Override
    public void OnPaused() {
    }

    @Override
    public void OnResumed() {
    }

    public boolean parseHeight(FrameworkNode widgetNode) {
        String str = widgetNode.getAttribute("Height");
        try {
            if (str.contains("%G") || str.contains("%g")) // % of parent grid
            {
                str = str.replace("%g", "");
                str = str.replace("%G", "");
                double percentVal = Double.parseDouble(str);
                setHeightPercentOfParentGrid(percentVal);
            } else if (str.contains("%A") || str.contains("%a") || str.contains("%")) // % of app
            {
                str = str.replace("%a", "");
                str = str.replace("%A", "");
                str = str.replace("%", "");
                double percentVal = Double.parseDouble(str);
                double canvasHeight = CONFIG.getCanvasHeight();
                if (0 == canvasHeight) {
                    Rectangle2D visualBounds = CONFIG.getPrimaryScreen().getVisualBounds();
                    canvasHeight = (int) visualBounds.getHeight();
                }
                setHeight(canvasHeight * (percentVal / 100.0));
            } else if (str.equalsIgnoreCase("width")) {
                _heightEqualsWidth = true;
                setDefaultIsSquare(true);
            } else {
                setHeight(Double.parseDouble(str));
            }
        } catch (NumberFormatException ex) {
            LOGGER.severe(getName() + ": Invalid Height specified " + str);
            return false;
        }
        return true;
    }

    public boolean parseWidth(FrameworkNode widgetNode) {
        String str = widgetNode.getAttribute("Width");
        try {
            if (str.contains("%G") || str.contains("%g")) {
                str = str.replace("%g", "");
                str = str.replace("%G", "");
                double percentVal = Double.parseDouble(str);
                setWidthPercentOfParentGrid(percentVal);
            } else if (str.contains("%A") || str.contains("%a") || str.contains("%")) {
                str = str.replace("%a", "");
                str = str.replace("%A", "");
                str = str.replace("%", "");
                double percentVal = Double.parseDouble(str);
                double canvasWidth = CONFIG.getCanvasWidth();
                if (0 == canvasWidth) {
                    canvasWidth = CONFIG.getCreationWidth();
                }
                setWidth(canvasWidth * (percentVal / 100.0));
            } else if (str.equalsIgnoreCase("height")) {
                _widthEqualsHeight = true;
                setDefaultIsSquare(true);
            } else {
                setWidth(Double.parseDouble(str));
            }
        } catch (NumberFormatException ex) {
            LOGGER.severe(getName() + ": Invalid Width specified " + str);
            return false;
        }
        return true;
    }

    /**
     * @return
     */
    @Override
    public boolean PerformPostCreateActions(GridWidget parentGrid, boolean updateToolTipOnly) {
        if (true == updateToolTipOnly) {
            if (CONFIG.isDebugMode()) {
                _ToolTip = this.toString();
            }
            if (_ToolTip != null && null != getStylableObject()) {
                HandleToolTipInit();
                Tooltip.install(this.getStylableObject(), _objToolTip);
            }
            return true;
        }
        _WidgetParentGridWidget = parentGrid;
        if (CONFIG.isDebugMode()) {
            _ToolTip = this.toString();
        }
        if (_ToolTip != null && null != getStylableObject()) {
            HandleToolTipInit();
            Tooltip.install(this.getStylableObject(), _objToolTip);
        }
        if (GetClickThroughTransparentRegion() && null != getStylableObject()) {
            getStylableObject().setPickOnBounds(false);
        }

        FireDefaultPeekaboo();
        return handlePercentageDimentions();
    }

    /**
     * * If no user configured size, get defaults from widget
     *
     * @param objRegion
     */
    protected void PreConfigDimensions(Region objRegion) {
        if (true == getDefaultIsSquare()) {
            if (getWidth() > 0 && getHeight() <= 0) {
                setHeight(_Width);
            } else if (getWidth() <= 0 && getHeight() > 0) {
                setWidth(_Height);
            }
        }
    }

    @Override
    public void PrepareForAppShutdown() {

    }

    public boolean ProperlySetup() {
        return _WidgetParentPane != null;
    }

    private List<Double> ReadRange(FrameworkNode node, boolean mustIncrease) {
        List<Double> retList = new ArrayList<>();
        for (String strVal : node.getTextContent().split(",")) {
            try {
                double dVal = Double.parseDouble(strVal);
                if (retList.size() > 1) {
                    if (mustIncrease && retList.get(retList.size() - 1) > dVal) {
                        LOGGER.severe(
                                "MaxSteppedRange must increase in value for each number.: " + node.getTextContent());
                        return null;
                    } else if (!mustIncrease && dVal > retList.get(retList.size() - 1)) {
                        LOGGER.severe(
                                "MinSteppedRange must decrease in value for each number.: " + node.getTextContent());
                        return null;
                    }
                }
                retList.add(dVal);
            } catch (NumberFormatException ex) {
                LOGGER.severe("Invalid stepped range value:" + strVal);
                return null;
            }
        }
        return retList;
    }

    @Override
    public void resetState(String param) {
        LOGGER.warning(
                "Tried to perform resetState for widget [" + this.getName() + "] that does not support this feature");
    }

    protected void resetSteppedRange() {
        _SteppedMinRangeIndex = 0;
        _SteppedMaxRangeIndex = 0;
    }

    private void SendDefaultPeekabooAction(int time) {
        if (null != _DefaultPeekabooAction) {
            /*
             * Some default action for peekaboo, don't want it to be general for all widgets
             * with same peekabook id/namespace, so simply call a worker function with that
             * peekaboo string at a later time (let eveyrthing load), but it needs to be in
             * GUI thread, thus a postponed task.
             */
            DefaultPeekabooTask objTask = new DefaultPeekabooTask(this);
            TaskManager.getTaskManager().AddPostponedTask(objTask, time);
        }
    }

    public void set__dataIndex(ValueRange __dataIndex) {
        this.__dataIndexRange = __dataIndex;
    }

    public void set__dataIndexToken(String __dataIndexToken) {
        this.__dataIndexToken = __dataIndexToken;
    }

    public boolean setAlignment(String alignString) {
        _strAlignment = alignString;
        if (0 == alignString.compareToIgnoreCase("Center")) {
            setHorizontalPosition(HPos.CENTER);
            setVerticalPosition(VPos.CENTER);
            _Position = Pos.CENTER;
        } else if (0 == alignString.compareToIgnoreCase("N")) {
            setHorizontalPosition(HPos.CENTER);
            setVerticalPosition(VPos.TOP);
            _Position = Pos.TOP_CENTER;
        } else if (0 == alignString.compareToIgnoreCase("NE")) {
            setHorizontalPosition(HPos.RIGHT);
            setVerticalPosition(VPos.TOP);
            _Position = Pos.TOP_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("E")) {
            setHorizontalPosition(HPos.RIGHT);
            setVerticalPosition(VPos.CENTER);
            _Position = Pos.CENTER_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("SE")) {
            setHorizontalPosition(HPos.RIGHT);
            setVerticalPosition(VPos.BOTTOM);
            _Position = Pos.BOTTOM_RIGHT;
        } else if (0 == alignString.compareToIgnoreCase("S")) {
            setHorizontalPosition(HPos.CENTER);
            setVerticalPosition(VPos.BOTTOM);
            _Position = Pos.BOTTOM_CENTER;
        } else if (0 == alignString.compareToIgnoreCase("SW")) {
            setHorizontalPosition(HPos.LEFT);
            setVerticalPosition(VPos.BOTTOM);
            _Position = Pos.BOTTOM_LEFT;
        } else if (0 == alignString.compareToIgnoreCase("W")) {
            setHorizontalPosition(HPos.LEFT);
            setVerticalPosition(VPos.CENTER);
            _Position = Pos.CENTER_LEFT;
        } else if (0 == alignString.compareToIgnoreCase("NW")) {
            setHorizontalPosition(HPos.LEFT);
            setVerticalPosition(VPos.TOP);
            _Position = Pos.TOP_LEFT;
        } else {
            LOGGER.severe("Invalid Alignment indicated in config file: " + alignString);
            return false;
        }
        return true;
    }

    public void setBaseCSSFilename(String _FileCSS) {
        this._FileCSS = _FileCSS;
    }

    @Override
    public void SetClickThroughTransparentRegion(boolean _CanClickOnTransparent) {
        this._ClickThroughTransparentRegion = _CanClickOnTransparent;
    }

    public void setColumn(int _Column) {
        this._Column = _Column;
    }

    public void setColumnSpan(int _Column) {
        if (_Column < 1) {
            LOGGER.severe("colSpan set to invalid value of " + Integer.toString(_Column) + ". Ignoring.");
            return;
        }
        this._ColumnSpan = _Column;
    }

    public void setDecimalPlaces(int _DecimalPlaces) {
        this._DecimalPlaces = _DecimalPlaces;
    }

    protected void setDefaultIsSquare(boolean _DefaultIsSquare) {
        this._DefaultIsSquare = _DefaultIsSquare;
    }

    public void SetDefaultPeekabooAction(String strDefault) {
        if (null == _DefaultPeekabooAction) {
            LOGGER.config("Setting default peekaboo action to: " + strDefault);
            _DefaultPeekabooAction = strDefault;
        } else {
            LOGGER.warning(" Attempted to se an already set default peekaboo action to: " + strDefault + ". Ignoring.");
        }
    }

    public void SetEnabled(boolean enabled) {

    }

    public void setHeight(double _Height) {
        this._Height = Math.round(_Height);
    }

    public void setHeightPercentOfParentGrid(double _HeightPercentOfParentGrid) {
        this._HeightPercentOfParentGrid = _HeightPercentOfParentGrid;
    }

    protected void setHorizontalPosition(HPos _HorizontalPosition) {
        this._HorizontalPosition = _HorizontalPosition;
    }

    public void setInitiallyEnabled(boolean enabled) {
        _InitiallyEnabled = enabled;
    }

    public void SetInitialValue(String value) {
        LOGGER.warning("Tried to set Initial Value of [" + value + "] for Widget that does not support it");
    }

    public void setMaxSteppedRange(List<Double> newRange) {
        if (!SupportsSteppedRanges()) {
            LOGGER.warning(getName() + " Does not support stepped ranges at this time.  Ignoring");
            return;
        }

        if (null != newRange) {
            _SteppedMaxRanges = newRange;
        } else {
            LOGGER.warning("Provided null list to setMaxSteppedRange");
        }
    }

    public void setMinionID(String _ID) {
        this._MinionID = _ID;
    }

    public void setMinSteppedRange(List<Double> newRange) {
        if (!SupportsSteppedRanges()) {
            LOGGER.warning(getName() + " Does not support stepped ranges at this time.  Ignoring");
            return;
        }
        if (null != newRange) {
            _SteppedMinRanges = newRange;
        } else {
            LOGGER.warning("Provided null list to setMaxSteppedRange");
        }
    }

    public void setMouseHasBeenSetup(boolean _MouseHasBeenSetup) {
        this._MouseHasBeenSetup = _MouseHasBeenSetup;
    }

    public void setNamespace(String _Namespace) {
        this._Namespace = _Namespace;
    }

    protected void SetParent(GridPane _Pane) {
        _WidgetParentPane = _Pane;
    }

    public void setPeekabooHideStr(String _PeekabooHideStr) {
        this._PeekabooHideStr = _PeekabooHideStr;
    }

    public void setPeekabooShowDefault(Boolean _PeekabooShowDefault) {
        this._PeekabooShowDefault = _PeekabooShowDefault;
    }

    public void setPeekabooShowStr(String _PeekabooShowStr) {
        this._PeekabooShowStr = _PeekabooShowStr;
    }

    public void setPosition(Pos newPosition) {
        _Position = newPosition;
    }

    public void setRow(int _Row) {
        this._Row = _Row;
    }

    public void setRowSpan(int _RowSpan) {
        if (_RowSpan < 1) {
            LOGGER.severe("rowSpan set to invalid value of " + Integer.toString(_Row) + ". Ignoring.");
            return;
        }
        this._RowSpan = _RowSpan;
    }

    public void setStyleID(String _StyleID) {
        this._StyleID = _StyleID;
    }

    public void setStyleOverride(List<String> _StyleOverride) {
        this._StyleOverride = _StyleOverride;
    }

    public void setTaskID(String strTaskID) {
        if (strTaskID != null && strTaskID.length() > 0) {
            _TaskID = strTaskID;
        }
    }

    public void setTitle(String title) {
        _Title = title;
    }

    public void SetToolTip(String newValue) {
        _ToolTip = newValue;
    }

    public void setUnitsOverride(String _UnitsOverride) {
        this._UnitsOverride = _UnitsOverride;
    }

    protected void SetupPeekaboo(DataManager dataMgr) {
        if (_Peekaboos.size() < 1) {
            return;
        }

        getStylableObject().setVisible(_PeekabooShowDefault);
        for (Pair<String, String> peekaboo : _Peekaboos) {
            dataMgr.AddListener(peekaboo.getValue(), peekaboo.getKey(), (o, oldVal, newVal) -> {
                String strPeek = newVal.toString();
                HandlePeekabooMessage(strPeek);
            });
        }
    }

    public EventHandler<MouseEvent> SetupTaskAction() {
        if (false == _MouseHasBeenSetup) // quick hack, as I call this from MOST widgets, but now want it from all. Will
        // eventually remove from individual widgets.
        {
            if (null != getTaskID() || CONFIG.isDebugMode()) // only do if a task to setup, or if debug mode
            {
                EventHandler<MouseEvent> eh = event -> {
                    mouseHandler(event);
                    // to make menus and such much zippier
                    CONFIG.requestImmediateRefresh();
                };
                getStylableObject().setOnMouseClicked(eh);
                _MouseHasBeenSetup = true;
                return eh;
            }
        }
        return null;
    }

    public void setValueScale(double _ValueScale) {
        this._ValueScale = _ValueScale;
    }

    protected void setVerticalPosition(VPos _VerticalPosition) {
        this._VerticalPosition = _VerticalPosition;
    }

    public void setWidgetInformation(String DefinintionFileDirectory, String DefinitionFile, String strType) {
        if (null != DefinintionFileDirectory) {
            _DefinintionFileDirectory = DefinintionFileDirectory;
        }
        if (null != DefinitionFile) {
            _DefinitionFile = DefinitionFile;
        }
        if (null != strType) {
            _WidgetType = strType;
        }
    }

    public void setWidth(double _Width) {
        this._Width = Math.round(_Width);
    }

    public void setWidthPercentOfParentGrid(double _WidthPercentOfParentGrid) {
        this._WidthPercentOfParentGrid = _WidthPercentOfParentGrid;
    }

    @Override
    public boolean SupportsEnableDisable() {
        return false;
    }

    @Override
    public boolean SupportsSteppedRanges() {
        return false;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean SingleLine) {
        String strCR = "\n";
        if (true == SingleLine) {
            strCR = " ";
        }
        StringBuilder retStr = new StringBuilder();

        retStr.append(getName());
        if (null != getMinionID()) {
            retStr.append(strCR);
            retStr.append("MinionSrc ID=");
            retStr.append(getMinionID());
            if (null != getNamespace()) {
                retStr.append(" Namespace=");
                retStr.append(getNamespace());
            }
        }
        if (null != getTaskID()) {
            retStr.append(strCR);
            retStr.append("Task ID: ");
            retStr.append(getTaskID());
        }

        retStr.append(strCR);
        retStr.append("Config Size : ");
        retStr.append("[");
        if (CONFIG.getScaleFactor() != 1.0) {
            retStr.append("(");
            retStr.append(Integer.toString((int) _Width));
            retStr.append("x");
            retStr.append(Integer.toString((int) _Height));
            retStr.append(")-> ");
        }

        retStr.append(Integer.toString((int) (getWidth() * CONFIG.getScaleFactor())));
        retStr.append("x");
        retStr.append(Integer.toString((int) (getHeight() * CONFIG.getScaleFactor())));
        retStr.append("]");
        retStr.append(" ");

        // Region objRegion = getRegionObject();
        Dimension visDim = getRealDimensions();

//        if (null != objRegion)
        {
            retStr.append(strCR);
            retStr.append("Actual Size : ");
            retStr.append("[");
            retStr.append(Integer.toString((int) visDim.getWidth()));
            retStr.append("x");
            retStr.append(Integer.toString((int) visDim.getHeight()));
            retStr.append("]");
        }

        return retStr.toString();
    }

    @Override
    public void UpdateValueRange() {
        LOGGER.warning("Tried to perform Peekaboo ValueRange update for widget [" + this.getName()
                + "] that does not support this feature");
    }

    public boolean ZeroDimensionOK() {
        return false;
    }

}
