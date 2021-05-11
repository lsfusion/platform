package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.CacheAspect;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.AbstractValuesContext;
import lsfusion.server.data.caches.ValuesContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.caches.hash.HashValues;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.sql.table.SQLTemporaryPool;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.type.parse.StringParseInterface;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.classes.change.ClassChanges;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.SerializedTable;
import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.function.IntFunction;

import static lsfusion.base.BaseUtils.hashEquals;

public class SessionTable extends NamedTable implements ValuesContext<SessionTable>, Value {// в явную хранимые ряды
    private static final Logger sqlLogger = ServerLoggers.sqlLogger;

    public final int count; // volatile, same as SubQueryContext.subQuery

    // nullable, иногда известно, иногда нет
    // assert что statKeys и statProps или одновременно null или одновременно нет
    private TableStatKeys statKeys;  
    private ImMap<PropertyField, PropStat> statProps;    

    public TableStatKeys getTableStatKeys() {
        if(statKeys == null)
            statKeys = getStatKeys(this, count);
        return statKeys;
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        if(statProps == null)
            statProps = getStatProps(this);
        return statProps;
    }

    @Override
    protected ImSet<ImOrderSet<Field>> getIndexes() {
        return SQLSession.getTemporaryIndexes(keys, properties);
    }

    public Value removeBig(MAddSet<Value> usedValues) {
        return null;
    }

