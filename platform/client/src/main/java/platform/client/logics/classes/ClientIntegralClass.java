package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.form.editor.IntegerPropertyEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.awt.*;

abstract public class ClientIntegralClass extends ClientDataClass {

    ClientIntegralClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String getMinimumMask() {
        return "999 999";
    }

    public String getPreferredMask() {
        return "9 999 999";
    }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    protected abstract Class getJavaClass() ;

    public PropertyRendererComponent getRendererComponent(Format format, String caption, Font font) { return new IntegerPropertyRenderer(format, font); }
    public PropertyEditorComponent getComponent(Object value, Format format, Font font) { return new IntegerPropertyEditor(value, (NumberFormat)format, font, getJavaClass()); }

}
