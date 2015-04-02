package lsfusion.server.data.expr.where.extra;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.Compare;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExprEqualsJoin;
import lsfusion.server.data.query.ExprOrderTopJoin;
import lsfusion.server.data.query.ExprStatJoin;
import lsfusion.server.data.query.innerjoins.KeyEquals;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.where.Where;

public class EqualsWhere extends CompareWhere<EqualsWhere> {

    // public только для symmetricWhere
    public EqualsWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(operator1.compatibleEquals(operator2))
            return operator1.getWhere();
        if(operator1 instanceof StaticExpr && operator1.getClass() == operator2.getClass())
            return FALSE;
        return create(operator1, operator2, new EqualsWhere(operator1, operator2));
    }

    public EqualsWhere(ParamExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected String getCompareSource(CompileSource compile) {
        return "=";
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator1) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator2) ||
               (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator2) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator1)));
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31;
    }

    protected EqualsWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new EqualsWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.EQUALS;
    }

    @Override
    public KeyEquals calculateKeyEquals() {
        if(operator1 instanceof ParamExpr && !operator2.hasKey((ParamExpr) operator1))
            return new KeyEquals((ParamExpr) operator1, operator2);
        if(operator2 instanceof ParamExpr && !operator1.hasKey((ParamExpr) operator2))
            return new KeyEquals((ParamExpr) operator2, operator1);
        return super.calculateKeyEquals();
    }

    protected boolean isEquals() {
        return true;
    }

    @Override
    public WhereJoin groupJoinsWheres(ImOrderSet<Expr> orderTop) {
        if (operator1.isValue())
            return new ExprStatJoin(operator2, Stat.ONE, operator1);
        if (operator2.isValue())
            return new ExprStatJoin(operator1, Stat.ONE, operator2);
        return new ExprEqualsJoin(operator1, operator2); // тут тоже assertion из calculateKeyEquals'а есть, но он сложный поэтому пока прописывать не будем
    }
}
