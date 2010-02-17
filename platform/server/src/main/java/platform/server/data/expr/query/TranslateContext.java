package platform.server.data.expr.query;

import platform.server.data.translator.KeyTranslator;
import platform.server.data.query.HashContext;
import platform.server.data.query.SourceJoin;

public interface TranslateContext<This extends TranslateContext> {

    int hashContext(HashContext hashContext);

    This translateDirect(KeyTranslator translator);

    SourceJoin[] getEnum();
}
