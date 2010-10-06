package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.TextPropertyEditor;
import platform.client.form.renderer.TextPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientTextClass extends ClientDataClass {

    public ClientTextClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public byte getTypeId() {
        return Data.TEXT;
    }

    public String getMinimumMask() {
        return "999 999";
    }

    public String getPreferredMask() {
        return "9 999 999";
    }

    public Format getDefaultFormat() {
        return null;
    }

    @Override
    public int getPreferredHeight(FontMetrics fontMetrics) {
        return 4 * (fontMetrics.getHeight() + 1);
    }

    @Override
    public int getPreferredWidth(FontMetrics fontMetrics) {
        return fontMetrics.getWidths()[48] * 25;//stringWidth(getPreferredMask()) + 8;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new TextPropertyRenderer(format, design);
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new TextPropertyEditor(value, design);
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }
}
