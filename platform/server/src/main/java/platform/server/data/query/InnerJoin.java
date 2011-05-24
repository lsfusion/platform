package platform.server.data.query;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

import java.util.Collection;
import java.util.Set;
import java.util.Map;

public interface InnerJoin<J> {
    VariableExprSet getJoinFollows();

    int hashOuter(HashContext hashContext);

    InnerJoin translateOuter(MapTranslate translator);

    boolean isIn(VariableExprSet set);

    Set<J> insufficientKeys();
    Map<J, BaseExpr> getJoins();
}
