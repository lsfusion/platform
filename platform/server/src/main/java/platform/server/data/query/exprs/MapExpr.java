package platform.server.data.query.exprs;

import platform.base.QuickMap;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.OrObjectClassSet;
import platform.server.data.query.*;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.TranslateExprLazy;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

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

    private SourceExpr classExpr;
    public SourceExpr getClassExpr(BaseClass baseClass) {
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

    public Where getIsClassWhere(AndClassSet set) {
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

    public SourceExpr getFJExpr() {
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

        public void fillContext(Context context) {
            MapExpr.this.fillContext(context);
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

    public static <K> DataWhereSet getExprFollows(Map<K,AndExpr> map) {
        DataWhereSet follows = new DataWhereSet();
        for(AndExpr expr : map.values())
            follows.addAll(expr.getFollows());
        return follows;        
    }

    public static Where getWhere(Collection<AndExpr> col) {
        Where joinsWhere = Where.TRUE;
        for(AndExpr expr : col)
            joinsWhere = joinsWhere.and(expr.getWhere());
        return joinsWhere;

    }

    public static <K> Where getJoinsWhere(Map<K, AndExpr> map) {
        return getWhere(map.values());
    }
}
