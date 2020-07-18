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
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.cell.Context;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.Math.min;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.base.view.ColorUtils.mixColors;
import static lsfusion.gwt.client.view.StyleDefaults.*;

// we need resizesimplepanel for "scroller" padding in headers (we don't know if there gonna be vertival scroller)
public abstract class DataGrid<T> extends ResizableSimplePanel implements HasData<T>, Focusable, ColorThemeChangeListener {

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
     * The current state of the presenter reflected in the view.
     */
    private State<T> state;

    /**
     * The pending state of the presenter to be pushed to the view.
     */
    private State<T> pendingState;

    /**
     * A boolean indicating that we are in the process of resolving state.
     */
    protected boolean isResolvingState;

    /**
     * A boolean indicating that the widget is refreshing, so all events should be ignored.
     */
    private boolean isRefreshing;

    /**
     * The command used to resolve the pending state.
     */
    boolean isFocused;

    private final List<Column<T, ?>> columns = new ArrayList<>();
    private final Map<Column<T, ?>, String> columnWidths = new HashMap<>();

    protected final GridStyle style;

    private HeaderBuilder<T> footerBuilder;
    private final List<Header<?>> footers = new ArrayList<>();

    /**
     * Indicates that at least one column handles selection.
     */
    private HeaderBuilder<T> headerBuilder;
    private final List<Header<?>> headers = new ArrayList<>();

    /**
     * Indicates that either the headers or footers are dirty, and both should be
     * refreshed the next time the table is redrawn.
     */
    private boolean columnsChanged;
    private boolean headersChanged;

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

    private int renderedRowCount = 0;

    //focused cell indices local to table (aka real indices in rendered portion of the data)
    int oldLocalSelectedRow = -1;
    int oldLocalSelectedCol = -1;

    private int rowHeight = StyleDefaults.VALUE_HEIGHT;

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

        this.state = new State<>();

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

