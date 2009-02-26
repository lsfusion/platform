package platform.client.interop.classes;

import platform.client.form.*;
import platform.client.interop.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientStringClass extends ClientClass {

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new StringPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new StringPropertyEditor(value); }

    public Class getJavaClass() {
        return String.class;
    }
}
