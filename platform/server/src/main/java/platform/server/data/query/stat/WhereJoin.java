package platform.server.data.query.stat;

import platform.base.Result;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Map;

public interface WhereJoin<K> extends BaseJoin<K> {

    InnerJoins getInnerJoins(); // для компиляции, то есть ClassJoin дает join c objects'ом
    InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres);

    int hashOuter(HashContext hashContext);

    WhereJoin translateOuter(MapTranslate translator);
}
