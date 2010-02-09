package platform.server.data.expr;

import platform.base.QuickMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.expr.where.IsClassWhere;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

@TranslateExprLazy
public abstract class MapExpr extends VariableClassExpr implements JoinData {

    private OrObjectClassSet getSet() {
        OrObjectClassSet result = OrObjectClassSet.FALSE;
        for(QuickMap<VariableClassExpr,AndClassSet> where : getWhere().getClassWhere().getAnds())
            result = result.or(where.get(this).getOr());
        return result;
    }

    private Expr classExpr;
    public Expr classExpr(BaseClass baseClass) {
        if(classExpr==null) {
            ConcreteObjectClass singleClass = getSet().getSingleClass(baseClass);
            if(singleClass!=null)
                classExpr = singleClass.getIDExpr().and(getWhere());
            else
                classExpr = new IsClassExpr(this,baseClass);
        }
        return classExpr;
    }

    private boolean intersect(AndClassSet set) {
        for(QuickMap<VariableClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds())
            if(!where.get(this).and(set).isEmpty()) return true;
        return false;
    }

    public Where isClass(AndClassSet set) {
        // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
        if(!intersect(set)) // если не пересекается то false
            return Where.FALSE;
        if(getSet().containsAll(set.getOr())) // если set содержит все элементы, то достаточно просто что не null
            return getWhere();
        return new IsClassWhere(this,set);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    public DataWhereSet getFollows() {
        return ((DataWhere)getWhere()).getFollows();
    }

    public abstract class NotNull extends DataWhere {

        public MapExpr getExpr() {
            return MapExpr.this;
        }

        public String getSource(CompileSource compile) {
            return MapExpr.this.getSource(compile) + " IS NOT NULL";
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return MapExpr.this.getSource(compile) + " IS NULL";
        }

        public Where translateDirect(KeyTranslator translator) {
            return MapExpr.this.translateDirect(translator).getWhere();
        }
        public Where translateQuery(QueryTranslator translator) {
            return MapExpr.this.translateQuery(translator).getWhere();
        }

        public void enumerate(SourceEnumerator enumerator) {
            MapExpr.this.enumerate(enumerator);
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            MapExpr.this.fillAndJoinWheres(joins,andWhere);
        }

        public int hashContext(HashContext hashContext) {
            return MapExpr.this.hashContext(hashContext);
        }

        @Override
        public boolean twins(AbstractSourceJoin o) {
            return MapExpr.this.equals(((NotNull) o).getExpr());
        }
    }

    public static <K> DataWhereSet getExprFollows(Map<K, BaseExpr> map) {
        DataWhereSet follows = new DataWhereSet();
        for(BaseExpr expr : map.values())
            follows.addAll(expr.getFollows());
        return follows;        
    }

    public static Where getWhere(Collection<BaseExpr> col) {
        Where joinsWhere = Where.TRUE;
        for(BaseExpr expr : col)
            joinsWhere = joinsWhere.and(expr.getWhere());
        return joinsWhere;

    }

    public static <K> Where getJoinsWhere(Map<K, BaseExpr> map) {
        return getWhere(map.values());
    }
}
