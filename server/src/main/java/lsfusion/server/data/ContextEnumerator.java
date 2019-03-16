package lsfusion.server.data;

import lsfusion.server.data.caches.OuterContext;

public interface ContextEnumerator {

    Boolean enumerate(OuterContext join);

}
