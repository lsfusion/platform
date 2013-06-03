package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.MCaseList;
import lsfusion.server.data.expr.where.cases.MapCase;
import lsfusion.server.data.expr.where.cases.MapCaseList;
import lsfusion.server.data.where.Where;

public abstract class AddPullWheres<K,R> extends PullWheres<R, K> {

    protected abstract MCaseList<R, ?, ?> initCaseList(boolean exclusive);
    protected abstract R proceedIf(Where ifWhere, R resultTrue, R resultFalse);

    protected R proceedIf(Where ifWhere, ImMap<K, Expr> mapTrue, ImMap<K, Expr> mapFalse) {
        return proceedIf(ifWhere, proceed(mapTrue), proceed(mapFalse)); 
    }

    protected R proceedCases(MapCaseList<K> cases) {
        MCaseList<R, ?, ?> caseList = initCaseList(cases.exclusive);
        for(MapCase<K> exprCase : cases)
            caseList.add(exprCase.where, proceed(exprCase.data));
        return caseList.getFinal();
    }
}
