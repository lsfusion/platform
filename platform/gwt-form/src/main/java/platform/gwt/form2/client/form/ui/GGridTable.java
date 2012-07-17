package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.HTML;
import platform.gwt.form2.client.form.dispatch.GwtEditPropertyActionDispatcher;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view2.GGroupObject;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.GridDataRecord;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GGridTable extends DataGrid {
    private final GFormController form;
    private final GGroupObjectController groupController;
    private final GwtEditPropertyActionDispatcher editDispatcher;

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();

    public ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();
    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    private Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    private GGroupObjectValue currentKey;
    private GGroupObject groupObject;
    private int currentInd = -1;

    private GType editType;
    private GPropertyDraw editProperty;

    private HTML tableContentLabel;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        this.form = iform;
        this.groupController = igroupController;
        this.groupObject = groupController.groupObject;
        this.editDispatcher = new GwtEditPropertyActionDispatcher(form);

        setEmptyTableWidget(tableContentLabel = new HTML("The table is empty"));

//        setHeight("300");
//        setWidth("400");

        addStyleName("gridTable");

//        setSelectionType(SelectionStyle.SINGLE);
//        setModalEditing(true);
//        setShowRollOver(false);
//        setCanResizeFields(true);
//        setCanSort(false);
//        setShowHeaderContextMenu(false);
//        setShowHeaderMenuButton(false);
//        setEmptyCellValue("--");
//
//        setCanEdit(this.form.isEditingEnabled());
//        setEditEvent(ListGridEditEvent.NONE);
//        setEditByCell(true);
//        setAutoSaveEdits(false);
//        setNeverValidate(true);
//
//        setShowAllRecords(true);
//        setShowRecordComponents(true);
//        setShowRecordComponentsByCell(true);
////        setRecordComponentPoolingMode(RecordComponentPoolingMode.RECYCLE);
////        setPoolComponentsPerColumn(true);
//
//        addCellDoubleClickHandler(new CellDoubleClickHandler() {
//            @Override
//            public void onCellDoubleClick(CellDoubleClickEvent event) {
//                Log.debug("Cell dbl click, init editing...");
//                //пока начинаем редактирование по дабл-клику
//                editDispatcher.executePropertyEditAction(GGridTable.this, event);
//            }
//        });
//
//        addCellSavedHandler(new CellSavedHandler() {
//            @Override
//            public void onCellSaved(CellSavedEvent event) {
////                commitingValue = event.getNewValue();
////                editDispatcher.commitValue(commitingValue);
//            }
//        });
//
//        addEditorExitHandler(new EditorExitHandler() {
//            @Override
//            public void onEditorExit(EditorExitEvent event) {
//                Log.debug("grid's editor exit: " + event.getNewValue());
//                if (editType != null) {
//                    //пока коммитим только по Enter
//                    if (event.getEditCompletionEvent() == EditCompletionEvent.ENTER_KEYPRESS) {
//                        try {
//                            Object newValue = editType.parseString((String) event.getNewValue());
//                            editDispatcher.commitValue(newValue);
//                        } catch (Exception e) {
//                            event.cancel();
//                            return;
//                        }
//                    } else {
//                        editDispatcher.cancelEdit();
//                    }
//                    editType = null;
//                    event.cancel();
//                    GGridTable.this.cancelEditing();
//                }
//            }
//        });
//
//        addSelectionChangedHandler(new SelectionChangedHandler() {
//            @Override
//            public void onSelectionChanged(SelectionEvent event) {
//                if (internalSelecting) {
//                    return;
//                }
//                if (event.getState()) {
//                    GridDataRecord record = (GridDataRecord) event.getSelectedRecord();
//                    if (record != null && !currentKey.equals(record.key)) {
//                        storeScrolling(record);
//
//                        GGridTable.this.form.changeGroupObject(groupController.groupObject, record.key);
//                    }
//                }
//            }
//        });
    }

