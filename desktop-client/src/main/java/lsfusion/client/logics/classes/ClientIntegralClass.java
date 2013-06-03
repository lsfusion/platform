package lsfusion.client.logics.classes;

import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.renderer.IntegerPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import java.text.*;

abstract public class ClientIntegralClass extends ClientDataClass {

    protected ClientIntegralClass() {
    }

    @Override
    public String getMinimumMask() {
        return "99 999 999";
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

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    public abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);
}
