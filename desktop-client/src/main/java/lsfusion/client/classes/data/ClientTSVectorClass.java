package lsfusion.client.classes.data;

import lsfusion.interop.classes.DataType;

public class ClientTSVectorClass extends ClientTextSearchClass {

    public final static ClientTSVectorClass instance = new ClientTSVectorClass();

    public byte getTypeId() {
        return DataType.TSVECTOR;
    }
}
