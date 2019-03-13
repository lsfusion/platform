package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.lambda.Processor;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.caches.InnerContext;
import lsfusion.server.logics.classes.BaseClass;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.AbstractJoin;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.RegisterClassRemove;
import lsfusion.server.session.UpdateCurrentClassesSession;

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

    public abstract void drop(SQLSession session, TableOwner owner, OperationOwner opOwner) throws SQLException;
    public abstract void rollDrop(SQLSession session, TableOwner owner, OperationOwner opOwner, boolean assertNotExists) throws SQLException;

    public abstract boolean used(InnerContext query);

    public abstract SessionData modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, TableOwner owner, OperationOwner opOwner, Result<Boolean> changed) throws SQLException, SQLHandledException;


    public abstract void out(SQLSession session) throws SQLException, SQLHandledException;
    public abstract void outClasses(SQLSession session, BaseClass baseClass, Processor<String> processor) throws SQLException, SQLHandledException;

    public abstract ClassWhere<KeyField> getClassWhere();
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);
    
    public abstract SessionData fixKeyClasses(ClassWhere<KeyField> fixClasses, PropertyField valueField);
    public abstract boolean hasClassChanges(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;
    public abstract SessionData updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;

    public abstract boolean isEmpty();
    
    private interface ResultSingleValues<R> {
        
        R empty();
        R singleRow(ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propValues) throws SQLException, SQLHandledException;
    }
    private static <R> R readSingleValues(SQLSession session, BaseClass baseClass, QueryEnvironment env, IQuery<KeyField, PropertyField> query, Result<IQuery<KeyField, PropertyField>> pullQuery, Result<ImMap<KeyField, DataObject>> keyValues, Result<ImMap<PropertyField, ObjectValue>> propValues, ResultSingleValues<R> resultRead) throws SQLException, SQLHandledException {
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

        final ImMap<Object, ObjectValue> readedValues = Expr.readObjectValues(session, baseClass, readValues, env);

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
                return resultRead.singleRow(castTypes(keyValues.result), castTypes(propValues.result));
        }

        pullQuery.set(query);
        return null;
    }

    // есть оптимизации вроде table instanceof SessionRows, writeRows.size() <= SESSION_ROWS, то есть полагаемся на инвариант что при передаче в JDBC СУБД само закастит (этот инвариант все равно будет, потому как когда идет insertSessionSelect там никаких Cast'ов нет и мы опять таки полагаемся на cast СУБД)
    public static <F extends Field, D extends ObjectValue> ImMap<F, D> castTypes(ImMap<F, D> values) {
        return values.mapValues(new GetKeyValue<D, F, D>() {
            @Override
            public D getMapValue(F key, D value) {
                if(!(key.type instanceof DataClass && value instanceof DataObject))
                    return value;

                DataClass dataClass = (DataClass) key.type;
                DataObject dataObject = (DataObject)value;
                if(BaseUtils.hashEquals(dataClass, dataObject.objectClass))
                    return value;

                return (D) new DataObject(dataClass.readCast(dataObject.object, dataObject.objectClass), dataClass);
            }
        });
    }

    private static SessionData write(final SQLSession session, final ImOrderSet<KeyField> keys, final ImSet<PropertyField> properties, IQuery<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, final TableOwner owner, boolean updateClasses, final int selectTop) throws SQLException, SQLHandledException {

        assert properties.equals(query.getProperties());

        Result<ImMap<KeyField, DataObject>> keyValues = new Result<>();
        Result<ImMap<PropertyField, ObjectValue>> propValues = new Result<>();

        if(!Settings.get().isDisableReadSingleValues()) {
            Result<IQuery<KeyField, PropertyField>> pullQuery = new Result<>();
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

        final OperationOwner opOwner = env.getOpOwner();

        final IQuery<KeyField, PropertyField> insertQuery = query;
        SessionTable table = session.createTemporaryTable(keys.filterOrderIncl(query.getMapKeys().keys()), query.getProperties(), null, null, null, new FillTemporaryTable() { // статистика обновится в readSingleValues / removeFields
            public Integer fill(String name) throws SQLException, SQLHandledException {
//                ServerLoggers.assertLog(session.getCount(name, opOwner)==0, "TEMPORARY TABLE SHOULD BE EMPTY");
                return session.insertSessionSelect(name, insertQuery, env, owner, selectTop);
            }
        }, getQueryClasses(query), owner, opOwner);

        // нужно прочитать то что записано
        if(table.count > SessionRows.MAX_ROWS) {
            if(!Settings.get().isDisableReadSingleValues()) { // чтение singleValues
                Result<ImMap<KeyField, Object>> actualKeyValues = new Result<>(); Result<ImMap<KeyField, Integer>> statKeys = new Result<>();
                Result<ImMap<PropertyField, Object>> actualPropValues = new Result<>(); Result<ImMap<PropertyField, PropStat>> statProps = new Result<>();
                session.readSingleValues(table, actualKeyValues, actualPropValues, statKeys, statProps, opOwner);
                keyValues.set(baseClass.getDataObjects(session, actualKeyValues.result, table.classes.getCommonClasses(actualKeyValues.result.keys()), opOwner).addExcl(keyValues.result));
                final ImMap<PropertyField,ClassWhere<Field>> fPropertyClasses = table.propertyClasses;
                propValues.set(baseClass.getObjectValues(session, actualPropValues.result, actualPropValues.result.mapKeyValues(new GetValue<AndClassSet, PropertyField>() {
                    public AndClassSet getMapValue(PropertyField value) { // тут может быть что type - numeric, а commonClass скажем integer и нарушится assertion в DataObject + оптимизация (сверху по идее ту же проверку, но пока не сталкивались)
                        return value.type instanceof DataClass ? (DataClass)value.type : fPropertyClasses.get(value).getCommonClass(value);
                    }}), opOwner).addExcl(propValues.result));
                table = table.removeFields(session, actualKeyValues.result.keys(), actualPropValues.result.keys(), owner, opOwner).updateKeyPropStats(statKeys.result, statProps.result);
            }
            table = table.checkClasses(session, baseClass, updateClasses, opOwner);
            return new SessionDataTable(table, keys, keyValues.result, propValues.result);
        } else {
            ImOrderMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> readRows = (table.count == 0 ? MapFact.<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>EMPTYORDER() : table.read(session, baseClass, opOwner));

            table.drop(session, owner, opOwner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionRows sessionRows = new SessionRows(keys, properties);
            for (int i=0,size=readRows.size();i<size;i++)
                sessionRows = (SessionRows) sessionRows.modifyRecord(session, readRows.getKey(i).addExcl(keyValues.result), readRows.getValue(i).addExcl(propValues.result), Modify.ADD, owner, opOwner, new Result<Boolean>());
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
        return new Pair<>(classes, propertyClasses);
    }

    public SessionData rewrite(SQLSession session, IQuery<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, TableOwner owner, boolean updateClasses) throws SQLException, SQLHandledException {
       return rewrite(session, query, baseClass, env, owner, updateClasses, 0);
    }

    public SessionData rewrite(SQLSession session, IQuery<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, TableOwner owner, boolean updateClasses, int selectTop) throws SQLException, SQLHandledException {
        boolean dropBefore = !Settings.get().isAlwaysDropSessionTableAfter() && !used(query);
        OperationOwner opOwner = env.getOpOwner();
        if(dropBefore)
            drop(session, owner, opOwner);

        SessionData result = write(session, getOrderKeys(), getProperties(), query, baseClass, env, owner, updateClasses, selectTop);

        if(!dropBefore)
            drop(session, owner, opOwner);
        return result;
    }

    public SessionData modifyRows(final SQLSession session, final IQuery<KeyField, PropertyField> query, BaseClass baseClass, final Modify type, final QueryEnvironment env, final TableOwner owner, final Result<Boolean> changed, boolean updateClasses) throws SQLException, SQLHandledException {
        if(!Settings.get().isDisableReadSingleValues()) {
            SessionData singleResult = readSingleValues(session, baseClass, env, query, new Result<IQuery<KeyField, PropertyField>>(), new Result<ImMap<KeyField, DataObject>>(),
                    new Result<ImMap<PropertyField, ObjectValue>>(), new ResultSingleValues<SessionData>() {
                public SessionData empty() {
                    return SessionData.this;
                }

                public SessionData singleRow(ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propValues) throws SQLException, SQLHandledException {
                    return modifyRecord(session, keyValues, propValues, type, owner, env.getOpOwner(), changed);
                }
            });
            if(singleResult!=null)
                return singleResult;
        }

        QueryBuilder<KeyField, PropertyField> modifyQuery = new QueryBuilder<>(query.getMapKeys());
        final Join<PropertyField> prevJoin = join(modifyQuery.getMapExprs());

        final Where prevWhere = prevJoin.getWhere();
        final Where newWhere = query.getWhere();
        modifyQuery.and(type == Modify.DELETE ? prevWhere.and(newWhere.not()) : (type == Modify.UPDATE ? prevWhere : prevWhere.or(newWhere)));
        modifyQuery.addProperties(getProperties().mapValues(new GetValue<Expr, PropertyField>() {
            public Expr getMapValue(PropertyField value) {
                Expr prevExpr = prevJoin.getExpr(value);
                if (type == Modify.DELETE || !query.getProperties().contains(value))
                    return prevExpr;
                Expr newExpr = query.getExpr(value);
                return (type == Modify.MODIFY || type == Modify.UPDATE ?
                        newExpr.ifElse(newWhere, prevExpr) : prevExpr.ifElse(prevWhere, newExpr));
            }
        }));
        SessionData result = rewrite(session, modifyQuery.getQuery(), baseClass, env, owner, updateClasses);
        if(!(this instanceof SessionRows && result instanceof SessionRows && BaseUtils.hashEquals(this, result))) // оптимизация
            changed.set(true); // тут по идее не надо, так как source все равно изменится
        return result;
    }
    public abstract SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, Pair<Long, Long>[] shifts, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException;

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> writeRows, TableOwner owner, OperationOwner opOwner) throws SQLException, SQLHandledException {
        drop(session, owner, opOwner);

        if(writeRows.size()> SessionRows.MAX_ROWS)
            return new SessionDataTable(session, getOrderKeys(), getProperties(), writeRows, owner, opOwner);
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

    public abstract T checkClasses(SQLSession session, BaseClass baseClass, boolean updateClasses, OperationOwner owner, boolean inconsistent, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> rereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException;
}
