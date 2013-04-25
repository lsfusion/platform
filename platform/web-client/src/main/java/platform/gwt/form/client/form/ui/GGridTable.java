package platform.gwt.form.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import platform.gwt.base.client.jsni.Function;
import platform.gwt.base.client.jsni.NativeHashMap;
import platform.gwt.base.client.ui.DialogBoxHelper;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.KeyboardRowChangedEvent;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.CellPreviewEvent;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.GGroupObjectValueBuilder;
import platform.gwt.form.shared.view.classes.GObjectType;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.GridEditableCell;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static platform.gwt.base.shared.GwtSharedUtils.nullEquals;

public class GGridTable extends GGridPropertyTable<GridDataRecord> {
    private ArrayList<GPropertyDraw> columnProperties = new ArrayList<GPropertyDraw>();
    private ArrayList<GGroupObjectValue> columnKeysList = new ArrayList<GGroupObjectValue>();

    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>>();

    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<GGroupObjectValue>();

    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> values = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> readOnlyValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private NativeHashMap<GPropertyDraw, Boolean> updatedProperties = new NativeHashMap<GPropertyDraw, Boolean>();
    private NativeHashMap<GPropertyDraw, List<GGroupObjectValue>> columnKeys = new NativeHashMap<GPropertyDraw, List<GGroupObjectValue>>();

    private boolean rowsUpdated = false;
    private boolean columnsUpdated = false;
    private boolean dataUpdated = false;

    private final ArrayList<GridDataRecord> currentRecords = new ArrayList<GridDataRecord>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;
    private GGridController gridController;

    private GridTableKeyboardSelectionHandler keyboardSelectionHandler;

    private GGroupObjectController groupObjectController;

    private int pageSize = 50;

    public GGridTable(GFormController iform, GGroupObjectController igroupController, GGridController gridController) {
        super(iform);

        this.groupObjectController = igroupController;
        this.groupObject = igroupController.groupObject;
        this.gridController = gridController;

        keyboardSelectionHandler =  new GridTableKeyboardSelectionHandler(this);
        setKeyboardSelectionHandler(keyboardSelectionHandler);
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

        getTableDataScroller().addScrollHandler(new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {
                int selectedRow = getKeyboardSelectedRow();
                GridDataRecord selectedRecord = getKeyboardSelectedRowValue();
                if (selectedRecord != null) {
                    TableRowElement childElement = getChildElement(selectedRow);
                    if (childElement != null) {
                        int oldRowScrollTop = childElement.getAbsoluteTop() - getTableDataScroller().getAbsoluteTop();
                        int scrollerHeight = getTableDataScroller().getClientHeight();
                        if (oldRowScrollTop < 0) {
                            int newRow = selectedRow + Math.abs(oldRowScrollTop / getRowHeight());
                            if (selectedRow == newRow) {
                                setKeyboardSelectedRow(++newRow, false);
                                setDesiredVerticalScrollPosition(getTableDataScroller().getVerticalScrollPosition() + (getRowHeight() + oldRowScrollTop));
                            } else {
                                setKeyboardSelectedRow(newRow, false);
                            }
                        } else if (oldRowScrollTop + getRowHeight() > scrollerHeight) {
                            int newRow = selectedRow - (oldRowScrollTop - scrollerHeight) / getRowHeight() - 1;
                            setKeyboardSelectedRow(newRow, false);

                            if (oldRowScrollTop < scrollerHeight) {
                                setDesiredVerticalScrollPosition(getTableDataScroller().getVerticalScrollPosition() - (scrollerHeight - oldRowScrollTop));
                            }
                        }
                    }
                }
            }
        });

        getElement().setPropertyObject("groupObject", groupObject);
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

