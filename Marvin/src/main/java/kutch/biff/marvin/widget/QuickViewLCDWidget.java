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
package kutch.biff.marvin.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.IQuickViewSort;
import kutch.biff.marvin.task.QuickViewWidgetSortTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.NaturalComparator;

/**
 *
 * @author Patrick Kutch
 */
public class QuickViewLCDWidget extends GridWidget implements IQuickViewSort
{
    
    public enum SortMode
    {
	
	None, Ascending, Descending
    };
    
    private SortMode _SortMode = SortMode.Descending;
    private int _RowWidth = 5;
    private String _EvenBackgroundStyle = "-fx-background-color:green";
    private String _EvenStyle;
    private String _EvenStyleID = "";
    
    private String _OddBackgroundStyle = "fx-background-color:grey";
    private String _OddStyle;
    private String _OddStyleID = "";
    
//    private GridWidget _GridWidget;
    private List<Pair<String, SteelLCDWidget>> _DataPoint; // Minion ID, LCDWidget
    private DataManager _dataMgr;
    // private int _hGap, _vGap;
    private HashMap<String, String> _ExclusionList;
    private HashMap<String, String> _DataPointMap;
    private AtomicInteger _SortCount;
    
    public QuickViewLCDWidget()
    {
	_DataPoint = new ArrayList<>();
//        _hGap = -1;
//        _vGap = -1;
	_ExclusionList = new HashMap<>(); // For those we do not want to show
	_DataPointMap = new HashMap<>(); // for quick lookup as new data comes in
	_SortCount = new AtomicInteger();
    }
    
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
	SetParent(pane);
	_dataMgr = dataMgr;
	
	getGridPane().setAlignment(getPosition());
	pane.add(getGridPane(), getColumn(), getRow(), getColumnSpan(), getRowSpan());
	
	if (gethGap() > -1)
	{
	    getGridPane().setHgap(gethGap());
	}
	if (getvGap() > -1)
	{
	    getGridPane().setVgap(getvGap());
	}
	
	dataMgr.AddWildcardListener(getMinionID(), getNamespace(), new ChangeListener<Object>()
	{
	    @Override
	    public void changed(ObservableValue<?> o, Object oldVal, Object newVal)
	    {
		if (IsPaused())
		{
		    return;
		}
		String strVal = newVal.toString();
		String[] parts = strVal.split(":");
		if (parts.length > 1)
		{
		    String ID = parts[0];
		    String Value = parts[1];
		    if (_DataPointMap.containsKey(ID.toLowerCase()))
		    {
			return; // already made one for this!
		    }
		    if (_ExclusionList.containsKey(ID.toLowerCase()))
		    {
			return; // not wanted
		    }
		    _DataPointMap.put(ID.toLowerCase(), ID); // add the key to the map don't care about the stored
							     // value, just the key
		    CreateDataWidget(ID, Value); // didn't find it, so go make one
		}
	    }
	});
	SetupPeekaboo(dataMgr);
	SetupTaskAction();
	
	return true;
    }
    
    private SteelLCDWidget CreateDataWidget(String ID, String initialVal)
    {
	SteelLCDWidget objWidget = new SteelLCDWidget();
	
	objWidget.setTextMode(false);
	objWidget.setMinionID(ID);
	objWidget.setNamespace(getNamespace());
	objWidget.setTitle(ID);
	objWidget.SetValue(initialVal);
	objWidget.setRow(0);
	objWidget.setColumn(0);
	objWidget.setDecimalPlaces(2);
	objWidget.setMaxValue(999999999);
	
	objWidget.setWidgetInformation(getDefinintionFileDirectory(), "", "huh");
	
	objWidget.getStylableObject().setVisible(false); // becomes visible when sorted
	objWidget.setWidth(getWidth());
	objWidget.setHeight(getHeight());
	
	objWidget.Create(getGridPane(), _dataMgr);
	objWidget.PerformPostCreateActions(getParentGridWidget(), false);
	
	_DataPoint.add(new Pair<>(ID.toUpperCase(), objWidget));
	SetupSort();
	
	return objWidget;
    }
    
    @Override
    public String[] GetCustomAttributes()
    {
	String[] Attributes = { "hgap", "vgap" };
	return Attributes;
    }
    
    public String getEvenBackgroundStyle()
    {
	return _EvenBackgroundStyle;
    }
    
    public String getEvenStyle()
    {
	return _EvenStyle;
    }
    
    public String getEvenStyleID()
    {
	return _EvenStyleID;
    }
    
    public String getOddEvenBackgroundStyle()
    {
	return _OddBackgroundStyle;
    }
    
    public String getOddStyle()
    {
	return _OddStyle;
    }
    
    public String getOddStyleID()
    {
	return _OddStyleID;
    }
    
    // @Override
