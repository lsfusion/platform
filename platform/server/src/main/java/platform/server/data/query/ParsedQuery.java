package platform.server.data.query;

import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ParsedQuery<K,V> {

    CompiledQuery<K,V> compileSelect(SQLSyntax syntax, OrderedMap<V, Boolean> orders, int top, String prefix);    
    <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps);

    Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues); // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся

    QuickSet<Value> getValues();

    public Query<K,V> pullValues(Map<K, Expr> pullKeys, Map<V, Expr> pullProps);
}
