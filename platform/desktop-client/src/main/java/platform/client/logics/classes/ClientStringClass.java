package platform.client.logics.classes;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientStringClass extends ClientAbstractStringClass {
    public final static ClientTypeClass type = new ClientStringTypeClass(false);
    public final static ClientTypeClass insensetiveType = new ClientStringTypeClass(true);

    public final int length;

    private String minimumMask;
    private String preferredMask;

    protected String sID;

    public ClientStringClass(boolean caseInsensitive, int length) {
        super(caseInsensitive);
        this.length = length;

        sID = "StringClass_" + (caseInsensitive ? "insensitive_" : "") + length;

        minimumMask = BaseUtils.replicate('0', length <= 3 ? length : (int) Math.round(Math.pow(length, 0.7)));
        preferredMask = BaseUtils.replicate('0', length <= 20 ? length : (int) Math.round(Math.pow(length, 0.8)));
    }

    public ClientTypeClass getTypeClass() {
        return caseInsensitive ? insensetiveType : type;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        return new StringPropertyEditor(property, value, length, false, false);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new StringPropertyEditor(property, value, length, false, true);
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public String getCode() {
        return "StringClass.get(" + length + "," + caseInsensitive + ")";
    }

    @Override
    public String toString() {
        return getTypeClass().toString() + "(" + length + ")";
    }

    public static class ClientStringTypeClass implements ClientTypeClass {
        public final boolean caseInsensitive;

        protected ClientStringTypeClass(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        public byte getTypeId() {
            return Data.STRING;
        }

        public ClientStringClass getDefaultClass(ClientObjectClass baseClass) {
            return getDefaultType();
        }

        public ClientStringClass getDefaultType() {
            return new ClientStringClass(caseInsensitive, 50);
        }

        @Override
        public String toString() {
            if (caseInsensitive) {
                return ClientResourceBundle.getString("logics.classes.insensitive.string");
            } else {
                return ClientResourceBundle.getString("logics.classes.string");
            }
        }
    }
}
