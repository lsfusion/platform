package platform.gwt.form.client.ui;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import platform.gwt.form.client.FormFrame;
import platform.gwt.form.client.utills.GwtUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GGridTable extends ListGrid {
    private final FormFrame frame;
    private final GForm form;
    private final GGroupObjectController groupController;

    private HashSet<String> createdFields = new HashSet<String>();
    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>>();
    private GGroupObjectValue currentKey;

    public GGridTable(FormFrame iframe, GForm iform, GGroupObjectController igroupController) {
        this.frame = iframe;
        this.form = iform;
        this.groupController = igroupController;

        setWidth("100%");
        setSelectionType(SelectionStyle.SINGLE);
        setShowAllRecords(true);
        setEmptyMessage("<empty>");
        setShowRollOver(false);
        setShowRecordComponents(true);
        setShowRecordComponentsByCell(true);
        setCanResizeFields(true);
        setEmptyCellValue("--");

        addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent event) {
                if (internalSelecting) {
                    return;
                }
                if (event.getState()) {
                    GridDataRecord record = (GridDataRecord) event.getSelectedRecord();
                    if (record != null && !currentKey.equals(record.key)) {
                        currentKey = record.key;
                        frame.changeGroupObject(groupController.groupObject, record.key);
                    }
                }
            }
        });
    }

    @Override
    protected Canvas createRecordComponent(ListGridRecord record, Integer colNum) {
        GPropertyDraw property = properties.get(colNum);
        Canvas cellRenderer = property.createGridCellRenderer(record.getAttributeAsObject(property.sID));
        if (cellRenderer != null) {
            return cellRenderer;
        }
        return super.createRecordComponent(record, colNum);
    }

    public void addField(int ins, ListGridField newField) {
        ListGridField[] fields = getFields();
        ListGridField[] newFields = new ListGridField[fields.length + 1];

        System.arraycopy(fields, 0, newFields, 0, ins);
        newFields[ins] = newField;
        System.arraycopy(fields, ins, newFields, ins+1, fields.length - ins);

        setFields(newFields);
    }

    public void removeProperty(GPropertyDraw property) {
        values.remove(property);
        properties.remove(property);

        if (createdFields.contains(property.sID)) {
            hideField(property.sID);
        }
    }

    public void addProperty(GPropertyDraw property) {
        if (createdFields.contains(property.sID)) {
            showField(property.sID);
        } else {
            int ins = GwtUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);

            addField(ins, property.createGridField());

            createdFields.add(property.sID);
        }
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.keys = keys;
    }

    public void setValues(GPropertyDraw property, HashMap<GGroupObjectValue, Object> propValues) {
        if (propValues != null) {
            values.put(property, propValues);
        }
    }

    boolean internalSelecting = false;
    public void update() {
        if (currentKey == null) {
            GridDataRecord selected = (GridDataRecord) getSelectedRecord();
            if (selected != null) {
                currentKey = selected.key;
            }
        }

        setData(GridDataRecord.createRecords(keys, values));

        int currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);
        if (currentInd != -1) {
            internalSelecting = true;
            selectSingleRecord(currentInd);
            internalSelecting = false;
        }

        setVisible(!isEmpty());
    }

    public boolean isEmpty() {
        return getTotalRows() == 0 || getFields().length == 0;
    }
}
