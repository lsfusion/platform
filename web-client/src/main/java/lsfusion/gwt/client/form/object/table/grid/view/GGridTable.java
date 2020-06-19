package lsfusion.gwt.client.form.object.table.grid.view;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.jsni.Function;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Context;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Math.min;
import static java.lang.String.valueOf;
import static java.util.Collections.singleton;
import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GGridTable extends GGridPropertyTable<GridDataRecord> implements GTableView {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final double QUICK_SEARCH_MAX_DELAY = 2000;

    private ArrayList<GPropertyDraw> properties = new ArrayList<>();
    // map for fast incremental update, essentially needed for working with group-to-columns (columnKeys)
    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap = new NativeHashMap<>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<>();

    private NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> values = new NativeHashMap<>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> showIfs = new HashMap<>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> readOnlyValues = new HashMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private NativeHashMap<GPropertyDraw, Boolean> updatedProperties = new NativeHashMap<>();
    private NativeHashMap<GPropertyDraw, List<GGroupObjectValue>> columnKeys = new NativeHashMap<>();

    private boolean rowsUpdated = false;
    private boolean columnsUpdated = false;
    private boolean captionsUpdated = false;
    private boolean dataUpdated = false;

    private final ArrayList<GridDataRecord> currentRecords = new ArrayList<>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    private GGridController groupObjectController;
    
    private GGridUserPreferences generalGridPreferences;
    private GGridUserPreferences userGridPreferences;
    private GGridUserPreferences currentGridPreferences;

    private int nextColumnID = 0;

    private int pageSize = 50;

    private String lastQuickSearchPrefix = "";

    private double lastQuickSearchTime = 0;

    private boolean autoSize;

    private long setRequestIndex;

    @Override
    protected boolean isAutoSize() {
        return autoSize;
    }

    @Override
    public int getPageSize() {
        return -1;
    }

    @Override
    public GListViewType getViewType() {
        return GListViewType.GRID;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    public GGridTable(GFormController iform, GGridController igroupController, GGridUserPreferences[] iuserPreferences, boolean autoSize) {
        super(iform, igroupController.groupObject, null);

        this.groupObjectController = igroupController;
        this.groupObject = igroupController.groupObject;

        this.autoSize = autoSize;

        generalGridPreferences = iuserPreferences != null && iuserPreferences[0] != null ? iuserPreferences[0] : new GGridUserPreferences(groupObject);
        userGridPreferences = iuserPreferences != null && iuserPreferences[1] != null ? iuserPreferences[1] : new GGridUserPreferences(groupObject);
        resetCurrentPreferences(true);

        if (currentGridPreferences.font != null) {
            font = currentGridPreferences.font;
        }
        if (font == null) {
            font = groupObject.grid.font;
        }

        setSelectionHandler(new GridTableSelectionHandler(this));

        setRowChangedHandler(() -> {
            final GridDataRecord selectedRecord = getSelectedRowValue();
            if (selectedRecord != null && !selectedRecord.getKey().equals(currentKey)) {
                assert !form.isEditing();
//                    if(form.isEditing())
//                        form.commitEditingTable();
                setCurrentKey(selectedRecord.getKey());
                form.changeGroupObjectLater(groupObject, selectedRecord.getKey());
            }
        });

        sortableHeaderManager = new GGridSortableHeaderManager<Map<GPropertyDraw, GGroupObjectValue>>(this, false) {
            @Override
            protected void orderChanged(Map<GPropertyDraw, GGroupObjectValue> columnKey, GOrder modiType, boolean alreadySet) {
                form.changePropertyOrder(columnKey.keySet().iterator().next(), columnKey.values().iterator().next(), modiType, alreadySet);
            }

            @Override
            protected void ordersCleared(GGroupObject groupObject) {
                form.clearPropertyOrders(groupObject);
            }

            @Override
            protected Map<GPropertyDraw, GGroupObjectValue> getColumnKey(int column) {
                GridColumn gridColumn = getGridColumn(column);
                HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<>();
                key.put(gridColumn.property, gridColumn.columnKey);
                return key;
            }
        };

        tableDataScroller.addScrollHandler(event -> {
            int selectedRow = getSelectedRow();
            GridDataRecord selectedRecord = getSelectedRowValue();
            if (selectedRecord != null) {
                int scrollHeight = tableDataScroller.getClientHeight();
                int scrollTop = tableDataScroller.getVerticalScrollPosition();

                TableRowElement rowElement = getChildElement(selectedRow);
                int rowTop = rowElement.getOffsetTop();
                int rowBottom = rowTop + rowElement.getClientHeight();

                int newRow = -1;
                if (rowBottom > scrollTop + scrollHeight + 1) { // поправка на 1 пиксель при скроллировании вверх - компенсация погрешности, возникающей при setScrollTop() / getScrollTop()
                    newRow = getLastSeenRow(scrollTop + scrollHeight, selectedRow);
                }
                if (rowTop < scrollTop) {
                    newRow = getFirstSeenRow(scrollTop, selectedRow);
                }
                if (newRow != -1) {
                    setSelectedRow(newRow, false);
                }
            }
        });

        getElement().setPropertyObject("groupObject", groupObject);
    }

    // to give public access
    public Widget getWidget() {
        return super.getWidget();
    }
    public Element getTableDataFocusElement() {
        return super.getTableDataFocusElement();
    }

    protected int getFirstSeenRow(int tableTop, int start) {
        for (int i = start; i < getRowCount(); i++) {
            TableRowElement rowElement = getChildElement(i);
            int rowTop = rowElement.getOffsetTop();
            if (rowTop >= tableTop) {
                return i;
            }
        }
        return 0;
    }
    
    protected int getLastSeenRow(int tableBottom, int start) {
        for (int i = start; i >= 0; i--) {
            TableRowElement rowElement = getChildElement(i);
            int rowBottom = rowElement.getOffsetTop() + rowElement.getClientHeight();
            if (rowBottom <= tableBottom) {
                return i;
            }
        }
        return 0;
    }
    
    public void update(Boolean updateState) {
        updateModify(false);
        if(updateState != null)
            updateState(updateState);
    }

    private void updateModify(boolean modifyGroupObject) {
        storeScrollPosition();

        updateColumnsImpl();

        updateCaptionsImpl();

        updateRowsImpl(modifyGroupObject);
        
        if (modifyGroupObject) {
            // обновим данные в колонках. при асинхронном удалении ряда можем не получить подтверждения от сервера - придётся вернуть строку
            for (GPropertyDraw property : properties) {
                updatedProperties.put(property, TRUE);
            }
            dataUpdated = true;
        }

        updateDataImpl();
    }

    private void updateRowsImpl(boolean modifyGroupObject) {
        if (rowsUpdated) {
            int currentSize = currentRecords.size();
            int newSize = rowKeys.size();

            if (currentSize > newSize) {
                if (modifyGroupObject) {
                    ArrayList<GridDataRecord> oldRecords = new ArrayList<>(currentRecords);
                    for (GridDataRecord record : oldRecords) {
                        if (!rowKeys.contains(record.getKey())) {
                            currentRecords.remove(record);
                        }
                    }
                } else {
                    for (int i = currentSize - 1; i >= newSize; --i) {
                        currentRecords.remove(i);
                    }
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
                setSelectedRow(rowKeys.indexOf(currentKey), false);
            }

            rowsUpdated = false;
        }
    }

    private void updateColumnsImpl() {
        if (columnsUpdated) {
            List<GPropertyDraw> orderedVisibleProperties = getOrderedVisibleProperties(properties);

            //разбиваем на группы свойств, которые будут идти чередуясь для каждого ключа из групп в колонках ("шахматка")
            NativeHashMap<String, NativeHashMap<List<GGroupObject>, Integer>> columnGroupsIndices = new NativeHashMap<>();
            List<List<GPropertyDraw>> columnGroups = new ArrayList<>();
            List<List<GGroupObjectValue>> columnGroupsColumnKeys = new ArrayList<>();

            for (GPropertyDraw property : orderedVisibleProperties) {
                if (property.columnsName != null && property.columnGroupObjects != null) {
                    List<GPropertyDraw> columnGroup;

                    Integer groupInd = getFromDoubleMap(columnGroupsIndices, property.columnsName, property.columnGroupObjects);
                    if (groupInd != null) {
                        // уже было свойство с такими же именем и группами в колонках
                        columnGroup = columnGroups.get(groupInd);
                    } else {
                        // новая группа свойств
                        columnGroup = new ArrayList<>();

                        putToDoubleNativeMap(columnGroupsIndices, property.columnsName, property.columnGroupObjects, columnGroups.size());
                        columnGroupsColumnKeys.add(columnKeys.get(property));
                        columnGroups.add(columnGroup);
                    }
                    columnGroup.add(property);
                } else {
                    columnGroupsColumnKeys.add(columnKeys.get(property));
                    columnGroups.add(Collections.singletonList(property));
                }
            }

            int rowHeight = 0;
            int headerHeight = getHeaderHeight();

            int currentIndex = 0;
            NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> newColumnsMap = new NativeHashMap<>();
            for (int i = 0; i < columnGroups.size(); i++) {
                for (GGroupObjectValue columnKey : columnGroupsColumnKeys.get(i)) {
                    for (GPropertyDraw property : columnGroups.get(i)) {
                        Map<GGroupObjectValue, Object> propShowIfs = showIfs.get(property);
                        if (propShowIfs != null && propShowIfs.get(columnKey) == null) // property is hidden
                            continue;

                        GridColumn column = removeFromColumnsMap(columnsMap, property, columnKey);
                        if (column != null) {
                            moveGridColumn(column, currentIndex);
                        } else {
                            column = insertGridColumn(currentIndex, property, columnKey);
                            // если колонка появилась через showif без обновления данных
                            if (!updatedProperties.containsKey(property)) {
                                updatedProperties.put(property, TRUE);
                                dataUpdated = true; // если кроме появления этой колонки в гриде ничего не поменялось, всё равно нужно обновить данные и подсветки
                            }
                        }

                        //дублирование логики изменения captions для оптимизации
                        String columnCaption;
                        Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);
                        columnCaption = getUserCaption(property);
                        if (columnCaption == null) {
                            Object propCaption = null;
                            if (propCaptions == null || (propCaption = propCaptions.get(columnKey)) != null) {
                                if (propCaptions != null) {
                                    columnCaption = property.getDynamicCaption(propCaption);
                                } else {
                                    columnCaption = property.getCaptionOrEmpty();
                                }
                            }
                        }

                        GGridPropertyTableHeader header = getGridHeader(currentIndex);
                        header.setCaption(columnCaption, property.notNull, property.hasChangeAction);
                        header.setToolTip(property.getTooltipText(columnCaption));
                        header.setHeaderHeight(headerHeight);

                        property.setUserPattern(getUserPattern(property));

                        putToColumnsMap(newColumnsMap, property, columnKey, column);

                        int columnMinimumHeight = property.getValueHeight(font);
                        rowHeight = Math.max(rowHeight, columnMinimumHeight);

                        currentIndex++;
                    }
                }
            }

            setCellHeight(rowHeight);

            // removing old columns
            columnsMap.foreachValue(columnsCollection -> columnsCollection.foreachValue(column -> {
                removeGridColumn(column);
            }));
            columnsMap = newColumnsMap;

            updateLayoutWidth(); // have to do after columnsmap update, because it is used inside

            refreshHeaders();

            columnsUpdated = false;
            captionsUpdated = false;
        }
    }

    @Override
    public boolean isNoColumns() {
        return getColumnCount() == 0;
    }

    @Override
    public long getSetRequestIndex() {
        return setRequestIndex;
    }

    @Override
    public void setSetRequestIndex(long index) {
        setRequestIndex = index;
    }

    protected GPropertyDraw getColumnPropertyDraw(int i) {
        return getGridColumn(i).property;
    }

    public boolean containsProperty(GPropertyDraw property) {
        return getPropertyIndex(property, null) >= 0;
    }

    public List<GPropertyDraw> getOrderedVisibleProperties(List<GPropertyDraw> propertiesList) {
        List<GPropertyDraw> result = new ArrayList<>();

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
            for (int i = 0, size = getColumnCount(); i < size; ++i) {
                GridColumn gridColumn = getGridColumn(i);
                GPropertyDraw property = gridColumn.property;

                String columnCaption;
                Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);
                if (propCaptions != null) {
                    columnCaption = property.getDynamicCaption(propCaptions.get(gridColumn.columnKey));
                } else {
                    columnCaption = property.getCaptionOrEmpty();
                }

                getGridHeader(i).setCaption(columnCaption, property.notNull, property.hasChangeAction);
            }
            refreshHeaders();
            captionsUpdated = false;
        }
    }

    public void columnsPreferencesChanged() {
        columnsUpdated = true;
        dataUpdated = true;

        updateColumnsImpl();
        updateDataImpl();
//
//        final ArrayList<GFontWidthString> fonts = new ArrayList<>();
//        for(GPropertyDraw property : properties)
//            property.getValueWidth(font, new GWidthStringProcessor() {
//                public void addWidthString(GFontWidthString fontWidthString) {
//                    fonts.add(fontWidthString);
//                }
//            });
//        GFontMetrics.calculateFontMetrics(fonts, new GFontMetrics.MetricsCallback() {
//            @Override
//            public Widget metricsCalculated() {
//                updatedColumnsImpl();
//                updateDataImpl();
//                return null;
//            }
//        });
    }
    
    public int getHeaderHeight() {
        Integer headerHeight = currentGridPreferences.headerHeight;
        if (headerHeight == null || headerHeight <= 0) {
            headerHeight = groupObject.grid.headerHeight;
        }
        return headerHeight;
    }
    
    public GFont getDesignFont() {
        return groupObject.grid.font;
    }

    public static void putToColumnsMap(NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column, GridColumn value) {
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap == null) {
            columnsMap.put(row, rowMap = new NativeHashMap<>());
        }
        rowMap.put(column, value);
    }

    public static GridColumn getFromColumnsMap(NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column) {
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap != null) {
            return rowMap.get(column);
        }
        return null;
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
            final Set<Column> updatedColumns = new HashSet<>();
            for (final GridDataRecord record : currentRecords) {
                final GGroupObjectValue rowKey = record.getKey();
                updatedProperties.foreachKey(new Function<GPropertyDraw>() {
                    @Override
                    public void apply(GPropertyDraw property) {
                        NativeHashMap<GGroupObjectValue, Object> propValues = values.get(property);
                        Map<GGroupObjectValue, Object> propReadOnly = readOnlyValues.get(property);
                        Map<GGroupObjectValue, Object> propertyBackgrounds = cellBackgroundValues.get(property);
                        Map<GGroupObjectValue, Object> propertyForegrounds = cellForegroundValues.get(property);
                        for (GGroupObjectValue columnKey : columnKeys.get(property)) {
                            NativeHashMap<GGroupObjectValue, GridColumn> propertyColumns = columnsMap.get(property);
                            GridColumn column = propertyColumns == null ? null : propertyColumns.get(columnKey);
                            // column == null, когда свойство скрыто через showif
                            if (column != null) {
                                updatedColumns.add(column);

                                GGroupObjectValue fullKey = GGroupObjectValue.getFullKey(rowKey, columnKey);

                                Object value = propValues.get(fullKey);
                                Object readOnly = propReadOnly == null ? null : propReadOnly.get(fullKey);
                                Object background = propertyBackgrounds == null ? null : propertyBackgrounds.get(fullKey);
                                Object foreground = propertyForegrounds == null ? null : propertyForegrounds.get(fullKey);

                                column.setValue(record, value);
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

    public GridColumn getGridColumn(int column) {
        return (GridColumn) getColumn(column);
    }

    private GridColumn insertGridColumn(int index, GPropertyDraw property, GGroupObjectValue columnKey) {
        GridColumn column = new GridColumn(property, columnKey);
        GGridPropertyTableHeader header = new GGridPropertyTableHeader(this, null, null);

        insertColumn(index, column, header);

        return column;
    }

    private void moveGridColumn(GridColumn column, int newIndex) {
        int oldIndex = getColumnIndex(column);
        if (oldIndex != newIndex)
            moveColumn(oldIndex, newIndex);
    }

    private void removeGridColumn(GridColumn column) {
        removeColumn(column);
    }

    public boolean isEmpty() {
        for (GPropertyDraw property : properties) {
            if (!values.get(property).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public GGroupObjectValue getCurrentKey() {
        return currentKey;
    }

    @Override
    public GAbstractTableController getGroupController() {
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

    public GGroupObjectValue getCurrentColumn() {
        GGroupObjectValue property = getSelectedColumnKey();
        if (property == null && getColumnCount() > 0) {
            property = getColumnKey(0);
        }
        return property;
    }

    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        GridDataRecord selectedRecord = getSelectedRowValue();
        int column = getPropertyIndex(property, columnKey);
        if (selectedRecord != null && column != -1 && column < getColumnCount()) {
            return getColumn(column).getValue(selectedRecord);
        }

        return null;
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        Log.debug("Setting current object to: " + currentKey);
        this.currentKey = currentKey;
    }

    public void updateState(boolean updateState) {
        Element element = getTableElement();
        if(updateState)
            element.getStyle().setProperty("opacity", "0.5");
        else
            element.getStyle().setProperty("opacity", "");
    }

    public void removeProperty(GPropertyDraw property) {
        values.remove(property);
        properties.remove(property);
        columnKeys.remove(property);

        columnsUpdated = true;
    }

    public void updateProperty(final GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values) {
        if(!updateKeys) {
            if (!properties.contains(property)) {
                int newColumnIndex = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
                properties.add(newColumnIndex, property);

                form.addPropertyBindings(property, () -> new GFormController.Binding(property.groupObject) {
                    @Override
                    public void pressed(Event event) {
//                        focusProperty(property); // редактирование сразу по индексу не захотело работать. поэтому сначала выделяем ячейку
                        onEditEvent(event, true, getCurrentCellContext(), getSelectedElement(), () -> {});
                    }

                    @Override
                    public boolean showing() {
                        return isShowing(GGridTable.this);
                    }
                });

                this.columnKeys.put(property, columnKeys);

                columnsUpdated = true;
            } else {
                List<GGroupObjectValue> oldValues = this.columnKeys.get(property);
                if (!columnKeys.equals(oldValues)) {
                    this.columnKeys.put(property, columnKeys);
                    columnsUpdated = true;
                }
            }
        }

        NativeHashMap<GGroupObjectValue, Object> valuesMap = this.values.get(property);
        if (updateKeys && valuesMap != null) {
            valuesMap.putAll(values);
        } else {
            NativeHashMap<GGroupObjectValue, Object> pvalues = new NativeHashMap<>();
            pvalues.putAll(values);
            this.values.put(property, pvalues);
        }
        
        updatedProperties.put(property, TRUE);
        dataUpdated = true;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.rowKeys = keys;

        needToRestoreScrollPosition = true;
        rowsUpdated = true;
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
    public void updateLastValues(GPropertyDraw property, int index, Map<GGroupObjectValue, Object> values) {
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
        return getGridColumn(column).property;
    }

    public GPropertyDraw getProperty(Context context) {
        return getProperty(context.getColumn());
    }

    @Override
    public String getCellBackground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getBackground(((GridColumn) getColumn(column)).columnID);
    }

    @Override
    public String getCellForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(((GridColumn) getColumn(column)).columnID);
    }

    public int getPropertyIndex(GPropertyDraw property, GGroupObjectValue columnKey) {
        for(int i=0,size=getColumnCount();i<size;i++) {
            GridColumn gridColumn = getGridColumn(i);
            if (property == gridColumn.property && (columnKey == null || columnKey.equals(gridColumn.columnKey))) {
                return i;
            }
        }
        return -1;
    }

    public void modifyGroupObject(GGroupObjectValue rowKey, boolean add, int position) {
        if (add) {
            if (position >= 0 && position <= rowKeys.size()) {
                rowKeys.add(position, rowKey);
            } else {
                rowKeys.add(rowKey);
            }
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

        updateModify(true);
    }

    @Override
    public boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        LinkedHashMap<HashMap<GPropertyDraw, GGroupObjectValue>, Boolean> setOrders = new LinkedHashMap<>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet())
            setOrders.put(getMinColumnKey(entry.getKey()), entry.getValue());
        return sortableHeaderManager.changeOrders(groupObject, setOrders, alreadySet);
    }

    private HashMap<GPropertyDraw, GGroupObjectValue> getMinColumnKey(GPropertyDraw property) {
        int ind = getPropertyIndex(property, null);
        HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<>();
        key.put(property, ind == -1 ? GGroupObjectValue.EMPTY : getColumnKey(ind));
        return key;
    }

    public GGroupObjectValue getSelectedColumnKey() {
        return getColumnKey(getCurrentCellContext());
    }

    @Override
    public GGroupObjectValue getColumnKey(Context context) {
        return getColumnKey(context.getColumn());
    }

    public GGroupObjectValue getColumnKey(int column) {
        return getGridColumn(column).columnKey;
    }

    @Override
    public boolean isReadOnly(Context context) {
        GPropertyDraw property = getProperty(context);
        if (property != null && !property.isReadOnly()) {
            GridDataRecord rowRecord = (GridDataRecord) context.getRowValue();
            GridColumn column = (GridColumn) getColumn(context.getColumn());
            return column == null || rowRecord == null || rowRecord.isReadonly(column.columnID);
        }
        return true;
    }

    public Object getValueAt(Context context) {
        Column column = getColumn(context.getColumn());
        Object rowValue = context.getRowValue();
        if (column == null || rowValue == null) {
            return null;
        }
        return column.getValue(rowValue);
    }
    
    public Object getValueAt(int row, int col) {
        Column column = getColumn(col);
        GridDataRecord rowValue = getRowValue(row);
        if (column == null || rowValue == null) {
            return null;
        }
        return column.getValue(rowValue);
    }

    private int getMaxColumnsCount(List<List<String>> table) {
        if(table.isEmpty())
            return 0;
        int tableColumns = 0;
        for(List<String> row : table) {
            int rowColumns = row.size();
            if(rowColumns > tableColumns)
                tableColumns = rowColumns;
        }
        return tableColumns;
    }

    @Override
    public void pasteData(final List<List<String>> table) {
        final int selectedColumn = getSelectedColumn();

        if (selectedColumn == -1 || table.isEmpty()) {
            return;
        }

        final int tableColumns = getMaxColumnsCount(table);

        boolean singleC = table.size() == 1 && tableColumns == 1;

        if (!singleC) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.formGridSureToPasteMultivalue(), false, new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    if (chosenOption == DialogBoxHelper.OptionType.YES) {
                        int columnsToInsert = Math.min(tableColumns, getColumnCount() - selectedColumn);

                        final ArrayList<GPropertyDraw> propertyList = new ArrayList<>();
                        final ArrayList<GGroupObjectValue> columnKeys = new ArrayList<>();
                        for (int i = 0; i < columnsToInsert; i++) {
                            GPropertyDraw propertyDraw = getProperty(selectedColumn + i);
                            propertyList.add(propertyDraw);
                            columnKeys.add(getColumnKey(selectedColumn + i));
                        }

                        form.pasteExternalTable(propertyList, columnKeys, table);
                    }
                }
            });
        } else if (!table.get(0).isEmpty()) {
            GGroupObjectValue fullKey = new GGroupObjectValueBuilder(getCurrentKey(), getColumnKey(selectedColumn)).toGroupObjectValue();
            form.pasteSingleValue(getProperty(selectedColumn), fullKey, table.get(0).get(0));
        }
    }

    @Override
    public void onResize() {
        super.onResize();

        if (isVisible()) {
            int tableHeight = getViewportHeight();
            if (tableHeight == 0) {
                return;
            }
            TableRowElement rowElement = getChildElement(getSelectedRow());
            if (rowElement != null) {
                int rowHeight = rowElement.getClientHeight();
                Integer currentPageSize = currentGridPreferences.pageSize;
                int newPageSize = currentPageSize != null ? currentPageSize : (tableHeight / rowHeight + 1);
                if (newPageSize != pageSize) {
                    form.changePageSizeAfterUnlock(groupObject, newPageSize);
                    pageSize = newPageSize;
                    setPageIncrement(pageSize - 1);
                }
            }
        }
    }

    @Override
    protected boolean useQuickSearchInsteadOfQuickFilter() {
        return groupObject.grid.quickSearch;
    }

    @Override
    public void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey) {
        groupObjectController.quickEditFilter(event, filterProperty, columnKey);
    }

    @Override
    protected void quickSearch(Event editEvent) {
        if (getRowCount() > 0 && getColumnCount() > 0) {
            

            char ch = (char) editEvent.getCharCode();

            double currentTime = Duration.currentTimeMillis();
            lastQuickSearchPrefix = (lastQuickSearchTime + QUICK_SEARCH_MAX_DELAY < currentTime) ? valueOf(ch) : (lastQuickSearchPrefix + ch);

            int searchColumn = 0;
            if (!sortableHeaderManager.getOrderDirections().isEmpty()) {
                for (int i = 0; i < getColumnCount(); ++i) {
                    if (sortableHeaderManager.getSortDirection(i) != null) {
                        searchColumn = i;
                        break;
                    }
                }
            }

            for (int i = 0; i < getRowCount(); ++i) {
                if (isRowWithinBounds(i)) {
                    Object value = getValueAt(i, searchColumn);
                    if (value != null && value.toString().regionMatches(true, 0, lastQuickSearchPrefix, 0, lastQuickSearchPrefix.length())) {
                        setSelectedRow(i);
                        break;
                    }
                }
            }

            lastQuickSearchTime = currentTime;
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        if (propertyDraw == null) {
            return;
        }

        int ind = getPropertyIndex(propertyDraw, null);
        if (ind != -1) {
            setSelectedColumn(ind, false);
        }
    }

    public void setValueAt(Context context, Object value) {
        GridDataRecord rowRecord = (GridDataRecord) context.getRowValue();
        GridColumn column = (GridColumn) getColumn(context.getColumn());

        if (column != null && rowRecord != null) {
            column.setValue(rowRecord, value);

            setRowValue(context.getIndex(), rowRecord);
            redrawColumns(singleton(column), false);
        }
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
            LinkedHashMap<GPropertyDraw, Boolean> orders = groupObjectController.getUserOrders();
            if(orders == null)
                orders = groupObjectController.getDefaultOrders();
            changePropertyOrders(orders, false);
        }
    }

    private void doResetPreferences(final boolean forAllUsers, final boolean completeReset, final ErrorHandlingCallback<ServerResponseResult> callback) {
        GGridUserPreferences prefs;
        if (forAllUsers) {
            prefs = completeReset ? null : userGridPreferences;
        } else {
            // assert !completeReset;
            prefs = generalGridPreferences;
        }
        
        form.saveUserPreferences(currentGridPreferences, forAllUsers, completeReset, getHiddenProps(prefs), new ErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void failure(Throwable caught) {
                resetCurrentPreferences(false);
                callback.failure(caught);
            }

            @Override
            public void success(ServerResponseResult result) {
                if (forAllUsers) {
                    generalGridPreferences.resetPreferences();
                    if (completeReset) {
                        userGridPreferences.resetPreferences();
                    }
                } else {
                    userGridPreferences.resetPreferences();
                }
                resetCurrentPreferences(false);
                callback.success(result);
            }
        });
    }
    
    public void resetPreferences(boolean forAll, boolean complete, final ErrorHandlingCallback<ServerResponseResult> callback) {
        currentGridPreferences.resetPreferences();

        if (forAll) {
            doResetPreferences(true, complete, callback);
        } else if (!properties.isEmpty()) {
            doResetPreferences(false, false, callback);
        }
    }

    public void saveCurrentPreferences(final boolean forAllUsers, final ErrorHandlingCallback<ServerResponseResult> callback) {
        currentGridPreferences.setHasUserPreferences(true);

        if (!properties.isEmpty()) {
            GGridUserPreferences prefs;
            if (forAllUsers) {
                prefs = userGridPreferences.hasUserPreferences() ? userGridPreferences : currentGridPreferences;
            } else {
                prefs = currentGridPreferences;
            }

            form.saveUserPreferences(currentGridPreferences, forAllUsers, false, getHiddenProps(prefs), new ErrorHandlingCallback<ServerResponseResult>() {
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

    private String[] getHiddenProps(final GGridUserPreferences preferences) {
        List<String> result = new ArrayList<>();
        if (preferences != null && preferences.hasUserPreferences()) {
            for (GPropertyDraw propertyDraw : preferences.getColumnUserPreferences().keySet()) {
                Boolean userHide = preferences.getColumnPreferences(propertyDraw).userHide;
                if (userHide != null && userHide) {
                    result.add(propertyDraw.propertyFormName);
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }
    
    public void refreshUPHiddenProps(String[] propSids) {
        assert groupObject != null; // при null нету таблицы, а значит и настроек
        form.refreshUPHiddenProps(groupObject.getSID(), propSids);
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

    public String getUserCaption(GPropertyDraw property) {
        return currentGridPreferences.getUserCaption(property);
    }

    public String getUserPattern(GPropertyDraw property) {
        return currentGridPreferences.getUserPattern(property);
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

    public void setUserPageSize(Integer pageSize) {
        currentGridPreferences.pageSize = pageSize;
    }

    public void setUserHeaderHeight(Integer headerHeight) {
        currentGridPreferences.headerHeight = headerHeight;
    }
    
    public void setUserFont(GFont userFont) {
        currentGridPreferences.font = userFont;
    }

    public void setUserHide(GPropertyDraw property, Boolean userHide) {
        currentGridPreferences.setUserHide(property, userHide);
    }

    public void setColumnSettings(GPropertyDraw property, String caption, String pattern, Integer order, Boolean hide) {
        currentGridPreferences.setColumnSettings(property, caption, pattern, order, hide);
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

    private class GridColumn extends GridPropertyColumn {
        private final int columnID;

        public final GGroupObjectValue columnKey;

        public GridColumn(GPropertyDraw property, GGroupObjectValue columnKey) {
            super(property);

            this.columnID = nextColumnID++;

            this.columnKey = columnKey;
        }

        public void setValue(GridDataRecord record, Object value) {
            record.setValue(columnID, value);
        }
        public Object getValue(GridDataRecord record) {
            return record.getValue(columnID);
        }
    }

    public class GridTableSelectionHandler extends GridPropertyTableSelectionHandler<GridDataRecord> {
        public GridTableSelectionHandler(DataGrid<GridDataRecord> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(Event event) {

            assert BrowserEvents.KEYDOWN.equals(event.getType());

            int keyCode = event.getKeyCode();
            boolean ctrlPressed = event.getCtrlKey();
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
    
    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList) {
        LinkedHashMap<GPropertyDraw, Boolean> userOrders = new LinkedHashMap<>();
        Collections.sort(propertyDrawList, getUserSortComparator());
        for (GPropertyDraw property : propertyDrawList) {
            Boolean userOrderSort;
            if (getUserSort(property) != null && (userOrderSort = getUserAscendingSort(property)) != null) {
                userOrders.put(property, userOrderSort);
            }
        }
        return userOrders;
    }
    
}
