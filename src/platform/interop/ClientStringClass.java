package platform.interop;

import java.text.Format;

import platform.client.form.*;

public class ClientStringClass extends ClientClass {

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new StringPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new StringPropertyEditor(value); }

    public Class getJavaClass() {
        return String.class;
    }
}
