package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.controller.AbstractGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LogicalGridCellEditor extends AbstractGridCellEditor {
    public LogicalGridCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    protected EditManager editManager;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        Boolean currentValue = (Boolean) oldValue;
        editManager.commitEditing(currentValue == null || !currentValue ? true : null);
    }

    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        //NOP
    }
}
