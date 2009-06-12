package platform.server.data.query;

import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.translators.Translator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

import java.util.Map;
import java.util.Set;

public interface SourceJoin<T extends SourceJoin> {

    String getSource(Map<QueryData, String> queryData, SQLSyntax syntax);

    int fillContext(Context context, boolean compile);
//    void fillJoins(List<? extends Join> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    // !!!! нельзя проверять на обычный equals
    abstract boolean equals(T sourceJoin, MapContext mapContext);

    public abstract T translate(Translator translator);    
}
