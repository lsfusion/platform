package lsfusion.server.data.expr.where.extra;

import lsfusion.base.TwinImmutableObject;
import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.where.Where;

// если operator1 не null и больше operator2 или operator2 null
public class GreaterWhere extends CompareWhere {

    public final boolean orEquals; // упрощает компиляцию, но не разбирает некоторые случаи, потом надо будет доделать

    // public только для symmetricWhere
    public GreaterWhere(BaseExpr operator1, BaseExpr operator2, boolean orEquals) {
        super(operator1, operator2);

        this.orEquals = orEquals;
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2, boolean orEquals) {
        if(operator1.compatibleEquals(operator2))
            return orEquals ? operator1.getWhere() : FALSE;
        return create(operator1, operator2, new GreaterWhere(operator1, operator2, orEquals));
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return (orEquals ? 2 : 1) + operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31*31;
    }

    @Override
    public boolean twins(TwinImmutableObject obj) {
        return super.twins(obj) && orEquals == ((GreaterWhere)obj).orEquals;
    }

    protected CompareWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new GreaterWhere(operator1, operator2, orEquals);
    }

    protected Compare getCompare() {
        return orEquals ? Compare.GREATER_EQUALS : Compare.GREATER;
    }

    protected String getCompareSource(CompileSource compile) {
        return ">" + (orEquals ? "=" : "");
    }

    protected boolean isEquals() {
        return false;
    }
}
