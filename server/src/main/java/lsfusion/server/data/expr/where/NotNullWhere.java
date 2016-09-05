package lsfusion.server.data.expr.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.Compare;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.expr.where.extra.BinaryWhere;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExprIndexedJoin;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

// из-за отсутствия множественного наследования приходится выделять (так было бы внутренним классом в NullableExpr)
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
    public <K extends BaseExpr> GroupJoinsWheres groupNotJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        BaseExpr expr = getExpr();
        Result<Boolean> isOrderTop = new Result<>();
        if(BinaryWhere.needIndexedJoin(expr, orderTop, null, isOrderTop))
            return groupDataNotJoinsWheres(new ExprIndexedJoin(expr, Compare.LESS, Expr.NULL, true, isOrderTop.result), type); // кривовато конечно, но пока достаточно
        return super.groupNotJoinsWheres(keepStat, statType, keyStat, orderTop, type);
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

    public Where translate(ExprTranslator translator) {
        Expr expr = getExpr();
        Expr translateExpr = expr.translateExpr(translator);
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

    protected ImSet<NullableExprInterface> getExprFollows() {
        return getExpr().getExprFollows(false, NullableExpr.FOLLOW, true);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return getExpr().equals(((NotNullWhere) o).getExpr());
    }
}
