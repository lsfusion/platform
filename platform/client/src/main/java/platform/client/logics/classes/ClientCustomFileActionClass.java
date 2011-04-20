package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.CustomFileEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;

public class ClientCustomFileActionClass extends ClientActionClass {
    private String sID;

    @Override
    public String getSID() {
        return sID;
    }

    public ClientCustomFileActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
        sID = "ClientCustomFileActionClass";
    }

    @Override
    public byte getTypeId() {
        return Data.CUSTOMFILEACTION;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }

    @Override
    public PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new CustomFileEditor(value, false);
    }
}