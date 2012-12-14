package platform.gwt.form.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.KeyboardRowChangedEvent;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.CellPreviewEvent;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GObjectType;
import platform.gwt.form.shared.view.grid.GridEditableCell;

import java.util.*;

import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static platform.gwt.base.shared.GwtSharedUtils.*;

public class GGridTable extends GGridPropertyTable<GridDataRecord> {
    private ArrayList<GPropertyDraw> columnProperties = new ArrayList<GPropertyDraw>();
    private ArrayList<GGroupObjectValue> columnKeysList = new ArrayList<GGroupObjectValue>();
    private Map<GPropertyDraw, HashMap<GGroupObjectValue, GridColumn>> columnsMap = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, GridColumn>>();

    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<GGroupObjectValue>();

    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Set<GPropertyDraw> updatedProperties = new HashSet<GPropertyDraw>();
    private Map<GPropertyDraw, List<GGroupObjectValue>> columnKeys = new HashMap<GPropertyDraw, List<GGroupObjectValue>>();

    private boolean rowsUpdated = false;
    private boolean columnsUpdated = false;
    private boolean dataUpdated = false;

    private final ArrayList<GridDataRecord> currentRecords = new ArrayList<GridDataRecord>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        super(iform);

        this.groupObject = igroupController.groupObject;

        setKeyboardSelectionHandler(new GridTableKeyboardSelectionHandler(this));
        editBindingMap.setKeyAction(new GKeyStroke(GKeyStroke.KEY_F12), GEditBindingMap.GROUP_CHANGE);

        addKeyboardRowChangedHandler(new KeyboardRowChangedEvent.Handler() {
            @Override
            public void onKeyboardRowChanged(KeyboardRowChangedEvent event) {
                final GridDataRecord selectedRecord = getKeyboardSelectedRowValue();
                if (selectedRecord != null && !selectedRecord.getKey().equals(currentKey)) {
                    setCurrentKey(selectedRecord.getKey());

                    form.changeGroupObjectLater(groupObject, selectedRecord.getKey());
                }
            }
        });

