package platform.server.data;

import platform.base.*;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.*;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.MCaseList;
import platform.server.data.expr.where.cases.MJoinCaseList;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.expr.where.ifs.NullJoin;
import platform.server.data.expr.where.pull.AddPullWheres;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public abstract class Table extends AbstractOuterContext<Table> implements MapKeysInterface<KeyField> {
    public String name;
    public ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public ImOrderSet<KeyField> getOrderTableKeys() {
        return keys;
    }
    public ImSet<KeyField> getTableKeys() {
        return keys.getSet();
    }
    public ImSet<PropertyField> properties;

    public abstract StatKeys<KeyField> getStatKeys();
    public abstract ImMap<PropertyField, Stat> getStatProps();

    private static Stat getFieldStat(Field field, Stat defStat) {
        if(field.type instanceof DataClass)
            return defStat.min(((DataClass)field.type).getTypeStat());
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

    protected static ImMap<PropertyField, Stat> getStatProps(Table table, final Stat stat) { // для мн-го наследования
        return table.properties.mapValues(new GetValue<Stat, PropertyField>() {
            public Stat getMapValue(PropertyField prop) {
                return getFieldStat(prop, stat);
            }});
    }
    
    protected static ImMap<PropertyField, Stat> getStatProps(Table table, int count) { // для мн-го наследования
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
    }

    public String getName(SQLSyntax Syntax) {
        return name;
    }

    public String getQueryName(CompileSource source) {
        return getName(source.syntax);
    }

    public String toString() {
        return name;
    }

    public KeyField findKey(String name) {
        for(KeyField key : keys)
            if(key.name.equals(name))
                return key;
        return null;
    }

    public PropertyField findProperty(String name) {
        for(PropertyField property : properties)
            if(property.name.equals(name))
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


    public Table(DataInputStream inStream, final BaseClass baseClass, int version) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        MOrderExclSet<KeyField> mKeys = SetFact.mOrderExclSet(keysNum); // десериализация, поэтому порядок важен
        for(int i=0;i<keysNum;i++)
            mKeys.add((KeyField) Field.deserialize(inStream, version));
        keys = mKeys.immutableOrder();
        int propNum = inStream.readInt();
        MExclSet<PropertyField> mProperties = SetFact.mExclSet(propNum);
        for(int i=0;i<propNum;i++)
            mProperties.exclAdd((PropertyField) Field.deserialize(inStream, version));
        properties = mProperties.immutable();

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

    public ImOrderMap<ImMap<KeyField,DataObject>,ImMap<PropertyField,ObjectValue>> read(SQLSession session, BaseClass baseClass) throws SQLException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> tableJoin = join(query.getMapExprs());
        query.addProperties(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.executeClasses(session, baseClass);
    }

    protected ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
    protected ImMap<PropertyField,ClassWhere<Field>> propertyClasses;

    public String outputKeys() {
        return ServerResourceBundle.getString("data.table")+" : " + name + ", "+ServerResourceBundle.getString("data.keys")+" : " + classes.getCommonParent(getTableKeys()).toString();
    }

    public String outputField(PropertyField field, boolean outputTable) {
        ImMap<Field, ValueClass> commonParent = propertyClasses.get(field).getCommonParent(SetFact.addExcl(getTableKeys(), field));
        return (outputTable ? ServerResourceBundle.getString("data.table")+" : " + name + ", ":"") + ServerResourceBundle.getString("data.field") +" : " + field.toString() + " - " + commonParent.get(field) + ", "+ServerResourceBundle.getString("data.keys")+" : " + commonParent.remove(field);
    }

    public boolean twins(TwinImmutableObject o) {
        return name.equals(((Table)o).name) && classes.equals(((Table)o).classes) && propertyClasses.equals(((Table) o).propertyClasses);
    }

    public int immutableHashCode() {
        return (name.hashCode() * 31 + classes.hashCode()) * 31 + propertyClasses.hashCode();
    }

    public Query<KeyField, PropertyField> getQuery() {
        QueryBuilder<KeyField,PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> join = join(query.getMapExprs());
        query.and(join.getWhere());
        query.addProperties(join.getExprs());
        return query.getQuery();
    }

    public void out(SQLSession session) throws SQLException {
        getQuery().outSelect(session);
    }

    public void outClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        getQuery().outClassesSelect(session, baseClass);
    }

    public platform.server.data.query.Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {
        return new AddPullWheres<KeyField, platform.server.data.query.Join<PropertyField>>() {
            protected MCaseList<platform.server.data.query.Join<PropertyField>, ?, ?> initCaseList(boolean exclusive) {
                return new MJoinCaseList<PropertyField>(properties, exclusive);
            }
            protected platform.server.data.query.Join<PropertyField> initEmpty() {
                return new NullJoin<PropertyField>(properties);
            }
            protected platform.server.data.query.Join<PropertyField> proceedIf(Where ifWhere, platform.server.data.query.Join<PropertyField> resultTrue, platform.server.data.query.Join<PropertyField> resultFalse) {
                return new IfJoin<PropertyField>(ifWhere, resultTrue, resultFalse);
            }

            protected platform.server.data.query.Join<PropertyField> proceedBase(ImMap<KeyField, BaseExpr> joinBase) {
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

    public class Join extends AbstractOuterContext<Join> implements InnerJoin<KeyField, Join>, platform.server.data.query.Join<PropertyField> {

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
            return platform.server.data.expr.Expr.getWhere(joins);
        }

        public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
            return InnerExpr.getExprFollows(this, recursive);
        }

        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(this);
        }

        public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) {
            return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
        }

        @TwinLazy
        public platform.server.data.expr.Expr getExpr(PropertyField property) {
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

        public boolean twins(TwinImmutableObject o) {
            return Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
        }

        public ImMap<PropertyField, platform.server.data.expr.Expr> getExprs() {
            return AbstractJoin.getExprs(this);
        }

        public platform.server.data.query.Join<PropertyField> and(Where where) {
            return AbstractJoin.and(this, where);
        }

        public platform.server.data.query.Join<PropertyField> translateValues(MapValuesTranslate translate) {
            return AbstractJoin.translateValues(this, translate);
        }
        public platform.server.data.query.Join<PropertyField> translateRemoveValues(MapValuesTranslate translate) {
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
        public platform.server.data.query.Join<PropertyField> translateQuery(QueryTranslator translator) {
            return join(translator.translate(joins));
        }

        public platform.server.data.query.Join<PropertyField> packFollowFalse(Where falseWhere) {
            ImMap<KeyField, platform.server.data.expr.Expr> packJoins = BaseExpr.packPushFollowFalse(joins, falseWhere);
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

            public String getFirstKey() {
                if(isSingle())
                    return "dumb";
                return keys.iterator().next().toString();
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
                return NotNullExpr.getFollows(getExprFollows(true));
            }

            public platform.server.data.expr.Expr getFJExpr() {
                return ValueExpr.get(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<platform.server.data.expr.Expr> orderTop, boolean noWhere) {
                return new GroupJoinsWheres(Join.this, this, noWhere);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.map(joins).and(getJoinsWhere().getClassWhere());
            }

            public boolean twins(TwinImmutableObject o) {
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

            public platform.server.data.expr.Expr translateQuery(QueryTranslator translator) {
                return Join.this.translateQuery(translator).getExpr(property);
            }

            @Override
            public platform.server.data.expr.Expr packFollowFalse(Where where) {
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
            public Stat getTypeStat(KeyStat keyStat) {
                return propertyClasses.get(property).getTypeStat(property);
            }

            public NotNull calculateNotNullWhere() {
                return new NotNull();
            }

            public boolean twins(TwinImmutableObject o) {
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
                    return propertyClasses.get(property).map(MapFact.addExcl(joins, property, Expr.this)).and(Join.this.getJoinsWhere().getClassWhere());
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

            public Stat getStatValue(KeyStat keyStat) {
                return getStatProps().get(property);
            }

            @Override
            public boolean isTableIndexed() {
                return true;
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
