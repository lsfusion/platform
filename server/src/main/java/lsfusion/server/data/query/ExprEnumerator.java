package lsfusion.server.data.query;

import lsfusion.server.data.caches.OuterContext;

public interface ExprEnumerator {

    Boolean enumerate(OuterContext join);

}
