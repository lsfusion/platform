package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public abstract class ClientDataClass extends ClientClass implements ClientType {

    ClientDataClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public boolean hasChilds() {
        return false;
    }

    public int getMinimumWidth() {
        return getPreferredWidth();
    }
    public int getPreferredWidth() {
        return 50;
    }
    public int getMaximumWidth() {
        return Integer.MAX_VALUE;
    }

    protected abstract PropertyEditorComponent getComponent(Object value, Format format);

    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return getComponent(value, format);
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return getComponent(value, format);
    }
}
