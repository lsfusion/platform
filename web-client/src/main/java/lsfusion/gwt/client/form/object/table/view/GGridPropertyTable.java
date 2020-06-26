package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Context;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.EditEventFilter;

public abstract class GGridPropertyTable<T extends GridDataRecord> extends GPropertyTable<T> implements HasMaxPreferredSize {
    public static int DEFAULT_PREFERRED_WIDTH = 130; // должно соответствовать значению в gridResizePanel в MainFrame.css
    public static int DEFAULT_PREFERRED_HEIGHT = 70; // должно соответствовать значению в gridResizePanel в MainFrame.css
    public static int DEFAULT_MAX_PREFERRED_HEIGHT = 140;
    
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<>();

    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<>();
    protected Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<>();
    protected Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<>();

    protected GGridPropertyTableHeader getGridHeader(int i) {
        return (GGridPropertyTableHeader) getHeader(i);
    }

    protected boolean needToRestoreScrollPosition = true;
    protected GGroupObjectValue oldKey = null;
    protected int oldRowScrollTop;

    public GGridSortableHeaderManager sortableHeaderManager;
    
    public GFont font;

    protected int preferredWidth;

    public GGridPropertyTable(GFormController iform, GGroupObject iGroupObject, GFont font) {
        super(iform, iGroupObject, getDefaultStyle(), false, true, false);
        
        this.font = font;

        setTableBuilder(new GGridPropertyTableBuilder<T>(this));
    }

    @Override
    protected GFont getFont() {
        return font;
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (GKeyStroke.isAddFilterEvent(event)) {
            stopPropagation(event);
            getGroupController().addFilter();
        } else if (GKeyStroke.isRemoveAllFiltersEvent(event)) {
            stopPropagation(event);
            getGroupController().removeFilters();
        } else if (GKeyStroke.isReplaceFilterEvent(event)) {
            stopPropagation(event);
            getGroupController().replaceFilter();
        } else if (GKeyStroke.isPossibleStartFilteringEvent(event)) {
            EditEventFilter editEventFilter = null;
            GPropertyDraw currentProperty = getSelectedProperty();
            if (currentProperty != null && currentProperty.changeType != null) {
                editEventFilter = currentProperty.changeType.getEditEventFilter();
            }

            if ((editEventFilter != null && !editEventFilter.accept(event) && !form.isEditing()) || isReadOnly(getCurrentCellContext())) {
                stopPropagation(event);
                if (useQuickSearchInsteadOfQuickFilter()) {
                    quickSearch(event);
                } else {
                    GPropertyDraw filterProperty = null;
                    GGroupObjectValue filterColumnKey = null;

                    if(currentProperty != null && currentProperty.quickFilterProperty != null) {
                        filterProperty = currentProperty.quickFilterProperty;
                        if(currentProperty.columnGroupObjects != null && filterProperty.columnGroupObjects != null && currentProperty.columnGroupObjects.equals(filterProperty.columnGroupObjects)) {
                            filterColumnKey = getSelectedColumnKey();
                        }
                    }

                    quickFilter(event, filterProperty, filterColumnKey);
                }
            }
        } else if (BrowserEvents.KEYDOWN.equals(event.getType()) && KeyCodes.KEY_ESCAPE == event.getKeyCode()) {
            GAbstractTableController goController = getGroupController();
            if (goController.filter != null && goController.filter.hasConditions()) {
                stopPropagation(event);
                goController.removeFilters();
                return;
            }
        }
        super.onBrowserEvent2(event);
    }

    public Context getCurrentCellContext() {
        return new Context(getSelectedRow(), getSelectedColumn(), getSelectedRowValue());
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
                max(isAutoSize() ? 0 : DEFAULT_MAX_PREFERRED_HEIGHT, getRowCount() * getRowHeight() + 30 + nativeScrollbarHeight)
        );
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        changeBorder("var(--focus-color)");
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        changeBorder("var(--component-border-color)");
    }

    public void changeBorder(String color) {
        getElement().getStyle().setBorderColor(color);
    }

    public GPropertyDraw getSelectedProperty() {
        return getProperty(getCurrentCellContext());
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
    }

    public void headerClicked(GGridPropertyTableHeader header, boolean ctrlDown, boolean shiftDown) {
        sortableHeaderManager.headerClicked(getHeaderIndex(header), ctrlDown, shiftDown);
        refreshHeaders();
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

    public abstract GGroupObjectValue getCurrentKey();
    public abstract GGroupObject getGroupObject();

    public abstract void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey);
    public abstract GAbstractTableController getGroupController();
    public abstract String getCellBackground(GridDataRecord rowValue, int row, int column);
    public abstract String getCellForeground(GridDataRecord rowValue, int row, int column);

    public void beforeHiding() {
        if (isShowing(this)) {
            storeScrollPosition();
            needToRestoreScrollPosition = true;
        }
    }

    public void afterShowing() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                afterAppliedChanges();
            }
        });
    }

    public void storeScrollPosition() {
        int selectedRow = getSelectedRow();
        GridDataRecord selectedRecord = getSelectedRowValue();
        if (selectedRecord != null) {
            oldKey = selectedRecord.getKey();
            TableRowElement childElement = getChildElement(selectedRow);
            if (childElement != null) {
                oldRowScrollTop = childElement.getOffsetTop() - tableDataScroller.getVerticalScrollPosition();
            }
        }
    }

    public void runGroupReport(boolean toExcel) {
        form.runGroupReport(groupObject.ID, toExcel);
    }

    public void afterAppliedChanges() {
        if (needToRestoreScrollPosition && oldKey != null && oldRowScrollTop != -1) {
            int currentInd = getSelectedRow();
            GGroupObjectValue currentKey = getCurrentKey();
            if (currentKey != null && currentKey.equals(oldKey) && isRowWithinBounds(currentInd)) {
                TableRowElement childElement = getChildElement(currentInd);
                if (childElement != null) {
                    int newVerticalScrollPosition = max(0, childElement.getOffsetTop() - oldRowScrollTop);

                    setDesiredVerticalScrollPosition(newVerticalScrollPosition);

                    oldKey = null;
                    oldRowScrollTop = -1;
                    needToRestoreScrollPosition = false;
                }
            }
        }
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
                changeColumn(0);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && !event.getCtrlKey()) {
                changeColumn(display.getColumnCount() - 1);
                return true;
            }
            return super.handleKeyEvent(event);
        }
    }

    // в общем то для "групп в колонки" разделено (чтобы когда были группы в колонки - все не расширялись(
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
        onResize();
    }

    private int getViewportWidth() {
        return tableDataScroller.getClientWidth();
    }
    public int getViewportHeight() {
        return tableDataScroller.getClientHeight();
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
        return getColumnPropertyDraw(i).getValueWidth(font);
    }

    protected abstract class GridPropertyColumn extends Column<T, Object> {
        public final GPropertyDraw property;

        public GridPropertyColumn(GPropertyDraw property) {
            this.property = property;
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, Object value, EventHandler handler) {
            form.onPropertyBrowserEvent(handler, parent,
                    () -> onEditEvent(handler, false, context, parent),
                    () -> CopyPasteUtils.putIntoClipboard(parent),
                    () -> CopyPasteUtils.getFromClipboard(handler, line -> pasteData(GwtClientUtils.getClipboardTable(line))));
        }

        @Override
        public void renderDom(Context context, Element cellElement, Object value) {
            form.render(property, cellElement, getRenderContext());

            updateDom(context, cellElement, value);
        }

        @Override
        public void updateDom(Context context, Element cellElement, Object value) {
            form.update(property, cellElement, value, getUpdateContext());
        }
    }

}
