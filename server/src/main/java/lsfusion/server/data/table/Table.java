package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.lambda.Processor;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ParamInstanceLazy;
import lsfusion.server.base.caches.TwinLazy;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.classes.InnerFollows;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.join.where.GroupJoinsWheres;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.expr.join.where.WhereJoins;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.RecursiveTable;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.cases.MCaseList;
import lsfusion.server.data.expr.where.cases.MJoinCaseList;
import lsfusion.server.data.expr.where.ifs.IfJoin;
import lsfusion.server.data.expr.where.ifs.NullJoin;
import lsfusion.server.data.expr.where.pull.AddPullWheres;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.struct.OrConcatenateClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;
import lsfusion.server.logics.classes.user.set.OrClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import lsfusion.server.physics.exec.db.table.SerializedTable;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Table extends AbstractOuterContext<Table> implements MapKeysInterface<KeyField> {
    
    public ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public ImOrderSet<KeyField> getOrderTableKeys() {
        return keys;
    }
    public ImSet<KeyField> getTableKeys() {
        return keys.getSet();
    }
    public ImSet<PropertyField> properties;

    public ImMap<PropertyField, Type> getPropTypes() {
        return properties.mapValues((PropertyField value) -> value.type);
    }

    public Stat getStatRows() {
        return getTableStatKeys().getRows();
    }
    public abstract TableStatKeys getTableStatKeys();
    public abstract ImMap<PropertyField, PropStat> getStatProps();

    // важно чтобы статистика таблицы была адекватна статистике классов, так как иначе infinite push down'ы могут пойти
    private static int getObjectKeyFieldStat(AndClassSet classSet) {
        OrClassSet orClassSet = classSet.getOr();
        if(orClassSet instanceof OrObjectClassSet) {
            ObjectValueClassSet valueClassSet = ((OrObjectClassSet) orClassSet).getValueClassSet();
            if(BaseUtils.hashEquals(orClassSet, valueClassSet)) { // если есть unknown то их может быть сколько угодно
                return valueClassSet.getCount();
            }
        } else
            assert orClassSet instanceof OrConcatenateClass;
        return -1;
    }
    private static Stat getObjectPropFieldStat(AndClassSet classSet) {
        OrClassSet orClassSet = classSet.getOr();
        if(orClassSet instanceof OrObjectClassSet) {
            ObjectValueClassSet valueClassSet = ((OrObjectClassSet) orClassSet).getValueClassSet();
            if(BaseUtils.hashEquals(orClassSet, valueClassSet)) {
                return new Stat(valueClassSet.getCount());
            }
        } else
            assert orClassSet instanceof OrConcatenateClass;
        return null; // если есть unknown то их может быть сколько угодно
    }

    private static int getKeyFieldStat(KeyField field, Table table) {
        int resultStat = -1;
        if (field.type instanceof ObjectType) {
            AndClassSet commonClass = table.getClasses().getCommonClass(field);
            if (commonClass != null)
                resultStat = getObjectKeyFieldStat(commonClass);
            else
                assert table.getClasses().isFalse();
        }            
        if(resultStat < 0)
            resultStat = field.type.getTypeStat(false).getCount();
        return resultStat;
    }

    private static Stat getPropFieldStat(PropertyField field, Table table) {
        Stat resultStat = null;
        if (field.type instanceof ObjectType) {
            AndClassSet commonClass = table.propertyClasses.get(field).getCommonClass(field);
            if(commonClass != null)
                resultStat = getObjectPropFieldStat(commonClass);
            else
                assert table.propertyClasses.get(field).isFalse();
        }
        if(resultStat == null)
            resultStat = field.type.getTypeStat(false);
        return resultStat;
    }

    protected static TableStatKeys getStatKeys(final Table table, final int count) { // для мн-го наследования
        ImMap<KeyField, Integer> statMap = table.getTableKeys().mapValues((KeyField value) -> BaseUtils.min(count, getKeyFieldStat(value, table)));
        return TableStatKeys.createForTable(count, statMap);
    }

    protected static ImMap<PropertyField, PropStat> getStatProps(final Table table) { // для мн-го наследования
        final Stat rows = table.getTableStatKeys().getRows();
        return table.properties.mapValues((PropertyField prop) -> new PropStat(InnerExpr.getAdjustStatValue(rows, getPropFieldStat(prop, table))));
    }

    public boolean isSingle() {
        return keys.size()==0;
    }

    public ImRevMap<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(getTableKeys());
    }

    protected Table() {
        this(SetFact.EMPTYORDER(), SetFact.EMPTY(), ClassWhere.FALSE(), MapFact.EMPTY());
    }

    protected Table(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties,ClassWhere<KeyField> classes,ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        this.keys = keys;
        this.properties = properties;
        this.classes = classes;
        this.propertyClasses = propertyClasses;

        // assert classes.fitTypes();
        // последний or для debug
        assert (this instanceof SerializedTable || this instanceof ImplementTable.InconsistentTable || classes == null) || classes.isEqual(keys.getSet()) && propClassesFull() && assertClasses(); // см. ClassExprWhere.getKeyType
    }

    private boolean assertClasses() {
        if(classes == null)
            return true;

        for(ClassWhere<Field> pClasses : propertyClasses.valueIt()) {
            assert pClasses.means(BaseUtils.immutableCast(classes), true);
        }
        return true;
    }

    private <K extends Field> ImMap<K, DataClass> getDataFields(ImSet<K> fields) {
        return BaseUtils.immutableCast(fields.mapValues((K value) -> value.type).filterFnValues(element -> element instanceof DataClass));
    }
    private boolean fitTypes() {
        ImMap<KeyField, DataClass> keyDataFields = getDataFields(keys.getSet());
        if(!classes.fitDataClasses(keyDataFields))
            return false;

        for(int i=0,size=propertyClasses.size();i<size;i++)
            if(!propertyClasses.getValue(i).fitDataClasses(MapFact.addExcl(keyDataFields, getDataFields(SetFact.singleton(propertyClasses.getKey(i))))))
                return false;
        return true;
    }
    private boolean propClassesFull() {
        if(!BaseUtils.hashEquals(propertyClasses.keys(), properties))
            return false;

        for(int i=0,size=propertyClasses.size();i<size;i++)
            if(!propertyClasses.getValue(i).isEqual(SetFact.addExcl(keys.getSet(), propertyClasses.getKey(i))))
                return false;
        return true;
    }

    public abstract String getQuerySource(CompileSource source);

    public KeyField findKey(String name) {
        for(KeyField key : keys)
            if(key.getName().equals(name))
                return key;
        return null;
    }

    public PropertyField findProperty(String name) {
        for(PropertyField property : properties)
            if(property.getName().equals(name))
                return property;
        return null;
    }

    protected void initBaseClasses(final BaseClass baseClass) {
        final ImMap<KeyField, AndClassSet> baseClasses = getTableKeys().mapValues((KeyField value) -> value.type.getBaseClassSet(baseClass));
        classes = new ClassWhere<>(baseClasses);

        propertyClasses = properties.mapValues((Function<PropertyField, ClassWhere<Field>>) value -> new ClassWhere<>(MapFact.addExcl(baseClasses, value, value.type.getBaseClassSet(baseClass))));
    }

    public Table(DataInputStream inStream, final BaseClass baseClass) throws IOException {
        int keysNum = inStream.readInt();
        MOrderExclSet<KeyField> mKeys = SetFact.mOrderExclSet(keysNum); // десериализация, поэтому порядок важен
        for(int i=0;i<keysNum;i++)
            mKeys.exclAdd((KeyField) Field.deserialize(inStream));
        keys = mKeys.immutableOrder();
        int propNum = inStream.readInt();
        MExclSet<PropertyField> mProperties = SetFact.mExclSet(propNum);
        for(int i=0;i<propNum;i++)
            mProperties.exclAdd((PropertyField) Field.deserialize(inStream));
        properties = mProperties.immutable();

        initBaseClasses(baseClass);
    }

    public ImOrderMap<ImMap<KeyField, DataObject>,ImMap<PropertyField, ObjectValue>> read(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this);
        lsfusion.server.data.query.build.Join<PropertyField> tableJoin = join(query.getMapExprs());
        query.addProperties(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.executeClasses(session, baseClass, owner);
    }

    public void readData(SQLSession session, BaseClass baseClass, OperationOwner owner, boolean noFilesAndLogs, ResultHandler<KeyField, PropertyField> result) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(this);
        lsfusion.server.data.query.build.Join<PropertyField> tableJoin = join(query.getMapExprs());
        ImMap<PropertyField, Expr> exprs = tableJoin.getExprs();
        if(noFilesAndLogs)
            exprs = exprs.filterFn(element -> !(element.type instanceof FileClass || element.getName().contains("_LG_") || element.getName().contains("_LOG_")));
        query.addProperties(exprs);
        query.and(tableJoin.getWhere());
        query.getQuery().executeSQL(session, MapFact.EMPTYORDER(), 0, false, DataSession.emptyEnv(owner), result);
    }

    public static boolean checkClasses(ObjectClassSet classSet, CustomClass inconsistentTableClass, Result<Boolean> mRereadChange, RegisterClassRemove classRemove, long timestamp) {
        if(!classRemove.removedAfterChecked(inconsistentTableClass, timestamp)) {
            mRereadChange.set(false);
            return false;
        }
            
        ValueClassSet tableClasses = inconsistentTableClass.getUpSet();
        boolean result = false;
        if(DataSession.fitClasses(tableClasses, classSet)) { // перечитываем только если fitClasses (так как если есть инородные классы - delete все равно сработает, классы могут остаться старыми)
            result = true;
            mRereadChange.set(true); // тоже помечаем на перечитывание изменений, хотя на самом деле, если они не обновяться никакого перечитывания не будет, оптимизатор уберет это условие 
        } else {
            // если все не подходят (notFit) не перечитываем изменения (assert'ся что все равно все изменяются)
            mRereadChange.set(DataSession.notFitClasses(tableClasses, classSet)); // если mixed - надо перечитывать значения (классы при этом не перечитываем пока)
        }
        return result;
    }
    
    private static <B extends Field, T extends B> ImMap<T, ObjectValueClassSet> splitRead(ImSet<T> fields, Function<T, AndClassSet> fieldClasses, boolean inconsistent, ImMap<B, ValueClass> inconsistentTableClasses, MExclSet<B> mInconsistentCheckChanges, RegisterClassRemove classRemove, long timestamp) {
        MExclMap<T, ObjectValueClassSet> mObjectFields = MapFact.mExclMapMax(fields.size());
        for(T field : fields) {
            if(field.type instanceof ObjectType) {
                ObjectClassSet classSet = (ObjectClassSet)fieldClasses.apply(field);
                ObjectValueClassSet valueClassSet = classSet.getValueClassSet();
                // если есть unknown или complex или inconsistent
                boolean checkClasses;
                ValueClass inconsistentTableClass;
                if(inconsistent && (inconsistentTableClass = inconsistentTableClasses.get(field)) != null) { // проверка для correlations
                    Result<Boolean> rereadChange = new Result<>();
                    checkClasses = checkClasses(classSet, (CustomClass)inconsistentTableClass, rereadChange, classRemove, timestamp);
                    if(rereadChange.result)
                        mInconsistentCheckChanges.exclAdd(field);
                } else {
                    checkClasses = (!BaseUtils.hashEquals(classSet, valueClassSet) && !valueClassSet.isEmpty()) || (valueClassSet.hasComplex() && valueClassSet.getSetConcreteChildren().size() > 1); 
                }
                if(checkClasses)
                    mObjectFields.exclAdd(field, valueClassSet);
            } else
                if(!(field.type instanceof DataClass))
                    return null; // concatenate type
        }
        return mObjectFields.immutable();
    }

    public ImMap<ImMap<KeyField, ConcreteClass>,ImMap<PropertyField,ConcreteClass>> readClasses(SQLSession session, final BaseClass baseClass, Result<ImSet<KeyField>> resultKeys, Result<ImSet<PropertyField>> resultProps, OperationOwner owner, boolean inconsistent, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> inconsistentRereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        MExclSet<Field> mInconsistentRereadChanges = SetFact.mExclSetMax(getTableKeys().size() + properties.size()); 
        final ImMap<KeyField, ObjectValueClassSet> objectKeyClasses = splitRead(getTableKeys(), classes.getCommonClasses(getTableKeys()).fnGetValue(), inconsistent, inconsistentTableClasses, mInconsistentRereadChanges, classRemove, timestamp);
        if(objectKeyClasses == null) {
            if(inconsistent)
                inconsistentRereadChanges.set(SetFact.EMPTY());
            return null;
        }
        final ImMap<PropertyField, ObjectValueClassSet> objectPropClasses = splitRead(properties, value -> propertyClasses.get(value).getCommonClass(value), inconsistent, inconsistentTableClasses, mInconsistentRereadChanges, classRemove, timestamp);
        if(objectPropClasses == null) {
            if(inconsistent)
                inconsistentRereadChanges.set(SetFact.EMPTY());
            return null;
        }

        final ImSet<KeyField> objectKeys = objectKeyClasses.keys();
        final ImSet<PropertyField> objectProps = objectPropClasses.keys();
        resultKeys.set(objectKeys);
        resultProps.set(objectProps);
        if(inconsistent)
            inconsistentRereadChanges.set(mInconsistentRereadChanges.immutable());

        if(objectKeyClasses.isEmpty() && objectPropClasses.isEmpty()) // no complex
            return MapFact.singleton(MapFact.EMPTY(), MapFact.EMPTY());

        ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        final lsfusion.server.data.query.build.Join<PropertyField> tableJoin = join(mapKeys);

        ImRevMap<KeyField, KeyExpr> objectMapKeys = mapKeys.filterRev(objectKeys);
        ImRevMap<Field, KeyExpr> classKeys = MapFact.addRevExcl(objectMapKeys, KeyExpr.getMapKeys(objectProps));
        ImMap<Field, Expr> fieldExprs = MapFact.addExcl(objectMapKeys, objectProps.mapValues(new Function<PropertyField, Expr>() {
            public Expr apply(PropertyField value) {
                return tableJoin.getExpr(value);
            }}));

        final ValueExpr nullExpr = new ValueExpr(-2L, baseClass.unknown);
        final ValueExpr unknownExpr = new ValueExpr(-1L, baseClass.unknown);
        final ImMap<Field, ObjectValueClassSet> fieldClasses = MapFact.addExcl(objectKeyClasses, objectPropClasses);
        final IsClassType classType = inconsistent ? IsClassType.INCONSISTENT : IsClassType.CONSISTENT;
        BiFunction<Field, Expr, Expr> classExpr = (key, value) -> value.classExpr(fieldClasses.get(key), classType).nvl(unknownExpr).ifElse(value.getWhere(), nullExpr);
        ImMap<Field, Expr> group = fieldExprs.mapValues(classExpr);

        ImSet<ImMap<Field, ConcreteClass>> readClasses = new Query<>(classKeys, GroupExpr.create(group, tableJoin.getWhere(), classKeys).getWhere()).execute(session, owner).keyOrderSet().getSet()
                .mapSetValues(value -> value.filterFnValues(element -> ((Long) element) != -2)
                .mapValues((key, id) -> baseClass.findConcreteClassID((Long) id, -1)));

        return readClasses.mapKeyValues(value -> value.filter(objectKeys), value -> value.filter(objectProps));
    }

    /*         final Result<ImMap<KeyField, DataClass>> dataKeys = new Result<ImMap<KeyField, DataClass>>();
        final Result<ImMap<PropertyField, DataClass>> dataProps = new Result<ImMap<PropertyField, DataClass>>();
        final ImSet<KeyField> objectKeys = splitData(keys.getSet(), dataKeys);
        if(objectKeys == null)
            return null;
        final ImSet<PropertyField> objectProps = splitData(properties, dataProps);
        if(objectProps == null)
            return null;

        ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        ImRevMap<KeyField, KeyExpr> objectMapKeys = mapKeys.filterRev(objectKeys);

        final lsfusion.server.data.query.build.Join<PropertyField> tableJoin = join(mapKeys);

        final ValueExpr unknownExpr = new ValueExpr(-1, baseClass.unknown);
        Function<Expr, Expr> classExpr = new Function<Expr, Expr>() {
            public Expr apply(Expr value) {
                return value.classExpr(baseClass).nvl(unknownExpr);
            }};

        ImMap<PropertyField, Expr> objectMapProps = objectProps.mapValues(new Function<Expr, PropertyField>() {
            public Expr apply(PropertyField value) {
                return tableJoin.getExpr(value);
            }
        });
        ImMap<Field, Expr> group = MapFact.addExcl(objectMapKeys, objectMapProps).mapValues(classExpr);

        Function<ImMap<Field, ConcreteClass>, ImMap<Field, Object>> findClasses = new Function<ImMap<Field, ConcreteClass>, ImMap<Field, Object>>() {
            public ImMap<Field, ConcreteClass> apply(ImMap<Field, Object> value) {
                return value.mapValues(new Function<ConcreteClass, Object>() {
                    public ConcreteClass apply(Object id) {
                        return baseClass.findConcreteClassID(((Integer) id) != -1 ? (Integer) id : null);
                    }
                });
            }};

        KeyExpr propKey = new KeyExpr("prop");
        MExclMap<PropertyField, ClassWhere<Field>> mPropertyClasses = MapFact.mExclMap(properties.size()); // из-за exception'а в том числе
        for(final PropertyField prop : properties) {
            boolean isObject = objectProps.contains(prop);

            ImRevMap<Field, KeyExpr> classKeys = BaseUtils.immutableCast(objectMapKeys);
            if(isObject)
                classKeys = classKeys.addRevExcl(prop, propKey);

            ImSet<ImMap<Field, ConcreteClass>> readClasses = new Query<Field, Object>(classKeys, GroupExpr.create(group.filter(classKeys.keys()), objectMapProps.get(prop).getWhere(), classKeys).getWhere()).execute(session).keyOrderSet().getSet().mapSetValues(findClasses);

            ClassWhere<Field> where = ClassWhere.FALSE();
            for(ImMap<Field, ConcreteClass> readClass : readClasses) {
                ImMap<Field, ConcreteClass> resultClass = MapFact.addExcl(readClass, dataKeys.result);
                if(!isObject)
                    resultClass = resultClass.addExcl(prop, dataProps.result.get(prop));
                where = where.or(new ClassWhere<Field>(resultClass));
            }
            mPropertyClasses.exclAdd(prop, where);
        }

        // в общем-то дублирование верхнего кода
        ImSet<ImMap<KeyField, ConcreteClass>> readClasses = new Query<KeyField, Object>(objectMapKeys, GroupExpr.create(group.filter(objectMapKeys.keys()), tableJoin.getWhere(), objectMapKeys).getWhere()).execute(session).
                keyOrderSet().getSet().mapSetValues(BaseUtils.<Function<ImMap<KeyField, ConcreteClass>, ImMap<KeyField, Object>>>immutableCast(findClasses));

        ClassWhere<KeyField> where = ClassWhere.FALSE();
        for(ImMap<KeyField, ConcreteClass> readClass : readClasses)
            where = where.or(new ClassWhere<KeyField>(MapFact.addExcl(readClass, dataKeys.result)));
        mPropertyClasses.exclAdd(prop, where);
    */

    protected ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
    protected ImMap<PropertyField,ClassWhere<Field>> propertyClasses;

    public boolean calcTwins(TwinImmutableObject o) {
        return classes.equals(((Table)o).classes) && propertyClasses.equals(((Table) o).propertyClasses);
    }

    public int immutableHashCode() {
        return classes.hashCode() * 31 + propertyClasses.hashCode();
    }

    public Query<KeyField, PropertyField> getQuery() {
        QueryBuilder<KeyField,PropertyField> query = new QueryBuilder<>(this);
        lsfusion.server.data.query.build.Join<PropertyField> join = join(query.getMapExprs());
        query.and(join.getWhere());
        query.addProperties(join.getExprs());
        return query.getQuery();
    }

    public void out(SQLSession session) throws SQLException, SQLHandledException {
        getQuery().outSelect(session);
    }

    public void outClasses(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        getQuery().outClassesSelect(session, baseClass);
    }
    public void outClasses(SQLSession session, BaseClass baseClass, Processor<String> processor) throws SQLException, SQLHandledException {
        getQuery().outClassesSelect(session, baseClass, processor);
    }

    public lsfusion.server.data.query.build.Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {
        return new AddPullWheres<KeyField, lsfusion.server.data.query.build.Join<PropertyField>>() {
            protected MCaseList<lsfusion.server.data.query.build.Join<PropertyField>, ?, ?> initCaseList(boolean exclusive) {
                return new MJoinCaseList<>(properties, exclusive);
            }
            protected lsfusion.server.data.query.build.Join<PropertyField> initEmpty() {
                return new NullJoin<>(properties);
            }
            protected lsfusion.server.data.query.build.Join<PropertyField> proceedIf(Where ifWhere, lsfusion.server.data.query.build.Join<PropertyField> resultTrue, lsfusion.server.data.query.build.Join<PropertyField> resultFalse) {
                return new IfJoin<>(ifWhere, resultTrue, resultFalse);
            }

            protected lsfusion.server.data.query.build.Join<PropertyField> proceedBase(ImMap<KeyField, BaseExpr> joinBase) {
                return joinAnd(joinBase);
            }
        }.proceed(joinImplement);
    }

    protected boolean isIndexed(PropertyField field) {
        return false;
    }

    public Join joinAnd(ImMap<KeyField, ? extends BaseExpr> joinImplement) {
        return new Join(joinImplement);
    }

    public int hash(HashContext hashContext) {
        return hashCode();
    }

    protected Table translate(MapTranslate translator) {
        return this;
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.EMPTY();
    }

    protected boolean isFull() {
        return false;
    }

    public StatKeys<KeyField> getStatKeys() {
        return StatKeys.create(getTableStatKeys());
    }

    @IdentityLazy
    protected ImSet<ImOrderSet<Field>> getIndexes() {
        return SQLSession.getKeyIndexes(keys);
    }

    private static class PushResult {
        private final Cost cost;
        private final ImSet<KeyField> pushedKeys;
        private final ImSet<BaseExpr> pushedProps;

        public PushResult(Cost cost, ImSet<KeyField> pushedKeys, ImSet<BaseExpr> pushedProps) {
            this.cost = cost;
            this.pushedKeys = pushedKeys;
            this.pushedProps = pushedProps;
        }

        // only for check caches
        public boolean equals(Object o) {
            return this == o || o instanceof PushResult && cost.equals(((PushResult) o).cost) && Objects.equals(pushedKeys, ((PushResult) o).pushedKeys) && Objects.equals(pushedProps, ((PushResult) o).pushedProps);
        }

        public int hashCode() {
            return Objects.hash(cost, pushedKeys, pushedProps);
        }
    }

    public class Join extends AbstractOuterContext<Join> implements InnerJoin<KeyField, Join>, lsfusion.server.data.query.build.Join<PropertyField> {

        public final ImMap<KeyField, BaseExpr> joins;

        public ImMap<KeyField, BaseExpr> getJoins() {
            return joins;
        }
        public StatKeys<KeyField> getInnerStatKeys(StatType type) {
            return Table.this.getStatKeys();
        }

        public RecursiveTable getRecursiveTable() {
            return Table.this instanceof RecursiveTable ? (RecursiveTable)Table.this : null;
        }

        public StatKeys<KeyField> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
            return QueryJoin.getStatKeys(this, keyStat, type);
        }

        @IdentityLazy
        public PushResult getPushedCost(Stat pushStat, ImMap<KeyField, Stat> pushKeys, ImMap<KeyField, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps) {
            ImRevMap<PropertyField, BaseExpr> mapProps = pushProps.keys().mapRevKeys((BaseExpr value) -> {
                if(value instanceof IsClassExpr)
                    value = ((IsClassExpr)value).getInnerJoinExpr();
                return ((Expr) value).property;
            });
            ImMap<PropertyField, Stat> pushPropFields = mapProps.join(pushProps);
            ImMap<Field, Stat> pushFieldStats = MapFact.addExcl(pushPropFields, pushKeys);

            ImMap<Field, Stat> pushFieldNotNulls = MapFact.addExcl(pushPropFields.keys().toMap(pushStat), pushNotNullKeys);

            TableStatKeys tableStatKeys = getTableStatKeys();
            Stat thisStat = tableStatKeys.getRows();
            ImMap<PropertyField, PropStat> tableStatProps = getStatProps();
            ImMap<Field, Stat> thisFieldStats = MapFact.addExcl(tableStatProps.mapValues(value -> value.distinct), tableStatKeys.getDistinct());

            ImMap<Field, Stat> thisFieldNotNulls = MapFact.addExcl(tableStatProps.mapValues(value -> value.notNull), tableStatKeys.getDistinct().keys().toMap(thisStat));

            Stat bestStat = thisStat; // интересуют результаты меньшие всего пробега по таблице
            ImOrderSet<Field> bestIndex = null;
            Iterable<ImOrderSet<Field>> indexes = getIndexes();
            for(ImOrderSet<Field> index : indexes) {
                Stat pushAdjStat = pushStat; // при predicate push down'е надо сделать еще min с mult distinct.min(notNull)
                Stat thisAdjStat = thisStat;

                int edgesCount = index.size();
                Stat[] pushEdgeStats = new Stat[edgesCount];
                Stat[] thisEdgeStats = new Stat[edgesCount];
                int i=0;
                for(;i<edgesCount;i++) {
                    Field field = index.get(i);

                    Stat pushFieldStat = pushFieldStats.get(field);
                    if(pushFieldStat == null) // не найден предикат, значит только верхнюю часть индекса использовать
                        break;

                    Stat thisFieldStat = thisFieldStats.get(field);

                    pushEdgeStats[i] = pushFieldStat;
                    thisEdgeStats[i] = thisFieldStat;

                    Stat thisNotNull = thisFieldNotNulls.get(field);
                    if(thisNotNull != null)
                        thisAdjStat = thisAdjStat.min(thisNotNull);
                    Stat pushNotNull = pushFieldNotNulls.get(field);
                    if(pushNotNull != null)
                        pushAdjStat = pushAdjStat.min(pushNotNull);
                }
                Stat newStat = WhereJoins.calcEstJoinStat(pushAdjStat, thisAdjStat, i, pushEdgeStats, thisEdgeStats, false, null, null, null); // пока не поддерживается predicate push down

                if(newStat.less(bestStat)) {
                    bestStat = newStat;
                    bestIndex = index.subOrder(0, i);
                }
            }
            if(bestIndex != null) {
                assert bestStat.less(thisStat);
                ImSet<BaseExpr> pushedProps = BaseUtils.<ImSet<PropertyField>>immutableCast(bestIndex.getSet().filterFn(element -> element instanceof PropertyField)).mapRev(mapProps);
                // пока не заполняем pushedKeys так как нет predicate push down в таблицу
                return new PushResult(new Cost(bestStat), null, pushedProps);
            } else // в худшем случае побежим по таблице
                return new PushResult(new Cost(thisStat), null, null);
        }

        @Override
        public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<KeyField, Stat> pushKeys, ImMap<KeyField, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<KeyField>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
            PushResult result = getPushedCost(pushStat, pushKeys, pushNotNullKeys, pushProps);
            if(rPushedKeys != null)
                rPushedKeys.set(result.pushedKeys);
            if(rPushedProps != null)
                rPushedProps.set(result.pushedProps);
            return result.cost;
        }

        public Join(ImMap<KeyField, ? extends BaseExpr> joins) {
            this.joins = (ImMap<KeyField, BaseExpr>) joins;
            assert (joins.size()==keys.size());
        }

        public ClassWhere<KeyField> getClassWhere() {
            return Table.this.getClasses();
        }

        @TwinLazy
        public InnerFollows<KeyField> getInnerFollows() {
            if(Settings.get().isDisableInnerFollows() || Table.this instanceof Property.VirtualTable || (Table.this instanceof RecursiveTable && ((RecursiveTable) Table.this).noInnerFollows))
                return InnerFollows.EMPTY();

            return new InnerFollows<>(getClassWhere(), keys.getSet(), Table.this);
        }

        @TwinLazy
        private ClassExprWhere getJoinsClassWhere() {
            return lsfusion.server.data.expr.BaseExpr.getNotNullClassWhere(joins);
        }

        public boolean hasExprFollowsWithoutNotNull() {
            return InnerExpr.hasExprFollowsWithoutNotNull(this);
        }

        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(this);
        }

        @TwinLazy
        public lsfusion.server.data.expr.Expr getExpr(PropertyField property) {
            return BaseExpr.create(new Expr(property));
        }
        @TwinLazy
        public Where getWhere() {
            return DataWhere.create(new IsIn());
        }

        // интерфейсы для translateDirect
        public Expr getDirectExpr(PropertyField property) {
            return new Expr(property);
        }
        public IsIn getDirectWhere() {
            return new IsIn();
        }

        public ImSet<PropertyField> getProperties() {
            return Table.this.properties;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
        }

        public ImMap<PropertyField, lsfusion.server.data.expr.Expr> getExprs() {
            return AbstractJoin.getExprs(this);
        }

        public lsfusion.server.data.query.build.Join<PropertyField> and(Where where) {
            return AbstractJoin.and(this, where);
        }

        public lsfusion.server.data.query.build.Join<PropertyField> translateValues(MapValuesTranslate translate) {
            return AbstractJoin.translateValues(this, translate);
        }
        public lsfusion.server.data.query.build.Join<PropertyField> translateRemoveValues(MapValuesTranslate translate) {
            return translateOuter(translate.mapKeys());
        }
        
        public boolean isSession() {
            return Table.this instanceof SessionTable;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return Table.this.hashOuter(hashContext)*31 + AbstractSourceJoin.hashOuter(joins, hashContext);
        }

        protected Join translate(MapTranslate translator) {
            return Table.this.translateOuter(translator).joinAnd(translator.translateDirect(joins));
        }
        public Join translateOuter(MapTranslate translator) {
            return aspectTranslate(translator);
        }

        @ParamInstanceLazy
        public lsfusion.server.data.query.build.Join<PropertyField> translateExpr(ExprTranslator translator) {
            return join(translator.translate(joins));
        }

        public lsfusion.server.data.query.build.Join<PropertyField> packFollowFalse(Where falseWhere) {
            ImMap<KeyField, lsfusion.server.data.expr.Expr> packJoins = BaseExpr.packPushFollowFalse(joins, falseWhere);
            if(!BaseUtils.hashEquals(packJoins, joins))
                return join(packJoins);
            else
                return this;
        }

        public String getQuerySource(CompileSource source) {
            return Table.this.getQuerySource(source);
        }

        private Table getTable() {
            return Table.this;
        }

        public InnerExpr getInnerExpr(WhereJoin join) {
            return QueryJoin.getInnerExpr(this, join);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.addExcl(joins.values().toSet(), Table.this);
        }

        public class IsIn extends DataWhere implements FJData {

            public String getFirstKey(SQLSyntax syntax) {
                if(isSingle())
                    return "dumb";
                return keys.iterator().next().getName(syntax);
            }

            public ImSet<OuterContext> calculateOuterDepends() {
                return SetFact.singleton(Join.this);
            }

            public Join getJoin() {
                return Join.this;
            }

            public InnerJoin getFJGroup() {
                return Join.this;
            }

            protected void fillDataJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
                joins.add(this,andWhere);
            }

            public String getSource(CompileSource compile) {
                return compile.getSource(this);
            }

            public String toString() {
                return "IN JOIN " + Join.this.toString();
            }

            protected Where translate(MapTranslate translator) {
                return Join.this.translateOuter(translator).getDirectWhere();
            }
            public Where translate(ExprTranslator translator) {
                return Join.this.translateExpr(translator).getWhere();
            }
            @Override
            public Where packFollowFalse(Where falseWhere) {
                return Join.this.packFollowFalse(falseWhere).getWhere();
            }

            protected ImSet<NullableExprInterface> getExprFollows() {
                return Join.this.getExprFollows(NullableExpr.FOLLOW, true);
            }

            public lsfusion.server.data.expr.Expr getFJExpr() {
                return ValueExpr.get(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<lsfusion.server.data.expr.Expr> orderTop, GroupJoinsWheres.Type type) {
                return groupDataJoinsWheres(Join.this, type);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.mapClasses(joins).and(getJoinsClassWhere());
            }

            public boolean calcTwins(TwinImmutableObject o) {
                return Join.this.equals(((IsIn) o).getJoin());
            }

            public int hash(HashContext hashContext) {
                return Join.this.hashOuter(hashContext);
            }

            @Override
            public boolean isClassWhere() {
                return isFull();
            }
        }

        public class Expr extends InnerExpr {

            public final PropertyField property;

            @Override
            public ImSet<OuterContext> calculateOuterDepends() {
                return SetFact.singleton(Join.this);
            }

            // напрямую может конструироваться только при полной уверенности что не null
            private Expr(PropertyField property) {
                this.property = property;
                assert properties.contains(property);
            }

            public lsfusion.server.data.expr.Expr translate(ExprTranslator translator) {
                return Join.this.translateExpr(translator).getExpr(property);
            }

            @Override
            public lsfusion.server.data.expr.Expr packFollowFalse(Where where) {
                return Join.this.packFollowFalse(where).getExpr(property);
            }

            protected Expr translate(MapTranslate translator) {
                return Join.this.translateOuter(translator).getDirectExpr(property);
            }

            public String toString() {
                return Join.this.toString() + "." + property;
            }

            public Type getType(KeyType keyType) {
                return property.type;
            }
            public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
                return propertyClasses.get(property).getTypeStat(property, forJoin);
            }

            public NotNull calculateNotNullWhere() {
                return new NotNull();
            }

            public boolean calcTwins(TwinImmutableObject o) {
                return Join.this.equals(((Expr) o).getInnerJoin()) && property.equals(((Expr) o).property);
            }

            protected boolean isComplex() {
                return true;
            }
            public int hash(HashContext hashContext) {
                return Join.this.hashOuter(hashContext)*31+property.hashCode();
            }

            public String getSource(CompileSource compile, boolean needValue) {
                return compile.getSource(this);
            }

            public class NotNull extends InnerExpr.NotNull {

                @Override
                protected ImSet<DataWhere> calculateFollows() {
                    return SetFact.addExcl(super.calculateFollows(), (DataWhere) Join.this.getWhere());
                }

                public ClassExprWhere calculateClassWhere() {
                    return propertyClasses.get(property).mapClasses(MapFact.addExcl(joins, property, Expr.this)).and(Join.this.getJoinsClassWhere());
                }
            }

            @Override
            public void fillFollowSet(MSet<DataWhere> fillSet) {
                super.fillFollowSet(fillSet);
                fillSet.add((DataWhere) Join.this.getWhere()); // ClassCast can be when property is materialized in wrong classes table (when there are wrong explicit classes : f(B b) = b IS A MATERIALIZED)
            }

            public Table.Join getInnerJoin() {
                return Join.this;
            }

            public PropStat getInnerStatValue(KeyStat keyStat, StatType type) {
                return getStatProps().get(property);
            }

            @Override
            public boolean isIndexed() {
                return Table.this.isIndexed(property);
            }

            @Override
            public boolean hasALotOfNulls() {
                assert isIndexed();
                Stat notNull = getStatProps().get(property).notNull;
                return notNull != null && notNull.less(Table.this.getStatRows());
            }
        }

        @Override
        public String toString() {
            return Table.this.toString();
        }
    }

    public ClassWhere<KeyField> getClasses() {
        return classes;
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        return propertyClasses.get(property);
    }
}

