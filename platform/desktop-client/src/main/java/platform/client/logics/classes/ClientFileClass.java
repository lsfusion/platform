package platform.client.logics.classes;

import platform.interop.Compare;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.EQUALS;
import static platform.interop.Compare.NOT_EQUALS;

public abstract class ClientFileClass extends ClientDataClass implements ClientTypeClass {

    public boolean multiple;

    public abstract String getFileSID();

    public String getSID() {
        return getFileSID() + (multiple?"_Multiple":"");
    }

    protected ClientFileClass() {
    }
    
    public ClientFileClass(DataInputStream inStream) throws IOException {
        super(inStream);

        multiple = inStream.readBoolean();
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