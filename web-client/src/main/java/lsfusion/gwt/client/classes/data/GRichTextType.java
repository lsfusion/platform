package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RichTextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.RichTextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GRichTextType extends GTextType {

    public GRichTextType() {
        super("rich");
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new RichTextCellRenderer(property);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new RichTextCellEditor(editManager);
    }
}
