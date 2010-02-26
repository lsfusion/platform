package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.SQLSession;
import platform.server.data.expr.*;
import platform.server.data.expr.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.NullReader;
import platform.server.data.type.Reader;
import platform.server.data.type.TypeObject;
import platform.server.data.where.Where;
import platform.server.caches.TranslateContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

// нужен для Map'а ключей / значений
// Immutable/Thread Safe
public class CompiledQuery<K,V> {

    final public String from;
    final public Map<K,String> keySelect;
    final public Map<V,String> propertySelect;
    final public Collection<String> whereSelect;

    final public String select;
    final public List<K> keyOrder;
    final public List<V> propertyOrder;
    final public Map<K,String> keyNames;
    final public Map<K,Reader> keyReaders;
    final public Map<V,String> propertyNames;
    final private Map<V,Reader> propertyReaders;

    final Map<ValueExpr,String> params;

    private boolean checkQuery() {
        assert !params.containsValue(null);

        return true;
    }

    // перемаппит другой CompiledQuery
    <MK,MV> CompiledQuery(CompiledQuery<MK,MV> compile,Map<K,MK> mapKeys,Map<V,MV> mapProperties,Map<ValueExpr, ValueExpr> mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = BaseUtils.join(mapKeys,compile.keySelect);
        propertySelect = BaseUtils.join(mapProperties,compile.propertySelect);
        keyOrder = new ArrayList<K>();
        propertyOrder = new ArrayList<V>();

        select = compile.select;
        for(MK key : compile.keyOrder)
            for(Map.Entry<K,MK> mapKey : mapKeys.entrySet())
                if(mapKey.getValue()==key) {
                    keyOrder.add(mapKey.getKey()); break;}
        for(MV property : compile.propertyOrder)
            for(Map.Entry<V,MV> mapProperty : mapProperties.entrySet())
                if(mapProperty.getValue()==property) {
                    propertyOrder.add(mapProperty.getKey()); break;}
        keyNames = BaseUtils.join(mapKeys,compile.keyNames);
        keyReaders = BaseUtils.join(mapKeys,compile.keyReaders);
        propertyNames = BaseUtils.join(mapProperties,compile.propertyNames);
        propertyReaders = BaseUtils.join(mapProperties,compile.propertyReaders);

        params = BaseUtils.crossJoin(mapValues,compile.params); // так как map'ся весь joinQuery а при parse -> compile могут уйти

        assert checkQuery();
    }

    static class FullSelect extends CompileSource {

        FullSelect(Map<ValueExpr, String> params, SQLSyntax syntax) {
            super(params, syntax);
        }

        public final Map<JoinData,String> joinData = new HashMap<JoinData, String>();

        public String getSource(Table.Join.Expr expr) {
            return joinData.get(expr);
        }

        public String getSource(Table.Join.IsIn where) {
            return joinData.get(where);
        }

        public String getSource(GroupExpr groupExpr) {
            return joinData.get(groupExpr);
        }

        public String getSource(OrderExpr orderExpr) {
            return joinData.get(orderExpr);
        }
    }

