package platform.server.data.query;

import platform.server.caches.OuterContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;

import java.util.Set;

public interface SourceJoin<T extends SourceJoin<T>> extends OuterContext<T> {

    String getSource(CompileSource compile);

    //    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    void enumDepends(ExprEnumerator enumerator);

    void enumerate(ExprEnumerator enumerator);
    void enumKeys(Set<KeyExpr> keys);

    void enumValues(Set<Value> values);
    void enumInnerValues(Set<Value> values);

    // для дебага, определяет сложность выражения, предполагается что например packFollowFalse уменьшает сложность выражения чтобы не допустить рекурсию
    long getComplexity();

    T and(Where where);
}
