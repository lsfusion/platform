package lsfusion.server.session;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.Modify;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(ImOrderSet<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(keys, SetFact.singletonOrder("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }

    public ModifyResult modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, ObjectValue propertyValue, Modify type) throws SQLException {
        return modifyRecord(session, keyFields, MapFact.singleton("value", propertyValue), type);
    }

    public ModifyResult modifyRows(SQLSession session, ImRevMap<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, Modify type, QueryEnvironment env) throws SQLException {
        return modifyRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, type, env);
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

    public void checkClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        table = table.checkClasses(session, baseClass);
    }
}
