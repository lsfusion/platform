package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.server.data.Table;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InnerWhere {

    static class JoinSet extends AddSet<InnerJoin, JoinSet> {

        JoinSet() {
        }

        JoinSet(InnerJoin[] iWheres) {
            super(iWheres);
        }

        JoinSet(InnerJoin where) {
            super(where);
        }

        protected JoinSet createThis(InnerJoin[] iWheres) {
            return new JoinSet(iWheres);
        }

        protected InnerJoin[] newArray(int size) {
            return new InnerJoin[size];
        }

        protected boolean containsAll(InnerJoin who, InnerJoin what) {
            return who.equals(what) || who.getJoinFollows().contains(what);
        }

        protected JoinSet and(JoinSet set) {
            return add(set);
        }

        void fillJoins(Collection<Table.Join> tables, Collection<GroupJoin> groups) {
            for(InnerJoin where : wheres)
                if (where instanceof Table.Join)
                    tables.add((Table.Join) where);
                else
                    groups.add((GroupJoin) where);
        }
    }

    public final JoinSet joins;
    final Map<KeyExpr,ValueExpr> keyValues;

    public InnerWhere() {
        joins = new JoinSet();
        keyValues = new HashMap<KeyExpr, ValueExpr>();
    }

    public InnerWhere(InnerJoin where) {
        joins = new JoinSet(where);
        keyValues = new HashMap<KeyExpr, ValueExpr>();
    }

    public InnerWhere(KeyExpr key, ValueExpr value) {
        joins = new JoinSet();
        keyValues = Collections.singletonMap(key, value);
    }

    boolean means(InnerWhere where) {
        return equals(and(where));
    }

    boolean means(InnerJoin inner) {
        return means(new InnerWhere(inner));
    }

    public InnerWhere(JoinSet joins, Map<KeyExpr, ValueExpr> keyValues) {
        this.joins = joins;
        this.keyValues = keyValues;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof InnerWhere && joins.equals(((InnerWhere) o).joins) && keyValues.equals(((InnerWhere) o).keyValues);
    }

    @Override
    public int hashCode() {
        return 31 * joins.hashCode() + keyValues.hashCode();
    }

    InnerWhere and(InnerWhere where) {
        Map<KeyExpr,ValueExpr> andValues = BaseUtils.mergeEqual(keyValues,where.keyValues);
        if(andValues==null) return null;
        return new InnerWhere(joins.and(where.joins),andValues);
    }
}
