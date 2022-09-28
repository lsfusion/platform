package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.resize.ResizeHelper;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.SimpleTextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.view.ColorUtils.getThemedColor;
import static lsfusion.gwt.client.form.event.GKeyStroke.*;

public abstract class GGridPropertyTable<T extends GridDataRecord> extends GPropertyTable<T> {
    protected boolean columnsUpdated = true; //could be no properties on init
    protected boolean captionsUpdated = false;
    protected boolean footersUpdated = false;

    public static class RangeArrayList<T> extends ArrayList<T> {
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }
    protected RangeArrayList<T> rows = new RangeArrayList<>();

    // we have to keep it until updateDataImpl to have rows order
    // plus what's more important we shouldn't change selectedRow, before update'in rows, otherwise we'll have inconsistent selectedRow - rows state
    protected GGroupObjectValue currentKey;
    protected Integer currentExpandingIndex;

    public void setCurrentKey(GGroupObjectValue currentKey) {
        // we're counting on the checkUpdateCurrentRow to handle that case (otherwise there will be problems with the expandingIndex)
        if(!(GwtClientUtils.nullHashEquals(getSelectedKey(), currentKey) && this.currentKey == null))
            setCurrentKey(currentKey, null);
    }
    public void setCurrentKey(GGroupObjectValue currentKey, Integer currentExpandingIndex) {
        this.currentKey = currentKey;
        this.currentExpandingIndex = currentExpandingIndex;

        this.currentRowUpdated = true;
    }

    @Override
    protected ArrayList<T> getRows() {
        return rows;
    }

    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> propertyCaptions = new NativeSIDMap<>();
    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> propertyFooters = new NativeSIDMap<>();

    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> cellBackgroundValues = new NativeSIDMap<>();
    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> cellForegroundValues = new NativeSIDMap<>();
    protected NativeHashMap<GGroupObjectValue, Object> rowBackgroundValues = new NativeHashMap<>();
    protected NativeHashMap<GGroupObjectValue, Object> rowForegroundValues = new NativeHashMap<>();

    protected NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> cellImages = new NativeSIDMap<>();

    public static String getPropertyCaption(GPropertyDraw property) {
        return getPropertyCaption(null, property, null);
    }
    public static String getPropertyCaption(NativeHashMap<GGroupObjectValue, Object> propCaptions, GPropertyDraw property, GGroupObjectValue columnKey) {
        String caption;
        if (propCaptions != null)
            caption = property.getDynamicCaption(propCaptions.get(columnKey));
        else
            caption = property.getCaption();
        return caption;
    }

    protected GGridPropertyTableHeader getGridHeader(int i) {
        return (GGridPropertyTableHeader) getHeader(i);
    }

    protected GGridPropertyTableFooter getGridFooter(int i) {
        return (GGridPropertyTableFooter) getFooter(i);
    }

    public GGridSortableHeaderManager sortableHeaderManager;
    
    public GFont font;

