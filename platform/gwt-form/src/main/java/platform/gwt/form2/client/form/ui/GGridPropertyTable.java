package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.*;

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

    public interface GGridTableResource extends Resources {
        @Source("GGridTable.css")
        GGridTableStyle dataGridStyle();
    }

    public interface GGridTableStyle extends Style {}

    public static final GGridTableResource GGRID_RESOURCES = GWT.create(GGridTableResource.class);

    public GGridPropertyTable(GFormController iform) {
        super(iform, GGRID_RESOURCES);

        setTableBuilder(new GGridCellTableBuilder(this));

        addStyleName("gridTable");

        setEmptyTableWidget(new HTML("The table is empty"));

        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

        selectionModel = new GGridTableSelectionModel();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());
    }

    public ScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (ScrollPanel) header.getContentWidget();
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

    protected static class GridHeader extends Header<String> {
        private String caption;

        public GridHeader() {
            this(null);
        }

        public GridHeader(String caption) {
            super(new TextCell());
            this.caption = caption;
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
