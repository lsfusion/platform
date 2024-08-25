package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.HTMLStringCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.HTMLTextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GHTMLStringType extends GAStringType {

    public static final GHTMLStringType instance = new GHTMLStringType();

    public GHTMLStringType() {
        super(GExtInt.UNLIMITED, false, false);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new HTMLTextCellRenderer(property);
    }

    //
//    @Override
//    public InputElement createTextInputElement() {
//        return Document.get().createTextAreaElement().cast();
//    }
//
    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new HTMLStringCellEditor(editManager, editProperty, inputList, inputListActions);
    }
}
