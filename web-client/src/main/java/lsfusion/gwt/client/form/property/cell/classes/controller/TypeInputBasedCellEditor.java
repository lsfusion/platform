package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;

public abstract class TypeInputBasedCellEditor extends ARequestValueCellEditor implements KeepCellEditor {

    public TypeInputBasedCellEditor(EditManager editManager) {
        super(editManager);
    }

    public void commit(Element parent) {
        commit(parent, CommitReason.FORCED);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if(GKeyStroke.isChangeEvent(handler.event)) {
            commit(parent);
            handler.consume();
        }
    }
}