//    public ObservableList<String> getStylesheets()
//    {
//        return _GridWidget.getStylesheets();
//    }
//
//    @Override
//    public Node getStylableObject()
//    {
//        return _GridWidget.getStylableObject();
//    }
    public int getRowWidth()
    {
	return _RowWidth;
    }
    
    public int getSortCount()
    {
	synchronized (this)
	{
	    return _SortCount.get();
	}
    }
    
    public SortMode getSortMode()
    {
	return _SortMode;
    }
    
    @Override
    public void HandleWidgetSpecificAttributes(FrameworkNode widgetNode)
    {
	if (widgetNode.hasAttribute("hgap"))
	{
	    try
	    {
		sethGap(Integer.parseInt(widgetNode.getAttribute("hgap")));
		LOGGER.config("Setting hGap for QuickViewWidget :" + widgetNode.getAttribute("hgap"));
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.warning("hgap for QuickViewWidget invalid: " + widgetNode.getAttribute("hgap") + ".  Ignoring");
	    }
	}
	if (widgetNode.hasAttribute("vgap"))
	{
	    try
	    {
		setvGap(Integer.parseInt(widgetNode.getAttribute("vgap")));
		LOGGER.config("Setting vGap for QuickViewWidget :" + widgetNode.getAttribute("vgap"));
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.warning("vgap for QuickViewWidget invalid: " + widgetNode.getAttribute("vgap") + ".  Ignoring");
	    }
	}
	if (true == widgetNode.hasAttribute("Align"))
	{
	    String str = widgetNode.getAttribute("Align");
	    setAlignment(str);
	}
	
    }
    
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
	if (node.getNodeName().equalsIgnoreCase("RowWidth"))
	{
	    String str = node.getTextContent();
	    try
	    {
		setRowWidth(Integer.parseInt(str));
		return true;
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.severe("Invalid <RowWidth> in QuickViewLCDWidget Widget Definition File : " + str);
	    }
	}
	else if (node.getNodeName().equalsIgnoreCase("EvenBackgroundStyle"))
	{
	    setEvenBackgroundStyle(node.getTextContent());
	    return true;
	}
	else if (node.getNodeName().equalsIgnoreCase("EvenStyle"))
	{
	    String ID = "";
	    if (node.hasAttribute("ID"))
	    {
		ID = node.getAttribute("ID");
	    }
	    setEvenStyle(ID, node.getTextContent());
	    return true;
	}
	
	else if (node.getNodeName().equalsIgnoreCase("OddBackgroundStyle"))
	{
	    setOddBackgroundStyle(node.getTextContent());
	    return true;
	}
	else if (node.getNodeName().equalsIgnoreCase("OddStyle"))
	{
	    String ID = "";
	    if (node.hasAttribute("ID"))
	    {
		ID = node.getAttribute("ID");
	    }
	    setOddStyle(ID, node.getTextContent());
	    return true;
	}
	else if (node.getNodeName().equalsIgnoreCase("Order"))
	{
	    String strVal = node.getTextContent();
	    if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Ascending.toString()))
	    {
		setSortMode(QuickViewLCDWidget.SortMode.Ascending);
	    }
	    else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.Descending.toString()))
	    {
		setSortMode(QuickViewLCDWidget.SortMode.Descending);
	    }
	    else if (strVal.equalsIgnoreCase(QuickViewWidget.SortMode.None.toString()))
	    {
		setSortMode(QuickViewLCDWidget.SortMode.None);
	    }
	    else
	    {
		LOGGER.warning("Invalid <Order> Tag in QuickViewLCDWidget Widget File. " + strVal);
	    }
	    return true;
	}
	else if (node.getNodeName().equalsIgnoreCase("ExcludeList"))
	{
	    String strVal = node.getTextContent();
	    for (String exVal : strVal.split(":"))
	    {
		String strClean = exVal.replaceAll("\\s+", ""); // get rid of invalid chars
		LOGGER.info("Addeding QuickViewWidget Exclusion: " + strClean);
		_ExclusionList.put(strClean.toLowerCase(), "Not Needed");
	    }
	    for (String exVal : strVal.split(";"))
	    {
		String strClean = exVal.replaceAll("\\s+", ""); // get rid of invalid chars
		LOGGER.info("Addeding QuickViewWidget Exclusion: " + strClean);
		_ExclusionList.put(strClean.toLowerCase(), "Not Needed");
	    }
	    return true;
	}
	
	return false;
    }
    
    private int incrementSortCount()
    {
	synchronized (this)
	{
	    return _SortCount.incrementAndGet();
	}
    }
    
    @Override
    public void PerformSort()
    {
	if (getSortCount() > 0)
	{
	    setIncrementSortCount(0);
	    Sort();
	}
    }
    
    public void setEvenBackgroundStyle(String _EvenBackgroundStyle)
    {
	this._EvenBackgroundStyle = _EvenBackgroundStyle;
    }
    
    public void setEvenStyle(String ID, String File)
    {
	_EvenStyleID = ID;
	_EvenStyle = File;
    }
    
    private void setIncrementSortCount(int newVal)
    {
	synchronized (this)
	{
	    _SortCount.set(newVal);
	}
    }
    
    public void setOddBackgroundStyle(String _OddEvenBackgroundStyle)
    {
	this._OddBackgroundStyle = _OddEvenBackgroundStyle;
    }
    
    public void setOddStyle(String ID, String File)
    {
	_OddStyleID = ID;
	_OddStyle = File;
    }
    
    public void setRowWidth(int _RowWidth)
    {
	this._RowWidth = _RowWidth;
    }
    
    public void setSortMode(SortMode _SortMode)
    {
	this._SortMode = _SortMode;
    }
    
    private void SetStyle(boolean Odd, SteelLCDWidget objWidget)
    {
	if (Odd)
	{
	    objWidget.setBaseCSSFilename(_OddStyle);
	    objWidget.setStyleID(_OddStyleID);
	}
	else
	{
	    objWidget.setBaseCSSFilename(_EvenStyle);
	    objWidget.setStyleID(_EvenStyleID);
	}
	objWidget.ApplyCSS();
	objWidget.ApplyOverrides();
	
    }
    
    private void SetupSort() // create a deferred task to do the actual sorting, otherwise when you have 5000
			     // datapoints it overlaods the gui
    {
	if (incrementSortCount() < 5) // don't need to stack them on if already in the queue
	{
	    QuickViewWidgetSortTask objTask = new QuickViewWidgetSortTask(this);
	    TaskManager.getTaskManager().AddPostponedTask(objTask, 500); // just every .5 secs do a sort
	}
    }
    
    /**
     * Go through and sort the goodies alphabetically
     */
    private void Sort()
    {
	if (getSortMode() == SortMode.Ascending)
	{
	    Collections.sort(_DataPoint, new Comparator<Pair<String, SteelLCDWidget>>()
	    {
		@Override
		public int compare(Pair<String, SteelLCDWidget> s1, Pair<String, SteelLCDWidget> s2) // do alphabetical
												     // sort
		{
		    NaturalComparator naturalCompare = new NaturalComparator();
		    return naturalCompare.compare(s1.getKey(), s2.getKey());
		}
	    });
	}
	else if (getSortMode() == SortMode.Descending)
	{
	    Collections.sort(_DataPoint, new Comparator<Pair<String, SteelLCDWidget>>()
	    {
		@Override
		public int compare(Pair<String, SteelLCDWidget> s1, Pair<String, SteelLCDWidget> s2) // do alphabetical
												     // sort
		{
		    NaturalComparator naturalCompare = new NaturalComparator();
		    return naturalCompare.compare(s2.getKey(), s1.getKey());
		}
	    });
	}
	
	int row = 0;
	int column = 0;
	boolean odd = true;
	
	getGridPane().getChildren().clear();
	
	for (Pair<String, SteelLCDWidget> pair : _DataPoint)
	{
	    SteelLCDWidget objLCD = pair.getValue();
	    
	    objLCD.setRow(row);
	    objLCD.setColumn(column);
	    
	    // it's not getting added to the right place, its not the right grid...'
	    getGridPane().add(objLCD.getStylableObject(), column, row);
	    
	    SetStyle(odd, objLCD);
	    odd = !odd;
	    objLCD.getStylableObject().setVisible(true);
	    
	    if (++column >= getRowWidth())
	    {
		row++;
		column = 0;
	    }
	}
    }
    
}
