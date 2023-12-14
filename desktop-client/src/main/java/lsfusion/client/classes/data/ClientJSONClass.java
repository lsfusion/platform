package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.interop.classes.DataType;

public class ClientJSONClass extends ClientAJSONClass {

    public byte getTypeId() {
        return DataType.JSON;
    }

    public final static ClientJSONClass instance = new ClientJSONClass();

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json");
    }
}