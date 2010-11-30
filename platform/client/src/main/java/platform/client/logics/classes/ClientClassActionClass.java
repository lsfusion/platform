package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.ClassActionPropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;

public class ClientClassActionClass extends ClientActionClass {

    ClientObjectClass baseClass;
    ClientObjectClass defaultClass;

    private String sID;

    @Override
    public String getSID() {
        return sID;
    }

    public ClientClassActionClass(DataInputStream inStream) throws IOException {
        super(inStream);

        // бредово конечно так делать, но приходится, поскольку Serialize CustomClass записывает в Stream также и свой тип
        // хотя известно, что придет именно ClientObjectClass
        baseClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inStream);
        defaultClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inStream);
        sID = "ClassActionClass[" + defaultClass.getSID() + "]";
    }

    @Override
    public byte getTypeId() {
        return Data.CLASSACTION;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        //todo:
    }

    @Override
    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new ClassActionPropertyEditor(form.getComponent(), baseClass, defaultClass);
    }
}
