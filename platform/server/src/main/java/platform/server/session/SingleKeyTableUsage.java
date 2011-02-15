package platform.server.session;

import platform.server.data.SQLSession;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SingleKeyTableUsage<P> extends SessionTableUsage<String, P> {

    public SingleKeyTableUsage(final Type keyType, List<P> properties, Type.Getter<P> propertyType) {
        super(Collections.singletonList("key"), properties, new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType;
            }
        }, propertyType);
    }

    public void insertRecord(SQLSession session, DataObject key, Map<P, ObjectValue> propertyValues, boolean update) throws SQLException {
        insertRecord(session, Collections.singletonMap("key", key), propertyValues, update);
    }

}
