package lsfusion.client.classes.data;

import lsfusion.interop.classes.DataType;

public class ClientTSQueryClass extends ClientTextSearchClass {

    public final static ClientTSQueryClass instance = new ClientTSQueryClass();

    public byte getTypeId() {
        return DataType.TSQUERY;
    }
}