    public GGridPropertyTable(GFormController form, GGroupObject groupObject, TableContainer tableContainer, GFont font) {
        super(form, groupObject, tableContainer, !groupObject.hasHeaders, !groupObject.hasFooters, false);
        
        this.font = font;

        setTableBuilder(new GGridPropertyTableBuilder<T>(this));

        GFormController.setBindingGroupObject(tableContainer, groupObject);

        // ADD FILTER
        addFilterBinding(new GKeyInputEvent(ADD_USER_FILTER_KEY_STROKE),
                event -> getGroupController().addFilter(event));
        // REPLACE FILTER
        addFilterBinding(new GKeyInputEvent(REPLACE_USER_FILTER_KEY_STROKE),
                event -> getGroupController().replaceFilter(event));
        // REMOVE FILTERS
        GFormController.BindingExec removeFilters = event -> getGroupController().resetFilters();
        addFilterBinding(new GKeyInputEvent(REMOVE_USER_FILTERS_KEY_STROKE),
                removeFilters);
        addFilterBinding(nativeEvent -> {
                    if (GKeyStroke.isEscapeKeyEvent(nativeEvent) && GKeyStroke.isPlainKeyEvent(nativeEvent)) {
                        GAbstractTableController goController = getGroupController();
                        return goController.filter != null && goController.filter.hasConditions();
                    }
                    return false;
                }, removeFilters);
        // AUTO FILTER
        addFilterBinding(GKeyStroke::isPossibleStartFilteringEvent,
                event -> {
                if (useQuickSearchInsteadOfQuickFilter()) {
                    quickSearch(event);
                } else {
                    GPropertyDraw filterProperty = null;
                    GGroupObjectValue filterColumnKey = null;
                    GPropertyDraw currentProperty = getSelectedProperty();
                    if(currentProperty != null && currentProperty.quickFilterProperty != null) {
                        filterProperty = currentProperty.quickFilterProperty;
                        filterColumnKey = GGroupObjectValue.EMPTY;
                        if(currentProperty.columnGroupObjects != null && filterProperty.columnGroupObjects != null && currentProperty.columnGroupObjects.equals(filterProperty.columnGroupObjects)) {
                            filterColumnKey = getSelectedColumnKey();
                        }
                    }

                    quickFilter(event, filterProperty, filterColumnKey);
                }
            });
        GwtClientUtils.setZeroZIndex(getElement());
    }

    public final ResizeHelper resizeHelper = new ResizeHelper() {

        @Override
        public int getChildAbsolutePosition(int index, boolean left) {
            Element element = getHeaderElement(index);
            return left ? element.getAbsoluteLeft() : element.getAbsoluteRight();
        }

        @Override
        public void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement) {
        }

        @Override
        public double resizeChild(int index, int delta) {
            return resizeColumn(index, delta);
        }

        @Override
        public boolean isChildResizable(int index) {
            return true;
        }

        @Override
        public int getChildCount() {
            return getColumnCount();
        }

