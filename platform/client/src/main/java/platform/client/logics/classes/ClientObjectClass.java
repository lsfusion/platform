package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.form.editor.ObjectPropertyEditor;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

public abstract class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    public ClientType getType() {
        return type;
    }

    public int ID;
    private String caption;
    public String toString() { return caption; }
    
    public ClientObjectClass(DataInputStream inStream) throws IOException {
        super(inStream);
        caption = inStream.readUTF();
        ID = inStream.readInt();
        hasChilds = inStream.readBoolean();
    }

    public static ClientClass deserializeObject(DataInputStream inStream) throws IOException {
        boolean concrete = inStream.readBoolean();
        if(concrete)
            return new ClientConcreteClass(inStream);
        else
            return new ClientAbstractClass(inStream); 
    }

    private boolean hasChilds;
    public boolean hasChilds() {
        return hasChilds;
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(form, property, this, value);
    }
}
