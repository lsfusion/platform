package lsfusion.gwt.form.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.jsni.Function;
import lsfusion.gwt.base.client.jsni.NativeHashMap;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.cellview.client.Column;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.KeyboardRowChangedEvent;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.cellview.client.cell.CellPreviewEvent;
import lsfusion.gwt.form.client.form.ui.toolbar.preferences.GGridUserPreferences;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValueBuilder;
import lsfusion.gwt.form.shared.view.classes.GObjectType;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.GridEditableCell;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static lsfusion.gwt.base.shared.GwtSharedUtils.nullEquals;

public class GGridTable extends GGridPropertyTable<GridDataRecord> {
    private ArrayList<GPropertyDraw> columnProperties = new ArrayList<GPropertyDraw>();
    private ArrayList<GGroupObjectValue> columnKeysList = new ArrayList<GGroupObjectValue>();

    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>>();

    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<GGroupObjectValue>();

    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> values = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> showIfs = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> readOnlyValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private NativeHashMap<GPropertyDraw, Boolean> updatedProperties = new NativeHashMap<GPropertyDraw, Boolean>();
    private NativeHashMap<GPropertyDraw, List<GGroupObjectValue>> columnKeys = new NativeHashMap<GPropertyDraw, List<GGroupObjectValue>>();

    private boolean rowsUpdated = false;
    private boolean columnsUpdated = false;
    private boolean captionsUpdated = false;
    private boolean dataUpdated = false;

    private final ArrayList<GridDataRecord> currentRecords = new ArrayList<GridDataRecord>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;
    private GGridController gridController;

    private GridTableKeyboardSelectionHandler keyboardSelectionHandler;

    private GGroupObjectController groupObjectController;
    
    private GGridUserPreferences generalGridPreferences;
    private GGridUserPreferences userGridPreferences;
    private GGridUserPreferences currentGridPreferences;

    private int nextColumnID = 0;

    private int pageSize = 50;

