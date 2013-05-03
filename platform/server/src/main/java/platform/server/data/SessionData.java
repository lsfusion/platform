package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.Settings;
import platform.server.caches.AbstractValuesContext;
import platform.server.classes.BaseClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.*;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;

public abstract class SessionData<T extends SessionData<T>> extends AbstractValuesContext<T> {

    public ImSet<KeyField> getKeys() {
        return getOrderKeys().getSet();
    }
    public abstract ImOrderSet<KeyField> getOrderKeys();
    public abstract ImSet<PropertyField> getProperties();

    public ImRevMap<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(getKeys());
    }

    public abstract Join<PropertyField> join(final ImMap<KeyField, ? extends Expr> joinImplement);

    public abstract void drop(SQLSession session, Object owner) throws SQLException;
    public abstract void rollDrop(SQLSession session, Object owner) throws SQLException;

    public abstract boolean used(IQuery<?, ?> query);

    public abstract SessionData modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, Object owner) throws SQLException;


    public abstract void out(SQLSession session) throws SQLException;

    public abstract ClassWhere<KeyField> getClassWhere();
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);
    
    public abstract SessionData fixKeyClasses(ClassWhere<KeyField> fixClasses);
    public abstract SessionData updateCurrentClasses(DataSession session) throws SQLException;

    public abstract boolean isEmpty();
    
    private interface ResultSingleValues<R> {
        
        R empty();
        R singleRow(ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propValues) throws SQLException;
    }
    private static <R> R readSingleValues(SQLSession session, BaseClass baseClass, QueryEnvironment env, IQuery<KeyField, PropertyField> query, Result<IQuery<KeyField, PropertyField>> pullQuery, Result<ImMap<KeyField, DataObject>> keyValues, Result<ImMap<PropertyField, ObjectValue>> propValues, ResultSingleValues<R> resultRead) throws SQLException {
        IQuery.PullValues<KeyField, PropertyField> pullValues = query.pullValues();
        query = pullValues.query;
        ImMap<KeyField, Expr> keyExprValues = pullValues.pullKeys;
        ImMap<PropertyField, Expr> propExprValues = pullValues.pullProps;

        if(query.isEmpty()) // оптимизация
            return resultRead.empty();

        assert ((AbstractWhere)query.getWhere()).isValue() == query.getMapKeys().isEmpty();
        boolean singleQuery = query.getMapKeys().isEmpty();

        ImMap<Object, Expr> readValues = MapFact.<Object, Expr>addExcl(keyExprValues, propExprValues);
        Object whereObject = null;
        if(singleQuery) { // where
            whereObject = new Object();
            readValues = readValues.addExcl(whereObject, ValueExpr.get(query.getWhere()));
        }

        final ImMap<Object, ObjectValue> readedValues = Expr.readValues(session, baseClass, readValues, env);

        ImMap<KeyField, ObjectValue> keyReadValues = readedValues.filterIncl(keyExprValues.keys());
        for(ObjectValue keyValue : keyReadValues.valueIt()) // keys
            if(!(keyValue instanceof DataObject))
                return resultRead.empty(); 
        keyValues.set(BaseUtils.<ImMap<KeyField, DataObject>>immutableCast(keyReadValues));
        propValues.set(readedValues.filterIncl(propExprValues.keys()));
        if(singleQuery) { // where
            ObjectValue whereValue = readedValues.get(whereObject);
            if(whereValue.isNull())
                return resultRead.empty();
            else
                return resultRead.singleRow(keyValues.result, propValues.result);
        }

        pullQuery.set(query);
        return null;
    }

    private static SessionData write(final SQLSession session, final ImOrderSet<KeyField> keys, final ImSet<PropertyField> properties, IQuery<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, Object owner) throws SQLException {

        assert properties.equals(query.getProperties());

        Result<ImMap<KeyField, DataObject>> keyValues = new Result<ImMap<KeyField, DataObject>>();
        Result<ImMap<PropertyField, ObjectValue>> propValues = new Result<ImMap<PropertyField, ObjectValue>>();

        if(!Settings.get().isDisableReadSingleValues()) {
            Result<IQuery<KeyField, PropertyField>> pullQuery = new Result<IQuery<KeyField, PropertyField>>();
            SessionRows singleResult = readSingleValues(session, baseClass, env, query, pullQuery, keyValues, propValues, new ResultSingleValues<SessionRows>() {
                public SessionRows empty() {
                    return new SessionRows(keys, properties);
                }

                public SessionRows singleRow(ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propValues) {
                    return new SessionRows(keys, properties, MapFact.singleton(keyValues, propValues));
                }
            });
            if(singleResult!=null)
                return singleResult;
            query = pullQuery.result;
        }

        final IQuery<KeyField, PropertyField> insertQuery = query;
        SessionTable table = session.createTemporaryTable(keys.filterOrderIncl(query.getMapKeys().keys()), query.getProperties(), null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                return session.insertSessionSelect(name, insertQuery, env);
            }
        }, getQueryClasses(query), owner);
        // нужно прочитать то что записано
        if(table.count > SessionRows.MAX_ROWS) {
            if(!Settings.get().isDisableReadSingleValues()) { // чтение singleValues
                Result<ImMap<KeyField, Object>> actualKeyValues = new Result<ImMap<KeyField, Object>>();
                Result<ImMap<PropertyField, Object>> actualPropValues = new Result<ImMap<PropertyField, Object>>();
                session.readSingleValues(table, actualKeyValues, actualPropValues);
                keyValues.set(baseClass.getDataObjects(session, actualKeyValues.result, table.classes.getCommonClasses(actualKeyValues.result.keys())).addExcl(keyValues.result));
                final ImMap<PropertyField,ClassWhere<Field>> fPropertyClasses = table.propertyClasses;
                propValues.set(baseClass.getObjectValues(session, actualPropValues.result, actualPropValues.result.mapKeyValues(new GetValue<AndClassSet, PropertyField>() {
                    public AndClassSet getMapValue(PropertyField value) {
                        return fPropertyClasses.get(value).getCommonClass(value);
                    }})).addExcl(propValues.result));
                table = table.removeFields(session, actualKeyValues.result.keys(), actualPropValues.result.keys(), owner);
            }

            return new SessionDataTable(table, keys, keyValues.result, propValues.result);
        } else {
            ImOrderMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> readRows = (table.count == 0 ? MapFact.<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>EMPTYORDER() : table.read(session, baseClass));

            table.drop(session, owner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionRows sessionRows = new SessionRows(keys, properties);
            for (int i=0,size=readRows.size();i<size;i++)
                sessionRows = (SessionRows) sessionRows.modifyRecord(session, readRows.getKey(i).addExcl(keyValues.result), readRows.getValue(i).addExcl(propValues.result), Modify.ADD, owner);
            return sessionRows;
        }
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getQueryClasses(final IQuery<KeyField, PropertyField> pullQuery) {
        // читаем классы не считывая данные
        ImMap<PropertyField,ClassWhere<Field>> propertyClasses = pullQuery.getProperties().mapValues(new GetValue<ClassWhere<Field>, PropertyField>() {
            public ClassWhere<Field> getMapValue(PropertyField value) {
                return pullQuery.<Field>getClassWhere(SetFact.singleton(value));
            }});
        ClassWhere<KeyField> classes = pullQuery.<KeyField>getClassWhere(SetFact.<PropertyField>EMPTY());
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes, propertyClasses);
    }

    public SessionData rewrite(SQLSession session, IQuery<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        boolean used = used(query);
        if(!used)
            drop(session, owner);

        SessionData result = write(session, getOrderKeys(), getProperties(), query, baseClass, env, owner);

        if(used)
            drop(session, owner);
        return result;
    }

    public SessionData modifyRows(final SQLSession session, final IQuery<KeyField, PropertyField> query, BaseClass baseClass, final Modify type, QueryEnvironment env, final Object owner) throws SQLException {
        if(!Settings.get().isDisableReadSingleValues()) {
            SessionData singleResult = readSingleValues(session, baseClass, env, query, new Result<IQuery<KeyField, PropertyField>>(), new Result<ImMap<KeyField, DataObject>>(),
                    new Result<ImMap<PropertyField, ObjectValue>>(), new ResultSingleValues<SessionData>() {
                public SessionData empty() {
                    return SessionData.this;
                }

                public SessionData singleRow(ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propValues) throws SQLException {
                    return modifyRecord(session, keyValues, propValues, type, owner);
                }
            });
            if(singleResult!=null)
                return singleResult;
        }

        QueryBuilder<KeyField, PropertyField> modifyQuery = new QueryBuilder<KeyField, PropertyField>(query.getMapKeys());
        final Join<PropertyField> prevJoin = join(modifyQuery.getMapExprs());

        final Where prevWhere = prevJoin.getWhere();
        final Where newWhere = query.getWhere();
        modifyQuery.and(type==Modify.DELETE ? prevWhere.and(newWhere.not()) : (type == Modify.UPDATE ? prevWhere : prevWhere.or(newWhere)));
        modifyQuery.addProperties(getProperties().mapValues(new GetValue<Expr, PropertyField>() {
            public Expr getMapValue(PropertyField value) {
                Expr prevExpr = prevJoin.getExpr(value);
                if(type == Modify.DELETE || !query.getProperties().contains(value))
                    return prevExpr;
                Expr newExpr = query.getExpr(value);
                return (type == Modify.MODIFY || type == Modify.UPDATE ?
                        newExpr.ifElse(newWhere, prevExpr) : prevExpr.ifElse(prevWhere, newExpr));
            }}));
        return rewrite(session, modifyQuery.getQuery(), baseClass, env, owner);
    }
    public abstract SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, Pair<Integer, Integer>[] shifts) throws SQLException;

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> writeRows, Object owner) throws SQLException {
        drop(session, owner);

        if(writeRows.size()> SessionRows.MAX_ROWS)
            return new SessionDataTable(session, getOrderKeys(), getProperties(), writeRows, owner);
        else
            return new SessionRows(getOrderKeys(), getProperties(), writeRows);
    }

    protected abstract class SessionJoin extends AbstractJoin<PropertyField> {

        protected final ImMap<KeyField, ? extends Expr> joinImplement;
        protected SessionJoin(ImMap<KeyField, ? extends Expr> joinImplement) {
            this.joinImplement = joinImplement;
        }

        public ImSet<PropertyField> getProperties() {
            return SessionData.this.getProperties();
        }

        public Join<PropertyField> translateRemoveValues(MapValuesTranslate translate) {
            return SessionData.this.translateRemoveValues(translate).join(translate.mapKeys().translate(joinImplement));
        }
    }
    
    public abstract int getCount();
}
