package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientStringClass extends ClientClass {

    private int length;

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
    }

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new StringPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new StringPropertyEditor(length, value); }

    public Class getJavaClass() {
        return String.class;
    }
}