    CompiledQuery(ParsedJoinQuery<K,V> query, SQLSyntax syntax, OrderedMap<V,Boolean> orders, int top) {

        keySelect = new HashMap<K, String>();
        propertySelect = new HashMap<V, String>();
        whereSelect = new ArrayList<String>();
        keyOrder = new ArrayList<K>();
        propertyOrder = new ArrayList<V>();

        keyNames = new HashMap<K,String>();

        int keyCount = 0;
        for(K Key : query.mapKeys.keySet())
            keyNames.put(Key,"jkey"+(keyCount++));

        int propertyCount = 0;
        propertyNames = new HashMap<V, String>();
        for(Map.Entry<V, Expr> property : query.properties.entrySet())
            propertyNames.put(property.getKey(),"jprop"+(propertyCount++));

        int paramCount = 0;
        params = new HashMap<ValueExpr, String>();
        for(ValueExpr mapValue : query.values)
            params.put(mapValue, "qwer" + (paramCount++) + "ffd");

        // разделяем на JoinWhere
        Collection<InnerJoins.Entry> queryJoins = query.where.getInnerJoins().compileMeans();

        keyReaders = new HashMap<K, Reader>();
        propertyReaders = new HashMap<V, Reader>();
        if(queryJoins.size()==0) {
            for(K key : query.mapKeys.keySet()) {
                keySelect.put(key, SQLSyntax.NULL);
                keyReaders.put(key, NullReader.instance);
            }
            for(V property : query.properties.keySet()) {
                propertySelect.put(property, SQLSyntax.NULL);
                propertyReaders.put(property, NullReader.instance);
            }
            from = "empty";
        } else {
            for(Map.Entry<K,KeyExpr> key : query.mapKeys.entrySet())
                keyReaders.put(key.getKey(),key.getValue().getType(query.where));
            for(Map.Entry<V, Expr> property : query.properties.entrySet())
                propertyReaders.put(property.getKey(),property.getValue().getReader(query.where));

            if(queryJoins.size()==1) { // "простой" запрос
                InnerJoins.Entry innerJoin = queryJoins.iterator().next();
                from = fillInnerSelect(query.mapKeys, innerJoin.mean, innerJoin.where, query.properties, keySelect, propertySelect, whereSelect, params, syntax);
            } else {
                // создаем And подзапросыs
                Collection<AndJoinQuery> andProps = new ArrayList<AndJoinQuery>();
                for(InnerJoins.Entry andWhere : queryJoins)
                    andProps.add(new AndJoinQuery(andWhere.mean,andWhere.where,"f"+andProps.size()));

                FullSelect FJSelect = new FullSelect(params,syntax);

                MapWhere<JoinData> joinDataWheres = new MapWhere<JoinData>();
                for(Map.Entry<V, Expr> joinProp : query.properties.entrySet())
                    if(!orders.containsKey(joinProp.getKey()))
                        joinProp.getValue().fillJoinWheres(joinDataWheres, Where.TRUE);

                // для JoinSelect'ов узнаем при каких условиях они нужны
                MapWhere<Object> joinWheres = new MapWhere<Object>();
                for(int i=0;i<joinDataWheres.size;i++)
                    joinWheres.add(joinDataWheres.getKey(i).getFJGroup(),joinDataWheres.getValue(i));

                // сначала распихиваем JoinSelect по And'ам
                Map<Object,Collection<AndJoinQuery>> joinAnds = new HashMap<Object, Collection<AndJoinQuery>>();
                for(int i=0;i<joinWheres.size;i++)
                    joinAnds.put(joinWheres.getKey(i),getWhereSubSet(andProps,joinWheres.getValue(i)));

                // затем все данные по JoinSelect'ам по вариантам
                int joinNum = 0;
                for(int i=0;i<joinDataWheres.size;i++) {
                    JoinData joinData = joinDataWheres.getKey(i);
                    String joinName = "join_" + (joinNum++);
                    Collection<AndJoinQuery> dataAnds = new ArrayList<AndJoinQuery>();
                    for(AndJoinQuery and : getWhereSubSet(joinAnds.get(joinData.getFJGroup()), joinDataWheres.getValue(i))) {
                        Expr joinExpr = joinData.getFJExpr();
                        if(!and.where.means(joinExpr.getWhere().not())) { // проверим что не всегда null
                            and.properties.put(joinName, joinExpr);
                            dataAnds.add(and);
                        }
                    }
                    String joinSource = ""; // заполняем Source
                    if(dataAnds.size()==0)
                        throw new RuntimeException("Не должно быть");
                    else
                    if(dataAnds.size()==1)
                        joinSource = dataAnds.iterator().next().alias +'.'+joinName;
                    else {
                        for(AndJoinQuery and : dataAnds)
                            joinSource = (joinSource.length()==0?"":joinSource+",") + and.alias + '.' + joinName;
                        joinSource = "COALESCE(" + joinSource + ")";
                    }
                    FJSelect.joinData.put(joinData,joinData.getFJString(joinSource));
                }

                // order'ы отдельно обрабатываем, они нужны в каждом запросе генерируем имена для Order'ов
                int io = 0;
                OrderedMap<String,Boolean> orderAnds = new OrderedMap<String, Boolean>();
                for(Map.Entry<V, Boolean> order : orders.entrySet()) {
                    String orderName = "order_" + (io++);
                    String orderFJ = "";
                    for(AndJoinQuery and : andProps) {
                        and.properties.put(orderName, query.properties.get(order.getKey()));
                        orderFJ = (orderFJ.length()==0?"":orderFJ+",") + and.alias + "." + orderName;
                    }
                    orderAnds.put(orderName,order.getValue());
                    propertySelect.put(order.getKey(),"COALESCE("+orderFJ+")");
                }

                // бежим по всем And'ам делаем JoinSelect запросы, потом объединяем их FULL'ами
                String compileFrom = "";
                boolean second = true; // для COALESCE'ов
                for(AndJoinQuery and : andProps) {
                    // закинем в And.Properties OrderBy
                    Map<K,String> andKeySelect = new HashMap<K, String>(); Collection<String> andWhereSelect = new ArrayList<String>();
                    Map<String,String> andPropertySelect = new HashMap<String, String>();
                    String andFrom = fillInnerSelect(query.mapKeys, and.inner, and.where, and.properties, andKeySelect, andPropertySelect, andWhereSelect, params, syntax);

                    List<String> propertyOrder = new ArrayList<String>();
                    String andSelect = "(" + syntax.getSelect(andFrom, SQLSession.stringExpr(SQLSession.mapNames(andKeySelect,keyNames,new ArrayList<K>()),
                            SQLSession.mapNames(andPropertySelect,BaseUtils.toMap(andPropertySelect.keySet()),propertyOrder)),
                            BaseUtils.toString(andWhereSelect, " AND "), platform.server.data.query.Query.stringOrder(propertyOrder,query.mapKeys.size(),orderAnds),"",top==0?"":String.valueOf(top)) + ") "+ and.alias;

                    if(compileFrom.length()==0) {
                        compileFrom = andSelect;
                        for(K Key : query.mapKeys.keySet())
                            keySelect.put(Key, and.alias +"."+ keyNames.get(Key));
                    } else {
                        String andJoin = "";
                        for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
                            String andKey = and.alias + "." + keyNames.get(mapKey.getKey());
                            andJoin = (andJoin.length()==0?"":andJoin + " AND ") + andKey + "=" + (second?mapKey.getValue():"COALESCE("+mapKey.getValue()+")");
                            mapKey.setValue(mapKey.getValue()+","+andKey);
                        }
                        compileFrom = compileFrom + " FULL JOIN " + andSelect + " ON " + (andJoin.length()==0? Where.TRUE_STRING :andJoin);
                        second = false;
                    }
                }
                from = compileFrom;

                // полученные KeySelect'ы в Data
                for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
                    mapKey.setValue("COALESCE("+mapKey.getValue()+")"); // обернем в COALESCE
                    FJSelect.keySelect.put(query.mapKeys.get(mapKey.getKey()),mapKey.getValue());
                }

                // закидываем PropertySelect'ы
                for(Map.Entry<V, Expr> mapProp : query.properties.entrySet())
                    if(!orders.containsKey(mapProp.getKey())) // orders'ы уже обработаны
                        propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(FJSelect));
            }
        }

        select = syntax.getSelect(from, SQLSession.stringExpr(
                SQLSession.mapNames(keySelect, keyNames, keyOrder),
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)),
                BaseUtils.toString(whereSelect, " AND "), platform.server.data.query.Query.stringOrder(propertyOrder,query.mapKeys.size(),orders),"",top==0?"":String.valueOf(top));

        assert checkQuery();
    }

    // в общем случае получить AndJoinQuery под которые подходит Where
    static Collection<AndJoinQuery> getWhereSubSet(Collection<AndJoinQuery> andWheres, Where where) {

        Collection<AndJoinQuery> result = new ArrayList<AndJoinQuery>();
        Where resultWhere = Where.FALSE;
        while(result.size()< andWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery lastQuery = null;
            Where lastWhere = null;
            for(AndJoinQuery and : andWheres)
                if(!result.contains(and)) {
                    lastQuery = and;
                    lastWhere = resultWhere.orMeans(lastQuery.where);
                    if(where.means(lastWhere)) {
                        result.add(lastQuery);

                        return result;
                    }
                }
            resultWhere = lastWhere;
            result.add(lastQuery);
        }
        return result;
    }

    static class InnerSelect extends CompileSource {
        
        final InnerWhere innerWhere;

        public InnerSelect(InnerWhere innerWhere, SQLSyntax syntax, Map<ValueExpr, String> params) {
            super(params, syntax);

            this.innerWhere = innerWhere;
            for(Map.Entry<KeyExpr, BaseExpr> exprValue : innerWhere.keyValues.entrySet())
                keySelect.put(exprValue.getKey(),exprValue.getValue().getSource(this));

            // обработаем inner'ы
            Collection<Table.Join> innerTables = new ArrayList<Table.Join>(); Collection<GroupJoin> innerGroups = new ArrayList<GroupJoin>();
            innerWhere.joins.fillJoins(innerTables, innerGroups);
            for(Table.Join table : innerTables) {
                assert !tables.containsKey(table); // assert'ы так как innerWhere должен быть взаимоисключающий
                tables.put(table,new TableSelect(table,syntax));
            }
            for(GroupJoin group : innerGroups) {
                assert !groups.containsKey(group);
                groups.put(group,new GroupSelect(group));
            }
        }

        int aliasNum=0;
        final List<JoinSelect> joins = new ArrayList<JoinSelect>();

        private abstract class JoinSelect<I> {

            final String alias; // final
            final String join; // final
            final boolean inner; // final

            protected abstract Map<String, BaseExpr> initJoins(I innerJoin);

            protected JoinSelect(I innerJoin) {
                alias = "t" + (aliasNum++);

                inner = innerJoin instanceof InnerJoin && innerWhere.means((InnerJoin) innerJoin); // вообще множественным наследованием надо было бы делать

                // здесь проблема что keySelect может рекурсивно использоваться 2 раза, поэтому сначала пробежим не по ключам
                String joinString = "";
                Map<String,KeyExpr> joinKeys = new HashMap<String, KeyExpr>();
                for(Map.Entry<String, BaseExpr> keyJoin : initJoins(innerJoin).entrySet()) {
                    String keySource = alias + "." + keyJoin.getKey();
                    if(keyJoin.getValue() instanceof KeyExpr)
                        joinKeys.put(keySource,(KeyExpr)keyJoin.getValue());
                    else
                        joinString = (joinString.length()==0?"":joinString+" AND ") + keySource + "=" + keyJoin.getValue().getSource(InnerSelect.this);
                }
                for(Map.Entry<String,KeyExpr> keyJoin : joinKeys.entrySet()) { // дозаполним ключи
                    String keySource = keySelect.get(keyJoin.getValue());
                    if(keySource==null) {
                        assert inner;
                        keySelect.put(keyJoin.getValue(),keyJoin.getKey());
                    } else
                        joinString = (joinString.length()==0?"":joinString+" AND ") + keyJoin.getKey() + "=" + keySource;
                }
                join = joinString;

                InnerSelect.this.joins.add(this);
            }

            public abstract String getSource();
        }

        // получает условия следующие из логики inner join'ов SQL
        private Where getInnerWhere() {
            Where trueWhere = Where.TRUE;
            Collection<Table.Join> innerTables = new ArrayList<Table.Join>(); Collection<GroupJoin> innerGroups = new ArrayList<GroupJoin>();
            innerWhere.joins.fillJoins(innerTables, innerGroups);
            for(Table.Join table : innerTables) // все inner'ы на таблицы заведомо true
                trueWhere = trueWhere.and(table.getWhere());
            for(GroupJoin group : innerGroups) { // для "верхних" group or всех выражений заведомо true
                Where groupWhere = Where.FALSE;
                for(GroupExpr groupExpr : groups.get(group).exprs.values())
                    groupWhere = groupWhere.or(groupExpr.getWhere());
                assert !groupWhere.isFalse();
                trueWhere = trueWhere.and(groupWhere);
            }
            for(Map.Entry<KeyExpr,BaseExpr> keyValue : innerWhere.keyValues.entrySet())
                trueWhere = trueWhere.and(EqualsWhere.create(keyValue.getKey(),keyValue.getValue()));

            return trueWhere;
        }

        public String getFrom(Where where,Collection<String> whereSelect) {
            // соответственно followFalse'им этими условиями
            where.getSource(this);// сначала надо узнать общий source чтобы заполнились groupExpr'ы в соответствующие exprs чтобы в getInnerWhere можно было бы построить общее условие 
            whereSelect.add(where.followFalse(getInnerWhere().not()).getSource(this));

            if(joins.isEmpty()) return "dumb";

            String from;
            Iterator<JoinSelect> ij = joins.iterator();
            JoinSelect first = ij.next();
            if(first.inner) {
                from = first.getSource() + " " + first.alias;
                if(!(first.join.length()==0))
                    whereSelect.add(first.join);
            } else {
                from = "dumb";
                ij = joins.iterator();
            }

            while(ij.hasNext()) {
                JoinSelect join = ij.next();
                from = from + (join.inner ?"":" LEFT")+" JOIN " + join.getSource() + " " + join.alias  + " ON " + (join.join.length()==0?Where.TRUE_STRING:join.join);
            }

            return from;
        }

        private class TableSelect extends JoinSelect<Table.Join> {
            private String source;

            protected Map<String, BaseExpr> initJoins(Table.Join table) {
                Map<String, BaseExpr> result = new HashMap<String, BaseExpr>();
                for(Map.Entry<KeyField, BaseExpr> expr : table.joins.entrySet())
                    result.put(expr.getKey().toString(),expr.getValue());
                return result;
            }

            TableSelect(Table.Join join,SQLSyntax syntax) {
                super(join);
                this.source = join.getName(syntax);
            }

            public String getSource() {
                return source;
            }
        }

        final Map<Table.Join, TableSelect> tables = new HashMap<Table.Join, TableSelect>();
        private String getAlias(Table.Join table) {
            TableSelect join = tables.get(table);
            if(join==null) {
                join = new TableSelect(table,syntax);
                tables.put(table,join);
            }
            return join.alias;
        }

        public String getSource(Table.Join.Expr expr) {
            return getAlias(expr.getJoin())+"."+expr.property;
        }
        public String getSource(Table.Join.IsIn where) {
            return getAlias(where.getJoin()) + "." + where.getFirstKey() + " IS NOT NULL";
        }

        private abstract class QuerySelect<K extends BaseExpr,I extends TranslateContext<I>,J extends QueryJoin<K,?>,E extends QueryExpr<K,I,J>> extends JoinSelect<J> {
            Map<String, K> group;

            protected Map<String, BaseExpr> initJoins(J groupJoin) {
                int keysNum = 0;
                Map<String, BaseExpr> result = new HashMap<String, BaseExpr>();
                group = new HashMap<String, K>();
                for(Map.Entry<K, BaseExpr> join : groupJoin.group.entrySet()) {
                    String keyName = "k" + (keysNum++);
                    result.put(keyName,join.getValue());
                    group.put(keyName,join.getKey());
                }
                return result;
            }

            QuerySelect(J groupJoin) {
                super(groupJoin);
            }

            final Map<I,String> queries = new HashMap<I, String>();
            final Map<I,E> exprs = new HashMap<I, E>(); // нужен для innerWhere и классовой информации, query транслированный -> в общее выражение

            public String add(I query,E expr) {
                String name = queries.get(query);
                if(name==null) {
                    name = "e"+ queries.size();
                    queries.put(query,name);
                    exprs.put(query,expr);
                }
                return alias + "." + name;
            }
        }

        private class GroupSelect extends QuerySelect<BaseExpr,Expr,GroupJoin,GroupExpr> {

            final Set<KeyExpr> keys;

            GroupSelect(GroupJoin groupJoin) {
                super(groupJoin);
                keys = groupJoin.getKeys();
            }

            public String getSource() {

                Set<Expr> queryExprs = new HashSet<Expr>(group.values()); // так как может одновременно и SUM и MAX нужен
                Where exprWhere = Where.FALSE;
                for(Expr query : queries.keySet()) {
                    queryExprs.add(query);
                    exprWhere = exprWhere.or(query.getWhere());
                }
                Where fullWhere = exprWhere.and(platform.server.data.expr.Expr.getWhere(group));

                Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
                Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
                String fromSelect = new Query<KeyExpr,Expr>(BaseUtils.toMap(keys),BaseUtils.toMap(queryExprs), fullWhere)
                    .compile(syntax).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, params);

                Map<String, String> keySelect = BaseUtils.join(group,fromPropertySelect);
                Map<String,String> propertySelect = new HashMap<String, String>();
                for(Map.Entry<Expr,GroupExpr> expr : exprs.entrySet())
                    propertySelect.put(queries.get(expr.getKey()),(expr.getValue().isMax()?"MAX":"SUM") + "(" + fromPropertySelect.get(expr.getKey()) +")");
                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect,propertySelect),
                        BaseUtils.toString(whereSelect," AND "),"",BaseUtils.toString(keySelect.values(),","),"") + ")";
            }
        }

        private class OrderSelect extends QuerySelect<KeyExpr, OrderExpr.Query,OrderJoin,OrderExpr> {

            final Map<KeyExpr,BaseExpr> mapKeys;
            private OrderSelect(OrderJoin orderJoin) {
                super(orderJoin);
                mapKeys = orderJoin.group;
            }

            private Where getPartitionWhere(Set<Expr> partitions) {
                Map<Expr, Expr> partitionMap = BaseUtils.toMap(partitions);
                Query<KeyExpr,Expr> mapQuery = new Query<KeyExpr,Expr>(BaseUtils.toMap(mapKeys.keySet())); // для кэша через Query
                mapQuery.properties.putAll(partitionMap);
                Join<Expr> joinQuery = mapQuery.join(mapKeys);
                return GroupExpr.create(joinQuery.getExprs(),ValueExpr.TRUE,getInnerWhere(),true,partitionMap).getWhere();
            }

            public String getSource() {

                Set<Expr> queryExprs = new HashSet<Expr>();

                Map<Set<Expr>,Where> cachedPartitions = new HashMap<Set<Expr>,Where>();

                Where fullWhere = Where.FALSE;
                for(OrderExpr.Query query : queries.keySet()) {
                    queryExprs.add(query.expr);
                    queryExprs.addAll(query.orders);
                    queryExprs.addAll(query.partitions);

                    // кэшируем так как не самая быстрая операция
                    Where partitionWhere;
                    if(OrderExpr.pushWhere) {
                        partitionWhere = cachedPartitions.get(query.partitions);
                        if(partitionWhere==null) {
                            partitionWhere = getPartitionWhere(query.partitions);
                            cachedPartitions.put(query.partitions,partitionWhere);
                        }
                    } else
                        partitionWhere = Where.TRUE;

                    fullWhere = fullWhere.or(query.getWhere().and(partitionWhere));
                }

                Map<String,String> keySelect = new HashMap<String,String>();
                Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
                Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
                String fromSelect = new Query<String,Expr>(group,BaseUtils.toMap(queryExprs),fullWhere)
                    .compile(syntax).fillSelect(keySelect, fromPropertySelect, whereSelect, params);

                Map<String,String> propertySelect = new HashMap<String, String>();
                for(Map.Entry<OrderExpr.Query,String> expr : queries.entrySet()) // ORDER BY не проверяем на Clause потому как всегда должна быть
                    propertySelect.put(expr.getValue(),"SUM(" + fromPropertySelect.get(expr.getKey().expr) +
                            ") OVER ("+ BaseUtils.clause("PARTITION BY ",BaseUtils.toString(BaseUtils.filterKeys(fromPropertySelect, expr.getKey().partitions).values(),",")) +
                            " ORDER BY " + BaseUtils.toString(BaseUtils.mapList(expr.getKey().orders,fromPropertySelect),",") + ")");
                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect,propertySelect),
                        BaseUtils.toString(whereSelect," AND "),"","","") + ")";
            }
        }

        private <K extends BaseExpr,I extends TranslateContext<I>,IJ extends TranslateContext<IJ>,J extends QueryJoin<K,IJ>,E extends QueryExpr<K,I,J>,Q extends QuerySelect<K,I,J,E>>
                    String getSource(Map<J,Q> selects, E expr) {
            J exprJoin = expr.getGroupJoin();

            KeyTranslator translator;
            for(Map.Entry<J,Q> group : selects.entrySet())
                if((translator=exprJoin.merge(group.getKey()))!=null)
                    return group.getValue().add(expr.query.translateDirect(translator),expr);

            Q select;
            if(((Object)exprJoin) instanceof GroupJoin) // нету группы - создаем, чтобы не нарушать модульность сделаем без наследования
                select = (Q) (Object) new GroupSelect((GroupJoin)(Object)exprJoin);
            else
                select = (Q) (Object) new OrderSelect((OrderJoin)(Object)exprJoin);

            selects.put(exprJoin,select);
            return select.add(expr.query,expr);
        }

        final Map<GroupJoin, GroupSelect> groups = new HashMap<GroupJoin, GroupSelect>();
        public String getSource(GroupExpr groupExpr) {
            return getSource(groups,groupExpr);
        }

        private final Map<OrderJoin,OrderSelect> orders = new HashMap<OrderJoin, OrderSelect>();
        public String getSource(OrderExpr orderExpr) {
            return getSource(orders,orderExpr);
        }
    }

    private static <K,AV> String fillInnerSelect(Map<K, KeyExpr> mapKeys, InnerWhere innerWhere, Where where, Map<AV, Expr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Collection<String> whereSelect, Map<ValueExpr,String> params, SQLSyntax syntax) {

        InnerSelect compile = new InnerSelect(innerWhere,syntax,params);
        // первым так как должны keySelect'ы и inner'ы заполнится
        for(Map.Entry<AV, Expr> joinProp : compiledProps.entrySet()) // свойства
            propertySelect.put(joinProp.getKey(), joinProp.getValue().getSource(compile));
        keySelect.putAll(BaseUtils.join(mapKeys,compile.keySelect));

        assert !keySelect.containsValue(null);
        assert !propertySelect.containsValue(null);
        assert !whereSelect.contains(null);

        return compile.getFrom(where,whereSelect);
    }

    String translateParam(String query,Map<String,String> paramValues) {
        Map<String,String> translateMap = new HashMap<String, String>();
        int paramNum = 0;
        for(Map.Entry<String,String> paramValue : paramValues.entrySet()) {
            String translate = "transp" + (paramNum++) + "nt";
            query = query.replaceAll(paramValue.getKey(),translate);
            translateMap.put(translate,paramValue.getValue());
        }
        for(Map.Entry<String,String> translateValue : translateMap.entrySet())
            query = query.replaceAll(translateValue.getKey(),translateValue.getValue());
        return query;
    }
    public Map<String, TypeObject> getQueryParams() {
        Map<String, TypeObject> mapValues = new HashMap<String, TypeObject>();
        for(Map.Entry<ValueExpr,String> param : params.entrySet())
            mapValues.put(param.getValue(),new TypeObject(param.getKey().object,param.getKey().objectClass.getType()));
        return mapValues;
    }

    private String fillSelect(Map<String,String> params, Map<K, String> fillKeySelect, Map<V, String> fillPropertySelect, Collection<String> fillWhereSelect) {
        for(Map.Entry<K,String> mapKey : keySelect.entrySet())
            fillKeySelect.put(mapKey.getKey(),translateParam(mapKey.getValue(), params));
        for(Map.Entry<V,String> mapProp : propertySelect.entrySet())
            fillPropertySelect.put(mapProp.getKey(),translateParam(mapProp.getValue(), params));
        for(String where : whereSelect)
            fillWhereSelect.add(translateParam(where, params));
        return translateParam(from, params);
    }

    // для GroupQuery
    String fillSelect(Map<K, String> fillKeySelect, Map<V, String> fillPropertySelect, Collection<String> fillWhereSelect, Map<ValueExpr,String> mapValues) {
        return fillSelect(BaseUtils.join(BaseUtils.reverse(params), mapValues), fillKeySelect, fillPropertySelect, fillWhereSelect);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session,boolean outSelect) throws SQLException {

        OrderedMap<Map<K,Object>,Map<V,Object>> execResult = new OrderedMap<Map<K, Object>, Map<V, Object>>();

        if(outSelect)
            System.out.println(select);

        PreparedStatement statement = session.getStatement(select,getQueryParams());
        try {
            ResultSet result = statement.executeQuery();
            try {
                while(result.next()) {
                    Map<K,Object> rowKeys = new HashMap<K, Object>();
                    for(Map.Entry<K,String> key : keyNames.entrySet())
                        rowKeys.put(key.getKey(), keyReaders.get(key.getKey()).read(result.getObject(key.getValue())));
                    Map<V,Object> rowProperties = new HashMap<V, Object>();
                    for(Map.Entry<V,String> property : propertyNames.entrySet())
                        rowProperties.put(property.getKey(),
                                propertyReaders.get(property.getKey()).read(result.getObject(property.getValue())));
                     execResult.put(rowKeys,rowProperties);
                }
            } finally {
                result.close();
            }
        } finally {
            statement.close();
        }

        return execResult;
    }

    void outSelect(SQLSession session) throws SQLException {
        // выведем на экран
        OrderedMap<Map<K, Object>, Map<V, Object>> result = execute(session,true);

        for(Map.Entry<Map<K,Object>,Map<V,Object>> rowMap : result.entrySet()) {
            for(Map.Entry<K,Object> key : rowMap.getKey().entrySet()) {
                System.out.print(key.getKey()+"-"+key.getValue());
                System.out.print(" ");
            }
            System.out.print("---- ");
            for(Map.Entry<V,Object> property : rowMap.getValue().entrySet()) {
                System.out.print(property.getKey()+"-"+property.getValue());
                System.out.print(" ");
            }

            System.out.println("");
        }
    }
}


