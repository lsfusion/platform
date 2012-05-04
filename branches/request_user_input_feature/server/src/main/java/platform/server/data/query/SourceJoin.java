package platform.server.data.query;

import platform.base.QuickSet;
import platform.server.caches.OuterContext;
import platform.server.caches.PackInterface;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;

import java.util.Set;

public interface SourceJoin<T extends SourceJoin<T>> extends OuterContext<T>, AndContext<T> {

    String getSource(CompileSource compile);

    T translateQuery(QueryTranslator translator);

    //    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}
