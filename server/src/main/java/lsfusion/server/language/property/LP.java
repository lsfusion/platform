package lsfusion.server.language.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.action.session.table.SinglePropertyTableUsage;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.readCalcImplements;

public class LP<T extends PropertyInterface> extends LAP<T, Property<T>> {

    public Property<T> property;

    public Property<T> getActionOrProperty() {
        return property;
    }

    public LP(Property<T> property) {
        super(property);
        this.property = property;
    }

    public LP(Property<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
        this.property = property;
    }

    public Object read(ExecutionEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(env, getMapValues(objects));
    }

    public ImMap<ImList<Object>, Object> readAll(ExecutionContext context) throws SQLException, SQLHandledException {
        return readAll(context.getEnv());
    }
    public ImMap<ImList<Object>, Object> readAll(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return property.readAll(env).mapKeys(value -> listInterfaces.mapList(value));
    }
    public ImMap<ImList<DataObject>, DataObject> readAllClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return property.readAllClasses(env).mapKeys(value -> listInterfaces.mapList(value));
    }

    public Object read(DataSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(session, getMapValues(objects), modifier, env);
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(session, getMapValues(objects), modifier, env);
    }

    public Object read(ExecutionContext context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return read(context.getSession(), context.getModifier(), context.getQueryEnv(), objects);
    }

    public Object read(DataSession session, ObjectValue... objects) throws SQLException, SQLHandledException {
        return read(session, session.getModifier(), session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        ImMap<T, ObjectValue> mapValues = getMapValues(objects);
        return property.readClasses(session, mapValues, modifier, env);
    }

    public ObjectValue readClasses(ExecutionContext context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(context.getEnv(), objects);
    }

    public ObjectValue readClasses(ExecutionEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(env.getSession(), env.getModifier(), env.getQueryEnv(), objects);
    }

    public ObjectValue readClasses(DataSession session, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(session, session.getModifier(), session.env, objects);
    }

    // execute'ы без Form'
    public void change(Object value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, (ExecutionEnvironment)session, objects);
    }
    // execute'ы из контекста
    public void change(Object value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
    }

    // с явными типами
    public void change(Integer value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Integer value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Long value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Long value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Boolean value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, false, session, objects);
    }
    public void change(Boolean value, boolean threeState, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, threeState, (ExecutionEnvironment) session, objects);
    }
    public void change(Boolean value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, false, context, objects);
    }
    public void change(Boolean value, boolean threeState, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, threeState, context.getEnv(), objects);
    }
    public void change(LocalDate value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(LocalDate value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(LocalDateTime value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(LocalDateTime value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Instant value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(BigDecimal value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(BigDecimal value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(RawFileData value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(RawFileData value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(FileData value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(FileData value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(String value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(String value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(java.sql.Time value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(java.sql.Time value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }

    // ObjectValue
    public void change(ObjectValue value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, (ExecutionEnvironment) session, objects);
    }
    public void change(ObjectValue value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
    }
    public void change(ObjectValue value, ExecutionEnvironment env, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(Boolean value, boolean threeState, ExecutionEnvironment env, DataObject... objects) throws SQLException, SQLHandledException {
        // change false to null
        if (!threeState && value != null && !value) {
            value = null;
        }
        change(value, env, objects);
    }
    public void change(Object value, ExecutionEnvironment env, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(ObjectValue value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException, SQLHandledException {
        property.change(keys, env, value);
    }

    public void change(Object value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException, SQLHandledException {
        property.change(keys, env, value);
    }

    public <K, V> void change(ExecutionContext context, ImMap<K, V> data) throws SQLException, SQLHandledException {
        change(context.getSession(), context.getEnv(), data);
    }

    public interface GetLPValue<K, V> {
        Object get(K key, V value, LP lp);
    }

    public static <K, V> void change(ExecutionContext context, final ImOrderSet<LP> props, ImMap<K, V> data, ConcreteClass keyClass, GetLPValue<K, V> mapper) throws SQLException, SQLHandledException {
        change(context.getSession(), context.getEnv(), props, data, keyClass, mapper);
    }

    // actually it is an "optimization" of the change for a set of properties
    public <K, V> void change(DataSession session, ExecutionEnvironment env, ImMap<K, V> data) throws SQLException, SQLHandledException {

        T propertyKey = listInterfaces.single();
        ValueClass propertyKeyClass = property.getInterfaceClasses(ClassType.editValuePolicy).get(propertyKey);
        ValueClass propertyValueClass = property.getValueClass(ClassType.editValuePolicy);

        SingleKeyPropertyUsage table = new SingleKeyPropertyUsage("updpm:sp", propertyKeyClass.getType(), propertyValueClass.getType());

        table.writeRows(session.sql, session.getOwner(), data.<DataObject, ObjectValue, SQLException, SQLHandledException>mapKeyValuesEx(
                key -> session.getDataObject(propertyKeyClass, key), value -> session.getObjectValue(propertyValueClass, value)));

        try {
            env.change(property, SingleKeyPropertyUsage.getChange(table, propertyKey));
        } finally {
            table.drop(session.sql, session.getOwner());
        }
    }

    public <V> void changeList(DataSession session, ExecutionEnvironment env, ImMap<ImList<Object>, V> params) throws SQLException, SQLHandledException {

        ImMap<T, ValueClass> keyClasses = property.getInterfaceClasses(ClassType.editValuePolicy);
        ValueClass propertyValueClass = property.getValueClass(ClassType.editValuePolicy);

        SinglePropertyTableUsage<T> table = new SinglePropertyTableUsage<>("updpm:sp", listInterfaces, key -> keyClasses.get(key).getType(), propertyValueClass.getType());

        table.writeRows(params.<ImMap<T, DataObject>, ObjectValue, SQLException, SQLHandledException>mapKeyValuesEx(
                keys -> listInterfaces.<DataObject, SQLException, SQLHandledException>mapOrderValuesEx((index, key) -> session.getDataObject(keyClasses.get(key), keys.get(index))),
                value -> session.getObjectValue(propertyValueClass, value)), session.sql, session.getOwner());
        try {
            env.change(property, SinglePropertyTableUsage.getChange(table));
        } finally {
            table.drop(session.sql, session.getOwner());
        }
    }

    public static <K, V> void change(DataSession session, ExecutionEnvironment env, final ImOrderSet<LP> props, ImMap<K, V> data, ConcreteClass keyClass, GetLPValue<K, V> mapper) throws SQLException, SQLHandledException {

        ValueClass propertyKeyClass = props.get(0).getInterfaceClasses(ClassType.editValuePolicy)[0];
        ImOrderMap<LP, ValueClass> propertyValueClasses = props.mapOrderValues((LP lp) -> ((LP<?>) lp).property.getValueClass(ClassType.editValuePolicy));

        SingleKeyTableUsage<LP> importTable = new SingleKeyTableUsage<>("updpm:wr", keyClass.getType(), props, key -> propertyValueClasses.get(key).getType());

        importTable.writeRows(session.sql, data.<ImMap<String, DataObject>, ImMap<LP, ObjectValue>, SQLException, SQLHandledException>mapKeyValuesEx(
                key -> MapFact.singleton("key", session.getDataObject(propertyKeyClass, key)), (key, value) -> props.getSet().<ObjectValue, SQLException, SQLHandledException>mapValuesEx((LP lp) -> session.getObjectValue(propertyValueClasses.get(lp), mapper.get(key, value, lp)))), session.getOwner());

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LP lp : props)
                env.change(lp.property, new PropertyChange(MapFact.singletonRev(lp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lp), where));
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    public static ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> readAll(LP[] lps, ExecutionEnvironment env) throws SQLException, SQLHandledException {
        ImOrderSet<KeyExpr> keySet = KeyExpr.getMapKeys(lps[0].listInterfaces.size());
        KeyExpr[] keyArray = keySet.toArray(new KeyExpr[keySet.size()]);
        QueryBuilder<Integer, Integer> readQuery = new QueryBuilder<>(keySet.toIndexedMap());
        Where where = Where.FALSE();
        for (int i = 0; i < lps.length; i++) {
            Expr expr = lps[i].getExpr(env.getModifier(), keyArray);
            readQuery.addProperty(i, expr);
            where = where.or(expr.getWhere());
        }
        readQuery.and(where);
        return readQuery.execute(env);

    }
    public <K> ImMap<String, Object> readAll(DataSession session, K[] data) throws SQLException, SQLHandledException {
        // create a temporary table with one key (STRING type), without fields
        ConcreteClass interfaceClass = (ConcreteClass)getInterfaceClasses(ClassType.iteratePolicy)[0];
        SingleKeyTableUsage<Object> importTable = new SingleKeyTableUsage<>("updpm:wr", interfaceClass.getType(), SetFact.EMPTYORDER(), key -> null);
        try {
            // write the values from the array
            importTable.writeRows(session.sql, SetFact.toExclSet(data).mapKeyValues(value -> MapFact.singleton("key", new DataObject(value, interfaceClass)), value -> MapFact.EMPTY()), session.getOwner());
            // create a query
            // with one key named key (however, it can be any name): property parameter and condition: in the created temporary table
            KeyExpr keyExpr = new KeyExpr(0);
            ImRevMap<String, KeyExpr> mapKeys = MapFact.singletonRev("key", keyExpr);
            Join<Object> importJoin = importTable.join(mapKeys);
            QueryBuilder<String, String> query = new QueryBuilder<>(mapKeys, importJoin.getWhere());
            // with one field value (however, any name is possible): the passed property
            query.addProperty("value", getExpr(keyExpr));
            // execute, re-mapping back to get map: property parameter -> value
            return query.execute(session).keys().mapKeyValues(key -> (String)key.singleValue(), value -> value.singleValue());
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    public void makeUserLoggable(BaseLogicsModule LM, SystemEventsLogicsModule systemEventsLM, DBNamingPolicy namingPolicy) {
        // assert that all LP/LA has the "same" listInterfaces
        LA<?> logFormAction = LM.addLFAProp(this, systemEventsLM, namingPolicy);
        property.setLogFormAction(logFormAction.getImplement(listInterfaces));
    }

    public <D extends PropertyInterface> void setWhenChange(LogicsModule lm, Event actionEvent, Object... params) {
        ImList<PropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setWhenChange(lm, actionEvent, listImplements.get(0), (PropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
    }

    public void addOperand(boolean hasWhen, List<ResolveClassSet> signature, Version version, Object... params) {
        ImList<PropertyInterfaceImplement<T>> readImplements = readCalcImplements(listInterfaces, params);
        PropertyInterfaceImplement<UnionProperty.Interface> operand = (PropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(0);
        if(hasWhen)
            ((CaseUnionProperty)property).addCase((PropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(1), operand, version);
        else {
//            if(((CaseUnionProperty) property).getAbstractType() == CaseUnionProperty.Type.MULTI) {
//                AbstractCase.cntexpl = AbstractCase.cntexpl + 1;
//                if(operand instanceof PropertyMapImplement) {
//                    if(BaseUtils.nullEquals(((PropertyMapImplement)operand).property.getName(), property.getName()))
//                        AbstractCase.cntexplname = AbstractCase.cntexplname + 1;
//                }
//            }
            ((CaseUnionProperty) property).addOperand((PropertyMapImplement<?, UnionProperty.Interface>) operand, signature, version);
        }
    }

    public ImRevMap<T, KeyExpr> getMapKeys() {
        return property.getMapKeys();
    }

    public Expr getExpr(Modifier modifier, final Expr... exprs) throws SQLException, SQLHandledException {
        return property.getExpr(getMap(exprs),modifier);
    }

    public Expr getExpr(final Expr... exprs) {
        return property.getExpr(getMap(exprs));
    }

    public <U> PropertyImplement<T, U> getMapping(U... mapping) {
        return new PropertyImplement<>(property, getMap(mapping));
    }
    public <U extends PropertyInterface> PropertyMapImplement<T, U> getImplement(U... mapping) {
        return new PropertyMapImplement<>(property, getRevMap(mapping));
    }
    public <U extends PropertyInterface> PropertyMapImplement<T, U> getImplement(ImOrderSet<U> mapping) {
        return new PropertyMapImplement<>(property, getRevMap(mapping));
    }

    public PropertyChange<T> getChange(Expr expr, Where where, KeyExpr... keys) {
        return new PropertyChange<>(getRevMap(keys), expr, where);
    }

    public void setAutoset(boolean autoset) {
        assert property.interfaces.size()==1;
        property.autoset = autoset;
    }

    public LP<T> getOld(PrevScope scope) {
        return new LP<>(property.getOld(scope), listInterfaces);
    }

    public LP<T> getChanged(IncrementType type, PrevScope prevScope) {
        return new LP<>(property.getChanged(type, prevScope), listInterfaces);
    }

    public ResolveClassSet getResolveClassSet(List<ResolveClassSet> classes) {
        return property.getResolveClassSet(listInterfaces.mapList(ListFact.fromJavaList(classes)));    
    }
}
