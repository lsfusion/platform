package platform.server.data.query.stat;

import platform.base.Result;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.OuterContext;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

public interface WhereJoin<K, T extends WhereJoin<K, T>> extends BaseJoin<K>, OuterContext<T> {

    InnerJoins getInnerJoins(); // для компиляции, то есть ClassJoin дает join c objects'ом
    InnerJoins getJoinFollows(Result<ImMap<InnerJoin,Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins);

    T translateOuter(MapTranslate translate); // прикол с generics'ами, но java ругается если
}
