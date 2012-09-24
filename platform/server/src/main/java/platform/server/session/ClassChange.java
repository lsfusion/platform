package platform.server.session;

import platform.base.ImmutableObject;
import platform.base.TwinImmutableObject;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.Modify;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.SessionRows;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class ClassChange extends ImmutableObject {
    
    public final DataObject keyValue;
    public final ObjectValue propValue;

    public final KeyExpr key;
    public final Where where;
    public final Expr expr;

    public ClassChange(DataObject keyValue, ConcreteObjectClass cls) {
        this.keyValue = keyValue;
        this.propValue = cls.getClassObject();

        this.key = null;
        this.where = null;
        this.expr = null;
    }

    public ClassChange(KeyExpr key, Where where, ConcreteObjectClass cls) {
        this(key, where, cls.getClassObject().getExpr());
    }
    
    public ClassChange(KeyExpr key, Where where, Expr expr) {
        this.key = key;
        this.where = where;
        this.expr = expr;

        this.keyValue = null;
        this.propValue = null;
    }

    public void modifyRows(SingleKeyPropertyUsage table, SQLSession session, BaseClass baseClass, Modify type, QueryEnvironment queryEnv) throws SQLException {
        if(keyValue !=null)
            table.modifyRecord(session, keyValue, propValue, type);
        else
            table.modifyRows(session, getQuery(), baseClass, type, queryEnv);
    }

    @IdentityLazy
    public Query<String, String> getQuery() {
        if(keyValue != null)
            return new Query<String, String>(Collections.singletonMap("key", new KeyExpr("key")), Where.TRUE, Collections.singletonMap("key", keyValue), Collections.singletonMap("value", propValue.getExpr()));
        else
            return new Query<String, String>(Collections.singletonMap("key", key), expr, "value", where);
    }

    public Join<String> join(Expr expr) {
        return getQuery().join(Collections.singletonMap("key", expr));
    }

    public ClassChange materialize(SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        SingleKeyPropertyUsage changedClasses = new SingleKeyPropertyUsage(ObjectType.instance, SystemClass.instance);
        changedClasses.writeRows(sql, getQuery(), baseClass, env);
        return changedClasses.getChange();
    }
    
    public boolean isEmpty() {
        return where != null && where.isFalse();
    }
}
