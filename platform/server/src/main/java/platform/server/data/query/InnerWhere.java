package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.data.where.DNFWheres;
import platform.server.caches.HashContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InnerWhere implements DNFWheres.Interface<InnerWhere> {

    public final JoinSet joins;
    final Map<KeyExpr, BaseExpr> keyExprs;

    public InnerWhere() {
        joins = new JoinSet();
        keyExprs = new HashMap<KeyExpr, BaseExpr>();
    }

    public InnerWhere(InnerJoin where) {
        joins = new JoinSet(where);
        keyExprs = new HashMap<KeyExpr, BaseExpr>();
    }

    public InnerWhere(KeyExpr key, BaseExpr expr) {
        joins = new JoinSet();
        assert !expr.hasKey(key);
        keyExprs = Collections.singletonMap(key, expr);
    }

    public boolean means(InnerWhere where) {
        return BaseUtils.isSubMap(where.keyExprs,keyExprs) && joins.means(where.joins);
    }

    public InnerWhere(JoinSet joins, Map<KeyExpr, BaseExpr> keyExprs) {
        this.joins = joins;
        this.keyExprs = keyExprs;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof InnerWhere && joins.equals(((InnerWhere) o).joins) && keyExprs.equals(((InnerWhere) o).keyExprs);
    }

    @Override
    public int hashCode() {
        return 31 * joins.hashCode() + keyExprs.hashCode();
    }

    public InnerWhere and(InnerWhere where) {
        return new InnerWhere(joins.and(where.joins), BaseUtils.merge(keyExprs,where.keyExprs)); // даже если совпадают ничего страшного, все равно зафиксировано в InnerJoins - Where
    }

    public boolean isFalse() {
        return false;
    }

    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyExprs.entrySet())
            hash += keyValue.getKey().hashContext(hashContext) ^ keyValue.getValue().hashContext(hashContext);
        return joins.hashContext(hashContext) * 31 + hash;
    }

    public InnerWhere translateDirect(KeyTranslator translator) {
        Map<KeyExpr,BaseExpr> transValues = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyExprs.entrySet())
            transValues.put(keyValue.getKey().translateDirect(translator),keyValue.getValue().translateDirect(translator));
        return new InnerWhere(joins.translateDirect(translator),transValues);
    }
}
