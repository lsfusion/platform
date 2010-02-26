package platform.server.caches;

import platform.server.data.translator.KeyTranslator;
import platform.server.caches.HashContext;
import platform.server.data.query.SourceJoin;

public interface TranslateContext<This extends TranslateContext> {

    int hashContext(HashContext hashContext);

    This translateDirect(KeyTranslator translator);

    SourceJoin[] getEnum();
}
