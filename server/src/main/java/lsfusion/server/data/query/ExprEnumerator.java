package lsfusion.server.data.query;

import lsfusion.server.base.caches.OuterContext;

public interface ExprEnumerator {

    Boolean enumerate(OuterContext join);

}
