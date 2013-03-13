package platform.server.session;

import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.classes.BaseClass;
import platform.server.data.Modify;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(ImOrderSet<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(keys, SetFact.singletonOrder("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }

    public void modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, ObjectValue propertyValue, Modify type) throws SQLException {
        modifyRecord(session, keyFields, MapFact.singleton("value", propertyValue), type);
    }

    public void modifyRows(SQLSession session, ImRevMap<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, Modify type, QueryEnvironment env) throws SQLException {
        modifyRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, type, env);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        ImRevMap<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<P>(mapKeys, join.getExpr("value"), join.getWhere());
    }

    public <B> ClassWhere<B> getClassWhere(ImRevMap<K, ? extends B> remapKeys, B mapProp) {
        return getClassWhere("value", remapKeys, mapProp);
    }

    public void fixKeyClasses(ClassWhere<K> classes) {
        table = table.fixKeyClasses(classes.remap(mapKeys.reverse()));
    }

    public void updateAdded(SQLSession sql, BaseClass baseClass, Pair<Integer,Integer>[] shifts) throws SQLException {
        updateAdded(sql, baseClass, "value", shifts);
    }

    public void updateCurrentClasses(DataSession session) throws SQLException {
        table = table.updateCurrentClasses(session);
    }
}
