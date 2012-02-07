package platform.server.data.query;

import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.InnerContext;
import platform.server.caches.PackInterface;
import platform.server.caches.TranslateValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.where.classes.ClassWhere;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class IQuery<K,V> extends AbstractInnerContext<IQuery<K, V>> implements MapKeysInterface<K> {

    @Override
    public IQuery<K, V> translateRemoveValues(MapValuesTranslate translate) {
        return translateQuery(translate.mapKeys());
    }

    @Override
    public IQuery<K,V> translateValues(MapValuesTranslate translate) { // оптимизация
        return translateMap(translate);
    }

    protected IQuery<K, V> translate(MapTranslate translator) {
        if (translator.identityKeys(getInnerKeys()))
            return translateValues(translator.mapValues());
        else
            return translateQuery(translator);
    }

    public abstract MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate);
    public abstract IQuery<K, V> translateQuery(MapTranslate translate);

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return compile(syntax, new OrderedMap<V, Boolean>(), 0, "");
    }

    public abstract CompiledQuery<K,V> compile(SQLSyntax syntax, OrderedMap<V, Boolean> orders, Integer top, String prefix);

    public abstract <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps);

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return join(joinImplement, MapValuesTranslator.noTranslate);
    }
    public abstract Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues); // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся
    public abstract Join<V> joinExprs(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues);


    public abstract Set<V> getProperties();
    
    public abstract Expr getExpr(V property);

    public abstract Query<K, V> getQuery(); // по сути protectedQ  GH  N
    public abstract <RMK, RMV> IQuery<RMK,RMV> map(Map<RMK, K> remapKeys, Map<RMV, V> remapProps, MapValuesTranslate translate);

    public abstract IQuery<K,V> pullValues(Map<K, Expr> pullKeys, Map<V, Expr> pullProps) throws SQLException;
}
