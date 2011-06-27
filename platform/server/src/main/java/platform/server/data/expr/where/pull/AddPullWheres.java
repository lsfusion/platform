package platform.server.data.expr.where.pull;

import platform.server.data.expr.where.cases.CaseList;
import platform.server.data.expr.where.cases.MapCase;
import platform.server.data.expr.where.cases.MapCaseList;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.Map;

public abstract class AddPullWheres<K,R> extends PullWheres<R, K> {

    protected abstract CaseList<R, ?, ?> initCaseList();
    protected abstract R proceedIf(Where ifWhere, R resultTrue, R resultFalse);

    protected R proceedIf(Where ifWhere, Map<K, Expr> mapTrue, Map<K, Expr> mapFalse) {
        return proceedIf(ifWhere, proceed(mapTrue), proceed(mapFalse)); 
    }

    protected R proceedCases(MapCaseList<K> cases) {
        CaseList<R, ?, ?> caseList = initCaseList();
        for(MapCase<K> exprCase : cases)
            caseList.add(exprCase.where, proceedBase(exprCase.data));
        return caseList.getFinal();
    }
}
