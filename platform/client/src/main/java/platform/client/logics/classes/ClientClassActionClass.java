package platform.client.logics.classes;

import platform.client.logics.classes.ClientActionClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.ClientCellView;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.editor.ClassPropertyEditor;
import platform.client.form.editor.ClassActionPropertyEditor;
import platform.interop.CellDesign;

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
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, CellDesign design) throws IOException, ClassNotFoundException {
        return new ClassActionPropertyEditor(form, baseClass, baseClass);
    }
}
