package platform.server.session;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.Modify;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

import static platform.base.BaseUtils.hashEquals;

public class PropertyChange<T extends PropertyInterface> extends AbstractInnerContext<PropertyChange<T>> {
    private final ImMap<T, DataObject> mapValues; // для оптимизации в общем-то, важно чтобы проходили через ветку execute

    private final ImRevMap<T,KeyExpr> mapKeys;
    public final Expr expr;
    public final Where where;

    public static <T extends PropertyInterface> ImMap<T, Expr> getMapExprs(ImRevMap<T, KeyExpr> mapKeys, ImMap<T, DataObject> mapValues) {
        return getMapExprs(mapKeys, mapValues, Where.TRUE);
    }

    public static <T extends PropertyInterface> ImMap<T, Expr> getMapExprs(ImRevMap<T, KeyExpr> mapKeys, ImMap<T, DataObject> mapValues, Where where) {
        final ImMap<BaseExpr, BaseExpr> exprValues = where.getExprValues();
        return DataObject.getMapExprs(mapValues).addExcl(mapKeys.mapValues(new GetValue<BaseExpr, KeyExpr>() {
            public BaseExpr getMapValue(KeyExpr value) {
                BaseExpr exprValue = exprValues.get(value);
                return exprValue != null ? exprValue : value;
            }
        }));
    }

    public static <T> ImRevMap<T, KeyExpr> getFullMapKeys(ImRevMap<T, KeyExpr> mapKeys, ImMap<T, DataObject> mapValues) {
        assert mapKeys.keys().disjoint(mapValues.keys());
        return mapKeys.addRevExcl(KeyExpr.getMapKeys(mapValues.keys()));
    }

    public static <C> ImMap<C, ? extends Expr> simplifyExprs(ImMap<C, ? extends Expr> implementExprs, Where andWhere) {
        KeyEquals keyEquals = andWhere.getKeyEquals(); // оптимизация
        KeyEqual keyEqual;
        if(keyEquals.size() == 1 && !(keyEqual=keyEquals.getKey(0)).isEmpty())
            implementExprs = keyEqual.getTranslator().translate(implementExprs);
        return implementExprs;
    }

    public ImMap<T, Expr> getMapExprs() {
        return getMapExprs(mapKeys, mapValues, where);
    }

    public ImMap<T, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public ImMap<T, DataObject> getMapValues() {
        return mapValues;
    }

    public PropertyChange(Expr expr, ImMap<T, DataObject> mapValues) {
        this(mapValues, MapFact.<T, KeyExpr>EMPTYREV(), expr, Where.TRUE);
    }

    public PropertyChange(ObjectValue value) {
        this(value.getExpr(), MapFact.<T, DataObject>EMPTY());
    }

    public PropertyChange(ObjectValue value, T propInterface, DataObject intValue) {
        this(value.getExpr(), MapFact.singleton(propInterface, intValue));
    }

    public PropertyChange(ImMap<T, DataObject> mapValues, ImRevMap<T, KeyExpr> mapKeys, Expr expr, Where where) {
        this.mapValues = mapValues;
        this.mapKeys = mapKeys;
        this.expr = expr;
        this.where = where;
    }

    public PropertyChange(PropertyChange<T> change, Expr expr) {
        this(change.mapValues, change.mapKeys, expr, change.where);
    }

    public PropertyChange(PropertyChange<T> change, Expr expr, Where where) {
        this(change.mapValues, change.mapKeys, expr, where);
    }

    public PropertyChange(ImRevMap<T, KeyExpr> mapKeys, Expr expr, Where where) {
        this(MapFact.<T, DataObject>EMPTYREV(), mapKeys, expr, where);
    }

    private final static PropertyChange TRUE = new PropertyChange(MapFact.EMPTYREV(), ValueExpr.TRUE, Where.TRUE);
    private final static PropertyChange FALSE = new PropertyChange(MapFact.EMPTYREV(), ValueExpr.TRUE, Where.FALSE);
    public static <P extends PropertyInterface> PropertyChange<P> STATIC(boolean isTrue) {
        return isTrue ? TRUE : FALSE;
    }
    public PropertyChange(ImRevMap<T, KeyExpr> mapKeys, Expr expr) {
        this(mapKeys, expr, expr.getWhere());
    }

    public PropertyChange(ImRevMap<T, KeyExpr> mapKeys, Where where) {
        this(mapKeys, Expr.NULL, where);
    }

    public ImSet<KeyExpr> getKeys() {
        return mapKeys.valuesSet();
    }

    public ImSet<Value> getValues() {
        return expr.getOuterValues().merge(where.getOuterValues()).merge(AbstractOuterContext.getOuterValues(DataObject.getMapExprs(mapValues).values()));
    }

