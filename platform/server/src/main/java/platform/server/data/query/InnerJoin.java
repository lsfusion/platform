package platform.server.data.query;

import platform.server.data.where.DataWhereSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.expr.VariableExprSet;
import platform.server.caches.hash.HashContext;

public interface InnerJoin {
    VariableExprSet getJoinFollows();

    int hashContext(HashContext hashContext);

    InnerJoin translateDirect(DirectTranslator translator);

    boolean isIn(VariableExprSet set);
}
