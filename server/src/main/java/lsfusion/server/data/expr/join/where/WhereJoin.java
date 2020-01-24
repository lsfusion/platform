package lsfusion.server.data.expr.join.where;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.base.BaseJoin;
import lsfusion.server.data.expr.join.base.UnionJoin;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.translate.MapTranslate;

import java.util.function.Predicate;

public interface WhereJoin<K, T extends WhereJoin<K, T>> extends BaseJoin<K>, OuterContext<T> {

    InnerJoins getInnerJoins(); // для компиляции, то есть ClassJoin дает join c objects'ом
    
    default InnerJoins getJoinFollows(Result<UpWheres<InnerJoin>> upWheres, Predicate<UnionJoin> removeUnionJoins) {
        InnerJoins result = InnerJoins.EMPTY;
        UpWheres<InnerJoin> upResult = UpWheres.EMPTY();
        ImSet<InnerExpr> innerExprs = NullableExpr.getInnerExprs(getExprFollows(NullableExpr.INNERJOINS, false), removeUnionJoins);
        for(int i=0,size=innerExprs.size();i<size;i++) {
            InnerExpr innerExpr = innerExprs.get(i);
            InnerJoin innerJoin = innerExpr.getInnerJoin();
            result = result.and(new InnerJoins(innerJoin));
            upResult = result.andUpWheres(upResult, new UpWheres<>(innerJoin, innerExpr.getUpNotNullWhere()));
        }
        upWheres.set(upResult);
        return result;
    }

    T translateOuter(MapTranslate translate); // прикол с generics'ами, но java ругается если
}
