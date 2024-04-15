package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableFooter;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GGridTable extends GGridPropertyTable<GridDataRecord> implements GTableView {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final double QUICK_SEARCH_MAX_DELAY = 2000;

    private ArrayList<GPropertyDraw> properties = new ArrayList<>();
    // map for fast incremental update, essentially needed for working with group-to-columns (columnKeys)
    private NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap = new NativeSIDMap<>();

    private ArrayList<ArrayList<Integer>> bindingEventIndices = new ArrayList<>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<>();

    private NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> values = new NativeSIDMap<>();
    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> showIfs = new NativeSIDMap<>();
    private NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> readOnlyValues = new NativeSIDMap<>();
    private NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> loadings = new NativeSIDMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private NativeSIDMap<GPropertyDraw, Boolean> updatedProperties = new NativeSIDMap<>();
    private NativeSIDMap<GPropertyDraw, ArrayList<GGroupObjectValue>> columnKeys = new NativeSIDMap<>();

    private boolean rowsUpdated = false;
    private boolean dataUpdated = false;

    private GGroupObject groupObject;

    private GGridController groupObjectController;
    
    private GGridUserPreferences generalGridPreferences;
    private GGridUserPreferences userGridPreferences;
    private GGridUserPreferences currentGridPreferences;

    private int nextColumnID = 0;

    private int pageSize = 50;

    private String lastQuickSearchPrefix = "";

    private double lastQuickSearchTime = 0;

    private long setRequestIndex;

    public GGridTable(GFormController iform, GGridController igroupController, TableContainer tableContainer, GGridUserPreferences[] iuserPreferences) {
        super(iform, igroupController.groupObject, tableContainer, null);

        this.groupObjectController = igroupController;
        this.groupObject = igroupController.groupObject;

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
            if (selectedRecord != null)
                form.changeGroupObjectLater(groupObject, selectedRecord.getKey());
        });

        sortableHeaderManager = new GGridSortableHeaderManager<Map<GPropertyDraw, GGroupObjectValue>>(this, false) {
            @Override
            protected void orderChanged(Map<GPropertyDraw, GGroupObjectValue> columnKey, GOrder modiType) {
                form.changePropertyOrder(columnKey.keySet().iterator().next(), columnKey.values().iterator().next(), modiType);
            }

            @Override
            protected void ordersSet(GGroupObject groupObject, LinkedHashMap<Map<GPropertyDraw, GGroupObjectValue>, Boolean> orders) {
                List<Integer> propertyList = new ArrayList<>();
                List<GGroupObjectValue> columnKeyList = new ArrayList<>();
                List<Boolean> orderList = new ArrayList<>();
                for(Map.Entry<Map<GPropertyDraw, GGroupObjectValue>, Boolean> entry : orders.entrySet()) {
                    propertyList.add(entry.getKey().keySet().iterator().next().ID);
                    columnKeyList.add(entry.getKey().values().iterator().next());
                    orderList.add(entry.getValue());
                }
                form.setPropertyOrders(groupObject, propertyList, columnKeyList, orderList);
                
                headersChanged();
            }

            @Override
            protected Map<GPropertyDraw, GGroupObjectValue> getColumnKey(int column) {
                GridColumn gridColumn = getGridColumn(column);
                HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<>();
                key.put(gridColumn.property, gridColumn.columnKey);
                return key;
            }
        };
    }

    public void update(Boolean updateState) {
        updateModify(false);
        if(updateState != null)
            GStateTableView.setOpacity(updateState, getTableElement());
    }

    private void updateModify(boolean modifyGroupObject) {
        updateColumns();

        updateCaptions();
        updateFooters();

        updateRows(modifyGroupObject);
        
        if (modifyGroupObject) {
            // обновим данные в колонках. при асинхронном удалении ряда можем не получить подтверждения от сервера - придётся вернуть строку
            for (GPropertyDraw property : properties) {
                updatedProperties.put(property, TRUE);
            }
            dataUpdated = true;
        }

        updateData();
    }

    private void updateRows(boolean modifyGroupObject) {
        if (rowsUpdated) {
            checkUpdateCurrentRow();

            updateGridRows(modifyGroupObject);

            rowsChanged();

            rowsUpdated = false;
        }

        updateCurrentRow();
    }

    // incremental records update
    private void updateGridRows(boolean modifyGroupObject) {
        int currentSize = rows.size();
        int newSize = rowKeys.size();

        if (currentSize > newSize) {
            if (modifyGroupObject) {
                for (int i = rows.size() - 1; i >= 0; i--) {
                    GridDataRecord record = rows.get(i);
                    if (!rowKeys.contains(record.getKey())) {
                        // not so sure about this incDeleteRows, because it can be called during dispatching process (when canceling async changes), and not at the beggining of the event loop
                        tableBuilder.incDeleteRows(tableWidget.getSection(), i, i + 1);
                        rows.remove(i);
                        incUpdateRowIndices(i, -1);
                        if(renderedSelectedRow == i)
                            selectedRowChanged();
                    }
                }
            } else {
                rows.removeRange(newSize, currentSize);
            }
        } else if (currentSize < newSize) {
            for (int i = currentSize; i < newSize; i++)
                rows.add(new GridDataRecord(i));
        }

        for (int i = 0; i < newSize; i++) {
            GGroupObjectValue rowKey = rowKeys.get(i);
            GridDataRecord record = rows.get(i);
            record.setKey(rowKey);

            record.setRowBackground(rowBackgroundValues.get(rowKey));
            record.setRowForeground(rowForegroundValues.get(rowKey));
        }
    }

    private void updateColumns() {
        if (columnsUpdated) {
            List<GPropertyDraw> orderedVisibleProperties = getOrderedVisibleProperties(properties);

            //разбиваем на группы свойств, которые будут идти чередуясь для каждого ключа из групп в колонках ("шахматка")
            NativeStringMap<NativeHashMap<List<GGroupObject>, Integer>> columnGroupsIndices = new NativeStringMap<>();
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

            int currentIndex = 0;
            NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> newColumnsMap = new NativeSIDMap<>();
            for (int i = 0; i < columnGroups.size(); i++) {
                for (GGroupObjectValue columnKey : columnGroupsColumnKeys.get(i)) {
                    for (GPropertyDraw property : columnGroups.get(i)) {
                        if (checkShowIf(property, columnKey))
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

                        // we do update captions and columns together for optimization purposes
                        updatePropertyHeader(columnKey, property, currentIndex);
                        updatePropertyFooter(columnKey, property, currentIndex);

                        property.setUserPattern(getUserPattern(property));

                        putToColumnsMap(newColumnsMap, property, columnKey, column);

                        currentIndex++;
                    }
                }
            }
            // removing old columns
            columnsMap.foreachValue(columnsCollection -> columnsCollection.foreachValue(column -> {
                removeGridColumn(column);
            }));
            columnsMap = newColumnsMap;

            updateLayoutWidth();

            columnsChanged();

            columnsUpdated = false;
            captionsUpdated = false;
            footersUpdated = false;
        }
    }

    private boolean checkShowIf(GPropertyDraw property, GGroupObjectValue columnKey) {
        // property is hidden
        NativeHashMap<GGroupObjectValue, PValue> propShowIfs = showIfs.get(property);
        return propShowIfs != null && !PValue.getBooleanValue(propShowIfs.get(columnKey));
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

    @Override
    protected GPropertyDraw getColumnPropertyDraw(int i) {
        return getGridColumn(i).property;
    }

    public boolean containsProperty(GPropertyDraw property) {
        return getGridColumn(property, null) != null;
    }

    public List<GPropertyDraw> getOrderedVisibleProperties(List<GPropertyDraw> propertiesList) {
        List<GPropertyDraw> result = new ArrayList<>();

        for (GPropertyDraw property : propertiesList) {
            if (!property.hide) {
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
                } else {
                    result.add(property);
                }
            }
        }

        if (hasUserPreferences()) {
            result.sort(getCurrentPreferences().getUserOrderComparator());
        }
        return result;
    }

    public void columnsPreferencesChanged() {
        columnsUpdated = true;
        dataUpdated = true;

        updateColumns();
        updateData();
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

    @Override
    protected Boolean isResizeOverflow() {
        return groupObject.grid.resizeOverflow;
    }

    public GSize getHeaderHeight() {
        Integer headerHeight = currentGridPreferences.headerHeight;
        if (headerHeight != null && headerHeight >= 0)
            return GSize.getResizeSize(headerHeight);

        return groupObject.grid.getHeaderHeight();
    }
    
    public GFont getDesignFont() {
        return groupObject.grid.font;
    }

    public static void putToColumnsMap(NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column, GridColumn value) {
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap == null) {
            columnsMap.put(row, rowMap = new NativeHashMap<>());
        }
        rowMap.put(column, value);
    }

    public static GridColumn getFromColumnsMap(NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column) {
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap != null) {
            return rowMap.get(column);
        }
        return null;
    }

    public static GridColumn removeFromColumnsMap(NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, GridColumn>> columnsMap, GPropertyDraw row, GGroupObjectValue column) {
        GridColumn result = null;
        NativeHashMap<GGroupObjectValue, GridColumn> rowMap = columnsMap.get(row);
        if (rowMap != null) {
            result = rowMap.remove(column);
        }
        return result;
    }

    private void updateData() {
        if (dataUpdated) {
            boolean firstRow = true;
            ArrayList<GridColumn> updatedColumns = new ArrayList<>();
            for (final GridDataRecord record : rows) {
                final GGroupObjectValue rowKey = record.getKey();
                final boolean fFirstRow = firstRow;
                updatedProperties.foreachKey(property -> {
                    NativeHashMap<GGroupObjectValue, PValue> propValues = values.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propLoadings = loadings.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propReadOnly = readOnlyValues.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyValueElementClasses = cellValueElementClasses.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyFonts = cellFontValues.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyBackgrounds = cellBackgroundValues.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyForegrounds = cellForegroundValues.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyPlaceholders = placeholders.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyPatterns = patterns.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyRegexps = regexps.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyRegexpMessages = regexpMessages.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> propertyValueTooltips = valueTooltips.get(property);
                    NativeHashMap<GGroupObjectValue, PValue> actionImages = property.isAction() ? cellImages.get(property) : null;

                    for (GGroupObjectValue columnKey : columnKeys.get(property)) {
                        NativeHashMap<GGroupObjectValue, GridColumn> propertyColumns = columnsMap.get(property);
                        GridColumn column = propertyColumns == null ? null : propertyColumns.get(columnKey);
                        // column == null, can be null if hidden with showif
                        if (column != null) {
                            if(fFirstRow)
                                updatedColumns.add(column);
                            GGroupObjectValue fullKey = GGroupObjectValue.getFullKey(rowKey, columnKey);

                            column.setValue(record, propValues.get(fullKey));
                            column.setLoading(record, propLoadings != null && PValue.getBooleanValue(propLoadings.get(fullKey)));
                            record.setReadOnly(column.columnSID, propReadOnly == null ? null : PValue.get3SBooleanValue(propReadOnly.get(fullKey)));
                            PValue valueElementClass = propertyValueElementClasses == null ? null : propertyValueElementClasses.get(fullKey);
                            record.setValueElementClass(column.columnSID, valueElementClass == null ? property.valueElementClass : PValue.getClassStringValue(valueElementClass));
                            PValue font = propertyFonts == null ? null : propertyFonts.get(fullKey);
                            record.setFont(column.columnSID, font == null ? property.font : PValue.getFontValue(font));
                            PValue background = propertyBackgrounds == null ? null : propertyBackgrounds.get(fullKey);
                            record.setBackground(column.columnSID, background == null ? property.getBackground() : PValue.getColorStringValue(background));
                            PValue foreground = propertyForegrounds == null ? null : propertyForegrounds.get(fullKey);
                            record.setForeground(column.columnSID, foreground == null ? property.getForeground() : PValue.getColorStringValue(foreground));
                            PValue placeholder = propertyPlaceholders == null ? null : propertyPlaceholders.get(fullKey);
                            record.setPlaceholder(column.columnSID, placeholder == null ? property.placeholder : PValue.getStringValue(placeholder));
                            PValue pattern = propertyPatterns == null ? null : propertyPatterns.get(fullKey);
                            record.setPattern(column.columnSID, pattern == null ? property.getPattern() : PValue.getStringValue(pattern));
                            PValue regexp = propertyRegexps == null ? null : propertyRegexps.get(fullKey);
                            record.setRegexp(column.columnSID, regexp == null ? property.regexp : PValue.getStringValue(regexp));
                            PValue regexpMessage = propertyRegexpMessages == null ? null : propertyRegexps.get(fullKey);
                            record.setRegexpMessage(column.columnSID, regexpMessage == null ? property.regexpMessage : PValue.getStringValue(regexpMessage));
                            PValue valueTooltip = propertyValueTooltips == null ? null : propertyValueTooltips.get(fullKey);
                            record.setValueTooltip(column.columnSID, valueTooltip == null ? property.valueTooltip : PValue.getStringValue(valueTooltip));
                            record.setImage(column.columnSID, actionImages == null ? null : PValue.getImageValue(actionImages.get(fullKey)));
                        }
                    }
                });
                firstRow = false;
            }

            dataChanged(updatedColumns);

            updatedProperties.clear();
            dataUpdated = false;
        }
    }

    public GridColumn getGridColumn(int column) {
        return (GridColumn) super.getGridColumn(column);
    }

    public GridColumn getGridColumn(Cell cell) {
        return (GridColumn) cell.getColumn();
    }
    public GridDataRecord getGridRow(Cell cell) {
        return (GridDataRecord) cell.getRow();
    }

    private GridColumn insertGridColumn(int index, GPropertyDraw property, GGroupObjectValue columnKey) {
        GridColumn column = new GridColumn(property, columnKey);
        GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, null, null, null, null, column.isSticky());
        GGridPropertyTableFooter footer = noFooters ? null : new GGridPropertyTableFooter(this, property, null, null, column.isSticky(), form);

        insertColumn(index, column, header, footer);

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

    public GGroupObjectValue getCurrentColumnKey() {
        GGroupObjectValue property = getSelectedColumnKey();
        if (property == null && getColumnCount() > 0) {
            property = getColumnKey(0);
        }
        return property;
    }

    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        GridDataRecord selectedRecord = getSelectedRowValue();
        if (selectedRecord != null) {
            GridColumn column = getGridColumn(property, columnKey);
            if(column != null)
                return column.getValue(selectedRecord);
        }

        return null;
    }

    @Override
    public List<Pair<lsfusion.gwt.client.form.view.Column, String>> getFilterColumns() {
        List<Pair<lsfusion.gwt.client.form.view.Column, String>> result = new ArrayList<>();
        for(GPropertyDraw property : properties)
            for(GGroupObjectValue columnKey : columnKeys.get(property))
                result.add(getFilterColumn(property, columnKey));
        return result;
    }

    public void removeProperty(GPropertyDraw property) {
        int index = properties.indexOf(property);
        properties.remove(index);
        form.removePropertyBindings(bindingEventIndices.remove(index));

        values.remove(property);
        loadings.remove(property);
        columnKeys.remove(property);

        columnsUpdated = true;
    }

    @Override
    public void updateLoadings(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> loadings) {
        NativeHashMap<GGroupObjectValue, PValue> loadingsMap = this.loadings.get(property);
        if (loadingsMap == null) {
            loadingsMap = new NativeHashMap<>();
            this.loadings.put(property, loadingsMap);
        }
        loadingsMap.putAll(loadings);

        updatedProperties.put(property, TRUE);
        dataUpdated = true;
    }

    public void updateProperty(final GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        if(!updateKeys) {
            if (!properties.contains(property)) {
                int newColumnIndex = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);

                properties.add(newColumnIndex, property);
                bindingEventIndices.add(newColumnIndex, form.addPropertyBindings(property, event -> onBinding(property, event), getWidget()));

                this.columnKeys.put(property, columnKeys);

                columnsUpdated = true;
            } else {
                ArrayList<GGroupObjectValue> oldValues = this.columnKeys.get(property);
                if (!columnKeys.equals(oldValues)) {
                    this.columnKeys.put(property, columnKeys);
                    columnsUpdated = true;
                }
            }
        }

        NativeHashMap<GGroupObjectValue, PValue> valuesMap = this.values.get(property);
        if (updateKeys && valuesMap != null) {
            valuesMap.putAll(values);
        } else {
            NativeHashMap<GGroupObjectValue, PValue> pvalues = new NativeHashMap<>();
            pvalues.putAll(values);
            this.values.put(property, pvalues);
        }
        
        updatedProperties.put(property, TRUE);
        dataUpdated = true;
    }

    public void onBinding(GPropertyDraw property, Event event) {
        int column = getGridColumnIndex(property, null);
        if(column >= 0 && getSelectedRow() >= 0) {
            form.onPropertyBinding(event, getSelectedEditContext(column));
        }
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.rowKeys = keys;

        rowsUpdated = true;
    }

    public void updateShowIfValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        NativeHashMap<GGroupObjectValue, PValue> oldValues = showIfs.get(propertyDraw);
        if (!nullEquals(oldValues, values)) {
            showIfs.put(propertyDraw, values);
            columnsUpdated = true;
        }
    }

    public void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        readOnlyValues.put(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateCellValueElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellValueElementClasses(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateCellFontValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellFontValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updatePlaceholderValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updatePlaceholderValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updatePatternValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updatePatternValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateTooltipValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        captionsUpdated = true;
    }

    @Override
    public void updateValueTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateValueTooltipValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        updatedProperties.put(propertyDraw, TRUE);
        dataUpdated = true;
    }

    @Override
    public void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateImageValues(propertyDraw, values);
        if(propertyDraw.isAction()) {
            updatedProperties.put(propertyDraw, TRUE);
            dataUpdated = true;
        } else {
            captionsUpdated = true;
        }
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRowBackgroundValues(values);
        rowsUpdated = true;
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRowForegroundValues(values);
        rowsUpdated = true;
    }

    public GPropertyDraw getProperty(int column) {
        return getGridColumn(column).property;
    }

    public GPropertyDraw getProperty(Cell cell) {
        return getGridColumn(cell).property;
    }

    public int getGridColumnIndex(GPropertyDraw property, GGroupObjectValue columnKey) {
        for(int i=0,size=getColumnCount();i<size;i++) {
            GridColumn gridColumn = getGridColumn(i);
            if (property == gridColumn.property && (columnKey == null || columnKey.equals(gridColumn.columnKey))) {
                return i;
            }
        }
        return -1;
    }

    public GridColumn getGridColumn(GPropertyDraw property, GGroupObjectValue columnKey) {
        for(int i=0,size=getColumnCount();i<size;i++) {
            GridColumn gridColumn = getGridColumn(i);
            if (property == gridColumn.property && (columnKey == null || columnKey.equals(gridColumn.columnKey))) {
                return gridColumn;
            }
        }
        return null;
    }

    public void modifyGroupObject(GGroupObjectValue rowKey, boolean add, int position) {
        boolean currentKeyChanged = false;
        GGroupObjectValue currentKey = null;
        if (add) {
            if (position >= 0 && position <= rowKeys.size()) {
                rowKeys.add(position, rowKey);
            } else {
                rowKeys.add(rowKey);
            }
            currentKey = rowKey;
            currentKeyChanged = true;
        } else {
            if (GwtClientUtils.nullEquals(getSelectedKey(), rowKey) && rowKeys.size() > 0) {
                if (rowKeys.size() > 1) {
                    int index = rowKeys.indexOf(rowKey);
                    index = index == rowKeys.size() - 1 ? index - 1 : index + 1;
                    currentKey = rowKeys.get(index);
                }
                currentKeyChanged = true;
            }
            rowKeys.remove(rowKey);
        }
        setKeys(rowKeys);
        if(currentKeyChanged)
            setCurrentKey(currentKey);

        updateModify(true);
    }

    @Override
    public boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        LinkedHashMap<HashMap<GPropertyDraw, GGroupObjectValue>, Boolean> setOrders = new LinkedHashMap<>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet())
            setOrders.put(getMinColumnKey(entry.getKey()), entry.getValue());
        return sortableHeaderManager.changeOrders(groupObject, setOrders, alreadySet);
    }

    @Override
    public void changePropertyOrders(LinkedHashMap<GPropertyDraw, GOrder> orders) {
        for (Map.Entry<GPropertyDraw, GOrder> entry : orders.entrySet()) {
            sortableHeaderManager.changeOrder(getMinColumnKey(entry.getKey()), entry.getValue());
        }
        if (!orders.isEmpty()) {
            headersChanged();
        }
    }

    private HashMap<GPropertyDraw, GGroupObjectValue> getMinColumnKey(GPropertyDraw property) {
        GridColumn column = getGridColumn(property, null);
        HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<>();
        key.put(property, column == null ? GGroupObjectValue.EMPTY : column.columnKey);
        return key;
    }

    @Override
    public GGroupObjectValue getColumnKey(Cell cell) {
        return getGridColumn(cell).columnKey;
    }

    @Override
    public GGroupObjectValue getColumnKey(int column) {
        return getGridColumn(column).columnKey;
    }

    @Override
    public Boolean isReadOnly(Cell cell) {
        GPropertyDraw property = getProperty(cell);
        if (property != null && property.isReadOnly() == null) {
            GridDataRecord rowRecord = getGridRow(cell);
            GridColumn column = getGridColumn(cell);
            if(column != null && rowRecord != null)
                return rowRecord.isReadonly(column.columnSID);
        }
        return false;
    }

    @Override
    public GGroupObjectValue getRowKey(Cell editCell) {
        return getGridRow(editCell).getKey();
    }

    public PValue getValueAt(int row, int col) {
        GridColumn column = getGridColumn(col);
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
    public void pasteData(Cell cell, Element renderElement, final List<List<String>> table) {
        final int tableColumns = getMaxColumnsCount(table);
        final int selectedColumn = getSelectedColumn();
        if (table.size() > 1 || tableColumns > 1) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.formGridSureToPasteMultivalue(), new PopupOwner(getPopupOwnerWidget()), chosenOption -> {
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
            });
            return;
        }
        super.pasteData(cell, renderElement, table);
    }

    @Override
    public void onResize() {
        super.onResize();

        if (getWidget().isVisible()) {
            int tableHeight = getViewportClientHeight();
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
                    String string = PValue.getStringValue(getValueAt(i, searchColumn));
                    if (string != null && string.regionMatches(true, 0, lastQuickSearchPrefix, 0, lastQuickSearchPrefix.length())) {
                        selectionHandler.changeRow(i, FocusUtils.Reason.KEYMOVENAVIGATE);
                        break;
                    }
                }
            }

            lastQuickSearchTime = currentTime;
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        activateColumn(getGridColumnIndex(propertyDraw, null));
    }

    @Override
    public void focus(FocusUtils.Reason reason) {
        GTableView.super.focus(reason);
    }

    // editing set value (in EditContext), changes model and value itself
    public void setValueAt(Cell cell, PValue value) {
        GridColumn gridColumn = getGridColumn(cell);

        gridColumn.setValue(getGridRow(cell), value); // updating inner model

        values.get(gridColumn.property).put(getRowKey(cell), value); // updating outer model - controller
    }

    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        GGroupObjectValue propertyColumnKey = property.filterColumnKeys(fullCurrentKey);
        if(propertyColumnKey == null)
            return null;
        return setLoadingValueAt(property, groupObject.filterRowKeys(fullCurrentKey), getGridColumnIndex(property, propertyColumnKey), propertyColumnKey, value);
    }

    @Override
    public void setLoadingAt(Cell cell) {
        GridColumn column = getGridColumn(cell);

        column.setLoading(getGridRow(cell), true); // updating inner model

        // updating outer model - controller
        NativeHashMap<GGroupObjectValue, PValue> loadingMap = loadings.get(column.property);
        if(loadingMap == null) {
            loadingMap = new NativeHashMap<>();
            loadings.put(column.property, loadingMap);
        }
        loadingMap.put(getRowKey(cell), PValue.getPValue(true));
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

    private void doResetPreferences(final boolean forAllUsers, final boolean completeReset, final AsyncCallback<ServerResponseResult> callback) {
        GGridUserPreferences prefs;
        if (forAllUsers) {
            prefs = completeReset ? null : userGridPreferences;
        } else {
            // assert !completeReset;
            prefs = generalGridPreferences;
        }
        
        form.saveUserPreferences(currentGridPreferences, forAllUsers, completeReset, getHiddenProps(prefs), new AsyncCallback<ServerResponseResult>() {
            @Override
            public void onFailure(Throwable caught) {
                resetCurrentPreferences(false);
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(ServerResponseResult result) {
                if (forAllUsers) {
                    generalGridPreferences.resetPreferences();
                    if (completeReset) {
                        userGridPreferences.resetPreferences();
                    }
                } else {
                    userGridPreferences.resetPreferences();
                }
                resetCurrentPreferences(false);
                callback.onSuccess(result);
            }
        });
    }
    
    public void resetPreferences(boolean forAll, boolean complete, final AsyncCallback<ServerResponseResult> callback) {
        currentGridPreferences.resetPreferences();

        if (forAll) {
            doResetPreferences(true, complete, callback);
        } else if (!properties.isEmpty()) {
            doResetPreferences(false, false, callback);
        }
    }

    public void saveCurrentPreferences(final boolean forAllUsers, final AsyncCallback<ServerResponseResult> callback) {
        currentGridPreferences.setHasUserPreferences(true);

        if (!properties.isEmpty()) {
            GGridUserPreferences prefs;
            if (forAllUsers) {
                prefs = userGridPreferences.hasUserPreferences() ? userGridPreferences : currentGridPreferences;
            } else {
                prefs = currentGridPreferences;
            }

            form.saveUserPreferences(currentGridPreferences, forAllUsers, false, getHiddenProps(prefs), new AsyncCallback<ServerResponseResult>() {
                @Override
                public void onSuccess(ServerResponseResult result) {
                    if (forAllUsers) {
                        generalGridPreferences = new GGridUserPreferences(currentGridPreferences);
                        resetCurrentPreferences(false);
                    } else {
                        userGridPreferences = new GGridUserPreferences(currentGridPreferences);
                    }
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    resetCurrentPreferences(false);
                    callback.onFailure(caught);
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

    protected Double getUserFlex(GPropertyDraw property) {
        return currentGridPreferences.getUserFlex(property);
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

    protected void setUserFlex(GPropertyDraw property, Double value) {
        currentGridPreferences.setUserFlex(property, value);
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
        private final String columnSID;

        public final GPropertyDraw property;
        public final GGroupObjectValue columnKey;

        public GridColumn(GPropertyDraw property, GGroupObjectValue columnKey) {
            this.columnSID = "" + (nextColumnID++);

            this.property = property;
            this.columnKey = columnKey;
        }

        @Override
        public String getNativeSID() {
            return columnSID;
        }

        @Override
        public boolean isCustomRenderer(RendererType rendererType) {
            return property.getCellRenderer(rendererType).isCustomRenderer();
        }

        @Override
        public boolean isFocusable() {
            return GGridTable.this.isFocusable(property);
        }

        @Override
        public boolean isSticky() {
            return property.sticky;
        }

        public void setValue(GridDataRecord record, PValue value) {
            record.setValue(columnSID, value);
        }
        public PValue getValue(GridDataRecord record) {
            return record.getValue(columnSID);
        }
        public void setLoading(GridDataRecord record, boolean loading) {
            record.setLoading(columnSID, loading);
        }

        @Override
        protected PValue getValue(GPropertyDraw property, GridDataRecord record) {
            assert this.property == property;
            return getValue(record);
        }
        @Override
        protected boolean isLoading(GPropertyDraw property, GridDataRecord record) {
            assert this.property == property;
            return record.isLoading(columnSID);
        }
        @Override
        protected AppBaseImage getImage(GPropertyDraw property, GridDataRecord record) {
            return record.getImage(columnSID);
        }

        @Override
        protected String getValueElementClass(GPropertyDraw property, GridDataRecord record) {
            return record.getValueElementClass(columnSID);
        }

        @Override
        protected GFont getFont(GPropertyDraw property, GridDataRecord record) {
            return record.getFont(columnSID);
        }
        @Override
        protected String getBackground(GPropertyDraw property, GridDataRecord record) {
            return record.getBackground(columnSID);
        }
        @Override
        protected String getForeground(GPropertyDraw property, GridDataRecord record) {
            return record.getForeground(columnSID);
        }
        @Override
        protected String getPlaceholder(GPropertyDraw property, GridDataRecord record) {
            return record.getPlaceholder(columnSID);
        }
        @Override
        protected String getPattern(GPropertyDraw property, GridDataRecord record) {
            return record.getPattern(columnSID);
        }
        @Override
        protected String getRegexp(GPropertyDraw property, GridDataRecord record) {
            return record.getRegexp(columnSID);
        }
        @Override
        protected String getRegexpMessage(GPropertyDraw property, GridDataRecord record) {
            return record.getRegexpMessage(columnSID);
        }
        @Override
        protected String getValueTooltip(GPropertyDraw property, GridDataRecord record) {
            return record.getValueTooltip(columnSID);
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
            if (keyCode == KeyCodes.KEY_HOME && event.getCtrlKey()) {
                form.scrollToEnd(groupObject, false);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && event.getCtrlKey()) {
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

    @Override
    public int getPageSize() {
        return -1;
    }

    @Override
    protected void scrollToEnd(boolean toEnd) {
        form.scrollToEnd(groupObject, toEnd);
    }
}
