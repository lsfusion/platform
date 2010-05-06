package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientStringClass extends ClientDataClass {

    private int length;

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
    }

    public int getMinimumWidth() { return length; }
    public int getPreferredWidth() { return length * 5; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new StringPropertyRenderer(format); }
    public PropertyEditorComponent getComponent(Object value, Format format) { return new StringPropertyEditor(length, value); }
}
