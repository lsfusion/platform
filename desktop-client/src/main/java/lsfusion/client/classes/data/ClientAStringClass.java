package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;

import java.awt.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.interop.form.property.Compare.*;

public abstract class ClientAStringClass extends ClientDataClass {

    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final ExtInt length;

    public ClientAStringClass(boolean blankPadded, boolean caseInsensitive, ExtInt length) {
        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, CONTAINS, MATCH};
    }

    public boolean trimTooltip() {
        return true;
    }

    public final static Map<Pair<Boolean, Boolean>, ClientTypeClass> types = new HashMap<>();

    public static ClientTypeClass getTypeClass(boolean blankPadded, boolean caseInsensitive) {
        Pair<Boolean, Boolean> type = new Pair<>(blankPadded, caseInsensitive);
        ClientTypeClass typeClass = types.get(type);
        if(typeClass == null) {
            typeClass = new ClientStringClass.ClientStringTypeClass(blankPadded, caseInsensitive);
            types.put(type, typeClass);
        }
        return typeClass;
    }

    public ClientTypeClass getTypeClass() {
        return getTypeClass(blankPadded, caseInsensitive);
    }

    @Override
    public int getDefaultCharWidth() {
        if(length.isUnlimited())
            return 15;

        int lengthValue = length.getValue();
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        return super.getFullWidthString(widthString, fontMetrics, propertyDraw) + SwingDefaults.getCharWidthSparePixels();
    }

    @Override
    public String formatString(Object obj) {
        if(blankPadded)
            return BaseUtils.rtrim(obj.toString());
        return obj.toString();
    }

    @Override
    public String toString() {
        return getTypeClass().toString() + "(" + length + ")";
    }

    public static class ClientStringTypeClass implements ClientTypeClass {
        public final boolean blankPadded;
        public final boolean caseInsensitive;

        protected ClientStringTypeClass(boolean blankPadded, boolean caseInsensitive) {
            this.blankPadded = blankPadded;
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        public String toString() {
            String result;
            if (caseInsensitive) {
                result = ClientResourceBundle.getString("logics.classes.insensitive.string");
            } else {
                result = ClientResourceBundle.getString("logics.classes.string");
            }
            return result + (blankPadded ? " (bp)" : "") ;
        }
    }
}
