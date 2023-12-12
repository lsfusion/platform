package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
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
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new TextCellEditor(editManager, editProperty, inputList, editContext);
    }

    @Override
    public GType getFilterMatchType() {
        return this;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTextCaption() + (type != null ? " " + type : "");
    }

    @Override
    public String getVertTextAlignment(boolean isInput) {
        return "top";
    }

    private final static GInputType inputType = new GInputType("textarea");
    @Override
    public GInputType getValueInputType() {
        return inputType;
    }
}
