package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GridEditableCell extends AbstractCell<Object> {

    private final GridCellRenderer cellRenderer;

    private final EditManager editManager;

    private GridCellEditor cellEditor = null;
    private GridDataRecord editRecord = null;

    public GridEditableCell(EditManager editManager, GridCellRenderer cellRenderer) {
        super("dblclick", "keyup", "keydown", "keypress", "blur");
        this.editManager = editManager;
        this.cellRenderer = cellRenderer;
    }

    @Override
    public boolean isEditing(Cell.Context context, Element parent, Object value) {
        return isEditingCell(context);
    }

    @Override
    public void onBrowserEvent(final Cell.Context context, final Element parent, final Object value,
                               NativeEvent event, ValueUpdater<Object> valueUpdater) {
        if (isEditingCell(context)) {
            cellEditor.onBrowserEvent(context, parent, value, event, valueUpdater);
        } else if (editManager.canStartNewEdit()) {
            String eventType = event.getType();
            int keyCode = event.getKeyCode();
            boolean editKeyPress = "keypress".equals(eventType) && keyCode != KeyCodes.KEY_ENTER;
            if ("dblclick".equals(eventType) || editKeyPress) {
                event.stopPropagation();
                event.preventDefault();
                editManager.executePropertyEditAction(this, event, context, parent);
            }
        }
    }

    public void startEditing(NativeEvent editEvent, final Context context, Element parent, GridCellEditor cellEditor) {
        this.editRecord = (GridDataRecord) context.getKey();
        this.cellEditor = cellEditor;

        //рендерим эдитор
        setValue(context, parent, null);

        cellEditor.startEditing(editEvent, context, parent);
    }

    public void finishEditing(Context context, Element parent, Object newValue) {
        this.editRecord = null;
        this.cellEditor = null;

        setValue(context, parent, newValue);
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        if (isEditingCell(context)) {
            cellEditor.render(context, value, sb);
        } else {
            cellRenderer.render(context, value, sb);
        }
    }

    @Override
    public boolean resetFocus(Cell.Context context, Element parent, Object value) {
        return isEditingCell(context) && cellEditor.resetFocus(context, parent, value);
    }

    private boolean isEditingCell(Cell.Context context) {
        if (context.getKey() == editRecord) {
            assert cellEditor != null;
            return true;
        }
        return false;
    }
}