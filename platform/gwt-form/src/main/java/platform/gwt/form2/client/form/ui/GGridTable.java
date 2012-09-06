package platform.gwt.form2.client.form.ui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.SelectionChangeEvent;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.utils.GwtSharedUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GGridTable extends GGridPropertyTable {

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        super(iform);

        this.groupObject = igroupController.groupObject;

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

    public boolean isEmpty() {
        return values.isEmpty() || properties.isEmpty();
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

    @Override
    public List<GPropertyDraw> getColumnProperties() {
        return properties;
    }

    @Override
    public void putValue(int row, int column, Object value) {
        values.get(getProperty(row, column)).put(getColumnKey(row, column), value);
    }
}
