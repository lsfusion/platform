package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
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
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SingleKeyPropertyUsage extends SinglePropertyTableUsage<String> {

    public SingleKeyPropertyUsage(final Type keyType, Type propertyType) {
        super(SetFact.singletonOrder("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType; 
            }
        }, propertyType);
    }

    public void modifyRecord(SQLSession session, DataObject keyObject, ObjectValue propertyObject, Modify type) throws SQLException {
        modifyRecord(session, MapFact.singleton("key", keyObject), MapFact.singleton("value", propertyObject), type);
    }
    
    public void writeRows(SQLSession session, KeyExpr key, Expr expr, Where where, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        writeRows(session, new Query<String, String>(MapFact.singletonRev("key", key), expr, "value", where), baseClass, env);
    }

    public Join<String> join(Expr expr) {
        return join(MapFact.singleton("key", expr));
    }

    public Where getWhere(Expr expr) {
        return getWhere(MapFact.singleton("key", expr));
    }

    public Expr getExpr(Expr expr) {
        return join(MapFact.singleton("key", expr)).getExpr("value");
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SingleKeyPropertyUsage table, P propertyInterface) {
        ImRevMap<String, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<P>(MapFact.singletonRev(propertyInterface, mapKeys.singleValue()), join.getExpr("value"), join.getWhere());
    }

    public ClassChange getChange() {
        KeyExpr key = new KeyExpr("key");
        Join<String> join = join(key);
        return new ClassChange(key, join.getWhere(), join.getExpr("value"));
    }

    public ImCol<ImMap<String, Object>> read(DataSession session, DataObject object) throws SQLException {
        return read(session, MapFact.singleton("key", object));
    }
}
