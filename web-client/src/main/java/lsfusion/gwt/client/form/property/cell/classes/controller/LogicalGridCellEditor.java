package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.property.cell.controller.AbstractGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LogicalGridCellEditor extends AbstractGridCellEditor {
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
