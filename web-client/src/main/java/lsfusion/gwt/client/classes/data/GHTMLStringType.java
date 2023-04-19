package lsfusion.gwt.client.classes.data;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.HTMLStringCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.HTMLTextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.HTMLTextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GHTMLStringType extends GStringType {

    public static final GHTMLStringType instance = new GHTMLStringType();

    public GHTMLStringType() {
        super(GExtInt.UNLIMITED, false, false);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new HTMLTextCellRenderer(property);
    }

    @Override
    public GType getFilterMatchType() {
        return this;
    }
//
//    @Override
//    public InputElement createTextInputElement() {
//        return Document.get().createTextAreaElement().cast();
//    }
//
    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new HTMLStringCellEditor(editManager, editProperty, inputList);
    }
}
