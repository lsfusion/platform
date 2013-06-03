package lsfusion.server.data.expr.where.cases;

import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.where.Where;

public class JoinCase<U> extends Case<Join<U>> {

    // дублируем чтобы различать
    public JoinCase(Where where, Join<U> join) {
        super(where, join);
    }

}
