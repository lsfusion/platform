package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ParamInstanceLazy;
import lsfusion.server.base.caches.TwinLazy;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.classes.IsClassExpr;
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
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.query.RecursiveTable;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.cases.MCaseList;
import lsfusion.server.data.expr.where.cases.MJoinCaseList;
import lsfusion.server.data.expr.where.ifs.IfJoin;
import lsfusion.server.data.expr.where.ifs.NullJoin;
import lsfusion.server.data.expr.where.pull.AddPullWheres;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.struct.OrConcatenateClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;
import lsfusion.server.logics.classes.user.set.OrClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;

import java.util.Objects;

public abstract class Table extends AbstractOuterContext<Table> implements MapKeysInterface<KeyField> {
    
    public ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public ImOrderSet<KeyField> getOrderTableKeys() {
        return keys;
    }
    public ImSet<KeyField> getTableKeys() {
        return keys.getSet();
    }

    public Stat getStatRows() {
        return getTableStatKeys().getRows();
    }
    public abstract TableStatKeys getTableStatKeys();

    public abstract ClassWhere<KeyField> getClasses();

    public abstract PropStat getStatProp(PropertyField property);
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);

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

    @Override
    public int immutableHashCode() {
        throw new UnsupportedOperationException();
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
            AndClassSet commonClass = table.getClassWhere(field).getCommonClass(field);
            if(commonClass != null)
                resultStat = getObjectPropFieldStat(commonClass);
            else
                assert table.getClassWhere(field).isFalse();
        }
        if(resultStat == null)
            resultStat = field.type.getTypeStat(false);
        return resultStat;
    }

    protected static TableStatKeys getStatKeys(Table table, final int count) { // для мн-го наследования
        ImMap<KeyField, Integer> statMap = table.getTableKeys().mapValues((KeyField value) -> BaseUtils.min(count, getKeyFieldStat(value, table)));
        return TableStatKeys.createForTable(count, statMap);
    }

    protected static PropStat getStatProp(Table table, PropertyField field) { // для мн-го наследования
        Stat rows = table.getTableStatKeys().getRows();
        return new PropStat(InnerExpr.getAdjustStatValue(rows, getPropFieldStat(field, table)));
    }

    protected static ClassWhere<Field> getClassWhere(Table table, PropertyField property) {
        return new ClassWhere<Field>(property, (DataClass)property.type).and(BaseUtils.<ClassWhere<Field>>immutableCast(table.getClasses()));
    }

    protected static ClassWhere<KeyField> getClassWhere(Table table) {
        return new ClassWhere<>(table.getTableKeys().mapValues((KeyField key) -> (DataClass)key.type));
    }


    public boolean isSingle() {
        return keys.isEmpty();
    }

    public ImRevMap<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(getTableKeys());
    }

    protected Table() {
        this(SetFact.EMPTYORDER());
    }

    protected Table(ImOrderSet<KeyField> keys) {
        this.keys = keys;
    }

    public abstract String getQuerySource(CompileSource source);
    public String getQuerySource(CompileSource source, ImMap<KeyField, String> lateralSources) {
        return getQuerySource(source);
    }
    public ImSet<KeyField> getLaterals() {
        return SetFact.EMPTY();
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
    
    public lsfusion.server.data.query.build.Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {
        return new AddPullWheres<KeyField, lsfusion.server.data.query.build.Join<PropertyField>>() {
            protected MCaseList<lsfusion.server.data.query.build.Join<PropertyField>, ?, ?> initCaseList(boolean exclusive) {
                return new MJoinCaseList<>(exclusive);
            }
            protected lsfusion.server.data.query.build.Join<PropertyField> initEmpty() {
                return NullJoin.getInstance();
            }
            protected lsfusion.server.data.query.build.Join<PropertyField> proceedIf(Where ifWhere, lsfusion.server.data.query.build.Join<PropertyField> resultTrue, lsfusion.server.data.query.build.Join<PropertyField> resultFalse) {
                return new IfJoin<>(ifWhere, resultTrue, resultFalse);
            }

            protected lsfusion.server.data.query.build.Join<PropertyField> proceedBase(ImMap<KeyField, BaseExpr> joinBase) {
                return joinAnd(joinBase);
            }
        }.proceed(joinImplement);
    }

    protected IndexType getIndexType(PropertyField field) {
        return null;
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

        public StatKeys<KeyField> getStatKeys(KeyStat keyStat, StatType type) {
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

                    Stat thisFieldStat;
                    Stat thisNotNull;
                    if (field instanceof PropertyField) {
                        PropStat statProp = getStatProp((PropertyField) field);
                        thisFieldStat = statProp.distinct;
                        thisNotNull = statProp.notNull;
                    } else {
                        thisFieldStat = tableStatKeys.getDistinct().get((KeyField) field);
                        thisNotNull = thisStat;
                    }

                    pushEdgeStats[i] = pushFieldStat;
                    thisEdgeStats[i] = thisFieldStat;

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

        public boolean calcTwins(TwinImmutableObject o) {
            return Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
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

        public String getQuerySource(CompileSource source, ImMap<KeyField, String> joinExprs) {
            return Table.this.getQuerySource(source, joinExprs);
        }

        public ImSet<KeyField> getLaterals() {
            return Table.this.getLaterals();
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

            public KeyField getFirstKey() {
                if(isSingle())
                    return KeyField.dumb;

                return keys.get(0);
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
                return getClasses().mapClasses(joins).and(getJoinsClassWhere());
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
//                assert properties.contains(property);
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
                return Table.this.getClassWhere(property).getTypeStat(property, forJoin);
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
                    return Table.this.getClassWhere(property).mapClasses(MapFact.addExcl(joins, property, Expr.this)).and(Join.this.getJoinsClassWhere());
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
                return getStatProp(property);
            }

            @Override
            protected IndexType getIndexType() {
                return Table.this.getIndexType(property);
            }

            @Override
            public boolean hasALotOfNulls() {
                Stat notNull = getStatProp(property).notNull;
                return notNull != null && notNull.less(Table.this.getStatRows());
            }
        }

        @Override
        public String toString() {
            return Table.this.toString();
        }
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