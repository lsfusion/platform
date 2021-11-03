package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientType;
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

public class ClientTextClass extends ClientStringClass implements ClientTypeClass {
    private final String type;

    public ClientTextClass() {
        this(null);
    }

    public ClientTextClass(String type) {
        super(false, false, ExtInt.UNLIMITED);
        this.type = type;
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
        return DataType.TEXT;
    }

    @Override
    public ClientType getDefaultType() {
        return this;
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.text") + (type != null ? " (" + type + ")" : "");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, false);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return new TextPropertyEditor(ownerComponent, value, property.design);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return  new TextPropertyEditor(value, property.design);
    }

}
