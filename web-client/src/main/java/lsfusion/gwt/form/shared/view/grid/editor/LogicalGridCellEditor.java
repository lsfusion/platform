package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class LogicalGridCellEditor implements GridCellEditor {
    public LogicalGridCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    protected EditManager editManager;

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        Boolean currentValue = (Boolean) oldValue;
        editManager.commitEditing(currentValue == null || !currentValue ? true : null);
    }

    @Override
    public boolean replaceCellRenderer() {
        return true;
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event) {
        //NOP
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value) {
        //NOP
    }
}
