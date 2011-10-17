package platform.server.data.query.innerjoins;

import platform.server.data.expr.Expr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;

// сделаем generics для обратной совместимости, хотя в общем то он не нужен
public class GroupStatWhere<K extends Expr> {

    public final KeyEqual keyEqual;
    public final StatKeys<K> stats;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его

    public GroupStatWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where) {
        this.keyEqual = keyEqual;
        this.stats = stats;
        this.where = where;

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GroupStatWhere && keyEqual.equals(((GroupStatWhere) o).keyEqual) && stats.equals(((GroupStatWhere) o).stats) && where.equals(((GroupStatWhere) o).where);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * keyEqual.hashCode() + stats.hashCode()) + where.hashCode();
    }
}
