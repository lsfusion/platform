package platform.server.session;

import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.*;
import java.sql.SQLException;

public class NoPropertyTableUsage<K> extends SessionTableUsage<K,Object> {

    public NoPropertyTableUsage(List<K> keys, Type.Getter<K> keyType) {
        super(keys, new ArrayList<Object>(), keyType, new Type.Getter<Object>() {
            public Type getType(Object key) {
                throw new RuntimeException("not supported");
            }
        });
    }

    public void insertRecord(SQLSession session, Map<K, DataObject> keyFields, boolean update) throws SQLException {
        insertRecord(session, keyFields, new HashMap<Object, ObjectValue>(), update);
    }
}
