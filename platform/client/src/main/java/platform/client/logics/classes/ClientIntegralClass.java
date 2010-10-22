package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.interop.ComponentDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

abstract public class ClientIntegralClass extends ClientDataClass {

    protected ClientIntegralClass() {
    }

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

    protected abstract Class getJavaClass();

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new IntegerPropertyRenderer(format, design);
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new IntegerPropertyEditor(value, (NumberFormat) format, design, getJavaClass());
    }

}
