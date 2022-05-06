package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GTextType extends GStringType {

    private final String type;

    public GTextType() {
        this(null);
    }

    public GTextType(String type) {
        super(GExtInt.UNLIMITED, false, false);
        this.type = type;
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new TextCellEditor(editManager, editProperty, inputList);
    }

    @Override
    public GType getFilterType() {
        return this;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTextCaption() + (type != null ? " " + type : "");
    }

}
