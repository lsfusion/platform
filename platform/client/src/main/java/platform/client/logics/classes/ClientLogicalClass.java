package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.LogicalPropertyEditor;
import platform.client.form.renderer.LogicalPropertyRenderer;
import platform.interop.CellDesign;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientLogicalClass extends ClientDataClass {

    public ClientLogicalClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public int getMinimumWidth(FontMetrics fontMetrics) {
        return getPreferredWidth(fontMetrics);
    }


    public int getPreferredWidth(FontMetrics fontMetrics) {
        return 25;
    }

    public String getPreferredMask() {
        return "";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design) { return new LogicalPropertyRenderer(); }
    public PropertyEditorComponent getComponent(Object value, Format format, CellDesign design) { return new LogicalPropertyEditor(value); }
}