        @Override
        public boolean isVertical() {
            return false;
        }
    };

    private void addFilterBinding(GInputEvent event, GFormController.BindingExec pressed) {
        addFilterBinding(event::isEvent, pressed);
    }
    private void addFilterBinding(GFormController.BindingCheck event, GFormController.BindingExec pressed) {
        form.addBinding(event, new GBindingEnv(null, null, null, GBindingMode.ONLY, GBindingMode.NO, null, null, null), pressed, tableContainer, groupObject);
    }

    public GFont getFont() {
        return font;
    }

    public int getAutoSize() {
        return getTableBodyElement().getOffsetHeight();
    }
    
    public GGroupObjectValue getSelectedKey() {
        GridDataRecord selectedRowValue = getSelectedRowValue();
        return selectedRowValue != null ? selectedRowValue.getKey() : null;
    }

    protected Integer getSelectedExpandingIndex() {
        GridDataRecord selectedRowValue = getSelectedRowValue();
        return selectedRowValue != null ? selectedRowValue.getExpandingIndex() : null;
    }

    // there is a contract if there are keys there should be current object
    // but for example modifyFormChangesWithChangeCurrentObjectAsyncs removes object change (+ when there are no keys nothing is also send)
    // so we'll put current (last) key to ensure that selected row will match rows collection
    protected void checkUpdateCurrentRow() {
        // assert rowUpdated AND ! it's important to do this before update rows to have relevant selectedKey
        if(!currentRowUpdated)
            setCurrentKey(getSelectedKey(), getSelectedExpandingIndex());
    }

    private boolean currentRowUpdated = false;
    public void updateCurrentRow() {
        if (currentRowUpdated) {
            int row = currentKey != null ? getRowByKey(currentKey, currentExpandingIndex) : -1;

            // if we didn't find currentKey with that expandingIndex, it can be because the currentKey has become unexpanded
            // so we're just looking for the object record and then shifting this index with the expanding index
            if(currentExpandingIndex != null && !currentExpandingIndex.equals(GridDataRecord.objectExpandingIndex) &&
                    row < 0) {
                int objectRow = getRowByKey(currentKey, GridDataRecord.objectExpandingIndex);
                if(objectRow >= 0)
                    row = Math.min(objectRow + currentExpandingIndex + 1, getRowCount() - 1);
            }

            setSelectedRow(row);

            currentKey = null;
            currentExpandingIndex = null;
            currentRowUpdated = false;
        }

        assert getSelectedRow() < getRowCount();
    }

    @Override
    protected boolean previewEvent(Element target, Event event) {
        SmartScheduler.getInstance().flush();
        return form.previewEvent(target, event);
    }

    public GPropertyDraw getSelectedProperty() {
        if(getSelectedRow() >= 0 && getSelectedColumn() >= 0)
            return getProperty(getSelectedCell());
        return null;
    }

    public GGroupObjectValue getSelectedColumnKey() {
        if(getSelectedRow() >= 0 && getSelectedColumn() >= 0)
            return getColumnKey(getSelectedCell());
        return null;
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
    }

    public void updateCellImages(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        cellImages.put(propertyDraw, values);
    }

    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
    }

    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
        captionsUpdated = true;
    }

    public void updatePropertyFooters(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyFooters.put(propertyDraw, values);
        footersUpdated = true;
    }

    public void headerClicked(int columnIndex, boolean ctrlDown, boolean shiftDown) {
        sortableHeaderManager.headerClicked(columnIndex, ctrlDown, shiftDown);
        updateHeadersDOM(false);
    }

    public Boolean getSortDirection(GGridPropertyTableHeader header) {
        return sortableHeaderManager.getSortDirection(getHeaderIndex(header));
    }
    
    protected boolean useQuickSearchInsteadOfQuickFilter() {
        return false;
    }

    protected void quickSearch(Event event) {
        //do nothing by default
    }

    public abstract GGroupObject getGroupObject();

    public abstract void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey);
    public abstract GAbstractTableController getGroupController();

    public void runGroupReport() {
        form.runGroupReport(groupObject.ID);
    }

    public void selectNextCellInColumn(boolean forward) {
        selectionHandler.nextColumn(forward, FocusUtils.Reason.KEYNEXTNAVIGATE);
    }

    public static class GridPropertyTableSelectionHandler<T extends GridDataRecord> extends DataGridSelectionHandler<T> {
        public GridPropertyTableSelectionHandler(DataGrid<T> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(Event event) {
            assert BrowserEvents.KEYDOWN.equals(event.getType());

            int keyCode = event.getKeyCode();
            FocusUtils.Reason reason = FocusUtils.Reason.KEYMOVENAVIGATE;
            if (keyCode == KeyCodes.KEY_HOME && !event.getCtrlKey()) {
                int i=0;
                while (!isFocusable(i))
                    i++;
                changeColumn(i, reason);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && !event.getCtrlKey()) {
                int i=display.getColumnCount()-1;
                while (!isFocusable(i))
                    i--;
                changeColumn(i, reason);
                return true;
            }
            return super.handleKeyEvent(event);
        }
    }

    @Override
    protected GSize getColumnWidth(int column) {
        if(resized)
            return GSize.getResizeSize(doublePrefs[column]);

        return prefs[column];
    }

    @Override
    protected double getColumnFlexPerc(int column) {
        return flexPercents[column];
    }

    @Override
    public boolean isColumnFlex(int column) {
        return baseFlexes[column] > 0;
    }

    private double[] flexPercents;
    private void updateLayoutWidthColumns() {
        double totalFlexValues = 0;
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++)
            totalFlexValues += flexes[i];

        int precision = 10000;
        int restPercent = 100 * precision;
        flexPercents = new double[columnCount];
        for(int i=0;i<columnCount;i++) {
            if(baseFlexes[i] > 0) {
                double flex = flexes[i];
                int flexPercent = (int) Math.round(flex * restPercent / totalFlexValues);
                restPercent -= flexPercent;
                totalFlexValues -= flex;
                flexPercents[i] = ((double) flexPercent / (double) precision);
            }
        }
    }

    private boolean[] flexPrefs;
    public double resizeColumn(int column, int delta) {
//        int body = ;
        int columnCount = getColumnCount();
        if(!resized) {
            resized = true;
            doublePrefs = new double[columnCount];
            intBasePrefs = new int[columnCount];
            flexPrefs = new boolean[columnCount];
            for (int i = 0; i < columnCount; i++) {
                Double pxPref = prefs[i].getResizeSize();
                Integer pxBasePref = basePrefs[i].getIntResizeSize();
                doublePrefs[i] = pxPref != null ? pxPref : -1.0;
                intBasePrefs[i] = pxBasePref != null ? pxBasePref : -1;
                flexPrefs[i] = false;
            }
            prefs = null;
            basePrefs = null;

            margins = 0;
            for (int i = 0; i < columnCount; i++) {
                int actualSize = getClientColumnWidth(i);
                if(doublePrefs[i] < -0.5 || intBasePrefs[i] < 0) {
                    if(doublePrefs[i] < -0.5)
                        doublePrefs[i] = actualSize;
                    if(intBasePrefs[i] < 0)
                        intBasePrefs[i] = actualSize;
                }
                margins += getFullColumnWidth(i) - actualSize;
            }
        }

        int viewWidth = getViewportWidth() - margins;
        double restDelta = GwtClientUtils.calculateNewFlexes(column, delta, viewWidth, doublePrefs, flexes, intBasePrefs, baseFlexes, flexPrefs, true);

        for (int i = 0; i < columnCount; i++) {
            setUserWidth(i, (int) Math.round(doublePrefs[i]));
            setUserFlex(i, flexes[i]);
        }

        updateLayoutWidthColumns();

        widthsChanged();
        onResize();

        return restDelta;
    }

    protected abstract void setUserWidth(GPropertyDraw property, Integer value);
    protected abstract void setUserFlex(GPropertyDraw property, Double value);
    protected abstract Integer getUserWidth(GPropertyDraw property);
    protected abstract Double getUserFlex(GPropertyDraw property);

    protected abstract GPropertyDraw getColumnPropertyDraw(int i);
    protected abstract GGroupObjectValue getColumnKey(int i);

    protected void updateCaptions() {
        if (captionsUpdated) {
            for (int i = 0, size = getColumnCount(); i < size; i++) {
                updatePropertyHeader(i);
            }
            headersChanged();
            captionsUpdated = false;
        }
    }

    protected void updateFooters() {
        if (footersUpdated) {
            for (int i = 0, size = getColumnCount(); i < size; i++) {
                updatePropertyFooter(i);
            }
            headersChanged();
            footersUpdated = false;
        }
    }

    private boolean resized;
    private double[] doublePrefs;  // mutable, should be equal to prefs
    private int[] intBasePrefs;
    private int margins;

    private double[] flexes;
    private double[] baseFlexes;
    private GSize[] prefs;
    private GSize[] basePrefs;

    public void updateLayoutWidth() {
        int columnsCount = getColumnCount();

        resized = false;
        doublePrefs = null;
        intBasePrefs = null;
        flexPrefs = null;
        margins = 0;

        flexes = new double[columnsCount];
        baseFlexes = new double[columnsCount];

        prefs = new GSize[columnsCount];
        basePrefs = new GSize[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            double baseFlex = getColumnFlex(i);
            baseFlexes[i] = baseFlex;

            GSize basePref = getColumnBaseWidth(i);
            basePrefs[i] = basePref;

            GSize pref = basePref;
            Integer userWidth = getUserWidth(i);
            if(baseFlex > 0 && userWidth != null)
                pref = pref.max(GSize.getResizeSize(userWidth));
            prefs[i] = pref;

            double flex = baseFlex;
            Double userFlex = getUserFlex(i);
            if(baseFlex > 0 && userFlex != null)
                flex = userFlex;
            flexes[i] = flex;
        }
        updateLayoutWidthColumns();
    }

    protected void updatePropertyHeader(int index) {
        updatePropertyHeader(getColumnKey(index), getColumnPropertyDraw(index), index);
    }

    protected void updatePropertyHeader(GGroupObjectValue columnKey, GPropertyDraw property, int index) {
        String columnCaption = getPropertyCaption(property, columnKey);
        GGridPropertyTableHeader header = getGridHeader(index);
        if(header != null) {
            header.setCaption(columnCaption, property.notNull, property.hasChangeAction);
            header.setPaths(property.path, property.creationPath);
            header.setToolTip(property.getTooltipText(columnCaption));
            header.setHeaderHeight(property.getHeaderCaptionHeight(this));
        } else
            assert columnCaption == null || columnCaption.isEmpty();
    }

    protected void updatePropertyFooter(int index) {
        updatePropertyFooter(getColumnKey(index), getColumnPropertyDraw(index), index);
    }

    protected void updatePropertyFooter(GGroupObjectValue columnKey, GPropertyDraw property, int index) {
        GGridPropertyTableFooter footer = getGridFooter(index);
        if(footer != null) {
            footer.setValue(getPropertyFooter(property, columnKey));
        }
    }

    protected Pair<GGroupObjectValue, Object> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue propertyRowKey, int columnIndex, GGroupObjectValue propertyColumnKey, Object value) {
        if(propertyRowKey == null)
            return null;

        if(columnIndex < 0)
            return null;

        int rowIndex = getRowByKey(propertyRowKey, GridDataRecord.objectExpandingIndex);
        if(rowIndex < 0)
            return null;

        GridPropertyColumn column = getGridColumn(columnIndex);
        T rowRecord = getRowValue(rowIndex);
        Cell cell = new Cell(rowIndex, columnIndex, column, rowRecord);

        Object oldValue = column.getValue(property, rowRecord);

        setLoadingAt(cell);
        setValueAt(cell, value);

        column.updateDom(cell, getElement(cell));

        return new Pair<>(GGroupObjectValue.getFullKey(propertyRowKey, propertyColumnKey), oldValue);
    }

    public Pair<lsfusion.gwt.client.form.view.Column, String> getSelectedColumn(GPropertyDraw property, GGroupObjectValue columnKey) {
        return new Pair<>(new lsfusion.gwt.client.form.view.Column(property, columnKey), getPropertyCaption(property, columnKey));
    }
    public static Pair<lsfusion.gwt.client.form.view.Column, String> getSelectedColumn(NativeHashMap<GGroupObjectValue, Object> propCaptions, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new Pair<>(new lsfusion.gwt.client.form.view.Column(property, columnKey), getPropertyCaption(propCaptions, property, columnKey));
    }

    protected String getPropertyCaption(GPropertyDraw property, GGroupObjectValue columnKey) {
        String userCaption = getUserCaption(property);
        if (userCaption != null)
            return userCaption;

        return getPropertyCaption(propertyCaptions.get(property), property, columnKey);
    }

    protected Object getPropertyFooter(GPropertyDraw property, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, Object> propFooters = propertyFooters.get(property);
        return propFooters != null ? propFooters.get(columnKey) : null;
    }

    public abstract GSize getHeaderHeight();

    protected String getUserCaption(GPropertyDraw propertyDraw) {
        return null;
    }

    protected double getColumnFlex(int i) {
        return getColumnPropertyDraw(i).getFlex();
    }

    protected void setUserWidth(int i, int width) {
        setUserWidth(getColumnPropertyDraw(i), width);
    }
    protected void setUserFlex(int i, double width) {
        setUserFlex(getColumnPropertyDraw(i), width);
    }

    protected Integer getUserWidth(int i) {
        return getUserWidth(getColumnPropertyDraw(i));
    }
    protected Double getUserFlex(int i) {
        return getUserFlex(getColumnPropertyDraw(i));
    }

    protected GSize getColumnBaseWidth(int i) {
        // we need not null, because table with table-layout:fixed, uses auto which just split the rest percents equally
        // and table-layout:auto won't do for a lot of reasons
        // plus calc(100px+3%) doesn't work (despite the css specification), however not sure that this would help
        // plus there is a problem with paddings:
        //      when setting width to a column, this width is set "with paddings", that could be fixed, by setting widths for the first row (however in that case we would have to create one invisible virtual row in the header)
        //      but the main problem is that % percentage works really odd (but only in Chrome, in Firefox it works fine), it respects paddings, but not the way it does in divs (i.e subtract paddings, and then split the rest). This might create problems in resizing,
        //      plus Firefox doesn't respect min-width for td (as well as Chrome)
        // but there is another way : to create separate columns for flex props : one fixed size, one percent size, and set colspan 2 for headers / rows
        return getColumnPropertyDraw(i).getValueWidth(font, true, true);
    }

    private <C> Element getRenderElement(Cell cell, TableCellElement parent) {
        if(parent == null)
            return null;

        GPropertyDraw property = getProperty(cell);
        if(property == null)
            return null;

        return GPropertyTableBuilder.getRenderSizedElement(parent, property);
    }

    protected Element getSelectedRenderElement(int column) {
        return getRenderElement(getSelectedCell(column), getSelectedElement(column));
    }

    public <C> void onBrowserEvent(Cell cell, Event event, Column<T, C> column, TableCellElement parent) {
        Element renderElement = getRenderElement(cell, parent);

        form.onPropertyBrowserEvent(new EventHandler(event), renderElement, parent != null, getTableDataFocusElement(),
                handler -> selectionHandler.onCellBefore(handler, cell, rowChanged -> isChangeOnSingleClick(cell, event, (Boolean) rowChanged, column)),
                handler -> column.onEditEvent(handler, cell, renderElement),
                handler -> selectionHandler.onCellAfter(handler, cell),
                handler -> CopyPasteUtils.putIntoClipboard(renderElement), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteData(cell, renderElement, GwtClientUtils.getClipboardTable(line))),
                false, cell.getColumn().isCustomRenderer());
    }

    @Override
    public void pasteData(Cell cell, Element renderElement, List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            form.pasteValue(getEditContext(cell, renderElement), table.get(0).get(0));
        }
    }

    protected boolean isFocusable(GPropertyDraw property) {
        return property.focusable == null || property.focusable;
    }

    public abstract class GridPropertyColumn extends Column<T, Object> {

        protected abstract Object getValue(GPropertyDraw property, T record);
        protected abstract boolean isLoading(GPropertyDraw property, T record);
        protected abstract Object getImage(GPropertyDraw property, T record);
        protected abstract String getBackground(GPropertyDraw property, T record);
        protected abstract String getForeground(GPropertyDraw property, T record);

        @Override
        public void onEditEvent(EventHandler handler, Cell editCell, Element editRenderElement) {
            GGridPropertyTable.this.onEditEvent(handler, false, editCell, editRenderElement);
        }

        public void renderDom(Cell cell, TableCellElement cellElement) {
            GPropertyDraw property = getProperty(cell);
            if(property == null) // in tree there can be no property in groups other than last
                return;

            Element renderElement = GPropertyTableBuilder.renderSized(cellElement, property, getFont());
            form.render(property, renderElement, getRenderContext(cell, renderElement, property, this));
        }

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {
            GPropertyDraw property = getProperty(cell);
            if (property == null) // in tree there can be no property in groups other than last
                return;

            // RERENDER IF NEEDED : we don't have the previous state, so we have to store it in element

            Element renderElement = GPropertyTableBuilder.getRenderSizedElement(cellElement, property);
            form.update(property, renderElement, getUpdateContext(cell, renderElement, property, this));
        }
    }

    protected static boolean incrementalUpdate = true;
    protected void incUpdateRowIndices(int startFrom, int shift) {
        assert incrementalUpdate;

        for(int i=startFrom,size=rows.size();i<size;i++) {
            GridDataRecord row = rows.get(i);
            assert row.getRowIndex() + shift == i;
            row.rowIndex = i;
        }
    }

    public GridPropertyColumn getGridColumn(int column) {
        return (GridPropertyColumn) getColumn(column);
    }

    @Override
    protected RenderContext getRenderContext(Cell cell, Element renderElement, GPropertyDraw property, GridPropertyColumn column) {
        return new RenderContext() {
            @Override
            public boolean globalCaptionIsDrawn() {
                return GGridPropertyTable.this.globalCaptionIsDrawn();
            }

            @Override
            public GFont getFont() {
                return GGridPropertyTable.this.getFont();
            }
//
//            @Override
//            public boolean isLoading() {
//                return column.isLoading(property, (T) cell.getRow());
//            }
        };
    }

    private boolean globalCaptionIsDrawn() {
        return true;
    }

    public UpdateContext getUpdateContext(Cell cell, Element renderElement, GPropertyDraw property, GridPropertyColumn column) {
        return new UpdateContext() {
            @Override
            public void changeProperty(Object result) {
                form.changeProperty(getEditContext(cell, renderElement), result);
            }

            @Override
            public void executeContextAction(int action) {
                form.executeContextAction(getEditContext(cell, renderElement), action);
            }

            @Override
            public boolean isPropertyReadOnly() {
                return GGridPropertyTable.this.isReadOnly(cell);
            }

            @Override
            public boolean globalCaptionIsDrawn() {
                return GGridPropertyTable.this.globalCaptionIsDrawn();
            }

            @Override
            public Object getValue() {
                return column.getValue(property, (T) cell.getRow());
            }

            @Override
            public boolean isLoading() {
                return column.isLoading(property, (T) cell.getRow());
            }

            @Override
            public boolean isSelectedRow() {
                return GGridPropertyTable.this.isSelectedRow(cell);
            }

            public boolean isFocusedColumn() {
                return GGridPropertyTable.this.isFocusedColumn(cell);
            }

            @Override
            public Object getImage() {
                return column.getImage(property, (T) cell.getRow());
            }

            @Override
            public CellRenderer.ToolbarAction[] getToolbarActions() {
                return isPropertyReadOnly() ? UpdateContext.super.getToolbarActions() : property.getQuickAccessActions(isSelectedRow(), isFocusedColumn());
            }

            @Override
            public String getBackground() {
                T row = (T) cell.getRow();
                String background = column.getBackground(property, row);
                return background != null ? background : row.getRowBackground();
            }

            @Override
            public String getForeground() {
                T row = (T) cell.getRow();
                String foreground = column.getForeground(property, row);
                return getThemedColor(foreground != null ? foreground : row.getRowForeground());
            }
        };
    }

    @Override
    public void changeSelectedCell(int row, int column, FocusUtils.Reason reason) {
        form.checkCommitEditing();

        super.changeSelectedCell(row, column, reason);

        if(getSelectedRow() >= 0 && getSelectedColumn() >= 0) {
            Element selectedRenderElement = getSelectedRenderElement(getSelectedColumn());
            if(selectedRenderElement != null) {
                InputElement inputElement = SimpleTextBasedCellRenderer.getInputElement(selectedRenderElement);
                if (inputElement != null)
                    FocusUtils.focus(inputElement, reason);
            }
        }
    }
}
