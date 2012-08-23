package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.SelectionChangeEvent;
import platform.gwt.form2.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form2.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.classes.GType;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GGridTable extends DataGrid implements EditManager, GEditPropertyHandler {

    /**
     * Default style's overrides
     */
    public interface GGridTableResource extends Resources {
        @Source("GGridTable.css")
        GGridTableStyle dataGridStyle();
    }

    public interface GGridTableStyle extends Style {}

    public static final GGridTableResource GGRID_RESOURCES = GWT.create(GGridTableResource.class);

    private final GFormController form;
    private final GGroupObjectController groupController;
    private final GEditPropertyDispatcher editDispatcher;

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
        this.editDispatcher = new GEditPropertyDispatcher(form);

        setEmptyTableWidget(new HTML("The table is empty"));

        addStyleName("gridTable");

        selectionModel = new GGridTableSelectionModel();
        setSelectionModel(selectionModel, DefaultSelectionEventManager.<GridDataRecord>createDefaultManager());
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GridDataRecord selectedRecord = selectionModel.getSelectedRecord();
                if (selectedRecord != null && !selectedRecord.key.equals(currentKey)) {
                    storeScrolling(selectedRecord);
                    form.changeGroupObject(groupObject, selectedRecord.key);
                }
            }
        });

        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
//        setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
//        addDomHandler(new KeyDownHandler() {
//            @Override
//            public void onKeyDown(KeyDownEvent event) {
//                if (selectionModel.getSelectedRecord() != null) {
//                    int pos;
//                    GridDataRecord object;
//                    int key = event.getNativeEvent().getKeyCode();
//                    switch (key) {
//                        case KeyCodes.KEY_UP:
//                            pos = currentInd;
//                            if (pos > 0) {
//                                object = getVisibleItems().get(pos - 1);
//                                selectionModel.setSelected(object, true);
//                                event.stopPropagation();
//                                event.preventDefault();
//                            }
//                            break;
//                        case KeyCodes.KEY_DOWN:
//                            pos = currentInd;
//                            if (pos != -1 && pos != getVisibleItems().size() - 1) {
//                                object = getVisibleItems().get(pos + 1);
//                                selectionModel.setSelected(object, true);
//                                event.stopPropagation();
//                                event.preventDefault();
//                            }
//                            break;
//                    }
//                }
//            }
//        }, KeyDownEvent.getType());
//        sinkEvents(Event.ONKEYDOWN);
    }

    public ScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (ScrollPanel) header.getContentWidget();
    }

    private void storeScrolling(GridDataRecord selectedRecord) {
        setCurrentKey(selectedRecord.key);
        currentInd = keys.indexOf(currentKey);
    }

    //
    private void restoreScrolling() {
        if (currentInd == -1) {
            return;
        }
//        scrollRecordIntoView(currentInd);
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

        GridHeader header = new GridHeader(property.getCaptionOrEmpty());
        headers.add(ins, header);

        Column<GridDataRecord, Object> gridColumn = property.createGridColumn(this, form);
        insertColumn(ins, gridColumn, header);
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

    ArrayList<GridDataRecord> records;

    public void update() {
        if (currentKey == null) {
            GridDataRecord selected = selectionModel.getSelectedRecord();
            if (selected != null) {
                currentKey = selected.key;
            }
        }

        int rowScrollTop = 0;
        if (currentInd != -1) {
            TableRowElement rowElement = getRowElement(currentInd);
            rowScrollTop = rowElement.getAbsoluteTop() - getScrollPanel().getAbsoluteTop();
        }

        currentInd = currentKey == null ? -1 : keys.indexOf(currentKey);

        if (dataUpdated) {
            records = GridDataRecord.createRecords(keys, values);
            setRowData(records);
            dataUpdated = false;
        }

        restoreScrolling();

        if (currentInd != -1) {
            scrollRowToVerticalPosition(currentInd, rowScrollTop);
            selectionModel.setSelected(records.get(currentInd), true);
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

    public Object getValueAt(int row, int column) {
        //todo: optimize maybe
        return values.get(getProperty(column)).get(keys.get(row));
    }

    public GPropertyDraw getProperty(int colNum) {
        return properties.get(colNum);
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext.getColumn()), oldValue);
        if (cellEditor != null) {
            editCell.startEditing(editContext, editCellParent, cellEditor);
        } else {
            cancelEditing();
        }
    }

    @Override
    public void updateEditValue(Object value) {
        //todo:

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
        GGroupObjectValue columnKey = ((GridDataRecord) editContext.getKey()).key;
        editDispatcher.executePropertyEditAction(this, getProperty(editContext.getColumn()), getEditCellCurrentValue(), columnKey);
    }

    private Object getEditCellCurrentValue() {
        return getValueAt(editContext.getIndex(), editContext.getColumn());
    }

    @Override
    public void commitEditing(Object value) {
        clearEditState();
        editDispatcher.commitValue(value);
    }

    @Override
    public void cancelEditing() {
        clearEditState();
        editDispatcher.cancelEdit();
    }

    private void clearEditState() {
        editCell.finishEditing(editContext, editCellParent, getEditCellCurrentValue());

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
