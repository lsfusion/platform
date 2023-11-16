package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class ColorCellEditor extends TypeInputBasedCellEditor {

    public ColorCellEditor(EditManager editManager) {
        super(editManager);
    }

    @Override
    public void start(EventHandler handler, Element parent, PValue oldValue) {
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        String value = TypeInputBasedCellRenderer.getInputElement(parent).getValue();
        return value != null ? PValue.getPValue(new ColorDTO(value.substring(1))) : null;
    }
}