    public PropertyChange<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertyChange<T>(mapValues, mapKeys, expr, where.and(andWhere));
    }

    public <P extends PropertyInterface> PropertyChange<P> map(ImRevMap<P,T> mapping) {
        return new PropertyChange<P>(mapping.rightJoin(mapValues), mapping.rightJoin(mapKeys),expr,where);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }

    public PropertyChange<T> add(PropertyChange<T> change) {
        if(isEmpty())
            return change;
        if(change.isEmpty())
            return this;
        if(equals(change))
            return this;

        if(mapValues.isEmpty()) {
            // assert что addJoin.getWhere() не пересекается с where, в общем случае что по пересекаемым они совпадают
            Join<String> addJoin = change.join(mapKeys);
            return new PropertyChange<T>(mapKeys, expr.ifElse(where, addJoin.getExpr("value")), where.or(addJoin.getWhere()));
        } else {
            ImRevMap<T, KeyExpr> addKeys = getFullMapKeys(mapKeys, mapValues); // тут по хорошему надо искать общие и потом частично join'ить
            
            Join<String> thisJoin = join(addKeys);
            Join<String> addJoin = change.join(addKeys);

            Where thisWhere = thisJoin.getWhere();
            return new PropertyChange<T>(addKeys, thisJoin.getExpr("value").ifElse(thisWhere, addJoin.getExpr("value")), thisWhere.or(addJoin.getWhere()));
        }
    }
    
    public final static AddValue<Object, PropertyChange<PropertyInterface>> addValue = new SimpleAddValue<Object, PropertyChange<PropertyInterface>>() {
        public PropertyChange<PropertyInterface> addValue(Object key, PropertyChange<PropertyInterface> prevValue, PropertyChange<PropertyInterface> newValue) {
            return prevValue.add(newValue);
        }

        public boolean symmetric() {
            return false;
        }
    };
    public static <M, K extends PropertyInterface> AddValue<M, PropertyChange<K>> addValue() {
        return BaseUtils.immutableCast(addValue);
    }

    public Where getWhere(ImMap<T, ? extends Expr> joinImplement) {
        return join(joinImplement).getWhere();
    }

    public Join<String> join(ImMap<T, ? extends Expr> joinImplement) {
        return getQuery().join(joinImplement);
    }

    public Pair<ImMap<T, DataObject>, ObjectValue> getSimple() {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            return new Pair<ImMap<T, DataObject>, ObjectValue>(mapValues, exprValue);
        return null;
    }

    public ImOrderMap<ImMap<T, DataObject>, ImMap<String, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            return MapFact.singletonOrder(mapValues, MapFact.<String, ObjectValue>singleton("value", exprValue));

        return getQuery().executeClasses(env);
    }

    public void modifyRows(SinglePropertyTableUsage<T> table, SQLSession session, BaseClass baseClass, Modify type, QueryEnvironment queryEnv) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            table.modifyRecord(session, mapValues, exprValue, type);
        else
            table.modifyRows(session, getQuery(), baseClass, type, queryEnv);
    }

    public void writeRows(SinglePropertyTableUsage<T> table, SQLSession session, BaseClass baseClass, QueryEnvironment queryEnv) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            table.writeRows(session, MapFact.singleton(mapValues, MapFact.singleton("value", exprValue)));
        else
            table.writeRows(session, getQuery(), baseClass, queryEnv);
    }

    @IdentityLazy
    public IQuery<T,String> getQuery() { // важно потому как aspect может на IQuery изменить
        return new Query<T, String>(getFullMapKeys(mapKeys, mapValues), where, mapValues, MapFact.singleton("value", expr)); // через query для кэша
   }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return 31 * (where.hashOuter(hashContext)*31*31 + expr.hashOuter(hashContext)*31 + AbstractOuterContext.hashOuter(mapKeys, hashContext)) +
                AbstractOuterContext.hashOuter(DataObject.getMapExprs(mapValues), hashContext);
    }

    public boolean equalsInner(PropertyChange<T> object) {
        return hashEquals(where, object.where) && hashEquals(expr, object.expr) && hashEquals(mapKeys, object.mapKeys) && hashEquals(mapValues, object.mapValues);
    }

    protected PropertyChange<T> translate(MapTranslate translator) {
        return new PropertyChange<T>(translator.translateDataObjects(mapValues), translator.translateKey(mapKeys),expr.translateOuter(translator),where.translateOuter(translator));
    }

    protected long calculateComplexity(boolean outer) {
        return where.getComplexity(outer) + expr.getComplexity(outer);
    }
    @Override
    public PropertyChange<T> calculatePack() {
        Where packWhere = where.pack();
        return new PropertyChange<T>(mapValues, mapKeys, expr.followFalse(packWhere.not(), true), packWhere);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, WhereBuilder where) {
        Join<String> join = join(joinImplement);
        if(where !=null) where.add(join.getWhere());
        return join.getExpr("value");
    }

    public static <P extends PropertyInterface> PropertyChange<P> addNull(PropertyChange<P> change1, PropertyChange<P> change2) {
        if(change1==null)
            return change2;
        if(change2==null)
            return change1;
        return change1.add(change2);
    }

/*    public StatKeys<T> getStatKeys() {
        return where.getStatKeys(getInnerKeys()).mapBack(mapKeys).and(new StatKeys<T>(mapColValues.keySet(), Stat.ONE));
    }*/
}
