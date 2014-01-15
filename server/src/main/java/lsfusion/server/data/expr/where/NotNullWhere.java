package lsfusion.server.data.expr.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWhere;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

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
        return NotNullExpr.getFollows(getExpr().getExprFollows(false, NotNullExpr.FOLLOW, true));
    }

    public boolean twins(TwinImmutableObject o) {
        return getExpr().equals(((NotNullWhere) o).getExpr());
    }
}
