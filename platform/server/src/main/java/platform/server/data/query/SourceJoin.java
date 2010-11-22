package platform.server.data.query;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.where.Where;

import java.util.Set;

public interface SourceJoin {

    String getSource(CompileSource compile);

    //    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    void enumDepends(ExprEnumerator enumerator);

    void enumerate(ExprEnumerator enumerator);
    void enumKeys(Set<KeyExpr> keys);
    void enumValues(Set<ValueExpr> values);

    // для дебага, определяет сложность выражения, предполагается что например packFollowFalse уменьшает сложность выражения чтобы не допустить рекурсию
    public abstract long getComplexity();
}
