package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.HTMLTextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.HTMLTextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GHTMLTextType extends GTextType {

    public GHTMLTextType() {
        super("html");
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new HTMLTextCellRenderer(property);
    }

    @Override
    public CellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new HTMLTextCellEditor(editManager);
    }
}
