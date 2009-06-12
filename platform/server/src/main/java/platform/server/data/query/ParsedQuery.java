package platform.server.data.query;

import platform.server.data.classes.where.ClassWhere;
import platform.server.data.classes.BaseClass;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.SQLSession;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.SQLException;

public interface ParsedQuery<K,V> extends Join<V> {

    CompiledQuery<K,V> compileSelect(SQLSyntax syntax, LinkedHashMap<V,Boolean> orders,int top);    
    <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps);
    Join<V> join(Map<K, ? extends SourceExpr> joinImplement);
    Join<V> join(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> joinValues);

    Collection<ValueExpr> getValues();

    <GK extends V, GV extends V> ParsedQuery<GK,GV> groupBy(Collection<GK> keys,Collection<GV> max, Collection<GV> sum);

    Map<K,KeyExpr> getMapKeys();
}
