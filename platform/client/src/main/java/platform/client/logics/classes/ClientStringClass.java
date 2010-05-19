package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.awt.*;

public class ClientStringClass extends ClientDataClass {

    private int length;

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
    }

    @Override
    public int getMinimumWidth(FontMetrics fontMetrics) {
        return length;
    }

    @Override
    public int getPreferredWidth(FontMetrics fontMetrics) { 
        return length * 5;
    }

    @Override
    public String getMinimumMask() {
        return BaseUtils.replicate('A', length / 5);
    }

    public String getPreferredMask() {
        return BaseUtils.replicate('A', length);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, Font font) { return new StringPropertyRenderer(format, font); }
    public PropertyEditorComponent getComponent(Object value, Format format, Font font) { return new StringPropertyEditor(length, value, font); }
}
