package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.ParseException;

public class ClientDoubleClass extends ClientIntegralClass {

    public ClientDoubleClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Double.", 0);
        }
    }
}
