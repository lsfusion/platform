package lsfusion.server.session;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.Modify;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public class NoPropertyTableUsage<K> extends SessionTableUsage<K,Object> {

    public NoPropertyTableUsage(ImOrderSet<K> keys, Type.Getter<K> keyType) {
        super(keys, SetFact.<Object>EMPTYORDER(), keyType, new Type.Getter<Object>() {
            public Type getType(Object key) {
                throw new RuntimeException("not supported");
            }
        });
    }

    public void modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, Modify type) throws SQLException, SQLHandledException {
        modifyRecord(session, keyFields, MapFact.<Object, ObjectValue>EMPTY(), type);
    }
}
