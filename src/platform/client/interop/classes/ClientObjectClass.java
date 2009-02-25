package platform.client.interop.classes;

import java.text.Format;
import java.text.NumberFormat;
import java.io.DataInputStream;
import java.io.IOException;

import platform.client.interop.ClientCellView;
import platform.client.form.*;

public class ClientObjectClass extends ClientClass {

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public ClientObjectClass(DataInputStream inStream) throws IOException {
        super(inStream);
        hasChilds = inStream.readBoolean();
    }

    private boolean hasChilds;
    public boolean hasChilds() {
        return hasChilds;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new ObjectPropertyEditor(form, property, this, value); }

    public Class getJavaClass() {
        return Integer.class;
    }

}
