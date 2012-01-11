package platform.server.data.query;

import platform.server.caches.OuterContext;

import java.util.Collection;
import java.util.Map;

public interface ExprEnumerator {

    boolean enumerate(OuterContext join);

}
