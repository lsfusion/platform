package platform.server.data.query;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import platform.base.*;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.data.*;
import platform.server.data.expr.*;
import platform.server.data.expr.order.PartitionCalc;
import platform.server.data.expr.order.PartitionToken;
import platform.server.data.expr.query.*;
import platform.server.data.query.innerjoins.GroupJoinsWhere;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.type.*;
import platform.server.data.where.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

// нужен для Map'а ключей / значений
// Immutable/Thread Safe
public class CompiledQuery<K,V> {
    private final static Logger logger = Logger.getLogger(CompiledQuery.class);

    final public String from;
    final public Map<K,String> keySelect;
    final public Map<V,String> propertySelect;
    final public Collection<String> whereSelect;

    final public String select;
    final public List<K> keyOrder;
    final public List<V> propertyOrder;
    final public Map<K,String> keyNames;
    final public Map<K, ClassReader> keyReaders;
    final public Map<V,String> propertyNames;
    final public Map<V, ClassReader> propertyReaders;

    public boolean union;

    final Map<Value,String> params;
    public final ExecuteEnvironment env;

    private boolean checkQuery() {
        assert !params.containsValue(null);

        return true;
    }

    // перемаппит другой CompiledQuery
    public <MK,MV> CompiledQuery(CompiledQuery<MK,MV> compile,Map<K,MK> mapKeys,Map<V,MV> mapProperties, MapValuesTranslate mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = join(mapKeys, compile.keySelect);
        propertySelect = join(mapProperties, compile.propertySelect);
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
        keyNames = join(mapKeys, compile.keyNames);
        keyReaders = join(mapKeys, compile.keyReaders);
        propertyNames = join(mapProperties, compile.propertyNames);
        propertyReaders = join(mapProperties, compile.propertyReaders);
        union = compile.union;

        params = new HashMap<Value, String>();
        for(Map.Entry<Value,String> param : compile.params.entrySet())
            params.put(mapValues.translate(param.getKey()), param.getValue());
        
        env = compile.env.translateValues(mapValues);

        assert checkQuery();
    }

    private static class FullSelect extends CompileSource {

        private FullSelect(KeyType keyType, Map<Value, String> params, SQLSyntax syntax) {
            super(keyType, params, syntax);
        }

        public final Map<JoinData,String> joinData = new HashMap<JoinData, String>();

        public String getSource(Table.Join.Expr expr) {
            assert joinData.get(expr)!=null;
            return joinData.get(expr);
        }

        public String getSource(Table.Join.IsIn where) {
            assert joinData.get(where)!=null;
            return joinData.get(where);
        }

        public String getSource(GroupExpr groupExpr) {
            assert joinData.get(groupExpr)!=null;
            return joinData.get(groupExpr);
        }

        public String getSource(PartitionExpr partitionExpr) {
            assert joinData.get(partitionExpr)!=null;
            return joinData.get(partitionExpr);
        }

        public String getSource(RecursiveExpr recursiveExpr) {
            assert joinData.get(recursiveExpr)!=null;
            return joinData.get(recursiveExpr);
        }

        public String getSource(SubQueryExpr subQueryExpr) {
            assert joinData.get(subQueryExpr)!=null;
            return joinData.get(subQueryExpr);
        }

        public String getSource(IsClassExpr classExpr) {
            assert joinData.get(classExpr)!=null;
            return joinData.get(classExpr);
        }
    }

    public CompiledQuery(Query<K,V> query, SQLSyntax syntax, OrderedMap<V,Boolean> orders, int top, SubQueryContext subcontext, boolean recursive) {

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
        params = new HashMap<Value, String>();
        for(Value mapValue : query.getInnerValues())
            params.put(mapValue, "qwer" + (paramCount++) + "ffd");

        env = new ExecuteEnvironment();

        keyReaders = new HashMap<K, ClassReader>();
        propertyReaders = new HashMap<V, ClassReader>();
        for(Map.Entry<K,KeyExpr> key : query.mapKeys.entrySet())
            keyReaders.put(key.getKey(),query.where.isFalse()?NullReader.instance:key.getValue().getType(query.where));
        for(Map.Entry<V,Expr> property : query.properties.entrySet())
            propertyReaders.put(property.getKey(),query.where.isFalse()?NullReader.instance:property.getValue().getReader(query.where));

        boolean useFJ = syntax.useFJ();
        Result<Boolean> unionAll = new Result<Boolean>();
        Collection<GroupJoinsWhere> queryJoins = GroupJoinsWhere.pack(query.getWhereJoins(!useFJ && !recursive, unionAll));
        union = !useFJ && queryJoins.size() >= 2 && (unionAll.result || !Settings.instance.isUseFJInsteadOfUnion());
        if (union) { // сложный UNION запрос
            Map<V, Type> castTypes = new HashMap<V, Type>();
            for(Map.Entry<V, ClassReader> propertyReader : propertyReaders.entrySet())
                if(propertyReader.getValue() instanceof Type)
                    castTypes.put(propertyReader.getKey(), (Type)propertyReader.getValue());

            String fromString = "";
            for(GroupJoinsWhere queryJoin : queryJoins) {
                boolean orderUnion = syntax.orderUnion(); // нужно чтобы фигачило внутрь orders а то многие SQL сервера не видят индексы внутри union all
                fromString = (fromString.length()==0?"":fromString+" UNION " + (unionAll.result?"ALL ":"")) + "(" + getInnerSelect(query.mapKeys, queryJoin, queryJoin.getFullWhere().followTrue(query.properties, !queryJoin.isComplex()), params, orderUnion?orders:new OrderedMap<V, Boolean>(), orderUnion?top:0, syntax, keyNames, propertyNames, keyOrder, propertyOrder, castTypes, subcontext, false, env) + ")";
                if(!orderUnion)
                    castTypes = null;
            }

            String alias = "UALIAS";
            for(K key : query.mapKeys.keySet())
                keySelect.put(key, alias + "." + keyNames.get(key));
            for(V property : query.properties.keySet())
                propertySelect.put(property, alias + "." + propertyNames.get(property));

            from = "(" + fromString + ") "+alias;

            select = syntax.getUnionOrder(fromString, Query.stringOrder(propertyOrder, query.mapKeys.size(), orders, syntax), top ==0?"":String.valueOf(top));
        } else {
            if(queryJoins.size()==0) { // "пустой" запрос
                for(K key : query.mapKeys.keySet())
                    keySelect.put(key, SQLSyntax.NULL);
                for(V property : query.properties.keySet())
                    propertySelect.put(property, SQLSyntax.NULL);
                from = "empty";
            } else {
                if(queryJoins.size()==1) { // "простой" запрос
                    GroupJoinsWhere innerJoin = queryJoins.iterator().next();
                    from = fillInnerSelect(query.mapKeys, innerJoin, query.properties, keySelect, propertySelect, whereSelect, params, syntax, subcontext, env);
                } else // "сложный" запрос с full join'ами
                    from = fillFullSelect(query.mapKeys, queryJoins, query.properties, orders, top, keySelect, propertySelect, params, syntax, subcontext, env);
            }

            select = getSelect(from, keySelect, keyNames, keyOrder, propertySelect, propertyNames, propertyOrder, whereSelect, syntax, orders, top, false);
        }

        assert checkQuery();
    }