//    @Override
//    protected boolean canEditCell(int rowNum, int colNum) {
//        return true;
//    }
//
    public Object getValueAt(int row, int column) {
        //todo: optimize somehow
        return values.get(getProperty(column)).get(keys.get(row));
    }

    public void editCellAt(GType type, Object oldValue, int row, int column) {
        editType = type;
        editProperty = getProperty(column);

//        getField(column).setEditorType(
//                editType.createGridEditorItem(form, editProperty)
//        );
//
//        startEditing(row, column, false);
    }

//    private void storeScrolling(GridDataRecord selectedRecord) {
//        currentKey = selectedRecord.key;
//    }
//
    private void restoreScrolling() {
        if (currentInd == -1) {
            return;
        }
//        scrollRecordIntoView(currentInd);
    }

    public GPropertyDraw getProperty(int colNum) {
        return properties.get(colNum);
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

        int ins = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
        properties.add(ins, property);

        Column<GridDataRecord,?> gridColumn = property.createGridColumn(form);
        insertColumn(ins, gridColumn, property.caption);
        setColumnWidth(gridColumn, "150px");

        dataUpdated = true;
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
        String tableContent = "";
        for (GPropertyDraw property : properties) {
            String propertyValues = "";
            for (int j = 0; j < keys.size(); j++) {
                propertyValues += values.get(property).get(keys.get(j)) + (j == keys.size() - 1 ? "" : ",");
            }
            tableContent += property.caption + ": " + propertyValues + "<br/>";
        }
        tableContentLabel.setHTML(tableContent);

        if (currentKey == null) {
//            GridDataRecord selected = (GridDataRecord) getSelectedRecord();
//            if (selected != null) {
//                currentKey = selected.key;
//            }
        }

        if (dataUpdated) {
            setRowData(GridDataRecord.createRecords(keys, values));
            dataUpdated = false;
        }

        currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);

        restoreScrolling();

        if (currentInd != -1) {
            internalSelecting = true;
//            selectSingleRecord(currentInd);
            internalSelecting = false;
        }

        boolean needsFieldsRefresh = false;
        for (GPropertyDraw property : properties) {
            Map<GGroupObjectValue, Object> captions = propertyCaptions.get(property);
            if (captions != null) {
                Object value = captions.values().iterator().next();
//                getField(property.sID).setTitle(value == null ? null : value.toString().trim());
                needsFieldsRefresh = true;
            }
        }
        if (needsFieldsRefresh) {
//            refreshFields();
        }
    }

//    @Override
//    protected String getCellCSSText(ListGridRecord record, int rowNum, int colNum) {
//        String cssText = "";
//
//        GPropertyDraw property = null;
//        for (GPropertyDraw prop : properties) {
//            if (getFieldName(colNum).equals(prop.sID)) {
//                property = prop;
//            }
//        }
//        if (property != null) {
//            Object background = null;
//            if (rowBackgroundValues.get(((GridDataRecord) record).key) != null) {
//                background = rowBackgroundValues.get(((GridDataRecord) record).key);
//            } else if (cellBackgroundValues.get(property) != null) {
//                background = cellBackgroundValues.get(property).get(((GridDataRecord) record).key);
//            }
//            if (background != null) {
//                cssText += "background-color:" + background + ";";
//            }
//
//            Object foreground = null;
//            if (rowForegroundValues.get(((GridDataRecord) record).key) != null) {
//                foreground = rowForegroundValues.get(((GridDataRecord) record).key);
//            } else if (cellForegroundValues.get(property) != null) {
//                foreground = cellForegroundValues.get(property).get(((GridDataRecord) record).key);
//            }
//            if (foreground != null) {
//                cssText += "color:" + foreground + ";";
//            }
//        }
//
//        return cssText.isEmpty() ? super.getCellCSSText(record, rowNum, colNum) : cssText;
//    }

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

//    private native void scrollRecordIntoView(int currentInd) /*-{
//        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
//        self.scrollRecordIntoView(currentInd);
//    }-*/;
}
