package platform.server.data.query.innerjoins;

import platform.server.data.query.JoinSet;
import platform.server.data.where.Where;

public class InnerSelectJoin {
    public final KeyEqual keyEqual;
    public final JoinSet joins;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его
    public final Where fullWhere; // включает keyEqual

    public InnerSelectJoin(KeyEqual keyEqual, JoinSet joins, Where where) {
        this.keyEqual = keyEqual;
        this.joins = joins;
        this.where = where;

        this.fullWhere = where.and(keyEqual.getWhere());

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }
}
