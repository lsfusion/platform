package platform.gwt.form.shared.view.grid;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.form.client.form.ui.GPropertyTable;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class GridEditableCell extends AbstractCell<Object> {

    private final GPropertyTable table;

    private GridCellEditor cellEditor = null;
    private Object editKey = null;

    public GridEditableCell(GPropertyTable table) {
        super(DBLCLICK, KEYDOWN, KEYPRESS, BLUR, CONTEXTMENU);
        this.table = table;
    }

    @Override
    public boolean isEditing(Context context, Element parent, Object value) {
        return isEditingCell(context);
    }

    @Override
    public void onBrowserEvent(final Context context, final Element parent, final Object value,
                               NativeEvent event, ValueUpdater<Object> valueUpdater) {
        if (isEditingCell(context)) {
            cellEditor.onBrowserEvent(context, parent, value, event, valueUpdater);
        } else {
            table.onEventFromCell(this, event, context, parent);
        }
    }

    public void startEditing(EditEvent editEvent, final Context context, Element parent, GridCellEditor cellEditor, Object oldValue) {
        this.editKey = context.getKey();
        this.cellEditor = cellEditor;

        //рендерим эдитор
        setValue(context, parent, oldValue);

        cellEditor.startEditing(editEvent, context, parent, oldValue);
    }

    public void finishEditing(Context context, Element parent, Object newValue) {
        this.editKey = null;
        this.cellEditor = null;

        setValue(context, parent, newValue);
    }

    @Override
    public void render(Context context, Object value, SafeHtmlBuilder sb) {
        if (isEditingCell(context)) {
            cellEditor.render(context, value, sb);
        } else {
            GPropertyDraw property = table.getProperty(context);
            if (property != null) {
                GridCellRenderer cellRenderer = property.getGridCellRenderer();
                cellRenderer.render(context, value, sb);
            }
        }
    }

    @Override
    public boolean resetFocus(Context context, Element parent, Object value) {
        return isEditingCell(context) && cellEditor.resetFocus(context, parent, value);
    }

    private boolean isEditingCell(Context context) {
        if (context.getKey() == editKey) {
            assert cellEditor != null;
            return true;
        }
        return false;
    }
}