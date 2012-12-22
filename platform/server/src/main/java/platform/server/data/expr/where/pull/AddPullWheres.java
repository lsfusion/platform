package platform.server.data.expr.where.pull;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.MCaseList;
import platform.server.data.expr.where.cases.MapCase;
import platform.server.data.expr.where.cases.MapCaseList;
import platform.server.data.where.Where;

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
