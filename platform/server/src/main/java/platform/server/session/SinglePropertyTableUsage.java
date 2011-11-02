package platform.server.session;

import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
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

    public void addRows(SQLSession session, Map<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        addRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, env);
    }

    public Expr getExpr(Map<K, ? extends Expr> joinImplement, WhereBuilder where) {
        Join<String> join = join(joinImplement);
        where.add(join.getWhere());
        return join.getExpr("value");
    }
}
