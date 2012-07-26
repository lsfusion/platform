package platform.server.session;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.util.Collections;
import java.sql.SQLException;
import java.util.Map;

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

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SingleKeyPropertyUsage table, P propertyInterface) {
        Map<String, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<P>(Collections.singletonMap(propertyInterface, BaseUtils.singleValue(mapKeys)), join.getExpr("value"), join.getWhere());
    }

}
