package platform.server.session;

import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.List;
import java.util.Collections;
import java.sql.SQLException;

public class SingleKeyPropertyUsage extends SinglePropertyTableUsage<String> {

    public SingleKeyPropertyUsage(final Type keyType, Type propertyType) {
        super(Collections.singletonList("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType; 
            }
        }, propertyType);
    }
        
    public void insertRecord(SQLSession session, DataObject keyObject, ObjectValue propertyObject, boolean update) throws SQLException {
        insertRecord(session, Collections.singletonMap("key", keyObject),Collections.singletonMap("value", propertyObject), update);
    }

    public Join<String> join(Expr expr) {
        return join(Collections.singletonMap("key", expr));
    }
}
