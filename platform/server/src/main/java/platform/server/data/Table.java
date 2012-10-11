package platform.server.data;

import platform.base.*;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.*;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.expr.where.cases.JoinCaseList;
import platform.server.data.expr.where.pull.AddPullWheres;
import platform.server.data.expr.where.ifs.NullJoin;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.MapWhere;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
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
import java.util.*;

public abstract class Table extends AbstractOuterContext<Table> implements MapKeysInterface<KeyField> {
    public String name;
    public final List<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public final Set<PropertyField> properties;

    public abstract StatKeys<KeyField> getStatKeys();
    public abstract Map<PropertyField, Stat> getStatProps();

    private static Stat getFieldStat(Field field, Stat defStat) {
        if(field.type instanceof DataClass)
            return defStat.min(((DataClass)field.type).getTypeStat());
        else
            return defStat;
    }

    protected static StatKeys<KeyField> getStatKeys(Table table, int count) { // для мн-го наследования
        Stat stat = new Stat(count);

        DistinctKeys<KeyField> distinctKeys = new DistinctKeys<KeyField>() ;
        for(KeyField key : table.keys)
            distinctKeys.add(key, getFieldStat(key, stat));
        return new StatKeys<KeyField>(distinctKeys.getMax().min(stat), distinctKeys);
    }

    protected static Map<PropertyField, Stat> getStatProps(Table table, Stat stat) { // для мн-го наследования
        Map<PropertyField, Stat> result = new HashMap<PropertyField, Stat>();
        for(PropertyField prop : table.properties)
            result.put(prop, getFieldStat(prop, stat));
        return result;
    }
    
    protected static Map<PropertyField, Stat> getStatProps(Table table, int count) { // для мн-го наследования
        return getStatProps(table, new Stat(count));
    }

    public boolean isSingle() {
        return keys.size()==0;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    protected Table(String name) {
        this(name, new ArrayList<KeyField>(), new HashSet<PropertyField>(), new ClassWhere<KeyField>(), new HashMap<PropertyField, ClassWhere<Field>>());
    }

    protected Table(String name, List<KeyField> keys, Set<PropertyField> properties,ClassWhere<KeyField> classes,Map<PropertyField, ClassWhere<Field>> propertyClasses) {
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


    public Table(DataInputStream inStream, BaseClass baseClass, int version) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        keys = new ArrayList<KeyField>();
        for(int i=0;i<keysNum;i++)
            keys.add((KeyField) Field.deserialize(inStream, version));
        int propNum = inStream.readInt();
        properties = new HashSet<PropertyField>();
        for(int i=0;i<propNum;i++)
            properties.add((PropertyField) Field.deserialize(inStream, version));

        Map<KeyField, AndClassSet> baseClasses = new HashMap<KeyField, AndClassSet>();
        for(KeyField key : keys)
            baseClasses.put(key,key.type.getBaseClassSet(baseClass));
        classes = new ClassWhere<KeyField>(baseClasses);

        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField property : properties)
            propertyClasses.put(property, new ClassWhere<Field>(BaseUtils.merge(baseClasses, Collections.singletonMap(property, property.type.getBaseClassSet(baseClass)))));
    }

    public OrderedMap<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> read(SQLSession session, BaseClass baseClass) throws SQLException {
        Query<KeyField, PropertyField> query = new Query<KeyField,PropertyField>(this);
        platform.server.data.query.Join<PropertyField> tableJoin = join(query.mapKeys);
        query.properties.putAll(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.executeClasses(session, baseClass);
    }

    protected ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе

    protected final Map<PropertyField,ClassWhere<Field>> propertyClasses;

    public String outputKeys() {
        return ServerResourceBundle.getString("data.table")+" : " + name + ", "+ServerResourceBundle.getString("data.keys")+" : " + classes.getCommonParent(keys).toString();
    }

    public String outputField(PropertyField field, boolean outputTable) {
        Map<Field,ValueClass> commonParent = propertyClasses.get(field).getCommonParent(BaseUtils.merge(keys, Collections.singleton(field)));
        return (outputTable ? ServerResourceBundle.getString("data.table")+" : " + name + ", ":"") + ServerResourceBundle.getString("data.field") +" : " + field.toString() + " - " + commonParent.get(field) + ", "+ServerResourceBundle.getString("data.keys")+" : " + BaseUtils.removeKey(commonParent, field);
    }

    public boolean twins(TwinImmutableInterface o) {
        return name.equals(((Table)o).name) && classes.equals(((Table)o).classes) && propertyClasses.equals(((Table) o).propertyClasses);
    }

    public int immutableHashCode() {
        return (name.hashCode() * 31 + classes.hashCode()) * 31 + propertyClasses.hashCode();
    }

    public Query<KeyField, PropertyField> getQuery() {
        Query<KeyField,PropertyField> query = new Query<KeyField,PropertyField>(this);
        platform.server.data.query.Join<PropertyField> join = joinAnd(query.mapKeys);
        query.and(join.getWhere());
        query.properties.putAll(join.getExprs());
        return query;
    }

    public void out(SQLSession session) throws SQLException {
        getQuery().outSelect(session);
    }

    public void outClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        getQuery().outClassesSelect(session, baseClass);
    }

