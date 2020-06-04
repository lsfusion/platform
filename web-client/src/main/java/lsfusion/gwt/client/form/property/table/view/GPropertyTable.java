package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.base.view.grid.cell.HasCell;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextBasedGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.cell.controller.dispatch.GEditPropertyDispatcher;
import lsfusion.gwt.client.form.property.cell.view.GridEditableCell;

import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.CONTEXTMENU;
import static lsfusion.gwt.client.base.GwtClientUtils.removeAllChildren;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.view.grid.cell.Cell.Context;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.*;

public abstract class GPropertyTable<T> extends DataGrid<T> implements EditManager, GEditPropertyHandler {

    private final GPropertyContextMenuPopup contextMenuPopup = new GPropertyContextMenuPopup();

    protected final GFormController form;
    protected final GGroupObject groupObject;
    protected final GEditBindingMap editBindingMap;
    protected final GEditPropertyDispatcher editDispatcher;

    protected GridCellEditor cellEditor = null;
    protected GridEditableCell editCell;
    protected EditEvent editEvent;
    protected Context editContext;
    protected Element editCellParent;

    public GPropertyTable(GFormController iform, GGroupObject iGroupObject, GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        super(style, noHeaders, noFooters, noScrollers);

        this.form = iform;
        this.groupObject = iGroupObject;

        this.editDispatcher = new GEditPropertyDispatcher(form, this);
        this.editBindingMap = new GEditBindingMap();
        this.editBindingMap.setMouseAction(GEditBindingMap.CHANGE);

        sinkEvents(Event.ONPASTE);

        initializeActionMap();
    }

