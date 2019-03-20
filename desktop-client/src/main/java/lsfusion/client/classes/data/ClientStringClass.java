package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.base.Pair;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.StringPropertyEditor;
import lsfusion.client.form.property.classes.editor.TextPropertyEditor;
import lsfusion.client.form.property.classes.editor.rich.RichTextPropertyEditor;
import lsfusion.client.form.property.classes.renderer.StringPropertyRenderer;
import lsfusion.client.form.property.classes.renderer.TextPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.DataType;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.interop.form.property.Compare.CONTAINS;
import static lsfusion.interop.form.property.Compare.EQUALS;

public class ClientStringClass extends ClientDataClass {

    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final boolean rich;
    public final ExtInt length;

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public Compare[] getFilterCompares() {
        return Compare.values();
    }

    @Override
    public Compare getDefaultCompare() {
        return caseInsensitive ? CONTAINS : EQUALS;
    }

    public final static Map<Pair<Boolean, Boolean>, ClientTypeClass> types = new HashMap<>();


    protected String sID;

    public ClientStringClass(boolean blankPadded, boolean caseInsensitive, boolean rich, ExtInt length) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.rich = rich;
        this.length = length;

        sID = "StringClass_" + (caseInsensitive ? "insensitive_" : "") + (blankPadded ? "bp_" : "") + length;
    }

    public static ClientTypeClass getTypeClass(boolean blankPadded, boolean caseInsensitive) {
        return getTypeClass(blankPadded, caseInsensitive, false);
    }
    
    public static ClientTypeClass getTypeClass(boolean blankPadded, boolean caseInsensitive, boolean rich) {
        Pair<Boolean, Boolean> type = new Pair<>(blankPadded, caseInsensitive);
        ClientTypeClass typeClass = types.get(type);
        if(typeClass == null) {
            typeClass = new ClientStringTypeClass(blankPadded, caseInsensitive, rich);
            types.put(type, typeClass);
        }
        return typeClass;
    }

    public ClientTypeClass getTypeClass() {
        return getTypeClass(blankPadded, caseInsensitive, rich);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(blankPadded);
        outStream.writeBoolean(caseInsensitive);
        outStream.writeBoolean(rich);
        length.serialize(outStream);
    }

    @Override
    public int getDefaultCharWidth() {
        if(length.isUnlimited()) {
            return 15;
        } else {
            int lengthValue = length.getValue();
            return lengthValue <= 12 ? lengthValue : (int) round(12 + pow(lengthValue - 12, 0.7));
        }
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        if(length.isUnlimited()) {
            return new TextPropertyRenderer(property, rich);
        }
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
        if(length.isUnlimited()) {
            return rich ? new RichTextPropertyEditor(ownerComponent, value, property.design) : new TextPropertyEditor(ownerComponent, value, property.design);
        }
        return super.getChangeEditorComponent(ownerComponent, form, property, value);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        if(length.isUnlimited()) {
            return rich ? new RichTextPropertyEditor(value, property.design) : new TextPropertyEditor(value, property.design);
        }
        return new StringPropertyEditor(property, value, length.getValue(), !blankPadded, true);
    }

    @Override
    public String formatString(Object obj) {
        if(blankPadded)
            return BaseUtils.rtrim(obj.toString());
        return obj.toString();
    }

    @Override
    public int getDefaultHeight(FontMetrics fontMetrics, int charHeight) {
        return super.getDefaultHeight(fontMetrics, charHeight == 1 && length.isUnlimited() ? 4 : charHeight);
    }

    @Override
    public String toString() {
        return getTypeClass().toString() + "(" + length + ")";
    }

    public static class ClientStringTypeClass implements ClientTypeClass {
        public final boolean blankPadded;
        public final boolean caseInsensitive;
        public final boolean rich;

        protected ClientStringTypeClass(boolean blankPadded, boolean caseInsensitive, boolean rich) {
            this.blankPadded = blankPadded;
            this.caseInsensitive = caseInsensitive;
            this.rich = rich;
        }

        public byte getTypeId() {
            return DataType.STRING;
        }

        public ClientStringClass getDefaultType() {
            return new ClientStringClass(blankPadded, caseInsensitive, false, new ExtInt(50));
        }

        @Override
        public String toString() {
            String result;
            if (caseInsensitive) {
                result = ClientResourceBundle.getString("logics.classes.insensitive.string");
            } else {
                result = ClientResourceBundle.getString("logics.classes.string");
            }
            return result + (blankPadded ? " (bp)" : "") + (rich ? " (rich)" : "");
        }
    }
}
