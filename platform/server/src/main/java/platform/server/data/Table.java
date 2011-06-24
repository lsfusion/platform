package platform.server.data;

import platform.base.*;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.CaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.pull.AddPullCases;
import platform.server.data.expr.query.KeyStat;
import platform.server.data.expr.query.StatKeys;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.ObjectJoinSets;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Table extends TwinImmutableObject implements MapKeysInterface<KeyField> {
    public final String name;
    public final List<KeyField> keys; // List потому как в таком порядке индексы будут строиться
    public final Set<PropertyField> properties;

    public boolean isSingle() {
        return keys.size()==0;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    public Table(String name) {
        this(name, new ArrayList<KeyField>(), new HashSet<PropertyField>(), new ClassWhere<KeyField>(), new HashMap<PropertyField, ClassWhere<Field>>());
    }

    public Table(String name, List<KeyField> keys, Set<PropertyField> properties,ClassWhere<KeyField> classes,Map<PropertyField, ClassWhere<Field>> propertyClasses) {
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


    public Table(DataInputStream inStream, BaseClass baseClass) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        keys = new ArrayList<KeyField>();
        for(int i=0;i<keysNum;i++)
            keys.add((KeyField) Field.deserialize(inStream));
        int propNum = inStream.readInt();
        properties = new HashSet<PropertyField>();
        for(int i=0;i<propNum;i++)
            properties.add((PropertyField) Field.deserialize(inStream));

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
        return "Таблица : " + name + ", Ключи : " + classes.getCommonParent(keys).toString();
    }

    public String outputField(PropertyField field, boolean outputTable) {
        Map<Field,ValueClass> commonParent = propertyClasses.get(field).getCommonParent(BaseUtils.merge(keys, Collections.singleton(field)));
        return (outputTable?"Таблица : " + name + ", ":"") + "Поле : " + field.toString() + " - " + commonParent.get(field) + ", Ключи : " + BaseUtils.removeKey(commonParent, field);
    }

    public boolean twins(TwinImmutableInterface o) {
        return name.equals(((Table)o).name) && classes.equals(((Table)o).classes) && propertyClasses.equals(((Table)o).propertyClasses);
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

    public platform.server.data.query.Join<PropertyField> join(Map<KeyField, ? extends Expr> joinImplement) {
        return new AddPullCases<KeyField, platform.server.data.query.Join<PropertyField>>() {
            protected CaseList<platform.server.data.query.Join<PropertyField>, ?> initAggregator() {
                return new JoinCaseList<PropertyField>(properties);
            }
            protected platform.server.data.query.Join<PropertyField> proceedBase(Map<KeyField, BaseExpr> joinBase) {
                return joinAnd(joinBase);
            }
        }.proceed(joinImplement);
    }

    public Join joinAnd(Map<KeyField, ? extends BaseExpr> joinImplement) {
        return new Join(joinImplement);
    }

    public int hashOuter(HashContext hashContext) {
        return hashCode();
    }

    public Table translateOuter(MapTranslate translator) {
        return this;
    }

    public void enumInnerValues(Set<Value> values) {
    }

    public int getCount() {
        return Integer.MAX_VALUE;
    }

    public boolean isFew() {
        return getCount() < Settings.instance.getFewCount();
    }

    public class Join extends platform.server.data.query.Join<PropertyField> implements InnerJoin<KeyField>, TwinImmutableInterface {

        public final Map<KeyField, BaseExpr> joins;

        public Map<KeyField, BaseExpr> getJoins() {
            return joins;
        }
        public StatKeys<KeyField> getStatKeys() {
            return new StatKeys<KeyField>(joins.keySet(), isFew()? KeyStat.FEW : KeyStat.MANY);
        }

        public Join(Map<KeyField, ? extends BaseExpr> joins) {
            this.joins = (Map<KeyField, BaseExpr>) joins;
            assert (joins.size()==keys.size());
        }

        @TwinLazy
        Where getJoinsWhere() {
            return platform.server.data.expr.Expr.getWhere(joins);
        }

        public VariableExprSet getJoinFollows() {
            return InnerExpr.getExprFollows(joins);
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

        // множественное наследование TwinImmutableObject {

        @Override
        public boolean equals(Object o) {
            return TwinImmutableObject.equals(this, o);
        }

        boolean hashCoded = false;
        int hashCode;
        @Override
        public int hashCode() {
            if(!hashCoded) {
                hashCode = immutableHashCode();
                hashCoded = true;
            }
            return hashCode;
        }

        // }

        public boolean twins(TwinImmutableInterface o) {
            return Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
        }

        public int immutableHashCode() {
            return hashOuter(HashContext.hashCode);
        }


        @IdentityLazy
        public int hashOuter(HashContext hashContext) {
            int hash = Table.this.hashOuter(hashContext)*31;
                // нужен симметричный хэш относительно выражений
            for(Map.Entry<KeyField, BaseExpr> join : joins.entrySet())
                hash += join.getKey().hashCode() * join.getValue().hashOuter(hashContext);
            return hash;
        }

        @ParamLazy
        public Join translateOuter(MapTranslate translator) {
            return Table.this.translateOuter(translator).joinAnd(translator.translateDirect(joins));
        }
        
        public void enumInnerValues(Set<Value> values) {
            Table.this.enumInnerValues(values);
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

        public boolean isIn(VariableExprSet set) {
            for(int i=0;i<set.size;i++) {
                VariableClassExpr expr = set.get(i);
                if(expr instanceof Expr && BaseUtils.hashEquals(this,((Expr)expr).getJoin()))
                    return true;
            }
            return false;
        }

        private long getComplexity() {
            return AbstractSourceJoin.getComplexity(joins.values()); 
        }

        public class IsIn extends DataWhere implements JoinData {

            public String getFirstKey() {
                if(isSingle())
                    return "dumb";
                return keys.iterator().next().toString();
            }

            public void enumDepends(ExprEnumerator enumerator) {
                enumerator.fill(joins);
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

            public Where translateOuter(MapTranslate translator) {
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
                return new DataWhereSet(getJoinFollows());
            }

            public platform.server.data.expr.Expr getFJExpr() {
                return ValueExpr.get(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public ObjectJoinSets groupObjectJoinSets() {
                return new ObjectJoinSets(Join.this,this);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.map(joins).and(getJoinsWhere().getClassWhere());
            }

            public boolean twins(TwinImmutableInterface o) {
                return Join.this.equals(((IsIn) o).getJoin());
            }

            public int hashOuter(HashContext hashContext) {
                return Join.this.hashOuter(hashContext);
            }

            public long calculateComplexity() {
                return Join.this.getComplexity();
            }

            @Override
            public void enumInnerValues(Set<Value> values) {
                Join.this.enumInnerValues(values);
            }
        }

        public class Expr extends InnerExpr {

            public final PropertyField property;

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

            public Expr translateOuter(MapTranslate translator) {
                return Join.this.translateOuter(translator).getDirectExpr(property);
            }

            @Override
            public void enumInnerValues(Set<Value> values) {
                Join.this.enumInnerValues(values);
            }

            public void enumDepends(ExprEnumerator enumerator) {
                enumerator.fill(joins);
            }

            public Join getJoin() {
                return Join.this;
            }

            public InnerJoin getFJGroup() {
                return Join.this;
            }

            public String toString() {
                return Join.this.toString() + "." + property;
            }

            public Type getType(KeyType keyType) {
                return property.type;
            }

            // возвращает Where без следствий
            public Where calculateWhere() {
                return new NotNull();
            }

            public boolean twins(TwinImmutableInterface o) {
                return Join.this.equals(((Expr) o).getJoin()) && property.equals(((Expr) o).property);
            }

            @IdentityLazy
            public int hashOuter(HashContext hashContext) {
                return Join.this.hashOuter(hashContext)*31+property.hashCode();
            }

            public String getSource(CompileSource compile) {
                return compile.getSource(this);
            }

            public VariableExprSet getJoinFollows() {
                return Join.this.getJoinFollows();
            }

            public class NotNull extends InnerExpr.NotNull {

                public ObjectJoinSets groupObjectJoinSets() {
                    return new ObjectJoinSets(Join.this,this);
                }

                @Override
                public Where packFollowFalse(Where falseWhere) {
                    return Expr.this.packFollowFalse(falseWhere).getWhere();
                }

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

            public long calculateComplexity() {
                return Join.this.getComplexity();
            }
        }

        @Override
        public String toString() {
            return Table.this.toString();
        }
    }
}
