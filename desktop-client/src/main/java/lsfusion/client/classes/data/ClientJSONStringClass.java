package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.interop.classes.DataType;

public class ClientJSONStringClass extends ClientAJSONClass {

    public byte getTypeId() {
        return DataType.JSONSTRING;
    }

    public final static ClientJSONStringClass instance = new ClientJSONStringClass();

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.string");
    }
}