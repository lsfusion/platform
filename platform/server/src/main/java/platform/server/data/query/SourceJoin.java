package platform.server.data.query;

import platform.server.data.expr.where.MapWhere;
import platform.server.data.where.Where;

public interface SourceJoin {

    String getSource(CompileSource compile);

    void fillContext(Context context);
//    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}
