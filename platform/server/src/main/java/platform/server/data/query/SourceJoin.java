package platform.server.data.query;

import platform.server.data.query.wheres.MapWhere;
import platform.server.where.Where;

public interface SourceJoin {

    String getSource(CompileSource compile);

    void fillContext(Context context);
//    void fillJoins(List<? extends JoinSelect> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}
