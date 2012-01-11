package platform.server.caches;

import platform.base.*;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

import java.util.Map;
import java.util.Set;

public interface InnerContext<I extends InnerContext<I>> extends InnerHashContext, ValuesContext<I>, TwinImmutableInterface {

    I translateInner(MapTranslate translate);

    QuickSet<Value> getInnerValues();

    BaseUtils.HashComponents<KeyExpr> getInnerComponents(boolean values);
    int hashInner(boolean values);
    QuickMap<KeyExpr, GlobalObject> getInnerMap(boolean values);

    MapTranslate mapInner(I object, boolean values);
    boolean equalsInner(I object); // проверка на соответствие если одинаковые контексты, на самом деле protected
}
