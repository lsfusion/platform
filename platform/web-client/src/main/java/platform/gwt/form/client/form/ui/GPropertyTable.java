package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.view.client.CellPreviewEvent;
import platform.gwt.cellview.client.AbstractCellTable;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.*;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class GPropertyTable extends DataGrid implements EditManager, GEditPropertyHandler {

    protected final GFormController form;
    protected final GEditBindingMap editBindingMap;
    protected final GEditPropertyDispatcher editDispatcher;

    private GGridPropertyTableMenuHandler contextMenuHandler = new GGridPropertyTableMenuHandler(this);

    protected GridEditableCell editCell;
    protected EditEvent editEvent;
    protected Cell.Context editContext;
    protected Element editCellParent;
    protected GType editType;

    public GPropertyTable(GFormController iform, Resources resources) {
        super(50, resources);

        addStyleName(getResources().style().widget());

        setKeyboardSelectionHandler(new PropertyTableKeyboardSelectionHandler(this));

        this.form = iform;

        this.editDispatcher = new GEditPropertyDispatcher(form, this);
        this.editBindingMap = new GEditBindingMap();
        this.editBindingMap.setMouseAction(GEditBindingMap.CHANGE);
        this.editBindingMap.setKeyAction(new GKeyStroke(KeyCodes.KEY_BACKSPACE), GEditBindingMap.EDIT_OBJECT);
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            EditEvent event = editEvent;
            editEvent = null;
            form.setCurrentEditingTable(this);
            editCell.startEditing(event, editContext, editCellParent, cellEditor, oldValue);
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

    public abstract boolean isEditable(Cell.Context context);

    public abstract GPropertyDraw getSelectedProperty();

    public abstract GPropertyDraw getProperty(Cell.Context editContext);

    public abstract GGroupObjectValue getColumnKey(Cell.Context editContext);

    public abstract void setValueAt(Cell.Context context, Object value);

    public abstract Object getValueAt(Cell.Context context);

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

    public void onEventFromCell(GridEditableCell editCell, NativeEvent event, Cell.Context editContext, Element parent) {
        //todo: в будущем похоже нужно вообще избавиться от GridEditableCell... вместо этого лучше напрямую переопределить AbstractCellTable.fireEventToCell()

        if (BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            stopPropagation(event);
            contextMenuHandler.show(event.getClientX(), event.getClientY(), editCell, editContext, parent);
        } else {
            onEditEvent(editCell, new NativeEditEvent(event), editContext, parent);
        }
    }

    public void startEdit(int row, int column, String actionSID) {
        GridEditableCell editCell = (GridEditableCell) getColumn(column).getCell();
        EditEvent editEvent = new InternalEditEvent(actionSID);
        Cell.Context editContext = new Cell.Context(row, column, getVisibleItem(row));
        Element editCellParent = getCellParent(row, column);
        
        onEditEvent(editCell, editEvent, editContext, editCellParent);
    }

    /**
     * @see platform.gwt.cellview.client.AbstractCellTable#getKeyboardSelectedElement()
     */
    private Element getCellParent(int row, int column) {
        TableCellElement td = getRowElement(row).getCells().getItem(column);

//        // The TD itself is a cell parent, which means its internal structure
//        // (including the tabIndex that we set) could be modified by its Cell. We
//        // return the TD to be safe.
//        if (tableBuilder.isColumn(td)) {
//            return td;
//        }

        // The default table builder adds a focusable div to the table cell because
        // TDs aren't focusable in all browsers. If the user defines a custom table
        // builder with a different structure, we must assume the keyboard selected
        // element is the TD itself.
        Element firstChild = td.getFirstChildElement();
        if (firstChild != null && td.getChildCount() == 1 && "div".equalsIgnoreCase(firstChild.getTagName())) {
            return firstChild;
        }

        return td;
    }

    public void onEditEvent(GridEditableCell editCell, EditEvent editEvent, Cell.Context editContext, Element editCellParent) {
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

    @Override
    public void commitEditing(Object value) {
        editDispatcher.commitValue(value);

        clearEditState();
        setFocus(true);
        form.setCurrentEditingTable(null);
    }

    @Override
    public void cancelEditing() {
        editDispatcher.cancelEdit();

        clearEditState();
        setFocus(true);
        form.setCurrentEditingTable(null);
    }

    private void clearEditState() {
        editCell.finishEditing(editContext, editCellParent, getValueAt(editContext));

        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;
    }

    public static class PropertyTableKeyboardSelectionHandler extends CellTableKeyboardSelectionHandler<GridDataRecord> {
        public PropertyTableKeyboardSelectionHandler(AbstractCellTable<GridDataRecord> table) {
            super(table);
        }

        @Override
        public void onCellPreview(CellPreviewEvent<GridDataRecord> event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            String eventType = nativeEvent.getType();
            if (BrowserEvents.KEYDOWN.equals(eventType) && !event.isCellEditing()) {
                //не обрабатываем пробел, чтобы он обработался как начало редактирования
                if (nativeEvent.getKeyCode() == 32) {
                    return;
                }

                if (handleKeyEvent(nativeEvent)) {
                    handledEvent(event);
                    return;
                }
            }
            super.onCellPreview(event);
        }

        public boolean handleKeyEvent(NativeEvent nativeEvent) {
            return false;
        }
    }
}
