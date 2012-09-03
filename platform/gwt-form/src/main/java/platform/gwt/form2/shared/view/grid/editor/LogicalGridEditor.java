package platform.gwt.form2.shared.view.grid.editor;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.form2.shared.view.grid.EditManager;

public class LogicalGridEditor implements GridCellEditor {
    public LogicalGridEditor(EditManager editManager, Object oldValue) {
        this.editManager = editManager;
        currentValue = (Boolean) oldValue;
    }

    protected EditManager editManager;
    protected Boolean currentValue;

    @Override
    public void startEditing(NativeEvent editEvent, Cell.Context context, Element parent) {
        editManager.commitEditing(currentValue == null || !currentValue ? true : null);
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event, ValueUpdater<Object> valueUpdater) {
        //NOP
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        //NOP
    }

    @Override
    public boolean resetFocus(Cell.Context context, Element parent, Object value) {
        //NOP
        return true;
    }
}
