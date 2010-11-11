package platform.server.caches;

import platform.server.caches.hash.HashContext;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;

public interface OuterContext<This extends OuterContext> {

    int hashOuter(HashContext hashContext);

    This translateOuter(MapTranslate translator);

    SourceJoin[] getEnum();
}
