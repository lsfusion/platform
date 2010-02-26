package platform.server.data.query;

import platform.base.OrderedMap;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.Collection;
import java.util.Map;

public interface ParsedQuery<K,V> {

    CompiledQuery<K,V> compileSelect(SQLSyntax syntax, OrderedMap<V,Boolean> orders,int top);    
    <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps);
    Join<V> join(Map<K, ? extends Expr> joinImplement);
    Join<V> join(Map<K, ? extends Expr> joinImplement,Map<ValueExpr,ValueExpr> joinValues); // последний параметр = какой есть\какой нужно, joinImplement не translate'ся

}
