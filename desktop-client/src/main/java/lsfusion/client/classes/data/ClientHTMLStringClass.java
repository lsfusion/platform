package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TextPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TextPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientHTMLStringClass extends ClientStringClass implements ClientTypeClass {


    public final static ClientHTMLStringClass instance = new ClientHTMLStringClass();

    public ClientHTMLStringClass() {
        super(false, false, ExtInt.UNLIMITED);
    }


    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeClass().getTypeId());
    }

    @Override
    public ClientTypeClass getTypeClass() {
        return this;
    }

    @Override
    public byte getTypeId() {
        return DataType.HTMLSTRING;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, true);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return new TextPropertyEditor(ownerComponent, value, property.design);
    }

    @Override
    public String toString() {
        return "HTML";
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new TextPropertyEditor(value, property.design);
    }
}
