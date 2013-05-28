package platform.server.data.query;

import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

public interface SourceJoin<T extends SourceJoin<T>> extends OuterContext<T>, AndContext<T> {

    String getSource(CompileSource compile);

    T translateQuery(QueryTranslator translator);

    //    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere);

    boolean needMaterialize();

}