    // в общем случае получить AndJoinQuery под которые подходит Where
    private static Collection<AndJoinQuery> getWhereSubSet(Collection<AndJoinQuery> andWheres, Where where) {

        Collection<AndJoinQuery> result = new ArrayList<AndJoinQuery>();
        CheckWhere resultWhere = Where.FALSE;
        while(result.size()< andWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery lastQuery = null;
            CheckWhere lastWhere = null;
            for(AndJoinQuery and : andWheres)
                if(!result.contains(and)) {
                    lastQuery = and;
                    lastWhere = resultWhere.orCheck(lastQuery.innerSelect.getFullWhere());
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

        final WhereJoins whereJoins;

        public InnerJoins getInnerJoins() {
            return whereJoins.getInnerJoins();
        }

        public boolean isInner(InnerJoin join) {
            return getInnerJoins().means(join);
        }

        final Map<WhereJoin, Where> upWheres;

        final SubQueryContext subcontext;
        final KeyStat keyStat;

        public InnerSelect(KeyType keyType, KeyStat keyStat, WhereJoins whereJoins, Map<WhereJoin, Where> upWheres, SQLSyntax syntax, Map<Value, String> params, SubQueryContext subcontext) {
            super(keyType, params, syntax);

            this.keyStat = keyStat;
            this.subcontext = subcontext;
            this.whereJoins = whereJoins;
            this.upWheres = upWheres;
        }

        public void fillInnerJoins() { // заполним Inner Joins, чтобы чтобы keySelect'ы были
            whereJoins.getWhere(upWheres).getSource(this);
        }

        int aliasNum=0;
        final List<JoinSelect> joins = new ArrayList<JoinSelect>();

        private abstract class JoinSelect<I extends InnerJoin> {

            final String alias; // final
            final String join; // final
            final I innerJoin;

            protected abstract Map<String, BaseExpr> initJoins(I innerJoin);

            protected boolean isInner() {
                return InnerSelect.this.isInner(innerJoin);
            }

            protected JoinSelect(I innerJoin) {
                alias = subcontext.wrapAlias("t" + (aliasNum++));
                this.innerJoin = innerJoin;

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
                        assert isInner();
                        keySelect.put(keyJoin.getValue(),keyJoin.getKey());
                    } else
                        joinString = (joinString.length()==0?"":joinString+" AND ") + keyJoin.getKey() + "=" + keySource;
                }
                join = joinString;

                InnerSelect.this.joins.add(this);
            }

            public abstract String getSource(ExecuteEnvironment env);

            protected abstract Where getInnerWhere(); // assert что isInner
        }

        // получает условия следующие из логики inner join'ов SQL
        private Where getInnerWhere() {
            Where result = Where.TRUE;
            for(InnerJoin innerJoin : getInnerJoins()) {
                JoinSelect joinSelect = getJoinSelect(innerJoin);
                if(joinSelect!=null)
                    result = result.and(joinSelect.getInnerWhere());
            }
            return result;
        }

        public String getFrom(Where where, Collection<String> whereSelect, ExecuteEnvironment env) {
            where.getSource(this);
            whereSelect.add(where.followFalse(getInnerWhere().not()).getSource(this));

            if(joins.isEmpty()) return "dumb";

            String from;
            Iterator<JoinSelect> ij = joins.iterator();
            JoinSelect first = ij.next();
            if(first.isInner()) {
                from = first.getSource(env) + " " + first.alias;
                if(!(first.join.length()==0))
                    whereSelect.add(first.join);
            } else {
                from = "dumb";
                ij = joins.iterator();
            }

            while(ij.hasNext()) {
                JoinSelect join = ij.next();
                from = from + (join.isInner() ?"":" LEFT")+" JOIN " + join.getSource(env) + " " + join.alias  + " ON " + (join.join.length()==0?Where.TRUE_STRING:join.join);
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

            TableSelect(Table.Join join) {
                super(join);
                this.source = join.getQueryName(InnerSelect.this);
            }

            public String getSource(ExecuteEnvironment env) {
                return source;
            }

            protected Where getInnerWhere() {
                return innerJoin.getWhere();
            }
        }

        final Map<Table.Join, TableSelect> tables = new HashMap<Table.Join, TableSelect>();
        private String getAlias(Table.Join table) {
            TableSelect join = tables.get(table);
            if(join==null) {
                join = new TableSelect(table);
                tables.put(table,join);
            }
            return join.alias;
        }

        public String getSource(Table.Join.Expr expr) {
            return getAlias(expr.getInnerJoin())+"."+expr.property;
        }
        public String getSource(Table.Join.IsIn where) {
            return getAlias(where.getJoin()) + "." + where.getFirstKey() + " IS NOT NULL";
        }
        public String getSource(IsClassExpr classExpr) {
            return getSource(classExpr.getJoinExpr());
        }

        private abstract class QuerySelect<K extends Expr, I extends OuterContext<I>,J extends QueryJoin<K,?,?,?>,E extends QueryExpr<K,I,J,?,?>> extends JoinSelect<J> {
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

        private class GroupSelect extends QuerySelect<Expr, GroupExpr.Query,GroupJoin,GroupExpr> {

            final QuickSet<KeyExpr> keys;

            GroupSelect(GroupJoin groupJoin) {
                super(groupJoin);
                keys = groupJoin.getInnerKeys();
            }

            public String getSource(ExecuteEnvironment env) {

                Set<Expr> queryExprs = new HashSet<Expr>(group.values()); // так как может одновременно и SUM и MAX нужен
                Where exprWhere = Where.FALSE;
                for(GroupExpr.Query query : queries.keySet()) {
                    queryExprs.addAll(query.getExprs());
                    exprWhere = exprWhere.or(query.getWhere());
                }
                Where fullWhere = exprWhere.and(platform.server.data.expr.Expr.getWhere(group));

                StatKeys<Expr> statKeys = innerJoin.getStatKeys(keyStat); // определяем ключи которые надо протолкнуть
                Set<Expr> groupExprs = innerJoin.getJoins().keySet();
                WhereBuilder insufWhere = new WhereBuilder();
                if(groupExprs.size() > 1 && whereJoins.getStatKeys(groupExprs, upWheres, innerJoin, insufWhere, keyStat).rows.less(statKeys.rows)) // проталкивание по многим ключам
                    fullWhere = fullWhere.and(GroupExpr.create(innerJoin.getJoins(), insufWhere.toWhere(), BaseUtils.toMap(groupExprs)).getWhere());
                else {
                    for(Expr key : groupExprs) { // проталкивание по одному ключу
                        WhereBuilder insufDistWhere = new WhereBuilder();
                        if(whereJoins.getStatKeys(Collections.singleton(key), upWheres, innerJoin, insufDistWhere, keyStat).rows.less(statKeys.distinct.get(key)))
                            fullWhere = fullWhere.and(GroupExpr.create(Collections.singletonMap(0, innerJoin.group.get(key)), insufDistWhere.toWhere(), Collections.singletonMap(0, key)).getWhere());
                    }
                }

                Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
                Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
                String fromSelect = new Query<KeyExpr,Expr>(keys.toMap(),BaseUtils.toMap(queryExprs), fullWhere)
                    .compile(syntax, subcontext).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, params, env);

                Map<String, String> keySelect = BaseUtils.join(group,fromPropertySelect);
                Map<String,String> propertySelect = new HashMap<String, String>();
                for(Map.Entry<GroupExpr.Query, String> expr : queries.entrySet())
                    propertySelect.put(expr.getValue(),expr.getKey().getSource(fromPropertySelect, syntax));
                return "(" + getGroupSelect(fromSelect, keySelect, propertySelect, whereSelect) + ")";
            }

            protected Where getInnerWhere() {
                // бежим по всем exprs'ам и проверяем что нет AggrType'а
                Where result = Where.TRUE;
                for(Map.Entry<GroupExpr.Query, GroupExpr> expr : exprs.entrySet()) {
                    if(expr.getKey().type.canBeNull())
                        return Where.TRUE;
                    result = result.or(expr.getValue().getWhere());
                }
                return result;
            }
        }

        private String getGroupBy(Collection<String> keySelect) {
            return BaseUtils.evl(BaseUtils.toString((Collection) (syntax.supportGroupNumbers() ? BaseUtils.genList(keySelect.size()) : keySelect), ","), "3+2");
        }

        private class PartitionSelect extends QuerySelect<KeyExpr, PartitionExpr.Query,PartitionJoin,PartitionExpr> {

            final Map<KeyExpr,BaseExpr> mapKeys;
            private PartitionSelect(PartitionJoin partitionJoin) {
                super(partitionJoin);
                mapKeys = partitionJoin.group;
            }

            public String getSource(ExecuteEnvironment env) {

                Set<Expr> queryExprs = new HashSet<Expr>();
                for(PartitionExpr.Query query : queries.keySet())
                    queryExprs.addAll(query.getExprs());
                Where fullWhere = innerJoin.getWhere();

                if(Settings.instance.isPushOrderWhere()) {
                    StatKeys<KeyExpr> statKeys = innerJoin.getStatKeys(keyStat); // определяем ключи которые надо протолкнуть
                    Set<KeyExpr> joins = innerJoin.getJoins().keySet();
                    WhereBuilder insufWhere = new WhereBuilder();
                    if(whereJoins.getStatKeys(joins, upWheres, innerJoin, insufWhere, keyStat).rows.less(statKeys.rows)) // проталкивание по многим ключам
                        fullWhere = fullWhere.and(PartitionExpr.getPartitionWhere(false, insufWhere.toWhere(), innerJoin.getJoins(), innerJoin.getPartitions()));
                }

                Map<String,String> keySelect = new HashMap<String,String>();
                Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
                Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
                String fromSelect = new Query<String,Expr>(group,BaseUtils.toMap(queryExprs),fullWhere)
                    .compile(syntax, subcontext).fillSelect(keySelect, fromPropertySelect, whereSelect, params, env);

                // обработка multi-level order'ов
                Map<PartitionToken, String> tokens = new HashMap<PartitionToken, String>();
                Map<PartitionCalc, String> resultNames = new HashMap<PartitionCalc, String>();
                for(Map.Entry<PartitionExpr.Query, String> query : queries.entrySet()) {
                    PartitionCalc calc = query.getKey().type.createAggr(tokens,
                            BaseUtils.mapList(query.getKey().exprs, fromPropertySelect),
                            BaseUtils.mapOrder(query.getKey().orders, fromPropertySelect),
                            new HashSet<String>(filterKeys(fromPropertySelect, query.getKey().partitions).values()));
                    resultNames.put(calc, query.getValue());
                }

                Set<String> proceeded = new HashSet<String>();
                for(int i=1;;i++) {
                    Set<PartitionToken> next = new HashSet<PartitionToken>();
                    boolean last = true;
                    for(Map.Entry<PartitionToken, String> token : tokens.entrySet()) {
                        boolean neededUp = token.getKey().next.isEmpty(); // верхний, надо протаскивать
                        last = true;
                        for(PartitionCalc usedToken : token.getKey().next) {
                            if(usedToken.getLevel() >= i) {
                                if(usedToken.getLevel() == i) // если тот же уровень
                                    next.add(usedToken);
                                else
                                    neededUp = true;
                                last = false;
                            }
                        }
                        if(neededUp)
                            next.add(token.getKey());
                    }
                    if(last)
                        return fromSelect;

                    Map<PartitionToken, String> nextTokens = new HashMap<PartitionToken, String>();
                    Map<String, String> propertySelect = new HashMap<String, String>(BaseUtils.toMap(proceeded));
                    for(PartitionToken token : next) {
                        String name = token.next.isEmpty() ? resultNames.get((PartitionCalc) token) : "ne" + nextTokens.size(); // если верхний то нужно с нормальным именем тащить
                        propertySelect.put(name, token.getSource(tokens, syntax));
                        nextTokens.put(token, name);
                    }
                    fromSelect = "(" + syntax.getSelect(fromSelect + (i>1?" q":""), SQLSession.stringExpr(keySelect,propertySelect),
                        (i>1?"":BaseUtils.toString(whereSelect," AND ")),"","","") + ")";
                    keySelect = BaseUtils.toMap(keySelect.keySet()); // ключи просто превращаем в имена
                    tokens = nextTokens;
                }
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(Map.Entry<PartitionExpr.Query, PartitionExpr> expr : exprs.entrySet())
                    if(!expr.getKey().type.canBeNull())
                        result = result.and(expr.getValue().getWhere());
                return result;
            }
        }

        private class SubQuerySelect extends QuerySelect<KeyExpr,Expr,SubQueryJoin,SubQueryExpr> {

            final Map<KeyExpr,BaseExpr> mapKeys;
            private SubQuerySelect(SubQueryJoin subQueryJoin) {
                super(subQueryJoin);
                mapKeys = subQueryJoin.group;
            }

            public String getSource(ExecuteEnvironment env) {

                Where fullWhere = innerJoin.getWhere();

                StatKeys<KeyExpr> statKeys = innerJoin.getStatKeys(keyStat);
                Set<KeyExpr> joins = innerJoin.getJoins().keySet();
                WhereBuilder insufWhere = new WhereBuilder();
                if(whereJoins.getStatKeys(joins, upWheres, innerJoin, insufWhere, keyStat).rows.less(statKeys.rows)) // проталкивание по многим ключам
                    fullWhere = fullWhere.and(GroupExpr.create(innerJoin.getJoins(), insufWhere.toWhere(), BaseUtils.toMap(joins)).getWhere());

                Map<String, String> keySelect = new HashMap<String, String>();
                Map<String, String> propertySelect = new HashMap<String, String>();
                Collection<String> whereSelect = new ArrayList<String>();
                String fromSelect = new Query<String,String>(group, reverse(queries), fullWhere).compile(syntax, subcontext).fillSelect(keySelect, propertySelect, whereSelect, params, env);
                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect,propertySelect),
                    BaseUtils.toString(whereSelect," AND "),"","","") + ")";

            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(Map.Entry<Expr, SubQueryExpr> expr : exprs.entrySet())
                    result = result.and(expr.getValue().getWhere());
                return result;
            }
        }

