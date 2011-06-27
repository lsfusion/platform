package platform.server.data.expr.where.cases;

import platform.server.data.expr.where.Case;
import platform.server.data.where.Where;
import platform.server.data.query.Join;

public class JoinCase<U> extends Case<Join<U>> {

    // дублируем чтобы различать
    public JoinCase(Where iWhere, Join<U> iJoin) {
        super(iWhere,iJoin);
    }

}
