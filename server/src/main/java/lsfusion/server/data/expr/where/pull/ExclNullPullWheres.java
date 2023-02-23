package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;

public abstract class ExclNullPullWheres<R, K, W extends AndContext<W>> extends ExclPullWheres<R, K, W> {

    @Override
    protected boolean supportNulls() {
        return true;
    }

    @Override
    protected R proceedBase(W data, ImMap<K, BaseExpr> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected R initEmpty() {
        throw new UnsupportedOperationException();
    }
}
