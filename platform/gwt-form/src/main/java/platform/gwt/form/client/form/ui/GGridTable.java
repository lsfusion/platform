package platform.gwt.form.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.view.client.SelectionChangeEvent;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.form.client.dispatch.DeferredRunner;
import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.GOrder;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GObjectType;
import platform.gwt.form.shared.view.grid.GridEditableCell;

import java.util.*;

public class GGridTable extends GGridPropertyTable {

    private ArrayList<GPropertyDraw> columnProperties = new ArrayList<GPropertyDraw>();
    private ArrayList<GGroupObjectValue> columnKeysList = new ArrayList<GGroupObjectValue>();

    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    private ArrayList<GGroupObjectValue> rowKeys = new ArrayList<GGroupObjectValue>();

    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, List<GGroupObjectValue>> columnKeys = new HashMap<GPropertyDraw, List<GGroupObjectValue>>();

    private boolean columnsUpdated = false;

    private ArrayList<GridDataRecord> currentRecords;
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        super(iform);

        this.groupObject = igroupController.groupObject;

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final GridDataRecord selectedRecord = selectionModel.getSelectedRecord();
                if (selectedRecord != null && !selectedRecord.key.equals(currentKey)) {
                    setCurrentKey(selectedRecord.key);

                    DeferredRunner.get().scheduleDelayedGroupObjectChange(groupObject, new DeferredRunner.AbstractCommand() {
                        @Override
                        public void execute() {
                            form.changeGroupObject(groupObject, selectedRecord.key);
                        }
                    });
                }
            }
        });

        sortableHeaderManager = new GGridSortableHeaderManager<Map<GPropertyDraw, GGroupObjectValue>>(this, false) {
            @Override
            protected void orderChanged(Map<GPropertyDraw, GGroupObjectValue> columnKey, GOrder modiType) {
                form.changePropertyOrder(columnKey.keySet().iterator().next(), columnKey.values().iterator().next(), modiType);
            }

            @Override
            protected Map<GPropertyDraw, GGroupObjectValue> getColumnKey(int column) {
                HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<GPropertyDraw, GGroupObjectValue>();
                key.put(columnProperties.get(column), columnKeysList.get(column));
                return key;
            }
        };
    }

    public void removeProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            return;
        }

        values.remove(property);
        properties.remove(property);

        columnsUpdated = true;
    }

    public void addProperty(final GPropertyDraw property) {
        if (properties.contains(property)) {
            return;
        }

        int newColumnIndex = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
        properties.add(newColumnIndex, property);

        columnsUpdated = true;
    }

    public void rememberScrollPosition() {
        GridDataRecord selectedRecord = selectionModel.getSelectedRecord();
        if (needToScroll && selectedRecord != null && selectedRecord.rowIndex < getRowCount()) {
            pendingState = new GridState();
            pendingState.oldRecord = selectedRecord;
            pendingState.oldKeyScrollTop = getRowElement(selectedRecord.rowIndex).getAbsoluteTop() - getScrollPanel().getAbsoluteTop();
        }
    }

    public void update() {
        updatedColumnsIfNeeded();

        if (dataUpdated) {
            currentRecords = GridDataRecord.createRecords(columnProperties, rowKeys, columnKeysList, values, rowBackgroundValues, rowForegroundValues, cellBackgroundValues, cellForegroundValues);
            setRowData(currentRecords);
            dataUpdated = false;
        }
    }

    public void preparePendingState() {
        if (pendingState == null) {
            pendingState = new GridState();
        }
        int currentInd = currentKey == null ? -1 : rowKeys.indexOf(currentKey);
        rememberOldState(currentInd);
    }

    public void applyPendingState() {
        int currentInd = currentKey == null ? -1 : rowKeys.indexOf(currentKey);
        if (pendingState != null && currentInd != -1 && needToScroll) {
            if (pendingState.oldRecord != null && currentKey.equals(pendingState.oldRecord.key)) {
                scrollRowToVerticalPosition();
            } else {
                scrollToNewKey();
            }
            needToScroll = false;
        }

        if (currentInd != -1) {
            setKeyboardSelectedRow(currentInd, false);
        }
        pendingState = null;
    }

    private void updatedColumnsIfNeeded() {
        if (columnsUpdated) {
            columnProperties.clear();
            columnKeysList.clear();

            List<String> columnCaptions = new ArrayList<String>();

            for (GPropertyDraw property : properties) {
                List<GGroupObjectValue> propertyColumnKeys = columnKeys.get(property);
                if (propertyColumnKeys == null) {
                    propertyColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                }

                Map<GGroupObjectValue, Object> propCaptions = propertyCaptions.get(property);

                for (GGroupObjectValue columnKey : propertyColumnKeys) {
                    Object propCaption = null;
                    if (propCaptions == null || (propCaption = propCaptions.get(columnKey)) != null) {
                        columnProperties.add(property);
                        columnKeysList.add(columnKey);
                        if (propCaptions != null) {
                            columnCaptions.add(property.getDynamicCaption(propCaption));
                        } else {
                            columnCaptions.add(property.getCaptionOrEmpty());
                        }
                    }
                }
            }

            int columnDiff = getColumnCount() - columnProperties.size();

            for (int i = 0; i < Math.abs(columnDiff); ++i) {
                if (columnDiff < 0) {
                    GridHeader header = new GridHeader();
                    headers.add(getColumnCount(), header);

                    GridColumn gridColumn = new GridColumn(getColumnCount());
                    addColumn(gridColumn, header);
                } else {
                    headers.remove(getColumnCount() - 1);
                    removeColumn(getColumnCount() - 1);
                }
            }

            if (columnDiff > 0) {
                refreshColumnWidths();
            }

            for (int i = 0; i < columnProperties.size(); ++i) {
                GridHeader header = headers.get(i);
                header.setCaption(columnCaptions.get(i));
                GPropertyDraw property = getProperty(i);
                if (property != null) {
                    setColumnWidth(getColumn(i), property.getMinimumWidth());
                }
            }

            redraw();

            columnsUpdated = false;
            dataUpdated = true;
        }
    }

    public boolean isEmpty() {
        return values.isEmpty() || properties.isEmpty();
    }

    public GGroupObjectValue getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        Log.debug("Setting current object to: " + currentKey);
        this.currentKey = currentKey;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.rowKeys = keys;
        dataUpdated = true;
        needToScroll = true;
    }

    public void updatePropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);
            dataUpdated = true;
        }
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        if (columnKeys != null) {
            this.columnKeys.put(property, columnKeys);
            columnsUpdated = true;
        }
    }

    @Override
    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updatePropertyCaptions(propertyDraw, values);
        columnsUpdated = true;
    }

    public GPropertyDraw getProperty(int column) {
        return columnProperties.get(column);
    }

    public GPropertyDraw getProperty(Cell.Context context) {
        return getProperty(context.getColumn());
    }

    private int getMinPropertyIndex(GPropertyDraw property) {
        for (int i = 0; i < columnProperties.size(); ++i) {
            if (property == columnProperties.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public void modifyGroupObject(GGroupObjectValue rowKey, boolean add) {
        if (add) {
            rowKeys.add(rowKey);
            setCurrentKey(rowKey);
        } else {
            if (currentKey.equals(rowKey) && rowKeys.size() > 0) {
                if (rowKeys.size() == 1) {
                    setCurrentKey(null);
                } else {
                    int index = rowKeys.indexOf(rowKey);
                    index = index == rowKeys.size() - 1 ? index - 1 : index + 1;
                    setCurrentKey(rowKeys.get(index));
                }
            }
            rowKeys.remove(rowKey);
        }
        dataUpdated = true;

        update();
    }

    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        int ind = getMinPropertyIndex(property);
        HashMap<GPropertyDraw, GGroupObjectValue> key = new HashMap<GPropertyDraw, GGroupObjectValue>();
        key.put(property, ind == -1 ? GGroupObjectValue.EMPTY : columnKeys.get(property).get(ind));
        sortableHeaderManager.changeOrder(key, modiType);
    }

    @Override
    public GGroupObjectValue getColumnKey(Cell.Context context) {
        return columnKeysList.get(context.getColumn());
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        GPropertyDraw property = getProperty(context);
        return property != null && !property.isReadOnly() && !(property.baseType instanceof GObjectType);
    }

    public Object getValueAt(Cell.Context context) {
        GridDataRecord record = (GridDataRecord) context.getKey();
        return record.getValue(context.getColumn());
    }

    public void setValueAt(Cell.Context context, Object value) {
        GridDataRecord rowRecord = (GridDataRecord) context.getKey();
        rowRecord.setValue(context.getColumn(), value);

        setRowData(context.getIndex(), Arrays.asList(rowRecord));
    }

    private class GridColumn extends Column<GridDataRecord, Object> {
        private final int column;

        public GridColumn(int column) {
            super(new GridEditableCell(GGridTable.this));
            this.column = column;
        }

        @Override
        public Object getValue(GridDataRecord record) {
            return record.getValue(column);
        }
    }
}
