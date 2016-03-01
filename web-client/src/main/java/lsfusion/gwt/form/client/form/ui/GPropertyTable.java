package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.CopyPasteUtils;
import lsfusion.gwt.base.client.ui.GKeyStroke;
import lsfusion.gwt.cellview.client.Column;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.cellview.client.cell.HasCell;
import lsfusion.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import lsfusion.gwt.form.client.form.dispatch.GEditPropertyHandler;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.classes.GType;
import lsfusion.gwt.form.shared.view.grid.*;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;

import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.CONTEXTMENU;
import static lsfusion.gwt.base.client.GwtClientUtils.removeAllChildren;
import static lsfusion.gwt.base.client.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.cellview.client.cell.Cell.Context;
import static lsfusion.gwt.form.shared.view.GEditBindingMap.getPropertyEditActionSID;
import static lsfusion.gwt.form.shared.view.GEditBindingMap.isEditableAwareEditEvent;

public abstract class GPropertyTable<T> extends DataGrid<T> implements EditManager, GEditPropertyHandler {

    private final GPropertyContextMenuPopup contextMenuPopup = new GPropertyContextMenuPopup();

    protected final GFormController form;
    protected final GEditBindingMap editBindingMap;
    protected final GEditPropertyDispatcher editDispatcher;

    protected GridCellEditor cellEditor = null;
    protected GridEditableCell editCell;
    protected EditEvent editEvent;
    protected Context editContext;
    protected Element editCellParent;
    protected GType editType;

    public GPropertyTable(GFormController iform, Resources resources) {
        this(iform, resources, false);
    }

    public GPropertyTable(GFormController iform, Resources resources, boolean nullHeader) {
        super(resources, nullHeader);

        this.form = iform;

        this.editDispatcher = new GEditPropertyDispatcher(form, this);
        this.editBindingMap = new GEditBindingMap();
        this.editBindingMap.setMouseAction(GEditBindingMap.CHANGE);

        sinkEvents(Event.ONPASTE);
    }

    public GPropertyDraw getProperty(Column column) {
        return getProperty(new Cell.Context(getKeyboardSelectedRow(), getColumnIndex(column), getKeyboardSelectedRowValue()));
    }

    public abstract boolean isEditable(Context context);

    public abstract GPropertyDraw getSelectedProperty();

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

        String actionSID = getPropertyEditActionSID(editEvent, property, editBindingMap);
        if (actionSID == null) {
            return;
        }

        if (isEditableAwareEditEvent(actionSID) && !isEditable(editContext)) {
            return;
        }

        editEvent.stopPropagation();

        this.editCell = editCell;
        this.editEvent = editEvent;
        this.editContext = editContext;
        this.editCellParent = editCellParent;

        GGroupObjectValue columnKey = getColumnKey(editContext);
        Object oldValue = getValueAt(editContext);

        //убираем фокус, чтобы не ловить последующие нажатия
        setFocus(false);
        // с той же целью. для групповой корректировки мало убрать фокус (воспроизводится на стрелках вправо/влево).
        // эта часть нужна главным образом при вызове кнопкой в тулбаре 
        if (GEditBindingMap.GROUP_CHANGE.equals(actionSID)) {
            cellIsEditing = true;
        }

        editDispatcher.setLatestEditEvent(editEvent);
        editDispatcher.executePropertyEditAction(property, columnKey, actionSID, oldValue);
    }

    @Override
    protected <C> void postFireEventToCell(Event event, String eventType, Element cellParent, T rowValue, Context context, HasCell<T, C> column) {
        GPropertyDraw property = getProperty(context);
        String actionSID = getPropertyEditActionSID(editEvent, property, editBindingMap);
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
        Context editContext = new Context(row, column, getRowValue(row));
        Element editCellParent = getCellParent(row, column);

        onEditEvent(editCell, editEvent, editContext, editCellParent);
    }

    public void editCurrentCell(String actionSID) {
        editCellAt(getKeyboardSelectedRow(), getKeyboardSelectedColumn(), actionSID);
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
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            EditEvent event = editEvent;
            editEvent = null;
            form.setCurrentEditingTable(this);

            this.cellEditor = cellEditor;
            editCell.setEditing(true);

            //рендерим эдитор
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
    public void takeFocusAfterEdit() {
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

    private void rerenderCell(Context context, Element parent, Object newValue) {
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
}
