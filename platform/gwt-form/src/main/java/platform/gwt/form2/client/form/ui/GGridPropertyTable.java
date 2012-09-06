package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.*;

public abstract class GGridPropertyTable extends GPropertyTable {
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
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

    protected void scrollRowToVerticalPosition(int rowIndex, int rowScrollTop) {
        if (rowScrollTop != -1 && rowIndex >= 0 && rowIndex < getRowCount()) {
            int rowOffsetTop = getRowElement(rowIndex).getOffsetTop();
            getScrollPanel().setVerticalScrollPosition(rowOffsetTop - rowScrollTop);
        }
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
    }

    protected void updatePropertyReaders() {
        if (currentRecords != null) {
            for (int i = 0; i < currentRecords.size(); i++) {
                GGroupObjectValue key = currentRecords.get(i).key;

                Object rowBackground = rowBackgroundValues.get(key);
                Object rowForeground = rowForegroundValues.get(key);

                for (GPropertyDraw property : getColumnProperties()) {
                    Object cellBackground = rowBackground;
                    TableCellElement cellElement = getRowElement(i).getCells().getItem(getColumnIndex(property));
                    if (cellBackground == null && cellBackgroundValues.get(property) != null) {
                        cellBackground = cellBackgroundValues.get(property).get(key);
                    }
                    cellElement.getStyle().setBackgroundColor(cellBackground == null ? null : cellBackground.toString());

                    Object cellForeground = rowForeground;
                    if (cellForeground == null && cellForegroundValues.get(property) != null) {
                        cellForeground = cellForegroundValues.get(property).get(key);
                    }
                    cellElement.getStyle().setColor(cellForeground == null ? null : cellForeground.toString());
                }
            }
        }
    }

    protected void updateHeader() {
        boolean needsHeaderRefresh = false;
        for (GPropertyDraw property : getColumnProperties()) {
            Map<GGroupObjectValue, Object> captions = propertyCaptions.get(property);
            if (captions != null) {
                Object value = captions.values().iterator().next();
                headers.get(getColumnIndex(property)).setCaption(value == null ? "" : value.toString().trim());
                needsHeaderRefresh = true;
            }
        }
        if (needsHeaderRefresh) {
            redrawHeaders();
        }
    }

    @Override
    public void setValueAt(int row, int column, Object value) {
        putValue(row, column, value);

        GridDataRecord rowRecord = currentRecords.get(row);
        rowRecord.setAttribute(getProperty(row, column), value);

        setRowData(row, Arrays.asList(rowRecord));
    }

    public int getColumnIndex(GPropertyDraw property) {
        return getColumnProperties().indexOf(property);
    }

    public abstract List<GPropertyDraw> getColumnProperties();
    public abstract void putValue(int row, int column, Object value);

    protected class GridHeader extends Header<String> {
        private String caption;

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
