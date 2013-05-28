package platform.server.caches;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.translator.MapTranslate;

public interface OuterContext<T extends OuterContext<T>> extends PackInterface<T>, TranslateContext<T> {

    ImSet<ParamExpr> getOuterKeys();

    ImSet<Value> getOuterValues();

    ImSet<StaticValueExpr> getOuterStaticValues();

    int hashOuter(HashContext hashContext);

    ImSet<OuterContext> getOuterDepends();

    T translateOuter(MapTranslate translator);

    boolean enumerate(ExprEnumerator enumerator);
}
