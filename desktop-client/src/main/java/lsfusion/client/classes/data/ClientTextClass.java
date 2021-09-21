package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TextPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.rich.RichTextPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TextPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientTextClass extends ClientStringClass implements ClientTypeClass {

    public final boolean rich;

    public ClientTextClass(boolean rich) {
        super(false, false, ExtInt.UNLIMITED);
        this.rich = rich;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(rich);
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
        return ClientResourceBundle.getString("logics.classes.text") + (rich ? " (rich)" : "");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, rich);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return rich ? new RichTextPropertyEditor(ownerComponent, value, property.design) : new TextPropertyEditor(ownerComponent, value, property.design);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return rich ? new RichTextPropertyEditor(value, property.design) : new TextPropertyEditor(value, property.design);
    }

}
