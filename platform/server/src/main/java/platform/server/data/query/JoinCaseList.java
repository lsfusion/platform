package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseList;
import platform.server.data.query.exprs.cases.CaseWhereInterface;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.where.Where;

public class JoinCaseList<U> extends CaseList<Join<U>,JoinCase<U>> {

    public SourceExpr getExpr(U property) {
        ExprCaseList result = new ExprCaseList();
        for(JoinCase<U> joinCase : this)
            result.add(joinCase.where,joinCase.data.getExpr(property));
        return result.getExpr();
    }

    public JoinCaseList() {
    }

    public JoinCaseList(Where where,Join<U> join) {
        add(new JoinCase<U>(where,join));
    }

    public Where getWhere() {
        return getWhere(new CaseWhereInterface<Join<U>>() {
            public Where getWhere(Join<U> cCase) {
                return cCase.getWhere();
           }
        });
    }
}
