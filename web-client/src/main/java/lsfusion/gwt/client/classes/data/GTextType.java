package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.rich.RichTextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.RichTextCellRenderer;
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
        return rich ? new RichTextCellRenderer(property) : new TextCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return rich ? new RichTextCellEditor(editManager, editProperty) : new TextCellEditor(editManager, editProperty, inputList);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTextCaption() + (rich ? " rich" : "");
    }

}
