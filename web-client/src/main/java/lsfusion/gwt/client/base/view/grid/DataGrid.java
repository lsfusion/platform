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
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.TableComponent;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Math.min;
import static lsfusion.gwt.client.base.view.ColorUtils.getThemedColor;

public abstract class DataGrid<T> implements TableComponent, ColorThemeChangeListener, HasMaxPreferredSize {

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

    private final ArrayList<Column<T, ?>> columns = new ArrayList<>();

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
    private boolean onResizeChanged;

    protected GPropertyTableBuilder<T> tableBuilder;

    protected final TableWidget tableWidget;
    protected final TableContainer tableContainer;

    protected DataGridSelectionHandler selectionHandler;

    public void setSelectionHandler(DataGridSelectionHandler selectionHandler) {
        this.selectionHandler = selectionHandler;
    }

    //focused cell indices local to table (aka real indices in rendered portion of the data)
    int renderedSelectedRow = -1;
    int renderedSelectedCol = -1;
    int renderedLeftStickyCol = -1;
    Object renderedSelectedKey = null; // needed for saving scroll position when keys are update
    Integer renderedSelectedExpandingIndex = null; // needed for saving scroll position when keys are update

    protected abstract Object getSelectedKey();
    protected abstract Integer getSelectedExpandingIndex();
    // virtualKey - null means that we're looking for any selected key but object other key
    protected int getRowByKey(Object key, Integer expandingIndex) {
        Object selectedKey = getSelectedKey();
        if(selectedKey != null && (selectedKey.equals(key) && (expandingIndex == null || expandingIndex.equals(getSelectedExpandingIndex())))) // optimization the most common case
            return getSelectedRow();

        return findRowByKey(key, expandingIndex == null ? GridDataRecord.objectExpandingIndex : expandingIndex);
    }
    protected abstract int findRowByKey(Object key, int expandingIndex);

    private int pageIncrement = 30;

    protected final boolean noHeaders;
    protected final boolean noFooters;
    private final boolean noScrollers;

    public DataGrid(TableContainer tableContainer, boolean noHeaders, boolean noFooters) {
        this.tableContainer = tableContainer;

        this.noHeaders = noHeaders;
        this.noFooters = noFooters;
        noScrollers = tableContainer.autoSize;

        // INITIALIZING MAIN DATA
        tableWidget = new TableWidget();

        // INITIALIZING HEADERS
        if(!noHeaders) {
            headerBuilder = new DefaultHeaderBuilder<>(this, false);
        }

        // INITIALIZING FOOTERS
        if (!noFooters) { // the same as for headers
            footerBuilder = new DefaultHeaderBuilder<>(this, true);
        }

        getTableDataFocusElement().setTabIndex(0);
        initSinkEvents(tableContainer);

        MainFrame.addColorThemeChangeListener(this);
    }
    
    public ScrollHandler getScrollHandler() {
        return event -> {
            calcLeftNeighbourRightBorder(true);
            checkSelectedRowVisible();
        };
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
//            eventTypes.addAll(getBrowserFocusEvents());
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

        widget.sinkEvents(Event.ONPASTE | Event.ONCONTEXTMENU | Event.ONCHANGE);
    }
    // the problem that onpaste ('paste') event is not triggered when there is no "selection" inside element (or no other contenteditable element)
    // also it's important that focusElement should have text, thats why EscapeUtils.UNICODE_NBSP is used in a lot of places
    public static void sinkPasteEvent(Element focusElement) {
        CopyPasteUtils.setEmptySelection(focusElement);
    }

