package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.ClassActionPropertyEditor;
import platform.client.logics.ClientCell;
import platform.interop.ComponentDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientClassActionClass extends ClientActionClass {

    ClientObjectClass baseClass;

    public ClientClassActionClass(DataInputStream inStream) throws IOException {
        super(inStream);

        // бредово конечно так делать, но приходится, поскольку Serialize CustomClass записывает в Stream также и свой тип
        // хотя известно, что придет именно ClientObjectClass
        baseClass = (ClientObjectClass)ClientClass.deserialize(inStream);
    }

    @Override
    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientCell property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new ClassActionPropertyEditor(form.getComponent(), baseClass, baseClass);
    }
}
