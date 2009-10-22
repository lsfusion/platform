package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.BitPropertyEditor;
import platform.client.form.renderer.BitPropertyRenderer;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientBitClass extends ClientDataClass {

    public ClientBitClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new BitPropertyRenderer(); }
    public PropertyEditorComponent getComponent(Object value, Format format) { return new BitPropertyEditor(value); }
}
