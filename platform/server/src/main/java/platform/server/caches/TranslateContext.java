package platform.server.caches;

import platform.server.data.translator.DirectTranslator;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.SourceJoin;

public interface TranslateContext<This extends TranslateContext> {

    int hashContext(HashContext hashContext);

    This translateDirect(DirectTranslator translator);

    SourceJoin[] getEnum();
}
