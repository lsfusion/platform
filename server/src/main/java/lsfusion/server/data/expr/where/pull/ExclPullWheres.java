package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.MapCase;
import lsfusion.server.data.expr.where.cases.MapCaseList;
import lsfusion.server.data.where.Where;

public abstract class ExclPullWheres<R, K, W extends AndContext<W>> {

    protected abstract R proceedBase(W data, ImMap<K, BaseExpr> map); // assert !supportNulls
    protected abstract R initEmpty(); // assert !supportNulls

    protected boolean supportNulls() {
        return false;
    }
    protected R proceedNullBase(W data, ImMap<K, ? extends Expr> map) {
        return proceedBase(data, (ImMap<K, BaseExpr>) map);
    }

    protected abstract R add(R op1, R op2);

    public R proceed(final W data, ImMap<K, ? extends Expr> map) {
        return new PullWheres<R, K>() {
            protected R proceedCases(MapCaseList<K> cases, ImMap<K, Expr> nullCase) {
                R result = null;
                boolean exclusive = cases.exclusive;
                boolean supportNulls = supportNulls();
                Where upWhere = Where.FALSE();
                for(MapCase<K> exprCase : cases) {
                    Where caseWhere = exprCase.where;
                    if(!exclusive)
                        caseWhere = caseWhere.and(upWhere.not());
                    if(!exclusive || supportNulls)
                        upWhere = upWhere.or(caseWhere);

                    R caseResult = ExclPullWheres.this.proceed(data.and(caseWhere), exprCase.data);

                    if(result == null)
                        result = caseResult;
                    else
                        result = add(result, caseResult);
                }
                if(supportNulls) {
                    assert result != null; // see where proceedCases is called and isNull check
                    result = add(result, ExclPullWheres.this.proceed(data.and(upWhere.not()), nullCase));
                } else {
                    if (result == null)
                        result = initEmpty();
                }
                return result;
            }

            protected R initEmpty() {
                return ExclPullWheres.this.initEmpty();
            }

            protected R proceedIf(Where ifWhere, ImMap<K, Expr> mapTrue, ImMap<K, Expr> mapFalse) {
                return add(ExclPullWheres.this.proceed(data.and(ifWhere), mapTrue), 
                           ExclPullWheres.this.proceed(data.and(ifWhere.not()), mapFalse));
            }

            protected R proceedBase(ImMap<K, BaseExpr> map) {
                throw new UnsupportedOperationException();
            }

            protected boolean supportNulls() {
                return ExclPullWheres.this.supportNulls();
            }

            protected R proceedNullBase(ImMap<K, ? extends Expr> map) {
                return ExclPullWheres.this.proceedNullBase(data, map);
            }
        }.proceed(map);
    }

}
