package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.ParseException;

public class ClientLongClass extends ClientIntegralClass {

    public ClientLongClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Long.", 0);
        }
    }
}
