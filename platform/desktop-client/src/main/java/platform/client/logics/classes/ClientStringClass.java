package platform.client.logics.classes;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Compare;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.START_WITH;

public class ClientStringClass extends ClientDataClass {

    public final int length;

    private String minimumMask;
    private String preferredMask;

    protected String sID;

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public String getCode() {
        return "StringClass.get(" + length + ")";
    }

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
        sID = "StringClass_" + length;

        minimumMask = BaseUtils.replicate('0', length <= 3 ? length : (int) Math.round(Math.pow(length, 0.7)));
        preferredMask = BaseUtils.replicate('0', length <= 20 ? length : (int) Math.round(Math.pow(length, 0.8)));
    }

    public ClientStringClass(int length) {
        this.length = length;
    }

    public final static ClientTypeClass type = new ClientTypeClass() {
        public byte getTypeId() {
            return Data.STRING;
        }

        public ClientStringClass getDefaultClass(ClientObjectClass baseClass) {
            return getDefaultType();
        }

        public ClientStringClass getDefaultType() {
            return new ClientStringClass(50);
        }

        @Override
        public String toString() {
            return ClientResourceBundle.getString("logics.classes.string");
        }
    };
    public ClientTypeClass getTypeClass() {
        return type;
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

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        return new StringPropertyEditor(property, value, length, false);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new StringPropertyEditor(property, value, length, true);
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.string")+"(" + length + ")";
    }

    @Override
    public Compare[] getFilterCompares() {
        return Compare.values();
    }

    @Override
    public Compare getDefaultCompare() {
        return START_WITH;
    }
}
