package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.editor.ActionPropertyEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.awt.*;

public class ClientActionClass extends ClientLogicalClass {
    
    public ClientActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public int getPreferredWidth(FontMetrics fontMetrics) {
        return 50;
    }

    @Override
    public PropertyRendererComponent getRendererComponent(Format format, String caption, Font font) { return new ActionPropertyRenderer(caption); }

    @Override
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException { return new ActionPropertyEditor(); }
}
