package platform.server.caches;

import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;

import java.util.Set;

public interface OuterContext<This extends OuterContext> {

    Set<Value> getOuterValues();

    int hashOuter(HashContext hashContext);

    This translateOuter(MapTranslate translator);

    SourceJoin[] getEnum();
}
