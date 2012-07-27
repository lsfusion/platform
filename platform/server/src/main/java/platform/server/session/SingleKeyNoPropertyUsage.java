package platform.server.session;

import platform.server.data.Insert;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.expr.Expr;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collections;
import java.util.HashMap;
import java.sql.SQLException;

public class SingleKeyNoPropertyUsage extends NoPropertyTableUsage<String> {

    public SingleKeyNoPropertyUsage(final Type keyType) {
        super(Collections.singletonList("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType;
            }
        });
    }

    public Where getWhere(Expr expr) {
        return getWhere(Collections.singletonMap("key", expr));
    }

    public void insertRecord(SQLSession session, DataObject keyObject, Insert type) throws SQLException {
        insertRecord(session, Collections.singletonMap("key", keyObject),new HashMap<Object, ObjectValue>(), type);
    }

    public void deleteRecords(SQLSession session, DataObject keyObject) throws SQLException {
        deleteRecords(session, Collections.singletonMap("key", keyObject));
    }
}