    @Override
    public String toDebugString() {
        return name + ": " + count + " - " + struct;
    }

    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, int count, TableStatKeys statKeys, ImMap<PropertyField, PropStat> statProps) {
        super(name, keys, properties, classes, propertyClasses);
        this.count = count;
        this.statKeys = statKeys;
        this.statProps = statProps;
    }

    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, int count, ImMap<KeyField, Integer> distinctKeys, ImMap<PropertyField, PropStat> statProps) {
        this(name, keys, properties, classes, propertyClasses, count, distinctKeys == null ? null : TableStatKeys.createForTable(count, distinctKeys), statProps);
    }

    // конструкторы со сбросом статистики
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, int count) {
        this(name, keys, properties, classes, propertyClasses, count, (ImMap<KeyField, Integer>) null, null);
    }
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, int count, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> tableClasses) {
        this(name, keys, properties, tableClasses.first, tableClasses.second, count);
    }

    // for debug
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) {
        super(name, keys, properties, null, null);
        initBaseClasses(getBaseClass());

        statKeys = SerializedTable.getStatKeys(this);
        count = statKeys.getRows().getCount();
        statProps = SerializedTable.getStatProps(this);
    }

    private static BaseClass getBaseClass() {
        return ThreadLocalContext.getBaseLM().baseClass;
    }

    private static Pair<ImMap<KeyField, Integer>, ImMap<PropertyField, PropStat>> getStats(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, final ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) {
        final ImList<MSet<DataObject>> distinctKeyValues = ListFact.toList(keys.size(), ListFact.mSet());
        ImOrderSet<PropertyField> propList = properties.toOrderSet();
        final ImList<MSet<ObjectValue>> distinctPropValues = ListFact.toList(propList.size(), ListFact.mSet());

        for(int i=0,size=rows.size();i<size;i++) {
            ImMap<KeyField, DataObject> keyValues = rows.getKey(i);
            for(int j=0,sizeJ=keys.size();j<sizeJ;j++)
                distinctKeyValues.get(j).add(keyValues.get(keys.get(j)));
            ImMap<PropertyField, ObjectValue> propValues = rows.getValue(i);
            for(int j=0,sizeJ=propList.size();j<sizeJ;j++)
                distinctPropValues.get(j).add(propValues.get(propList.get(j)));
        }
        ImMap<KeyField, Integer> distinctKeys = keys.mapOrderValues((int i) -> distinctKeyValues.get(i).size());
        ImMap<PropertyField, PropStat> distinctProps = propList.mapOrderValues((IntFunction<PropStat>) i -> new PropStat(new Stat(distinctPropValues.get(i).size())));
        return new Pair<>(distinctKeys, distinctProps);
    }

    // создает таблицу batch'ем
    public static SessionTable create(final SQLSession session, final ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, final ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, final TableOwner owner, final OperationOwner opOwner) throws SQLException, SQLHandledException {
        // прочитаем статистику
        Pair<ImMap<KeyField, Integer>, ImMap<PropertyField, PropStat>> stats = getStats(keys, properties, rows);

        // прочитаем классы
        return session.createTemporaryTable(keys, properties, rows.size(), stats.first, stats.second, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                session.insertSessionBatchRecords(name, keys, rows, opOwner, owner);
                return null;
            }
            public boolean canBeNotEmptyIfFailed() {
                return true;
            }
        }, SessionRows.getClasses(properties, rows), owner, opOwner);
    }

    public ImSet<PropertyField> getProperties() {
        return properties;
    }

    @Override
    public String getName(SQLSyntax syntax) {
        return syntax.getSessionTableName(name);
    }

    @Override
    public String getQuerySource(CompileSource source) {
        assert source.params.containsKey(this);
        return source.params.get(this);
    }

    protected Table translate(MapTranslate translator) {
        return translateValues(translator.mapValues());
    }

    public int hash(HashContext hashContext) {
        return hashValues(hashContext.values);
    }

    public ImSet<Value> getValues() {
        return getContextValues();
    }

    public SessionTable translateValues(MapValuesTranslate mapValues) {
        return mapValues.translate(this);
    }

    public SessionTable translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }

    public int hashValues(HashValues hashValues) {
        return hashValues.hash(this);
    }

    public ImSet<Value> getContextValues() {
        return SetFact.singleton(this);
    }

    public ParseInterface getParseInterface(QueryEnvironment env, final EnsureTypeEnvironment typeEnv) {
        return new StringParseInterface() {
            public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
                return syntax.getQueryName(name, usedRecursion ? getFunctionType() : null, envString, usedRecursion, typeEnv);
            }

            @Override
            public SessionTable getSessionTable() {
                return SessionTable.this;
            }
        };
    }
    public static ParseInterface getParseInterface(final String table) {
        return new StringParseInterface() {
            public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
                return syntax.getSessionTableName(table);
            }
        };
    }
    
    @Override
    public boolean isAlwaysSafeString() {
        return true;
    }

    public TypeStruct getFunctionType() {
        return new TypeStruct(keys, properties);
    }

    // не будем агрегировать в Struct так как используется только в рекурсиях
    public static class TypeStruct extends TwinImmutableObject implements GlobalObject, FunctionType {
        public final ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        public final ImOrderSet<PropertyField> properties;
        
        public ImOrderSet<Field> getFields() {
            return SetFact.addOrderExcl(keys, properties);
        }
        
        private TypeStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) {
            this.keys = keys;
            this.properties = properties.toOrderSet();
        }

        @Override
        public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
            typeEnv.addNeedTableType(this);
            return syntax.getTableTypeName(this);
        }

        @Override
        public String getParamFunctionDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
            typeEnv.addNeedTableType(this);
            return getDB(syntax, typeEnv) + " READONLY";
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return keys.equals(((TypeStruct) o).keys) && properties.equals(((TypeStruct) o).properties);
        }

        public int immutableHashCode() {
            return 31 * keys.hashCode() + properties.hashCode();
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            SystemUtils.write(out, keys);
            SystemUtils.write(out, properties);
        }
    }
    
    // теоретически достаточно только
    private static class Struct extends TwinImmutableObject implements GlobalObject {

        private final ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        private final ImCol<PropertyField> properties;
        protected final ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
        protected final ImMap<PropertyField, ClassWhere<Field>> propertyClasses;

        protected final TableStatKeys statKeys;
        protected final ImMap<PropertyField, PropStat> statProps;

        private Struct(ImOrderSet<KeyField> keys, ImCol<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, TableStatKeys statKeys, ImMap<PropertyField, PropStat> statProps) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;

            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses) && statKeys.equals(((Struct)o).statKeys) && statProps.equals(((Struct)o).statProps);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode()) + statKeys.hashCode()) + statProps.hashCode();
        }

        @Override
        public String toString() {
            return "{ cl : " + classes + " " + propertyClasses + ", st: " + statKeys + " " + statProps + "}" ;
        }
    }

    private Struct struct = null;

    @ManualLazy
    public Struct getValueClass() {
        if (struct == null) {
            struct = CacheAspect.twinObject(new Struct(keys, properties, classes, propertyClasses, getTableStatKeys(), getStatProps())); // possibly premature optimization, but it\s here for a pretty long time, so just in case
        }
        return struct;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((SessionTable) o).name) && getValueClass().equals(((SessionTable) o).getValueClass());
    }

    public int immutableHashCode() {
        return name.hashCode() * 31 + getValueClass().hashCode();
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses) {
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.merge(orClasses.second, ClassWhere.getAddOr());
        return new Pair<>(classes.or(orClasses.first), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, final ImMap<PropertyField, ObjectValue> propFields) {
        return orFieldsClassWheres(DataObject.getMapDataClasses(keyFields), ObjectValue.getMapClasses(propFields), classes, propertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(final ImMap<KeyField, ConcreteClass> keyFields, final ImMap<PropertyField, ConcreteClass> propFields, ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {

        assert propertyClasses.keys().containsAll(propFields.keys());
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.mapValues((propField, existedPropertyClasses) -> {
            ConcreteClass propClass = propFields.get(propField);
            if (propClass != null)
                existedPropertyClasses = existedPropertyClasses.or(new ClassWhere<>(
                        MapFact.addExcl(keyFields, propField, propClass)));
            return existedPropertyClasses;
        });
        return new Pair<>(
                classes.or(new ClassWhere<>(keyFields)), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> andFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) {
        // определяем новые классы чтобы создать таблицу
        final ClassWhere<KeyField> addKeyClasses = new ClassWhere<>(DataObject.getMapDataClasses(keyFields));

        final ClassWhere<KeyField> andKeyClasses = classes.and(addKeyClasses);

        ImMap<PropertyField, ClassWhere<Field>> andPropertyClasses = propertyClasses.mapValues(value -> value.and(BaseUtils.<ClassWhere<Field>>immutableCast(addKeyClasses))).addExcl(
                propFields.mapValues((key, value) -> !(value instanceof DataObject)?ClassWhere.FALSE():
                        new ClassWhere<>(MapFact.<Field, ConcreteClass>singleton(key, ((DataObject) value).objectClass)).
                                and(BaseUtils.immutableCast(andKeyClasses))));
        return new Pair<>(andKeyClasses, andPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> removeFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, final ImSet<KeyField> keyFields, ImSet<PropertyField> propFields) {
        if(keyFields.isEmpty())
            return new Pair<>(classes, propertyClasses.remove(propFields));
        else {
            ImMap<PropertyField, ClassWhere<Field>> removePropClasses = propertyClasses.remove(propFields).mapValues(value -> value.remove(keyFields));
            return new Pair<>(classes.remove(keyFields), removePropClasses);
        }
    }

    public SessionTable modifyRecord(final SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, final TableOwner owner, OperationOwner opOwner, Result<Boolean> changed) throws SQLException, SQLHandledException {

        boolean updateClasses = changeTable;
        if(type==Modify.DELETE) { // статистику пока не учитываем
            int proceeded = deleteRecords(session, keyFields, opOwner, owner);
            if(proceeded == 0)
                return this;
            changed.set(true);
            return new SessionTable(name, keys, properties, classes, propertyClasses, count - proceeded).
                    updateStatistics(session, count, 0, owner, opOwner).checkClasses(session, null, updateClasses, opOwner);
        }
        if (type == Modify.LEFT && session.isRecord(this, keyFields, opOwner))
            return this;

        boolean update = (type==Modify.UPDATE || type==Modify.MODIFY);

        if(update) {
            if(session.updateRecordsCount(this, keyFields, propFields, opOwner, owner)==0) { // запись не найдена
                if(type==Modify.UPDATE)
                    return this;
                else
                    update = false;
            }
        }
        if(!update)
            session.insertRecord(this, keyFields, propFields, owner, opOwner);

        changed.set(true);
        int proceeded = (update?0:1);
        return new SessionTable(name, keys, properties, count + proceeded,
                        orFieldsClassWheres(classes, propertyClasses, keyFields, propFields)).
                            updateStatistics(session, count, proceeded, owner, opOwner).checkClasses(session, null, updateClasses, opOwner);
    }

    public SessionTable modifyRows(SQLSession session, IQuery<KeyField, PropertyField> query, Modify type, QueryEnvironment env, TableOwner owner, Result<Boolean> changed, boolean updateClasses) throws SQLException, SQLHandledException {

        if(query.isEmpty()) // оптимизация
            return this;

        OperationOwner opOwner = env.getOpOwner();

        ModifyQuery modify = new ModifyQuery(this, query, env, owner);
        int inserted, proceeded;
        switch (type) {
            case MODIFY:
                Result<Integer> modifyProceeded = new Result<>();
                inserted = session.modifyRecords(modify, modifyProceeded);
                proceeded = modifyProceeded.result;
                break;
            case LEFT:
                proceeded = session.insertLeftSelect(modify, true, false);
                inserted = proceeded;
                break;
            case ADD:
                proceeded = session.insertSelect(modify);
                inserted = proceeded;
                break;
            case UPDATE:
                proceeded = session.updateRecords(modify);
                inserted = 0;
                break;
            case DELETE:
                proceeded = session.deleteRecords(modify);
                if(proceeded==0)
                    return this;

                changed.set(true);
                return new SessionTable(name, keys, properties, classes, propertyClasses, count - proceeded).
                        updateStatistics(session, count, 0, owner, opOwner).checkClasses(session, null, updateClasses, opOwner);
            default:
                throw new RuntimeException("should not be");
        }
        if(proceeded==0)
            return this;

        changed.set(true);
        return new SessionTable(name, keys, properties, count + inserted,
                        orFieldsClassWheres(classes, propertyClasses, SessionData.getQueryClasses(query))).
                            updateStatistics(session, count, proceeded, owner, opOwner).checkClasses(session, null, updateClasses, opOwner);
    }
    public void updateAdded(SQLSession session, BaseClass baseClass, PropertyField field, Pair<Long, Long>[] shifts, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this);
        lsfusion.server.data.query.build.Join<PropertyField> join = join(query.getMapExprs());

        String formula = ""; String aggsh = "";
        MExclMap<String, Expr> mParams = MapFact.mExclMap(1 + 2 * shifts.length);
        mParams.exclAdd("prm1", join.getExpr(field));
        for(int i=0;i<shifts.length;i++) {
            String idsh = "prm" + (2*i+2);
            String countsh = "prm" + (2*i+3);

            if(i==0) {
                formula = idsh;
                aggsh = countsh;
            } else {
                formula = "WHEN prm1 > (" + aggsh + ") THEN " + idsh + " - (" + aggsh + ") " + (i==1?"ELSE ":"") + formula;
                aggsh += "+" + countsh;
            }
            mParams.exclAdd(idsh, new ValueExpr(shifts[i].first, ObjectType.idClass));
            if(i!=shifts.length-1) // последний параметр не нужен
                mParams.exclAdd(countsh, new ValueExpr(shifts[i].second, ObjectType.idClass));
        }
        if(shifts.length > 1)
            formula = "CASE " + formula + " END";

        query.addProperty(field, FormulaExpr.createCustomFormula("prm1+" + formula, baseClass.unknown, mParams.immutable()));
        query.and(join.getWhere());
        session.updateRecords(new ModifyQuery(this, query.getQuery(), owner, tableOwner));
    }

    public boolean hasClassChanges(ClassChanges classChanges) {
        for(KeyField key : keys)
            if(classChanges.hasChanges(classes.getCommonClass(key)))
                return true;

        for(int i=0,size=propertyClasses.size();i<size;i++) {
            PropertyField property = propertyClasses.getKey(i);
            if (classChanges.hasChanges(propertyClasses.getValue(i).getCommonClass(property)))
                return true;
        }

        return false;
    }
    
    public SessionTable updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        if(!hasClassChanges(session.changes)) // повторная проверка - оптимизация 
            return this;

        final ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        lsfusion.server.data.query.build.Join<PropertyField> join = join(mapKeys);

        MExclMap<Field, Expr> mMapExprs = MapFact.mExclMapMax(keys.size()+properties.size());
        MExclMap<Field, DataClass> mMapData = MapFact.mExclMapMax(keys.size()+properties.size());
        ClassWhere<KeyField> updatedClasses = ClassWhere.FALSE();
        for(KeyField key : keys)
            if(key.type instanceof ObjectType)
                mMapExprs.exclAdd(key, mapKeys.get(key));
            else
                mMapData.exclAdd(key, (DataClass) key.type);
        for(PropertyField property : properties)
            if(property.type instanceof ObjectType)
                mMapExprs.exclAdd(property, join.getExpr(property));
            else
                mMapData.exclAdd(property, (DataClass) property.type);
        ImMap<Field, Expr> mapExprs = mMapExprs.immutable();
        ImMap<Field, DataClass> mapData = mMapData.immutable();
        ImMap<PropertyField, ClassWhere<Field>> updatedPropertyClasses = properties.toMap(ClassWhere.FALSE());

        // пока исходим из assertion'а что не null, потом надо будет разные делать
        SQLSession sql = session.sql;
        QueryEnvironment env = session.env;
        for(ImMap<Field, ConcreteObjectClass> diffClasses : ClassChanges.readChangedCurrentObjectClasses(join.getWhere(), MapFact.EMPTY(), mapExprs, sql, session.modifier, env, session.baseClass)) {
            final ImMap<Field, ConcreteClass> result = MapFact.addExcl(diffClasses, mapData);
            updatedClasses = updatedClasses.or(new ClassWhere<>(result.filterIncl(getTableKeys())));
            
            updatedPropertyClasses = updatedPropertyClasses.mapValues((key, value) -> value.or(new ClassWhere<>(result.filterIncl(SetFact.addExcl(getTableKeys(), key)))));
        }
        return new SessionTable(name, keys, properties, updatedClasses, updatedPropertyClasses, count, statKeys, statProps).checkClasses(sql, null, nonead, env.getOpOwner());
    }

    public SessionTable updateStatistics(final SQLSession session, int prevCount, int updated, final TableOwner owner, final OperationOwner opOwner) throws SQLException, SQLHandledException {
//        assert statKeys == null && statProps == null;
        if(!SQLTemporaryPool.getDBStatistics(count).equals(SQLTemporaryPool.getDBStatistics(prevCount)) || (updated >= 1 && new Stat(Settings.get().getUpdateStatisticsLimit()).lessEquals(new Stat(updated)))) { // проблема в том, что может появиться много записей с field = n, а СУБД этого не будет знать и будет сильно ошибаться со статистикой
            return session.createTemporaryTable(keys, properties, count, null, null, new FillTemporaryTable() {
                public Integer fill(String name) throws SQLException, SQLHandledException {
                    QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<>(SessionTable.this);
                    lsfusion.server.data.query.build.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs());
                    session.insertSessionSelect(name, moveData.getQuery(), DataSession.emptyEnv(opOwner), owner);
                    session.returnTemporaryTable(SessionTable.this, owner, opOwner, count);
                    return null;
                }
            }, new Pair<>(classes, propertyClasses), owner, opOwner);
        }
        return this;
    }

    public int deleteRecords(SQLSession session, ImMap<KeyField, DataObject> keys, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        return session.deleteKeyRecords(this, DataObject.getMapDataValues(keys), owner, tableOwner);
    }


    public SessionTable addFields(final SQLSession session, final ImOrderSet<KeyField> keys, final ImMap<KeyField, DataObject> addKeys, final ImMap<PropertyField, ObjectValue> addProps, final TableOwner owner, final OperationOwner opOwner) throws SQLException, SQLHandledException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        // пока null'им статистику, так как в modify* она все равно null'ся
        return session.createTemporaryTable(keys, properties.addExcl(addProps.keys()), count, null, null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException, SQLHandledException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                ImSet<KeyField> tableKeys = getTableKeys();
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<>(tableKeys.addExcl(addKeys.keys()), addKeys);
                lsfusion.server.data.query.build.Join<PropertyField> prevJoin = join(moveData.getMapExprs().filterIncl(tableKeys));
                moveData.and(prevJoin.getWhere());
                moveData.addProperties(prevJoin.getExprs());
                moveData.addProperties(DataObject.getMapExprs(addProps));
                session.insertSessionSelect(name, moveData.getQuery(), DataSession.emptyEnv(opOwner), owner);
                session.returnTemporaryTable(SessionTable.this, owner, opOwner, count);
                return null;
            }
        }, andFieldsClassWheres(classes, propertyClasses, addKeys, addProps), owner, opOwner);
    }

    public SessionTable updateKeyPropStats(ImMap<KeyField, Integer> updatedKeyStats, ImMap<PropertyField, PropStat> updatedPropStats) {
//        assert statKeys == null && statProps == null;
        return new SessionTable(name, keys, properties, classes, propertyClasses, count, updatedKeyStats, updatedPropStats);
    }

    public SessionTable removeFields(final SQLSession session, ImSet<KeyField> removeKeys, ImSet<PropertyField> removeProps, final TableOwner owner, final OperationOwner opOwner) throws SQLException, SQLHandledException {
        if(removeKeys.isEmpty() && removeProps.isEmpty())
            return this;

        // assert что удаляемые ключи с одинаковыми значениями, но вообще может использоваться как слияние
        final ImOrderSet<KeyField> remainOrderKeys = keys.removeOrder(removeKeys);
        final ImSet<KeyField> remainKeys = remainOrderKeys.getSet();
        final ImSet<PropertyField> remainProps = properties.remove(removeProps);
        // передаем null в качестве статистики так как в месте использования все равно уточнение пойдет
        return session.createTemporaryTable(remainOrderKeys, remainProps, count, null, null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException, SQLHandledException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<>(remainKeys);

                if (remainKeys.size() == keys.size()) { // для оптимизации
                    lsfusion.server.data.query.build.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs().filterIncl(remainProps));
                } else {
                    ImRevMap<KeyField, KeyExpr> tableKeys = getMapKeys();
                    lsfusion.server.data.query.build.Join<PropertyField> prevJoin = join(tableKeys);
                    ImRevMap<KeyField, KeyExpr> groupKeys = tableKeys.filterInclRev(remainKeys);
                    moveData.and(GroupExpr.create(groupKeys, prevJoin.getWhere(), moveData.getMapExprs()).getWhere());
                    for (PropertyField prop : remainProps)
                        moveData.addProperty(prop, GroupExpr.create(groupKeys, prevJoin.getExpr(prop), GroupType.ASSERTSINGLE(), moveData.getMapExprs())); // не может быть недетерминированности, так как просто копирование из таблицы в таблицу
                }
                session.insertSessionSelect(name, moveData.getQuery(), DataSession.emptyEnv(opOwner), owner);
                session.returnTemporaryTable(SessionTable.this, owner, opOwner, count);
                return null;
            }
        }, removeFieldsClassWheres(classes, propertyClasses, removeKeys, removeProps), owner, opOwner);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if (components == null)
            components = AbstractValuesContext.getComponents(this);
        return components;
    }

    public void drop(SQLSession session, TableOwner owner, OperationOwner opOwner) throws SQLException {
        session.returnTemporaryTable(this, owner, opOwner, count);
    }
    public void rollDrop(SQLSession session, TableOwner owner, OperationOwner opOwner, boolean assertNotExists) throws SQLException {
        session.rollReturnTemporaryTable(this, owner, opOwner, assertNotExists);
    }

    // см. usage
    public SessionTable fixKeyClasses(ClassWhere<KeyField> fixClasses) {
//        assert propertyClasses.isEmpty();
        ClassWhere<KeyField> fixedClasses = classes.and(fixClasses);
        if(hashEquals(fixedClasses, classes))
            return this;
        else
            return new SessionTable(name, keys, properties, fixedClasses, propertyClasses, count, statKeys, statProps);
    }

    // для проверки общей целостности есть специальные административные процедуры
