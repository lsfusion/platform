package platform.client.logics.classes;

import platform.base.BaseUtils;
import platform.base.ExtInt;
import platform.base.Pair;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.form.editor.TextPropertyEditor;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.form.renderer.TextPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Compare;
import platform.interop.Data;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static platform.interop.Compare.CONTAINS;

public class ClientStringClass extends ClientDataClass {

    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final ExtInt length;

    public Format getDefaultFormat() {
        return null;
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public Object transformServerValue(Object obj) {
        return obj == null ? null : formatString(obj);
    }

    @Override
    public Compare[] getFilterCompares() {
        return Compare.values();
    }

    @Override
    public Compare getDefaultCompare() {
        return CONTAINS;
    }

    public final static Map<Pair<Boolean, Boolean>, ClientTypeClass> types = new HashMap<Pair<Boolean, Boolean>, ClientTypeClass>();


    private String minimumMask;
    private String preferredMask;

    protected String sID;

    public ClientStringClass(boolean blankPadded, boolean caseInsensitive, ExtInt length) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;

        sID = "StringClass_" + (caseInsensitive ? "insensitive_" : "") + (blankPadded ? "bp_" : "") + length;

        if(length.isUnlimited()) {
            minimumMask = "999 999";
            preferredMask = "9 999 999";
        } else {
            int lengthValue = length.getValue();
            minimumMask = BaseUtils.replicate('0', lengthValue <= 3 ? lengthValue : (int) Math.round(Math.pow(lengthValue, 0.7)));
            preferredMask = BaseUtils.replicate('0', lengthValue <= 20 ? lengthValue : (int) Math.round(Math.pow(lengthValue, 0.8)));
        }
    }

    public static ClientTypeClass getTypeClass(boolean blankPadded, boolean caseInsensitive) {
        Pair<Boolean, Boolean> type = new Pair<Boolean, Boolean>(blankPadded, caseInsensitive);
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
        length.serialize(outStream);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        if(length.isUnlimited())
            return new TextPropertyRenderer(property);
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        if(length.isUnlimited())
            return super.getValueEditorComponent(form, property, value);
        return new StringPropertyEditor(property, value, length.getValue(), !blankPadded, false);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) {
        if(length.isUnlimited())
            return new TextPropertyEditor(ownerComponent, value, property.design);
        return super.getChangeEditorComponent(ownerComponent, form, property, value);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        if(length.isUnlimited())
            return new TextPropertyEditor(value, property.design);
        return new StringPropertyEditor(property, value, length.getValue(), !blankPadded, true);
    }

    @Override
    public String formatString(Object obj) {
        if(blankPadded)
            return BaseUtils.rtrim(obj.toString());
        return obj.toString();
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public String getCode() {
        return "StringClass.get(" + blankPadded + "," + caseInsensitive + "," + length + ")";
    }

    @Override
    public int getPreferredHeight(FontMetrics fontMetrics) {
        if(length.isUnlimited())
            return 4 * (fontMetrics.getHeight() + 1);
        return super.getPreferredHeight(fontMetrics);
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        if(length.isUnlimited())
            return Integer.MAX_VALUE;
        return super.getPreferredHeight(fontMetrics);
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        if(length.isUnlimited())
            return fontMetrics.charWidth('0') * 25;//stringWidth(getPreferredMask()) + 8;
        return super.getPreferredWidth(prefCharWidth, fontMetrics);
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
            return Data.STRING;
        }

        public ClientStringClass getDefaultClass(ClientObjectClass baseClass) {
            return getDefaultType();
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
            return result + (blankPadded ? " (bp)" : "");
        }
    }
}
