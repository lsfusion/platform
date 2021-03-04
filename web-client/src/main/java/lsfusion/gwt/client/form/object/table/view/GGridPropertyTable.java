package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public abstract class GGridPropertyTable<T extends GridDataRecord> extends GPropertyTable<T> implements HasMaxPreferredSize {
    public static int DEFAULT_PREFERRED_WIDTH = 130; // должно соответствовать значению в gridResizePanel в MainFrame.css
    public static int DEFAULT_PREFERRED_HEIGHT = 70; // должно соответствовать значению в gridResizePanel в MainFrame.css
    public static int DEFAULT_MAX_PREFERRED_HEIGHT = 140;

    protected ArrayList<T> rows = new ArrayList<>();

    // we have to keep it until updateDataImpl to have rows order
    // plus what's more important we shouldn't change selectedRow, before update'in rows, otherwise we'll have inconsistent selectedRow - rows state
    protected GGroupObjectValue currentKey;

    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;

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

    public static String getPropertyCaption(NativeHashMap<GGroupObjectValue, Object> propCaptions, GPropertyDraw property, GGroupObjectValue columnKey) {
        String caption;
        if (propCaptions != null)
            caption = property.getDynamicCaption(propCaptions.get(columnKey));
        else
            caption = property.getCaptionOrEmpty();
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

    protected int preferredWidth;

    public GGridPropertyTable(GFormController iform, GGroupObject iGroupObject, GFont font) {
        super(iform, iGroupObject, getDefaultStyle(), false, !iGroupObject.hasFooters, false);
        
        this.font = font;

        setTableBuilder(new GGridPropertyTableBuilder<T>(this));

        if(groupObject != null) {
            // ADD FILTER
            addFilterBinding(new GKeyInputEvent(new GKeyStroke(GKeyStroke.KEY_F2)),
                    event -> getGroupController().addFilter());
            // REPLACE FILTER
            addFilterBinding(new GKeyInputEvent(new GKeyStroke(GKeyStroke.KEY_F2, true, false, false)),
                    event -> getGroupController().replaceFilter());
            // REMOVE FILTERS
            GFormController.BindingExec removeFilters = event -> getGroupController().removeFilters();
            addFilterBinding(new GKeyInputEvent(new GKeyStroke(GKeyStroke.KEY_F2, false, false, true)),
                    removeFilters);
            addFilterBinding(nativeEvent -> {
                        if (GKeyStroke.isEscapeKeyEvent(nativeEvent) && GKeyStroke.isPlainKeyEvent(nativeEvent)) {
                            GAbstractTableController goController = getGroupController();
                            return goController.userFilters != null && goController.userFilters.hasConditions();
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
        }
    }

    private void addFilterBinding(GInputEvent event, GFormController.BindingExec pressed) {
        addFilterBinding(event::isEvent, pressed);
    }
    private void addFilterBinding(GFormController.BindingCheck event, GFormController.BindingExec pressed) {
        form.addBinding(event, new GBindingEnv(null, null, null, GBindingMode.ONLY, null, null, null, null), pressed, GGridPropertyTable.this, groupObject);
    }

    @Override
    public GFont getFont() {
        return font;
    }

    public Cell getSelectedCell() {
        return getSelectedCell(getSelectedColumn());
    }

    public Cell getSelectedCell(int column) {
        return new Cell(getSelectedRow(), column, getColumn(column), getSelectedRowValue());
    }

    protected boolean isAutoSize() {
        return false;
    }

    public int getAutoSize() {
        return getTableBodyElement().getOffsetHeight();
    }
    
    @Override
    public Dimension getMaxPreferredSize() {
        return new Dimension(
                max(isAutoSize() ? 0 : DEFAULT_PREFERRED_WIDTH, preferredWidth + nativeScrollbarWidth + 17),
                max(isAutoSize() ? 0 : DEFAULT_MAX_PREFERRED_HEIGHT, getRowCount() * tableBuilder.getCellHeight() + 1 + 30 + nativeScrollbarHeight)
        );
    }

    protected abstract GGroupObjectValue getSelectedKey();

    // there is a contract if there are keys there should be current object
    // but for example modifyFormChangesWithChangeCurrentObjectAsyncs removes object change (+ when there are no keys nothing is also send)
    // so we'll put current (last) key to ensure that selected row will match rows collection
    protected void checkUpdateCurrentRow() {
        // assert rowUpdated AND ! it's important to do this before update rows to have relevant selectedKey
        if(!currentRowUpdated)
            setCurrentKey(getSelectedKey());
    }

    private boolean currentRowUpdated = false;
    public void updateCurrentRow() {
        if (currentRowUpdated) {
            setSelectedRow(currentKey != null ? getRowByKeyOptimistic(currentKey) : -1);

            currentKey = null;
            currentRowUpdated = false;
        }

        assert getSelectedRow() < getRowCount();
    }

    @Override
    protected boolean previewEvent(Element target, Event event) {
        return form.previewEvent(target, event);
    }

    @Override
    protected void onFocus() {
        super.onFocus();
    }

    @Override
    protected void onBlur(Event event) {
        form.previewBlurEvent(event);

        super.onBlur(event);
    }

    public GPropertyDraw getSelectedProperty() {
        if(getSelectedRow() >= 0)
            return getProperty(getSelectedCell());
        return null;
    }

    public GGroupObjectValue getSelectedColumnKey() {
        if(getSelectedRow() >= 0)
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
    }

    public void updatePropertyFooters(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyFooters.put(propertyDraw, values);
    }

    public void headerClicked(GGridPropertyTableHeader header, boolean ctrlDown, boolean shiftDown) {
        sortableHeaderManager.headerClicked(getHeaderIndex(header), ctrlDown, shiftDown);
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

    public abstract String getColumnSID(int column);

    public void runGroupReport() {
        form.runGroupReport(groupObject.ID);
    }

    public void selectNextRow(boolean down) {
        selectionHandler.nextRow(down);
    }

    public void selectNextCellInColumn(boolean forward) {
        selectionHandler.nextColumn(forward);
    }

    public static class GridPropertyTableSelectionHandler<T extends GridDataRecord> extends DataGridSelectionHandler<T> {
        public GridPropertyTableSelectionHandler(DataGrid<T> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(Event event) {
            assert BrowserEvents.KEYDOWN.equals(event.getType());

            int keyCode = event.getKeyCode();
            if (keyCode == KeyCodes.KEY_HOME && !event.getCtrlKey()) {
                for(int i=0;!changeColumn(i);i++);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && !event.getCtrlKey()) {
                for(int i=display.getColumnCount()-1;!changeColumn(i);i--);
                return true;
            }
            return super.handleKeyEvent(event);
        }
    }

    private void updateLayoutWidthColumns() {
        List<Column> flexColumns = new ArrayList<>();
        List<Double> flexValues = new ArrayList<>();
        double totalPref = 0.0;
        double totalFlexValues = 0;

        for (int i = 0, columnCount = getColumnCount(); i < columnCount; ++i) {
            Column column = getColumn(i);

            double pref = prefs[i];
            if(flexes[i]) {
                flexColumns.add(column);
                flexValues.add(pref);
                totalFlexValues += pref;
            } else {
                int intPref = (int) Math.round(prefs[i]);
                assert intPref == basePrefs[i];
                setColumnWidth(column, intPref + "px");
            }
            totalPref += pref;
        }

        // поправка для округлений (чтобы не дрожало)
        int flexSize = flexValues.size();
        if(flexSize % 2 != 0)
            flexSize--;
        for(int i=0;i<flexSize;i++)
            flexValues.set(i, flexValues.get(i) + (i % 2 == 0 ? 0.1 : -0.1));

        int precision = 10000;
        int restPercent = 100 * precision;
        for(int i=0,size=flexColumns.size();i<size;i++) {
            Column flexColumn = flexColumns.get(i);
            double flexValue = flexValues.get(i);
            int flexPercent = (int) Math.round(flexValue * restPercent / totalFlexValues);
            restPercent -= flexPercent;
            totalFlexValues -= flexValue;
            setColumnWidth(flexColumn, ((double)flexPercent / (double)precision)  + "%");
        }
        preferredWidth = (int) Math.round(totalPref);
        setMinimumTableWidth(totalPref, com.google.gwt.dom.client.Style.Unit.PX);
    }

    public void resizeColumn(int column, int delta) {
//        int body = ;
        int viewWidth = getViewportWidth() - 1; // непонятно откуда этот один пиксель берется (судя по всему padding)
        GwtClientUtils.calculateNewFlexesForFixedTableLayout(column, delta, viewWidth, prefs, basePrefs, flexes);
        for (int i = 0; i < prefs.length; i++)
            setUserWidth(i, (int) Math.round(prefs[i]));
        updateLayoutWidthColumns();

        widthsChanged();
        onResize();
    }

    protected abstract void setUserWidth(GPropertyDraw property, Integer value);
    protected abstract Integer getUserWidth(GPropertyDraw property);

    protected abstract GPropertyDraw getColumnPropertyDraw(int i);

    private double[] prefs;  // mutable
    private int[] basePrefs;
    private boolean[] flexes;
    public void updateLayoutWidth() {
        int columnsCount = getColumnCount();
        prefs = new double[columnsCount];
        basePrefs = new int[columnsCount];
        flexes = new boolean[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            boolean flex = isColumnFlex(i);
            flexes[i] = flex;

            int basePref = getColumnBaseWidth(i);
            basePrefs[i] = basePref;

            Integer userWidth = getUserWidth(i);
            int pref = flex && userWidth != null ? Math.max(userWidth, basePref) : basePref;
            prefs[i] = pref;
        }
        updateLayoutWidthColumns();
    }

    protected void updatePropertyHeader(GGroupObjectValue columnKey, GPropertyDraw property, int index) {
        String columnCaption = getPropertyCaption(property, columnKey);
        GGridPropertyTableHeader header = getGridHeader(index);
        header.setCaption(columnCaption, property.notNull, property.hasChangeAction);
        header.setToolTip(property.getTooltipText(columnCaption));
        header.setHeaderHeight(getHeaderHeight());
    }

    protected void updatePropertyFooter(GGroupObjectValue columnKey, GPropertyDraw property, int index) {
        GGridPropertyTableFooter footer = getGridFooter(index);
        if(footer != null) {
            footer.setValue(getPropertyFooter(property, columnKey));
        }
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

    protected int getHeaderHeight() {
        return 0;
    }

    protected String getUserCaption(GPropertyDraw propertyDraw) {
        return null;
    }

    protected boolean isColumnFlex(int i) {
        return getColumnPropertyDraw(i).getFlex() > 0;
    }

    protected void setUserWidth(int i, int width) {
        setUserWidth(getColumnPropertyDraw(i), width);
    }

    protected Integer getUserWidth(int i) {
        return getUserWidth(getColumnPropertyDraw(i));
    }
    
    protected int getColumnBaseWidth(int i) {
        return getColumnPropertyDraw(i).getValueWidthWithPadding(font);
    }

    public <C> void onBrowserEvent(Cell cell, Event event, Column<T, C> column, Element parent) {
        form.onPropertyBrowserEvent(new EventHandler(event), parent, getTableDataFocusElement(),
                handler -> selectionHandler.onCellBefore(handler, cell, rowChanged -> isChangeOnSingleClick(cell, (Boolean) rowChanged)),
                handler -> column.onEditEvent(handler, false, cell, parent),
                handler -> selectionHandler.onCellAfter(handler, cell),
                handler -> CopyPasteUtils.putIntoClipboard(parent), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteData(GwtClientUtils.getClipboardTable(line))), false);
    }

    protected boolean isFocusable(GPropertyDraw property) {
        return property.focusable == null || property.focusable;
    }

    protected abstract class GridPropertyColumn extends Column<T, Object> {

        protected abstract Object getValue(GPropertyDraw property, T record);

        @Override
        public void onEditEvent(EventHandler handler, boolean isBinding, Cell editCell, Element editCellParent) {
            GGridPropertyTable.this.onEditEvent(handler, isBinding, editCell, editCellParent);
        }

        @Override
        public void renderAndUpdateDom(Cell cell, Element cellElement) {
            renderDom(cell, cellElement);

            updateDom(cell, cellElement);
        }

        public void renderDom(Cell cell, Element cellElement) {
            GPropertyDraw property = getProperty(cell);
            if(property != null) // in tree there can be no property in groups other than last
                form.render(property, cellElement, GGridPropertyTable.this);
        }

        @Override
        public void updateDom(Cell cell, Element cellElement) {
            GPropertyDraw property = getProperty(cell);
            if (property != null) // in tree there can be no property in groups other than last
                form.update(property, cellElement, getValue(property, (T) cell.getRow()), GGridPropertyTable.this);
        }
    }
}
