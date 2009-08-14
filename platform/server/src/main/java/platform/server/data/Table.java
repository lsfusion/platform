package platform.server.data;

import platform.server.data.classes.LogicalClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.session.SQLSession;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.caches.Lazy;
import platform.server.caches.ParamLazy;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
public class Table implements MapKeysInterface<KeyField> {
    public final String name;
    public final Collection<KeyField> keys = new ArrayList<KeyField>();
    public final Collection<PropertyField> properties = new ArrayList<PropertyField>();

    public Map<KeyField, KeyExpr> getMapKeys() {
        Map<KeyField,KeyExpr> result = new HashMap<KeyField, KeyExpr>();
        for(KeyField key : keys)
            result.put(key,new KeyExpr(key.name));
        return result;
    }

    public Table(String iName,ClassWhere<KeyField> iClasses) {
        name =iName;
        classes = iClasses;
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String iName) {
        name =iName;
        classes = new ClassWhere<KeyField>();
        propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
    }

    public Table(String iName,ClassWhere<KeyField> iClasses,Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        name =iName;
        classes = iClasses;
        propertyClasses = iPropertyClasses;
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

    public ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе

    public final Map<PropertyField,ClassWhere<Field>> propertyClasses;

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Table && name.equals(((Table)obj).name) && classes.equals(((Table)obj).classes) && propertyClasses.equals(((Table)obj).propertyClasses);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public void out(SQLSession session) throws SQLException {
        JoinQuery<KeyField,PropertyField> query = new JoinQuery<KeyField,PropertyField>(this);
        Join join = joinAnd(query.mapKeys);
        query.and(join.getWhere());
        query.properties.putAll(join.getExprs());
        query.outSelect(session);
    }

    public platform.server.data.query.Join<PropertyField> join(Map<KeyField, ? extends SourceExpr> joinImplement) {
        JoinCaseList<PropertyField> result = new JoinCaseList<PropertyField>();
        for(MapCase<KeyField> caseJoin : CaseExpr.pullCases(joinImplement))
            result.add(new JoinCase<PropertyField>(caseJoin.where,joinAnd(caseJoin.data)));
        return new CaseJoin<PropertyField>(result, properties);
    }

    public Join joinAnd(Map<KeyField, ? extends AndExpr> joinImplement) {
        return new Join(joinImplement);
    }

    @Immutable
    public class Join extends platform.server.data.query.Join<PropertyField> implements InnerJoin {

        public Map<KeyField, AndExpr> joins;

        public Join(Map<KeyField, ? extends AndExpr> iJoins) {
            joins = (Map<KeyField, AndExpr>) iJoins;
            assert (joins.size()==keys.size());
        }

        @Lazy
        Where getJoinsWhere() {
            return MapExpr.getJoinsWhere(joins);
        }

        public DataWhereSet getJoinFollows() {
            return MapExpr.getExprFollows(joins);
        }

        protected DataWhereSet getExprFollows() {
            return ((IsIn)getWhere()).getFollows();
        }

        @Lazy
        public SourceExpr getExpr(PropertyField property) {
            return AndExpr.create(new Expr(property));
        }

        @Lazy
        public Where<?> getWhere() {
            ClassExprWhere classWhere = classes.map(joins).and(getJoinsWhere().getClassWhere());
            if(classWhere.isFalse())
                return Where.FALSE;
            else
                return new IsIn(classWhere);
        }

        public Collection<PropertyField> getProperties() {
            return Table.this.properties;
        }

        public int hashContext(HashContext hashContext) {
            int hash = Table.this.hashCode()*31;
                // нужен симметричный хэш относительно выражений
            for(Map.Entry<KeyField,AndExpr> join : joins.entrySet())
                hash += join.getKey().hashCode() ^ join.getValue().hashContext(hashContext);
            return hash;
        }

        @ParamLazy
        public Join translateDirect(KeyTranslator translator) {
            return new Join(translator.translateDirect(joins));
        }

        @ParamLazy
        public platform.server.data.query.Join<PropertyField> translateQuery(QueryTranslator translator) {
            return join(translator.translate(joins));
        }

        public platform.server.data.query.Join<PropertyField> translate(Translator<?> translator) {
            if(translator instanceof KeyTranslator)
                return translateDirect((KeyTranslator)translator);
            else
                return translateQuery((QueryTranslator)translator);
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

        @Override
        public int hashCode() {
            return hashContext(new HashContext(){
                public int hash(KeyExpr expr) {
                    return expr.hashCode();
                }

                public int hash(ValueExpr expr) {
                    return expr.hashCode();
                }
            });
        }

        public class IsIn extends DataWhere implements JoinData {

            public String getFirstKey() {
                if(keys.size()==0) {
                    assert Table.this.name.equals("global");
                    return "dumb";
                }
                return keys.iterator().next().toString();
            }

            ClassExprWhere joinClassWhere;

            public IsIn(ClassExprWhere iJoinClassWhere) {
                joinClassWhere = iJoinClassWhere;
            }

            public void fillContext(Context context) {
                context.fill(joins);
            }

            public Join getJoin() {
                return Join.this;
            }

            public Object getFJGroup() {
                return Join.this;
            }

            public InnerJoins getInnerJoins() {
                return new InnerJoins(Join.this,this);
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

            public Where translate(Translator translator) {
                return Join.this.translate(translator).getWhere();
            }

            protected DataWhereSet getExprFollows() {
                return getJoinFollows();
            }

            public SourceExpr getFJExpr() {
                return new ValueExpr(true, LogicalClass.instance).and(this);
            }

            public String getFJString(String exprFJ) {
                return exprFJ + " IS NOT NULL";
            }

            public ClassExprWhere calculateClassWhere() {
                return joinClassWhere;
            }

            @Override
            public boolean equals(Object o) {
                return this == o || o instanceof IsIn && Join.this.equals(((IsIn) o).getJoin());
            }

            public int hashContext(HashContext hashContext) {
                return Join.this.hashContext(hashContext);
            }
        }

        public class Expr extends MapExpr {

            public final PropertyField property;

            // напрямую может конструироваться только при полной уверенности что не null
            private Expr(PropertyField iProperty) {
                property = iProperty;
            }

            public SourceExpr translateQuery(QueryTranslator translator) {
                return Join.this.translate(translator).getExpr(property);
            }

            public Table.Join.Expr translateDirect(KeyTranslator translator) {
                return (Expr) Join.this.translateDirect(translator).getExpr(property);
            }

            public void fillContext(Context context) {
                context.fill(joins);
            }

            public Join getJoin() {
                return Join.this;
            }

            public Object getFJGroup() {
                return Join.this;
            }

            public String toString() {
                return Join.this.toString() + "." + property;
            }

            public Type getType(Where where) {
                return property.type;
            }

            // возвращает Where без следствий
            protected Where calculateWhere() {
                return new NotNull();
            }

            @Override
            public boolean equals(Object o) {
                return this == o || o instanceof Expr && Join.this.equals(((Expr) o).getJoin()) && property.equals(((Expr) o).property);
            }

            public int hashContext(HashContext hashContext) {
                return Join.this.hashContext(hashContext)*31+property.hashCode();
            }

            public String getSource(CompileSource compile) {
                return compile.getSource(this);
            }

            public class NotNull extends MapExpr.NotNull {

                protected DataWhereSet getExprFollows() {
                    return Join.this.getExprFollows();
                }

                public InnerJoins getInnerJoins() {
                    return new InnerJoins(Join.this,this);
                }

                public ClassExprWhere calculateClassWhere() {
                    return propertyClasses.get(property).map(BaseUtils.merge(joins,Collections.singletonMap(property,Expr.this))).and(Join.this.getJoinsWhere().getClassWhere());
                }
            }
        }

        @Override
        public String toString() {
            return Table.this.toString();
        }
    }
}
