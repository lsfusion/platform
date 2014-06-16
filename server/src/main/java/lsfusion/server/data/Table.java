package lsfusion.server.data;

import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.caches.TwinLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.expr.where.cases.MCaseList;
import lsfusion.server.data.expr.where.cases.MJoinCaseList;
import lsfusion.server.data.expr.where.ifs.IfJoin;
import lsfusion.server.data.expr.where.ifs.NullJoin;
import lsfusion.server.data.expr.where.pull.AddPullWheres;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public abstract class Table extends AbstractOuterContext<Table> implements MapKeysInterface<KeyField> {
    protected String name;
    public void setName(String name) {
        this.name = name;
    }
    
    public ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public ImOrderSet<KeyField> getOrderTableKeys() {
        return keys;
    }
    public ImSet<KeyField> getTableKeys() {
        return keys.getSet();
    }
    public ImSet<PropertyField> properties;

    public abstract StatKeys<KeyField> getStatKeys();
    public abstract ImMap<PropertyField, PropStat> getStatProps();

    private static Stat getFieldStat(Field field, Stat defStat) {
        if(field.type instanceof DataClass)
            return defStat.min(((DataClass)field.type).getTypeStat(false));
        else
            return defStat;
    }

    protected static StatKeys<KeyField> getStatKeys(Table table, int count) { // для мн-го наследования
        final Stat stat = new Stat(count);

        ImMap<KeyField, Stat> statMap = table.getTableKeys().mapValues(new GetValue<Stat, KeyField>() {
            public Stat getMapValue(KeyField value) {
                return getFieldStat(value, stat);
            }});
        DistinctKeys<KeyField> distinctKeys = new DistinctKeys<KeyField>(statMap);

        return new StatKeys<KeyField>(distinctKeys.getMax().min(stat), distinctKeys);
    }

    protected static ImMap<PropertyField, PropStat> getStatProps(Table table, final Stat stat) { // для мн-го наследования
        return table.properties.mapValues(new GetValue<PropStat, PropertyField>() {
            public PropStat getMapValue(PropertyField prop) {
                return new PropStat(getFieldStat(prop, stat));
            }});
    }
    
    protected static ImMap<PropertyField, PropStat> getStatProps(Table table, int count) { // для мн-го наследования
        return getStatProps(table, new Stat(count));
    }

    public boolean isSingle() {
        return keys.size()==0;
    }

    public ImRevMap<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(getTableKeys());
    }

    protected Table(String name) {
        this(name, SetFact.<KeyField>EMPTYORDER(), SetFact.<PropertyField>EMPTY(), ClassWhere.<KeyField>FALSE(), MapFact.<PropertyField, ClassWhere<Field>>EMPTY());
    }

    protected Table(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties,ClassWhere<KeyField> classes,ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        this.name = name;
        this.keys = keys;
        this.properties = properties;
        this.classes = classes;
        this.propertyClasses = propertyClasses;

        // assert classes.fitTypes();
        assert (this instanceof SerializedTable || this instanceof ImplementTable.InconsistentTable) || classes.isEqual(keys.getSet()) && propClassesFull(); // см. ClassExprWhere.getKeyType
    }

    private <K extends Field> ImMap<K, DataClass> getDataFields(ImSet<K> fields) {
        return BaseUtils.immutableCast(fields.mapValues(new GetValue<Type, K>() {
            public Type getMapValue(K value) {
                return value.type;
            }
        }).filterFnValues(new SFunctionSet<Type>() {
            public boolean contains(Type element) {
                return element instanceof DataClass;
            }
        }));
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

    public String getName(SQLSyntax syntax) {
        return syntax.getTableName(name);
    }

    public String getQueryName(CompileSource source) {
        return getName(source.syntax);
    }

    public String toString() {
        return getName();
    }
    
    public String getName() {
        return name; 
    }

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

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(name);
            outStream.writeInt(keys.size());
        for(KeyField key : keys)
            key.serialize(outStream);
        outStream.writeInt(properties.size());
        for(PropertyField property : properties)
            property.serialize(outStream);
    }

    protected void initBaseClasses(final BaseClass baseClass) {
        final ImMap<KeyField, AndClassSet> baseClasses = getTableKeys().mapValues(new GetValue<AndClassSet, KeyField>() {
            public AndClassSet getMapValue(KeyField value) {
                return value.type.getBaseClassSet(baseClass);
            }});
        classes = new ClassWhere<KeyField>(baseClasses);

        propertyClasses = properties.mapValues(new GetValue<ClassWhere<Field>, PropertyField>() {
            public ClassWhere<Field> getMapValue(PropertyField value) {
                return new ClassWhere<Field>(MapFact.addExcl(baseClasses, value, value.type.getBaseClassSet(baseClass)));
            }});
    }

    public Table(DataInputStream inStream, final BaseClass baseClass, int version) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        MOrderExclSet<KeyField> mKeys = SetFact.mOrderExclSet(keysNum); // десериализация, поэтому порядок важен
        for(int i=0;i<keysNum;i++)
            mKeys.exclAdd((KeyField) Field.deserialize(inStream, version));
        keys = mKeys.immutableOrder();
        int propNum = inStream.readInt();
        MExclSet<PropertyField> mProperties = SetFact.mExclSet(propNum);
        for(int i=0;i<propNum;i++)
            mProperties.exclAdd((PropertyField) Field.deserialize(inStream, version));
        properties = mProperties.immutable();

        initBaseClasses(baseClass);
    }

    public ImOrderMap<ImMap<KeyField,DataObject>,ImMap<PropertyField,ObjectValue>> read(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        lsfusion.server.data.query.Join<PropertyField> tableJoin = join(query.getMapExprs());
        query.addProperties(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.executeClasses(session, baseClass, owner);
    }

    public void readData(SQLSession session, BaseClass baseClass, OperationOwner owner, boolean noFilesAndLogs, ResultHandler<KeyField, PropertyField> result) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        lsfusion.server.data.query.Join<PropertyField> tableJoin = join(query.getMapExprs());
        ImMap<PropertyField, Expr> exprs = tableJoin.getExprs();
        if(noFilesAndLogs)
            exprs = exprs.filterFn(new SFunctionSet<PropertyField>() {
                public boolean contains(PropertyField element) {
                    return !(element.type instanceof FileClass || element.getName().contains("_LG_"));
                }});
        query.addProperties(exprs);
        query.and(tableJoin.getWhere());
        query.getQuery().executeSQL(session, MapFact.<PropertyField, Boolean>EMPTYORDER(), 0, DataSession.emptyEnv(owner), result);
    }

    private static <T extends Field> ImSet<T> splitData(ImSet<T> fields, Result<ImMap<T, DataClass>> dataClasses) {
        MExclSet<T> mObjectFields = SetFact.mExclSetMax(fields.size());
        MExclMap<T, DataClass> mDataClasses = MapFact.mExclMapMax(fields.size());
        for(T field : fields) {
            if(field.type instanceof DataClass)
                mDataClasses.exclAdd(field, (DataClass) field.type);
            else
            if(field.type instanceof ObjectType)
                mObjectFields.exclAdd(field);
            else
                return null;
        }
        dataClasses.set(mDataClasses.immutable());
        return mObjectFields.immutable();
    }

    public ImMap<ImMap<KeyField,ConcreteClass>,ImMap<PropertyField,ConcreteClass>> readClasses(SQLSession session, final BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        final Result<ImMap<KeyField, DataClass>> dataKeys = new Result<ImMap<KeyField, DataClass>>();
        final Result<ImMap<PropertyField, DataClass>> dataProps = new Result<ImMap<PropertyField, DataClass>>();
        final ImSet<KeyField> objectKeys = splitData(keys.getSet(), dataKeys);
        if(objectKeys == null)
            return null;
        final ImSet<PropertyField> objectProps = splitData(properties, dataProps);
        if(objectProps == null)
            return null;

        ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        ImRevMap<KeyField, KeyExpr> objectMapKeys = mapKeys.filterRev(objectKeys);
        ImRevMap<Field, KeyExpr> classKeys = MapFact.addRevExcl(objectMapKeys, KeyExpr.getMapKeys(properties));
        if(classKeys.isEmpty())
            return MapFact.singleton(BaseUtils.<ImMap<KeyField, ConcreteClass>>immutableCast(dataKeys.result), BaseUtils.<ImMap<PropertyField, ConcreteClass>>immutableCast(dataProps.result));

        final lsfusion.server.data.query.Join<PropertyField> tableJoin = join(mapKeys);

        final ValueExpr nullExpr = new ValueExpr(-2, baseClass.unknown);
        final ValueExpr unknownExpr = new ValueExpr(-1, baseClass.unknown);
        GetKeyValue<Expr, Field, Expr> classExpr = new GetKeyValue<Expr, Field, Expr>() {
            public Expr getMapValue(Field key, Expr value) {
                Expr resultExpr;
                if((key instanceof PropertyField && !objectProps.contains((PropertyField) key)))
                    resultExpr = unknownExpr;
                else
                    resultExpr = value.classExpr(baseClass).nvl(unknownExpr);
                return resultExpr.ifElse(value.getWhere(), nullExpr);
            }};
        ImMap<Field, Expr> group = MapFact.addExcl(objectMapKeys, properties.mapValues(new GetValue<Expr, PropertyField>() {
            public Expr getMapValue(PropertyField value) {
                return tableJoin.getExpr(value);
            }
        })).mapValues(classExpr);

        ImSet<ImMap<Field, ConcreteClass>> readClasses = new Query<Field, Object>(classKeys, GroupExpr.create(group, tableJoin.getWhere(), classKeys).getWhere()).execute(session, owner).keyOrderSet().getSet().mapSetValues(new GetValue<ImMap<Field, ConcreteClass>, ImMap<Field, Object>>() {
            public ImMap<Field, ConcreteClass> getMapValue(ImMap<Field, Object> value) {
                return value.filterFnValues(new SFunctionSet<Object>() {
                    public boolean contains(Object element) {
                        return ((Integer) element) != -2;
                    }
                }).mapValues(new GetKeyValue<ConcreteClass, Field, Object>() {
                    public ConcreteClass getMapValue(Field key, Object id) {
                        if(key instanceof PropertyField && !objectProps.contains((PropertyField) key))
                            return dataProps.result.get((PropertyField) key);
                        else
                            return baseClass.findConcreteClassID(((Integer) id) != -1 ? (Integer) id : null);
                    }
                });
            }
        });

        return readClasses.mapKeyValues(new GetValue<ImMap<KeyField, ConcreteClass>, ImMap<Field, ConcreteClass>>() {
            public ImMap<KeyField, ConcreteClass> getMapValue(ImMap<Field, ConcreteClass> value) {
                return MapFact.addExcl(value.filter(objectKeys), dataKeys.result);
            }}, new GetValue<ImMap<PropertyField, ConcreteClass>, ImMap<Field, ConcreteClass>>() {
            public ImMap<PropertyField, ConcreteClass> getMapValue(ImMap<Field, ConcreteClass> value) {
                return value.filter(properties);
            }});
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

        final lsfusion.server.data.query.Join<PropertyField> tableJoin = join(mapKeys);

        final ValueExpr unknownExpr = new ValueExpr(-1, baseClass.unknown);
        GetValue<Expr, Expr> classExpr = new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.classExpr(baseClass).nvl(unknownExpr);
            }};

        ImMap<PropertyField, Expr> objectMapProps = objectProps.mapValues(new GetValue<Expr, PropertyField>() {
            public Expr getMapValue(PropertyField value) {
                return tableJoin.getExpr(value);
            }
        });
        ImMap<Field, Expr> group = MapFact.addExcl(objectMapKeys, objectMapProps).mapValues(classExpr);

        GetValue<ImMap<Field, ConcreteClass>, ImMap<Field, Object>> findClasses = new GetValue<ImMap<Field, ConcreteClass>, ImMap<Field, Object>>() {
            public ImMap<Field, ConcreteClass> getMapValue(ImMap<Field, Object> value) {
                return value.mapValues(new GetValue<ConcreteClass, Object>() {
                    public ConcreteClass getMapValue(Object id) {
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
                keyOrderSet().getSet().mapSetValues(BaseUtils.<GetValue<ImMap<KeyField, ConcreteClass>, ImMap<KeyField, Object>>>immutableCast(findClasses));

        ClassWhere<KeyField> where = ClassWhere.FALSE();
        for(ImMap<KeyField, ConcreteClass> readClass : readClasses)
            where = where.or(new ClassWhere<KeyField>(MapFact.addExcl(readClass, dataKeys.result)));
        mPropertyClasses.exclAdd(prop, where);
    */

    protected ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
    protected ImMap<PropertyField,ClassWhere<Field>> propertyClasses;

    public String outputKeys() {
        return ServerResourceBundle.getString("data.table")+" : " + name + ", "+ServerResourceBundle.getString("data.keys")+" : " + classes.getCommonParent(getTableKeys()).toString();
    }

    public String outputField(PropertyField field, boolean outputTable) {
        ImMap<Field, ValueClass> commonParent = propertyClasses.get(field).getCommonParent(SetFact.addExcl(getTableKeys(), field));
        return (outputTable ? ServerResourceBundle.getString("data.table")+" : " + name + ", ":"") + ServerResourceBundle.getString("data.field") +" : " + field.getName() + " - " + commonParent.get(field) + ", "+ServerResourceBundle.getString("data.keys")+" : " + commonParent.remove(field);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((Table)o).name) && classes.equals(((Table)o).classes) && propertyClasses.equals(((Table) o).propertyClasses);
    }

    public int immutableHashCode() {
        return (name.hashCode() * 31 + classes.hashCode()) * 31 + propertyClasses.hashCode();
    }

    public Query<KeyField, PropertyField> getQuery() {
        QueryBuilder<KeyField,PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        lsfusion.server.data.query.Join<PropertyField> join = join(query.getMapExprs());
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

    public lsfusion.server.data.query.Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {
        return new AddPullWheres<KeyField, lsfusion.server.data.query.Join<PropertyField>>() {
            protected MCaseList<lsfusion.server.data.query.Join<PropertyField>, ?, ?> initCaseList(boolean exclusive) {
                return new MJoinCaseList<PropertyField>(properties, exclusive);
            }
            protected lsfusion.server.data.query.Join<PropertyField> initEmpty() {
                return new NullJoin<PropertyField>(properties);
            }
            protected lsfusion.server.data.query.Join<PropertyField> proceedIf(Where ifWhere, lsfusion.server.data.query.Join<PropertyField> resultTrue, lsfusion.server.data.query.Join<PropertyField> resultFalse) {
                return new IfJoin<PropertyField>(ifWhere, resultTrue, resultFalse);
            }

            protected lsfusion.server.data.query.Join<PropertyField> proceedBase(ImMap<KeyField, BaseExpr> joinBase) {
                return joinAnd(joinBase);
            }
        }.proceed(joinImplement);
    }

    public Join joinAnd(ImMap<KeyField, ? extends BaseExpr> joinImplement) {
        return new Join(joinImplement);
    }

    protected int hash(HashContext hashContext) {
        return hashCode();
    }

    protected Table translate(MapTranslate translator) {
        return this;
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.EMPTY();
    }

    public class Join extends AbstractOuterContext<Join> implements InnerJoin<KeyField, Join>, lsfusion.server.data.query.Join<PropertyField> {

        public final ImMap<KeyField, BaseExpr> joins;

        public ImMap<KeyField, BaseExpr> getJoins() {
            return joins;
        }
        public StatKeys<KeyField> getStatKeys(KeyStat keyStat) {
            return Table.this.getStatKeys();
        }

        public Join(ImMap<KeyField, ? extends BaseExpr> joins) {
            this.joins = (ImMap<KeyField, BaseExpr>) joins;
            assert (joins.size()==keys.size());
        }

        @TwinLazy
        Where getJoinsWhere() {
            return lsfusion.server.data.expr.Expr.getWhere(joins);
        }

        public ImSet<NotNullExpr> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
            return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
        }

        public boolean hasExprFollowsWithoutNotNull() {
            return InnerExpr.hasExprFollowsWithoutNotNull(this);
        }

        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(this);
        }

        public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) {
            return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
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

        public lsfusion.server.data.query.Join<PropertyField> and(Where where) {
            return AbstractJoin.and(this, where);
        }

        public lsfusion.server.data.query.Join<PropertyField> translateValues(MapValuesTranslate translate) {
            return AbstractJoin.translateValues(this, translate);
        }
        public lsfusion.server.data.query.Join<PropertyField> translateRemoveValues(MapValuesTranslate translate) {
            return translateOuter(translate.mapKeys());
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return Table.this.hashOuter(hashContext)*31 + AbstractSourceJoin.hashOuter(joins, hashContext);
        }

        protected Join translate(MapTranslate translator) {
            return Table.this.translateOuter(translator).joinAnd(translator.translateDirect(joins));
        }
        public Join translateOuter(MapTranslate translator) {
            return (Join) aspectTranslate(translator);
        }

        @ParamLazy
        public lsfusion.server.data.query.Join<PropertyField> translateQuery(QueryTranslator translator) {
            return join(translator.translate(joins));
        }

        public lsfusion.server.data.query.Join<PropertyField> packFollowFalse(Where falseWhere) {
            ImMap<KeyField, lsfusion.server.data.expr.Expr> packJoins = BaseExpr.packPushFollowFalse(joins, falseWhere);
            if(!BaseUtils.hashEquals(packJoins, joins))
                return join(packJoins);
            else
                return this;
        }

        public String getQueryName(CompileSource source) {
            return Table.this.getQueryName(source);
        }

        private Table getTable() {
            return Table.this;
        }

        public InnerExpr getInnerExpr(WhereJoin join) {
            return QueryJoin.getInnerExpr(this, join);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.<OuterContext>addExcl(joins.values().toSet(), Table.this);
        }

        public class IsIn extends DataWhere implements JoinData {

            public String getFirstKey(SQLSyntax syntax) {
                if(isSingle())
                    return "dumb";
                return keys.iterator().next().getName(syntax);
            }

            public ImSet<OuterContext> calculateOuterDepends() {
                return SetFact.<OuterContext>singleton(Join.this);
            }

            public Join getJoin() {
                return Join.this;
            }

            public InnerJoin getFJGroup() {
                return Join.this;
            }

            protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
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
            public Where translateQuery(QueryTranslator translator) {
                return Join.this.translateQuery(translator).getWhere();
            }
            @Override
            public Where packFollowFalse(Where falseWhere) {
                return Join.this.packFollowFalse(falseWhere).getWhere();
            }

            protected ImSet<DataWhere> calculateFollows() {
                return NotNullExpr.getFollows(getExprFollows(NotNullExpr.FOLLOW, true));
            }

            public lsfusion.server.data.expr.Expr getFJExpr() {
                return ValueExpr.get(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<lsfusion.server.data.expr.Expr> orderTop, GroupJoinsWheres.Type type) {
                return new GroupJoinsWheres(Join.this, this, type);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.mapClasses(joins).and(getJoinsWhere().getClassWhere());
            }

            public boolean calcTwins(TwinImmutableObject o) {
                return Join.this.equals(((IsIn) o).getJoin());
            }

            public int hash(HashContext hashContext) {
                return Join.this.hashOuter(hashContext);
            }

        }

        public class Expr extends InnerExpr {

            public final PropertyField property;

            @Override
            public ImSet<OuterContext> calculateOuterDepends() {
                return SetFact.<OuterContext>singleton(Join.this);
            }

            // напрямую может конструироваться только при полной уверенности что не null
            private Expr(PropertyField property) {
                this.property = property;
                assert properties.contains(property);
            }

            public lsfusion.server.data.expr.Expr translateQuery(QueryTranslator translator) {
                return Join.this.translateQuery(translator).getExpr(property);
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
            protected int hash(HashContext hashContext) {
                return Join.this.hashOuter(hashContext)*31+property.hashCode();
            }

            public String getSource(CompileSource compile) {
                return compile.getSource(this);
            }

            public class NotNull extends InnerExpr.NotNull {

                @Override
                protected ImSet<DataWhere> calculateFollows() {
                    return SetFact.addExcl(super.calculateFollows(), (DataWhere) Join.this.getWhere());
                }

                public ClassExprWhere calculateClassWhere() {
                    return propertyClasses.get(property).mapClasses(MapFact.addExcl(joins, property, Expr.this)).and(Join.this.getJoinsWhere().getClassWhere());
                }
            }

            @Override
            public void fillFollowSet(MSet<DataWhere> fillSet) {
                super.fillFollowSet(fillSet);
                fillSet.add((DataWhere) Join.this.getWhere());
            }

            public Table.Join getInnerJoin() {
                return Join.this;
            }

            public PropStat getStatValue(KeyStat keyStat) {
                return getStatProps().get(property);
            }

            @Override
            public boolean isTableIndexed() {
                return true;
            }
        }

        @Override
        public String toString() {
            return Table.this.getName();
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