package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.ParseException;

public class ClientLongClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientLongClass instance = new ClientLongClass();

    public Class getJavaClass() {
        return Long.class;
    }

    public ClientTypeClass getTypeClass() {
        return this;
    }

    public ClientClass getDefaultClass() {
        return this;
    }

    public byte getTypeId() {
        return Data.LONG;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Long.", 0);
        }
    }

    @Override
    public String toString() {
        return "Длинное целое число";
    }
}
