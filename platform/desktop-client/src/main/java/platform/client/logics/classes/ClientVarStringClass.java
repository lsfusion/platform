package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

public class ClientVarStringClass extends ClientStringClass {
    public final static ClientTypeClass type = new ClientVarStringTypeClass(false);
    public final static ClientTypeClass insensetiveType = new ClientVarStringTypeClass(true);

    protected String sID;

    public ClientVarStringClass(boolean caseInsensitive, int length) {
        super(caseInsensitive, length);
        sID = "VarStringClass_" + (caseInsensitive ? "insensitive_" : "") + length;
    }

    public ClientTypeClass getTypeClass() {
        return caseInsensitive ? insensetiveType : type;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        return new StringPropertyEditor(property, value, length, true, false);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new StringPropertyEditor(property, value, length, true, true);
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public String getCode() {
        return "StringClass.getv(" + length + "," + caseInsensitive + ")";
    }

    @Override
    public String toString() {
        return getTypeClass().toString() + "(" + length + ")";
    }

    public static class ClientVarStringTypeClass extends ClientStringTypeClass {

        protected ClientVarStringTypeClass(boolean caseInsensitive) {
            super(caseInsensitive);
        }

        public byte getTypeId() {
            return Data.VARSTRING;
        }

        public ClientVarStringClass getDefaultType() {
            return new ClientVarStringClass(caseInsensitive, 50);
        }

        @Override
        public String toString() {
            if (caseInsensitive) {
                return ClientResourceBundle.getString("logics.classes.var.insensitive.string");
            } else {
                return ClientResourceBundle.getString("logics.classes.var.string");
            }
        }
    }
}
