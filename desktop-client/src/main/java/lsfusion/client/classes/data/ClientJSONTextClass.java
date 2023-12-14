package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.interop.classes.DataType;

public class ClientJSONTextClass extends ClientAJSONClass {

    public byte getTypeId() {
        return DataType.JSONTEXT;
    }

    public final static ClientJSONTextClass instance = new ClientJSONTextClass();

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.string");
    }
}