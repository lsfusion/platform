package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

import java.util.*;

class CompiledJoinQuery<K,V> {

    Map<V,SourceExpr> properties;
    Where where;

    Map<K, KeyExpr> keys;

    List<CompiledJoin> joins;
    Map<ValueExpr, ValueExpr> values;

    private CompiledJoinQuery(Map<K, KeyExpr> iKeys) {
        keys = iKeys;
    }

    CompiledJoinQuery(JoinQuery<K,V> query) {
        this(query.mapKeys);

        ExprTranslator translated = new ExprTranslator();

        joins = new ArrayList<CompiledJoin>();
        for(Join join : query.getJoins())
            join.source.compileJoin(join, translated, joins);

        // Where
        where = query.where.translate(translated);

        // свойства
        properties = new HashMap<V, SourceExpr>();
        for(Map.Entry<V, SourceExpr> mapProperty : query.properties.entrySet())
            properties.put(mapProperty.getKey(),mapProperty.getValue().translate(translated)); //.followFalse(where.not())
        // Values
        values = query.getValues();

        compiler = new Compiler();
    }

    Map<V, SourceExpr> andProperties = null;
    Map<V, SourceExpr> getAndProperties() {
        if(andProperties==null) andProperties = compiler.getAndProperties();
        return andProperties;
    }

    Map<V, SourceExpr> packedProperties = null;
    Map<V, SourceExpr> getPackedProperties() {
        if(packedProperties==null) packedProperties = compiler.getPackedProperties();
        return packedProperties;
    }

    void removeProperty(V property) {
        properties.remove(property);
        if(andProperties!=null)
            andProperties.remove(property);
        if(packedProperties!=null)
            packedProperties.remove(property);
    }

    Compiler compiler;
    CompiledQuery<K,V> compile = null;
    LinkedHashMap<V,Boolean> compileOrders;
    int compileTop = 0;
    CompiledQuery<K,V> compileSelect(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int top) {
        synchronized(this) { // тут он уже в кэше может быть
            if(compile==null || !(compileOrders.equals(orders) && compileTop==top)) {
                compile = compiler.compile(syntax, orders, top);
                compileOrders = orders;
                compileTop = top;
            }
            return compile;
        }
    }
    CompiledQuery<K,V> compileSelect(SQLSyntax syntax) {
        return compileSelect(syntax,new LinkedHashMap<V, Boolean>(),0);
    }

    boolean isEmpty() {
        return where.isFalse();
    }

    void compileJoin(Join<K, V> join, ExprTranslator translated, Collection<CompiledJoin> translatedJoins) {

        ExprTranslator joinTranslated = new ExprTranslator();
        // закинем перекодирование ключей
        for(Map.Entry<K, KeyExpr> mapKey : keys.entrySet())
            joinTranslated.put(mapKey.getValue(), join.joins.get(mapKey.getKey()).translate(translated));

        joinTranslated.putAll(BaseUtils.reverse(values));

        // рекурсивно погнали остальные JoinQuery, здесь уже DataSource'ы причем без CaseExpr'ов
        for(CompiledJoin compileJoin : joins) // здесь по сути надо перетранслировать ValueExpr'ы а также в GroupQuery перебить JoinQuery на новые Values
            compileJoin.translate(joinTranslated, translatedJoins, compileJoin.getDataSource().translateValues(values));

        // включать direct если нету case'ов, но почему-то не сильно помогает (процентов на 20)
        joinTranslated.direct = !joinTranslated.hasCases();

        translated.put(join.inJoin, where.translate(joinTranslated));
        for(Map.Entry<V, SourceExpr> mapProperty : getAndProperties().entrySet())
            translated.put(join.exprs.get(mapProperty.getKey()), mapProperty.getValue().translate(joinTranslated));
    }

    private class Compiler {
        CompiledQuery<K,V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(CompiledJoinQuery.this, syntax, orders, top);
        }

        Map<V, SourceExpr> getAndProperties() {
            Map<V, SourceExpr> result = new HashMap<V, SourceExpr>();
            for(Map.Entry<V, SourceExpr> mapProperty : properties.entrySet())
                result.put(mapProperty.getKey(),mapProperty.getValue().and(where));
            return result;
        }

        Map<V, SourceExpr> getPackedProperties() {
            Map<V, SourceExpr> result = new HashMap<V, SourceExpr>();
            for(Map.Entry<V, SourceExpr> mapProperty : properties.entrySet())
                result.put(mapProperty.getKey(),mapProperty.getValue().followFalse(where.not()));
            return result;
        }
    }

    private class MapCompiler<MK,MV> extends Compiler {
        CompiledJoinQuery<MK,MV> mapQuery;
        Map<K,MK> mapKeys;
        Map<V,MV> mapProps;
        Map<ValueExpr, ValueExpr> mapValues;

        private MapCompiler(CompiledJoinQuery<MK, MV> iMapQuery, Map<K, MK> iMapKeys, Map<V, MV> iMapProps, Map<ValueExpr, ValueExpr> iMapValues) {
            mapQuery = iMapQuery;
            mapKeys = iMapKeys;
            mapProps = iMapProps;
            mapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(mapQuery.compileSelect(syntax,BaseUtils.linkedJoin(orders,mapProps),top), mapKeys, mapProps, mapValues);
        }

        Map<V, SourceExpr> getAndProperties() {
            return BaseUtils.join(mapProps, mapQuery.getAndProperties());
        }

        Map<V, SourceExpr> getPackedProperties() {
            return BaseUtils.join(mapProps, mapQuery.getPackedProperties());
        }
    }
    <MK,MV> CompiledJoinQuery(CompiledJoinQuery<MK,MV> query, Map<K, MK> mapKeys, Map<V, MV> mapProps, Map<ValueExpr, ValueExpr> mapValues) {
        this(BaseUtils.join(mapKeys, query.keys));
        properties = BaseUtils.join(mapProps, query.properties);
        where = query.where;
        joins = query.joins;
        values = BaseUtils.join(mapValues, query.values);

        compiler = new MapCompiler<MK,MV>(query, mapKeys, mapProps, mapValues);
    }

    private class ValueCompiler extends Compiler {
        CompiledJoinQuery<K,V> mapQuery;
        Map<ValueExpr, ValueExpr> mapValues;

        private ValueCompiler(CompiledJoinQuery<K, V> iMapQuery, Map<ValueExpr, ValueExpr> iMapValues) {
            mapQuery = iMapQuery;
            mapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(mapQuery.compileSelect(syntax,orders,top), mapValues);
        }

        Map<V, SourceExpr> getAndProperties() {
            return mapQuery.getAndProperties();
        }

        Map<V, SourceExpr> getPackedProperties() {
            return mapQuery.getPackedProperties();
        }
    }
    CompiledJoinQuery(CompiledJoinQuery<K,V> query, Map<ValueExpr, ValueExpr> mapValues) {
        this(query.keys);
        properties = query.properties;
        where = query.where;
        joins = query.joins;
        values = BaseUtils.join(mapValues,query.values);

        compiler = new ValueCompiler(query,mapValues);
    }

}
