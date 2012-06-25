package platform.gwt.form.client.ui;

import com.smartgwt.client.types.*;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellSavedEvent;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GGridTable extends ListGrid {
    private final GFormController formController;
    private final GForm form;
    private final GGroupObjectController groupController;

    private HashSet<String> createdFields = new HashSet<String>();
    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;
    private int currentInd = -1;

    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    private Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    public GGridTable(GFormController iformController, GForm iform, GGroupObjectController igroupController) {
        this.formController = iformController;
        this.form = iform;
        this.groupController = igroupController;
        this.groupObject = groupController.groupObject;

        setSelectionType(SelectionStyle.SINGLE);
        setShowAllRecords(true);
        setModalEditing(true);
        setShowRollOver(false);
        setCanResizeFields(true);
        setCanSort(false);
        setShowHeaderContextMenu(false);
        setShowHeaderMenuButton(false);
        setEmptyCellValue("--");

        setCanEdit(iformController.isEditingEnabled());
        setEditEvent(ListGridEditEvent.DOUBLECLICK);
        setEditByCell(true);

        setShowRecordComponents(true);
        setShowRecordComponentsByCell(true);
//        setRecordComponentPoolingMode(RecordComponentPoolingMode.RECYCLE);
//        setPoolComponentsPerColumn(true);

        addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent event) {
                if (internalSelecting) {
                    return;
                }
                if (event.getState()) {
                    GridDataRecord record = (GridDataRecord) event.getSelectedRecord();
                    if (record != null && !currentKey.equals(record.key)) {
                        storeScrolling(record);

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

    private void storeScrolling(GridDataRecord selectedRecord) {
        currentKey = selectedRecord.key;
    }

    private void restoreScrolling() {
        if (currentInd == -1) {
            return;
        }
        scrollRecordIntoView(currentInd);
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

    @Override
    public Canvas updateRecordComponent(ListGridRecord record, Integer colNum, Canvas component, boolean recordChanged) {
        GPropertyDraw property = properties.get(colNum);
        Canvas cellRenderer =  property.updateGridCellRenderer(component, (GridDataRecord) record);
        if (cellRenderer != null) {
            return cellRenderer;
        }

        return super.updateRecordComponent(record, colNum, component, recordChanged);
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

        int ins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
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
        System.arraycopy(fields, ins, newFields, ins + 1, fields.length - ins);

        setFields(newFields);
    }

    public GGroupObjectValue getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;
    }

    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        dataUpdated = true;
        this.keys = keys;
    }

    public void setValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues) {
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

        currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);

        restoreScrolling();

        if (currentInd != -1) {
            internalSelecting = true;
            selectSingleRecord(currentInd);
            internalSelecting = false;
        }

        boolean needsFieldsRefresh = false;
        for (GPropertyDraw property : properties) {
            Map<GGroupObjectValue, Object> captions = propertyCaptions.get(property);
            if (captions != null) {
                Object value = captions.values().iterator().next();
                getField(property.sID).setTitle(value == null ? null : value.toString().trim());
                needsFieldsRefresh = true;
            }
        }
        if (needsFieldsRefresh)
            refreshFields();
    }

    @Override
    protected String getCellCSSText(ListGridRecord record, int rowNum, int colNum) {
        String cssText = "";

        GPropertyDraw property = null;
        for (GPropertyDraw prop : properties) {
            if (getFieldName(colNum).equals(prop.sID)){
                property = prop;
            }
        }
        if (property != null) {
            Object background = null;
            if (rowBackgroundValues.get(((GridDataRecord) record).key) != null) {
                background = rowBackgroundValues.get(((GridDataRecord) record).key);
            } else if (cellBackgroundValues.get(property) != null) {
                background = cellBackgroundValues.get(property).get(((GridDataRecord) record).key);
            }
            if (background != null) {
                cssText += "background-color:" + background + ";";
            }

            Object foreground = null;
            if (rowForegroundValues.get(((GridDataRecord) record).key) != null) {
                foreground = rowForegroundValues.get(((GridDataRecord) record).key);
            } else if (cellForegroundValues.get(property) != null) {
                foreground = cellForegroundValues.get(property).get(((GridDataRecord) record).key);
            }
            if (foreground != null) {
                cssText += "color:" + foreground + ";";
            }
        }

        return cssText.isEmpty() ? super.getCellCSSText(record, rowNum, colNum) : cssText;
    }

    public boolean isEmpty() {
        return getTotalRows() == 0 || getFields().length == 0;
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

    private native void scrollRecordIntoView(int currentInd) /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        self.scrollRecordIntoView(currentInd);
    }-*/;
}
