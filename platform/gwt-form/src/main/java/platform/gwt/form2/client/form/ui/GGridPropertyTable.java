package platform.gwt.form2.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

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

    public ArrayList<GridHeader> headers = new ArrayList<GridHeader>();

    protected boolean dataUpdated;

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

        selectionModel = new GGridTableSelectionModel();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());
    }

    @Override
    public GGridTableSelectionModel getSelectionModel() {
        return (GGridTableSelectionModel) super.getSelectionModel();
    }

    protected void scrollRowToVerticalPosition(int rowIndex, int rowScrollTop) {
        if (rowScrollTop != -1 && rowIndex >= 0 && rowIndex < getRowCount()) {
            int rowOffsetTop = getRowElement(rowIndex).getOffsetTop();
            getScrollPanel().setVerticalScrollPosition(rowOffsetTop - rowScrollTop);
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
    }

    public Boolean getSortDirection(Header header) {
        return sortableHeaderManager.getSortDirection(headers.indexOf(header));
    }

    protected class GridHeader extends Header<String> {
        private String caption;

        public GridHeader() {
            this(null);
        }

        public GridHeader(String caption) {
            super(new GGridHeaderCell());
            this.caption = caption;
            ((GGridHeaderCell) getCell()).setHeader(this);
            ((GGridHeaderCell) getCell()).setTable(GGridPropertyTable.this);
        }

        public void setCaption(String caption) {
            this.caption = caption;
        }

        @Override
        public String getValue() {
            return caption;
        }
    }
}
