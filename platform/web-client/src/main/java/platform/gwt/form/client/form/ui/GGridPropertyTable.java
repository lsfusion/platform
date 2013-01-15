package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HTML;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.Header;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.CellPreviewEvent;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class GGridPropertyTable<T extends GridDataRecord> extends GPropertyTable<T> {
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    protected Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    protected ArrayList<GGridPropertyTableHeader> headers = new ArrayList<GGridPropertyTableHeader>();

    protected boolean needToRestoreScrollPosition = true;
    protected GGroupObjectValue oldKey = null;
    protected int oldRowScrollTop;

    protected Double rowHeight = 0.0;

    public GGridSortableHeaderManager sortableHeaderManager;

    public interface GGridPropertyTableResource extends Resources {
        @Source("GGridPropertyTable.css")
        GGridPropertyTableStyle style();
    }

    public interface GGridPropertyTableStyle extends Style {}

    public static final GGridPropertyTableResource GGRID_RESOURCES = GWT.create(GGridPropertyTableResource.class);

    public GGridPropertyTable(GFormController iform) {
        super(iform, GGRID_RESOURCES);

        setTableBuilder(new GGridPropertyTableBuilder<T>(this) {
            @Override
            public Double getCellPixelHeight() {
                return getRowHeight();
            }
        });

        setEmptyTableWidget(new HTML("The table is empty"));

        setKeyboardSelectionHandler(new GridPropertyTableKeyboardSelectionHandler(this));
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (form.isDialog() && event.getTypeInt() == Event.ONDBLCLICK) {
            stopPropagation(event);
            form.okPressed();
        }
        super.onBrowserEvent2(event);
    }

    public GPropertyDraw getSelectedProperty() {
        int row = getKeyboardSelectedRow();
        int column = getKeyboardSelectedColumn();

        return getProperty(new Cell.Context(row, column, null));
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

    public void headerClicked(GGridPropertyTableHeader header, boolean withCtrl) {
        sortableHeaderManager.headerClicked(getHeaderIndex(header), withCtrl);
        refreshHeaders();
    }

    public int getHeaderIndex(Header header) {
        return headers.indexOf(header);
    }

    public Boolean getSortDirection(Header header) {
        return sortableHeaderManager.getSortDirection(getHeaderIndex(header));
    }

    protected Double getRowHeight() {
        return rowHeight;
    }

    public abstract GGroupObjectValue getCurrentKey();

    void storeScrollPosition() {
        int selectedRow = getKeyboardSelectedRow();
        GridDataRecord selectedRecord = getKeyboardSelectedRowValue();
        if (selectedRecord != null) {
            oldKey = selectedRecord.getKey();
            oldRowScrollTop = getChildElement(selectedRow).getAbsoluteTop() - getTableDataScroller().getAbsoluteTop();
        }
    }

    void restoreScrollPosition() {
        int currentInd = getKeyboardSelectedRow();

        if (needToRestoreScrollPosition && isRowWithinBounds(currentInd)) {
            // note: именно здесь происходит browser-flush, который обрабатывает все изменения,
            // которые произошли в доме после вызова flush() -> resolvePendingState()
            TableRowElement currentRow = getChildElement(currentInd);

            int rowHeight = currentRow.getOffsetHeight();
            int rowTop = currentRow.getOffsetTop();
            int rowBottom = rowTop + rowHeight;

            int hScrollBarHeight = getTableDataScroller().getHorizontalScrollbar().asWidget().getOffsetHeight();
            int scrollHeight = getTableDataScroller().getOffsetHeight() - hScrollBarHeight;
            int verticalScrollPosition = getTableDataScroller().getVerticalScrollPosition();

            GGroupObjectValue currentKey = getCurrentKey();
            int newVerticalScrollPosition;
            if (oldKey != null && oldRowScrollTop != -1 && currentKey != null && currentKey.equals(oldKey)) {
                newVerticalScrollPosition = max(0, rowTop - oldRowScrollTop);
            } else {
                newVerticalScrollPosition = verticalScrollPosition;
            }

            if (rowBottom >= newVerticalScrollPosition + scrollHeight) {
                newVerticalScrollPosition = rowBottom - scrollHeight;
            }
            if (rowTop < newVerticalScrollPosition) {
                newVerticalScrollPosition = rowTop;
            }

            getTableDataScroller().setVerticalScrollPosition(newVerticalScrollPosition);

            oldKey = null;
            oldRowScrollTop = -1;
            needToRestoreScrollPosition = false;
        }
    }

    public static class GridPropertyTableKeyboardSelectionHandler<T extends GridDataRecord> extends DataGridKeyboardSelectionHandler<T> {
        public GridPropertyTableKeyboardSelectionHandler(DataGrid<T> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(CellPreviewEvent<T> event) {
            NativeEvent nativeEvent = event.getNativeEvent();

            assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

            int keyCode = nativeEvent.getKeyCode();
            boolean ctrlPressed = nativeEvent.getCtrlKey();
            if (keyCode == KeyCodes.KEY_HOME && !ctrlPressed) {
                getDisplay().setKeyboardSelectedColumn(0);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && !ctrlPressed) {
                getDisplay().setKeyboardSelectedColumn(getDisplay().getColumnCount() - 1);
                return true;
            }
            return super.handleKeyEvent(event);
        }
    }
}
