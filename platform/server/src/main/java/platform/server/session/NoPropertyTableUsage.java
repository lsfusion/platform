package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.data.Modify;
import platform.server.data.SQLSession;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;

public class NoPropertyTableUsage<K> extends SessionTableUsage<K,Object> {

    public NoPropertyTableUsage(ImOrderSet<K> keys, Type.Getter<K> keyType) {
        super(keys, SetFact.<Object>EMPTYORDER(), keyType, new Type.Getter<Object>() {
            public Type getType(Object key) {
                throw new RuntimeException("not supported");
            }
        });
    }

    public void modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, Modify type) throws SQLException {
        modifyRecord(session, keyFields, MapFact.<Object, ObjectValue>EMPTY(), type);
    }
}
