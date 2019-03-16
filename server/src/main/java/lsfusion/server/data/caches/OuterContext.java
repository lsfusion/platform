package lsfusion.server.data.caches;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.ContextEnumerator;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.pack.PackInterface;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.TranslateContext;
import lsfusion.server.data.value.Value;

public interface OuterContext<T extends OuterContext<T>> extends PackInterface<T>, TranslateContext<T> {

    ImSet<ParamExpr> getOuterKeys();

    ImSet<Value> getOuterValues();

    ImSet<StaticValueExpr> getOuterStaticValues();

    int hashOuter(HashContext hashContext);

    ImSet<OuterContext> getOuterDepends();

    T translateOuter(MapTranslate translator);

    boolean enumerate(ContextEnumerator enumerator);
}