//    private boolean assertCheckClasses(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
//        if(1==1 || session.inconsistent)
//            return true;
//
//        if(baseClass==null)
//            baseClass = ThreadLocalContext.getBusinessLogics().LM.baseClass;
//
//        final Pair<ClassWhere<KeyField>,ImMap<PropertyField,ClassWhere<Field>>> readClasses;
//        ImMap<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> readData = readClasses(session, baseClass, OperationOwner.debug); // теоретически может очень долго работать, когда много колонок, из-за большого количества case'ов по которым надо группировать
//        if(readData!=null)
//            readClasses = SessionRows.getClasses(readData,  properties);
//        else // если concatenate type есть, читаем сами значения
//            readClasses = SessionRows.getClasses(properties, read(session, baseClass, OperationOwner.debug).getMap());
//
//        // похоже все же не имеет смысла пока
///*        if(!classes.means(readClasses.first, true))
//            classes = readClasses.first;
//        propertyClasses = propertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
//            public ClassWhere<Field> apply(PropertyField key, ClassWhere<Field> value) {
//                ClassWhere<Field> readWhere = readClasses.second.get(key);
//                if(!value.means(readWhere, true))
//                    return readWhere;
//                return value;
//            }});*/
//
//        if(!readClasses.first.means(classes, true))
//            return false;
//
//        for(PropertyField property : properties)
//            if(!readClasses.second.get(property).means(propertyClasses.get(property), true))
//                return false;
//
//        return true;
//    }

    public final static boolean matGlobalQuery = true; // global expr usage
    public final static boolean matLocalQuery = false; // local usage разбить на совсем local + сохр. в change'и
    public final static boolean matGlobalQueryFromTable = false; // global query, но таблицу уже читалась readChangeTable
    public final static boolean matExprLocalQuery = true; // local expr usage, но потом может использоваться для property.getExpr и в сложных запросах
    public final static boolean changeTable = false; // changing table with specific values
    public final static boolean nonead = false; // all the rest

    public SessionTable checkClasses(SQLSession session, BaseClass baseClass, boolean checkClassesUpdate, OperationOwner owner) throws SQLException, SQLHandledException {  
        return checkClasses(session, baseClass, checkClassesUpdate, owner, false, null, null, null, 0);
    }
    public SessionTable checkClasses(SQLSession session, BaseClass baseClass, boolean checkClassesUpdate, OperationOwner owner, final boolean inconsistent, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> rereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
//        assert assertCheckClasses(session, baseClass);
        assert !(session.inconsistent && inconsistent);
        assert checkClassesUpdate || !inconsistent;

        if(session.inconsistent || !checkClassesUpdate || (!inconsistent && Settings.get().isDisableReadClasses()))
            return this;

        if(baseClass==null)
            baseClass = getBaseClass();

        final Pair<ClassWhere<KeyField>,ImMap<PropertyField,ClassWhere<Field>>> readClasses;
        final Result<ImSet<KeyField>> objectKeys = new Result<>(); final Result<ImSet<PropertyField>> objectProps = new Result<>();
        ImMap<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> readData = readClasses(session, baseClass, objectKeys, objectProps, owner, inconsistent, inconsistentTableClasses, rereadChanges, classRemove, timestamp); // теоретически может очень долго работать, когда много колонок, из-за большого количества case'ов по которым надо группировать
        
        if(readData!=null && !(objectKeys.result.isEmpty() && objectProps.result.isEmpty())) // вторая проверка оптимизация
            readClasses = SessionRows.getClasses(readData,  objectProps.result);
        else // если concatenate type есть, читаем сами значения
            return this;
//            readClasses = SessionRows.getClasses(properties, read(session, baseClass, OperationOwner.debug).getMap());

        // assert что readClasses.first => classes + readClasses.second => propertyClasses.
        if(!inconsistent) {
            assert readClasses.first.means(classes.filterKeys(objectKeys.result), true);
            for (PropertyField property : objectProps.result)
                assert readClasses.second.get(property).means(propertyClasses.get(property).filterKeys(SetFact.addExcl(objectKeys.result, property)), true);
        }

        // проверяем что у classes \ propertyClasses есть complex - классы \ unknown

        ClassWhere<KeyField> newClasses = classes; ImMap<PropertyField,ClassWhere<Field>> newPropertyClasses;

        boolean updatedClasses;
        if(inconsistent) {
            updatedClasses = !readClasses.first.means(classes, true); // оптимизация, если даже уточняет классы не будем трогать, чтобы не пересоздавать ссылки
            if(updatedClasses)
                newClasses = newClasses.remove(objectKeys.result).and(readClasses.first);
        } else {
            updatedClasses = !classes.means(readClasses.first, true); // оптимизация
            if (updatedClasses)
                newClasses = newClasses.and(readClasses.first);
        }

        final Result<Boolean> updatedProps = new Result<>(false); // оптимизация
        final ClassWhere<KeyField> fNewClasses = newClasses;
        final boolean fUpdatedClasses = updatedClasses;
        newPropertyClasses = propertyClasses.mapItValues((key, value) -> {
            ClassWhere<Field> readWhere = readClasses.second.get(key);
            assert (readWhere != null) == objectProps.result.contains(key);
            if (readWhere != null) {
                if (inconsistent) {
                    if (!readWhere.means(value, true)) { // если даже уточняет классы не будем трогать, чтобы не пересоздавать ссылки
                        updatedProps.set(true);
                        value = value.remove(SetFact.addExcl(objectKeys.result, key)).and(readWhere);
                    }
                } else {
                    if (!value.means(readWhere, true)) {
                        updatedProps.set(true);
                        value = value.and(readWhere);
                    }
                }
            }
            if(fUpdatedClasses)
                value = value.and(BaseUtils.immutableCast(fNewClasses));
            return value;
        });

        if(updatedClasses || updatedProps.result)
            return new SessionTable(name, keys, properties, newClasses, newPropertyClasses, count, statKeys, statProps);

        return this;
    }
    
    public void saveToDBForDebug(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        try(SQLSession dbSql = ThreadLocalContext.getDbManager().createSQL()) {

            dbSql.startTransaction(DBManager.DEBUG_TIL, OperationOwner.unknown);
            dbSql.ensureTable(this, sqlLogger);
            dbSql.insertSessionBatchRecords(getName(sql.syntax), keys, read(sql, getBaseClass(), OperationOwner.debug).getMap(), OperationOwner.debug, TableOwner.debug);
            dbSql.commitTransaction();
        }
    }
    
    public static void saveToDBForDebug(ImSet<? extends Value> values, SQLSession sql) throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException, SQLHandledException {
        for(Value value : values)
            if(value instanceof SessionTable)
                ((SessionTable)value).saveToDBForDebug(sql);
    }
}
