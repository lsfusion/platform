package platform.server.data.query;

import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface ParsedQuery<K,V> {

    CompiledQuery<K,V> compileSelect(SQLSyntax syntax, LinkedHashMap<V,Boolean> orders,int top);    
    <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps);
    Join<V> join(Map<K, ? extends SourceExpr> joinImplement);
    Join<V> join(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> joinValues);

}
