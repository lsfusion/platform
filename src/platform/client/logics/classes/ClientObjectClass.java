package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

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
