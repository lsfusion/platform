package lsfusion.server.data.query.stat;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.OuterContext;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.InnerJoins;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.translator.MapTranslate;

public interface WhereJoin<K, T extends WhereJoin<K, T>> extends BaseJoin<K>, OuterContext<T> {

    InnerJoins getInnerJoins(); // для компиляции, то есть ClassJoin дает join c objects'ом
    InnerJoins getJoinFollows(Result<UpWheres<InnerJoin>> upWheres, MSet<UnionJoin> mUnionJoins);

    T translateOuter(MapTranslate translate); // прикол с generics'ами, но java ругается если
}