    public platform.server.data.query.Join<PropertyField> join(Map<KeyField, ? extends Expr> joinImplement) {
        return new AddPullWheres<KeyField, platform.server.data.query.Join<PropertyField>>() {
            protected JoinCaseList<PropertyField> initCaseList() {
                return new JoinCaseList<PropertyField>(properties);
            }
            protected platform.server.data.query.Join<PropertyField> initEmpty() {
                return new NullJoin<PropertyField>(properties);
            }
            protected platform.server.data.query.Join<PropertyField> proceedIf(Where ifWhere, platform.server.data.query.Join<PropertyField> resultTrue, platform.server.data.query.Join<PropertyField> resultFalse) {
                return new IfJoin<PropertyField>(ifWhere, resultTrue, resultFalse);
            }
            protected platform.server.data.query.Join<PropertyField> proceedBase(Map<KeyField, BaseExpr> joinBase) {
                return joinAnd(joinBase);
            }
        }.proceed(joinImplement);
    }

    public Join joinAnd(Map<KeyField, ? extends BaseExpr> joinImplement) {
        return new Join(joinImplement);
    }

    protected int hash(HashContext hashContext) {
        return hashCode();
    }

    protected Table translate(MapTranslate translator) {
        return this;
    }

    public QuickSet<OuterContext> calculateOuterDepends() {
        return QuickSet.EMPTY();
    }

    public class Join extends AbstractOuterContext<Join> implements InnerJoin<KeyField, Join>, platform.server.data.query.Join<PropertyField>, TwinImmutableInterface {

        public final Map<KeyField, BaseExpr> joins;

        public Map<KeyField, BaseExpr> getJoins() {
            return joins;
        }
        public StatKeys<KeyField> getStatKeys(KeyStat keyStat) {
            return Table.this.getStatKeys();
        }

        public Join(Map<KeyField, ? extends BaseExpr> joins) {
            this.joins = (Map<KeyField, BaseExpr>) joins;
            assert (joins.size()==keys.size());
        }

        @TwinLazy
        Where getJoinsWhere() {
            return platform.server.data.expr.Expr.getWhere(joins);
        }

        public NotNullExprSet getExprFollows(boolean recursive) {
            return InnerExpr.getExprFollows(this, recursive);
        }

        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(this);
        }

        public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins) {
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

        public Collection<PropertyField> getProperties() {
            return Table.this.properties;
        }

        public boolean twins(TwinImmutableInterface o) {
            return Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
        }

        public Map<PropertyField, platform.server.data.expr.Expr> getExprs() {
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
            Map<KeyField, platform.server.data.expr.Expr> packJoins = BaseExpr.packPushFollowFalse(joins, falseWhere);
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

        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(joins.values(), Table.this);
        }

        public class IsIn extends DataWhere implements JoinData {

            public String getFirstKey() {
                if(isSingle())
                    return "dumb";
                return keys.iterator().next().toString();
            }

            public QuickSet<OuterContext> calculateOuterDepends() {
                return new QuickSet<OuterContext>(Join.this);
            }

            public Join getJoin() {
                return Join.this;
            }

            public InnerJoin getFJGroup() {
                return Join.this;
            }

            protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
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

            protected DataWhereSet calculateFollows() {
                return new DataWhereSet(getExprFollows(true));
            }

            public platform.server.data.expr.Expr getFJExpr() {
                return ValueExpr.get(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<platform.server.data.expr.Expr> orderTop, boolean noWhere) {
                return new GroupJoinsWheres(Join.this, this, noWhere);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.map(joins).and(getJoinsWhere().getClassWhere());
            }

            public boolean twins(TwinImmutableInterface o) {
                return Join.this.equals(((IsIn) o).getJoin());
            }

            public int hash(HashContext hashContext) {
                return Join.this.hashOuter(hashContext);
            }

        }

        public class Expr extends InnerExpr {

            public final PropertyField property;

            @Override
            public QuickSet<OuterContext> calculateOuterDepends() {
                return new QuickSet<OuterContext>(Join.this);
            }

            // напрямую может конструироваться только при полной уверенности что не null
            private Expr(PropertyField property) {
                this.property = property;
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

            public boolean twins(TwinImmutableInterface o) {
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
                protected DataWhereSet calculateFollows() {
                    DataWhereSet result = new DataWhereSet(super.calculateFollows());
                    result.add((DataWhere) Join.this.getWhere());
                    return result;
                }

                public ClassExprWhere calculateClassWhere() {
                    return propertyClasses.get(property).map(BaseUtils.merge(joins,Collections.singletonMap(property, Expr.this))).and(Join.this.getJoinsWhere().getClassWhere());
                }
            }

            @Override
            public void fillFollowSet(DataWhereSet fillSet) {
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
