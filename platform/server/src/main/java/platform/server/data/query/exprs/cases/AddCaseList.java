package platform.server.data.query.exprs.cases;

import platform.server.where.Where;

public abstract class AddCaseList<D,C extends Case<D>> extends CaseList<D,C> {

    Where upWhere;
    
    public AddCaseList() {
        upWhere = Where.FALSE;
    }

    AddCaseList(Where falseWhere) {
        upWhere = falseWhere;
    }
}

