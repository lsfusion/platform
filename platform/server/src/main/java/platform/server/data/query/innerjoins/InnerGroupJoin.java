package platform.server.data.query.innerjoins;

import platform.server.data.query.JoinSet;
import platform.server.data.where.Where;

public class InnerGroupJoin<J extends GroupJoinSet> {

    public final KeyEqual keyEqual;
    public final J joins;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его
    public final Where fullWhere; // включает keyEqual

    public InnerGroupJoin(KeyEqual keyEqual, J joins, Where where) {
        this.keyEqual = keyEqual;
        this.joins = joins;
        this.where = where;

        this.fullWhere = where.and(keyEqual.getWhere());

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }
}
