package platform.server.data.query.innerjoins;

import platform.server.caches.ManualLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.where.Where;

import java.util.*;

public class GroupJoinsWhere {

    public final KeyEqual keyEqual;
    public final WhereJoins joins;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его

    public final Map<WhereJoin, Where> upWheres;

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Map<WhereJoin, Where> upWheres, Where where) {
        this.keyEqual = keyEqual;
        this.joins = joins;
        this.where = where;
        this.upWheres = upWheres;
    }

    public GroupJoinsWhere pack() { // upWheres особого смысла паковать нет, все равно
        return new GroupJoinsWhere(keyEqual, joins, upWheres, where.pack());
    }

    public static Collection<GroupJoinsWhere> pack(Collection<GroupJoinsWhere> whereJoins) {
        if(whereJoins.size()==1) // нет смысла упаковывать если один whereJoins
            return whereJoins;
        else {
            Collection<GroupJoinsWhere> result = new ArrayList<GroupJoinsWhere>();
            for(GroupJoinsWhere innerJoin : whereJoins)
                result.add(innerJoin.pack());
            return result;
        }
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups) {
        return joins.getStatKeys(groups, where, keyEqual);
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
        return this == o || o instanceof GroupJoinsWhere && joins.equals(((GroupJoinsWhere) o).joins) && keyEqual.equals(((GroupJoinsWhere) o).keyEqual) && upWheres.equals(((GroupJoinsWhere) o).upWheres) && where.equals(((GroupJoinsWhere) o).where);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * keyEqual.hashCode() + joins.hashCode()) + where.hashCode()) + upWheres.hashCode();
    }
}
