package platform.server.data.expr.cases;

import platform.server.data.where.Where;

import java.util.ArrayList;

public abstract class CaseList<D,C extends Case<D>> extends ArrayList<C> {

    protected Where upWhere;

    public CaseList() {
        upWhere = Where.FALSE;
    }

    public CaseList(Where falseWhere) {
        upWhere = falseWhere;
    }

    public Where getWhere(CaseWhereInterface<D> caseInterface) {

        Where result = Where.FALSE;
        Where up = Where.FALSE;
        for(C cCase : this) {
            result = result.or(cCase.where.and(caseInterface.getWhere(cCase.data)).and(up.not()));
            up = up.or(cCase.where);
        }

        return result.or(caseInterface.getElse().and(up.not()));
    }

    public abstract void add(Where where, D data);
    public abstract D getFinal();
}
