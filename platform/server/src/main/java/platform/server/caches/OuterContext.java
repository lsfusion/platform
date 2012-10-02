package platform.server.caches;

import platform.base.QuickSet;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;

import java.util.Set;

public interface OuterContext<T extends OuterContext> extends PackInterface<T> {

    QuickSet<KeyExpr> getOuterKeys();

    QuickSet<Value> getOuterValues();

    int hashOuter(HashContext hashContext);

    T translateOuter(MapTranslate translator);

    QuickSet<OuterContext> getOuterDepends();

    boolean enumerate(ExprEnumerator enumerator);
}
