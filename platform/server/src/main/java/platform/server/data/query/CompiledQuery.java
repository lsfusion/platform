package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.*;
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

    // mapValues key то что нужно, value то что сейчас
    CompiledQuery(CompiledQuery<K,V> compile,Map<ValueExpr, ValueExpr> mapValues) { // чисто для groupQuery
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = compile.keySelect;
        propertySelect = compile.propertySelect;

        select = compile.select;
        keyOrder = compile.keyOrder;
        propertyOrder = compile.propertyOrder;
        keyNames = compile.keyNames;
        keyReaders = compile.keyReaders;
        propertyNames = compile.propertyNames;
        propertyReaders = compile.propertyReaders;

        params = BaseUtils.join(mapValues,compile.params);

        assert checkQuery();
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

        params = BaseUtils.innerJoin(mapValues,compile.params); // так как map'ся весь joinQuery а при parse -> compile могут уйти

        assert checkQuery();
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
                from = fillAndSelect(query.mapKeys, innerJoin.mean, innerJoin.where, query.properties, keySelect, propertySelect, whereSelect, params, syntax);
            } else {
                // создаем And подзапросыs
                Collection<AndJoinQuery> andProps = new ArrayList<AndJoinQuery>();
                for(InnerJoins.Entry andWhere : queryJoins)
                    andProps.add(new AndJoinQuery(andWhere.mean,andWhere.where,"f"+andProps.size()));

                // сюда будут класться данные для чтения
                Map<QueryData,String> sourceFJ = new HashMap<QueryData, String>();

                // параметры
                sourceFJ.putAll(params);

                // бежим по всем property не в order'ах, определяем Join параметры с Where
                MapWhere<JoinData> joinDataWheres = new MapWhere<JoinData>();
                for(Map.Entry<V, SourceExpr> joinProp : query.properties.entrySet())
                    if(!orders.containsKey(joinProp.getKey()))
                        joinProp.getValue().fillJoinWheres(joinDataWheres, Where.TRUE);

                // для Join'ов узнаем при каких условиях они нужны
                MapWhere<Join> joinWheres = new MapWhere<Join>();
                for(Map.Entry<JoinData, Where> joinData : joinDataWheres.entrySet())
                    joinWheres.add(joinData.getKey().getJoin(),joinData.getValue());

                // сначала распихиваем Join по And'ам
                Map<Join,Collection<AndJoinQuery>> joinAnds = new HashMap<Join,Collection<AndJoinQuery>>();
                for(Map.Entry<Join, Where> joinWhere : joinWheres.entrySet())
                    joinAnds.put(joinWhere.getKey(),getWhereSubSet(andProps,joinWhere.getValue()));

                // затем все данные по Join'ам по вариантам
                int joinNum = 0;
                for(Map.Entry<JoinData, Where> joinData : joinDataWheres.entrySet()) {
                    String joinName = "join_" + (joinNum++);
                    Collection<AndJoinQuery> dataAnds = new ArrayList<AndJoinQuery>();
                    for(AndJoinQuery and : getWhereSubSet(joinAnds.get(joinData.getKey().getJoin()), joinData.getValue())) {
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
                    sourceFJ.put(joinData.getKey(),joinData.getKey().getFJString(joinSource));
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

                // бежим по всем And'ам делаем Join запросы, потом объединяем их FULL'ами
                String compileFrom = "";
                boolean second = true; // для COALESCE'ов
                for(AndJoinQuery and : andProps) {
                    // закинем в And.Properties OrderBy
                    Map<K,String> andKeySelect = new HashMap<K, String>(); Collection<String> AndWhereSelect = new ArrayList<String>();
                    Map<String,String> andPropertySelect = new HashMap<String, String>();
                    String andFrom = fillAndSelect(query.mapKeys, and.joinWhere, and.where, and.properties, andKeySelect, andPropertySelect, AndWhereSelect, params, syntax);

                    List<String> propertyOrder = new ArrayList<String>();
                    String andSelect = "(" + syntax.getSelect(andFrom, SQLSession.stringExpr(SQLSession.mapNames(andKeySelect,keyNames,new ArrayList<K>()),
                            SQLSession.mapNames(andPropertySelect,BaseUtils.toMap(andPropertySelect.keySet()),propertyOrder)),
                            SQLSession.stringWhere(AndWhereSelect), JoinQuery.stringOrder(propertyOrder,query.mapKeys.size(),orderAnds),"",top==0?"":String.valueOf(top)) + ") "+ and.alias;

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
                    sourceFJ.put(query.mapKeys.get(mapKey.getKey()),mapKey.getValue());
                }

                // закидываем PropertySelect'ы
                for(Map.Entry<V, SourceExpr> mapProp : query.properties.entrySet())
                    if(!orders.containsKey(mapProp.getKey())) // orders'ы уже обработаны
                        propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(sourceFJ, syntax));
            }
        }

        select = syntax.getSelect(from, SQLSession.stringExpr(
                SQLSession.mapNames(keySelect, keyNames, keyOrder),
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)),
                SQLSession.stringWhere(whereSelect), JoinQuery.stringOrder(propertyOrder,query.mapKeys.size(),orders),"",top==0?"":String.valueOf(top));

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

    public static <K,AV> String fillAndSelect(Map<K, KeyExpr> mapKeys, Where joinWhere, Where where, Map<AV, SourceExpr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Collection<String> whereSelect, Map<ValueExpr,String> params, SQLSyntax syntax) {

        Map<KeyExpr, ValueExpr> exprValues = new HashMap<KeyExpr, ValueExpr>();
        Map<QueryData,String> queryData = new HashMap<QueryData,String>();

        // параметры
        queryData.putAll(params);

        // бежим по JoinWhere, заполняем ExprValues, QueryData
        for(Where dataWhere : joinWhere.getOr()) {
            if(dataWhere instanceof CompareWhere) { // ничего проверять не будем, на получении InnerJoins все проверено
                CompareWhere compare = (CompareWhere)dataWhere;
                exprValues.put((KeyExpr)compare.operator1,(ValueExpr)compare.operator2);
                queryData.put((KeyExpr)compare.operator1,compare.operator2.getSource(queryData, syntax));
            }
        }

        // заполняем все необходимые Join'ы
        Context context = new Context();
        joinWhere.fillContext(context, true); // сначала fillJoins по JoinWhere чтобы Inner'ы вперед пошли
        where.fillContext(context, true); // по where запроса
        context.fill(compiledProps.values(), true); // по свойствам

        String from = "";
        for(Collection<DataJoin> level : context)
            for(DataJoin join : level)
                from = join.getFrom(from, queryData, joinWhere.means(join.getWhere()), whereSelect, exprValues, params, syntax);
        if(from.length()==0) from = "dumb";

        // ключи заполняем
        for(Map.Entry<K, KeyExpr> mapKey : mapKeys.entrySet())
            keySelect.put(mapKey.getKey(), queryData.get(mapKey.getValue()));

        assert !keySelect.containsValue(null);

        // свойства
        for(Map.Entry<AV, SourceExpr> joinProp : compiledProps.entrySet()) {
            String propertyValue = joinProp.getValue().getSource(queryData, syntax);
//            if(propertyValue.equals(SQLSyntax.NULL))
//                propertyValue = syntax.getNullValue(joinProp.getValue().getType());
            propertySelect.put(joinProp.getKey(),propertyValue);
        }
        whereSelect.add(where.followFalse(joinWhere.not()).getSource(queryData, syntax));

        return from;
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
