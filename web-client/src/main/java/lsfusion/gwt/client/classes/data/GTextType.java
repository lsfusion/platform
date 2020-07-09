package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.rich.RichTextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GTextType extends GStringType {

    public boolean rich;

    public GTextType() {
    }

    public GTextType(boolean rich) {
        super(GExtInt.UNLIMITED, false, rich);
        this.rich = rich;
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property, rich);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return rich ? new RichTextCellEditor(editManager, editProperty) : new TextCellEditor(editManager, editProperty, rich);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTextCaption() + (rich ? " rich" : "");
    }

}
