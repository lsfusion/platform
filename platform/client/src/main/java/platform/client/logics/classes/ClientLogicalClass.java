package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.LogicalPropertyRenderer;
import platform.client.form.editor.LogicalPropertyEditor;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientLogicalClass extends ClientDataClass {

    public ClientLogicalClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new LogicalPropertyRenderer(); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new LogicalPropertyEditor(value); }
}
