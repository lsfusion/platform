package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import platform.gwt.cellview.client.AbstractCellTable;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.GridEditableCell;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

import static platform.gwt.form.shared.view.GEditBindingMap.Key;

public abstract class GPropertyTable extends DataGrid implements EditManager, GEditPropertyHandler {

    protected final GFormController form;
    protected final GEditBindingMap editBindingMap;
    protected final GEditPropertyDispatcher editDispatcher;

    protected GridEditableCell editCell;
    protected NativeEvent editEvent;
    protected Cell.Context editContext;
    protected Element editCellParent;
    protected GType editType;

    public GPropertyTable(GFormController iform, Resources resources) {
        super(50, resources);

        addStyleName(getResources().style().widget());

        setKeyboardSelectionHandler(new PropertyTableKeyboardSelectionHandler(this));

        this.form = iform;

        this.editDispatcher = new GEditPropertyDispatcher(form);
        this.editBindingMap = new GEditBindingMap();
        this.editBindingMap.setMouseAction(GEditBindingMap.CHANGE);
        this.editBindingMap.setKeyAction(new Key(KeyCodes.KEY_BACKSPACE), GEditBindingMap.EDIT_OBJECT);
    }

    public CustomScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (CustomScrollPanel) header.getContentWidget();
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            NativeEvent event = editEvent;
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

    public abstract boolean isEditable(Cell.Context context);

    public abstract GPropertyDraw getProperty(Cell.Context editContext);

    public abstract GGroupObjectValue getColumnKey(Cell.Context editContext);

    public abstract void setValueAt(Cell.Context context, Object value);

    public abstract Object getValueAt(Cell.Context context);

    protected String getEditAction(GPropertyDraw property, NativeEvent event) {
        String actionSID = null;
        if (property.editBindingMap != null) {
            actionSID = property.editBindingMap.getAction(event);
        }

        if (actionSID == null) {
            actionSID = editBindingMap.getAction(event);
        }

        return actionSID;
    }

    public void startEdit(GridEditableCell editCell, NativeEvent event, Cell.Context editContext, Element parent) {
        //todo: в будущем похоже нужно вообще избавиться от GridEditableCell... вместо этого лучше напрямую переопределить AbstractCellTable.fireEventToCell()

        if (form.isEditing()) return;

        if (!isEditable(editContext)) return;

        GPropertyDraw property = getProperty(editContext);

        String actionSID = getEditAction(property, event);

        if (actionSID != null) {
            event.stopPropagation();
            event.preventDefault();

            this.editCell = editCell;
            this.editEvent = event;
            this.editContext = editContext;
            this.editCellParent = parent;

            GGroupObjectValue columnKey = getColumnKey(editContext);
            Object oldValue = getValueAt(editContext);

            editDispatcher.executePropertyEditAction(this, property, columnKey, actionSID, oldValue);
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
