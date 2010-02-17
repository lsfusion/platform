package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.server.data.Table;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.query.GroupJoin;

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
            return BaseUtils.hashEquals(who,what) || what.isIn(who.getJoinFollows());
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

        public int hashContext(HashContext hashContext) {
            int hash = 0;
            for(InnerJoin where : wheres)
                hash += where.hashContext(hashContext);
            return hash;
        }

        public JoinSet translateDirect(KeyTranslator translator) {
            InnerJoin[] transJoins = new InnerJoin[wheres.length];
            for(int i=0;i<wheres.length;i++)
                transJoins[i] = wheres[i].translateDirect(translator);
            return new JoinSet(transJoins);
        }
    }

    public final JoinSet joins;
    final Map<KeyExpr, BaseExpr> keyValues;

    public InnerWhere() {
        joins = new JoinSet();
        keyValues = new HashMap<KeyExpr, BaseExpr>();
    }

    public InnerWhere(InnerJoin where) {
        joins = new JoinSet(where);
        keyValues = new HashMap<KeyExpr, BaseExpr>();
    }

    public InnerWhere(KeyExpr key, BaseExpr value) {
        joins = new JoinSet();
        assert value.isValue();
        keyValues = Collections.singletonMap(key, value);
    }

    boolean means(InnerWhere where) {
        return equals(and(where));
    }

    boolean means(InnerJoin inner) {
        return means(new InnerWhere(inner));
    }

    public InnerWhere(JoinSet joins, Map<KeyExpr, BaseExpr> keyValues) {
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
        Map<KeyExpr, BaseExpr> andValues = BaseUtils.mergeEqual(keyValues,where.keyValues);
        if(andValues==null) return null;
        return new InnerWhere(joins.and(where.joins),andValues);
    }

    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyValues.entrySet())
            hash += keyValue.getKey().hashContext(hashContext) ^ keyValue.getValue().hashContext(hashContext);
        return joins.hashContext(hashContext) * 31 + hash;
    }

    public InnerWhere translateDirect(KeyTranslator translator) {
        Map<KeyExpr,BaseExpr> transValues = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyValues.entrySet())
            transValues.put(keyValue.getKey().translateDirect(translator),keyValue.getValue().translateDirect(translator));
        return new InnerWhere(joins.translateDirect(translator),transValues);
    }
}
