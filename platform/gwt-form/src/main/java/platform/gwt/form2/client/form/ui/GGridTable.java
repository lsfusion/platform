package platform.gwt.form2.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.SelectionChangeEvent;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.utils.GwtSharedUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GGridTable extends GPropertyTable {

    /**
     * Default style's overrides
     */
    public interface GGridTableResource extends Resources {
        @Source("GGridTable.css")
        GGridTableStyle dataGridStyle();
    }

    public interface GGridTableStyle extends Style {}

    public static final GGridTableResource GGRID_RESOURCES = GWT.create(GGridTableResource.class);

    private final GGroupObjectController groupController;

    private final GGridTableSelectionModel selectionModel;

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    public ArrayList<GridHeader> headers = new ArrayList<GridHeader>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    private Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    private ArrayList<GridDataRecord> currentRecords;
    private boolean dataUpdated = false;

    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        super(iform, GGRID_RESOURCES);

        this.groupController = igroupController;
        this.groupObject = groupController.groupObject;

        setEmptyTableWidget(new HTML("The table is empty"));

        addStyleName("gridTable");

        selectionModel = new GGridTableSelectionModel();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GridDataRecord selectedRecord = selectionModel.getSelectedRecord();
                if (selectedRecord != null && !selectedRecord.key.equals(currentKey)) {
                    setCurrentKey(selectedRecord.key);
                    form.changeGroupObject(groupObject, selectedRecord.key);
                }
            }
        });

        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    }

    public ScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (ScrollPanel) header.getContentWidget();
    }

    public void removeProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            return;
        }

        int removeIndex = properties.indexOf(property);

        dataUpdated = true;
        values.remove(property);
        properties.remove(removeIndex);

        removeColumn(removeIndex);
    }

    public void addProperty(final GPropertyDraw property) {
        if (properties.contains(property)) {
            return;
        }

        int newColumnIndex = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
        properties.add(newColumnIndex, property);

        GridHeader header = new GridHeader(property.getCaptionOrEmpty());
        headers.add(newColumnIndex, header);

        Column<GridDataRecord, Object> gridColumn = createGridColumn(property);
        insertColumn(newColumnIndex, gridColumn, header);
        setColumnWidth(gridColumn, "150px");

        dataUpdated = true;
    }

    private Column<GridDataRecord, Object> createGridColumn(final GPropertyDraw property) {
        return new Column<GridDataRecord, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(GridDataRecord record) {
                return record.getAttribute(property);
            }
        };
    }

    public GGroupObjectValue getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        Log.debug("Setting current object to: " + currentKey);
        this.currentKey = currentKey;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        dataUpdated = true;
        this.keys = keys;
    }

    public void setPropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            dataUpdated = true;
            GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);
        }
    }

    public void update() {
        GridDataRecord selectedRecord = selectionModel.getSelectedRecord();

        int oldKeyScrollTop = 0;
        GGroupObjectValue oldKey = null;
        if (selectedRecord != null) {
            int oldKeyInd = currentRecords.indexOf(selectedRecord);

            if (oldKeyInd != -1) {
                oldKey = selectedRecord.key;
                TableRowElement rowElement = getRowElement(oldKeyInd);
                oldKeyScrollTop = rowElement.getAbsoluteTop() - getScrollPanel().getAbsoluteTop();
            }
        }

        if (dataUpdated) {
            currentRecords = GridDataRecord.createRecords(keys, values);
            setRowData(currentRecords);
            dataUpdated = false;
        }

        int currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);
        if (currentInd != -1) {
            //сразу ресолвим, чтобы избежать бага в DataGrid с неперерисовкой строк... (т.о. убираем рекурсивный resolvePendingState())
            selectionModel.setSelectedAndResolve(currentRecords.get(currentInd), true);
            setKeyboardSelectedRow(currentInd, false);
            if (currentKey.equals(oldKey)) {
                scrollRowToVerticalPosition(currentInd, oldKeyScrollTop);
            } else {
                getRowElement(currentInd).scrollIntoView();
            }
        }

        updatePropertyReaders();

        updateHeader();
    }

    private void scrollRowToVerticalPosition(int rowIndex, int rowScrollTop) {
        if (rowScrollTop != -1 && rowIndex >= 0 && rowIndex < getRowCount()) {
            int rowOffsetTop = getRowElement(rowIndex).getOffsetTop();
            getScrollPanel().setVerticalScrollPosition(rowOffsetTop - rowScrollTop);
        }
    }

    private void updatePropertyReaders() {
        for (int i = 0; i < keys.size(); i++) {
            GGroupObjectValue key = keys.get(i);

            Object rowBackground = rowBackgroundValues.get(key);
            Object rowForeground = rowForegroundValues.get(key);

            for (GPropertyDraw property : properties) {
                Object cellBackground = rowBackground;
                if (cellBackground == null && cellBackgroundValues.get(property) != null) {
                    cellBackground = cellBackgroundValues.get(property).get(key);
                }
                if (cellBackground != null) {
                    getRowElement(i).getCells().getItem(properties.indexOf(property)).getStyle().setBackgroundColor(cellBackground.toString());
                }

                Object cellForeground = rowForeground;
                if (cellForeground == null && cellForegroundValues.get(property) != null) {
                    cellForeground = cellForegroundValues.get(property).get(key);
                }
                if (cellForeground != null) {
                    getRowElement(i).getCells().getItem(properties.indexOf(property)).getStyle().setColor(cellForeground.toString());
                }
            }
        }
    }

    private void updateHeader() {
        boolean needsHeaderRefresh = false;
        for (GPropertyDraw property : properties) {
            Map<GGroupObjectValue, Object> captions = propertyCaptions.get(property);
            if (captions != null) {
                Object value = captions.values().iterator().next();
                headers.get(properties.indexOf(property)).setCaption(value == null ? "" : value.toString().trim());
                needsHeaderRefresh = true;
            }
        }
        if (needsHeaderRefresh) {
            redrawHeaders();
        }
    }

    public boolean isEmpty() {
        return values.isEmpty() || properties.isEmpty();
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

    public GPropertyDraw getProperty(int row, int column) {
        return properties.get(column);
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return keys.get(row);
    }

    public Object getValueAt(int row, int column) {
        return currentRecords.get(row).getAttribute(getProperty(row, column));
    }

    public void setValueAt(int row, int column, Object value) {
        GPropertyDraw columnProperty = getProperty(row, column);
        values.get(columnProperty).put(keys.get(row), value);

        GridDataRecord rowRecord = currentRecords.get(row);
        rowRecord.setAttribute(columnProperty, value);

        setRowData(row, Arrays.asList(rowRecord));
    }

    public void modifyGroupObject(GGroupObjectValue rowKey, boolean add) {
        if (add) {
            keys.add(rowKey);
            setCurrentKey(rowKey);
        } else {
            if (currentKey.equals(rowKey) && keys.size() > 0) {
                if (keys.size() == 1) {
                    setCurrentKey(null);
                } else {
                    int index = keys.indexOf(rowKey);
                    index = index == keys.size() - 1 ? index - 1 : index + 1;
                    setCurrentKey(keys.get(index));
                }
            }
            keys.remove(rowKey);
        }
        dataUpdated = true;

        update();
    }

    private class GridHeader extends Header<String> {
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
