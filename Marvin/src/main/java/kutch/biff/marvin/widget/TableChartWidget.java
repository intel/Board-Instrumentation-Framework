/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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

import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.datamanager.MarvinChangeListener;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.utility.Utility;
import kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder;

/**
 *
 * @author Patrick Kutch
 */
public class TableChartWidget extends BaseWidget
{
    private class TableColumnClass
    {
	private String _text;
	private int _Width;
	private List<TableColumnClass> _subColumn;
	
	public TableColumnClass(String text)
	{
	    _text = text;
	    _subColumn = new ArrayList<TableColumnClass>();
	    _Width = -1;
	}
	
	public void addSubColumn(TableColumnClass subCol)
	{
	    _subColumn.add(subCol);
	}
	
	public String getText()
	{
	    return _text;
	}
	
	public List<TableColumnClass> getSubColumns()
	{
	    return _subColumn;
	}
	
	public int getWidth()
	{
	    return _Width;
	}
	
	public void setWidth(int newWidth)
	{
	    _Width = newWidth;
	}
    }
    
    private class TableCellClass
    {
	private String _ID, _Namespace;
	private StringProperty _cellData;
	private ValueRange __dataIndexRange;
	private String __dataIndexToken;
	
	public TableCellClass(String ID, String namespace, String initialValue) throws IllegalArgumentException
	{
	    if (null == initialValue)
	    {
		if (null == namespace)
		{
		    LOGGER.severe("No Namespace specified for TableChart Row");
		    throw new IllegalArgumentException("No Namespace specified for TableChart Row");
		}
		if (null == ID)
		{
		    LOGGER.severe("No ID specified for TableChart Row");
		    throw new IllegalArgumentException("No ID specified for TableChart Row");
		}
		initialValue = "";
	    }
	    
	    _ID = ID;
	    _Namespace = namespace;
	    _cellData = new SimpleStringProperty(initialValue);
	    __dataIndexRange = ValueRange.of(-1, -1);
	    __dataIndexToken = ",";
	    
	}
	
	StringProperty getProperty()
	{
	    return _cellData;
	}
	
	public void set__dataIndex(ValueRange __dataIndex)
	{
	    this.__dataIndexRange = __dataIndex;
	}
	
	public void set__dataIndexToken(String __dataIndexToken)
	{
	    this.__dataIndexToken = __dataIndexToken;
	}
	
	public String get__dataIndexToken()
	{
	    return __dataIndexToken;
	}
	
	public ValueRange get__dataIndex()
	{
	    return __dataIndexRange;
	}
	
	public void setupListener(DataManager dataMgr)
	{
	    if (null == _ID || null == _Namespace)
	    {
		return;
	    }
	    // dataMgr.AddListener(_ID, _Namespace, new ChangeListener<Object>()
	    dataMgr.AddListener(_ID, _Namespace, new MarvinChangeListener(get__dataIndex(), get__dataIndexToken())
	    {
		@Override
		public void onChanged(String newValue)
		{
		    _cellData.set(newValue);
		    
		}
	    });
	    
	}
    }
    
    private TableView<ObservableList<StringProperty>> _table;
    private TableColumnClass _columns;
    private String columnSrcID, columnSrcNamespace;
    private List<List<TableCellClass>> _rows;
    
    public TableChartWidget()
    {
	_table = new TableView<>();
	_columns = new TableColumnClass("Master");
	_rows = new ArrayList<>();
	columnSrcID = null;
	columnSrcNamespace = null;
    }
    
    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
	if (!SetupTable(dataMgr))
	{
	    return false;
	}
	ConfigureDimentions();
	
	ConfigureAlignment();
	SetupPeekaboo(dataMgr);
	
