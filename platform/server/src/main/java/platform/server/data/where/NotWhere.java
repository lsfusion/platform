package platform.server.data.where;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.extra.BinaryWhere;
import platform.server.data.expr.where.extra.IsClassWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.where.classes.PackClassWhere;

public class NotWhere extends ObjectWhere {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        for(OrObjectWhere orWhere : meanWhere.getOr())
            if(orWhere instanceof NotWhere && where.follow(((NotWhere)orWhere).where))
                return true;
        return false;
    }

    public DataWhere not() {
        return where;
    }

    final static String PREFIX = "NOT ";

    public boolean twins(TwinImmutableObject o) {
        return where.equals(((NotWhere)o).where);
    }

    public int hash(HashContext hashContext) {
        return where.hashOuter(hashContext)*31;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected Where translate(MapTranslate translator) {
        return where.translateOuter(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return where.translateQuery(translator).not();
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(where);
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return PREFIX + where.getSource(compile);

        return where.getNotSource(compile);
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
        WhereJoin exprJoin;
        if(where instanceof BinaryWhere && (exprJoin=((BinaryWhere)where).groupJoinsWheres(orderTop, true))!=null)
            return new GroupJoinsWheres(exprJoin, this, noWhere);
        return new GroupJoinsWheres(this, noWhere);
    }

    public MeanClassWheres calculateMeanClassWheres(boolean useNots) {
        if(useNots && (where instanceof IsClassWhere || where instanceof PackClassWhere))
            return new MeanClassWheres(new MeanClassWhere(where.getClassWhere(), true), this);
        return new MeanClassWheres(MeanClassWhere.TRUE,this);
    }

    public ClassExprWhere calculateClassWhere() {
        return ClassExprWhere.TRUE;
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return where.packFollowFalse(falseWhere).not();
    }

    public boolean isNot() {
        return true;
    }
}