/* !!!!! UNION ALL код            // в Properties закинем Orders,
            HashMap<Object,Query> UnionProps = new HashMap<Object, Query>(query.property);
            LinkedHashMap<String,Boolean> OrderNames = new LinkedHashMap<String, Boolean>();
            int io = 0;
            for(Map.Entry<Query,Boolean> Order : query.orders.entrySet()) {
                String OrderName = "order_"+io;
                UnionProps.put(OrderName,Order.getKey());
                OrderNames.put(OrderName,Order.getValue());
            }

            From = "";
            while(true) {
                Where AndWhere = QueryJoins.iterator().next();
                Map<K,String> AndKeySelect = new HashMap<K, String>();
                LinkedHashMap<Object,String> AndPropertySelect = new LinkedHashMap<Object, String>();
                Collection<String> AndWhereSelect = new ArrayList<String>();
                String AndFrom = fillAndSelect(Query.Keys,AndWhere,UnionProps,new LinkedHashMap<Query,Boolean>(),AndKeySelect,
                    AndPropertySelect,AndWhereSelect,QueryParams,new LinkedHashMap<String,Boolean>(), Syntax);

                LinkedHashMap<String,String> NamedProperties = new LinkedHashMap<String, String>();
                for(V Property : Query.Properties.keySet()) {
                    NamedProperties.put(PropertyNames.get(Property),AndPropertySelect.get(Property));
                    if(From.length()==0) PropertyOrder.add(Property);
                }
                for(String Order : OrderNames.keySet())
                    NamedProperties.put(Order,AndPropertySelect.get(Order));

                From = (From.length()==0?"":From+" UNION ALL ") +
                    Syntax.getSelect(AndFrom,Source.stringExpr(Source.mapNames(AndKeySelect,KeyNames,From.length()==0?KeyOrder:new ArrayList<K>()),NamedProperties),
                            Source.stringWhere(AndWhereSelect),"","","");

                OrWhere = OrWhere.andNot(AndWhere).getOr();
                if(OrWhere.isFalse()) break;
                break;
            }

            String Alias = "G";
            for(K Key : query.keys.keySet())
                keySelect.put(Key, Alias + "." + keyNames.get(Key));
            for(V Property : query.property.keySet())
                propertySelect.put(Property, Alias + "." + propertyNames.get(Property));

            from = "(" + from + ") "+Alias;

            select = syntax.getUnionOrder(from,Query.stringOrder(OrderNames), query.top ==0?"":String.valueOf(query.top));
            */
