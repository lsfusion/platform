package platform.server.data.query;

import platform.server.data.query.exprs.ObjectExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.where.Where;

public interface Translator {

    Where translate(JoinWhere where);
    SourceExpr translate(ObjectExpr expr);

    boolean direct();
}
