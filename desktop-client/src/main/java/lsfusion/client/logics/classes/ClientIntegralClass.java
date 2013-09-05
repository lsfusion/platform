package lsfusion.client.logics.classes;

import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.renderer.IntegerPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import java.text.*;
import java.util.EventObject;

import static lsfusion.client.form.EditBindingMap.EditEventFilter;
import static lsfusion.interop.KeyStrokes.isSuitableNumberEditEvent;

abstract public class ClientIntegralClass extends ClientDataClass {

    public static final EditEventFilter numberEditEventFilter = new EditEventFilter() {
        public boolean accept(EventObject e) {
            return isSuitableNumberEditEvent(e);
        }
    };

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


    @Override
    public EditEventFilter getEditEventFilter() {
        return numberEditEventFilter;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    public abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);
}
