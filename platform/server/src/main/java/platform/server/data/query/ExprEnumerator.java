package platform.server.data.query;

import platform.server.caches.OuterContext;

public interface ExprEnumerator {

    Boolean enumerate(OuterContext join);

}
