package platform.server.session;

import platform.base.OrderedMap;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PropertySet<T extends PropertyInterface> {
    public final Map<T, DataObject> mapValues; // для оптимизации в общем-то, важно чтобы проходили через ветку execute

    public final Map<T,KeyExpr> mapKeys;
    public final Where where;

    public Map<T, Expr> getMapExprs() {
        return PropertyChange.getMapExprs(mapKeys, mapValues, where);
    }

    public PropertySet(Map<T, DataObject> mapValues, Map<T, KeyExpr> mapKeys, Where where) {
        this.mapValues = mapValues;
        this.mapKeys = mapKeys;
        this.where = where;
    }

    public Collection<Map<T, DataObject>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException {
        if(mapKeys.isEmpty() && where.isTrue()) // оптимизация для нее в том числе mapValues ведется                     
            return Collections.singleton(mapValues);
            
        return getQuery().executeClasses(session, env, baseClass).keySet();
    }

    public PropertySet<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertySet<T>(mapValues, mapKeys, where.and(andWhere));
    }

    @IdentityLazy
    public Query<T,String> getQuery() {
        return new Query<T, String>(PropertyChange.getFullMapKeys(mapKeys, mapValues), where, mapValues);
    }
}
