package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.StringPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.StringPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.interop.form.property.Compare.*;

public class ClientStringClass extends ClientDataClass {

    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final ExtInt length;

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, LIKE, MATCH};
    }

    @Override
    public Compare getDefaultCompare() {
        return caseInsensitive ? MATCH : EQUALS;
    }

    public final static Map<Pair<Boolean, Boolean>, ClientTypeClass> types = new HashMap<>();

    protected String sID;

    public ClientStringClass(boolean blankPadded, boolean caseInsensitive, ExtInt length) {
        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;

        sID = "StringClass_" + (caseInsensitive ? "insensitive_" : "") + (blankPadded ? "bp_" : "") + length;
    }

    public static ClientTypeClass getTypeClass(boolean blankPadded, boolean caseInsensitive) {
        Pair<Boolean, Boolean> type = new Pair<>(blankPadded, caseInsensitive);
        ClientTypeClass typeClass = types.get(type);
        if(typeClass == null) {
            typeClass = new ClientStringTypeClass(blankPadded, caseInsensitive);
            types.put(type, typeClass);
        }
        return typeClass;
    }

    public ClientTypeClass getTypeClass() {
        return getTypeClass(blankPadded, caseInsensitive);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(blankPadded);
        outStream.writeBoolean(caseInsensitive);
        outStream.writeBoolean(false); // backward compatibility
        length.serialize(outStream);
    }

    @Override
    public int getDefaultCharWidth() {
        if(length.isUnlimited())
            return 15;

        int lengthValue = length.getValue();
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        if(length.isUnlimited())
            return super.getValueEditorComponent(form, property, asyncChange, value);
        return new StringPropertyEditor(property, asyncChange, value, length.getValue(), !blankPadded, false);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new StringPropertyEditor(property, asyncChange, value, length.isUnlimited() ? Integer.MAX_VALUE : length.getValue(), !blankPadded, true);
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

        public byte getTypeId() {
            return DataType.STRING;
        }

        public ClientStringClass getDefaultType() {
            return new ClientStringClass(blankPadded, caseInsensitive, new ExtInt(50));
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
