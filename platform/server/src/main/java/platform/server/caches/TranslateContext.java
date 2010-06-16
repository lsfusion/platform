package platform.server.caches;

import platform.server.caches.hash.HashContext;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;

public interface TranslateContext<This extends TranslateContext> {

    int hashContext(HashContext hashContext);

    This translate(MapTranslate translator);

    SourceJoin[] getEnum();
}