            headerPanel.addFillFlex(tableDataContainer, 1);

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
            // for tooltips on header
            eventTypes.add(BrowserEvents.MOUSEUP);
            eventTypes.add(BrowserEvents.MOUSEOVER);
            eventTypes.add(BrowserEvents.MOUSEOUT);
            browserMouseEvents = eventTypes;
        }
        return browserMouseEvents;
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
    public static Element getTargetAndCheck(Element element, Event event) {
        EventTarget eventTarget = event.getEventTarget();
        //it seems that now all this is not needed
//        if (!Element.is(eventTarget)) {
//            return null;
//        }
        Element target = Element.as(eventTarget);
//        if (!element.isOrHasChild(Element.as(eventTarget))) {
//            return null;
//        }
        return target;
    }
    public static boolean checkSinkEvents(Event event) {
        String eventType = event.getType();
        return getBrowserFocusEvents().contains(eventType) ||
                getBrowserMouseEvents().contains(eventType) ||
                checkSinkGlobalEvents(event);
    }
    public static boolean checkSinkGlobalEvents(Event event) {
        return getBrowserKeyEvents().contains(event.getType()) || event.getTypeInt() == Event.ONPASTE || event.getType().equals(BrowserEvents.CONTEXTMENU);
    }

    public GridStyle getStyle() {
        return style;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        ensurePendingState();
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public int getPageIncrement() {
        return pageIncrement;
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
    @Override
    public int getRowCount() {
        return getCurrentState().getRowCount();
    }

    @Override
    public T getRowValue(int row) {
        checkRowBounds(row);
        return getCurrentState().getRowValue(row);
    }

    public final void setRowValue(int row, T rowValue) {
        setRowData(row, Arrays.asList(rowValue));
    }

    /**
     * <p>
     * Set the complete list of values to display on one page.
     * </p>
     *
     * @param values
     */
    public final void setRowData(List<? extends T> values) {
        setRowData(0, values, true);
    }

    @Override
    public void setRowData(int start, List<? extends T> values) {
        setRowData(start, values, false);
    }

    private void setRowData(int start, List<? extends T> values, boolean all) {
        assert start >= 0;

        int end = start + values.size();

        if (!all && end == start) {
            //0-range update
            return;
        }

        State<T> pending = ensurePendingState();

        // Insert the new values into the data array.
        int currentCnt = pending.rowData.size();
        for (int i = start; i < end; i++) {
            T value = values.get(i - start);
            if (i < currentCnt) {
                pending.rowData.set(i, value);
            } else {
                pending.rowData.add(value);
            }
        }

        if (all) {
            assert start == 0;

            int removeCnt = currentCnt - values.size();
            for (int i = 1; i <= removeCnt; ++i) {
                pending.rowData.remove(currentCnt - i);
            }
            redraw();
        } else {
            // Redraw the range that has been replaced.
            pending.redrawRows(start, end);
        }
    }

    /**
     * Handle browser events. Subclasses should override
     * {@link #onBrowserEvent2(Element, Event)} if they want to extend browser event
     * handling.
     *
     * @see #onBrowserEvent2(Element, Event)
     */
    @Override
    public final void onBrowserEvent(Event event) {
        // Ignore spurious events (such as onblur) while we refresh the table.
        if (isRefreshing) {
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
        if(!previewClickEvent(target, event))
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

//        else if (BrowserEvents.KEYDOWN.equals(eventType)) {
//            // A key event indicates that we already have focus.
//            isFocused = true;
//        } else if (BrowserEvents.MOUSEDOWN.equals(eventType)
//                && CellBasedWidgetImpl.get().isFocusable(Element.as(target))) {
//            // If a natively focusable element was just clicked, then we must have
//            // focus.
//            isFocused = true;
//        }

        // Let subclasses handle the event now.
        onBrowserEvent2(target, event);
    }

    @SuppressWarnings("deprecation")
    protected <C> void onBrowserEvent2(Element target, Event event) {
        // Find the cell where the event occurred.
        TableSectionElement tbody = getTableBodyElement();
        TableSectionElement tfoot = getTableFootElement();
        TableSectionElement thead = getTableHeadElement();

        int row = -1;
        Column<T, C> column = null;
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
                    column = (Column<T, C>) tableBuilder.getColumn(columnParent);
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
                    column = (Column<T, C>) tableBuilder.getColumn(cur);
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
            if (column != null) {
                Context context = new Context(row, getColumnIndex(column), getRowValue(row));

                EventHandler handler = new EventHandler(event);

                onBrowserEvent(context, handler, column, columnParent);
            }
        }
    }

    public abstract <C> void onBrowserEvent(Context context, EventHandler handler, Column<T, C> column, Element parent);

    protected void checkRowBounds(int row) {
        if (!isRowWithinBounds(row)) {
            throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: " + getRowCount());
        }
    }

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
     * Checks that the row is currently rendered in the table
     *
     * @param row row index to check
     * @return true if row is rendered, false if not
     */
    protected boolean isRowRendered(int row) {
        return row >= 0 && row < renderedRowCount;
    }

    /**
     * Adds a column to the end of the table with an associated header.
     *
     * @param col    the column to be added
     * @param header the associated {@link Header}
     */
    public void addColumn(Column<T, ?> col, Header<?> header) {
        insertColumn(getColumnCount(), col, header);
    }

    /**
     * Inserts a column into the table at the specified index with an associated
     * header.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     * @param header      the associated {@link Header}
     */
    public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header) {
        insertColumn(beforeIndex, col, header, null);
    }

    /**
     * Inserts a column into the table at the specified index with an associated
     * header and footer.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     * @param header      the associated {@link Header}
     * @param footer      the associated footer (as a {@link Header} object)
     * @throws IndexOutOfBoundsException if the index is out of range
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
        if (beforeIndex <= selectedColumn) {
            ensurePendingState().selectedColumn = min(selectedColumn + 1, columns.size() - 1);
        }

        refreshColumnsAndRedraw();
    }

    public void moveColumn(int oldIndex, int newIndex) {
        checkColumnBounds(oldIndex);
        checkColumnBounds(newIndex);
        if (oldIndex == newIndex) {
            return;
        }

        int selectedColumn = getSelectedColumn();
        if (oldIndex == selectedColumn) {
            ensurePendingState().selectedColumn = newIndex;
        } else if (oldIndex < selectedColumn && selectedColumn > 0) {
            ensurePendingState().selectedColumn--;
        }

        Column<T, ?> column = columns.remove(oldIndex);
        Header<?> header = headers.remove(oldIndex);
        Header<?> footer = footers.remove(oldIndex);

        columns.add(newIndex, column);
        headers.add(newIndex, header);
        footers.add(newIndex, footer);

        // Redraw the table asynchronously.
        refreshColumnsAndRedraw();
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
        if (index <= selectedColumn && selectedColumn > 0) {
            ensurePendingState().selectedColumn = selectedColumn - 1;
        }

        // Redraw the table asynchronously.
        refreshColumnsAndRedraw();

        // We don't unsink events because other handlers or user code may have sunk them intentionally.
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
        redraw();
    }

    public void refreshColumnsAndRedraw() {
        columnsChanged = true;
        redraw();
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
        return getCurrentState().getSelectedColumn();
    }

    protected TableRowElement getChildElement(int row) {
        return getRowElementNoFlush(row);
    }

    public void setColumnWidth(Column<T, ?> column, String width) {
        columnWidths.put(column, width);
        updateColumnWidthImpl(column, width);
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

        setSelectedRow(row);

        rowChangedHandler.run();
    }

    public void setSelectedColumn(int column) {
        assert column >= 0 : "Column must be zero or greater";

        if (getSelectedColumn() == column)
            return;

        ensurePendingState().setSelectedColumn(column);
    }

    public boolean isFocusable(int column) {
        return getColumn(column).isFocusable();
    }
    public boolean isFocusable(Context context) {
        return isFocusable(context.getColumn());
    }
    public boolean isEditOnSingleClick(Context context) {
        return !isFocusable(context);
    }

    public void setSelectedRow(int row) {
        if (getSelectedRow() == row)
            return;

        ensurePendingState().setSelectedRow(row);
    }

    protected void setDesiredVerticalScrollPosition(int newVerticalScrollPosition) {
        ensurePendingState().desiredVerticalScrollPosition = newVerticalScrollPosition;
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
            setBlockMinWidth(minWidthValue + (hasVerticalScroll ? nativeScrollbarWidth : 0), headerWidget);
    }

    private void updateTableMargins() {
        if(!noHeaders)
            updateHeaderTablePadding(tableHeader, tableHeaderScroller);
        if(!noFooters)
            updateHeaderTablePadding(tableHeader, tableFooterScroller);
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

    /**
     * Get a row element given the index of the row value.
     *
     * @param row the absolute row value index
     * @return the row element, or null if not found
     */
    private TableRowElement getRowElementNoFlush(int row) {
        if (!isRowRendered(row)) {
            return null;
        }

        return getTableBodyElement().getRows().getItem(row);
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

    protected abstract boolean previewClickEvent(Element target, Event event);

    protected void onFocus() {
        isFocused = true;
        DataGrid.sinkPasteEvent(getTableDataFocusElement());

        updateSelectedRowStyles();
    }

    protected void onBlur(Event event) {
        isFocused = false;

        updateSelectedRowStyles();
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

    /**
     * Update the width of all instances of the specified column. A column
     * instance may appear multiple times in the table.
     *
     * @param column the column to update
     * @param width  the width of the column, or null to clear the width
     */
    private void updateColumnWidthImpl(Column<T, ?> column, String width) {
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (columns.get(i) == column) {
                setColumnWidthImpl(i, width);
            }
        }
    }

    private void setColumnWidthImpl(int column, String width) {
        if (width == null) {
            tableData.ensureTableColElement(column).getStyle().clearWidth();
            if(!noHeaders)
                tableHeader.ensureTableColElement(column).getStyle().clearWidth();
            if(!noFooters)
                tableFooter.ensureTableColElement(column).getStyle().clearWidth();
        } else {
            tableData.ensureTableColElement(column).getStyle().setProperty("width", width);
            if(!noHeaders)
                tableHeader.ensureTableColElement(column).getStyle().setProperty("width", width);
            if(!noFooters)
                tableFooter.ensureTableColElement(column).getStyle().setProperty("width", width);
        }
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
        return getCurrentState().getSelectedRow();
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

    /**
     * Redraw the list with the current data.
     */
    public void redraw() {
        ensurePendingState().redrawAllRows();
        ensurePendingState().redrawAllColumns();
    }

    public void redrawColumns(Set<? extends Column> updatedColumns) {
        redrawColumns(updatedColumns, true);
    }

    public void redrawColumns(Set<? extends Column> updatedColumns, boolean redrawAllRows) {
        ensurePendingState().redrawColumns(updatedColumns);
        if (redrawAllRows) {
            pendingState.redrawAllRows();
        }
    }

    /**
     * Mark the headers as dirty and redraw the table.
     */
    public void refreshHeaders() {
        if (pendingState != null) {
            //already pending some redraw, so render headers there...
            headersChanged = true;
        } else {
            //just update headers
            updateHeadersImpl(false);
        }
    }

    /**
     * Get the current state of the presenter.
     *
     * @return the pending state if one exists, otherwise the state
     */
    private State<T> getCurrentState() {
        return pendingState == null ? state : pendingState;
    }

    /**
     * Ensure that a pending {@link DefaultState} exists and return it.
     *
     * @return the pending state
     */
    private State<T> ensurePendingState() {
        if (isResolvingState) {
            throw new IllegalStateException("It's not allowed to change current state, when resolving pending state");
        }

        // Create the pending state if needed.
        if (pendingState == null) {
            pendingState = new State<>(state);
            PendingStateCommand.schedule(this);
        }

        return pendingState;
    }

    protected void resolvePendingStateBeforeUpdate() {
        if (isResolvingState || pendingState == null) {
            return;
        }

        isResolvingState = true;
    }

    protected void resolvePendingStateUpdate() {
        if (!isResolvingState) {
            return;
        }
        if (columnsChanged) {
            refreshColumnWidths();
        }

        if (columnsChanged || headersChanged) {
            updateHeadersImpl(columnsChanged);
        }

        isRefreshing = true;

        updateTableData(pendingState);

        isRefreshing = false;
    }

    protected void preResolvePendingStateAfterUpdate() {
        int rowToShow = pendingState.selectedRowSet ? pendingState.selectedRow : -1;
        int colToShow = pendingState.selectedColumnSet ? pendingState.selectedColumn : -1;

        //force browser-flush
        preAfterUpdateTableData(pendingState, rowToShow, colToShow);
    }

    protected void resolvePendingStateAfterUpdate() {
        if (!isResolvingState) {
            return;
        }

        isRefreshing = true;

        afterUpdateTableData(pendingState);

        isRefreshing = false;

        headersChanged = false;
        columnsChanged = false;

        updateSelectedRowStyles();

        state = pendingState;
        pendingState = null;
        isResolvingState = false;
    }

    private void preAfterUpdateTableData(State<T> pendingState, int rowToShow, int colToShow) {
        if(!noScrollers) {
            preAfterUpdateScrollHorizontal(pendingState, colToShow);
            preAfterUpdateScrollVertical(pendingState, rowToShow);
        }
    }

    private void preAfterUpdateScrollHorizontal(State<T> pendingState, int colToShow) {
        int colCount = getColumnCount();

        int scrollWidth = tableDataScroller.getClientWidth();
        boolean hasVerticalScroll = scrollWidth != tableDataScroller.getOffsetWidth();
        int currentScrollLeft = tableDataScroller.getHorizontalScrollPosition();

        //scroll column to visible if needed
        int scrollLeft = currentScrollLeft;
        if (colToShow >=0 && colToShow < colCount && renderedRowCount > 0) {
            TableRowElement tr = tableData.tableElement.getRows().getItem(0);
            TableCellElement td = tr.getCells().getItem(colToShow);

            int columnLeft = td.getOffsetLeft();
            int columnRight = columnLeft + td.getOffsetWidth();
            if (columnRight >= scrollLeft + scrollWidth) {
                scrollLeft = columnRight - scrollWidth;
            }

            if (columnLeft < scrollLeft) {
                scrollLeft = columnLeft;
            }
        }

        pendingState.pendingScrollLeft = scrollLeft;
        pendingState.pendingCurrentScrollLeft = currentScrollLeft;
        pendingState.pendingHasVerticalScroll = hasVerticalScroll;
    }

    private void preAfterUpdateScrollVertical(State<T> pendingState, int rowToShow) {
        int rowCount = pendingState.rowData.size();

        int tableHeight = 0;
        if (rowCount > 0) {
            TableRowElement lastRowElement = getChildElement(rowCount - 1);
            tableHeight = lastRowElement.getOffsetTop() + lastRowElement.getClientHeight();
        }

        int scrollHeight = tableDataScroller.getClientHeight();
        int currentScrollTop = tableDataScroller.getVerticalScrollPosition();
        int scrollTop = pendingState.desiredVerticalScrollPosition;

        if (tableHeight <= scrollHeight) {
            scrollTop = 0;
        } else {
            if (scrollTop < 0) {
                scrollTop = currentScrollTop;
            }
            if (scrollTop > tableHeight - scrollHeight) {
                scrollTop = tableHeight - scrollHeight;
            }
        }

        //scroll row to visible if needed
        if (rowToShow >= 0 && rowToShow < rowCount) {
            TableRowElement rowElement = getChildElement(rowToShow);
            int rowTop = rowElement.getOffsetTop();
            int rowBottom = rowTop + rowElement.getClientHeight();
            if (rowBottom >= scrollTop + scrollHeight) {
                scrollTop = rowBottom - scrollHeight;
            }

            if (rowTop < scrollTop) {
                scrollTop = rowTop;
            }
        }

        pendingState.pendingScrollTop = scrollTop;
        pendingState.pendingCurrentScrollTop = currentScrollTop;
    }

    boolean hasVerticalScroll = false;
    private void afterUpdateTableData(State<T> pendingState) {
        if(!noScrollers) {
            afterUpdateScrollHorizontal(pendingState);
            afterUpdateScrollVertical(pendingState);
        }
    }

    private void afterUpdateScrollVertical(State<T> pendingState) {
        if (pendingState.pendingScrollTop != pendingState.pendingCurrentScrollTop) {
            tableDataScroller.setVerticalScrollPosition(pendingState.pendingScrollTop);
        }
    }

    private void afterUpdateScrollHorizontal(State<T> pendingState) {
        if(!noScrollers) {
            boolean newHasVerticalScroll = pendingState.pendingHasVerticalScroll;
            if(hasVerticalScroll != newHasVerticalScroll) {
                hasVerticalScroll = newHasVerticalScroll;
                updateTableMargins();
            }
        }

        if (pendingState.pendingScrollLeft != pendingState.pendingCurrentScrollLeft) {
            tableDataScroller.setHorizontalScrollPosition(pendingState.pendingScrollLeft);
        }
    }

    private void updateTableData(State<T> pendingState) {
        int pendingNewRowCnt = pendingState.getRowCount();
        if (pendingState.needRedraw() || columnsChanged || renderedRowCount != pendingNewRowCnt) {
            tableBuilder.update(tableData.getSection(), pendingState.rowData, 0, pendingNewRowCnt, columnsChanged);

            renderedRowCount = pendingNewRowCnt;
            if(!noScrollers) {
                if (renderedRowCount == 0) {
                    tableDataScroller.setWidget(emptyTableWidgetContainer);
                } else {
                    tableDataScroller.setWidget(tableData);
                }
            }
        }
    }

    private void updateSelectedRowStyles() {
        int newLocalSelectedRow = getSelectedRow();
        int newLocalSelectedCol = getSelectedColumn();

        updateSelectedRow(newLocalSelectedRow, newLocalSelectedCol);

        if (drawFocusedCellBorder())
            updateFocusedCell(newLocalSelectedRow, newLocalSelectedCol);

        oldLocalSelectedRow = newLocalSelectedRow;
        oldLocalSelectedCol = newLocalSelectedCol;
    }

    private void updateSelectedRow(int newLocalSelectedRow, int newLocalSelectedCol) {
        NodeList<TableRowElement> rows = tableData.tableElement.getRows();
        int rowCount = rows.getLength();

        // CLEAR PREVIOUS STATE
        if (oldLocalSelectedRow >= 0 && oldLocalSelectedRow < rowCount &&
                oldLocalSelectedRow != newLocalSelectedRow) {
            updateSelectedRowCellsBackground(oldLocalSelectedRow, rows, false, -1);
        }

        // SET NEW STATE
        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < rowCount) {
            updateSelectedRowCellsBackground(newLocalSelectedRow, rows, true, this.isFocused ? newLocalSelectedCol : -1);
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

            GFormController.setBackgroundColor(td, setColor);
        }
    }

    private void updateFocusedCell(int newLocalSelectedRow, int newLocalSelectedCol) {
        NodeList<TableRowElement> rows = tableData.tableElement.getRows();
        NodeList<TableRowElement> headerRows = noHeaders ? null : tableHeader.tableElement.getTHead().getRows(); // we need headerRows for upper border

        int columnCount = getColumnCount();
        // CLEAR PREVIOUS STATE
        // if old row index is out of bounds only by 1, than we still need to clean top cell, which is in bounds (for vertical direction there is no such problem since we set outer border, and not inner, for horz it's not possible because of header)
        if (oldLocalSelectedRow >= 0 && oldLocalSelectedRow <= rows.getLength() && oldLocalSelectedCol >= 0 && oldLocalSelectedCol < columnCount &&
                (oldLocalSelectedRow != newLocalSelectedRow || oldLocalSelectedCol != newLocalSelectedCol)) {
            setFocusedCellStyles(oldLocalSelectedRow, oldLocalSelectedCol, rows, headerRows, false);
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

    protected boolean drawFocusedCellBorder() {
        return true;
    }

    private void updateHeadersImpl(boolean columnsChanged) {
        if (!noHeaders)
            headerBuilder.update(columnsChanged);
        if (!noFooters)
            footerBuilder.update(columnsChanged);
    }

    private void refreshColumnWidths() {
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            setColumnWidthImpl(i, getColumnWidth(columns.get(i)));
        }

        // Hide unused col elements in the colgroup.
        if(!noHeaders)
            tableHeader.hideUnusedColumns(columnCount);
        tableData.hideUnusedColumns(columnCount);
        if(!noFooters)
            tableFooter.hideUnusedColumns(columnCount);
    }

    @Override
    public void colorThemeChanged() {
        refreshColumnsAndRedraw();
    }

    /**
     * Represents the pending state of the presenter.
     *
     * @param <T> the data type of the presenter
     */
    private static class State<T> {
        protected final List<T> rowData;
        protected int selectedRow = 0;
        protected int selectedColumn = 0;

        private int desiredVerticalScrollPosition = -1;

        private boolean selectedRowSet = false;
        private boolean selectedColumnSet = false;

        /**
         * The list of ranges that has to be redrawn.
         */
        private List<Range> rangesToRedraw = null;
        private Set<Column> columnsToRedraw = null;
        private boolean redrawAllColumns = false;

        //values, used for delayed updating
        int pendingScrollTop;
        int pendingScrollLeft;
        int pendingCurrentScrollTop;
        int pendingCurrentScrollLeft;
        boolean pendingHasVerticalScroll;

        public State() {
            this(new ArrayList<T>());
        }

        public State(ArrayList<T> rowData) {
            this.rowData = rowData;
        }

        public State(State<T> state) {
            this(new ArrayList<>(state.rowData));
            this.selectedRow = state.getSelectedRow();
            this.selectedColumn = state.getSelectedColumn();
        }

        public int getSelectedRow() {
            return selectedRow;
        }

        public void setSelectedRow(int selectedRow) {
            this.selectedRow = selectedRow;
            this.selectedRowSet = true;
        }

        public int getSelectedColumn() {
            return selectedColumn;
        }

        public void setSelectedColumn(int selectedColumn) {
            this.selectedColumn = selectedColumn;
            this.selectedColumnSet = true;
        }

        public int getRowCount() {
            return rowData.size();
        }

        public T getRowValue(int index) {
            return rowData.get(index);
        }

        private List<Range> createRangesToRedraw() {
            if (rangesToRedraw == null) {
                rangesToRedraw = new ArrayList<>();
            }
            return rangesToRedraw;
        }

        private Set<Column> createColumnsToRedraw() {
            if (columnsToRedraw == null) {
                columnsToRedraw = new HashSet<>();
            }
            return columnsToRedraw;
        }

        /**
         * Update the range data to be redraw.
         *
         * @param start the start index
         * @param end   the end index (excluded)
         */
        public void redrawRows(int start, int end) {
            // merge ranges on insertion

            if (createRangesToRedraw().size() == 0) {
                rangesToRedraw.add(new Range(start, end));
                return;
            }

            int rangeCnt = rangesToRedraw.size();

            int startIndex;
            for (startIndex = 0; startIndex < rangeCnt; ++startIndex) {
                Range prevRange = rangesToRedraw.get(startIndex);
                if (prevRange.start > start) {
                    break;
                }
            }

            if (startIndex > 0) {
                //range previous range

                //prevRange.start < start because of cycle break condition
                Range prevRange = rangesToRedraw.get(startIndex - 1);
                if (prevRange.end >= end) {
                    //fully included to the bigger range
                    return;
                }

                if (prevRange.end >= start) {
                    //merge prevRange with this one
                    rangeCnt--;
                    startIndex--;
                    rangesToRedraw.remove(startIndex);
                    start = prevRange.start;
                }
            }

            //merge following ranges
            if (startIndex < rangeCnt) {
                Range nextRange = rangesToRedraw.get(startIndex);
                while (nextRange.start <= end) {
                    rangeCnt--;
                    rangesToRedraw.remove(startIndex);
                    if (nextRange.end > end) {
                        //extend current range and break if merged range is farther
                        end = nextRange.end;
                        break;
                    }

                    if (startIndex == rangeCnt) {
                        break;
                    }

                    nextRange = rangesToRedraw.get(startIndex);
                }
            }

            rangesToRedraw.add(startIndex, new Range(start, end));
        }

        public void redrawAllRows() {
            createRangesToRedraw().clear();
            redrawRows(0, rowData.size());
        }

        public boolean needRedraw() {
            return rangesToRedraw != null && (redrawAllColumns || columnsToRedraw != null);
        }

        public void redrawColumns(Set<? extends Column> updatedColumns) {
            if (!redrawAllColumns) {
                createColumnsToRedraw().addAll(updatedColumns);
            }
        }

        public void redrawAllColumns() {
            redrawAllColumns = true;
            columnsToRedraw = null;
        }

        public int[] getColumnsToRedraw(DataGrid thisGrid) {
            if (redrawAllColumns) {
                return null;
            }

            int[] columnsToRedrawIndices = null;
            if (columnsToRedraw != null) {
                columnsToRedrawIndices = new int[columnsToRedraw.size()];
                int i = 0;
                for (Column column : columnsToRedraw) {
                    columnsToRedrawIndices[i++] = thisGrid.getColumnIndex(column);
                }
            }
            return columnsToRedrawIndices;
        }
    }

    private static PendingStateCommand pendingStateCommandStatic;

    private static class PendingStateCommand implements Scheduler.ScheduledCommand {
        private final ArrayList<DataGrid> grids = new ArrayList<>();

        private PendingStateCommand(DataGrid grid) {
            grids.add(grid);
        }

        @Override
        public void execute() {
            for (DataGrid grid : grids) {
                grid.resolvePendingStateBeforeUpdate();
            }
            for (DataGrid grid : grids) {
                grid.resolvePendingStateUpdate();
            }
            for (DataGrid grid : grids) {
                grid.preResolvePendingStateAfterUpdate();
            }
            for (DataGrid grid : grids) {
                grid.resolvePendingStateAfterUpdate();
            }
            pendingStateCommandStatic = null;
        }

        public static void schedule(DataGrid grid) {
            if (pendingStateCommandStatic == null) {
                pendingStateCommandStatic = new PendingStateCommand(grid);
                Scheduler.get().scheduleFinally(pendingStateCommandStatic);
            } else {
                pendingStateCommandStatic.add(grid);
            }
        }

        private void add(DataGrid grid) {
            grids.add(grid);
        }
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
        void hideUnusedColumns(int start) {
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
        if(!isResolvingState) // hack, because in preAfterUpdateTableData there is browser event flush, which causes scheduleDeferred flush => HeaderPanel.forceLayout => onResize and IllegalStateException, everything is really twisted here, so will just suppress ensurePendingState
            ensurePendingState();
        super.onResize();
    }

    public static abstract class DataGridSelectionHandler<T> {
        protected final DataGrid<T> display;

        public DataGridSelectionHandler(DataGrid<T> display) {
            this.display = display;
        }

        public void onCellBefore(EventHandler handler, Context context, Supplier<Boolean> isEditOnSingleClick) {
            Event event = handler.event;
            if (GMouseStroke.isChangeEvent(event) || GMouseStroke.isContextMenuEvent(event)) {
                int col = context.getColumn();
                int row = context.getIndex();
                if ((display.getSelectedColumn() != col) || (display.getSelectedRow() != row)) {

                    changeColumn(col);
                    changeRow(row);

                    if(!isEditOnSingleClick.get())
                        handler.consume(); // we need to propagate at least MOUSEDOWN since native handler is needed for focus event
                }
//                else if(BrowserEvents.CLICK.equals(eventType) && // if clicked on grid and element is not natively focusable steal focus
//                        !CellBasedWidgetImpl.get().isFocusable(Element.as(event.getEventTarget())))
//                    display.focus();
            }
        }

        public void onCellAfter(EventHandler handler, Context context) {
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
            if (display.renderedRowCount > 0) {
                int rowCount = display.getRowCount();
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
            return false;
        }
    }
}
