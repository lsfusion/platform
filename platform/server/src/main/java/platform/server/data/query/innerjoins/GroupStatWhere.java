package platform.server.data.query.innerjoins;

import platform.server.caches.ManualLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;

import java.util.Set;

public class GroupStatWhere<J extends StatInterface<J>> {

    public final KeyEqual keyEqual;
    public final J joins;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его

    public GroupStatWhere(KeyEqual keyEqual, J joins, Where where) {
        this.keyEqual = keyEqual;
        this.joins = joins;
        this.where = where;

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }

    private Where fullWhere;
    @ManualLazy
    public Where getFullWhere() {
        if(fullWhere==null)
            fullWhere = where.and(keyEqual.getWhere());
        return fullWhere;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupStatWhere that = (GroupStatWhere) o;

        if (fullWhere != null ? !fullWhere.equals(that.fullWhere) : that.fullWhere != null) return false;
        if (joins != null ? !joins.equals(that.joins) : that.joins != null) return false;
        if (keyEqual != null ? !keyEqual.equals(that.keyEqual) : that.keyEqual != null) return false;
        if (where != null ? !where.equals(that.where) : that.where != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = keyEqual != null ? keyEqual.hashCode() : 0;
        result = 31 * result + (joins != null ? joins.hashCode() : 0);
        result = 31 * result + (where != null ? where.hashCode() : 0);
        result = 31 * result + (fullWhere != null ? fullWhere.hashCode() : 0);
        return result;
    }
}