            int rowHeight = 0;
            NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> newColumnsMap = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>>();
            for (int i = 0; i < columnProperties.size(); ++i) {
                GPropertyDraw property = columnProperties.get(i);
                GGroupObjectValue columnKey = columnKeysList.get(i);

                GridColumn column = removeFromColumnsMap(columnsMap, property, columnKey);
                if (column != null) {
                    moveGridColumn(column, i);
                } else {
                    column = insertGridColumn(i);
                    setColumnWidth(column, property.getMinimumWidth());
                    // если колонка появилась через showif без обновления данных
                    if (!updatedProperties.containsKey(property)) {
                        updatedProperties.put(property, TRUE);
                        dataUpdated = true; // если кроме появления этой колонки в гриде ничего не поменялось, всё равно нужно обновить данные и подсветки
                    }
                }

                GGridPropertyTableHeader header = headers.get(i);
                header.setCaption(columnCaptions.get(i));

                putToColumnsMap(newColumnsMap, property, columnKey, column);

                rowHeight = Math.max(rowHeight, property.getMinimumPixelHeight());
            }
            setCellHeight(rowHeight);

            columnsMap.foreachValue(new Function<NativeHashMap<GGroupObjectValue, GridColumn>>() {
                @Override
                public void apply(NativeHashMap<GGroupObjectValue, GridColumn> columnsCollection) {
                    columnsCollection.foreachValue(new Function<GridColumn>() {
                        @Override
                        public void apply(GridColumn column) {
                            removeGridColumn(column);
                        }
                    });
                }
            });

            columnsMap = newColumnsMap;

            refreshHeaders();

            gridController.setForceHidden(columnProperties.isEmpty());

