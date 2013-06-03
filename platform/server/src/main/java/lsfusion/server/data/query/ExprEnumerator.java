package lsfusion.server.data.query;

import lsfusion.server.caches.OuterContext;

public interface ExprEnumerator {

    Boolean enumerate(OuterContext join);

}
