package platform.server.session;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

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

    public void writeRows(SQLSession session, Map<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        writeRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, env);
    }

    public void addRows(SQLSession session, Map<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        addRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, env);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        Map<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<P>(mapKeys, join.getExpr("value"), join.getWhere());
    }

    public <B> ClassWhere<B> getClassWhere(Map<K, ? extends B> remapKeys, B mapProp) {
        return getClassWhere("value", remapKeys, mapProp);
    }
}
