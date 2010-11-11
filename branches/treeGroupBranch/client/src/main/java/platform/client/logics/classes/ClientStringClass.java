package platform.client.logics.classes;

import platform.base.BaseUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.StringPropertyEditor;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientStringClass extends ClientDataClass {

    public final int length;

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
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
            return "Строка";
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
        return BaseUtils.replicate('0', length / 5);
    }

    public String getPreferredMask() {
        return BaseUtils.replicate('0', length);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) { return new StringPropertyRenderer(format, design); }
    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) { return new StringPropertyEditor(length, value, design); }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public String toString() {
        return "Строка(" + length + ")";
    }
}
