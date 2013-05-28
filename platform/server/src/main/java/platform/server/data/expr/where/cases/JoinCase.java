package platform.server.data.expr.where.cases;

import platform.server.data.expr.where.Case;
import platform.server.data.query.Join;
import platform.server.data.where.Where;

public class JoinCase<U> extends Case<Join<U>> {

    // дублируем чтобы различать
    public JoinCase(Where where, Join<U> join) {
        super(where, join);
    }

}
