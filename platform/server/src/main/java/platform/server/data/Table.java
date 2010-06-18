package platform.server.data;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.Lazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashCodeContext;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.*;
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

@Immutable
public class Table implements MapKeysInterface<KeyField> {
    public final String name;
    public final Collection<KeyField> keys = new ArrayList<KeyField>();
    public final Collection<PropertyField> properties = new ArrayList<PropertyField>();

    public boolean isSingle() {
        return keys.size()==0;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        Map<KeyField,KeyExpr> result = new HashMap<KeyField, KeyExpr>();
        for(KeyField key : keys)
            result.put(key,new KeyExpr(key.name));
        return result;
    }

    public Table(String name,ClassWhere<KeyField> classes) {
        this.name = name;
        this.classes = classes;
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String name) {
        this.name = name;
        classes = new ClassWhere<KeyField>();
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String name,ClassWhere<KeyField> classes,Map<PropertyField, ClassWhere<Field>> propertyClasses) {
        this.name = name;
        this.classes = classes;
        this.propertyClasses = propertyClasses;
    }

    public String getName(SQLSyntax Syntax) {
        return name;
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


    public Table(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        int keysNum = inStream.readInt();
        for(int i=0;i<keysNum;i++)
            keys.add((KeyField) Field.deserialize(inStream));
        int propNum = inStream.readInt();
        for(int i=0;i<propNum;i++)
            properties.add((PropertyField) Field.deserialize(inStream));

        classes = new ClassWhere<KeyField>();
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
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

    @Override
    public boolean equals(Object obj) {
        return this == obj || getClass()==obj.getClass() && name.equals(((Table)obj).name) && classes.equals(((Table)obj).classes) && propertyClasses.equals(((Table)obj).propertyClasses);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public void out(SQLSession session) throws SQLException {
        Query<KeyField,PropertyField> query = new Query<KeyField,PropertyField>(this);
        platform.server.data.query.Join<PropertyField> join = joinAnd(query.mapKeys);
        query.and(join.getWhere());
        query.properties.putAll(join.getExprs());
        query.outSelect(session);
    }

    public platform.server.data.query.Join<PropertyField> join(Map<KeyField, ? extends Expr> joinImplement) {
        JoinCaseList<PropertyField> result = new JoinCaseList<PropertyField>();
        for(MapCase<KeyField> caseJoin : CaseExpr.pullCases(joinImplement))
            result.add(new JoinCase<PropertyField>(caseJoin.where,joinAnd(caseJoin.data)));
        return new CaseJoin<PropertyField>(result, properties);
    }

    public platform.server.data.query.Join<PropertyField> joinAnd(Map<KeyField, ? extends BaseExpr> joinImplement) {
        return new Join(joinImplement);
    }

    @Immutable
    public class Join extends platform.server.data.query.Join<PropertyField> implements InnerJoin {

        public final Map<KeyField, BaseExpr> joins;

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

        @Lazy
        public int hashContext(HashContext hashContext) {
            int hash = Table.this.hashCode()*31;
                // нужен симметричный хэш относительно выражений
            for(Map.Entry<KeyField, BaseExpr> join : joins.entrySet())
                hash += join.getKey().hashCode() ^ join.getValue().hashContext(hashContext);
            return hash;
        }

        @ParamLazy
        public Join translate(MapTranslate translator) {
            return new Join(translator.translateDirect(joins));
        }

        @ParamLazy
        public platform.server.data.query.Join<PropertyField> translateQuery(QueryTranslator translator) {
            return join(translator.translate(joins));
        }

        public String getName(SQLSyntax syntax) {
            return Table.this.getName(syntax);
        }

        private Table getTable() {
            return Table.this;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Join && Table.this.equals(((Join) o).getTable()) && joins.equals(((Join) o).joins);
        }

        public boolean isIn(VariableExprSet set) {
            for(int i=0;i<set.size;i++) {
                VariableClassExpr expr = set.get(i);
                if(expr instanceof Expr && equals(((Expr)expr).getJoin()))
                    return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashContext(HashCodeContext.instance);
        }

        public class IsIn extends DataWhere implements JoinData {

            public String getFirstKey() {
                if(isSingle())
                    return "for where.getSource() в getFrom";
                return keys.iterator().next().toString();
            }

            public void enumerate(ContextEnumerator enumerator) {
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

            public Where translate(MapTranslate translator) {
                return Join.this.translate(translator).getDirectWhere();
            }
            public Where translateQuery(QueryTranslator translator) {
                return Join.this.translateQuery(translator).getWhere();
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

            public InnerJoins groupInnerJoins() {
                return new InnerJoins(Join.this,this);
            }
            public ClassExprWhere calculateClassWhere() {
                return classes.map(joins).and(getJoinsWhere().getClassWhere());
            }

            public boolean twins(AbstractSourceJoin o) {
                return Join.this.equals(((IsIn) o).getJoin());
            }

            public int hashContext(HashContext hashContext) {
                return Join.this.hashContext(hashContext);
            }
        }

        public class Expr extends InnerExpr {

            public final PropertyField property;

            // напрямую может конструироваться только при полной уверенности что не null
            private Expr(PropertyField iProperty) {
                property = iProperty;
            }

            public platform.server.data.expr.Expr translateQuery(QueryTranslator translator) {
                return Join.this.translateQuery(translator).getExpr(property);
            }

            public Expr translate(MapTranslate translator) {
                return Join.this.translate(translator).getDirectExpr(property);
            }

            public void enumerate(ContextEnumerator enumerator) {
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

            @Override
            public boolean twins(AbstractSourceJoin o) {
                return Join.this.equals(((Expr) o).getJoin()) && property.equals(((Expr) o).property);
            }

            @Lazy
            public int hashContext(HashContext hashContext) {
                return Join.this.hashContext(hashContext)*31+property.hashCode();
            }

            public String getSource(CompileSource compile) {
                return compile.getSource(this);
            }

            public VariableExprSet getJoinFollows() {
                return Join.this.getJoinFollows();
            }

            public class NotNull extends InnerExpr.NotNull {

                public InnerJoins groupInnerJoins() {
                    return new InnerJoins(Join.this,this);
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
        }

        @Override
        public String toString() {
            return Table.this.toString();
        }
    }
}
