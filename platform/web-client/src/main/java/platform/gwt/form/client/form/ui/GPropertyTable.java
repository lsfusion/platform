package platform.gwt.form.client.form.ui;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.HasCell;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.*;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

import static platform.gwt.base.client.GwtClientUtils.removeAllChildren;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class GPropertyTable<T> extends DataGrid<T> implements EditManager, GEditPropertyHandler {

    protected final GFormController form;
    protected final GEditBindingMap editBindingMap;
    protected final GEditPropertyDispatcher editDispatcher;

    private GGridPropertyTableMenuHandler contextMenuHandler = new GGridPropertyTableMenuHandler(this);

    protected GridCellEditor cellEditor = null;
    protected GridEditableCell editCell;
    protected EditEvent editEvent;
    protected Cell.Context editContext;
    protected Element editCellParent;
    protected GType editType;


    public GPropertyTable(GFormController iform, Resources resources) {
        super(resources);

        addStyleName(getResources().style().dataGridWidget());

        this.form = iform;

        this.editDispatcher = new GEditPropertyDispatcher(form, this);
        this.editBindingMap = new GEditBindingMap();
        this.editBindingMap.setMouseAction(GEditBindingMap.CHANGE);
        this.editBindingMap.setKeyAction(new GKeyStroke(KeyCodes.KEY_BACKSPACE), GEditBindingMap.EDIT_OBJECT);
    }

    public abstract boolean isEditable(Cell.Context context);

    public abstract GPropertyDraw getSelectedProperty();

    public abstract GPropertyDraw getProperty(Cell.Context editContext);

    public abstract GGroupObjectValue getColumnKey(Cell.Context editContext);

    public abstract void setValueAt(Cell.Context context, Object value);

    public abstract Object getValueAt(Cell.Context context);

    @Override
    protected <C> void fireEventToCellImpl(Event event, String eventType, Element cellParent, T rowValue, Cell.Context context, HasCell<T, C> column) {
        Cell<C> cell = column.getCell();
        if (cell instanceof GridEditableCell) {
            if (cellEditor != null) {
                cellEditor.onBrowserEvent(context, cellParent, rowValue, event);
            } else {
                if (BrowserEvents.CONTEXTMENU.equals(event.getType())) {
                    stopPropagation(event);
                    contextMenuHandler.show(event.getClientX(), event.getClientY(), context);
                } else {
                    onEditEvent((GridEditableCell) cell, new NativeEditEvent(event), context, cellParent);
                }
            }
        } else {
            super.fireEventToCellImpl(event, eventType, cellParent, rowValue, context, column);
        }
    }

    private void onEditEvent(GridEditableCell editCell, EditEvent editEvent, Cell.Context editContext, Element editCellParent) {
        if (form.isEditing()) return;

        if (!isEditable(editContext)) return;

        GPropertyDraw property = getProperty(editContext);

        String actionSID = getEditAction(property, editEvent);

        if (actionSID != null) {
            editEvent.stopPropagation();

            this.editCell = editCell;
            this.editEvent = editEvent;
            this.editContext = editContext;
            this.editCellParent = editCellParent;

            GGroupObjectValue columnKey = getColumnKey(editContext);
            Object oldValue = getValueAt(editContext);

            //убираем фокус, чтобы не ловить последующие нажатия
            setFocus(false);
            editDispatcher.executePropertyEditAction(property, columnKey, actionSID, oldValue);
        }
    }

    protected String getEditAction(GPropertyDraw property, EditEvent event) {
        String actionSID = null;
        if (property.editBindingMap != null) {
            actionSID = property.editBindingMap.getAction(event);
        }

        if (actionSID == null) {
            actionSID = editBindingMap.getAction(event);
        }

        return actionSID;
    }

    public void editCellAt(int row, int column, String actionSID) {
        GridEditableCell editCell = (GridEditableCell) getColumn(column).getCell();
        EditEvent editEvent = new InternalEditEvent(actionSID);
        Cell.Context editContext = new Cell.Context(row, column, getRowValue(row));
        Element editCellParent = getCellParent(row, column);

        onEditEvent(editCell, editEvent, editContext, editCellParent);
    }

    public void editCurrentCell(String actionSID) {
        editCellAt(getKeyboardSelectedRow(), getKeyboardSelectedColumn(), actionSID);
    }

    private Element getCellParent(int row, int column) {
        TableCellElement td = getRowElement(row).getCells().getItem(column);
        return getCellParentElement(td);
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            EditEvent event = editEvent;
            editEvent = null;
            form.setCurrentEditingTable(this);

            this.cellEditor = cellEditor;
            editCell.setEditing(true);

            //рендерим эдитор
            removeAllChildren(editCellParent);
            cellEditor.renderDom(editContext, editCellParent.<DivElement>cast(), oldValue);
            cellEditor.startEditing(event, editContext, editCellParent, oldValue);
        } else {
            cancelEditing();
        }
    }

    @Override
    public void updateEditValue(Object value) {
        setValueAt(editContext, value);
    }

    @Override
    public void onEditFinished() {
        setFocus(true);
    }

    @Override
    public void commitEditing(Object value) {
        assert cellEditor != null;

        rerenderCell(editContext, editCellParent, getValueAt(editContext));

        editDispatcher.commitValue(value);

        clearEditState();
    }

    @Override
    public void cancelEditing() {
        assert cellEditor != null;

        rerenderCell(editContext, editCellParent, getValueAt(editContext));

        editDispatcher.cancelEdit();

        clearEditState();
    }

    private void rerenderCell(Cell.Context context, Element parent, Object newValue) {
        //важно сначала обнулить эдитор, иначе при его удаление возникает ONBLUR, который влечёт за собой cancelEditing
        this.cellEditor = null;

        removeAllChildren(parent);

        editCell.setEditing(false);
        editCell.renderDom(context, parent.<DivElement>cast(), newValue);
    }

    private void clearEditState() {
        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;

        setFocus(true);
        form.setCurrentEditingTable(null);
    }
}
