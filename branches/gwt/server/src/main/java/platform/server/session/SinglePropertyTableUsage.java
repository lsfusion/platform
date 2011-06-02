package platform.server.session;

import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(List<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(keys, Collections.singletonList("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }
    
    public void insertRecord(SQLSession session, Map<K, DataObject> keyFields, ObjectValue propertyValue, boolean update, boolean groupLast) throws SQLException {
        insertRecord(session, keyFields, Collections.singletonMap("value", propertyValue), update, groupLast);
    }
}
