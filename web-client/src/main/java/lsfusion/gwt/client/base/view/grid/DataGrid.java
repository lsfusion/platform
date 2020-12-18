/*
/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.Function;

import static java.lang.Math.min;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.base.view.ColorUtils.mixColors;
import static lsfusion.gwt.client.view.StyleDefaults.*;

// we need resizesimplepanel for "scroller" padding in headers (we don't know if there gonna be vertival scroller)
public abstract class DataGrid<T> extends ResizableSimplePanel implements Focusable, ColorThemeChangeListener {

    private static GridStyle DEFAULT_STYLE;

    protected static GridStyle getDefaultStyle() {
        if (DEFAULT_STYLE == null) {
            DEFAULT_STYLE = new GridTableStyle();
        }
        return DEFAULT_STYLE;
    }

    public static int nativeScrollbarWidth = AbstractNativeScrollbar.getNativeScrollbarWidth();
    public static int nativeScrollbarHeight = AbstractNativeScrollbar.getNativeScrollbarHeight();

    /**
     * A boolean indicating that we are in the process of resolving state.
     */
    protected boolean isResolvingState;

    /**
     * The command used to resolve the pending state.
     */
    boolean isFocused;

    private final List<Column<T, ?>> columns = new ArrayList<>();
    private final Map<Column<T, ?>, String> columnWidths = new HashMap<>();

    protected final GridStyle style;

    private HeaderBuilder<T> footerBuilder;
    private final List<Header<?>> footers = new ArrayList<>();

    private HeaderBuilder<T> headerBuilder;
    private final List<Header<?>> headers = new ArrayList<>();

    // pending dom updates
    // all that flags should dropped on finishResolving, and scheduleUpdateDOM should be called
    private boolean columnsChanged;
    private boolean headersChanged;
    private boolean widthsChanged;
    private boolean dataChanged;
    private ArrayList<Column> dataColumnsChanged = new ArrayList<>(); // ordered set, null - rows changed
    private boolean selectedRowChanged;
    private boolean selectedColumnChanged;
    private boolean focusedChanged;

    protected GPropertyTableBuilder<T> tableBuilder;

    private final TableWidget tableData;
    protected TableScrollPanel tableDataScroller; // vertical scroller

    private FooterWidget tableFooter;
    private TableScrollPanel tableFooterScroller;

    private HeaderWidget tableHeader;
    private TableScrollPanel tableHeaderScroller;

    private final FlexTable emptyTableWidgetContainer;

    protected DataGridSelectionHandler selectionHandler;

    public void setSelectionHandler(DataGridSelectionHandler selectionHandler) {
        this.selectionHandler = selectionHandler;
    }

    //focused cell indices local to table (aka real indices in rendered portion of the data)
    int renderedSelectedRow = -1;
    int renderedSelectedCol = -1;
    Object renderedSelectedKey = null; // needed for saving scroll position when keys are update

    protected abstract Object getSelectedKey();
    protected int getRowByKeyOptimistic(Object key) {
        Object selectedKey = getSelectedKey();
        if(selectedKey != null && selectedKey.equals(key)) // optimization the most common case
            return getSelectedRow();

        return getRowByKey(key);
    }
    protected abstract int getRowByKey(Object key);

    private int pageIncrement = 30;

    private final boolean noHeaders;
    private final boolean noFooters;
    private final boolean noScrollers;

    // there are two ways to remove outer grid borders :
    // 1) set styles for outer cells and set border-left/top : none there
    // 2) set margins (not paddings since they do not support negative values) to -1
    // first solution is cleaner (since border can be wider than 1px + margin can be used for other purposes, for example scroller padding), however the problem is that in that case the same should be implemented for focus which is pretty tricky
    // the second solution is easier to implement
    // the same problem is in gpivot (.subtotalouterdiv .scrolldiv / .headerdiv), and there it is solved the same way (however since there is no focus cell there in history there should be a solution with styles, conditional css)
    public static void removeOuterGridBorders(Widget parent) {
        parent.getElement().getStyle().setMarginLeft(-1, Unit.PX);
        setOuterRightGridBorder(parent.getElement(), true);
    }
    public static void setOuterRightGridBorder(Element element, boolean setMargin) {
        element.getStyle().setMarginRight(setMargin ? -1 : 0, Unit.PX);
    }

    public DataGrid(GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        this.noHeaders = noHeaders;
        this.noFooters = noFooters;
        this.noScrollers = noScrollers;

        this.style = style;

        tableData = new TableWidget();

        Widget tableDataContainer;
        if(!noScrollers) {
            tableDataScroller = new TableScrollPanel(tableData, Style.Overflow.AUTO);
            tableDataContainer = tableDataScroller;
        } else {
            tableDataContainer = tableData;
        }

        if(noHeaders && noFooters) {
            setFillWidget(tableDataContainer);
        } else {
            FlexPanel headerPanel = new FlexPanel(true); // will use flex for headers / footers

            if(!noHeaders) {
                Widget tableHeaderContainer;
                tableHeader = new HeaderWidget();
                if(!noScrollers) {
                    tableHeaderScroller = new TableScrollPanel(tableHeader, Style.Overflow.HIDDEN);
                    tableHeaderContainer = tableHeaderScroller;
                } else
                    tableHeaderContainer = tableHeader;

                headerPanel.add(tableHeaderContainer, GFlexAlignment.STRETCH);

                headerBuilder = new DefaultHeaderBuilder<>(this, false);
            } else
                tableHeaderScroller = null;

            headerPanel.addFillFlex(tableDataContainer, !noScrollers ? 0 : null); // for scrollers we need 0 basis (since that is the point of scroller)

            if (!noFooters) { // the same as for headers
                Widget tableFooterContainer;
                tableFooter = new FooterWidget();
                if(!noScrollers) {
                    tableFooterScroller = new TableScrollPanel(tableFooter, Style.Overflow.HIDDEN);
                    tableFooterContainer = tableFooterScroller;
                } else
                    tableFooterContainer = tableFooter;

                headerPanel.add(tableFooterContainer, GFlexAlignment.STRETCH);

                footerBuilder = new DefaultHeaderBuilder<>(this, true);
            } else
                tableFooterScroller = null;

            if(!noScrollers) {
                // Synchronize the scroll positions of the three tables.
                // it's not possible to avoid this by splitting to separate horizontal and vertical scroller, since we don't want vertical scroller to be scrolled by horizontal scroller
                tableDataScroller.addScrollHandler(event -> {
                    int scrollLeft = tableDataScroller.getHorizontalScrollPosition();
                    if (tableHeaderScroller != null) {
                        tableHeaderScroller.setHorizontalScrollPosition(scrollLeft);
                    }
                    if (tableFooterScroller != null) {
                        tableFooterScroller.setHorizontalScrollPosition(scrollLeft);
                    }
                });
            } else {
                removeOuterGridBorders(headerPanel);
            }
            tableDataScroller.addScrollHandler(event -> checkSelectedRowVisible());

            setFillWidget(headerPanel);
        }

        emptyTableWidgetContainer = new FlexTable();
        emptyTableWidgetContainer.setHeight("1px");

        getTableDataFocusElement().setTabIndex(0);
        initSinkEvents(this);

        addStyleName(style.dataGridWidget());

        MainFrame.addColorThemeChangeListener(this);
    }

    private static Set<String> browserKeyEvents;
    private static Set<String> getBrowserKeyEvents() {
        if(browserKeyEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.add(BrowserEvents.KEYPRESS);
            eventTypes.add(BrowserEvents.KEYDOWN);
            eventTypes.add(BrowserEvents.KEYUP);
            browserKeyEvents = eventTypes;
        }
        return browserKeyEvents;
    }

    private static Set<String> browserFocusEvents;
    private static Set<String> getBrowserFocusEvents() {
        if(browserFocusEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.add(BrowserEvents.FOCUS);
            eventTypes.add(BrowserEvents.BLUR);
            browserFocusEvents = eventTypes;
        }
        return browserFocusEvents;
    }

    private static Set<String> browserMouseEvents;
    private static Set<String> getBrowserMouseEvents() {
        if(browserMouseEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.add(BrowserEvents.CLICK);
            eventTypes.add(BrowserEvents.DBLCLICK);
            eventTypes.add(BrowserEvents.MOUSEDOWN);
            eventTypes.addAll(getBrowserTooltipMouseEvents());
            browserMouseEvents = eventTypes;
        }
        return browserMouseEvents;
    }

    private static Set<String> browserDragDropEvents;
    private static Set<String> getBrowserDragDropEvents() {
        if(browserDragDropEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.add(BrowserEvents.DRAGOVER);
            eventTypes.add(BrowserEvents.DRAGLEAVE);
            eventTypes.add(BrowserEvents.DROP);
            browserDragDropEvents = eventTypes;
        }
        return browserDragDropEvents;
    }

    // for tooltips on header
    private static Set<String> browserTooltipMouseEvents;
    public static Set<String> getBrowserTooltipMouseEvents() {
        if(browserTooltipMouseEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.add(BrowserEvents.MOUSEUP);
            eventTypes.add(BrowserEvents.MOUSEOVER);
            eventTypes.add(BrowserEvents.MOUSEOUT);
            eventTypes.add(BrowserEvents.MOUSEMOVE);
            browserTooltipMouseEvents = eventTypes;
        }
        return browserTooltipMouseEvents;
    }

    private static Set<String> browserEvents;
    private static Set<String> getBrowserEvents() {
        if(browserEvents == null) {
            Set<String> eventTypes = new HashSet<>();
            eventTypes.addAll(getBrowserFocusEvents());
            eventTypes.addAll(getBrowserMouseEvents());
            eventTypes.addAll(getBrowserKeyEvents());
            browserEvents = eventTypes;
        }
        return browserEvents;
    }
    // should be called for every widget that has Widget.onBrowserEvent implemented
    // however now there are 2 ways of handling events:
    // grid / panel handlers : uses this initSinkEvents + consumed event
    // form bindings and others : manual sinkEvents
    public static void initSinkEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserEvents());

        widget.sinkEvents(Event.ONPASTE | Event.ONCONTEXTMENU);
    }
    // the problem that onpaste ('paste') event is not triggered when there is no "selection" inside element (or no other contenteditable element)
    // also it's important that focusElement should have text, thats why EscapeUtils.UNICODE_NBSP is used in a lot of places
    public static void sinkPasteEvent(Element focusElement) {
        CopyPasteUtils.setEmptySelection(focusElement);
    }

    public static void initSinkMouseEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserMouseEvents());
    }

    public static void initSinkDragDropEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserDragDropEvents());
    }

    public static Element getTargetAndCheck(Element element, Event event) {
        EventTarget eventTarget = event.getEventTarget();
        //it seems that now all this is not needed
//        if (!Element.is(eventTarget)) {
//            return null;
//        }
        Element target = Element.as(eventTarget);
//        if (!element.isOrHasChild(target)) {
//            return null;
//        }
        return target;
    }
    public static boolean checkSinkEvents(Event event) {
        String eventType = event.getType();
        return getBrowserFocusEvents().contains(eventType) ||
                getBrowserMouseEvents().contains(eventType) ||
                getBrowserDragDropEvents().contains(eventType) ||
                checkSinkGlobalEvents(event);
    }
    public static boolean checkSinkGlobalEvents(Event event) {
        return getBrowserKeyEvents().contains(event.getType()) || event.getTypeInt() == Event.ONPASTE || event.getType().equals(BrowserEvents.CONTEXTMENU);
    }

    public GridStyle getStyle() {
        return style;
    }

    public void setPageIncrement(int pageIncrement) {
        this.pageIncrement = Math.max(1, pageIncrement);
    }

    @Override
    public void setAccessKey(char key) {
    }

    @Override
    public void setTabIndex(int index) {
    }

    @Override
    public int getTabIndex() {
        return 0;
    }

    // transfering focus to table data element
    @Override
    public void setFocus(boolean focused) {
        Element focusHolderElement = getTableDataFocusElement();

        if (focused) {
            focusHolderElement.focus();
        } else {
            focusHolderElement.blur();
        }
    }

    public void focus() {
        setFocus(true);
    }

    private Runnable rowChangedHandler;
    public void setRowChangedHandler(Runnable handler) {
        rowChangedHandler = handler;
    }

    /**
     * Get the overall data size.
     *
     * @return the data size
     */
    public int getRowCount() {
        return getRows().size();
    }

    public T getRowValue(int row) {
        return getRows().get(row);
    }

    protected abstract ArrayList<T> getRows();

    @Override
    public final void onBrowserEvent(Event event) {
        // Ignore spurious events (such as onblur) while we refresh the table.
        if (isResolvingState) {
            return;
        }

        // Verify that the target is still a child of this widget. IE fires focus
        // events even after the element has been removed from the DOM.
//        EventTarget eventTarget = event.getEventTarget();
//        if (!Element.is(eventTarget)) {
//            return;
//        }
//        Element target = Element.as(eventTarget);
//        if (!getElement().isOrHasChild(Element.as(eventTarget))) {
//            return;
//        }
        Element target = getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        // here grid handles somewhy other than sink events, so will check it after super
        if(!checkSinkEvents(event))
            return;

        String eventType = event.getType();
        if (BrowserEvents.FOCUS.equals(eventType))
            onFocus();
        else if (BrowserEvents.BLUR.equals(eventType))
            onBlur(event);

        // Find the cell where the event occurred.
        TableSectionElement tbody = getTableBodyElement();
        TableSectionElement tfoot = getTableFootElement();
        TableSectionElement thead = getTableHeadElement();

        int row = -1;
        Column column = null;
        Element columnParent = null;

        Header header = null;
        Element headerParent = null;
        Header footer = null;
        Element footerParent = null;

        TableSectionElement targetTableSection = null;

        if (target == getTableDataFocusElement() || target == getTableElement()) { // need this when focus is on grid and not cell itself, so we need to propagate key events there
            // usually all events has target getTableDataFocusElement, but ONPASTE when focus is on grid somewhy has target tableElement
            if (checkSinkGlobalEvents(event)) {
                targetTableSection = tbody;

                row = getSelectedRow();
                columnParent = getSelectedElement();
                if(columnParent != null)
                    column = tableBuilder.getColumn(columnParent);
            }
        } else {
            Element cur = target;
            while (cur != null) {
                if (cur == tbody || cur == tfoot || cur == thead) {
                    targetTableSection = cur.cast(); // We found the table section.
                    break;
                }

                if(row < 0)
                    row = tableBuilder.getRowValueIndex(cur);
                if (column == null) {
                    column = tableBuilder.getColumn(cur);
                    if(column != null)
                        columnParent = cur;
                }
                if (header == null && !noHeaders) {
                    header = headerBuilder.getHeader(cur);
                    if(header != null)
                        headerParent = cur;
                }
                if (footer == null && !noFooters) {
                    footer = footerBuilder.getHeader(cur);
                    if(footer != null)
                        footerParent = cur;
                }

                cur = cur.getParentElement();
            }
        }

        if (targetTableSection == thead) {
            if (header != null)
                header.onBrowserEvent(headerParent, event);
        } else if(targetTableSection == tfoot) {
            if (footer != null)
                footer.onBrowserEvent(footerParent, event);
        } else {
            if (column != null)
                onBrowserEvent(new Cell(row, getColumnIndex(column), column, getRowValue(row)), event, column, columnParent);
        }
    }

    public abstract <C> void onBrowserEvent(Cell cell, Event event, Column<T, C> column, Element parent);

    /**
     * Checks that the row is within bounds of the view.
     *
     * @param row row index to check
     * @return true if within bounds, false if not
     */
    protected boolean isRowWithinBounds(int row) {
        return row >= 0 && row < getRowCount();
    }

    /**
     * Adds a column to the end of the table with an associated header.
     *
     * @param col    the column to be added
     * @param header the associated {@link Header}
     */
    public void addColumn(Column<T, ?> col, Header<?> header, Header<?> footer) {
        insertColumn(getColumnCount(), col, header, footer);
    }

    /**
     * Inserts a column into the table at the specified index with an associated
     * header.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     * @param header      the associated {@link Header}
     */
    public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header, Header<?> footer) {
        if (noHeaders && header != null) {
            throw new UnsupportedOperationException("the table isn't allowed to have header");
        }
        if (noFooters && footer != null) {
            throw new UnsupportedOperationException("the table isn't allowed to have footer");
        }

        // Allow insert at the end.
        if (beforeIndex != getColumnCount()) {
            checkColumnBounds(beforeIndex);
        }

        headers.add(beforeIndex, header);
        footers.add(beforeIndex, footer);
        columns.add(beforeIndex, col);

        // Increment the keyboard selected column.
        int selectedColumn = getSelectedColumn();
        if(selectedColumn == -1)
            setSelectedColumn(0);
        else if (beforeIndex <= selectedColumn)
            setSelectedColumn(selectedColumn + 1);
    }

    public void moveColumn(int oldIndex, int newIndex) {
        checkColumnBounds(oldIndex);
        checkColumnBounds(newIndex);
        if (oldIndex == newIndex) {
            return;
        }

        int selectedColumn = getSelectedColumn();
        if (oldIndex == selectedColumn)
            setSelectedColumn(newIndex);
        else if (oldIndex < selectedColumn && selectedColumn > 0)
            setSelectedColumn(selectedColumn - 1);

        Column<T, ?> column = columns.remove(oldIndex);
        Header<?> header = headers.remove(oldIndex);
        Header<?> footer = footers.remove(oldIndex);

        columns.add(newIndex, column);
        headers.add(newIndex, header);
        footers.add(newIndex, footer);
    }

    /**
     * Remove a column.
     *
     * @param col the column to remove
     */
    public void removeColumn(Column<T, ?> col) {
        int index = columns.indexOf(col);
        if (index < 0) {
            throw new IllegalArgumentException("The specified column is not part of this table.");
        }
        removeColumn(index);
    }

    /**
     * Remove a column.
     *
     * @param index the column index
     */
    public void removeColumn(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("The specified column index is out of bounds.");
        }
        columns.remove(index);
        headers.remove(index);
        footers.remove(index);

        int selectedColumn = getSelectedColumn();
        // Decrement the keyboard selected column.
        if (index <= selectedColumn)
            setSelectedColumn(selectedColumn - 1);
    }

    /**
     * Get the column at the specified index.
     *
     * @param col the index of the column to retrieve
     * @return the {@link Column} at the index
     */
    public Column<T, ?> getColumn(int col) {
        checkColumnBounds(col);
        return columns.get(col);
    }

    /**
     * Get the number of columns in the table.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Get the index of the specified column.
     *
     * @param column the column to search for
     * @return the index of the column, or -1 if not found
     */
    public int getColumnIndex(Column<T, ?> column) {
        return columns.indexOf(column);
    }

    public String getColumnWidth(Column<T, ?> column) {
        return columnWidths.get(column);
    }

    public void setTableBuilder(GPropertyTableBuilder<T> tableBuilder) {
        this.tableBuilder = tableBuilder;
    }

    public void columnsChanged() {
        columnsChanged = true; // in fact leads to all other changes (widths, data, headers)
        scheduleUpdateDOM();
    }
    public void widthsChanged() {
        widthsChanged = true;
        scheduleUpdateDOM();
    }
    public void rowsChanged() {
        dataChanged(null);
    }
    private boolean areRowsChanged() {
        return dataChanged && dataColumnsChanged == null;
    }
    public void dataChanged(ArrayList<? extends Column> updatedColumns) {
        if(updatedColumns == null) // rows changed
            dataColumnsChanged = null;
        else if(dataColumnsChanged != null)
            GwtClientUtils.addOrderedSets(dataColumnsChanged, updatedColumns);
        dataChanged = true;

        scheduleUpdateDOM();
    }
    public void headersChanged() {
        headersChanged = true;
        scheduleUpdateDOM();
    }

    public void selectedRowChanged() {
        selectedRowChanged = true;
        scheduleUpdateDOM();
    }
    public void selectedColumnChanged() {
        selectedColumnChanged = true;
        scheduleUpdateDOM();
    }
    public void focusedChanged() {
        focusedChanged = true;
        scheduleUpdateDOM();
    }

    /**
     * Get the {@link Header} from the footer section that was added with a
     * {@link Column}.
     */
    public Header<?> getFooter(int index) {
        return footers.get(index);
    }

    /**
     * Get the {@link HeaderBuilder} used to generate the footer section.
     */
    public HeaderBuilder<T> getFooterBuilder() {
        return footerBuilder;
    }

    /**
     * Get the {@link Header} from the header section that was added with a
     * {@link Column}.
     */
    public Header<?> getHeader(int index) {
        return headers.get(index);
    }
    public int getHeaderIndex(Header<?> header) {
        return headers.indexOf(header);
    }

    /**
     * Get the index of the column that is currently selected via the keyboard.
     *
     * @return the currently selected column, or -1 if none selected
     */
    public int getSelectedColumn() {
        return selectedColumn;
    }

    protected TableRowElement getChildElement(int row) {
        return getRowElementNoFlush(row);
    }

    public void setColumnWidth(Column<T, ?> column, String width) {
        columnWidths.put(column, width);
    }

    // when row or column are changed by keypress in grid (KEYUP, PAGEUP, etc), no focus is lost
    // so to handle this there are to ways of doing that, either with GFormController.checkCommitEditing, or moving focus to grid
    // so far will do it with checkCommitEditing (like in form bindings)
    // see overrides
    public boolean changeSelectedColumn(int column) {
//        setFocus(true);
        int columnCount = getColumnCount();
        if(columnCount == 0)
            return true;
        if (column < 0)
            column = 0;
        else if (column >= columnCount)
            column = columnCount - 1;

        if(!isFocusable(column))
            return false;

        setSelectedColumn(column);
        return true;
    }
    public void changeSelectedRow(int row) {
//        setFocus(true);
        int rowCount = getRowCount();

        if(rowCount == 0)
            return;
        if (row < 0)
            row = 0;
        else if (row >= rowCount)
            row = rowCount - 1;

        if(setSelectedRow(row))
            rowChangedHandler.run();
    }

    public void setSelectedColumn(int column) {
        assert column >= 0 : "Column must be zero or greater";

        if (getSelectedColumn() == column)
            return;

        this.selectedColumn = column;
        selectedColumnChanged();
    }

    public boolean isFocusable(int column) {
        return getColumn(column).isFocusable();
    }
    public boolean isFocusable(Cell cell) {
        return cell.getColumn().isFocusable();
    }
    public boolean isChangeOnSingleClick(Cell cell, boolean rowChanged) {
        return !isFocusable(cell);
    }

    public boolean setSelectedRow(int row) {
        if (getSelectedRow() == row)
            return false;

        assert row >= -1 && row < getRowCount();
        this.selectedRow = row;
        selectedRowChanged();
        return true;
    }


    /**
     * Set the minimum width of the tables in this widget. If the widget become
     * narrower than the minimum width, a horizontal scrollbar will appear so the
     * user can scroll horizontally.
     * <p/>
     * <p>
     * Note that this method is not supported in IE6 and earlier versions of IE.
     * </p>
     *
     * @param value the width
     * @param unit  the unit of the width
     * @see #setTableWidth(double, Unit)
     */
    private double minWidthValue = 0.0;
    private Unit minWidthUnit = null;
    public void setMinimumTableWidth(double value, Unit unit) {
        minWidthValue = value;
        minWidthUnit = unit;

        setBlockMinWidth(value, tableData);
        updateHeaderFooterTableMinimumWidth();
    }

    public void setBlockMinWidth(double value, Widget tableData) {
        tableData.getElement().getStyle().setProperty("minWidth", value, minWidthUnit);
    }

    private void updateHeaderTablePadding(TableWrapperWidget headerWidget, TableScrollPanel tableHeaderScroller) {
        updateTablePadding(hasVerticalScroll, headerWidget.tableElement);
//        updateTableMargin(hasVerticalScroll, tableHeaderScroller.getElement());

        updateHeaderTableMinimumWidth(headerWidget); // padding changed, we need to reupdate minimumWidth
    }
    private void updateTableRightOuterBorder() {
        updateTableRightOuterBorder(hasVerticalScroll, tableDataScroller.getElement());
    }

    /* see DataGrid.css, dataGridTableWrapperWidget description */
    public static void updateTablePadding(boolean hasVerticalScroll, Element tableElement) {
        if(hasVerticalScroll)
            tableElement.getStyle().setPaddingRight(nativeScrollbarWidth + 1, Unit.PX); // 1 for right outer border margin
        else
            tableElement.getStyle().clearPaddingRight();
    }
    public static void updateTableMargin(boolean hasVerticalScroller, Element tableElement) {
        if(hasVerticalScroller)
            tableElement.getStyle().setMarginRight(nativeScrollbarWidth + 1, Unit.PX); // 1 for right outer border margin
        else
            tableElement.getStyle().clearMarginRight();
    }
    public static void updateTableRightOuterBorder(boolean hasVerticalScroller, Element tableElement) {
        setOuterRightGridBorder(tableElement, !hasVerticalScroller);
    }

    private void updateHeaderTableMinimumWidth(TableWrapperWidget headerWidget) {
        if(minWidthUnit != null)
            setBlockMinWidth(minWidthValue + (hasVerticalScroll ? nativeScrollbarWidth + 1 : 0), headerWidget); // 1 for right outer border margin
    }

    private void updateTableMargins() {
        if(!noHeaders)
            updateHeaderTablePadding(tableHeader, tableHeaderScroller);
        if(!noFooters)
            updateHeaderTablePadding(tableFooter, tableFooterScroller);
        if(!noScrollers)
            updateTableRightOuterBorder();
    }
    private void updateHeaderFooterTableMinimumWidth() {
        if(!noHeaders)
            updateHeaderTableMinimumWidth(tableHeader);
        if(!noFooters)
            updateHeaderTableMinimumWidth(tableFooter);
    }

    /**
     * Set the width of the tables in this widget. By default, the width is not
     * set and the tables take the available width.
     * <p/>
     * <p>
     * The table width is not the same as the width of this widget. If the tables
     * are narrower than this widget, there will be a gap between the table and
     * the edge of the widget. If the tables are wider than this widget, a
     * horizontal scrollbar will appear so the user can scroll horizontally.
     * </p>
     * <p/>
     * <p>
     * If your table has many columns and you want to ensure that the columns are
     * not truncated, you probably want to use
     * {@link #setMinimumTableWidth(double, Unit)} instead. That will ensure that
     * the table is wide enough, but it will still allow the table to expand to
     * 100% if the user has a wide screen.
     * </p>
     * <p/>
     * <p>
     * Note that setting the width in percentages will not work on older versions
     * of IE because it does not account for scrollbars when calculating the
     * width.
     * </p>
     *
     * @param value the width
     * @param unit  the unit of the width
     * @see #setMinimumTableWidth(double, Unit)
     */
    public void setTableWidth(double value, Unit unit) {
        /*
         * The min-width style attribute doesn't apply to tables, so we set the
         * min-width of the element that contains the table instead. For
         * consistency, we set the width of the container as well.
         */
        assert noHeaders && noScrollers && noFooters;
        tableData.getElement().getStyle().setWidth(value, unit);
    }

    private TableRowElement getRowElementNoFlush(int row) {
        NodeList<TableRowElement> rows = getTableBodyElement().getRows();
        if (!(row >= 0 && row < rows.getLength()))
            return null;
        return rows.getItem(row);
    }

    protected Element getSelectedElement() {
        return getSelectedElement(getSelectedColumn());
    }

    protected Element getSelectedElement(int column) {
        return getElement(getSelectedRow(), column);
    }

    protected Element getElement(int rowIndex, int colIndex) { // element used for rendering
        TableCellElement result = null;
        TableRowElement tr = getRowElementNoFlush(rowIndex); // Do not use getRowElement() because that will flush the presenter.
        if (tr != null && colIndex >= 0) {
            int cellCount = tr.getCells().getLength();
            if (cellCount > 0) {
                int column = min(colIndex, cellCount - 1);
                result = tr.getCells().getItem(column);
            }
        }
        return result;
    }

    protected final TableElement getTableElement() {
        return tableData.tableElement;
    }

    protected final TableSectionElement getTableBodyElement() {
        return tableData.getSection();
    }

    protected final TableSectionElement getTableFootElement() {
        if(!noFooters)
            return tableFooter.getSection();
        return null;
    }

    protected final TableSectionElement getTableHeadElement() {
        if(!noHeaders)
            return tableHeader.getSection();
        return null;
    }

    protected abstract boolean previewEvent(Element target, Event event);

    protected void onFocus() {
        DataGrid.sinkPasteEvent(getTableDataFocusElement());

        if(isFocused)
            return;
        isFocused = true;
        focusedChanged();
    }

    protected void onBlur(Event event) {
        if(!isFocused)
            return;
        isFocused = false;
        focusedChanged();
    }

    public void changeBorder(String color) {
        getElement().getStyle().setBorderColor(color);
    }

    public Element getTableDataFocusElement() {
        if(!noScrollers)
            return tableDataScroller.getElement();
        else
            return tableData.tableElement;
    }

    /**
     * Check that the specified column is within bounds.
     *
     * @param col the column index
     * @throws IndexOutOfBoundsException if the column is out of bounds
     */
    private void checkColumnBounds(int col) {
        if (col < 0 || col >= getColumnCount()) {
            throw new IndexOutOfBoundsException("Column index is out of bounds: " + col);
        }
    }

    private void updateColumnWidthImpl(int column, String width) {
        updateColumnWidthImpl(tableData, column, width);
        if (!noHeaders)
            updateColumnWidthImpl(tableHeader, column, width);
        if (!noFooters)
            updateColumnWidthImpl(tableFooter, column, width);
    }

    private void updateColumnWidthImpl(TableWrapperWidget tableData, int column, String width) {
        Style colElementStyle = tableData.ensureTableColElement(column).getStyle();
        if (width == null)
            colElementStyle.clearWidth();
        else
            colElementStyle.setProperty("width", width);
    }

    /**
     * Get the index of the row that is currently selected via the keyboard,
     * relative to the page start index.
     *
     * <p>
     * This is not same as the selected row in the {@link com.google.gwt.view.client.SelectionModel}. The
     * keyboard selected row refers to the row that the user navigated to via the
     * keyboard or mouse.
     * </p>
     *
     * @return the currently selected row, or -1 if none selected
     */
    public int getSelectedRow() {
        return selectedRow;
    }

    /**
     * Get the value that the user selected.
     *
     * @return the value, or null if a value was not selected
     */
    public T getSelectedRowValue() {
        int selectedRow = getSelectedRow();
        return isRowWithinBounds(selectedRow) ? getRowValue(selectedRow) : null;
    }

    /**
     * Check whether or not the data set is empty. That is, the row count is exactly 0.
     *
     * @return true if data set is empty
     */
    public boolean isEmpty() {
        return getRowCount() == 0;
    }

    protected void startResolving() {
        if (isResolvingState) {
            return;
        }

        isResolvingState = true;
    }

    protected void updateDOM() {
        if (columnsChanged || widthsChanged)
            updateWidthsDOM(columnsChanged); // updating colgroup (column widths)

        if (columnsChanged || headersChanged)
            updateHeadersDOM(columnsChanged); // updating column headers table

        if (columnsChanged || dataChanged)
            updateDataDOM(columnsChanged, dataColumnsChanged); // updating data (rows + column values)

        if (columnsChanged || selectedRowChanged || selectedColumnChanged || dataChanged) // dataChanged because background is data (and updated during data update)
            updateSelectedRowBackgroundDOM(); // updating selection background

        if (columnsChanged || selectedRowChanged || selectedColumnChanged || focusedChanged)
            updateFocusedCellDOM(); // updating focus cell border

        if(focusedChanged) // updating focus grid border
            changeBorder(isFocused ? "var(--focus-color)" : "var(--component-border-color)");
    }

    private void finishResolving() {
        headersChanged = false;
        columnsChanged = false;
        widthsChanged = false;
        dataChanged = false;
        dataColumnsChanged = new ArrayList<>();
        selectedRowChanged = false;
        selectedColumnChanged = false;
        focusedChanged = false;

        renderedSelectedKey = getSelectedKey();
        renderedSelectedRow = getSelectedRow();
        renderedSelectedCol = getSelectedColumn();

        isResolvingState = false;
    }

    private int getLastVisibleRow(int tableBottom, int start) {
        for (int i = start; i >= 0; i--) {
            TableRowElement rowElement = getChildElement(i);
            int rowBottom = rowElement.getOffsetTop() + rowElement.getClientHeight();
            if (rowBottom <= tableBottom) {
                return i;
            }
        }
        return 0;
    }

    private int getFirstVisibleRow(int tableTop, int start) {
        for (int i = start; i < getRowCount(); i++) {
            TableRowElement rowElement = getChildElement(i);
            int rowTop = rowElement.getOffsetTop();
            if (rowTop >= tableTop) {
                return i;
            }
        }
        return 0;
    }

    public void checkSelectedRowVisible() {
        int selectedRow = getSelectedRow();
        if (selectedRow >= 0) {
            int scrollHeight = tableDataScroller.getClientHeight();
            int scrollTop = tableDataScroller.getVerticalScrollPosition();

            TableRowElement rowElement = getChildElement(selectedRow);
            int rowTop = rowElement.getOffsetTop();
            int rowBottom = rowTop + rowElement.getClientHeight();

            int newRow = -1;
            if (rowBottom > scrollTop + scrollHeight + 1) { // 1 for border
                newRow = getLastVisibleRow(scrollTop + scrollHeight, selectedRow);
            }
            if (rowTop < scrollTop) {
                newRow = getFirstVisibleRow(scrollTop, selectedRow);
            }
            if (newRow != -1) {
                changeSelectedRow(newRow);
            }
        }
    }

    private void beforeUpdateDOMScroll(SetPendingScrollState pendingState) {
        beforeUpdateDOMScrollVertical(pendingState);
    }

    private void beforeUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        if (areRowsChanged() && renderedSelectedRow >= 0) // rows changed and there was some selection
            pendingState.renderedSelectedScrollTop = getChildElement(renderedSelectedRow).getOffsetTop() - tableDataScroller.getVerticalScrollPosition();
    }

    //force browser-flush
    private void preAfterUpdateDOMScroll(SetPendingScrollState pendingState) {
        preAfterUpdateDOMScrollHorizontal(pendingState);
        preAfterUpdateDOMScrollVertical(pendingState);
    }

    private void preAfterUpdateDOMScrollHorizontal(SetPendingScrollState pendingState) {

        int viewportWidth = getViewportWidth();
        boolean hasVerticalScroll = viewportWidth != tableDataScroller.getOffsetWidth();
        if(hasVerticalScroll != this.hasVerticalScroll)
            pendingState.hasVertical = hasVerticalScroll;

        int currentScrollLeft = tableDataScroller.getHorizontalScrollPosition();

        //scroll column to visible if needed
        int scrollLeft = currentScrollLeft;
        int colToShow;
        if (selectedColumnChanged && (colToShow = getSelectedColumn()) >=0 && getRowCount() > 0) {
            TableRowElement tr = tableData.tableElement.getRows().getItem(0);
            TableCellElement td = tr.getCells().getItem(colToShow);

            int columnLeft = td.getOffsetLeft();
            int columnRight = columnLeft + td.getOffsetWidth();
            if (columnRight >= scrollLeft + viewportWidth) // not completely visible from right
                scrollLeft = columnRight - viewportWidth;
            if (columnLeft < scrollLeft) // not completely visible from left
                scrollLeft = columnLeft;
        }

        if(currentScrollLeft != scrollLeft)
            pendingState.left = scrollLeft;

//        updateScrollHorizontal(pendingState);
    }

    private void preAfterUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        int rowCount = getRowCount();

        int tableHeight = 0;
        if (rowCount > 0) {
            TableRowElement lastRowElement = getChildElement(rowCount - 1);
            tableHeight = lastRowElement.getOffsetTop() + lastRowElement.getClientHeight();
        }

        int viewportHeight = tableDataScroller.getClientHeight();
        int currentScrollTop = tableDataScroller.getVerticalScrollPosition();

        int scrollTop = currentScrollTop;

        // we're trying to keep viewport the same after rerendering
        int rerenderedSelectedRow;
        if(pendingState.renderedSelectedScrollTop != null && (rerenderedSelectedRow = getRowByKeyOptimistic(renderedSelectedKey)) >= 0) {
            scrollTop = getChildElement(rerenderedSelectedRow).getOffsetTop() - pendingState.renderedSelectedScrollTop;

            if(scrollTop < 0) // upper than top
                scrollTop = 0;
            if (scrollTop > tableHeight - viewportHeight) // lower than bottom (it seems it can be if renderedSelectedScrollTop is strongly negative)
                scrollTop = tableHeight - viewportHeight;
        }

        //scroll row to visible if needed
        int rowToShow;
        if (selectedRowChanged && (rowToShow = getSelectedRow()) >= 0) {
            TableRowElement rowElement = getChildElement(rowToShow);
            int rowTop = rowElement.getOffsetTop();
            int rowBottom = rowTop + rowElement.getClientHeight();
            if (rowBottom >= scrollTop + viewportHeight) // not completely visible from bottom
                scrollTop = rowBottom - viewportHeight;
            if (rowTop <= scrollTop) // not completely visible from top
                scrollTop = rowTop - 1; // 1 for border
        }

        if(scrollTop != currentScrollTop)
            pendingState.top = scrollTop;

