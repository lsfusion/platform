package platform.server.data.query;

import platform.server.data.where.DataWhereSet;
import platform.server.data.translator.DirectTranslator;
import platform.server.caches.hash.HashContext;

public interface InnerJoin {
    DataWhereSet getJoinFollows();

    int hashContext(HashContext hashContext);

    InnerJoin translateDirect(DirectTranslator translator);

    boolean isIn(DataWhereSet set);
}
