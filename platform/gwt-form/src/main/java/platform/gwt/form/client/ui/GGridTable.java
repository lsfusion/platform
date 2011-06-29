package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellSavedEvent;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.utills.GwtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GGridTable extends ListGrid {
    private final GFormController formController;
    private final GForm form;
    private final GGroupObjectController groupController;

    private HashSet<String> createdFields = new HashSet<String>();
    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;

    public GGridTable(GFormController iformController, GForm iform, GGroupObjectController igroupController) {
        this.formController = iformController;
        this.form = iform;
        this.groupController = igroupController;
        this.groupObject = groupController.groupObject;

        setHeight(150);
        setWidth("100%");
        setSelectionType(SelectionStyle.SINGLE);
        setShowAllRecords(true);
        setModalEditing(true);
        setEmptyMessage("<empty>");
        setShowRollOver(false);
        setShowRecordComponents(true);
        setShowRecordComponentsByCell(true);
        setCanResizeFields(true);
        setCanSort(false);
        setShowHeaderContextMenu(false);
        setShowHeaderMenuButton(false);
        setAutoFitData(Autofit.VERTICAL);
        setAutoFitMaxRecords(10);

        setCanEdit(iformController.isEditingEnabled());
        setEditEvent(ListGridEditEvent.DOUBLECLICK);
        setEditByCell(true);

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
                        formController.changeGroupObject(groupController.groupObject, record.key);
                    }
                }
            }
        });

        addCellSavedHandler(new CellSavedHandler() {
            @Override
            public void onCellSaved(CellSavedEvent event) {
                formController.changePropertyDraw(getProperty(event.getColNum()), event.getNewValue());
            }
        });
    }

    private GPropertyDraw getProperty(int colNum) {
        return properties.get(colNum);
    }

    @Override
    protected Canvas createRecordComponent(ListGridRecord record, Integer colNum) {
        GPropertyDraw property = properties.get(colNum);
        Canvas cellRenderer = property.createGridCellRenderer(formController, groupObject, (GridDataRecord) record);
        if (cellRenderer != null) {
            return cellRenderer;
        }

        return super.createRecordComponent(record, colNum);
    }

    public void removeProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            return;
        }

        dataUpdated = true;
        values.remove(property);
        properties.remove(property);

        hideField(property.sID);
    }

    public void addProperty(final GPropertyDraw property) {
        if (properties.contains(property)) {
            return;
        }

        int ins = GwtUtils.relativePosition(property, form.propertyDraws, properties);
        properties.add(ins, property);

        if (createdFields.contains(property.sID)) {
            showField(property.sID);
        } else {
            addField(ins, property.createGridField(formController));
            createdFields.add(property.sID);
        }

        dataUpdated = true;
    }

    private void addField(int ins, ListGridField newField) {
        ListGridField[] fields = getFields();
        ListGridField[] newFields = new ListGridField[fields.length + 1];

        System.arraycopy(fields, 0, newFields, 0, ins);
        newFields[ins] = newField;
        System.arraycopy(fields, ins, newFields, ins+1, fields.length - ins);

        setFields(newFields);
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        dataUpdated = true;
        this.keys = keys;
    }

    public void setValues(GPropertyDraw property, HashMap<GGroupObjectValue, Object> propValues) {
        if (propValues != null) {
            dataUpdated = true;
            values.put(property, propValues);
        }
    }

    private boolean dataUpdated = false;
    private boolean internalSelecting = false;
    public void update() {
        if (currentKey == null) {
            GridDataRecord selected = (GridDataRecord) getSelectedRecord();
            if (selected != null) {
                currentKey = selected.key;
            }
        }

        if (dataUpdated) {
            setData(GridDataRecord.createRecords(keys, values));
            dataUpdated = false;
        }

        int currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);
        if (currentInd != -1) {
            internalSelecting = true;
            selectSingleRecord(currentInd);
            internalSelecting = false;
        }

        if (isVisible() != !isEmpty()) {
            setVisible(!isEmpty());
        }
    }

    public boolean isEmpty() {
        return getTotalRows() == 0 || getFields().length == 0;
    }
}
