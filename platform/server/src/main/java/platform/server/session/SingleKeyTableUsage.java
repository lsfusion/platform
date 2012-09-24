package platform.server.session;

import platform.server.data.Modify;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
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

    public Join<P> join(Expr key) {
        return join(Collections.singletonMap("key", key));
    }

    public void modifyRecord(SQLSession session, DataObject key, Map<P, ObjectValue> propertyValues, Modify type) throws SQLException {
        modifyRecord(session, Collections.singletonMap("key", key), propertyValues, type);
    }

}