	pane.add(_table, getColumn(), getRow(), getColumnSpan(), getRowSpan());
	setupRowListeners(dataMgr);
	/*
	 * dataMgr.AddListener(getMinionID(), getNamespace(), new
	 * ChangeListener<Object>() {
	 * 
	 * @Override public void changed(ObservableValue<?> o, Object oldVal, Object
	 * newVal) { //onChange(o, oldVal, newVal); } });
	 */
	SetupTaskAction();
	return ApplyCSS();
    }
    
    @Override
    public Node getStylableObject()
    {
	// TODO Auto-generated method stub
	return _table;
    }
    
    @Override
    public ObservableList<String> getStylesheets()
    {
	// TODO Auto-generated method stub
	return _table.getStylesheets();
    }
    
    @Override
    public void UpdateTitle(String newTitle)
    {
	// TODO Auto-generated method stub
    }
    
    protected boolean SetupTable(DataManager dataMgr)
    {
	if (_columns.getSubColumns().size() < 1)
	{
	    if (columnSrcID == null && null == columnSrcNamespace)
	    {
		LOGGER.severe("TableChart has no columns");
		return false;
	    }
	}
	int index = 0;
	for (TableColumnClass col : _columns.getSubColumns())
	{
	    TableColumn<ObservableList<StringProperty>, String> objCol;
	    objCol = createColumn(index, col.getText());
	    if (col.getWidth() > 0)
	    {
		objCol.setMinWidth(col.getWidth());
	    }
	    for (TableColumnClass subCol : col.getSubColumns())
	    {
		TableColumn<ObservableList<StringProperty>, String> subSubCol;
		subSubCol = createColumn(index++, subCol.getText());
		if (subCol.getWidth() > 0)
		{
		    subSubCol.setMinWidth(subCol.getWidth());
		}
		
		objCol.getColumns().add(subSubCol);
	    }
	    
	    _table.getColumns().add(objCol);
	    
	    index += 1;
	}
	
	for (List<TableCellClass> row : _rows)
	{
	    if (row.size() != index)
	    {
		LOGGER.severe("Number of CELLS in TableChart Row does match the # of columns");
		return false;
	    }
	    ObservableList<StringProperty> rowCells = FXCollections.observableArrayList();
	    for (TableCellClass cell : row)
	    {
		rowCells.add(cell.getProperty());
	    }
	    _table.getItems().add(rowCells);
	}
	
	return true;
    }
    
    private TableColumnClass readColumn(FrameworkNode colNode)
    {
	TableColumnClass objColumn = null;
	Utility.ValidateAttributes(new String[] { "Text", "Width" }, colNode);
	
	if (colNode.hasAttribute("Text"))
	{
	    objColumn = new TableColumnClass(colNode.getAttribute("Text"));
	    for (FrameworkNode subNode : colNode.getChildNodes())
	    {
		if (subNode.getNodeName().equalsIgnoreCase("Column"))
		{
		    TableColumnClass objSubCol = readColumn(subNode);
		    if (null != objSubCol)
		    {
			objColumn.addSubColumn(objSubCol);
		    }
		    else
		    {
			return null;
		    }
		}
	    }
	}
	if (colNode.hasAttribute("Width"))
	{
	    int width = colNode.getIntegerAttribute("Width", -1);
	    objColumn.setWidth(width);
	}
	
	return objColumn;
    }
    
    private boolean readColumns(FrameworkNode columnNode)
    {
	for (FrameworkNode colNode : columnNode.getChildNodes())
	{
	    if (colNode.getNodeName().equalsIgnoreCase("Column"))
	    {
		TableColumnClass column = readColumn(colNode);
		
		if (null != column)
		{
		    _columns.addSubColumn(column);
		}
		else
		{
		    return false;
		}
	    }
	}
	
	return true;
    }
    
    private void setupRowListeners(DataManager dataMgr)
    {
	for (List<TableCellClass> row : _rows)
	{
	    for (TableCellClass cell : row)
	    {
		cell.setupListener(dataMgr);
	    }
	}
    }
    
    private boolean readRows(FrameworkNode rowMasterNode)
    {
	for (FrameworkNode rowNode : rowMasterNode.getChildNodes())
	{
	    if (rowNode.getNodeName().equalsIgnoreCase("Row"))
	    {
		List<TableCellClass> columnsInRow = new ArrayList<>();
		_rows.add(columnsInRow);
		for (FrameworkNode columnNode : rowNode.getChildNodes())
		{
		    
		    Utility.ValidateAttributes(new String[] { "ID", "Namespace","DataIndex","Separator" }, columnNode);
		    try
		    {
			TableCellClass cell = new TableCellClass(columnNode.getAttribute("ID"),
				columnNode.getAttribute("Namespace"), columnNode.getTextContent());
			columnsInRow.add(cell);
			Pair<ValueRange, String> indexInfo = WidgetBuilder.ReadMinionSrcIndexInfo(columnNode);
			cell.set__dataIndex(indexInfo.getKey());
			cell.set__dataIndexToken(indexInfo.getValue());
		    }
		    catch(IllegalArgumentException ex)
		    {
			return false;
		    }
		}
		
	    }
	}
	return true;
    }
    
    private TableColumn<ObservableList<StringProperty>, String> createColumn(final int columnIndex, String columnTitle)
    {
	TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
	String title;
	if (columnTitle == null || columnTitle.trim().length() == 0)
	{
	    title = "Column " + (columnIndex + 1);
	}
	else
	{
	    title = columnTitle;
	}
	column.setText(title);
	column.setCellValueFactory(
		new Callback<TableColumn.CellDataFeatures<ObservableList<StringProperty>, String>, ObservableValue<String>>()
		{
		    @Override
		    public ObservableValue<String> call(
			    CellDataFeatures<ObservableList<StringProperty>, String> cellDataFeatures)
		    {
			ObservableList<StringProperty> values = cellDataFeatures.getValue();
			if (columnIndex >= values.size())
			{
			    return new SimpleStringProperty("");
			}
			else
			{
			    return cellDataFeatures.getValue().get(columnIndex);
			}
		    }
		});
	return column;
    }
    
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode widgetNode)
    {
	if (widgetNode.getNodeName().equalsIgnoreCase("Columns"))
	{
	    return readColumns(widgetNode);
	}
	else if (widgetNode.getNodeName().equalsIgnoreCase("Rows"))
	{
	    return readRows(widgetNode);
	}
	
	return false;
    }
    
}
