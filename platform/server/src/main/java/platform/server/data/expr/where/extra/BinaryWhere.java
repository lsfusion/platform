package platform.server.data.expr.where.extra;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.*;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.List;

public abstract class BinaryWhere<This extends BinaryWhere<This>> extends DataWhere {

    public final BaseExpr operator1;
    public final BaseExpr operator2;

    protected BinaryWhere(BaseExpr operator1, BaseExpr operator2) {
        this.operator1 = operator1;
        this.operator2 = operator2;
    }

    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(operator1, operator2);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public DataWhereSet calculateFollows() {
        return new DataWhereSet(new NotNullExprSet(BaseUtils.toSet(operator1, operator2), true));
    }

    protected abstract This createThis(BaseExpr operator1, BaseExpr operator2);
    protected abstract Compare getCompare();

    protected Where translate(MapTranslate translator) {
        return createThis(operator1.translateOuter(translator),operator2.translateOuter(translator));
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return operator1.translateQuery(translator).compare(operator2.translateQuery(translator),getCompare());
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        Expr packOperator1 = operator1.packFollowFalse(falseWhere);
        Expr packOperator2 = operator2.packFollowFalse(falseWhere);
        if(BaseUtils.hashEquals(packOperator1, operator1) && BaseUtils.hashEquals(packOperator2, operator2))
            return this;
        else
            return packOperator1.compare(packOperator2, getCompare());
    }

    public WhereJoin groupJoinsWheres(List<Expr> orderTop, boolean not) {
        if(operator1.isValue()) {
            if(operator2.isTableIndexed() && orderTop.contains(operator2))
                return new ExprOrderTopJoin(operator2, getCompare().reverse(), operator1, not);
            if(getCompare().equals(Compare.EQUALS) && !not)
                return new ExprStatJoin(operator2, Stat.ONE, operator1);
        }
        if(operator2.isValue()) {
            if(operator1.isTableIndexed() && orderTop.contains(operator1))
                return new ExprOrderTopJoin(operator1, getCompare(), operator2, not);
            if(getCompare().equals(Compare.EQUALS) && !not)
                return new ExprStatJoin(operator1, Stat.ONE, operator2);
        }
        if(getCompare().equals(Compare.EQUALS) && !not)
            return new ExprEqualsJoin(operator1, operator2);
        return null;        
    }
    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<Expr> orderTop, boolean noWhere) {
        WhereJoin exprJoin = groupJoinsWheres(orderTop, false);
        if(exprJoin!=null)
            return new GroupJoinsWheres(exprJoin, this, noWhere);
        return getOperandWhere().groupJoinsWheres(keepStat, keyStat, orderTop, noWhere).and(new GroupJoinsWheres(this, noWhere));
    }

    @IdentityLazy
    protected Where getOperandWhere() {
        return operator1.getNotNullWhere().and(operator2.getNotNullWhere());
    }

    public ClassExprWhere calculateClassWhere() {
        return getOperandWhere().getClassWhere();
    }

    public boolean twins(TwinImmutableInterface obj) {
        return operator1.equals(((BinaryWhere)obj).operator1) && operator2.equals(((BinaryWhere)obj).operator2);
    }

    protected abstract String getCompareSource(CompileSource compile);
    public String getSource(CompileSource compile) {
        return operator1.getSource(compile) + getCompareSource(compile) + operator2.getSource(compile);
    }

    protected static Where create(BaseExpr operator1, BaseExpr operator2, BinaryWhere where) {
        return create(where).and(operator1.getOrWhere().and(operator2.getOrWhere()));
    }
}
