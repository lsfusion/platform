package lsfusion.server.data.query.build;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.pull.AndContext;
import lsfusion.server.data.translate.TranslateValues;
import lsfusion.server.data.where.Where;

public interface Join<U> extends AndContext<Join<U>>, TranslateValues<Join<U>> {

    Expr getExpr(U property);
    Where getWhere();

}

