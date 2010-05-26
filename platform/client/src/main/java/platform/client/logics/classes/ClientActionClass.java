package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.editor.ActionPropertyEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientCellView;
import platform.interop.CellDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.text.Format;
import java.awt.*;

public class ClientActionClass extends ClientDataClass implements ClientType {

    public ClientActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public String getPreferredMask() {
        return "1";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design) { return new ActionPropertyRenderer(caption); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, CellDesign design) throws IOException, ClassNotFoundException { return new ActionPropertyEditor(); }
    public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException { return null; }
    protected PropertyEditorComponent getComponent(Object value, Format format, CellDesign design) { return null; }
}