            columnsUpdated = false;
        }
    }

    public static void putToColumnsMap(NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column, GridColumn value) {
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap == null) {
            columnsMap.put(row, rowMap = new NativeHashMap<GGroupObjectValue, GridColumn>());
        }
        rowMap.put(column, value);
    }

    public static GridColumn removeFromColumnsMap(NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column) {
        GridColumn result = null;
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap != null) {
            result = rowMap.remove(column);
        }
        return result;
    }

    private void updateDataImpl() {
        if (dataUpdated) {
            final Set<Column> updatedColumns = new HashSet<Column>();
            for (final GridDataRecord record : currentRecords) {
                final GGroupObjectValue rowKey = record.getKey();
                updatedProperties.foreachKey(new Function<GPropertyDraw>() {
                    @Override
                    public void apply(GPropertyDraw property) {
                        List<GGroupObjectValue> propColumnKeys = columnKeys.get(property);
                        if (propColumnKeys == null) {
                            propColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                        }
                        NativeHashMap<GGroupObjectValue, Object> propValues = values.get(property);
                        Map<GGroupObjectValue, Object> propReadOnly = readOnlyValues.get(property);
                        Map<GGroupObjectValue, Object> propertyBackgrounds = cellBackgroundValues.get(property);
                        Map<GGroupObjectValue, Object> propertyForegrounds = cellForegroundValues.get(property);
                        for (GGroupObjectValue columnKey : propColumnKeys) {
                            NativeHashMap<GGroupObjectValue, GridColumn> propertyColumns = columnsMap.get(property);
                            GridColumn column = propertyColumns == null ? null : propertyColumns.get(columnKey);
                            // column == null, когда свойство скрыто через showif
                            if (column != null) {
                                updatedColumns.add(column);

                                GGroupObjectValue fullKey = columnKey.isEmpty() ? rowKey : new GGroupObjectValueBuilder(rowKey, columnKey).toGroupObjectValue();

                                Object value = propValues.get(fullKey);
                                Object readOnly = propReadOnly == null ? null : propReadOnly.get(fullKey);
                                Object background = propertyBackgrounds == null ? null : propertyBackgrounds.get(fullKey);
                                Object foreground = propertyForegrounds == null ? null : propertyForegrounds.get(fullKey);

                                record.setValue(column.columnID, value);
                                record.setReadOnly(column.columnID, readOnly);
                                record.setBackground(column.columnID, background == null ? property.background : background);
                                record.setForeground(column.columnID, foreground == null ? property.foreground : foreground);
                            }
                        }
                    }
                });
            }

            redrawColumns(updatedColumns);

            updatedProperties.clear();
            dataUpdated = false;
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

    @Override
    public GridPropertyTableKeyboardSelectionHandler getKeyboardSelectionHandler() {
        return keyboardSelectionHandler;
    }

    @Override
    public GAbstractGroupObjectController getGroupController() {
        return groupObjectController;
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
        if (selectedRecord != null && getMinPropertyIndex(property) != -1) {
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
                values.get(property).putAll(propValues);
            } else {
                NativeHashMap<GGroupObjectValue, Object> pvalues = new NativeHashMap<GGroupObjectValue, Object>();
                pvalues.putAll(propValues);
                values.put(property, pvalues);
            }
            updatedProperties.put(property, TRUE);

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
        Map<GGroupObjectValue, Object> oldValues = propertyCaptions.get(propertyDraw);
        if (!nullEquals(oldValues, values)) {
            super.updatePropertyCaptions(propertyDraw, values);
            columnsUpdated = true;
        }
    }

    public void updateReadOnlyValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        readOnlyValues.put(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
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

    @Override
    String getCellBackground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getBackground(((GridColumn)getColumn(column)).columnID);
    }

    @Override
    String getCellForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(((GridColumn)getColumn(column)).columnID);
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

    public GGroupObjectValue getColumnKey(int column) {
        return columnKeysList.get(column);
    }

    public GGroupObjectValue getCurrentColumnKey() {
        return getColumnKey(getKeyboardSelectedColumn());
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        GPropertyDraw property = getProperty(context);
        if (property != null && !property.isReadOnly() && !(property.baseType instanceof GObjectType)) {
            GridDataRecord rowRecord = (GridDataRecord) context.getRowValue();
            GridColumn column = (GridColumn) getColumn(context.getColumn());
            return !rowRecord.isReadonly(column.columnID);
        }
        return false;
    }

    public Object getValueAt(Cell.Context context) {
        Column column = getColumn(context.getColumn());
        return column.getValue(context.getRowValue());
    }

    @Override
    public void pasteData(final String dataLine, boolean multi) {
        int selectedColumn = getKeyboardSelectedColumn();

        if (selectedColumn == -1 || dataLine == null) {
            return;
        }

        final ArrayList<GPropertyDraw> propertiesAfterSelected = new ArrayList<GPropertyDraw>();
        final List<GGroupObjectValue> columnKeys = new ArrayList<GGroupObjectValue>();
        for (int i = selectedColumn; i < getColumnCount(); i++) {
            GPropertyDraw propertyDraw = getProperty(i);
            propertiesAfterSelected.add(propertyDraw);
            columnKeys.add(getColumnKey(i));
        }

        if (multi) {
            DialogBoxHelper.showConfirmBox("lsFusion", "Вы уверены, что хотите изменить значения нескольких ячеек?", new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    if (chosenOption == DialogBoxHelper.OptionType.YES) {
                        form.pasteExternalTable(propertiesAfterSelected, columnKeys, dataLine);
                    }
                }
            });
        } else {
            form.pasteExternalTable(propertiesAfterSelected, columnKeys, dataLine);
        }
    }

    @Override
    public void onResize() {
        super.onResize();

        int tableHeight = getOffsetHeight();
        if (tableHeight == 0) {
            return;
        }
        int newPageSize = tableHeight / getRowHeight() + 1;
        if (newPageSize != pageSize) {
            form.changePageSizeAfterUnlock(groupObject, newPageSize);
            pageSize = newPageSize;
        }
    }

    @Override
    public void quickFilter(EditEvent event) {
        groupObjectController.quickEditFilter(event);
    }

    public void selectProperty(GPropertyDraw propertyDraw) {
        if (propertyDraw == null) {
            return;
        }

        int ind = getMinPropertyIndex(propertyDraw);
        if (ind != -1) {
            setKeyboardSelectedColumn(ind, false);
        }
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
