package lsfusion.server.data.expr.where.extra;

import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.where.OrObjectWhere;
import lsfusion.server.data.where.Where;

// если operator1 не null и больше operator2 или operator2 null
public class GreaterWhere<T> extends CompareWhere<GreaterWhere<T>> {

    public final boolean orEquals; // упрощает компиляцию, но не разбирает некоторые случаи, потом надо будет доделать

    // public только для symmetricWhere
    public GreaterWhere(BaseExpr operator1, BaseExpr operator2, boolean orEquals) {
        super(operator1, operator2);

        this.orEquals = orEquals;
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2, boolean orEquals) {
        if(checkEquals(operator1,operator2))
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
    public boolean calcTwins(TwinImmutableObject obj) {
        return super.calcTwins(obj) && orEquals == ((GreaterWhere)obj).orEquals;
    }

    protected GreaterWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new GreaterWhere(operator1, operator2, orEquals);
    }

    protected Compare getCompare() {
        return orEquals ? Compare.GREATER_EQUALS : Compare.GREATER;
    }

    private Where symmetricGreaterWhere = null;
    // A>(>=)B = !A<=(<)B AND A AND B
    private Where getSymmetricGreaterWhere() { // так EqualsWhere не появляются, в контексте использования (groupNotJoinsWheres) это удобно
        if (symmetricGreaterWhere == null) {
            GreaterWhere backCompare = new GreaterWhere(operator2, operator1, !orEquals);

            OrObjectWhere[] operators = getOperandWhere().getOr();
            OrObjectWhere[] symmetricOrs = new OrObjectWhere[operators.length + 1];
            System.arraycopy(operators, 0, symmetricOrs, 0, operators.length);
            symmetricOrs[operators.length] = backCompare.not();
            symmetricGreaterWhere = toWhere(symmetricOrs);
        }
        return symmetricGreaterWhere;
    }

    // тут есть нюанс, что может неявно появится keyEquals (при текущей реализации с orEquals не появится), поэтому правильнее может быть было бы в getKeyEquals перенести, но пока не важно
    @Override
    public <K extends BaseExpr> GroupJoinsWheres groupNotJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        if (needIndexedJoin(operator2, orderTop, operator1, null) || // избаляемся от not'ов, NOT EQUALS не интересует так как в индексе не помогает
                needIndexedJoin(operator1, orderTop, operator2, null))
            return getSymmetricGreaterWhere().not().groupJoinsWheres(keepStat, keyStat, orderTop, type);

        return super.groupNotJoinsWheres(keepStat, keyStat, orderTop, type);
    }

    protected String getCompareSource(CompileSource compile) {
        return ">" + (orEquals ? "=" : "");
    }

    protected boolean isEquals() {
        return false;
    }

    @Override
    protected boolean adjustSelectivity(SQLSyntax syntax) {
        return syntax.hasSelectivityProblem();
    }
}
