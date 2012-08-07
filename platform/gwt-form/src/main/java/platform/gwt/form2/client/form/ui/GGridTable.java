package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import platform.gwt.form2.client.form.dispatch.GwtEditPropertyActionDispatcher;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view2.GGroupObject;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.GridDataRecord;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;
import platform.gwt.view2.grid.EditManager;
import platform.gwt.view2.grid.editor.GridCellEditor;
import platform.gwt.view2.grid.GridEditableCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GGridTable extends DataGrid implements FieldUpdater<GridDataRecord, Object>, EditManager {

    /**
     * Default style's overrides
     */
    public interface GGridTableResource extends Resources {
        @Source("GGridTable.css")
        DataGrid.Style dataGridStyle();
    }
    public static final GGridTableResource GGRID_RESOURCES = GWT.create(GGridTableResource.class);

    private final GFormController form;
    private final GGroupObjectController groupController;
    private final GwtEditPropertyActionDispatcher editDispatcher;

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    public ArrayList<GridHeader> headers = new ArrayList<GridHeader>();

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

    private GridEditableCell editCell;
    private Cell.Context editContext;
    private Element editCellParent;
    private GType editType;

    public GGridTable(GFormController iform, GGroupObjectController igroupController) {
        super(50, GGRID_RESOURCES);

        this.form = iform;
        this.groupController = igroupController;
        this.groupObject = groupController.groupObject;
        this.editDispatcher = new GwtEditPropertyActionDispatcher(form);

        setEmptyTableWidget(new HTML("The table is empty"));

        addStyleName("gridTable");

        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

        addCellPreviewHandler(new CellPreviewEvent.Handler() {
            @Override
            public void onCellPreview(CellPreviewEvent event) {
                System.out.println(event.getNativeEvent().getType());
                if (event.getNativeEvent().getType().equals("keyup")) {
                    System.out.println("    " + event.getNativeEvent().getKeyCode());
                }
            }
        });

        final SingleSelectionModel<GridDataRecord> selectionModel = new SingleSelectionModel<GridDataRecord>();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GridDataRecord selectedRecord = selectionModel.getSelectedObject();
                if (selectedRecord != null) {
                    System.out.println("changing current object");
                    form.changeGroupObject(groupObject, selectedRecord.key);
                }
            }
        });
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

        Column<GridDataRecord, Object> gridColumn = property.createGridColumn(this, form);
        gridColumn.setFieldUpdater(this);
        GridHeader header = new GridHeader(property.getCaptionOrEmpty());
        headers.add(ins, header);
        insertColumn(ins, gridColumn, header);
        setColumnWidth(gridColumn, "150px");

        dataUpdated = true;
    }

    @Override
    public void update(int index, GridDataRecord object, Object value) {
        //todo: commit values...
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

        updatePropertyReaders();

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

    private void updatePropertyReaders() {
        for (int i = 0; i < keys.size(); i++) {
            Object rowBackground = rowBackgroundValues.get(keys.get(i));
            Object rowForeground = rowForegroundValues.get(keys.get(i));

            for (GPropertyDraw property : properties) {
                Object cellBackground = rowBackground;
                if (cellBackground == null && cellBackgroundValues.get(property) != null) {
                    cellBackground = cellBackgroundValues.get(property).get(keys.get(i));
                }
                if (cellBackground != null) {
                    getRowElement(i).getCells().getItem(properties.indexOf(property)).getStyle().setBackgroundColor((String) cellBackground);
                }

                Object cellForeground = rowForeground;
                if (cellForeground == null && cellForegroundValues.get(property) != null) {
                    cellForeground = cellForegroundValues.get(property).get(keys.get(i));
                }
                if (cellForeground != null) {
                    getRowElement(i).getCells().getItem(properties.indexOf(property)).getStyle().setColor((String) cellForeground);
                }
            }
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

    public Object getValueAt(int row, int column) {
        //todo: optimize maybe
        return values.get(getProperty(column)).get(keys.get(row));
    }

    public void startEditing(GType type, Object oldValue) {
        editType = type;

        GridCellEditor cellEditor = type.createGridCellEditor(this, getProperty(editContext.getColumn()), oldValue);
        if (cellEditor != null) {
            editCell.startEditing(editContext, editCellParent, cellEditor);
        } else {
            cancelEditing();
        }
    }

    @Override
    public boolean isCurrentlyEditing() {
        //todo: возвращать true, если любая таблица редактируется, чтобы избежать двойного редактирования...
        return editType != null;
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, Cell.Context editContext, Element parent) {
        this.editCell = editCell;
        this.editContext = editContext;
        this.editCellParent = parent;
        editDispatcher.executePropertyEditAction(GGridTable.this, getEditCellCurrentValue(), editContext);
    }

    private Object getEditCellCurrentValue() {
        return getValueAt(editContext.getIndex(), editContext.getColumn());
    }

    @Override
    public void commitEditing(Object value) {
        clearEditState(value);
        editDispatcher.commitValue(value);
    }

    @Override
    public void cancelEditing() {
        clearEditState(getEditCellCurrentValue());
        editDispatcher.cancelEdit();
    }

    private void clearEditState(Object newValue) {
        editCell.finishEditing(editContext, editCellParent, newValue);

        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;
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
