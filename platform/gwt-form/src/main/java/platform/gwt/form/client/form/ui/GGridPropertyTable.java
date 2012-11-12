package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import platform.gwt.cellview.client.AbstractCellTable;
import platform.gwt.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class GGridPropertyTable extends GPropertyTable {
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    protected Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    protected final GGridTableSelectionModel selectionModel;

    protected ArrayList<GridDataRecord> currentRecords;

    public ArrayList<GGridPropertyTableHeader> headers = new ArrayList<GGridPropertyTableHeader>();

    protected boolean dataUpdated;
    protected boolean needToScroll;

    protected GridState pendingState;

    public GGridSortableHeaderManager sortableHeaderManager;

    public interface GGridPropertyTableResource extends Resources {
        @Source("GGridPropertyTable.css")
        GGridPropertyTableStyle dataGridStyle();
    }

    public interface GGridPropertyTableStyle extends Style {}

    public static final GGridPropertyTableResource GGRID_RESOURCES = GWT.create(GGridPropertyTableResource.class);

    public GGridPropertyTable(GFormController iform) {
        super(iform, GGRID_RESOURCES);

        setTableBuilder(new GGridPropertyTableCellBuilder(this));

        setEmptyTableWidget(new HTML("The table is empty"));

        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
        setKeyboardSelectionHandler(new GridPropertyTableKeyboardSelectionHandler(this));

        selectionModel = new GGridTableSelectionModel();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());

        setAutoHeaderRefreshDisabled(true);
        setAutoFooterRefreshDisabled(true);
    }

    @Override
    public GGridTableSelectionModel getSelectionModel() {
        return (GGridTableSelectionModel) super.getSelectionModel();
    }

    public void rememberOldState(int currentIndex) {
        if (currentIndex != -1 && needToScroll) {
            pendingState.verticalScrollPosition = getScrollPanel().getVerticalScrollPosition();
            pendingState.scrollPanelHeight = getScrollPanel().getOffsetHeight();
            TableRowElement currentRow = getRowElement(currentIndex);
            pendingState.currentRowTop = currentRow.getOffsetTop();
            pendingState.rowHeight = currentRow.getOffsetHeight();
        }
    }

    protected void scrollToNewKey() {
        if (pendingState.verticalScrollPosition >= pendingState.currentRowTop + pendingState.rowHeight) {
            getScrollPanel().setVerticalScrollPosition(pendingState.currentRowTop);
        } else if (pendingState.verticalScrollPosition + pendingState.scrollPanelHeight <= pendingState.currentRowTop + pendingState.rowHeight) {
            getScrollPanel().setVerticalScrollPosition(pendingState.currentRowTop
                    + (getScrollPanel().getMaximumHorizontalScrollPosition() == 0 ? pendingState.rowHeight : pendingState.rowHeight * 2)
                    - pendingState.scrollPanelHeight);
        } else {
            scrollRowToVerticalPosition();
        }
    }

    protected void scrollRowToVerticalPosition() {
        if (pendingState.oldKeyScrollTop != -1) {
            getScrollPanel().setVerticalScrollPosition(pendingState.currentRowTop - pendingState.oldKeyScrollTop);
        }
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
        //пока используем этот флаг для обновления цветов... в будущем возможно придётся разделить для оптимизации
        dataUpdated = true;
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
        dataUpdated = true;
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
        dataUpdated = true;
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
        dataUpdated = true;
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
    }

    public void headerClicked(Header header, boolean withCtrl) {
        sortableHeaderManager.headerClicked(headers.indexOf(header), withCtrl);
        refreshHeadersAndRedraw();
    }

    public Boolean getSortDirection(Header header) {
        return sortableHeaderManager.getSortDirection(headers.indexOf(header));
    }

    protected class GridState {
        public int verticalScrollPosition;
        public int scrollPanelHeight;
        public int currentRowTop;
        public int rowHeight;

        public GridDataRecord oldRecord = null;
        public int oldKeyScrollTop;
    }

    public static class GridPropertyTableKeyboardSelectionHandler extends PropertyTableKeyboardSelectionHandler {
        public GridPropertyTableKeyboardSelectionHandler(AbstractCellTable<GridDataRecord> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(NativeEvent nativeEvent) {
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
            return super.handleKeyEvent(nativeEvent);
        }
    }
}