        sortableHeaderManager = new GGridSortableHeaderManager<Map<GPropertyDraw, GGroupObjectValue>>(this, false) {
            @Override
            protected void orderChanged(Map<GPropertyDraw, GGroupObjectValue> columnKey, GOrder modiType) {
                form.changePropertyOrder(columnKey.keySet().iterator().next(), columnKey.values().iterator().next(), modiType);
            }

            @Override
            protected Map<GPropertyDraw, GGroupObjectValue> getColumnKey(int column) {
                HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<GPropertyDraw, GGroupObjectValue>();
                key.put(columnProperties.get(column), columnKeysList.get(column));
                return key;
            }
        };
    }

    public void update() {
        storeScrollPosition();

        updatedColumnsImpl();

        updateRowsImpl();

        updateDataImpl();
    }

    private void updateRowsImpl() {
        if (rowsUpdated) {
            int currentSize = currentRecords.size();
            int newSize = rowKeys.size();

            if (currentSize > newSize) {
                for (int i = currentSize - 1; i >= newSize; --i) {
                    currentRecords.remove(i);
                }
            } else if (currentSize < newSize) {
                for (int i = currentSize; i < newSize; ++i) {
                    GGroupObjectValue rowKey = rowKeys.get(i);

                    GridDataRecord record = new GridDataRecord(i, rowKey);
                    record.setRowBackground(rowBackgroundValues.get(rowKey));
                    record.setRowForeground(rowForegroundValues.get(rowKey));

                    currentRecords.add(record);
                }
            }

            for (int i = 0; i < min(newSize, currentSize); ++i) {
                GGroupObjectValue rowKey = rowKeys.get(i);

                GridDataRecord record = currentRecords.get(i);
                record.reinit(rowKey, rowBackgroundValues.get(rowKey), rowForegroundValues.get(rowKey));
            }

            if (currentSize != newSize) {
                setRowData(currentRecords);
            } else {
                redraw();
            }

            if (currentKey != null && rowKeys.contains(currentKey)) {
                setKeyboardSelectedRow(rowKeys.indexOf(currentKey), false);
            }

            rowsUpdated = false;
            dataUpdated = true;
        }
    }

    private void updatedColumnsImpl() {
        if (columnsUpdated) {
            List<String> columnCaptions = new ArrayList<String>();

            columnProperties.clear();
            columnKeysList.clear();

            for (GPropertyDraw property : properties) {
                List<GGroupObjectValue> propertyColumnKeys = columnKeys.get(property);
                if (propertyColumnKeys == null) {
                    propertyColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                }

                Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);

                for (GGroupObjectValue columnKey : propertyColumnKeys) {
                    Object propCaption = null;
                    if (propCaptions == null || (propCaption = propCaptions.get(columnKey)) != null) {
                        columnProperties.add(property);
                        columnKeysList.add(columnKey);
                        if (propCaptions != null) {
                            columnCaptions.add(property.getDynamicCaption(propCaption));
                        } else {
                            columnCaptions.add(property.getCaptionOrEmpty());
                        }
                    }
                }
            }

            Map<GPropertyDraw, HashMap<GGroupObjectValue, GridColumn>> newColumnsMap = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, GridColumn>>();
            for (int i = 0; i < columnProperties.size(); ++i) {
                GPropertyDraw property = columnProperties.get(i);
                GGroupObjectValue columnKey = columnKeysList.get(i);

                GridColumn column = removeFromDoubleMap(columnsMap, property, columnKey);
                if (column != null) {
                    moveGridColumn(column, i);
                } else {
                    column = insertGridColumn(i);
                    setColumnWidth(column, property.getMinimumWidth());
                }

                GGridPropertyTableHeader header = headers.get(i);
                header.setCaption(columnCaptions.get(i));

                putToDoubleMap(newColumnsMap, property, columnKey, column);
            }

            for (Map<GGroupObjectValue, GridColumn> columnsCollection : columnsMap.values()) {
                for (GridColumn column : columnsCollection.values()) {
                    removeGridColumn(column);
                }
            }

            columnsMap = newColumnsMap;

            refreshHeaders();

            columnsUpdated = false;
            dataUpdated = true;
        }
    }

    private void updateDataImpl() {
        if (dataUpdated) {
            Set<Column> updatedColumns = new HashSet<Column>();
            for (GridDataRecord record : currentRecords) {
                GGroupObjectValue rowKey = record.getKey();
                for (GPropertyDraw property : updatedProperties) {
                    List<GGroupObjectValue> propColumnKeys = columnKeys.get(property);
                    if (propColumnKeys == null) {
                        propColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                    }
                    updateDI1(updatedColumns, record, rowKey, property, propColumnKeys);
                }
            }

            redrawColumns(updatedColumns);

            updatedProperties.clear();
            dataUpdated = false;
        }
    }

    private void updateDI1(Set<Column> updatedColumns, GridDataRecord record, GGroupObjectValue rowKey, GPropertyDraw property, List<GGroupObjectValue> propColumnKeys) {
        for (GGroupObjectValue columnKey : propColumnKeys) {
            HashMap<GGroupObjectValue, GridColumn> propertyColumns = columnsMap.get(property);
            GridColumn column = propertyColumns == null ? null : propertyColumns.get(columnKey);
            // column == null, когда свойство скрыто через showif
            if (column != null) {
                updatedColumns.add(column);

                GGroupObjectValue fullKey = columnKey.isEmpty() ? rowKey : new GGroupObjectValue(rowKey, columnKey);

                Object value = values.get(property).get(fullKey);
                Object background = cellBackgroundValues.containsKey(property) ? cellBackgroundValues.get(property).get(fullKey) : null;
                Object foreground = cellForegroundValues.containsKey(property) ? cellForegroundValues.get(property).get(fullKey) : null;

                record.setValue(column.columnID, value);
                record.setBackground(column.columnID, background);
                record.setForeground(column.columnID, foreground);
            }
        }
    }

    private GridColumn insertGridColumn(int index) {
        GridColumn column = new GridColumn();
        GGridPropertyTableHeader header = new GGridPropertyTableHeader(this);

        headers.add(index, header);

        insertColumn(index, column, header);

        return column;
    }

    private void moveGridColumn(GridColumn column, int newIndex) {
        int oldIndex = getColumnIndex(column);
        if (oldIndex != newIndex) {
            GGridPropertyTableHeader header = headers.remove(oldIndex);
            headers.add(newIndex, header);

            moveColumn(oldIndex, newIndex);
        }
    }

    private void removeGridColumn(GridColumn column) {
        GGridPropertyTableHeader header = (GGridPropertyTableHeader) getHeader(getColumnIndex(column));
        headers.remove(header);
        removeColumn(column);
    }

    public boolean isEmpty() {
        return values.isEmpty() || properties.isEmpty();
    }

    public GGroupObjectValue getCurrentKey() {
        return currentKey;
    }

    public GPropertyDraw getCurrentProperty() {
        GPropertyDraw property = getSelectedProperty();
        if (property == null && getColumnCount() > 0) {
            property = getProperty(0);
        }
        return property;
    }

    public Object getSelectedValue(GPropertyDraw property) {
        GridDataRecord selectedRecord = getKeyboardSelectedRowValue();
        if (selectedRecord != null) {
            return getColumn(getMinPropertyIndex(property)).getValue(selectedRecord);
        }

        return null;
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        Log.debug("Setting current object to: " + currentKey);
        this.currentKey = currentKey;
    }

    public void removeProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            return;
        }

        values.remove(property);
        properties.remove(property);

        columnsUpdated = true;
    }

    public void addProperty(final GPropertyDraw property) {
        if (properties.contains(property)) {
            return;
        }

        int newColumnIndex = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
        properties.add(newColumnIndex, property);

        columnsUpdated = true;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.rowKeys = keys;

        needToRestoreScrollPosition = true;
        rowsUpdated = true;
    }

    public void updatePropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            if (updateKeys) {
                propValues = override(values.get(property), propValues);
            }
            values.put(property, propValues);
            updatedProperties.add(property);

            dataUpdated = true;
        }
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        if (columnKeys != null) {
            this.columnKeys.put(property, columnKeys);
            columnsUpdated = true;
        }
    }

    @Override
    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updatePropertyCaptions(propertyDraw, values);
        columnsUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        updatedProperties.add(propertyDraw);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        updatedProperties.add(propertyDraw);
        dataUpdated = true;
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        super.updateRowBackgroundValues(values);
        rowsUpdated = true;
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        super.updateRowForegroundValues(values);
        rowsUpdated = true;
    }

    public GPropertyDraw getProperty(int column) {
        return columnProperties.get(column);
    }

    public GPropertyDraw getProperty(Cell.Context context) {
        return getProperty(context.getColumn());
    }

    private int getMinPropertyIndex(GPropertyDraw property) {
        for (int i = 0; i < columnProperties.size(); ++i) {
            if (property == columnProperties.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public void modifyGroupObject(GGroupObjectValue rowKey, boolean add) {
        if (add) {
            rowKeys.add(rowKey);
            setCurrentKey(rowKey);
        } else {
            if (currentKey.equals(rowKey) && rowKeys.size() > 0) {
                if (rowKeys.size() == 1) {
                    setCurrentKey(null);
                } else {
                    int index = rowKeys.indexOf(rowKey);
                    index = index == rowKeys.size() - 1 ? index - 1 : index + 1;
                    setCurrentKey(rowKeys.get(index));
                }
            }
            rowKeys.remove(rowKey);
        }
        dataUpdated = true;

        update();
    }

    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        int ind = getMinPropertyIndex(property);
        HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<GPropertyDraw, GGroupObjectValue>();
        key.put(property, ind == -1 ? GGroupObjectValue.EMPTY : columnKeys.get(property).get(ind));
        sortableHeaderManager.changeOrder(key, modiType);
    }

    @Override
    public GGroupObjectValue getColumnKey(Cell.Context context) {
        return columnKeysList.get(context.getColumn());
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        GPropertyDraw property = getProperty(context);
        return property != null && !property.isReadOnly() && !(property.baseType instanceof GObjectType);
    }

    public Object getValueAt(Cell.Context context) {
        Column column = getColumn(context.getColumn());
        return column.getValue(context.getRowValue());
    }

    public void setValueAt(Cell.Context context, Object value) {
        GridDataRecord rowRecord = (GridDataRecord) context.getRowValue();
        GridColumn column = (GridColumn) getColumn(context.getColumn());

        rowRecord.setValue(column.columnID, value);

        setRowValue(context.getIndex(), rowRecord);
        redrawColumns(singleton(column), false);
    }

    private int nextColumnID = 0;
    private class GridColumn extends Column<GridDataRecord, Object> {
        private int columnID;

        public GridColumn() {
            super(new GridEditableCell(GGridTable.this));
            this.columnID = nextColumnID++;
        }

        @Override
        public Object getValue(GridDataRecord record) {
            return record.getValue(columnID);
        }
    }

    public class GridTableKeyboardSelectionHandler extends GridPropertyTableKeyboardSelectionHandler<GridDataRecord> {
        public GridTableKeyboardSelectionHandler(DataGrid<GridDataRecord> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(CellPreviewEvent<GridDataRecord> event) {
            NativeEvent nativeEvent = event.getNativeEvent();

            assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

            int keyCode = nativeEvent.getKeyCode();
            boolean ctrlPressed = nativeEvent.getCtrlKey();
            if (keyCode == KeyCodes.KEY_HOME && ctrlPressed) {
                form.scrollToEnd(groupObject, false);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && ctrlPressed) {
                form.scrollToEnd(groupObject, true);
                return true;
            }

            return super.handleKeyEvent(event);
        }
    }
}
