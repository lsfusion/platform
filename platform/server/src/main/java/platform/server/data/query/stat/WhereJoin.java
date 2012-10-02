package platform.server.data.query.stat;

import platform.base.Result;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

public interface WhereJoin<K, T extends WhereJoin> extends BaseJoin<K>, OuterContext<T> {

    InnerJoins getInnerJoins(); // для компиляции, то есть ClassJoin дает join c objects'ом
    InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins);

    T translateOuter(MapTranslate translate); // прикол с generics'ами, но java ругается если
}