//        updateScrollVertical(pendingState);
    }

    protected int getViewportWidth() {
        return tableDataScroller.getClientWidth();
    }
    public int getViewportHeight() {
        return tableDataScroller.getClientHeight();
    }

    boolean hasVerticalScroll = false;
    private void afterUpdateDOMScroll(SetPendingScrollState pendingState) {
        afterUpdateDOMScrollHorizontal(pendingState);
        afterUpdateDOMScrollVertical(pendingState);
    }

    private void afterUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        if (pendingState.top != null) {
            tableDataScroller.setVerticalScrollPosition(pendingState.top);
        }
    }

    private void afterUpdateDOMScrollHorizontal(SetPendingScrollState pendingState) {
        if(pendingState.hasVertical != null) {
            hasVerticalScroll = pendingState.hasVertical;
            updateTableMargins();
        }

        if (pendingState.left != null) {
            tableDataScroller.setHorizontalScrollPosition(pendingState.left);
        }
    }

    private void updateDataDOM(boolean columnsChanged, ArrayList<Column> dataColumnsChanged) {
        int[] columnsToRedraw = null;
        if(!columnsChanged && dataColumnsChanged != null) { // if only columns has changed
            int size = dataColumnsChanged.size();
            columnsToRedraw = new int[size];
            for (int i = 0 ; i < size; i++)
                columnsToRedraw[i] = getColumnIndex(dataColumnsChanged.get(i));
        }

        tableBuilder.update(tableData.getSection(), getRows(), columnsChanged, columnsToRedraw);

        if(!noScrollers) {
            if (getRowCount() == 0) {
                tableDataScroller.setWidget(emptyTableWidgetContainer);
            } else {
                tableDataScroller.setWidget(tableData);
            }
        }
    }

    private void updateSelectedRowBackgroundDOM() {
        NodeList<TableRowElement> rows = tableData.tableElement.getRows();
        int rowCount = rows.getLength();

        int newLocalSelectedRow = getSelectedRow();

        // CLEAR PREVIOUS STATE
        if (renderedSelectedRow >= 0 && renderedSelectedRow < rowCount &&
                renderedSelectedRow != newLocalSelectedRow) {
            updateSelectedRowCellsBackground(renderedSelectedRow, rows, false, -1);
        }

        // SET NEW STATE
        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < rowCount) {
            updateSelectedRowCellsBackground(newLocalSelectedRow, rows, true, this.isFocused ? getSelectedColumn() : -1);
        }
    }

    private void updateSelectedRowCellsBackground(int row, NodeList<TableRowElement> rows, boolean selected, int focusedColumn) {
        TableRowElement tr = rows.getItem(row);
        NodeList<TableCellElement> cells = tr.getCells();
        for (int column = 0; column < cells.getLength(); column++) {
            TableCellElement td = cells.getItem(column);

            String background = td.getPropertyString(GPropertyTableBuilder.BKCOLOR);
            if(background != null && background.isEmpty())
                background = null;

            String setColor;
            if (selected) {
                setColor = (column == focusedColumn) ? getFocusedCellBackgroundColor(true) : getSelectedRowBackgroundColor(true);
                if (background != null)
                    setColor = mixColors(background, setColor);
            } else {
                assert column != focusedColumn;
                setColor = background != null ? getDisplayColor(background) : null;
            }
            onSelectedChanged(td, row, column, selected);

            GFormController.setBackgroundColor(td, setColor);
        }
    }

    protected abstract void onSelectedChanged(TableCellElement td, int row, int column, boolean selected);

    private void updateFocusedCellDOM() {
        NodeList<TableRowElement> rows = tableData.tableElement.getRows();
        NodeList<TableRowElement> headerRows = noHeaders ? null : tableHeader.tableElement.getTHead().getRows(); // we need headerRows for upper border

        int newLocalSelectedRow = getSelectedRow();
        int newLocalSelectedCol = getSelectedColumn();

        int columnCount = getColumnCount();
        // CLEAR PREVIOUS STATE
        // if old row index is out of bounds only by 1, than we still need to clean top cell, which is in bounds (for vertical direction there is no such problem since we set outer border, and not inner, for horz it's not possible because of header)
        if (renderedSelectedRow >= 0 && renderedSelectedRow <= rows.getLength() && renderedSelectedCol >= 0 && renderedSelectedCol < columnCount &&
                (renderedSelectedRow != newLocalSelectedRow || renderedSelectedCol != newLocalSelectedCol)) {
            setFocusedCellStyles(renderedSelectedRow, renderedSelectedCol, rows, headerRows, false);
        }

        // SET NEW STATE
        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < rows.getLength() && newLocalSelectedCol >= 0 && newLocalSelectedCol < columnCount) {
            setFocusedCellStyles(newLocalSelectedRow, newLocalSelectedCol, rows, headerRows, isFocused);
        }
    }

    private void setFocusedCellStyles(int row, int column, NodeList<TableRowElement> rows, NodeList<TableRowElement> headerRows, boolean focused) {
        // setting left and bottom borders since they are used to draw lines in grid
        int rowCount = rows.getLength();
        int columnCount = getColumnCount();
        assert row >= 0 && row <= rowCount && column >= 0 && column <= columnCount;
        // LEFT, RIGHT AND BOTTOM BORDER
        if(row < rowCount) {
            TableRowElement thisRow = rows.getItem(row);
            NodeList<TableCellElement> cells = thisRow.getCells();

            TableCellElement thisCell = cells.getItem(column);
            if(column < columnCount) {
                // LEFT BORDER
//                if(column > 0) // we don't want focused border on leftmost border to prevent shifting leftmost cell
                setFocusedCellLeftBorder(thisCell, focused);

                // in theory we might want to prevent extra border on the bottom and on the right (on the top there is no problem because of header)
                // but there is a problem (with scroller, in that case we'll have to track hasVerticalScroller, hasHorizontalScroller) + table can be not full of rows (and we also have to track it)
                // so we'll just draw that borders always

                // BOTTOM BORDER
                setFocusedCellBottomBorder(thisCell, focused);

                // RIGHT BORDER
                if (column < columnCount - 1) {
                    TableCellElement rightCell = cells.getItem(column + 1);
                    setFocusedCellLeftBorder(rightCell, focused);
                }
                setFocusedCellRightBorder(thisCell, focused && column == columnCount - 1);
            }
        }

        // TOP BORDER (BOTTOM of upper row)
        if(column < columnCount) {
            TableCellElement upperCell = (row > 0 ? rows.getItem(row - 1) : headerRows.getItem(0)).getCells().getItem(column);
            setFocusedCellBottomBorder(upperCell, focused);
        }
    }

    private void setFocusedCellBottomBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderBottom", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderBottom");
        }
    }

    private void setFocusedCellRightBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderRight", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderRight");
        }
    }
    
    private void setFocusedCellLeftBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderLeft", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderLeft");
        }
    }

    public void updateHeadersDOM(boolean columnsChanged) {
        if (!noHeaders)
            headerBuilder.update(columnsChanged);
        if (!noFooters)
            footerBuilder.update(columnsChanged);
    }

    // mechanism is slightly different - removing redundant columns, resetting others, however there is no that big difference from other updates so will leave it this way
    private void updateWidthsDOM(boolean columnsChanged) {
        int columnCount = getColumnCount();

        if(columnsChanged)
            removeUnusedColumnWidthsImpl(columnCount);
        for (int i = 0; i < columnCount; i++)
            updateColumnWidthImpl(i, getColumnWidth(columns.get(i)));
    }

    private void removeUnusedColumnWidthsImpl(int columnCount) {
        tableData.removeUnusedColumns(columnCount);
        if(!noHeaders)
            tableHeader.removeUnusedColumns(columnCount);
        if(!noFooters)
            tableFooter.removeUnusedColumns(columnCount);
    }

    @Override
    public void colorThemeChanged() {
        columnsChanged();
    }

    protected int selectedRow = -1;
    protected int selectedColumn = -1;

    private static class SetPendingScrollState {
        private Integer renderedSelectedScrollTop; // from selected till upper border

        private Integer top;
        private Integer left;
        private Boolean hasVertical;
    }

    // all this pending is needed for two reasons :
    // 1) update dom once if there are several changes before event loop
    // 2) first do dom changes, then do getOffset* routing thus avoiding unnecessary layouting flushes
    private static UpdateDOMCommand updateDOMCommandStatic;

    private static class UpdateDOMCommand implements Scheduler.ScheduledCommand {
        private final ArrayList<DataGrid> grids = new ArrayList<>();

        @Override
        public void execute() {
            for (DataGrid grid : grids)
                grid.startResolving();

            int size = grids.size();
            boolean[] showing = new boolean[size];
            SetPendingScrollState[] pendingStates = new SetPendingScrollState[size];
            for (int i = 0; i < size; i++) {
                DataGrid grid = grids.get(i);
                if(!grid.noScrollers && GwtClientUtils.isShowing(grid)) { // need this check, since grid can be already hidden (for example when SHOW DOCKED is executed), and in that case get*Width return 0, which leads for example to updateTablePaddings (removing scroll) and thus unnecessary blinking when the grid becomes visible again
                    showing[i] = true;
                    pendingStates[i] = new SetPendingScrollState();
                }
            }

            for (int i = 0; i < size; i++)
                if(showing[i])
                    grids.get(i).beforeUpdateDOMScroll(pendingStates[i]);

            for (DataGrid grid : grids)
                grid.updateDOM();

            // not sure what for there is a separation of reading scroll position from it's setting
            for (int i = 0; i < size; i++)
                if(showing[i])
                    grids.get(i).preAfterUpdateDOMScroll(pendingStates[i]);

            for (int i = 0; i < size; i++)
                if(showing[i])
                    grids.get(i).afterUpdateDOMScroll(pendingStates[i]);

            for (DataGrid grid : grids)
                grid.finishResolving();

            updateDOMCommandStatic = null;
        }

        public static void schedule(DataGrid grid) {
            if (updateDOMCommandStatic == null) {
                updateDOMCommandStatic = new UpdateDOMCommand();
                updateDOMCommandStatic.add(grid);
                Scheduler.get().scheduleFinally(updateDOMCommandStatic);
            } else
                updateDOMCommandStatic.add(grid);
        }

        private void add(DataGrid grid) {
            if(!grids.contains(grid))
                grids.add(grid);
        }
    }

    private void scheduleUpdateDOM() {
        if (isResolvingState)
            throw new IllegalStateException("It's not allowed to change current state, when resolving pending state");

        UpdateDOMCommand.schedule(this);
    }

    private abstract static class TableWrapperWidget extends Widget {
        protected final TableElement tableElement;
        protected final TableColElement colgroupElement;
        protected final TableSectionElement sectionElement;

        public TableWrapperWidget(String extraClass) {
            tableElement = Document.get().createTableElement();

            tableElement.addClassName("dataGridTableWrapperWidget");
            if(extraClass != null)
                tableElement.addClassName(extraClass);

            colgroupElement = tableElement.appendChild(Document.get().createColGroupElement());

            sectionElement = createSectionElement();

            setElement(tableElement);
        }

        protected abstract TableSectionElement createSectionElement();

        /**
         * Get the {@link TableColElement} at the specified index, creating it if
         * necessary.
         *
         * @param index the column index
         * @return the {@link TableColElement}
         */
        public TableColElement ensureTableColElement(int index) {
            // Ensure that we have enough columns.
            for (int i = colgroupElement.getChildCount(); i <= index; i++) {
                colgroupElement.appendChild(Document.get().createColElement());
            }
            return colgroupElement.getChild(index).cast();
        }

        /**
         * Hide columns that aren't used in the table.
         *
         * @param start the first unused column index
         */
        void removeUnusedColumns(int start) {
            // Remove all col elements that appear after the last column.
            int colCount = colgroupElement.getChildCount();
            for (int i = start; i < colCount; i++) {
                colgroupElement.removeChild(ensureTableColElement(start));
            }
        }

        public TableSectionElement getSection() {
            return sectionElement;
        }
    }

    private class HeaderWidget extends TableWrapperWidget {
        public HeaderWidget() {
            super(style.dataGridHeader());
        }

        @Override
        protected TableSectionElement createSectionElement() {
            return tableElement.createTHead();
        }
    }

    private class FooterWidget extends TableWrapperWidget {
        private FooterWidget() {
            super(style.dataGridFooter());
        }

        @Override
        protected TableSectionElement createSectionElement() {
            return tableElement.createTFoot();
        }
    }

    private class TableWidget extends TableWrapperWidget {
//        private final DivElement containerElement;

        public TableWidget() {
            super(null);
        }

        @Override
        protected TableSectionElement createSectionElement() {
            if (tableElement.getTBodies().getLength() > 0) {
                return tableElement.getTBodies().getItem(0);
            } else {
                return tableElement.appendChild(Document.get().createTBodyElement());
            }
        }
    }

    @Override
    public void onResize() {
        if(!isResolvingState) // hack, because in preUpdateScroll there is browser event flush, which causes scheduleDeferred flush => HeaderPanel.forceLayout => onResize and IllegalStateException, everything is really twisted here, so will just suppress ensurePendingState
            scheduleUpdateDOM();
        super.onResize();
    }

    public static abstract class DataGridSelectionHandler<T> {
        protected final DataGrid<T> display;

        public DataGridSelectionHandler(DataGrid<T> display) {
            this.display = display;
        }

        public void onCellBefore(EventHandler handler, Cell cell, Function<Boolean, Boolean> isChangeOnSingleClick) {
            Event event = handler.event;
            boolean changeEvent = GMouseStroke.isChangeEvent(event);
            if (changeEvent || GMouseStroke.isContextMenuEvent(event)) {
                int col = cell.getColumnIndex();
                int row = cell.getRowIndex();
                boolean rowChanged = display.getSelectedRow() != row;
                if ((display.getSelectedColumn() != col) || rowChanged) {

                    changeColumn(col);
                    changeRow(row);

                    if(changeEvent && !isChangeOnSingleClick.apply(rowChanged)) // we'll propagate native events to enable (support) "text selection" feature
                        handler.consume(true, true); // we'll propagate events upper, to process bindings if there are any (for example CTRL+CLICK)
                }
//                else if(BrowserEvents.CLICK.equals(eventType) && // if clicked on grid and element is not natively focusable steal focus
//                        !CellBasedWidgetImpl.get().isFocusable(Element.as(event.getEventTarget())))
//                    display.focus();
            }
        }

        public void onCellAfter(EventHandler handler, Cell cell) {
            Event event = handler.event;
            String eventType = event.getType();
            if (BrowserEvents.KEYDOWN.equals(eventType) && handleKeyEvent(event))
                handler.consume();
        }

        public boolean handleKeyEvent(Event event) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyCodes.KEY_RIGHT:
                    return nextColumn(true);
                case KeyCodes.KEY_LEFT:
                    return nextColumn(false);
                case KeyCodes.KEY_DOWN:
                    return nextRow(true);
                case KeyCodes.KEY_UP:
                    return nextRow(false);
                case KeyCodes.KEY_PAGEDOWN:
                    return changeRow(display.getSelectedRow() + display.pageIncrement);
                case KeyCodes.KEY_PAGEUP:
                    return changeRow(display.getSelectedRow() - display.pageIncrement);
                case KeyCodes.KEY_HOME:
                    return changeRow(0);
                case KeyCodes.KEY_END:
                    return changeRow(display.getRowCount() - 1);
            }
            return false;
        }

        protected boolean changeColumn(int column) {
            return display.changeSelectedColumn(column);
        }
        protected boolean changeRow(int row) {
            display.changeSelectedRow(row);
            return true;
        }

        public boolean nextRow(boolean down) {
            int rowIndex = display.getSelectedRow();
            return changeRow(down ? rowIndex + 1 : rowIndex - 1);
        }

        public boolean nextColumn(boolean forward) {
            int rowCount = display.getRowCount();
            if(rowCount == 0) // not sure if it's needed
                return false;
            int columnCount = display.getColumnCount();

            int rowIndex = display.getSelectedRow();
            int columnIndex = display.getSelectedColumn();

            while(true) {
                if (forward) {
                    if (columnIndex == columnCount - 1) {
                        if (rowIndex != rowCount - 1) {
                            columnIndex = 0;
                            rowIndex++;
                        } else
                            break;
                    } else {
                        columnIndex++;
                    }
                } else {
                    if (columnIndex == 0) {
                        if (rowIndex != 0) {
                            columnIndex = columnCount - 1;
                            rowIndex--;
                        } else
                            break;
                    } else {
                        columnIndex--;
                    }
                }

                if(changeColumn(columnIndex))
                    break;
            }

            return changeRow(rowIndex);
        }
    }

    private boolean wasUnloaded;

    @Override
    protected void onUnload() {
        wasUnloaded = true;
        super.onUnload();
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        // when grid is unloaded and then loaded (when moving from one container to another in maximize / minimize tabspanel, not sure if there are other cases)
        // scroll position changes to 0 (without any event, but that doesn't matter), and we want to keep the selected row, so we mark it as changed, and in afterUpdateDOM method it is ensured that selected cell is visible
        if(wasUnloaded)
            selectedRowChanged();
    }
}
