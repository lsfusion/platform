package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.*;

abstract public class ClientIntegralClass extends ClientDataClass {

    protected ClientIntegralClass() {
    }

    ClientIntegralClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String getMinimumMask() {
        return "9 999 999";
    }

    public String getPreferredMask() {
        return "99 999 999";
    }

    public Format getDefaultFormat() {
        NumberFormat format = new DecimalFormat() {
            @Override
            public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
                if (obj == null) {
                    try {
                        return super.formatToCharacterIterator(parseString("0"));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return super.formatToCharacterIterator(obj);
                }
            }
        }; // временно так чтобы устранить баг, но теряется locale, NumberFormat.getInstance()
        format.setGroupingUsed(true);
        return format;
    }

    protected abstract Class getJavaClass();

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new IntegerPropertyRenderer(format, design);
    }

    @Override
    protected PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return new IntegerPropertyEditor(value, (NumberFormat) property.getFormat(), property.design, getJavaClass());
    }
}
