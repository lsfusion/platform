package platform.client.logics.classes;

import platform.interop.Compare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.EQUALS;
import static platform.interop.Compare.NOT_EQUALS;

public abstract class ClientFileClass extends ClientDataClass implements ClientTypeClass {

    public boolean multiple;
    public boolean storeName;

    public abstract String getFileSID();

    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
    }

    protected ClientFileClass() {
    }
    
    public ClientFileClass(DataInputStream inStream) throws IOException {
        super(inStream);

        multiple = inStream.readBoolean();
        storeName = inStream.readBoolean();
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    public abstract String[] getExtensions();

    public String getPreferredMask() {
        return "1234567";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }
}