package platform.server.data.expr.where;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;

// из-за отсутствия множественного наследования приходится выделять (так было бы внутренним классом в NotNullExpr)
public abstract class NotNullWhere extends DataWhere {

    protected abstract BaseExpr getExpr();

    protected boolean isComplex() {
        return false;
    }

    public String getSource(CompileSource compile) {
        return getExpr().getSource(compile) + " IS NOT NULL";
    }

    @Override
    protected String getNotSource(CompileSource compile) {
        return getExpr().getSource(compile) + " IS NULL";
    }

    protected Where translate(MapTranslate translator) {
        return getExpr().translateOuter(translator).getNotNullWhere();
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        BaseExpr expr = getExpr();
        Expr packExpr = expr.packFollowFalse(falseWhere);
//            if(packExpr instanceof BaseExpr) // чтобы бесконечных циклов не было
//                return ((BaseExpr)packExpr).getNotNullWhere();
        if(BaseUtils.hashEquals(packExpr, expr)) // чтобы бесконечных циклов не было
            return this;
        else
            return packExpr.getWhere();
    }

    public Where translateQuery(QueryTranslator translator) {
        Expr expr = getExpr();
        Expr translateExpr = expr.translateQuery(translator);
//            if(translateExpr instanceof BaseExpr) // ??? в pack на это нарвались, здесь по идее может быть аналогичная ситуация
//                return ((BaseExpr)translateExpr).getNotNullWhere();
        if(BaseUtils.hashEquals(translateExpr, expr)) // чтобы бесконечных циклов не было
            return this;
        else
            return translateExpr.getWhere();
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(getExpr());
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        getExpr().fillAndJoinWheres(joins,andWhere);
    }

    public int hash(HashContext hashContext) {
        return getExpr().hashOuter(hashContext);
    }

    protected ImSet<DataWhere> calculateFollows() {
        return NotNullExpr.getFollows(getExpr().getExprFollows(false, true));
    }

    public boolean twins(TwinImmutableObject o) {
        return getExpr().equals(((NotNullWhere) o).getExpr());
    }
}