    public GGridTable(GFormController iform, GGroupObjectController igroupController, GGridController gridController, GGridUserPreferences[] iuserPreferences) {
        super(iform, null);

        this.groupObjectController = igroupController;
        this.groupObject = igroupController.groupObject;
        this.gridController = gridController;

        generalGridPreferences = iuserPreferences != null && iuserPreferences[0] != null ? iuserPreferences[0] : new GGridUserPreferences(groupObject);
        userGridPreferences = iuserPreferences != null && iuserPreferences[1] != null ? iuserPreferences[1] : new GGridUserPreferences(groupObject);
        resetCurrentPreferences(true);

        if (currentGridPreferences.font != null) {
            font = currentGridPreferences.font;
        }
        if (font == null) {
            font = gridController.getFont();
        }

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
            protected void ordersCleared(GGroupObject groupObject) {
                form.clearPropertyOrders(groupObject);
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
                    int rowHeight = getRowHeight();

                    int scrollHeight = getTableDataScroller().getClientHeight();
                    int scrollTop = getTableDataScroller().getVerticalScrollPosition();

                    int rowTop = selectedRow * rowHeight;
                    int rowBottom = rowTop + rowHeight;

                    int newRow = -1;
                    if (rowBottom > scrollTop + scrollHeight) {
                        newRow = (scrollTop + scrollHeight - rowHeight) / rowHeight;
                    }
                    if (rowTop < scrollTop) {
                        newRow = scrollTop / rowHeight + 1;
                    }
                    if (newRow != -1) {
                        setKeyboardSelectedRow(newRow, false);
                    }
                }
            }
        });

        getElement().setPropertyObject("groupObject", groupObject);
        gridController.setForceHidden(true);
    }

    public void update() {
        storeScrollPosition();

        updatedColumnsImpl();

        updateCaptionsImpl();

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
            columnProperties.clear();
            columnKeysList.clear();

            List<GPropertyDraw> orderedVisibleProperties = getOrderedVisibleProperties(properties); 
            
            for (GPropertyDraw property : orderedVisibleProperties) {
                List<GGroupObjectValue> propertyColumnKeys = columnKeys.get(property);
                if (propertyColumnKeys == null) {
                    propertyColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                }

                Map<GGroupObjectValue, Object> propShowIfs = showIfs.get(property);
                for (GGroupObjectValue columnKey : propertyColumnKeys) {
                    if ((propShowIfs == null || propShowIfs.get(columnKey) != null)) {
                        columnProperties.add(property);
                        columnKeysList.add(columnKey);
                    }
                }
            }

            int rowHeight = 0;
            preferredWidth = 0;
            NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> newColumnsMap = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>>();
            for (int i = 0; i < columnProperties.size(); ++i) {
                GPropertyDraw property = columnProperties.get(i);
                GGroupObjectValue columnKey = columnKeysList.get(i);

                GridColumn column = removeFromColumnsMap(columnsMap, property, columnKey);
                if (column != null) {
                    moveGridColumn(column, i);
                } else {
                    column = insertGridColumn(i);
                    // если колонка появилась через showif без обновления данных
                    if (!updatedProperties.containsKey(property)) {
                        updatedProperties.put(property, TRUE);
                        dataUpdated = true; // если кроме появления этой колонки в гриде ничего не поменялось, всё равно нужно обновить данные и подсветки
                    }
                }

                int columnMinimumWidth = getUserWidth(property) != null ? getUserWidth(property) : property.getMinimumPixelWidth(font);
                int columnMinimumHeight = property.getMinimumPixelHeight(font);
                setColumnWidth(column, columnMinimumWidth  + "px");

                //дублирование логики изменения captions для оптимизации
                String columnCaption = null;
                Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);
                Object propCaption = null;
                if (propCaptions == null || (propCaption = propCaptions.get(columnKey)) != null) {
                    if (propCaptions != null) {
                        columnCaption = property.getDynamicCaption(propCaption);
                    } else {
                        columnCaption = property.getCaptionOrEmpty();
                    }
                }

                GGridPropertyTableHeader header = headers.get(i);
                header.setCaption(columnCaption);
                header.setToolTip(property.getTooltipText(columnCaption));

                putToColumnsMap(newColumnsMap, property, columnKey, column);

                rowHeight = Math.max(rowHeight, columnMinimumHeight);
                preferredWidth += columnMinimumWidth;
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
            captionsUpdated = false;
        }
    }
    
    public boolean containsProperty(GPropertyDraw property) {
        return properties.contains(property);
    }

    public List<GPropertyDraw> getOrderedVisibleProperties(List<GPropertyDraw> propertiesList) {
        List<GPropertyDraw> result = new ArrayList<GPropertyDraw>();

        for (GPropertyDraw property : propertiesList) {
            if (hasUserPreferences()) {
                Boolean userHide = getUserHide(property);
                if (userHide == null || !userHide) {
                    if (getUserOrder(property) == null) {
                        setUserHide(property, true);
                        setUserOrder(property, Short.MAX_VALUE + propertiesList.indexOf(property));
                    } else {
                        result.add(property);
                    }
                }
            } else if (!property.hide) {
                result.add(property);
            }
        }

        if (hasUserPreferences()) {
            Collections.sort(result, getCurrentPreferences().getUserOrderComparator());
        }
        return result;
    }

    public void updateCaptionsImpl() {
        if (captionsUpdated) {
            for (int i = 0; i < columnProperties.size(); ++i) {
                GPropertyDraw property = columnProperties.get(i);

                String columnCaption = null;
                Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);
                if (propCaptions != null) {
                    columnCaption = property.getDynamicCaption(propCaptions.get(columnKeysList.get(i)));
                } else {
                    columnCaption = property.getCaptionOrEmpty();
                }

                headers.get(i).setCaption(columnCaption);
            }
            refreshHeaders();
            captionsUpdated = false;
        }
    }

    public void columnsPreferencesChanged() {
        columnsUpdated = true;
        dataUpdated = true;

        ArrayList<GFont> fonts = new ArrayList<GFont>();
        fonts.add(font);
        GFontMetrics.calculateFontMetrics(fonts, new GFontMetrics.MetricsCallback() {
            @Override
            public void metricsCalculated() {
                updatedColumnsImpl();
                updateDataImpl();
            }
        });
    }
    
    public GFont getDesignFont() {
        return gridController.getFont();
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

    public GGroupObject getGroupObject() {
        return groupObject;
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

    public ArrayList<GPropertyDraw> getProperties() {
        return properties;
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
        super.updatePropertyCaptions(propertyDraw, values);
        captionsUpdated = true;
    }

    public void updateShowIfValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        Map<GGroupObjectValue, Object> oldValues = showIfs.get(propertyDraw);
        if (!nullEquals(oldValues, values)) {
            showIfs.put(propertyDraw, values);
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
        return rowValue.getBackground(((GridColumn) getColumn(column)).columnID);
    }

    @Override
    String getCellForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(((GridColumn) getColumn(column)).columnID);
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
        setKeys(rowKeys);

        update();
    }

    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        int ind = getMinPropertyIndex(property);
        HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<GPropertyDraw, GGroupObjectValue>();
        key.put(property, ind == -1 ? GGroupObjectValue.EMPTY : columnKeysList.get(ind));
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
    public void pasteData(final List<List<String>> table) {
        final int selectedColumn = getKeyboardSelectedColumn();

        if (selectedColumn == -1 || table.isEmpty()) {
            return;
        }

        final int tableColumns = table.get(0).size();

        boolean singleC = table.size() == 1 && tableColumns == 1;

        if (!singleC) {
            DialogBoxHelper.showConfirmBox("lsFusion", "Вы уверены, что хотите изменить значения нескольких ячеек?", new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    if (chosenOption == DialogBoxHelper.OptionType.YES) {
                        int columnsToInsert = Math.min(tableColumns, getColumnCount() - selectedColumn);

                        final ArrayList<GPropertyDraw> propertyList = new ArrayList<GPropertyDraw>();
                        final ArrayList<GGroupObjectValue> columnKeys = new ArrayList<GGroupObjectValue>();
                        for (int i = 0; i < columnsToInsert; i++) {
                            GPropertyDraw propertyDraw = getProperty(selectedColumn + i);
                            propertyList.add(propertyDraw);
                            columnKeys.add(getColumnKey(selectedColumn + i));
                        }

                        form.pasteExternalTable(propertyList, columnKeys, table, columnsToInsert);
                    }
                }
            });
        } else if (!table.get(0).isEmpty()) {
            form.pasteSingleValue(getProperty(selectedColumn), getColumnKey(selectedColumn), table.get(0).get(0));
        }
    }

    @Override
    public void onResize() {
        super.onResize();

        if (isVisible()) {
            int tableHeight = getTableDataScroller().getClientHeight();
            if (tableHeight == 0) {
                return;
            }
            int newPageSize = tableHeight / getRowHeight() + 1;
            if (newPageSize != pageSize) {
                form.changePageSizeAfterUnlock(groupObject, newPageSize);
                pageSize = newPageSize;
                setPageIncrement(pageSize - 1);
            }
        }
    }

    @Override
    public void quickFilter(EditEvent event, GPropertyDraw filterProperty) {
        groupObjectController.quickEditFilter(event, filterProperty);
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

    public void clearGridOrders(GGroupObject groupObject) {
        sortableHeaderManager.clearOrders(groupObject);
    }

    public Map<Map<GPropertyDraw, GGroupObjectValue>, Boolean> getOrderDirections() {
        return sortableHeaderManager.getOrderDirections();
    }

    public boolean userPreferencesSaved() {
        return userGridPreferences.hasUserPreferences();
    }

    public boolean generalPreferencesSaved() {
        return generalGridPreferences.hasUserPreferences();
    }

    public GGroupObjectUserPreferences getCurrentUserGridPreferences() {
        if (currentGridPreferences.hasUserPreferences()) {
            return currentGridPreferences.convertPreferences();
        }
        return userGridPreferences.convertPreferences();
    }

    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return generalGridPreferences.convertPreferences();
    }

    public void resetCurrentPreferences(boolean initial) {
        currentGridPreferences = new GGridUserPreferences(userGridPreferences.hasUserPreferences() ? userGridPreferences : generalGridPreferences);
        
        if (!initial) {
            gridController.clearGridOrders(groupObject);
            if (!currentGridPreferences.hasUserPreferences()) {
                groupObjectController.applyDefaultOrders();
            } else {
                groupObjectController.applyUserOrders();
            }
        }
    }

    public void resetPreferences(final boolean forAllUsers, final ErrorHandlingCallback<ServerResponseResult> callback) {
        currentGridPreferences.resetPreferences();

        if (!properties.isEmpty()) {
            form.saveUserPreferences(currentGridPreferences, forAllUsers, new ErrorHandlingCallback<ServerResponseResult>() {
                @Override
                public void failure(Throwable caught) {
                    resetCurrentPreferences(false);
                    callback.failure(caught);
                }

                @Override
                public void success(ServerResponseResult result) {
                    (forAllUsers ? generalGridPreferences : userGridPreferences).resetPreferences();
                    resetCurrentPreferences(false);
                    callback.success(result);
                }
            });
        }
    }

    public void saveCurrentPreferences(final boolean forAllUsers, final ErrorHandlingCallback<ServerResponseResult> callback) {
        currentGridPreferences.setHasUserPreferences(true);

        if (!getProperties().isEmpty()) {

            form.saveUserPreferences(currentGridPreferences, forAllUsers, new ErrorHandlingCallback<ServerResponseResult>() {
                @Override
                public void success(ServerResponseResult result) {
                    if (forAllUsers) {
                        generalGridPreferences = new GGridUserPreferences(currentGridPreferences);
                        resetCurrentPreferences(false);
                    } else {
                        userGridPreferences = new GGridUserPreferences(currentGridPreferences);
                    }
                    callback.success(result);
                }

                @Override
                public void failure(Throwable caught) {
                    resetCurrentPreferences(false);
                    callback.failure(caught);
                }
            });
        }
    }

    public GGridUserPreferences getCurrentPreferences() {
        return currentGridPreferences;
    }

    public boolean hasUserPreferences() {
        return currentGridPreferences.hasUserPreferences();
    }

    public void setHasUserPreferences(boolean hasUserPreferences) {
        currentGridPreferences.setHasUserPreferences(hasUserPreferences);
    }

    public GFont getUserFont() {
        return currentGridPreferences.font;
    }

    public Boolean getUserHide(GPropertyDraw property) {
        return currentGridPreferences.getUserHide(property);
    }

    public Integer getUserWidth(GPropertyDraw property) {
        return currentGridPreferences.getUserWidth(property);
    }

    public Integer getUserOrder(GPropertyDraw property) {
        return currentGridPreferences.getUserOrder(property);
    }

    public Integer getUserSort(GPropertyDraw property) {
        return currentGridPreferences.getUserSort(property);
    }

    public Boolean getUserAscendingSort(GPropertyDraw property) {
        return currentGridPreferences.getUserAscendingSort(property);
    }

    public void setUserFont(GFont userFont) {
        currentGridPreferences.font = userFont;
    }

    public void setUserHide(GPropertyDraw property, Boolean userHide) {
        currentGridPreferences.setUserHide(property, userHide);
    }

    public void setUserWidth(GPropertyDraw property, Integer userWidth) {
        currentGridPreferences.setUserWidth(property, userWidth);
    }

    public void setUserOrder(GPropertyDraw property, Integer userOrder) {
        currentGridPreferences.setUserOrder(property, userOrder);
    }

    public void setUserSort(GPropertyDraw property, Integer userSort) {
        currentGridPreferences.setUserSort(property, userSort);
    }

    public void setUserAscendingSort(GPropertyDraw property, Boolean userAscendingSort) {
        currentGridPreferences.setUserAscendingSort(property, userAscendingSort);
    }

    public Comparator<GPropertyDraw> getUserSortComparator() {
        return getCurrentPreferences().getUserSortComparator();
    }

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
