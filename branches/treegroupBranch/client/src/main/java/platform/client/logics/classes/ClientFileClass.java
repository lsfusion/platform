package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

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
}