        protected String getGroupSelect(String fromSelect, Map<String, String> keySelect, Map<String, String> propertySelect, Collection<String> whereSelect) {
            return syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect, propertySelect), BaseUtils.toString(whereSelect," AND "), "", getGroupBy(keySelect.values()), "");
        }

        private class RecursiveSelect extends QuerySelect<KeyExpr,RecursiveExpr.Query,RecursiveJoin,RecursiveExpr> {
            private RecursiveSelect(RecursiveJoin recJoin) {
                super(recJoin);
            }

            private String getSelect(Map<String, KeyExpr> keys, Map<String, Expr> props, Map<String, Type> columnTypes, Where where, List<String> keyOrder, List<String> propOrder, boolean recursive, Map<Value, String> params, SubQueryContext subcontext, ExecuteEnvironment env) {
                Map<String, KeyExpr> allKeys = new HashMap<String, KeyExpr>(keys); int pv = 0;
                for(KeyExpr mapIt : innerJoin.getMapIterate().values())
                    allKeys.put("pv_"+(pv++),mapIt);

                Map<String, String> keySelect = new HashMap<String, String>();
                Map<String, String> propertySelect = new HashMap<String, String>();
                Collection<String> whereSelect = new ArrayList<String>();
                String fromSelect = new Query<String,String>(allKeys, props, where).compile(syntax, subcontext, recursive).fillSelect(keySelect, propertySelect, whereSelect, params, env);

                OrderedMap<String, String> orderKeySelect = SQLSession.mapNames(BaseUtils.filterKeys(keySelect, keys.keySet()), keyOrder);
                OrderedMap<String, String> orderPropertySelect = SQLSession.mapNames(propertySelect, propOrder);

//                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(orderKeySelect, orderPropertySelect), BaseUtils.toString(whereSelect," AND "),"","","") + ")";
                OrderedMap<String, String> orderGroupPropertySelect = new OrderedMap<String, String>();
                for(Map.Entry<String, String> prop : orderPropertySelect.entrySet()) {
                    Type type = columnTypes.get(prop.getKey());
                    orderGroupPropertySelect.put(prop.getKey(), "CAST(" + (type instanceof ArrayClass ? "AGGAR_SETADD" : "SUM") + "(" + prop.getValue() + ")" + " AS " + type.getDB(syntax) + ")");
                }
                return "(" + getGroupSelect(fromSelect, orderKeySelect, orderGroupPropertySelect, whereSelect) + ")";
            }

            public String getSource(ExecuteEnvironment env) {

                Map<KeyExpr, KeyExpr> mapIterate = innerJoin.getMapIterate();

                Where initialWhere = innerJoin.getInitialWhere();
                Where stepWhere = innerJoin.getStepWhere();

                boolean cyclesPossible = true; // теоретически - надо проверить, что функция не строго возрастающая, то есть из stepWhere => in > ip OR ...

                String rowPath = "qwpather";

                Collection<String> props = queries.values();
                if(cyclesPossible)
                    props = BaseUtils.add(props, rowPath);
                
                String recName = subcontext.wrapRecursion("rectable"); Map<String, KeyExpr> recKeys = new HashMap<String, KeyExpr>();
                Join<String> recJoin = innerJoin.getRecJoin(props, recName, recKeys);
                
                Map<String, Expr> initialExprs = new HashMap<String, Expr>();
                Map<String, Expr> stepExprs = new HashMap<String, Expr>();
                Map<String, String> propertySelect = new HashMap<String, String>();
                for(Map.Entry<RecursiveExpr.Query, String> query : queries.entrySet()) {
                    initialExprs.put(query.getValue(), query.getKey().initial);
                    stepExprs.put(query.getValue(), recJoin.getExpr(query.getValue()).mult(query.getKey().step, RecursiveExpr.type));
                    propertySelect.put(query.getValue(), "SUM(" + query.getValue() + ")");
                }

                Where recWhere;
                if(cyclesPossible) {
                    Expr concKeys = ConcatenateExpr.create(new ArrayList<KeyExpr>(mapIterate.keySet()));
                    ArrayClass rowType = ArrayClass.get(concKeys.getType(initialWhere));
                    Expr rowSource = FormulaExpr.create1("CAST(ARRAY[prm1] AS " + rowType.getDB(syntax) + ")", rowType, concKeys); // баг сервера, с какого-то бодуна ARRAY[char(8)] дает text[]

                    initialExprs.put(rowPath, rowSource); // заполняем начальный путь
                    Expr prevPath = recJoin.getExpr(rowPath);
                    stepExprs.put(rowPath, FormulaExpr.create2("CAST((prm1 || prm2) AS " + rowType.getDB(syntax) + ")", rowType, prevPath, rowSource)); // добавляем тек. вершину

                    Where noNodeCycle = concKeys.compare(prevPath, Compare.INARRAY).not();
                    Expr maxExpr = RecursiveExpr.type.getStaticExpr(RecursiveExpr.maxvalue);
                    recWhere = Where.TRUE;
                    for(String query : queries.values()) {
                        stepExprs.put(query, stepExprs.get(query).ifElse(noNodeCycle, maxExpr)); // если цикл даем максимальное значение
                        recWhere = recWhere.and(recJoin.getExpr(query).compare(maxExpr, Compare.LESS)); // останавливаемся если количество значений становится очень большим
                    }
                } else
                    recWhere = recJoin.getWhere();

                Where pushWhere = Where.TRUE; // проталкивание в итерацию начальных групп
                Map<KeyExpr, BaseExpr> staticGroup = BaseUtils.filterNotKeys(innerJoin.getJoins(), mapIterate.keySet());
                StatKeys<KeyExpr> statKeys = initialWhere.getStatKeys(new QuickSet<KeyExpr>(staticGroup.keySet())); // определяем ключи которые надо протолкнуть
                WhereBuilder insufWhere = new WhereBuilder(); // statKeys.rows
                if(whereJoins.getStatKeys(staticGroup.keySet(), upWheres, innerJoin, insufWhere, keyStat).rows.less(statKeys.rows)) // проталкивание по многим ключам
                    pushWhere = GroupExpr.create(staticGroup, insufWhere.toWhere(), BaseUtils.toMap(staticGroup.keySet())).getWhere();

                boolean useRecursionFunction = true;

                String outerParams = null;
                Map<Value, String> innerParams;
                if(useRecursionFunction) {
                    QuickSet<Value> values = AbstractOuterContext.getOuterValues(BaseUtils.add(queries.keySet(), pushWhere));
                    outerParams = "";
                    innerParams = new HashMap<Value, String>();
                    int iv = 1;
                    for(int i=0;i<values.size;i++) {
                        Value value = values.get(i);
                        String paramValue = params.get(value);
                        if(!value.getParseInterface().isSafeString()) {
                            Type type = ((ValueExpr)value).getType();
                            innerParams.put(value, "CAST($1["+(iv++)+"] AS "+type.getDB(syntax)+")");
                            outerParams += "," + "CAST(" + paramValue + " AS " + syntax.getBinaryType(type.getBinaryLength(syntax.isBinaryString())) +")";
                        } else
                            innerParams.put(value, paramValue);
                    }
                    if(outerParams.isEmpty()) // забавный прикол СУБД (в частности postgre)
                        outerParams = ",CAST('x' AS char(1))";
                } else
                    innerParams = params;

                Map<String, Type> columnTypes = new HashMap<String, Type>();
                for(Map.Entry<String, KeyExpr> recKey : recKeys.entrySet())
                    columnTypes.put(recKey.getKey(), recKey.getValue().getType(initialWhere));
                for(Map.Entry<String, Expr> initialExpr : initialExprs.entrySet())
                    columnTypes.put(initialExpr.getKey(), initialExpr.getValue().getType(initialWhere));

                SubQueryContext pushContext = subcontext.pushRecursion();// чтобы имена не пересекались

                List<String> keyOrder = new ArrayList<String>();
                List<String> propOrder = new ArrayList<String>();
                String initialSelect = getSelect(recKeys, initialExprs, columnTypes, initialWhere.and(pushWhere), keyOrder, propOrder, false, innerParams, pushContext, env);
                if(!useRecursionFunction) // чтобы избежать ограничения на 2 раза использования
                    stepWhere = SubQueryExpr.create(stepWhere);
                String stepSelect = getSelect(recKeys, stepExprs, columnTypes, stepWhere.and(recWhere), keyOrder, propOrder, true, innerParams, pushContext, env);
                List<String> fieldOrder = BaseUtils.mergeList(keyOrder, propOrder);

                Map<String, String> keySelect = BaseUtils.join(group, BaseUtils.reverse(recKeys));
                if(useRecursionFunction) {
                    env.addNoReadOnly();
                    return "(" + getGroupSelect("recursion('" + recName +"'," +
                            "'" + StringEscapeUtils.escapeSql(initialSelect)+"','" +StringEscapeUtils.escapeSql(stepSelect)+"'"+outerParams+") recursion ("
                            + Field.getDeclare(BaseUtils.mapOrder(fieldOrder, columnTypes), syntax) + ")", keySelect, propertySelect, new ArrayList<String>()) + ")";
                } else {
                    return "(WITH RECURSIVE " + recName + "(" + BaseUtils.toString(fieldOrder, ",") + ") AS (" +
                            initialSelect + " UNION ALL " + stepSelect + ") " +
                                getGroupSelect(recName, keySelect, propertySelect, new ArrayList<String>()) + ")";
                }
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(Map.Entry<RecursiveExpr.Query, RecursiveExpr> expr : exprs.entrySet())
                    result = result.and(expr.getValue().getWhere());
                return result;
            }
        }

        private <K extends Expr,I extends OuterContext<I>,IJ extends OuterContext<IJ>,J extends QueryJoin<K,IJ,J,?>,E extends QueryExpr<K,I,J,?,?>,Q extends QuerySelect<K, I,J,E>>
                    String getSource(Map<J,Q> selects, E expr) {
            J exprJoin = expr.getInnerJoin();

            MapTranslate translator;
            for(Map.Entry<J,Q> group : selects.entrySet())
                if((translator= exprJoin.mapInner(group.getKey(), false))!=null)
                    return group.getValue().add(expr.query.translateOuter(translator),expr);

            Q select;
            if(((Object)exprJoin) instanceof GroupJoin) // нету группы - создаем, чтобы не нарушать модульность сделаем без наследования
                select = (Q) (Object) new GroupSelect((GroupJoin)(Object)exprJoin);
            else
            if(((Object)exprJoin) instanceof PartitionJoin)
                select = (Q) (Object) new PartitionSelect((PartitionJoin)(Object)exprJoin);
            else
            if(((Object)exprJoin) instanceof RecursiveJoin)
                select = (Q) (Object) new RecursiveSelect((RecursiveJoin)(Object)exprJoin);
            else
                select = (Q) (Object) new SubQuerySelect((SubQueryJoin)(Object)exprJoin);

            selects.put(exprJoin,select);
            return select.add(expr.query,expr);
        }

        final Map<GroupJoin, GroupSelect> groups = new HashMap<GroupJoin, GroupSelect>();
        final Map<GroupExpr, String> groupExprSources = new HashMap<GroupExpr, String>();
        public String getSource(GroupExpr groupExpr) {
            if(Settings.instance.getInnerGroupExprs() >0 && !isInner(groupExpr.getInnerJoin())) { // если left join
                String groupExprSource = groupExprSources.get(groupExpr);
                if(groupExprSource==null) {
                    groupExprSource = groupExpr.getExprSource(this, subcontext.pushAlias(groupExprSources.size()));
                    groupExprSources.put(groupExpr, groupExprSource);
                }
                return groupExprSource;
            } else
                return getSource(groups, groupExpr);
        }

        private final Map<PartitionJoin,PartitionSelect> partitions = new HashMap<PartitionJoin, PartitionSelect>();
        public String getSource(PartitionExpr partitionExpr) {
            return getSource(partitions, partitionExpr);
        }

        private final Map<RecursiveJoin,RecursiveSelect> recursives = new HashMap<RecursiveJoin, RecursiveSelect>();
        public String getSource(RecursiveExpr recursiveExpr) {
            return getSource(recursives, recursiveExpr);
        }

        private final Map<SubQueryJoin,SubQuerySelect> subqueries = new HashMap<SubQueryJoin, SubQuerySelect>();
        public String getSource(SubQueryExpr subQueryExpr) {
            return getSource(subqueries, subQueryExpr);
        }

        private JoinSelect getJoinSelect(InnerJoin innerJoin) {
            if(innerJoin instanceof Table.Join)
                return tables.get((Table.Join)innerJoin);
            if(innerJoin instanceof GroupJoin)
                return groups.get((GroupJoin)innerJoin);
            if(innerJoin instanceof PartitionJoin)
                return partitions.get((PartitionJoin)innerJoin);
            if(innerJoin instanceof RecursiveJoin)
                return partitions.get((RecursiveJoin)innerJoin);
            if(innerJoin instanceof SubQueryJoin)
                return subqueries.get((SubQueryJoin)innerJoin);
            throw new RuntimeException("no matching class");
        }
    }

    private static <V> Map<V, String> castProperties(Map<V,String> propertySelect, Map<V,Type> castTypes, SQLSyntax syntax) { // проставим Cast'ы для null'ов
        Map<V,String> castPropertySelect = new HashMap<V, String>();
        for(Map.Entry<V,String> property : propertySelect.entrySet()) {
            String propertyString = property.getValue();
            Type castType;
            if(propertyString.equals(SQLSyntax.NULL) && (castType = castTypes.get(property.getKey()))!=null) // кривовато с проверкой на null, но собсно и надо чтобы обойти баг
                propertyString = "CAST(" + propertyString + " AS " + castType.getDB(syntax) + ")";
            castPropertySelect.put(property.getKey(), propertyString);
        }
        return castPropertySelect;        
    }

    // castTypes параметр чисто для бага Postgre и может остальных
    private static <K,V> String getInnerSelect(Map<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, Map<V, Expr> compiledProps, Map<Value, String> params, OrderedMap<V, Boolean> orders, int top, SQLSyntax syntax, Map<K, String> keyNames, Map<V, String> propertyNames, List<K> keyOrder, List<V> propertyOrder, Map<V, Type> castTypes, SubQueryContext subcontext, boolean noInline, ExecuteEnvironment env) {
        Map<K,String> andKeySelect = new HashMap<K, String>(); Collection<String> andWhereSelect = new ArrayList<String>(); Map<V,String> andPropertySelect = new HashMap<V, String>();
        String andFrom = fillInnerSelect(mapKeys, innerSelect, compiledProps, andKeySelect, andPropertySelect, andWhereSelect, params, syntax, subcontext, env);

        if(castTypes!=null)
            andPropertySelect = castProperties(andPropertySelect, castTypes, syntax);

        return getSelect(andFrom, andKeySelect, keyNames, keyOrder, andPropertySelect, propertyNames, propertyOrder, andWhereSelect, syntax, orders, top, noInline);
    }

    private static <K,V> String getSelect(String from, Map<K, String> keySelect, Map<K, String> keyNames, List<K> keyOrder, Map<V, String> propertySelect, Map<V, String> propertyNames, List<V> propertyOrder, Collection<String> whereSelect, SQLSyntax syntax, OrderedMap<V, Boolean> orders, int top, boolean noInline) {
        return syntax.getSelect(from, SQLSession.stringExpr(SQLSession.mapNames(keySelect, keyNames, keyOrder),
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)) + (noInline && syntax.inlineTrouble()?",random()":""),
                BaseUtils.toString(whereSelect, " AND "), Query.stringOrder(propertyOrder,keySelect.size(), orders,syntax),"",top==0?"":String.valueOf(top));
    }

    private static <K,AV> String fillSingleSelect(Map<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, Map<AV, Expr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Map<Value,String> params, SQLSyntax syntax, SubQueryContext subcontext, ExecuteEnvironment env) {
        return fillFullSelect(mapKeys, Collections.singleton(innerSelect), compiledProps, new OrderedMap<AV, Boolean>(), 0, keySelect, propertySelect, params, syntax, subcontext, env);

/*        FullSelect FJSelect = new FullSelect(innerSelect.where, params,syntax); // для keyType'а берем первый where

        MapWhere<JoinData> joinDatas = new MapWhere<JoinData>();
        for(Map.Entry<AV, Expr> joinProp : compiledProps.entrySet())
            joinProp.getValue().fillJoinWheres(joinDatas, Where.TRUE);

        String innerAlias = subcontext+"inalias";
        Map<String, Expr> joinProps = new HashMap<String, Expr>();
        // затем все данные по JoinSelect'ам по вариантам
        for(JoinData joinData : joinDatas.keys()) {
            String joinName = "join_" + joinProps.size();
            joinProps.put(joinName, joinData.getFJExpr());
            FJSelect.joinData.put(joinData,joinData.getFJString(innerAlias +'.'+joinName));
        }

        Map<K, String> keyNames = new HashMap<K, String>();
        for(K key : mapKeys.keySet()) {
            String keyName = "jkey" + keyNames.size();
            keyNames.put(key, keyName);
            keySelect.put(key, innerAlias +"."+ keyName);
            FJSelect.keySelect.put(mapKeys.get(key),innerAlias +"."+ keyName);
        }

        for(Map.Entry<AV, Expr> mapProp : compiledProps.entrySet())
            propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(FJSelect));

        return "(" + getInnerSelect(mapKeys, innerSelect, joinProps, params, new OrderedMap<String, Boolean>(),0 , syntax, keyNames, BaseUtils.toMap(joinProps.keySet()), new ArrayList<K>(), new ArrayList<String>(), null, subcontext, true) + ") " + innerAlias;*/
    }

    private static <K,AV> String fillInnerSelect(Map<K, KeyExpr> mapKeys, final GroupJoinsWhere innerSelect, Map<AV, Expr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Collection<String> whereSelect, Map<Value, String> params, SQLSyntax syntax, SubQueryContext subcontext, ExecuteEnvironment env) {

        final InnerSelect compile = new InnerSelect(innerSelect.where, innerSelect.where,innerSelect.joins,innerSelect.upWheres,syntax,params, subcontext);

        if(Settings.instance.getInnerGroupExprs() > 0) { // если не одни joinData
            final Set<GroupExpr> groupExprs = new HashSet<GroupExpr>(); final Counter repeats = new Counter();
            for(Expr property : compiledProps.values())
                property.enumerate(new ExprEnumerator() {
                    public boolean enumerate(OuterContext join) {
                        if(join instanceof JoinData) { // если JoinData то что внутри не интересует
                            if(join instanceof GroupExpr && !compile.isInner(((GroupExpr) join).getInnerJoin()) && !groupExprs.add((GroupExpr)join))
                                repeats.add();
                            return false;
                        }
                        return true;
                    }
                });
            if(repeats.getValue() > Settings.instance.getInnerGroupExprs())
                return fillSingleSelect(mapKeys, innerSelect, compiledProps, keySelect, propertySelect, params, syntax, subcontext, env);
        }

        compile.fillInnerJoins();
        QueryTranslator keyEqualTranslator = innerSelect.keyEqual.getTranslator();
        for(Map.Entry<AV, Expr> joinProp : keyEqualTranslator.translate(compiledProps).entrySet()) // свойства
            propertySelect.put(joinProp.getKey(), joinProp.getValue().getSource(compile));
        for(Map.Entry<K,KeyExpr> mapKey : mapKeys.entrySet()) {
            Expr keyValue = keyEqualTranslator.translate(mapKey.getValue());
            keySelect.put(mapKey.getKey(),BaseUtils.hashEquals(keyValue,mapKey.getValue())?compile.keySelect.get(mapKey.getValue()):keyValue.getSource(compile));
        }

        return compile.getFrom(innerSelect.where, whereSelect, env);
    }

    private static <K,AV> String fillFullSelect(Map<K, KeyExpr> mapKeys, Collection<GroupJoinsWhere> innerSelects, Map<AV, Expr> compiledProps, OrderedMap<AV,Boolean> orders, int top, Map<K, String> keySelect, Map<AV, String> propertySelect, Map<Value,String> params, SQLSyntax syntax, SubQueryContext subcontext, ExecuteEnvironment env) {
        FullSelect FJSelect = new FullSelect(innerSelects.iterator().next().getFullWhere(), params,syntax); // для keyType'а берем первый where

        // создаем And подзапросыs
        Collection<AndJoinQuery> andProps = new ArrayList<AndJoinQuery>();
        for(GroupJoinsWhere andWhere : innerSelects)
            andProps.add(new AndJoinQuery(andWhere,subcontext.wrapAlias("f"+andProps.size())));

        MapWhere<JoinData> joinDataWheres = new MapWhere<JoinData>();
        for(Map.Entry<AV, Expr> joinProp : compiledProps.entrySet())
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
                if(!and.innerSelect.getFullWhere().means(joinExpr.getWhere().not())) { // проверим что не всегда null
                    and.properties.put(joinName, joinExpr);
                    dataAnds.add(and);
                }
            }
            String joinSource = ""; // заполняем Source
            if(dataAnds.size()==0)
                throw new RuntimeException(ServerResourceBundle.getString("data.query.should.not.be"));
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
        for(Map.Entry<AV, Boolean> order : orders.entrySet()) {
            String orderName = "order_" + (io++);
            String orderFJ = "";
            for(AndJoinQuery and : andProps) {
                and.properties.put(orderName, compiledProps.get(order.getKey()));
                orderFJ = (orderFJ.length()==0?"":orderFJ+",") + and.alias + "." + orderName;
            }
            if(!(top==0)) // если все то не надо упорядочивать, потому как в частности MS SQL не поддерживает
                orderAnds.put(orderName,order.getValue());
            propertySelect.put(order.getKey(),"COALESCE("+orderFJ+")");
        }

        int keyCount = 0;
        Map<K, String> keyNames = new HashMap<K, String>();
        for(K Key : mapKeys.keySet())
            keyNames.put(Key,"jkey"+(keyCount++));

        // бежим по всем And'ам делаем JoinSelect запросы, потом объединяем их FULL'ами
        String compileFrom = "";
        boolean first = true; // для COALESCE'ов
        for(AndJoinQuery and : andProps) {
            // закинем в And.Properties OrderBy, все равно какие порядки ключей и выражений
            String andSelect = "(" + getInnerSelect(mapKeys, and.innerSelect, and.properties, params, orderAnds, top, syntax, keyNames, BaseUtils.toMap(and.properties.keySet()), new ArrayList<K>(), new ArrayList<String>(), null, subcontext, innerSelects.size()==1, env) + ") " + and.alias;

            if(compileFrom.length()==0) {
                compileFrom = andSelect;
                for(K Key : mapKeys.keySet())
                    keySelect.put(Key, and.alias +"."+ keyNames.get(Key));
            } else {
                String andJoin = "";
                for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
                    String andKey = and.alias + "." + keyNames.get(mapKey.getKey());
                    andJoin = (andJoin.length()==0?"":andJoin + " AND ") + andKey + "=" + (first?mapKey.getValue():"COALESCE("+mapKey.getValue()+")");
                    mapKey.setValue(mapKey.getValue()+","+andKey);
                }
                compileFrom = compileFrom + " FULL JOIN " + andSelect + " ON " + (andJoin.length()==0? Where.TRUE_STRING :andJoin);
                first = false;
            }
        }

        // полученные KeySelect'ы в Data
        for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
            if(innerSelects.size()>1)
                mapKey.setValue("COALESCE("+mapKey.getValue()+")"); // обернем в COALESCE
            FJSelect.keySelect.put(mapKeys.get(mapKey.getKey()),mapKey.getValue());
        }

        // закидываем PropertySelect'ы
        for(Map.Entry<AV, Expr> mapProp : compiledProps.entrySet())
            if(!orders.containsKey(mapProp.getKey())) // orders'ы уже обработаны
                propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(FJSelect));

        return compileFrom;
    }

    private String translateParam(String query,Map<String,String> paramValues) {
        Map<String,String> translateMap = new HashMap<String, String>();
        int paramNum = 0;
        for(Map.Entry<String,String> paramValue : paramValues.entrySet()) {
            String translate = "transp" + (paramNum++) + "nt";
            query = query.replace(paramValue.getKey(), translate);
            translateMap.put(translate,paramValue.getValue());
        }
        for(Map.Entry<String,String> translateValue : translateMap.entrySet())
            query = query.replace(translateValue.getKey(), translateValue.getValue());
        return query;
    }
    public Map<String, ParseInterface> getQueryParams(QueryEnvironment env) {
        Map<String, ParseInterface> mapValues = new HashMap<String, ParseInterface>();
        for(Map.Entry<Value,String> param : params.entrySet())
            mapValues.put(param.getValue(),param.getKey().getParseInterface());
        mapValues.put(SQLSession.userParam, env.getSQLUser());
        mapValues.put(SQLSession.computerParam, env.getSQLComputer());
        mapValues.put(SQLSession.sessionParam, env.getID());
        mapValues.put(SQLSession.isServerRestartingParam, env.getIsServerRestarting());

        return mapValues;
    }

    private String fillSelect(Map<String,String> params, Map<K, String> fillKeySelect, Map<V, String> fillPropertySelect, Collection<String> fillWhereSelect, ExecuteEnvironment fillEnv) {
        for(Map.Entry<K,String> mapKey : keySelect.entrySet())
            fillKeySelect.put(mapKey.getKey(),translateParam(mapKey.getValue(), params));
        for(Map.Entry<V,String> mapProp : propertySelect.entrySet())
            fillPropertySelect.put(mapProp.getKey(),translateParam(mapProp.getValue(), params));
        for(String where : whereSelect)
            fillWhereSelect.add(translateParam(where, params));
        fillEnv.add(env);
        return translateParam(from, params);
    }

    private Map<String, String> getTranslate(Map<Value, String> mapValues) {
        return join(reverse(params), mapValues);
    }

    // для подзапросов
    public String fillSelect(Map<K, String> fillKeySelect, Map<V, String> fillPropertySelect, Collection<String> fillWhereSelect, Map<Value, String> mapValues, ExecuteEnvironment fillEnv) {
        return fillSelect(getTranslate(mapValues), fillKeySelect, fillPropertySelect, fillWhereSelect, fillEnv);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session, QueryEnvironment queryEnv) throws SQLException {
        return session.executeSelect(select, env, getQueryParams(queryEnv), keyNames, keyReaders, propertyNames, propertyReaders);
    }

    public void outSelect(SQLSession session, QueryEnvironment env) throws SQLException {
        // выведем на экран
        OrderedMap<Map<K, Object>, Map<V, Object>> result = execute(session, env);

        for(Map.Entry<Map<K,Object>,Map<V,Object>> rowMap : result.entrySet()) {
            for(Map.Entry<K,Object> key : rowMap.getKey().entrySet()) {
                System.out.println(key.getKey()+"-"+key.getValue());
            }
            System.out.println("---- ");
            for(Map.Entry<V,Object> property : rowMap.getValue().entrySet()) {
                System.out.println(property.getKey()+"-"+property.getValue());
            }
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
