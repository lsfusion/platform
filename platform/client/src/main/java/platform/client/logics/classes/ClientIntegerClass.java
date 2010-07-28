package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.ParseException;

public class ClientIntegerClass extends ClientIntegralClass {

    public ClientIntegerClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Integer.", 0);
        }
    }
}
