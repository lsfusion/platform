package lsfusion.server.data.query.compile.where;

import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.JoinExprTranslator;
import lsfusion.server.data.where.Where;

public interface UpWhere {

    UpWhere or(UpWhere upWhere);

    UpWhere and(UpWhere upWhere);

    UpWhere not();

    Where getWhere(JoinExprTranslator translator);

    // rebuilds the up where with its expressions translated; used when the join it belongs to is rewritten (see WhereJoins.removeJoin), otherwise getWhere would resurrect the original expressions
    // returns null if it cannot be rebuilt consistently (the caller should fall back and not use the rewritten join)
    default UpWhere translateExpr(ExprTranslator translator) {
        return this;
    }
}
