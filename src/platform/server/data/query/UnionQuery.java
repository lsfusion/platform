package platform.server.data.query;

import java.util.Collection;
import java.util.Map;

import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.exprs.LinearExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.Source;
import platform.server.where.Where;

public abstract class UnionQuery<K,V> extends JoinQuery<K,V> {

    protected UnionQuery(Collection<? extends K> iKeys) {
        super(iKeys);
        where = Where.FALSE;
    }

    abstract SourceExpr getUnionExpr(SourceExpr prevExpr, SourceExpr expr, JoinWhere inJoin);

    // добавляем на OR запрос
    public void add(Source<? extends K,V> source,Integer coeff) {

        Join<K,V> join = new Join<K,V>((Source<K,V>) source,this);
        for(Map.Entry<V, JoinExpr<K,V>> mapExpr : join.exprs.entrySet()) {
            SourceExpr unionExpr = new LinearExpr(mapExpr.getValue(),coeff);
            SourceExpr prevExpr = properties.get(mapExpr.getKey());
            if(prevExpr!=null)
                unionExpr = getUnionExpr(prevExpr,unionExpr,join.inJoin);
            properties.put(mapExpr.getKey(), unionExpr);
        }
        where = where.or(join.inJoin);
    }

    public void add(Source<? extends K,V> source) {
        add(source,1);
    }
}