    private void initializeActionMap() {
        //  Have the enter key work the same as the tab key
        if(groupObject != null) {
            form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER), null), getEnterBinding(false));
            form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, true), null), getEnterBinding(true));
        }
    }

    protected GFormController.Binding getEnterBinding(boolean shiftPressed) {
        GFormController.Binding binding = new GFormController.Binding(groupObject, -100, null) {
            @Override
            public void pressed(EventTarget eventTarget) {
                selectNextCellInColumn(!shiftPressed);
            }

            @Override
            public boolean showing() {
                return true;
            }

            @Override
            public boolean enabled() {
                return super.enabled();
            }
        };
        binding.bindGroup = GBindingMode.ONLY;
        return binding;
    }

    public GPropertyDraw getProperty(Column column) {
        return getProperty(new Cell.Context(getKeyboardSelectedRow(), getColumnIndex(column), getKeyboardSelectedRowValue()));
    }

    public abstract boolean isEditable(Context context);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumn();

    public abstract GPropertyDraw getProperty(Context editContext);

    public abstract GGroupObjectValue getColumnKey(Context editContext);

    public abstract void setValueAt(Context context, Object value);

    public abstract Object getValueAt(Context context);

    public abstract void pasteData(List<List<String>> table);

    @Override
    protected <C> void fireEventToCellImpl(Event event, String eventType, Element cellParent, T rowValue, final Context context, HasCell<T, C> column) {
        Cell<C> cell = column.getCell();
        if (cell instanceof GridEditableCell) {
            if (cellEditor != null) {
                cellEditor.onBrowserEvent(context, cellParent, rowValue, event);
            } else {
                if (CONTEXTMENU.equals(event.getType())) {
                    stopPropagation(event);
                    contextMenuPopup.show(getSelectedProperty(), event.getClientX(), event.getClientY(), new GPropertyContextMenuPopup.ItemSelectionListener() {
                        @Override
                        public void onMenuItemSelected(String actionSID) {
                            onContextMenuEvent(context, actionSID);
                        }
                    });
                } else if (GKeyStroke.isCopyToClipboardEvent(event)) {
                    CopyPasteUtils.putIntoClipboard(getFocusCellElement());
                } else if (GKeyStroke.isPasteFromClipboardEvent(event)) {  // для IE, в котором не удалось словить ONPASTE, но он и так даёт доступ к буферу обмена
                    executePaste(event);
                } else {
                    onEditEvent((GridEditableCell) cell, new NativeEditEvent(event), context, cellParent);
                }
            }
        } else {
            super.fireEventToCellImpl(event, eventType, cellParent, rowValue, context, column);
        }
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        CopyPasteUtils.setEmptySelection(getFocusCellElement());
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (GKeyStroke.shouldPreventDefaultBrowserAction(event) && cellEditor == null) {
            event.preventDefault();
        }
        if (cellEditor == null && event.getTypeInt() == Event.ONPASTE) { // пока работает только для Chrome
            executePaste(event);
        } else {
            super.onBrowserEvent2(event);
        }
    }

    protected void onEditEvent(GridEditableCell editCell, EditEvent editEvent, Context editContext, Element editCellParent) {
        if (form.isEditing()) return;

        GPropertyDraw property = getProperty(editContext);
        GGroupObjectValue columnKey = getColumnKey(editContext);

        this.editContext = editContext;

        String keyPressActionSID = getPropertyKeyPressActionSID(editEvent, property);
        if (keyPressActionSID != null) {
            editDispatcher.executePropertyEventAction(property, columnKey, keyPressActionSID);
        }

        String actionSID = getPropertyEventActionSID(editEvent, property, editBindingMap);
        if (actionSID == null) {
            return;
        }

        if (isEditableAwareEditEvent(actionSID) && !isEditable(editContext)) {
            return;
        }

        // editContext is not dropped when there is no actual input
//        if(this.editContext != null) // we don't need edit event if editing lifecycle has already started (however requestvalue wasn't called, so form.isEditing is false), because in that case started lifecycle will drop context, and this event will have no context
//            return;
            
        editEvent.stopPropagation();

        this.editCell = editCell;
        this.editEvent = editEvent;
        this.editCellParent = editCellParent;

//        GExceptionManager.addStackTrace("SET CONTEXT");

        //убираем фокус, чтобы не ловить последующие нажатия
        setFocus(false);
        // с той же целью. для групповой корректировки мало убрать фокус (воспроизводится на стрелках вправо/влево).
        // эта часть нужна главным образом при вызове кнопкой в тулбаре 
        if (GEditBindingMap.GROUP_CHANGE.equals(actionSID)) {
            cellIsEditing = true;
        }

        editDispatcher.setLatestEditEvent(editEvent);
        editDispatcher.executePropertyEventAction(property, columnKey, actionSID);
    }

    @Override
    protected <C> void postFireEventToCell(Event event, String eventType, Element cellParent, T rowValue, Context context, HasCell<T, C> column) {
        GPropertyDraw property = getProperty(context);
        String actionSID = getPropertyEventActionSID(new NativeEditEvent(event), property, editBindingMap);
        // см. комментарий в onEditEvent(). дублируется, поскольку при F12 после первого хака cellIsEditing успевает сброситься 
        if (actionSID != null && GEditBindingMap.GROUP_CHANGE.equals(actionSID)) {
            cellIsEditing = true;
        } else {
            super.postFireEventToCell(event, eventType, cellParent, rowValue, context, column);
        }
    }

    protected void onContextMenuEvent(Context context, String actionSID) {
        editCellAt(context.getIndex(), context.getColumn(), actionSID);
    }

    public void editCellAt(int row, int column, String actionSID) {
        GridEditableCell editCell = (GridEditableCell) getColumn(column).getCell();
        EditEvent editEvent = new InternalEditEvent(actionSID);
        if (isRowWithinBounds(row)) {
            Context editContext = new Context(row, column, getRowValue(row));
            Element editCellParent = getCellParent(row, column);

            onEditEvent(editCell, editEvent, editContext, editCellParent);
        }
    }

    private Element getCellParent(int row, int column) {
        TableCellElement td = getChildElement(row).getCells().getItem(column);
        
        // желательно предоставить здесь не сам td, а вложенный в него div, как при обычном редактировании
        // или до конца разобраться, почему наличие треугольника в углу мешает редактированию по hotkey 
        for (int i = 0; i < td.getChildCount(); i++) {
            Element elem = td.getChild(i).cast();
            if (!elem.hasClassName("rightBottomCornerTriangle") && getTableBuilder().isColumn(elem)) {
                return elem;
            }
        }
        return getCellParentElement(td);
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
//        if(editContext == null)
//            GExceptionManager.throwStackedException("EDIT CONTEXT IS NULL");

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            EditEvent event = editEvent;
            editEvent = null;
            form.setCurrentEditingTable(this);

            this.cellEditor = cellEditor;
            editCell.setEditing(true);

            if (cellEditor.replaceCellRenderer()) {
                removeAllChildren(editCellParent);
                cellEditor.renderDom(editContext, this, editCellParent.<DivElement>cast(), oldValue);
            }
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
    public Object getEditValue() {
        return getValueAt(editContext);
    }

    @Override
    public void takeFocusAfterEdit() {
        setFocus(true);
    }

    @Override
    public void commitEditing(Object value) {
        assert cellEditor != null;

        rerenderCell(editContext, editCellParent, getValueAt(editContext)); // here we have new values

        editDispatcher.commitValue(value);

        clearEditState();
    }

    @Override
    public void cancelEditing() {
        assert cellEditor != null;

        rerenderCell(editContext, editCellParent, getValueAt(editContext)); // here we have new values

        editDispatcher.cancelEdit();

        clearEditState();
    }

    private void rerenderCell(Context context, Element parent, Object newValue) {
        //важно сначала обнулить эдитор, иначе при его удаление возникает ONBLUR, который влечёт за собой cancelEditing
        this.cellEditor = null;

        removeAllChildren(parent);

        editCell.setEditing(false);
        editCell.renderDom(context, parent.<DivElement>cast(), newValue);
    }

    private void clearEditState() {
        // editContext is not dropped when there is no actual input, so we will not drop if there was actual input (and there will be no editContext is null exception)
//        editCell = null;
//        editContext = null;
//        editCellParent = null;

//        GExceptionManager.addStackTrace("CLEARED CONTEXT");

        setFocus(true);
        form.setCurrentEditingTable(null);
    }

    public GPropertyTableBuilder<T> getTableBuilder() {
        return (GPropertyTableBuilder<T>) super.getTableBuilder();
    }

    protected void setCellHeight(int cellHeight) {
        getTableBuilder().setCellHeight(cellHeight);
        setRowHeight(cellHeight + 1); //1px for border
    }

    private Element getFocusCellElement() {
        TableRowElement selectedRow = getChildElement(getKeyboardSelectedRow());
        if (selectedRow != null) {
            return selectedRow.getCells().getItem(getKeyboardSelectedColumn()).getFirstChildElement();
        } else {
            return null;
        }
    }

    private void executePaste(Event event) {
        String line = CopyPasteUtils.getClipboardData(event);
        if (!line.isEmpty()) {
            stopPropagation(event);
            line = line.replaceAll("\r\n", "\n");    // браузеры заменяют разделители строк на "\r\n"
            pasteData(GwtClientUtils.getClipboardTable(line));
        }
    }

    public void validateAndCommit() {
        if(cellEditor != null && cellEditor instanceof TextBasedGridCellEditor) {
            ((TextBasedGridCellEditor) cellEditor).validateAndCommit(editCellParent, true);
        }
    }
}
