package platform.client.logics.classes;

import platform.interop.Compare;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.*;

public abstract class ClientFileClass extends ClientDataClass implements ClientTypeClass {

    protected ClientFileClass() {
    }
    
    public ClientFileClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

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
    public Compare[] getFilerCompares() {
        return new Compare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }

}