/* для работы с cross-column статистикой

    public final Set<List<List<Field>>> indexes; // предполагается безпрефиксные

    public static String getTuple(List<String> list) {
        assert list.size() > 0;
        if(list.size()==1)
            return single(list);
    }

    private static <K> void recBuildMaps(int i, List<List<Field>> index, Map<K, ? extends Field> fields, Stack<List<K>> current, RecIndexTuples<K> result) {

        result.proceed(fields); // нужно еще промежуточные покрытия добавлять, чтобы комбинировать индексы

        if(i >= index.size())
            return;

        List<Field> tuple = index.get(i);
        List<K> map = dfdf; Map<K, Field> rest = dsds;

        current.push(map); // итерироваться
        recBuildMaps(i+1, index, rest, current, result);
        current.pop();
    }
    private static <K> void recBuildMaps(List<List<Field>> index, Map<K, ? extends Field> fields, RecIndexTuples<K> result) {
        recBuildMaps(0, index, fields, new Stack<List<K>>(), result);
    }

    public static interface RecIndexTuples<K> {
        void proceed(Map<K, ? extends Field> restFields);
    }
    public static <K> void recIndexTuples(final int i, final List<List<List<Field>>> indexes, final Map<K, ? extends Field> fields, final Stack<List<List<K>>> current, final RecIndexTuples<K> result) {
        if(i >= indexes.size()) {
            result.proceed(fields);
            return;
        }

        recBuildMaps(indexes.get(i), fields, new RecIndexTuples<K>() {
            public void proceed(Map<K, ? extends Field> restFields) {
                recIndexTuples(i + 1, indexes, fields, current, result);
            }
        });
    }

    public <K> void recIndexTuples(Map<K, ? extends Field> fields, Stack<List<List<K>>> current, RecIndexTuples<K> result) {
        recIndexTuples(0, new ArrayList<List<List<Field>>>(indexes), fields, current, result);
    }
*/ 