    public static void initSinkMouseEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserMouseEvents());
    }

    public static void initSinkFocusEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserFocusEvents());
    }

    public static void initSinkDragDropEvents(Widget widget) {
        CellBasedWidgetImpl.get().sinkEvents(widget, getBrowserDragDropEvents());
    }

    public static boolean isFakeBlur(Event event, Element blur) {
        EventTarget focus = event.getRelatedEventTarget();
        return focus != null && blur.isOrHasChild(Element.as(focus));
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
    public static boolean isMouseEvent(Event event) {
        String eventType = event.getType();
        return getBrowserMouseEvents().contains(eventType);
    }
    public static boolean checkSinkEvents(Event event) {
        String eventType = event.getType();
        return getBrowserMouseEvents().contains(eventType) ||
                getBrowserDragDropEvents().contains(eventType) ||
                checkSinkGlobalEvents(event);
    }
    public static boolean checkSinkFocusEvents(Event event) {
        String eventType = event.getType();
        return getBrowserFocusEvents().contains(eventType);
    }
    public static boolean checkSinkGlobalEvents(Event event) {
        return getBrowserKeyEvents().contains(event.getType()) || event.getTypeInt() == Event.ONPASTE || event.getType().equals(BrowserEvents.CONTEXTMENU) || event.getType().equals(BrowserEvents.CHANGE);
    }

    public void setPageIncrement(int pageIncrement) {
        this.pageIncrement = Math.max(1, pageIncrement);
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

    public final void onBrowserEvent(Event event) {
        // Ignore spurious events (such as onblur) while we refresh the table.
        if (isResolvingState) {
            return;
        }

        Element target = getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!previewEvent(target, event))
            return;

        // here grid handles somewhy other than sink events, so will check it after super
        if(!checkSinkEvents(event))
            return;

        onGridBrowserEvent(target, event);
    }

    public void onGridBrowserEvent(Element target, Event event) {
        // moved to GridContainerPanel
//        String eventType = event.getType();
//        if (BrowserEvents.FOCUS.equals(eventType))
//            onFocus();
//        else if (BrowserEvents.BLUR.equals(eventType))
//            onBlur(event);

        // Find the cell where the event occurred.
        TableSectionElement tbody = getTableBodyElement();
        TableSectionElement tfoot = getTableFootElement();
        TableSectionElement thead = getTableHeadElement();

        int row = -1;
        Column column = null;
        TableCellElement columnParent = null;

        Header header = null;
        Element headerParent = null;
        Header footer = null;
        Element footerParent = null;

        TableSectionElement targetTableSection = null;

        if (target == getTableDataFocusElement() || GKeyStroke.isPasteFromClipboardEvent(event)) { // need this when focus is on grid and not cell itself, so we need to propagate key events there
            // usually all events has target getTableDataFocusElement, but ONPASTE when focus is on grid somewhy has target tableElement (in last version something has changed and now target is copied cell, however it is also undesirable)
            if (checkSinkGlobalEvents(event)) {
                targetTableSection = tbody;

                row = getSelectedRow();
                columnParent = getSelectedElement(getSelectedColumn());
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
                        columnParent = (TableCellElement) cur; // COLUMN_ATTRIBUTE is only set for TableCellElement
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
                onBrowserEvent(new Cell(row, getColumnIndex(column), column, (RowIndexHolder) getRowValue(row)), event, column, columnParent);
        }
    }

    public abstract <C> void onBrowserEvent(Cell cell, Event event, Column<T, C> column, TableCellElement parent);

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
            setSelectedColumn(columns.size() - 1);
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
        if (index <= selectedColumn) {
            if (selectedColumn == 0 && columns.size() > 0)
                setSelectedColumn(0);
            else
                setSelectedColumn(selectedColumn - 1);
        }
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
        if(!isResolvingState) // hack, grid elements can have focus and not be in editing mode (for example input) and removing such elements will lead to blur
            scheduleUpdateDOM();
    }

    public void onResizeChanged() {
        onResizeChanged = true;
        if(!isResolvingState) // hack, because in preUpdateScroll there is browser event flush, which causes scheduleDeferred flush => HeaderPanel.forceLayout => onResize and IllegalStateException, everything is really twisted here, so will just suppress ensurePendingState
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

    public int getFocusedColumn() {
        return this.isFocused ? getSelectedColumn() : -1;
    }

    public boolean isSelectedRow(Cell cell) {
        return getSelectedRow() == cell.getRowIndex();
    }

    public boolean isFocusedColumn(Cell cell) {
        return getFocusedColumn() == cell.getColumnIndex();
    }

    protected TableRowElement getChildElement(int row) {
        return getRowElementNoFlush(row);
    }

    protected abstract GSize getColumnWidth(int column);
    protected abstract double getColumnFlexPerc(int column);
    public abstract boolean isColumnFlex(int column);

    public int getFullColumnWidth(int index) {
        return GwtClientUtils.getFullWidth(getWidthElement(index));
    }
    public int getClientColumnWidth(int index) {
        return GwtClientUtils.getWidth(getWidthElement(index));
    }

    // when row or column are changed by keypress in grid (KEYUP, PAGEUP, etc), no focus is lost
    // so to handle this there are to ways of doing that, either with GFormController.checkCommitEditing, or moving focus to grid
    // so far will do it with checkCommitEditing (like in form bindings)
    // see overrides
    public void changeSelectedCell(int row, int column, FocusUtils.Reason reason) {
        // there are index checks inside, sometimes they are redundant, sometimes not
        // however not it's not that important as well, as if the row / column actually was changed
        changeSelectedRow(row);
        changeSelectedColumn(column);
    }

    private void changeSelectedColumn(int column) {
        int columnCount = getColumnCount();
        if(columnCount == 0)
            return;
        if (column < 0)
            column = 0;
        else if (column >= columnCount)
            column = columnCount - 1;

        assert isFocusable(column);
        setSelectedColumn(column);
    }
    private void changeSelectedRow(int row) {
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

    public Cell getSelectedCell() {
        return getSelectedCell(getSelectedColumn());
    }

    public Cell getSelectedCell(int column) {
        return new Cell(getSelectedRow(), column, getColumn(column), (RowIndexHolder) getSelectedRowValue());
    }

    public void setSelectedColumn(int column) {
        assert column >= 0 || columns.size() == 0 : "Column must be zero or greater";

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

    protected void focusColumn(int columnIndex, FocusUtils.Reason reason) {
        if (columnIndex == -1 || !isFocusable(columnIndex))
            return;
        focus(reason);
        selectionHandler.changeColumn(columnIndex, reason);
    }

    public abstract void focus(FocusUtils.Reason reason);

    public boolean isChangeOnSingleClick(Cell cell, Event event, boolean rowChanged, Column column) {
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

    @Override
    public void setPreferredSize(boolean set, Result<Integer> grids) {
        FlexPanel.setMaxPrefWidth(getElement(), set ? "min-content" : null);

        grids.set(grids.result + 1);
    }

    /* see DataGrid.css, dataGridTableWrapperWidget description */
    public static void updateTablePadding(boolean hasVerticalScroll, Element tableElement) {
        if(hasVerticalScroll)
            tableElement.getStyle().setPaddingRight(nativeScrollbarWidth + 1, Unit.PX); // 1 for right outer border margin
        else
            tableElement.getStyle().clearPaddingRight();
    }

    // there are two ways to remove outer grid borders :
    // 1) set styles for outer cells and set border-left/top : none there
    // 2) set margins (not paddings since they do not support negative values) to -1
    // first solution is cleaner (since border can be wider than 1px + margin can be used for other purposes, for example scroller padding), however the problem is that in that case the same should be implemented for focus which is pretty tricky
    // the second solution is easier to implement
    // the same problem is in gpivot (.subtotalouterdiv .scrolldiv / .headerdiv), and there it is solved the same way (however since there is no focus cell there in history there should be a solution with styles, conditional css)

    // in theory margin should be set for inner (child element)
    // but 1) margin-right doesn't work for table if it's width 100%
    // 2) it's hard to tell what to do with scroller, since we want right border when there is scroll, and don't wan't it when there is no such scroll
    // however we can remove margin when there is a vertical scroller (so there's no difference whether to set border for child or parent)

    public static void updateVerticalScroll(boolean hasVerticalScroller, Element tableElement) {
        boolean setMargin = !hasVerticalScroller;
        if(setMargin)
            tableElement.addClassName("no-vertical-scroll");
        else
            tableElement.removeClassName("no-vertical-scroll");
    }

    private TableRowElement getRowElementNoFlush(int row) {
        NodeList<TableRowElement> rows = getTableBodyElement().getRows();
        if (!(row >= 0 && row < rows.getLength()))
            return null;
        return rows.getItem(row);
    }

    protected TableCellElement getSelectedElement(int column) {
        return getElement(getSelectedRow(), column);
    }

    protected TableCellElement getElement(Cell cell) {
        return getElement(cell.getRowIndex(), cell.getColumnIndex());
    }

    protected TableCellElement getElement(int rowIndex, int colIndex) { // element used for rendering
        TableCellElement result = null;
        TableRowElement tr = getChildElement(rowIndex); // Do not use getRowElement() because that will flush the presenter.
        if (tr != null && colIndex >= 0) {
            int cellCount = tr.getCells().getLength();
            if (cellCount > 0) {
                int column = min(colIndex, cellCount - 1);
                result = tr.getCells().getItem(column);
            }
        }
        return result;
    }

    public Element getElement() {
        return tableWidget.getElement();
    }

    @Override
    public Widget getWidget() {
        return tableWidget;
    }
    
    protected final TableElement getTableElement() {
        return tableWidget.tableElement;
    }

    protected final TableSectionElement getTableBodyElement() {
        return tableWidget.getSection();
    }

    protected final TableSectionElement getTableFootElement() {
        return tableWidget.footerElement;
    }

    public final TableSectionElement getTableHeadElement() {
        return tableWidget.headerElement;
    }

    protected abstract boolean previewEvent(Element target, Event event);

    public void onFocus() {
        if(isFocused)
            return;
        DataGrid.sinkPasteEvent(getTableDataFocusElement());
        isFocused = true;
        focusedChanged();
    }

    public void onBlur(Event event) {
        if(!isFocused || isFakeBlur(event, getElement()))
            return;
        //if !isFocused should be replaced to assert; isFocused must be true, but sometimes is not (related to LoadingManager)
        //assert isFocused;
        isFocused = false;
        focusedChanged();
    }

    public Element getTableDataFocusElement() {
        if(!noScrollers)
            return tableContainer.getElement();

        return getTableElement();
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

        if((selectedRowChanged || selectedColumnChanged || focusedChanged)) // this is the check that all columns are already updated
            updateSelectedDOM(dataColumnsChanged, !columnsChanged && !(dataChanged && dataColumnsChanged == null));

        if (columnsChanged || selectedRowChanged || selectedColumnChanged || focusedChanged)
            updateFocusedCellDOM(); // updating focus cell border

        // moved to GridContainerPanel
//        if(focusedChanged) // updating focus grid border
//            getElement().getStyle().setBorderColor(isFocused ? "var(--focus-color)" : "var(--component-border-color)");
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
        onResizeChanged = false;

        renderedSelectedKey = getSelectedKey();
        renderedSelectedExpandingIndex = getSelectedExpandingIndex();
        renderedSelectedRow = getSelectedRow();
        renderedSelectedCol = getSelectedColumn();

        isResolvingState = false;
    }

    private int getLastVisibleRow(Integer scrollTop, Integer scrollBottom, int start) {
        for (int i = start; i >= 0; i--) {
            TableRowElement rowElement = getChildElement(i);
            int rowTop = rowElement.getOffsetTop();
            if(rowTop <= scrollBottom) {
                if (scrollTop == null || rowTop >= scrollTop) {
                    return i;
                } else {
                    break;
                }
            }
        }
        return -1;
    }

    private int getFirstVisibleRow(Integer scrollTop, Integer scrollBottom, int start) {
        for (int i = start; i < getRowCount(); i++) {
            TableRowElement rowElement = getChildElement(i);
            int rowBottom = rowElement.getOffsetTop() + rowElement.getClientHeight();
            if (rowBottom >= scrollTop) {
                if (scrollBottom == null || rowBottom <= scrollBottom) {
                    return i;
                } else {
                    break;
                }
            }
        }
        return -1;
    }

    public void checkSelectedRowVisible() {
        int selectedRow = getSelectedRow();
        if (selectedRow >= 0) {
            int scrollHeight = tableContainer.getClientHeight();
            int scrollTop = tableContainer.getVerticalScrollPosition();

            TableRowElement rowElement = getChildElement(selectedRow);
            int rowTop = rowElement.getOffsetTop();
            int rowBottom = rowTop + rowElement.getClientHeight();

            int headerHeight = 0;
            if (getTableHeadElement() != null) {
                headerHeight = getTableHeadElement().getClientHeight();
            }
            int footerHeight = 0;
            if (getTableFootElement() != null) {
                footerHeight = getTableFootElement().getClientHeight();
            }
            int visibleTop = scrollTop + headerHeight;
            int visibleBottom = scrollTop + scrollHeight - footerHeight;

            int newRow = -1;
            if (rowBottom > visibleBottom + 1) { // 1 for border
                newRow = getLastVisibleRow(rowTop <= visibleBottom ? visibleTop : null, visibleBottom, selectedRow - 1);
            }
            if (rowTop < visibleTop) {
                newRow = getFirstVisibleRow(visibleTop, rowBottom >= visibleTop ? visibleBottom : null, selectedRow + 1);
            }
            if (newRow != -1) {
                selectionHandler.changeRow(newRow, FocusUtils.Reason.SCROLLNAVIGATE);
            }
        }
    }

    private void beforeUpdateDOMScroll(SetPendingScrollState pendingState) {
        assert !noScrollers;
        beforeUpdateDOMScrollVertical(pendingState);
    }

    private void beforeUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        if (areRowsChanged() && renderedSelectedRow >= 0 && renderedSelectedRow < getRowCount()) // rows changed and there was some selection
            pendingState.renderedSelectedScrollTop = getChildElement(renderedSelectedRow).getOffsetTop() - tableContainer.getVerticalScrollPosition();
    }

    //force browser-flush
    private void preAfterUpdateDOMScroll(SetPendingScrollState pendingState) {
        assert !noScrollers;
        preAfterUpdateDOMScrollHorizontal(pendingState);
        preAfterUpdateDOMScrollVertical(pendingState);
    }

    private void preAfterUpdateDOMScrollHorizontal(SetPendingScrollState pendingState) {

        NodeList<TableRowElement> rows = tableWidget.getDataRows();

        if(!noScrollers) {
            boolean hasVerticalScroll = GwtClientUtils.hasVerticalScroll(tableContainer.getElement()); // probably getFullWidth should be used
            if (this.hasVerticalScroll == null || !this.hasVerticalScroll.equals(hasVerticalScroll))
                pendingState.hasVertical = hasVerticalScroll;
        }

        int currentScrollLeft = tableContainer.getHorizontalScrollPosition();

        int viewportWidth = getViewportWidth();

        //scroll column to visible if needed
        int scrollLeft = currentScrollLeft;
        int colToShow;
        if (selectedColumnChanged && (colToShow = getSelectedColumn()) >=0 && getRowCount() > 0) {
            NodeList<TableCellElement> cells = rows.getItem(0).getCells();
            TableCellElement td = cells.getItem(colToShow);

            int columnLeft = td.getOffsetLeft() - getPrevStickyCellsOffsetWidth(cells, colToShow);
            int columnRight = td.getOffsetLeft() + td.getOffsetWidth();
            if (columnRight >= scrollLeft + viewportWidth) // not completely visible from right
                scrollLeft = columnRight - viewportWidth;
            if (columnLeft < scrollLeft) // not completely visible from left
                scrollLeft = columnLeft;
        }

        if(currentScrollLeft != scrollLeft)
            pendingState.left = scrollLeft;

        //calculate left neighbour right border for focused cell
        if (columnsChanged || selectedRowChanged || selectedColumnChanged || focusedChanged) {
            pendingState.leftNeighbourRightBorder = calcLeftNeighbourRightBorder(isFocused);
        }

        //calculate left for sticky properties
        if (columnsChanged || headersChanged || dataChanged || widthsChanged || onResizeChanged) {
            GSize left = GSize.ZERO;
            TableRowElement tr = rows.getItem(0);
            if(tr != null) {
                pendingState.stickyLefts = new ArrayList<>();
                List<Integer> stickyColumns = getStickyColumns();
                for (int i = 0; i < stickyColumns.size(); i++) {
                    Element cell = tr.getCells().getItem(stickyColumns.get(i));
                    GSize cellLeft = GwtClientUtils.getOffsetWidth(cell);
                    //protect from too much sticky columns
                    GSize nextLeft = left.add(cellLeft);
                    // assert that nextLeft is Fixed PX, so the resize size is not null
                    pendingState.stickyLefts.add(nextLeft.getResizeSize() <= viewportWidth * 0.67 ? left : null);
                    left = nextLeft;
                }
            }
        }

//        updateScrollHorizontal(pendingState);
    }

    private void preAfterUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        int rowCount = getRowCount();

        int tableHeight = 0;
        if (rowCount > 0) {
            TableRowElement lastRowElement = getChildElement(rowCount - 1);
            tableHeight = lastRowElement.getOffsetTop() + lastRowElement.getClientHeight();
        }

        int viewportHeight = tableContainer.getClientHeight();
        int currentScrollTop = tableContainer.getVerticalScrollPosition();

        int scrollTop = currentScrollTop;
        
        int headerHeight = 0;
        if (getTableHeadElement() != null) {
            headerHeight = getTableHeadElement().getClientHeight();
        }
        int footerHeight = 0;
        if (getTableFootElement() != null) {
            footerHeight = getTableFootElement().getClientHeight();
        }

        // we're trying to keep viewport the same after rerendering
        int rerenderedSelectedRow;
        if(pendingState.renderedSelectedScrollTop != null && (rerenderedSelectedRow = getRowByKey(renderedSelectedKey, renderedSelectedExpandingIndex)) >= 0) {
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
            if (rowBottom >= scrollTop + viewportHeight - footerHeight) // not completely visible from bottom
                scrollTop = rowBottom - viewportHeight + footerHeight;
            if (rowTop <= scrollTop + headerHeight) // not completely visible from top
                scrollTop = rowTop - headerHeight - 1; // 1 for border
        }

        if(scrollTop != currentScrollTop)
            pendingState.top = scrollTop;
    }

    protected int getViewportWidth() {
        if(!noScrollers)
            return tableContainer.getWidth();

        return GwtClientUtils.getWidth(getTableElement());
    }
    public int getViewportClientHeight() {
        if(!noScrollers)
            return tableContainer.getClientHeight();

        return getTableElement().getClientHeight();
    }

    Boolean hasVerticalScroll;
    private void afterUpdateDOMScroll(SetPendingScrollState pendingState) {
        afterUpdateDOMScrollHorizontal(pendingState);
        afterUpdateDOMScrollVertical(pendingState);
    }

    private void afterUpdateDOMScrollVertical(SetPendingScrollState pendingState) {
        if (pendingState.top != null) {
            tableContainer.setVerticalScrollPosition(pendingState.top);
        }
    }

    private void afterUpdateDOMScrollHorizontal(SetPendingScrollState pendingState) {
        if(pendingState.hasVertical != null) {
            hasVerticalScroll = pendingState.hasVertical;
            updateVerticalScroll(hasVerticalScroll, tableContainer.getElement());
        }

        if (pendingState.left != null) {
            tableContainer.setHorizontalScrollPosition(pendingState.left);
        }

        //set left neighbour right border for focused cell
        if(pendingState.leftNeighbourRightBorder != null) {
            setLeftNeighbourRightBorder(pendingState.leftNeighbourRightBorder);
        }

        //set left sticky
        if(pendingState.stickyLefts != null) {
            updateStickyLeftDOM(pendingState.stickyLefts);
        }
    }

    private void updateSelectedCells(int rowIndex, ArrayList<Column> dataColumnsChanged, boolean updateRowImpl, boolean selectedRow) {
        TableRowElement rowElement = getChildElement(rowIndex);
        if(updateRowImpl) {
            // last parameter is an optimization (not to update already updated cells)
            tableBuilder.updateRowImpl(rowIndex, getRowValue(rowIndex), null, rowElement, (tColumn, cell) -> (dataColumnsChanged == null || !dataColumnsChanged.contains(tColumn)));
        }

        setTableActive(rowElement, selectedRow);
    }

    private void updateDataDOM(boolean columnsChanged, ArrayList<Column> dataColumnsChanged) {
        int[] columnsToRedraw = null;
        if(!columnsChanged && dataColumnsChanged != null) { // if only columns has changed
            int size = dataColumnsChanged.size();
            columnsToRedraw = new int[size];
            for (int i = 0 ; i < size; i++)
                columnsToRedraw[i] = getColumnIndex(dataColumnsChanged.get(i));
        }

        tableBuilder.update(tableWidget.getSection(), getRows(), columnsChanged, columnsToRedraw);
    }

    private void updateStickyLeftDOM(List<GSize> stickyLefts) {
        List<Integer> stickyColumns = getStickyColumns();
        if (!noHeaders)
            headerBuilder.updateStickyLeft(stickyColumns, stickyLefts);
        if (!noFooters)
            footerBuilder.updateStickyLeft(stickyColumns, stickyLefts);

        tableBuilder.updateRowStickyLeft(tableWidget.getSection(), stickyColumns, stickyLefts);
    }

    private List<Integer> getStickyColumns() {
        List<Integer> stickyColumns = new ArrayList<>();
        for(int i = 0; i < columns.size(); i++) {
            if(columns.get(i).isSticky()) {
                stickyColumns.add(i);
            }
        }
        return stickyColumns;
    }

    private void updateSelectedDOM(ArrayList<Column> dataColumnsChanged, boolean updateRowImpl) {
        NodeList<TableRowElement> rows = tableWidget.getDataRows();
        int rowCount = rows.getLength();

        int newLocalSelectedRow = getSelectedRow();

        // CLEAR PREVIOUS STATE
        if (renderedSelectedRow >= 0 && renderedSelectedRow < rowCount &&
                renderedSelectedRow != newLocalSelectedRow)
            updateSelectedCells(renderedSelectedRow, dataColumnsChanged, updateRowImpl, false);

        // SET NEW STATE
        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < rowCount)
            updateSelectedCells(newLocalSelectedRow, dataColumnsChanged, updateRowImpl, true);
    }

    private void updateFocusedCellDOM() {
        NodeList<TableRowElement> rows = tableWidget.getDataRows();
        TableRowElement headerRow = noHeaders ? null : headerBuilder.getHeaderRow(); // we need headerRows for upper border

        int newLocalSelectedRow = getSelectedRow();
        int newLocalSelectedCol = getSelectedColumn();

        int columnCount = getColumnCount();
        // CLEAR PREVIOUS STATE
        // if old row index is out of bounds only by 1, than we still need to clean top cell, which is in bounds (for vertical direction there is no such problem since we set outer border, and not inner, for horz it's not possible because of header)
        if (renderedSelectedRow >= 0 && renderedSelectedRow <= rows.getLength() && renderedSelectedCol >= 0 && renderedSelectedCol < columnCount &&
                (renderedSelectedRow != newLocalSelectedRow || renderedSelectedCol != newLocalSelectedCol)) {
            setFocusedCellStyles(renderedSelectedRow, renderedSelectedCol, rows, headerRow, false);
            if(renderedSelectedRow < rows.getLength() && renderedLeftStickyCol >= 0 && renderedLeftStickyCol < columnCount) {
                setLeftNeighbourRightBorder(new LeftNeighbourRightBorder(renderedSelectedRow, renderedLeftStickyCol, false));
                renderedLeftStickyCol = -1;
            }
        }

        // SET NEW STATE
        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < rows.getLength() && newLocalSelectedCol >= 0 && newLocalSelectedCol < columnCount) {
            setFocusedCellStyles(newLocalSelectedRow, newLocalSelectedCol, rows, headerRow, isFocused);
        }
    }

    private LeftNeighbourRightBorder calcLeftNeighbourRightBorder(boolean set) {
        //border for previous sticky cell
        //focused is sticky: draw border if prev cell is invisible
        //focused is not sticky and prev cell is sticky: draw border if focused is visible
        //focused is not sticky and prev cell is not sticky: draw border if prev sticky is at the border of focused
        LeftNeighbourRightBorder leftNeighbourRightBorder = null;
        NodeList<TableRowElement> rows = tableWidget.getDataRows();

        int row = getSelectedRow();
        int column = getSelectedColumn();

        if (row >= 0 && row < rows.getLength() && column >= 0 && column < getColumnCount()) {
            NodeList<TableCellElement> cells = tableWidget.getDataRows().getItem(row).getCells();
            TableCellElement focusedCell = cells.getItem(column);
            TableCellElement prevCell = cells.getItem(column - 1);
            Integer prevStickyCellNum = getPrevStickyCell(cells, column);
            if (prevStickyCellNum != null) {
                TableCellElement prevStickyCell = cells.getItem(prevStickyCellNum);
                if (isStickyCell(focusedCell)) {
                    leftNeighbourRightBorder = new LeftNeighbourRightBorder(row, prevStickyCellNum, set && getAbsoluteRight(prevCell) <= getAbsoluteRight(prevStickyCell));
                } else if (prevCell.equals(prevStickyCell)) {
                    leftNeighbourRightBorder = new LeftNeighbourRightBorder(row, prevStickyCellNum, set && getAbsoluteLeft(focusedCell) + 1 >= getAbsoluteRight(prevStickyCell));
                } else if (!isStickyCell(prevCell)) {
                    leftNeighbourRightBorder = new LeftNeighbourRightBorder(row, prevStickyCellNum, set && (getAbsoluteLeft(focusedCell) == getAbsoluteRight(prevStickyCell)));
                }
            }
        }
        return leftNeighbourRightBorder;
    }

    private void setLeftNeighbourRightBorder(LeftNeighbourRightBorder leftNeighbourRightBorder) {
        if (leftNeighbourRightBorder != null) {
            setLeftNeighbourRightBorder(tableWidget.getDataRows().getItem(leftNeighbourRightBorder.row).getCells().getItem(leftNeighbourRightBorder.column), leftNeighbourRightBorder.value);
            if (leftNeighbourRightBorder.value) {
                renderedLeftStickyCol = leftNeighbourRightBorder.column;
            }
        }
    }

    private Integer getPrevStickyCell(NodeList<TableCellElement> cells, int column) {
        for (int i = column - 1; i >= 0; i--) {
            TableCellElement prevCell = cells.getItem(i);
            if (isStickyCell(prevCell)) {
                return i;
            }
        }
        return null;
    }

    private int getPrevStickyCellsOffsetWidth(NodeList<TableCellElement> cells, int column) {
        int left = 0;
        for (int i = column - 1; i >= 0; i--) {
            TableCellElement prevCell = cells.getItem(i);
            if (isStickyCell(prevCell)) {
                left += prevCell.getOffsetWidth();
            }
        }

        return left;
    }

    //use native getAbsoluteLeft, because GWT methods are casting to int incorrect
    private native double getAbsoluteLeft(Element elem) /*-{
        var left = 0;
        var curr = elem;
        // This intentionally excludes body which has a null offsetParent.
        while (curr.offsetParent) {
            left -= curr.scrollLeft;
            curr = curr.parentNode;
        }
        while (elem) {
            left += elem.offsetLeft;
            elem = elem.offsetParent;
        }
        return left;
    }-*/;

    public final double getAbsoluteRight(Element elem) {
        return getAbsoluteLeft(elem) + getOffsetWidth(elem);
    }

    private native double getOffsetWidth(Element elem) /*-{
        return elem.offsetWidth || 0;
    }-*/;


    private boolean isStickyCell(TableCellElement cell) {
        return cell.hasClassName("dataGridStickyCell");
    }

    private void setFocusedCellStyles(int row, int column, NodeList<TableRowElement> rows, TableRowElement headerRow, boolean focused) {
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
                // LEFT BORDER (RIGHT of left column)
                if(column > 0) {
                    setLeftNeighbourRightBorder(cells.getItem(column - 1), focused);
                }

                // in theory we might want to prevent extra border on the bottom and on the right (on the top there is no problem because of header)
                // but there is a problem (with scroller, in that case we'll have to track hasVerticalScroller, hasHorizontalScroller) + table can be not full of rows (and we also have to track it)
                // so we'll just draw that borders always

                // BOTTOM BORDER
                setFocusedCellBottomBorder(thisCell, focused);

                // RIGHT BORDER
                setFocusedCellRightBorder(thisCell, focused);
            }

            setFocusedCell(thisCell, focused);
        }

        // TOP BORDER (BOTTOM of upper row)
        if(column < columnCount) {
            TableRowElement upperRow = row > 0 ? rows.getItem(row - 1) : headerRow;
            if(upperRow != null)
                setFocusedCellBottomBorder(upperRow.getCells().getItem(column), focused);
        }
    }

    private void setFocusedCell(Element element, boolean focused) {
        if (focused) {
            element.addClassName("focused-cell");
        } else {
            element.removeClassName("focused-cell");
        }
    }

    private void setTableActive(Element element, boolean active) {
        if (active) {
            element.addClassName("table-active");
        } else {
            element.removeClassName("table-active");
        }
    }

    private void setFocusedCellBottomBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.addClassName("focusedCellBottomBorder");
        } else {
            td.removeClassName("focusedCellBottomBorder");
        }
    }

    private void setFocusedCellRightBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.addClassName("focusedCellRightBorder");
        } else {
            td.removeClassName("focusedCellRightBorder");
        }
    }

    private void setLeftNeighbourRightBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.addClassName("leftNeighbourRightBorder");
        } else {
            td.removeClassName("leftNeighbourRightBorder");
        }
    }

    public void updateHeadersDOM(boolean columnsChanged) {
        if (!noHeaders)
            headerBuilder.update(columnsChanged);
        if (!noFooters)
            footerBuilder.update(columnsChanged);
    }

    public Element getHeaderElement(int element) {
        assert !noHeaders;
        return headerBuilder.getHeaderRow().getCells().getItem(element);
    }

    public Element getWidthElement(int element) {
        int shift = 0;
        for(int i=0;i<element;i++) {
            shift++;
            if(isColumnFlex(i))
                shift++;
        }
        return tableWidget.colRowElement.getCells().getItem(shift);
    }

    // mechanism is slightly different - removing redundant columns, resetting others, however there is no that big difference from other updates so will leave it this way
    protected void updateWidthsDOM(boolean columnsChanged) {
        if(columnsChanged) {
            tableBuilder.rebuildColumnRow(tableWidget.colRowElement);
//            tableWidget.rebuildColumnGroup();
        }

        int colRowInd = 0;
        for (int i = 0, columnCount = getColumnCount(); i < columnCount; i++) {
            TableCellElement colElement = tableWidget.colRowElement.getCells().getItem(colRowInd++);
//            TableColElement colElement = tableWidget.colGroupElement.getChild(i).cast();

            FlexPanel.setGridWidth(colElement, getColumnWidth(i).getString());

            if(isColumnFlex(i)) {
                double columnFlexPerc = getColumnFlexPerc(i);
                colElement = tableWidget.colRowElement.getCells().getItem(colRowInd++);
                FlexPanel.setGridWidth(colElement, columnFlexPerc + "%");
            }
        }
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

        private LeftNeighbourRightBorder leftNeighbourRightBorder;
        private List<GSize> stickyLefts;
    }

    private static class LeftNeighbourRightBorder {
        int row;
        int column;
        boolean value;

        public LeftNeighbourRightBorder(int row, int column, boolean value) {
            this.row = row;
            this.column = column;
            this.value = value;
        }
    }

    // all this pending is needed for two reasons :
    // 1) update dom once if there are several changes before event loop
    // 2) first do dom changes, then do getOffset* routing thus avoiding unnecessary layouting flushes
    private static UpdateDOMCommand updateDOMCommandStatic;

    private static class UpdateDOMCommand implements Scheduler.ScheduledCommand {
        private final ArrayList<DataGrid> grids = new ArrayList<>();

        public boolean executed;

        @Override
        public void execute() {
            if(executed)
                return;

            for (DataGrid grid : grids)
                grid.startResolving();

            int size = grids.size();
            boolean[] showing = new boolean[size];
            SetPendingScrollState[] pendingStates = new SetPendingScrollState[size];
            for (int i = 0; i < size; i++) {
                DataGrid grid = grids.get(i);
                if(!grid.noScrollers && GwtClientUtils.isShowing(grid.tableWidget)) { // need this check, since grid can be already hidden (for example when SHOW DOCKED is executed), and in that case get*Width return 0, which leads for example to updateTablePaddings (removing scroll) and thus unnecessary blinking when the grid becomes visible again
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

            executed = true;
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

        public static void flush() {
            if (updateDOMCommandStatic != null)
                updateDOMCommandStatic.execute();
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

    public static void flushUpdateDOM() {
        UpdateDOMCommand.flush();
    }

    protected class TableWidget extends Widget {
        protected final TableElement tableElement;
//        protected final TableColElement colGroupElement;
        protected TableRowElement colRowElement;
        protected TableSectionElement headerElement = null;
        protected final TableSectionElement bodyElement;
        protected TableSectionElement footerElement = null;
        
        public TableWidget() {
            tableElement = Document.get().createTableElement();

            tableElement.addClassName("table");

//            if (!noHeaders) {
            headerElement = tableElement.createTHead();
            headerElement.setClassName("dataGridHeader");
//            }

            colRowElement = headerElement.insertRow(-1);

            bodyElement = GwtClientUtils.createTBody(tableElement);
            bodyElement.setClassName("dataGridBody");

            if (!noFooters) {
                footerElement = tableElement.createTFoot();
                footerElement.setClassName("dataGridFooter");
            }

            setElement(tableElement);
        }

//        public void rebuildColumnGroup() {
//            GwtClientUtils.removeAllChildren(colGroupElement);
//
//            for(int i = 0, columnCount = getColumnCount(); i < columnCount; i++) {
//                colGroupElement.appendChild(Document.get().createColElement());
//            }
//        }
//
        public TableSectionElement getSection() {
            return bodyElement;
        }
        
        public NodeList<TableRowElement> getDataRows() {
            return bodyElement.getRows();
        } 
    }


    public void onResize() {
        onResizeChanged();
        //need to recalculate scrollTop in preAfterUpdateDOMScrollVertical
        selectedRowChanged = true;
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
                int selectedColumn = display.getSelectedColumn();

                if (selectedColumn != col || rowChanged) {
                    FocusUtils.Reason reason = FocusUtils.Reason.MOUSENAVIGATE;
                    if(!isFocusable(col))
                        changeRow(row, reason);
                    else
                        changeCell(row, col, reason);

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
            FocusUtils.Reason reason = FocusUtils.Reason.KEYMOVENAVIGATE;
            switch (keyCode) {
                case KeyCodes.KEY_RIGHT:
                    nextColumn(true, reason);
                    return true;
                case KeyCodes.KEY_LEFT:
                    nextColumn(false, reason);
                    return true;
                case KeyCodes.KEY_DOWN:
                    nextRow(true, reason);
                    return true;
                case KeyCodes.KEY_UP:
                    nextRow(false, reason);
                    return true;
                case KeyCodes.KEY_PAGEDOWN:
                    changeRow(display.getSelectedRow() + display.pageIncrement, reason);
                    return true;
                case KeyCodes.KEY_PAGEUP:
                    changeRow(display.getSelectedRow() - display.pageIncrement, reason);
                    return true;
                case KeyCodes.KEY_HOME:
                    changeRow(0, reason);
                    return true;
                case KeyCodes.KEY_END:
                    changeRow(display.getRowCount() - 1, reason);
                    return true;
            }
            return false;
        }

        protected boolean isFocusable(int column) {
            return display.isFocusable(column);
        }
        protected void changeCell(int row, int column, FocusUtils.Reason reason) {
            display.changeSelectedCell(row, column, reason);
        }
        public void changeColumn(int column, FocusUtils.Reason reason) {
            changeCell(display.getSelectedRow(), column, reason);
        }
        public void changeRow(int row, FocusUtils.Reason reason) {
            changeCell(row, display.getSelectedColumn(), reason);
        }

        public void nextRow(boolean down, FocusUtils.Reason reason) {
            int rowIndex = display.getSelectedRow();
            changeRow(down ? rowIndex + 1 : rowIndex - 1, reason);
        }

        public void nextColumn(boolean forward, FocusUtils.Reason reason) {
            int rowCount = display.getRowCount();
            if(rowCount == 0) // not sure if it's needed
                return;
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

                if(isFocusable(columnIndex))
                    break;
            }

            changeCell(rowIndex, columnIndex, reason);
        }
    }

    private boolean wasUnloaded;

    public void onTableContainerUnload() {
        wasUnloaded = true;
    }

    public void onTableContainerLoad() {
        // when grid is unloaded and then loaded (when moving from one container to another in maximize / minimize tabspanel, not sure if there are other cases)
        // scroll position changes to 0 (without any event, but that doesn't matter), and we want to keep the selected row, so we mark it as changed, and in afterUpdateDOM method it is ensured that selected cell is visible
        if(wasUnloaded)
            selectedRowChanged();
    }
}
