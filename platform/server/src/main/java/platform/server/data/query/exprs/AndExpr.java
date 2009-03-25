package platform.server.data.query.exprs;

import platform.server.data.query.JoinData;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.wheres.MapWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import net.jcip.annotations.Immutable;


public abstract class AndExpr extends SourceExpr {

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public abstract DataWhereSet getFollows();

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }


    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public SourceExpr followFalse(Where where) {
        if(getWhere().means(where))
            return getType().getExpr(null);
        else
            return this;
    }
}
