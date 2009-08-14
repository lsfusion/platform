package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.*;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.NullReader;
import platform.server.data.types.Reader;
import platform.server.data.types.TypeObject;
import platform.server.data.Table;
import platform.server.data.KeyField;
import platform.server.session.SQLSession;
import platform.server.where.Where;

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
    }

    CompiledQuery(ParsedJoinQuery<K,V> query, SQLSyntax syntax, LinkedHashMap<V,Boolean> orders, int top) {

        keySelect = new HashMap<K, String>();
        propertySelect = new HashMap<V, String>();
        whereSelect = new ArrayList<String>();
        keyOrder = new ArrayList<K>();
        propertyOrder = new ArrayList<V>();

        keyNames = new HashMap<K,String>();

        int KeyCount = 0;
        for(K Key : query.mapKeys.keySet())
            keyNames.put(Key,"jkey"+(KeyCount++));

        int propertyCount = 0;
        propertyNames = new HashMap<V, String>();
        for(Map.Entry<V, SourceExpr> property : query.properties.entrySet())
            propertyNames.put(property.getKey(),"jprop"+(propertyCount++));

        int paramCount = 0;
        params = new HashMap<ValueExpr, String>();
        for(ValueExpr mapValue : query.context.values)
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
            for(Map.Entry<V, SourceExpr> property : query.properties.entrySet())
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
                for(Map.Entry<V, SourceExpr> joinProp : query.properties.entrySet())
                    if(!orders.containsKey(joinProp.getKey()))
                        joinProp.getValue().fillJoinWheres(joinDataWheres, Where.TRUE);

                // для JoinSelect'ов узнаем при каких условиях они нужны
                MapWhere<Object> joinWheres = new MapWhere<Object>();
                for(Map.Entry<JoinData, Where> joinData : joinDataWheres.entrySet())
                    joinWheres.add(joinData.getKey().getFJGroup(),joinData.getValue());

                // сначала распихиваем JoinSelect по And'ам
                Map<Object,Collection<AndJoinQuery>> joinAnds = new HashMap<Object, Collection<AndJoinQuery>>();
                for(Map.Entry<Object, Where> joinWhere : joinWheres.entrySet())
                    joinAnds.put(joinWhere.getKey(),getWhereSubSet(andProps,joinWhere.getValue()));

                // затем все данные по JoinSelect'ам по вариантам
                int joinNum = 0;
                for(Map.Entry<JoinData, Where> joinData : joinDataWheres.entrySet()) {
                    String joinName = "join_" + (joinNum++);
                    Collection<AndJoinQuery> dataAnds = new ArrayList<AndJoinQuery>();
                    for(AndJoinQuery and : getWhereSubSet(joinAnds.get(joinData.getKey().getFJGroup()), joinData.getValue())) {
                        SourceExpr joinExpr = joinData.getKey().getFJExpr();
                        if(!and.where.means(joinExpr.getWhere().not())) { // проверим что не всегда null
                            and.properties.put(joinName, joinExpr);
                            dataAnds.add(and);
                        }
                    }
                    String joinSource = ""; // заполняем Source
                    if(dataAnds.size()==0)
                        throw new RuntimeException("Не должно быть");
                    if(dataAnds.size()==1)
                        joinSource = dataAnds.iterator().next().alias +'.'+joinName;
                    else {
                        for(AndJoinQuery and : dataAnds)
                            joinSource = (joinSource.length()==0?"":joinSource+",") + and.alias + '.' + joinName;
                        joinSource = "COALESCE(" + joinSource + ")";
                    }
                    FJSelect.joinData.put(joinData.getKey(),joinData.getKey().getFJString(joinSource));
                }

                // order'ы отдельно обрабатываем, они нужны в каждом запросе генерируем имена для Order'ов
                int io = 0;
                LinkedHashMap<String,Boolean> orderAnds = new LinkedHashMap<String, Boolean>();
                for(Map.Entry<V,Boolean> order : orders.entrySet()) {
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
                            BaseUtils.toString(andWhereSelect, " AND "), JoinQuery.stringOrder(propertyOrder,query.mapKeys.size(),orderAnds),"",top==0?"":String.valueOf(top)) + ") "+ and.alias;

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
                for(Map.Entry<V, SourceExpr> mapProp : query.properties.entrySet())
                    if(!orders.containsKey(mapProp.getKey())) // orders'ы уже обработаны
                        propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(FJSelect));
            }
        }

        select = syntax.getSelect(from, SQLSession.stringExpr(
                SQLSession.mapNames(keySelect, keyNames, keyOrder),
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)),
                BaseUtils.toString(whereSelect, " AND "), JoinQuery.stringOrder(propertyOrder,query.mapKeys.size(),orders),"",top==0?"":String.valueOf(top));

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
            for(Map.Entry<KeyExpr,ValueExpr> exprValue : innerWhere.keyValues.entrySet())
                keySelect.put((KeyExpr)exprValue.getKey(),params.get(exprValue.getValue()));

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

        abstract class JoinSelect<I extends InnerJoin> {
            final String alias;
            final String join;
            final boolean inner;

            protected abstract Map<String,AndExpr> initJoins(I innerJoin);

            public JoinSelect(I innerJoin) {
                alias = "t" + (aliasNum++);
                inner = innerWhere.means(innerJoin);

                String joinString = "";
                for(Map.Entry<String,AndExpr> keyJoin : initJoins(innerJoin).entrySet()) {
                    String keySourceString = alias + "." + keyJoin.getKey();
                    String keyJoinString = keyJoin.getValue().getSource(InnerSelect.this);
                    if(keyJoinString==null) {// значит KeyExpr которого еще не было
                        assert inner;
                        keySelect.put((KeyExpr) keyJoin.getValue(),keySourceString);
                    } else
                        joinString = (joinString.length()==0?"":joinString+" AND ") + keySourceString + "=" + keyJoinString;
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
            for(GroupJoin group : innerGroups) { // для "верхних" groups or всех выражений заведомо true
                Where groupWhere = Where.FALSE;
                for(GroupSelect.Expr expr : groups.get(group).exprs.keySet())
                    groupWhere = groupWhere.or(expr.group.getWhere());
                assert !groupWhere.isFalse();
                trueWhere = trueWhere.and(groupWhere);
            }
            return trueWhere;
        }

        public String getFrom(Where where,Collection<String> whereSelect) {
            // соответственно followFalse'им этими условиями
            where.getSource(this);// сначала надо узнать общий source чтобы заполнились дополнительные groupExpr'ы
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

        class TableSelect extends JoinSelect<Table.Join> {
            String source;

            protected Map<String, AndExpr> initJoins(Table.Join table) {
                Map<String,AndExpr> result = new HashMap<String,AndExpr>();
                for(Map.Entry<KeyField,AndExpr> expr : table.joins.entrySet())
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

        final Map<Table.Join, JoinSelect> tables = new HashMap<Table.Join, JoinSelect>();
        private String getAlias(Table.Join table) {
            JoinSelect join = tables.get(table);
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

        class GroupSelect extends JoinSelect<GroupJoin> {
            Map<String,AndExpr> group;
            final Context context;
            final Where where;

            protected Map<String, AndExpr> initJoins(GroupJoin groupJoin) {
                int keysNum = 0;
                Map<String,AndExpr> result = new HashMap<String, AndExpr>();
                group = new HashMap<String, AndExpr>();
                for(Map.Entry<AndExpr,AndExpr> join : groupJoin.group.entrySet()) {
                    String keyName = "k" + (keysNum++);
                    result.put(keyName,join.getValue());
                    group.put(keyName,join.getKey());
                }
                return result;
            }

            GroupSelect(GroupJoin groupJoin) {
                super(groupJoin);
                where = groupJoin.where;
                context = groupJoin.getContext();
            }

            final Map<Expr,String> exprs = new HashMap<Expr, String>();

            class Expr {
                SourceExpr expr;
                boolean max;

                GroupExpr group;

                Expr(SourceExpr expr, GroupExpr group) {
                    this.expr = expr;
                    this.max = group.isMax();
                    this.group = group;
                }

                @Override
                public boolean equals(Object o) {
                    return this == o || o instanceof Expr && max == ((Expr) o).max && expr.equals(((Expr) o).expr);
                }

                public int hashCode() {
                    return 31 * expr.hashCode() + (max ? 1 : 0);
                }
            }
            public String add(SourceExpr sourceExpr,GroupExpr groupExpr) {
                Expr expr = new Expr(sourceExpr,groupExpr);
                String name = exprs.get(expr);
                if(name==null) {
                    name = "e"+exprs.size();
                    exprs.put(expr,name);
                }
                return alias + "." + name;
            }

            public String getSource() {

                Where exprWhere = Where.FALSE;
                Set<SourceExpr> queryExprs = new HashSet<SourceExpr>(group.values()); // так как может одновременно и SUM и MAX нужен
                for(Expr groupExpr : exprs.keySet()) {
                    queryExprs.add(groupExpr.expr);
                    exprWhere = exprWhere.or(groupExpr.expr.getWhere());
                }

                Where fullWhere = where.and(exprWhere).and(GroupExpr.getJoinsWhere(group));

                Map<SourceExpr,String> fromPropertySelect = new HashMap<SourceExpr, String>();
                Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
                String fromSelect;
/*                if(inner)
                    fromSelect = CompiledQuery.fillAndSelect(new HashMap<KeyExpr,KeyExpr>(),getJoinWhere(),getFullWhere(),
                        BaseUtils.<Object,V,K,SourceExpr>merge(BaseUtils.merge(max,sum),groupKeys),new HashMap<KeyExpr,String>(),
                        fromPropertySelect,whereSelect,BaseUtils.crossJoin(values,params),syntax);
                else */
                fromSelect = new ParsedJoinQuery<KeyExpr,SourceExpr>(context,BaseUtils.toMap(context.keys),BaseUtils.toMap(queryExprs), fullWhere)
                    .compileSelect(syntax).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, params);

                Map<String, String> keySelect = BaseUtils.join(group,fromPropertySelect);
                Map<String,String> propertySelect = new HashMap<String, String>();
                for(Map.Entry<Expr,String> expr : exprs.entrySet())
                    propertySelect.put(expr.getValue(),(expr.getKey().max?"MAX":"SUM") + "(" + fromPropertySelect.get(expr.getKey().expr) +")");
                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect,propertySelect),
                        BaseUtils.toString(whereSelect," AND "),"",BaseUtils.toString(keySelect.values(),","),"") + ")";
            }
        }

        final Map<GroupJoin, GroupSelect> groups = new HashMap<GroupJoin, GroupSelect>();
        public String getSource(GroupExpr groupExpr) {
            GroupJoin exprJoin = groupExpr.getGroupJoin();

            KeyTranslator translator;
            for(Map.Entry<GroupJoin, GroupSelect> group : groups.entrySet())
                if((translator=exprJoin.merge(group.getKey()))!=null)
                    return group.getValue().add(groupExpr.expr.translateDirect(translator),groupExpr);

            GroupSelect group = new GroupSelect(exprJoin); // нету группы - создаем
            groups.put(exprJoin,group);
            return group.add(groupExpr.expr,groupExpr);
        }
    }

    private static <K,AV> String fillInnerSelect(Map<K, KeyExpr> mapKeys, InnerWhere innerWhere, Where where, Map<AV, SourceExpr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Collection<String> whereSelect, Map<ValueExpr,String> params, SQLSyntax syntax) {

        InnerSelect compile = new InnerSelect(innerWhere,syntax,params);
        // первым так как должны keySelect'ы и inner'ы заполнится
        for(Map.Entry<AV, SourceExpr> joinProp : compiledProps.entrySet()) // свойства
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

    public LinkedHashMap<Map<K, Object>, Map<V, Object>> executeSelect(SQLSession session,boolean outSelect) throws SQLException {

        LinkedHashMap<Map<K,Object>,Map<V,Object>> execResult = new LinkedHashMap<Map<K, Object>, Map<V, Object>>();

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
        LinkedHashMap<Map<K, Object>, Map<V, Object>> result = executeSelect(session,true);

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
            HashMap<Object,SourceExpr> UnionProps = new HashMap<Object, SourceExpr>(query.properties);
            LinkedHashMap<String,Boolean> OrderNames = new LinkedHashMap<String, Boolean>();
            int io = 0;
            for(Map.Entry<SourceExpr,Boolean> Order : query.orders.entrySet()) {
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
                String AndFrom = fillAndSelect(Query.Keys,AndWhere,UnionProps,new LinkedHashMap<SourceExpr,Boolean>(),AndKeySelect,
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
            for(V Property : query.properties.keySet())
                propertySelect.put(Property, Alias + "." + propertyNames.get(Property));

            from = "(" + from + ") "+Alias;

            select = syntax.getUnionOrder(from,JoinQuery.stringOrder(OrderNames), query.top ==0?"":String.valueOf(query.top));
            */
