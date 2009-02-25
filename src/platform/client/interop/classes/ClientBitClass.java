package platform.client.interop.classes;

import java.text.Format;
import java.io.DataInputStream;
import java.io.IOException;

import platform.client.interop.ClientCellView;
import platform.client.form.*;

public class ClientBitClass extends ClientClass {

    public ClientBitClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new BitPropertyRenderer(); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new BitPropertyEditor(value); }

    public Class getJavaClass() {
        return Boolean.class;
    }

}
