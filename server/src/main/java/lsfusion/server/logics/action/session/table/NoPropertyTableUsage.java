package lsfusion.server.logics.action.session.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.*;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;

import java.sql.SQLException;

public class NoPropertyTableUsage<K> extends SessionTableUsage<K,String> {

    public NoPropertyTableUsage(String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType) {
        super(debugInfo, keys, SetFact.<String>EMPTYORDER(), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                throw new RuntimeException("not supported");
            }
        });
    }

    public void modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        modifyRecord(session, keyFields, MapFact.<String, ObjectValue>EMPTY(), type, owner);
    }
}
