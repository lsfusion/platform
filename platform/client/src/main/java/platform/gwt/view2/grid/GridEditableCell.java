package platform.gwt.view2.grid;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.view2.GridDataRecord;
import platform.gwt.view2.grid.editor.GridCellEditor;
import platform.gwt.view2.grid.renderer.GridCellRenderer;

public class GridEditableCell extends AbstractCell<Object> {

    private final GridCellRenderer cellRenderer;

    private final EditManager editManager;

    private GridCellEditor cellEditor = null;
    private GridDataRecord editRecord = null;

    public GridEditableCell(EditManager editManager, GridCellRenderer cellRenderer) {
        super("click", "dblclick", "keyup", "keydown", "blur");
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
        } else if (!editManager.isCurrentlyEditing()) {
            //пока редактирование только по дабл-клику
            if ("dblclick".equals(event.getType())) {
                editManager.executePropertyEditAction(this, context, parent);
            }
        }
    }

    public void startEditing(final Cell.Context context, Element parent, GridCellEditor cellEditor) {
        this.editRecord = (GridDataRecord) context.getKey();
        this.cellEditor = cellEditor;

        //рендерим эдитор
        setValue(context, parent, null);

        cellEditor.startEditing(context, parent);
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