package platform.server.data.where;

import platform.base.QuickSet;
import platform.server.data.expr.BaseExpr;

import java.util.List;
import java.util.Collection;

public class DataWhereSet extends QuickSet<DataWhere> {

    public DataWhereSet() {
    }

    public DataWhereSet(DataWhereSet set) {
        super(set);
    }

    public DataWhereSet(DataWhereSet[] sets) {
        super(sets);
    }

    public DataWhereSet(Collection<BaseExpr> exprs) {
        for(BaseExpr expr : exprs)
            addAll(expr.getFollows());
    }
}

