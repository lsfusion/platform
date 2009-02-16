package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

interface Translator {

    Where translate(JoinWhere where);
    SourceExpr translate(ObjectExpr expr);
}

class ExprTranslator implements Translator {

    private Map<JoinWhere,Where> wheres = new HashMap<JoinWhere, Where>();
//    Map<DataWhere,Where> Wheres = new HashMap<DataWhere, Where>();
    private Map<ObjectExpr,SourceExpr> exprs = new HashMap<ObjectExpr, SourceExpr>();

    void put(JoinWhere where, Where to) {
        wheres.put(where, to);
    }

    void put(ObjectExpr expr, SourceExpr to) {
        exprs.put(expr, to);
    }

    void putAll(Map<? extends ObjectExpr,? extends SourceExpr> map) {
        exprs.putAll(map);
    }

    public Where translate(JoinWhere where) {
        Where result = wheres.get(where);
        if(result==null) result = where;
        return result;
    }

    public SourceExpr translate(ObjectExpr expr) {
        SourceExpr result = exprs.get(expr);
        if(result==null) result = expr;
        return result;
    }
}

class MapWhere<T> extends HashMap<T,Where> {

    void add(T object, Where where) {
        Where InWhere = get(object);
        if(InWhere!=null)
            InWhere = InWhere.or(where);
        else
            InWhere = where;
        put(object,InWhere);
    }
}

interface QueryData {
}

interface JoinData extends QueryData {
    Join getJoin();
    SourceExpr getFJExpr();
    String getFJString(String exprFJ);
}

interface SourceJoin {

    String getSource(Map<QueryData, String> queryData, SQLSyntax syntax);

    <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values);
//    void fillJoins(List<? extends Join> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins,Where andWhere);
}

class AndJoinQuery {

    AndJoinQuery(Where iJoinWhere,Where iQueryWhere, String iAlias) {
        joinWhere = iJoinWhere;
        queryWhere = iQueryWhere;
        alias = iAlias;

        where = joinWhere.and(queryWhere);
    }

    Where joinWhere;
    Where queryWhere;
    Where where;
    String alias;
    Map<String,SourceExpr> properties = new HashMap<String, SourceExpr>();
}

// нужен для Map'а ключей / значений
class CompiledQuery<K,V> {

    String from;
    Map<K,String> keySelect;
    Map<V,String> propertySelect;
    Collection<String> whereSelect;

    String select;
    List<K> keyOrder = new ArrayList<K>();
    List<V> propertyOrder = new ArrayList<V>();
    Map<K,String> keyNames;
    Map<V,String> propertyNames;
    Map<V,Type> propertyTypes;

    Map<ValueExpr,String> params;

    CompiledQuery(CompiledQuery<K,V> compile,Map<ValueExpr,ValueExpr> mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = compile.keySelect;
        propertySelect = compile.propertySelect;

        select = compile.select;
        keyOrder = compile.keyOrder;
        propertyOrder = compile.propertyOrder;
        keyNames = compile.keyNames;
        propertyNames = compile.propertyNames;
        propertyTypes = compile.propertyTypes;

        params = BaseUtils.join(mapValues,compile.params);
    }

    // перемаппит другой CompiledQuery
    <MK,MV> CompiledQuery(CompiledQuery<MK,MV> compile,Map<K,MK> mapKeys,Map<V,MV> mapProperties,Map<ValueExpr,ValueExpr> mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = BaseUtils.join(mapKeys,compile.keySelect);
        propertySelect = BaseUtils.join(mapProperties,compile.propertySelect);

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
        propertyNames = BaseUtils.join(mapProperties,compile.propertyNames);
        propertyTypes = BaseUtils.join(mapProperties,compile.propertyTypes);

        params = BaseUtils.join(mapValues,compile.params);
    }

    CompiledQuery(CompiledJoinQuery<K,V> query, SQLSyntax syntax, LinkedHashMap<V,Boolean> orders, int top) {

        keySelect = new HashMap<K, String>();
        propertySelect = new HashMap<V, String>();
        whereSelect = new ArrayList<String>();

        keyNames = new HashMap<K,String>();

        int KeyCount = 0;
        for(K Key : query.keys.keySet())
            keyNames.put(Key,"jkey"+(KeyCount++));

        int propertyCount = 0;
        propertyNames = new HashMap<V, String>();
        propertyTypes = new HashMap<V, Type>();
        for(Map.Entry<V,SourceExpr> property : query.properties.entrySet()) {
            propertyNames.put(property.getKey(),"jprop"+(propertyCount++));
            propertyTypes.put(property.getKey(),property.getValue().getType());
        }

        int ParamCount = 0;
        params = new HashMap<ValueExpr, String>();
        Map<ValueExpr,String> queryParams = new HashMap<ValueExpr, String>();
        for(Map.Entry<ValueExpr,ValueExpr> MapValue : query.values.entrySet()) {
            String Param = "qwer" + (ParamCount++) + "ffd";
            queryParams.put(MapValue.getValue(),Param);
            params.put(MapValue.getKey(),Param);
        }

        Collection<JoinWhereEntry> queryJoins = query.where.getInnerJoins().compileMeans(); //getJoinWhere()
        if(syntax.useFJ() || queryJoins.size()==0) {
            if(queryJoins.size()==0) {
                for(K Key : query.keys.keySet())
                    keySelect.put(Key, Type.NULL);
                for(V Property : query.properties.keySet())
                    propertySelect.put(Property,Type.NULL);
                from = "empty";
            } else
            if(queryJoins.size()==1) { // "простой" запрос
                JoinWhereEntry innerJoin = queryJoins.iterator().next();
                from = fillAndSelect(query.keys, innerJoin.join, innerJoin.where, query.properties, keySelect, propertySelect, whereSelect, queryParams, syntax);
            } else {
                // создаем And подзапросыs
                Collection<AndJoinQuery> andProps = new ArrayList<AndJoinQuery>();
                for(JoinWhereEntry andWhere : queryJoins)
                    andProps.add(new AndJoinQuery(andWhere.join,andWhere.where,"f"+andProps.size()));

                // сюда будут класться данные для чтения
                Map<QueryData,String> sourceFJ = new HashMap<QueryData, String>();

                // параметры
                sourceFJ.putAll(queryParams);

                // бежим по всем property не в order'ах, определяем Join параметры с Where
                MapWhere<JoinData> joinDataWheres = new MapWhere<JoinData>();
                for(Map.Entry<V,SourceExpr> joinProp : query.properties.entrySet())
                    if(!orders.containsKey(joinProp.getKey()))
                        joinProp.getValue().fillJoinWheres(joinDataWheres,Where.TRUE);

                // группируем по Join'ам
                MapWhere<Join> joinWheres = new MapWhere<Join>();
                for(Map.Entry<JoinData,Where> joinData : joinDataWheres.entrySet())
                    joinWheres.add(joinData.getKey().getJoin(),joinData.getValue());

                // сначала распихиваем Join по And'ам
                Map<Join,Collection<AndJoinQuery>> joinAnds = new HashMap<Join,Collection<AndJoinQuery>>();
                for(Map.Entry<Join,Where> joinWhere : joinWheres.entrySet())
                    joinAnds.put(joinWhere.getKey(),getWhereSubSet(andProps,joinWhere.getValue()));

                // затем все данные по Join'ам по вариантам
                int joinNum = 0;
                for(Map.Entry<JoinData,Where> joinData : joinDataWheres.entrySet()) {
                    String joinName = "join_" + (joinNum++);
                    Collection<AndJoinQuery> dataAnds = getWhereSubSet(joinAnds.get(joinData.getKey().getJoin()), joinData.getValue());
                    for(AndJoinQuery and : dataAnds)
                        and.properties.put(joinName,joinData.getKey().getFJExpr());
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
                        and.properties.put(orderName,query.properties.get(order.getKey()));
                        orderFJ = (orderFJ.length()==0?"":orderFJ+",") + and.alias + "." + orderName;
                    }
                    orderAnds.put(orderName,order.getValue());
                    propertySelect.put(order.getKey(),"COALESCE("+orderFJ+")");
                }

                // бежим по всем And'ам делаем Join запросы, потом объединяем их FULL'ами
                from = "";
                boolean second = true; // для COALESCE'ов
                for(AndJoinQuery and : andProps) {
                    // закинем в And.Properties OrderBy
                    Map<K,String> andKeySelect = new HashMap<K, String>(); Collection<String> AndWhereSelect = new ArrayList<String>();
                    Map<String,String> andPropertySelect = new HashMap<String, String>();
                    String andFrom = fillAndSelect(query.keys, and.joinWhere, and.queryWhere, and.properties, andKeySelect, andPropertySelect, AndWhereSelect, queryParams, syntax);

                    List<String> propertyOrder = new ArrayList<String>();
                    String andSelect = "(" + syntax.getSelect(andFrom,Source.stringExpr(Source.mapNames(andKeySelect,keyNames,new ArrayList<K>()),
                            Source.mapNames(andPropertySelect,BaseUtils.toMap(andPropertySelect.keySet()),propertyOrder)),
                            Source.stringWhere(AndWhereSelect),JoinQuery.stringOrder(propertyOrder,query.keys.size(),orderAnds),"",top==0?"":String.valueOf(top)) + ") "+ and.alias;

                    if(from.length()==0) {
                        from = andSelect;
                        for(K Key : query.keys.keySet())
                            keySelect.put(Key, and.alias +"."+ keyNames.get(Key));
                    } else {
                        String andJoin = "";
                        for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
                            String andKey = and.alias + "." + keyNames.get(mapKey.getKey());
                            andJoin = (andJoin.length()==0?"":andJoin + " AND ") + andKey + "=" + (second?mapKey.getValue():"COALESCE("+mapKey.getValue()+")");
                            mapKey.setValue(mapKey.getValue()+","+andKey);
                        }
                        from = from + " FULL JOIN " + andSelect + " ON " + (andJoin.length()==0?Where.TRUE_STRING :andJoin);
                        second = false;
                    }
                }

                // полученные KeySelect'ы в Data
                for(Map.Entry<K,String> mapKey : keySelect.entrySet()) {
                    mapKey.setValue("COALESCE("+mapKey.getValue()+")"); // обернем в COALESCE
                    sourceFJ.put(query.keys.get(mapKey.getKey()),mapKey.getValue());
                }

                // закидываем PropertySelect'ы
                for(Map.Entry<V,SourceExpr> mapProp : query.properties.entrySet())
                    if(!orders.containsKey(mapProp.getKey())) { // orders'ы уже обработаны
                        String propertyValue = mapProp.getValue().getSource(sourceFJ, syntax);
                        if(propertyValue.equals(Type.NULL))
                            propertyValue = syntax.getNullValue(mapProp.getValue().getType());
                        propertySelect.put(mapProp.getKey(),propertyValue);
                    }
            }

            select = syntax.getSelect(from,Source.stringExpr(
                    Source.mapNames(keySelect, keyNames, keyOrder),
                    Source.mapNames(propertySelect, propertyNames, propertyOrder)),
                    Source.stringWhere(whereSelect),JoinQuery.stringOrder(propertyOrder,query.keys.size(),orders),"",top==0?"":String.valueOf(top));
        } else {
/*            // в Properties закинем Orders,
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
        }
    }

    // в общем случае получить AndJoinQuery под которые подходит Where
    static Collection<AndJoinQuery> getWhereSubSet(Collection<AndJoinQuery> andWheres,Where where) {

        Collection<AndJoinQuery> result = new ArrayList<AndJoinQuery>();
        Where resultWhere = Where.FALSE;
        while(result.size()< andWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery lastQuery = null;
            Where lastWhere = null;
            for(AndJoinQuery and : andWheres)
                if(!result.contains(and)) {
                    lastQuery = and;
                    lastWhere = OrWhere.op(resultWhere,lastQuery.where,true);
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
    
    static <K,AV> String fillAndSelect(Map<K, KeyExpr> mapKeys, Where joinWhere, Where queryWhere, Map<AV, SourceExpr> compiledProps, Map<K, String> keySelect, Map<AV, String> propertySelect, Collection<String> whereSelect, Map<ValueExpr,String> params, SQLSyntax syntax) {

        Map<AndExpr,ValueExpr> exprValues = new HashMap<AndExpr, ValueExpr>();
        Map<QueryData,String> queryData = new HashMap<QueryData,String>();

        // параметры
        queryData.putAll(params);

        // бежим по JoinWhere, заполняем ExprValues, QueryData
        for(OrObjectWhere dataWhere : joinWhere.getOr()) {
            if(dataWhere instanceof CompareWhere) { // ничего проверять не будем, на получении InnerJoins все проверено
                CompareWhere compare = (CompareWhere)dataWhere;
                exprValues.put((AndExpr)compare.operator1,(ValueExpr)compare.operator2);
                queryData.put((KeyExpr)compare.operator1,compare.operator2.getSource(queryData, syntax));
            }
        }

        // заполняем все необходимые Join'ы
        List<CompiledJoin> allJoins = new ArrayList<CompiledJoin>();
        // сначала fillJoins по JoinWhere чтобы Inner'ы вперед пошли
        joinWhere.fillJoins(allJoins,new HashSet<ValueExpr>());
        // по where запроса
        queryWhere.fillJoins(allJoins,new HashSet<ValueExpr>());
        // по свойствам
        for(SourceExpr property : compiledProps.values()) // здесь также надо скомпайлить чтобы не пошли лишние Joins
            property.fillJoins(allJoins, new HashSet<ValueExpr>());

        String from = "";
        for(CompiledJoin join : allJoins)
            from = join.getFrom(from, queryData, joinWhere.means(join.inJoin), whereSelect, exprValues, params, syntax);
        if(from.length()==0) from = "dumb";

        // ключи заполняем
        for(Map.Entry<K,KeyExpr> mapKey : mapKeys.entrySet())
            keySelect.put(mapKey.getKey(), queryData.get(mapKey.getValue()));

        // свойства
        for(Map.Entry<AV,SourceExpr> joinProp : compiledProps.entrySet()) {
            String propertyValue = joinProp.getValue().getSource(queryData, syntax);
            if(propertyValue.equals(Type.NULL))
                propertyValue = syntax.getNullValue(joinProp.getValue().getType());
            propertySelect.put(joinProp.getKey(),propertyValue);
        }
        whereSelect.add(queryWhere.getSource(queryData, syntax));

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
    Map<String,String> getMapValues(SQLSyntax syntax) {
        Map<String,String> result = new HashMap<String, String>();
        for(Map.Entry<ValueExpr,String> param : params.entrySet())
            result.put(param.getValue(),param.getKey().getString(syntax));
        return result;
    }

    // нужны для транслирования параметров
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

    // для update
    String fillSelect(Map<K, String> fillKeySelect, Map<V, String> fillPropertySelect, Collection<String> fillWhereSelect, SQLSyntax syntax) {
        return fillSelect(getMapValues(syntax), fillKeySelect, fillPropertySelect, fillWhereSelect);
    }

    // для выполнения в InsertSelect
    public String getSelect(SQLSyntax syntax) {
        return translateParam(select,getMapValues(syntax));
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession session,boolean outSelect) throws SQLException {
        LinkedHashMap<Map<K,Integer>,Map<V,Object>> execResult = new LinkedHashMap<Map<K, Integer>, Map<V, Object>>();
        Statement statement = session.connection.createStatement();

        try {
            String execute = getSelect(session.syntax);
            if(outSelect)
                System.out.println(execute);
            ResultSet result = statement.executeQuery(execute);
            try {
                while(result.next()) {
                    Map<K,Integer> rowKeys = new HashMap<K, Integer>();
                    for(Map.Entry<K,String> key : keyNames.entrySet())
                        rowKeys.put(key.getKey(),Type.Object.read(result.getObject(key.getValue())));
                    Map<V,Object> rowProperties = new HashMap<V, Object>();
                    for(Map.Entry<V,String> property : propertyNames.entrySet())
                        rowProperties.put(property.getKey(),
                                propertyTypes.get(property.getKey()).read(result.getObject(property.getValue())));
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

    void outSelect(DataSession Session) throws SQLException {
        // выведем на экран
        LinkedHashMap<Map<K, Integer>, Map<V, Object>> result = executeSelect(Session,true);

        for(Map.Entry<Map<K,Integer>,Map<V,Object>> rowMap : result.entrySet()) {
            for(Map.Entry<K,Integer> key : rowMap.getKey().entrySet()) {
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

// поиск в кэше
class JoinCache<K,V> {
    JoinQuery<K,V> in;

    CompiledJoinQuery<K,V> out;

    JoinCache(JoinQuery<K, V> iIn, CompiledJoinQuery<K, V> iOut) {
        in = iIn;
        out = iOut;
    }

    <CK,CV> CompiledJoinQuery<CK,CV> cache(JoinQuery<CK,CV> query) {
        Map<CK,K> mapKeys = new HashMap<CK,K>();
        Map<CV,V> mapProps = new HashMap<CV,V>();
        Map<ValueExpr,ValueExpr> mapValues = new HashMap<ValueExpr, ValueExpr>();
        if(query.equalsMap(in,mapKeys,mapProps,mapValues)) // нашли нужный кэш
            return new CompiledJoinQuery<CK,CV>(out,mapKeys,mapProps,mapValues);

        return null;
    }
}

// запрос Join
class JoinQuery<K,V> extends Source<K,V> {

    JoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);

        mapKeys = new HashMap<K, KeyExpr>();
        for(K Key : keys)
            mapKeys.put(Key,new KeyExpr());
    }
    JoinQuery(Map<K,KeyExpr> iMapKeys) {
        super(iMapKeys.keySet());
        mapKeys = iMapKeys;
    }

    Map<V,SourceExpr> properties = new HashMap<V, SourceExpr>();
    Where where = Where.TRUE;

    Map<K,KeyExpr> mapKeys;

    Collection<V> getProperties() {
        return properties.keySet();
    }

    Type getType(V Property) {
        return properties.get(Property).getType();
    }

    private List<Join> joins = null;
    Map<ValueExpr,ValueExpr> values = null;
    
    private void fillJoins() {
        if(joins==null) {
            joins = new ArrayList<Join>();
            Set<ValueExpr> joinValues = new HashSet<ValueExpr>();
            for(SourceExpr property : properties.values())
                property.fillJoins(joins, joinValues);
            where.fillJoins(joins, joinValues);

            if(values ==null) {
                for(Join join : joins)
                    joinValues.addAll(join.source.getValues().keySet());
                values = BaseUtils.toMap(joinValues);
            }
        }
    }
    // скомпилированные св-ва
    List<Join> getJoins() {
        fillJoins();
        return joins;
    }
    Map<ValueExpr,ValueExpr> getValues() {
        fillJoins();
        return values;
    }

    // перетранслирует
    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        parse().compileJoin(Join, Translated, TranslatedJoins);
    }

    static <K> String stringOrder(List<K> sources, int offset, LinkedHashMap<K,Boolean> orders) {
        String orderString = "";
        for(Map.Entry<K,Boolean> order : orders.entrySet())
            orderString = (orderString.length()==0?"":orderString+",") + (sources.indexOf(order.getKey())+offset+1) + " " + (order.getValue()?"DESC":"ASC");
        return orderString;
    }

    int hashProperty(V Property) {
        return properties.get(Property).hash();
    }

    void and(Where AddWhere) {
        where = where.and(AddWhere);
    }

    void putKeyWhere(Map<K,Integer> KeyValues) {
        for(Map.Entry<K,Integer> MapKey : KeyValues.entrySet())
            and(new CompareWhere(mapKeys.get(MapKey.getKey()),Type.Object.getExpr(MapKey.getValue()),CompareWhere.EQUALS));
    }

    void fillJoinQueries(Set<JoinQuery> Queries) {
        Queries.add(this);
        for(Join Join : getJoins())
            Join.source.fillJoinQueries(Queries);
    }

    CompiledJoinQuery<K,V> compile = null;
    CompiledJoinQuery<K,V> parse() {
//        System.out.println("compile..." + this);
//        if(1==1) return new CompiledJoinQuery<K,V>(this,Orders,SelectTop,Params);
        if(compile!=null) return compile;

        compile = getCache(this);
        if(compile !=null) {
//            System.out.println("cached");
            return compile;
        }

//      System.out.println("not cached");

        compile = new CompiledJoinQuery<K,V>(this);
        putCache(this,compile);

        return compile;
    }

    CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return parse().compileSelect(syntax);
    }
    CompiledQuery<K,V> compile(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int selectTop) {
        return parse().compileSelect(syntax,orders,selectTop);
    }

    <MK, MV> JoinQuery<K, Object> or(JoinQuery<MK, MV> toMergeQuery, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps) {

//        throw new RuntimeException("not supported");
        // результат
        JoinQuery<K,Object> mergeQuery = new JoinQuery<K,Object>(keys);

        // себя Join'им
        Join<K,Object> join = new Join<K,Object>((Source<K,Object>)this,mergeQuery);
        mergeQuery.properties.putAll(join.exprs);

        // Merge запрос Join'им, надо пересоздать объекты и построить карту
        Join<MK,MV> mergeJoin = new Join<MK,MV>(toMergeQuery,mergeQuery, mergeKeys);
        for(Map.Entry<MV,JoinExpr<MK,MV>> mergeExpr : mergeJoin.exprs.entrySet()) {
            Object mergeObject = new Object();
            mergeProps.put(mergeExpr.getKey(),mergeObject);
            mergeQuery.properties.put(mergeObject, mergeExpr.getValue());
        }

        // закинем их на Or
        mergeQuery.and(join.inJoin.or(mergeJoin.inJoin));

        CompiledJoinQuery<K, Object> parsedMerge = mergeQuery.parse();

        // погнали сливать одинаковые (именно из раных)
        for(Map.Entry<MV,Object> mergeProperty : mergeProps.entrySet()) {
            SourceExpr mergeExpr = parsedMerge.properties.get(mergeProperty.getValue());
            for(V property : properties.keySet()) {
                if(parsedMerge.properties.get(property).equals(mergeExpr)) {
                    mergeQuery.properties.remove(mergeProperty.getValue());
                    parsedMerge.properties.remove(mergeProperty.getValue());
                    mergeProperty.setValue(property);
                    break;
                }
            }
        }

//        System.out.println("ored");
        return mergeQuery;
    }

    static <V> LinkedHashMap<V,Boolean> reverseOrder(LinkedHashMap<V,Boolean> orders) {
        LinkedHashMap<V,Boolean> result = new LinkedHashMap<V, Boolean>();
        for(Map.Entry<V,Boolean> order : orders.entrySet())
            result.put(order.getKey(),!order.getValue());
        return result;
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession session) throws SQLException {
        return compile(session.syntax).executeSelect(session,false);
    }
    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        return compile(session.syntax,orders,selectTop).executeSelect(session,false);
    }

    public void outSelect(DataSession Session) throws SQLException {
        compile(Session.syntax).outSelect(Session);
    }
    public void outSelect(DataSession Session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        compile(Session.syntax,orders,selectTop).outSelect(Session);
    }

    String string = null;
    public String toString() {
        if(string==null) {
//            string = compile(Main.Adapter).getSelect(Main.Adapter);
        }
        // временно
//        return string;

        return "JQ";
    }

    // для кэша - возвращает выражения + where чтобы потом еще Orders сравнить
    <EK,EV> boolean equals(JoinQuery<EK,EV> query,Map<K,EK> equalKeys,Map<V,EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        if(this== query) {
            if(BaseUtils.identity(equalKeys) && BaseUtils.identity(mapValues)) {
                for(V property : properties.keySet())
                    mapProperties.put(property, (EV) property);
                return true;
            } else
                return false;
        }

        if(properties.size()!= query.properties.size()) return false;

        mapValues = BaseUtils.crossJoin(getValues(), query.getValues(), mapValues);
        if(mapValues.values().contains(null)) return false;

        Map<ObjectExpr,ObjectExpr> mapExprs = new HashMap<ObjectExpr,ObjectExpr>(mapValues);
        mapExprs.putAll(BaseUtils.crossJoin(mapKeys, query.mapKeys, equalKeys));

        // бежим по всем Join'ам, пытаемся промаппить их друг на друга
        List<Join> joins = getJoins();
        List<Join> queryJoins = query.getJoins();

        if(joins.size()!=queryJoins.size())
            return false;

        // строим hash
        Map<Integer,Collection<Join>> hashQueryJoins = new HashMap<Integer,Collection<Join>>();
        for(Join join : queryJoins) {
            Integer hash = join.hash();
            Collection<Join> hashJoins = hashQueryJoins.get(hash);
            if(hashJoins==null) {
                hashJoins = new ArrayList<Join>();
                hashQueryJoins.put(hash,hashJoins);
            }
            hashJoins.add(join);
        }

        Map<JoinWhere,JoinWhere> mapWheres = new HashMap<JoinWhere,JoinWhere>();
        // бежим
        for(Join join : joins) {
            Collection<Join> hashJoins = hashQueryJoins.get(join.hash());
            if(hashJoins==null) return false;
            boolean found = false;
            Iterator<Join> i = hashJoins.iterator();
            while(i.hasNext())
                if(join.equals(i.next(), mapValues, mapExprs, mapWheres)) {i.remove(); found = true; break;}
            if(!found) return false;
        }

        if(!where.equals(query.where,mapExprs,mapWheres)) return false;

        // свойства проверим
        Map<EV,V> queryMap = new HashMap<EV, V>();
        for(Map.Entry<V,SourceExpr> mapProperty : properties.entrySet()) {
            EV mapQuery = null;
            for(Map.Entry<EV,SourceExpr> mapQueryProperty : query.properties.entrySet())
                if(!queryMap.containsKey(mapQueryProperty.getKey()) &&
                    mapProperty.getValue().equals(mapQueryProperty.getValue(),mapExprs, mapWheres)) {
                    mapQuery = mapQueryProperty.getKey();
                    break;
                }
            if(mapQuery==null) return false;
            queryMap.put(mapQuery,mapProperty.getKey());
        }

        // составляем карту возврата
        for(Map.Entry<EV,V> mapQuery : queryMap.entrySet())
            mapProperties.put(mapQuery.getValue(),mapQuery.getKey());
        return true;
    }

    class EqualCache<EK,EV> {
        JoinQuery<EK,EV> query;
        Map<K,EK> mapKeys;
        Map<ValueExpr,ValueExpr> mapValues;

        EqualCache(JoinQuery<EK, EV> iQuery, Map<K, EK> iMapKeys, Map<ValueExpr, ValueExpr> iMapValues) {
            query = iQuery;
            mapKeys = iMapKeys;
            mapValues = iMapValues;
        }

        public boolean equals(Object o) {
            return this==o || (o instanceof EqualCache && query.equals(((EqualCache)o).query) && mapKeys.equals(((EqualCache)o).mapKeys) && mapValues.equals(((EqualCache)o).mapValues));
        }

        public int hashCode() {
            return mapValues.hashCode()*31*31+ mapKeys.hashCode()*31+ query.hashCode();
        }
    }
    
    // кэш для сравнений и метод собсно тоже для кэша
    Map<EqualCache,Map<V,?>> cacheEquals = new HashMap<EqualCache, Map<V,?>>();
    <EK,EV> boolean equals(Source<EK,EV> source,Map<K,EK> equalKeys,Map<V,EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        if(!(source instanceof JoinQuery)) return false;

        JoinQuery<EK,EV> query = (JoinQuery<EK,EV>) source;
        
        EqualCache<EK,EV> equalCache = new EqualCache<EK,EV>(query, equalKeys, mapValues);
        Map<V,EV> equalMap = (Map<V,EV>) cacheEquals.get(equalCache);
        if(equalMap==null) {
            equalMap = new HashMap<V, EV>();
            if(!equals(query, equalKeys, equalMap, mapValues))
                return false;
            cacheEquals.put(equalCache,equalMap);
        }

        mapProperties.putAll(equalMap);
        return true;
    }

    int getHash() {
        // должны совпасть hash'и properties и hash'и wheres
        int hash = 0;
        for(SourceExpr Property : properties.values())
            hash += Property.hash();
        return where.hash()*31+hash;
    }

    static Map<Integer,Collection<JoinCache>> сacheCompile = new HashMap<Integer, Collection<JoinCache>>();
    static <K,V> CompiledJoinQuery<K,V> getCache(JoinQuery<K,V> query) {
        Collection<JoinCache> hashCaches = сacheCompile.get(query.hash());
        if(hashCaches==null) return null;
        for(JoinCache<?,?> cache : hashCaches) {
            CompiledJoinQuery<K,V> result = cache.cache(query);
            if(result!=null) return result;
        }
        return null;
    }
    static <K,V> void putCache(JoinQuery<K,V> query,CompiledJoinQuery<K,V> compiled) {
        Collection<JoinCache> hashCaches = сacheCompile.get(query.hash());
        if(hashCaches==null) {
            hashCaches = new ArrayList<JoinCache>();
            сacheCompile.put(query.hash(),hashCaches);
        }
        hashCaches.add(new JoinCache<K,V>(query,compiled));
    }

    <EK,EV> boolean equalsMap(JoinQuery<EK,EV> query,Map<K,EK> equalKeys,Map<V,EV> equalProperties, Map<ValueExpr, ValueExpr> equalValues) {

        // переберем Values, Keys
        Collection<Map<K,EK>> keyMaps = MapBuilder.buildPairs(keys, query.keys);
        if(keyMaps==null) return false;

        Collection<Map<ValueExpr,ValueExpr>> valueMaps = MapBuilder.buildPairs(getValues().keySet(), query.getValues().keySet());
        if(valueMaps==null) return false;

        for(Map<ValueExpr,ValueExpr> valueMap : valueMaps)
            for(Map<K,EK> mapKeys : keyMaps)
                if(equals(query,mapKeys,equalProperties,valueMap)) {
                    equalKeys.putAll(mapKeys);
                    equalValues.putAll(valueMap);
                    return true;
                }
        return false;
    }

    // конструктор копирования
    JoinQuery(JoinQuery<K,V> query) {
        this(query.mapKeys);
        properties.putAll(query.properties);
        where = query.where;
        values = query.values;
    }

    // конструктор фиксации переменных
    JoinQuery(Map<? extends V, ValueExpr> propertyValues,JoinQuery<K,V> query) {
        this(query.keys);

        Join<K,V> sourceJoin = new Join<K,V>(query,this);
        where = sourceJoin.inJoin;

        // раскидываем по dumb'ам или на выход
        for(Map.Entry<V,JoinExpr<K,V>> property : sourceJoin.exprs.entrySet()) {
            ValueExpr propertyValue = propertyValues.get(property.getKey());
            if(propertyValue==null)
                properties.put(property.getKey(), property.getValue());
            else
                where = where.and(new CompareWhere(property.getValue(),propertyValue,CompareWhere.EQUALS));
        }
    }

    // конструктор трансляции переменных
    JoinQuery(JoinQuery<K,V> query,Map<ValueExpr, ValueExpr> mapValues) {
        this(query.mapKeys);

        properties = query.properties;
        where = query.where;
        values = BaseUtils.join(mapValues, query.getValues());

        if(query.compile !=null)
            compile = new CompiledJoinQuery<K,V>(query.compile, mapValues);
    }
}

class CompiledJoinQuery<K,V> {

    Map<V,SourceExpr> properties;
    Where where;

    Map<K,KeyExpr> keys;

    List<CompiledJoin> joins;
    Map<ValueExpr,ValueExpr> values;

    private CompiledJoinQuery(Map<K,KeyExpr> iKeys) {
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
        for(Map.Entry<V,SourceExpr> MapProperty : query.properties.entrySet())
            properties.put(MapProperty.getKey(),MapProperty.getValue().translate(translated).followFalse(where.not()));
        // Values
        values = query.getValues();

        compiler = new Compiler();
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
        for(Map.Entry<K,KeyExpr> mapKey : keys.entrySet())
            joinTranslated.put(mapKey.getValue(), join.joins.get(mapKey.getKey()).translate(translated));

        joinTranslated.putAll(BaseUtils.reverse(values));

        // рекурсивно погнали остальные JoinQuery, здесь уже DataSource'ы причем без CaseExpr'ов
        for(CompiledJoin compileJoin : joins) // здесь по сути надо перетранслировать ValueExpr'ы а также в GroupQuery перебить JoinQuery на новые Values
            compileJoin.translate(joinTranslated, translatedJoins, compileJoin.getDataSource().translateValues(values));

        Where translatedWhere = where.translate(joinTranslated);
        translated.put(join.inJoin, translatedWhere);
        for(Map.Entry<V,SourceExpr> mapProperty : properties.entrySet())
            translated.put(join.exprs.get(mapProperty.getKey()), mapProperty.getValue().translate(joinTranslated).and(translatedWhere));
    }

    private class Compiler {
        CompiledQuery<K,V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(CompiledJoinQuery.this, syntax, orders, top);
        }
    }

    private class MapCompiler<MK,MV> extends Compiler {
        CompiledJoinQuery<MK,MV> mapQuery;
        Map<K,MK> mapKeys;
        Map<V,MV> mapProps;
        Map<ValueExpr,ValueExpr> mapValues;

        private MapCompiler(CompiledJoinQuery<MK, MV> iMapQuery, Map<K, MK> iMapKeys, Map<V, MV> iMapProps, Map<ValueExpr, ValueExpr> iMapValues) {
            mapQuery = iMapQuery;
            mapKeys = iMapKeys;
            mapProps = iMapProps;
            mapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(mapQuery.compileSelect(syntax,BaseUtils.linkedJoin(orders,mapProps),top), mapKeys, mapProps, mapValues);
        }
    }
    <MK,MV> CompiledJoinQuery(CompiledJoinQuery<MK,MV> query, Map<K, MK> mapKeys, Map<V, MV> mapProps, Map<ValueExpr,ValueExpr> mapValues) {
        this(BaseUtils.join(mapKeys, query.keys));
        properties = BaseUtils.join(mapProps, query.properties);
        where = query.where;
        joins = query.joins;
        values = BaseUtils.join(mapValues, query.values);

        compiler = new MapCompiler<MK,MV>(query, mapKeys, mapProps, mapValues);
    }
    
    private class ValueCompiler extends Compiler {
        CompiledJoinQuery<K,V> mapQuery;
        Map<ValueExpr,ValueExpr> mapValues;

        private ValueCompiler(CompiledJoinQuery<K, V> iMapQuery, Map<ValueExpr, ValueExpr> iMapValues) {
            mapQuery = iMapQuery;
            mapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
            return new CompiledQuery<K,V>(mapQuery.compileSelect(syntax,orders,top), mapValues);
        }
    }
    CompiledJoinQuery(CompiledJoinQuery<K,V> Query, Map<ValueExpr,ValueExpr> MapValues) {
        this(Query.keys);
        properties = Query.properties;
        where = Query.where;
        joins = Query.joins;
        values = BaseUtils.join(MapValues,Query.values);

        compiler = new ValueCompiler(Query,MapValues);
    }

}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends JoinQuery<K,V> {

    UnionQuery(Collection<? extends K> iKeys, int iDefaultOperator) {
        super(iKeys);
        DefaultOperator = iDefaultOperator;
        where = Where.FALSE;
    }

    // как в List 0 - MAX, 1 - SUM, 2 - NVL, плюс 3 - если есть в Source
    int DefaultOperator;

    static SourceExpr getExpr(List<SourceExpr> Operands,int Operator) {
        SourceExpr Result = null;
        for(SourceExpr Operand : Operands)
            Result = getUnionExpr(Result,Operand,Operator);
        return Result;
    }

    static SourceExpr getUnionExpr(SourceExpr PrevExpr, SourceExpr Expr, int Operator) {
        if(PrevExpr==null) return Expr;

        SourceExpr Result;
        switch(Operator) {
            case 0: // MAX CE(New.notNull AND !(Prev>New),New,Prev)
                Result = new CaseExpr(new CompareWhere(PrevExpr,Expr,CompareWhere.GREATER).or(Expr.getWhere().not()),PrevExpr,Expr);
                break;
            case 1: // SUM CE(Prev.null,New,New.null,Prev,true,New+Prev)
//                Result = new CaseExpr(Expr.getWhere().not(),PrevExpr,new CaseExpr(PrevExpr.getWhere().not(),Expr,new FormulaExpr(PrevExpr,Expr,true)));
                Result = new LinearExpr(Expr,PrevExpr,true);
                break;
            case 2: // NVL CE(New.notNull,New,Prev)
                Result = new CaseExpr(Expr.getWhere(),Expr,PrevExpr);
                break;
            default:
                throw new RuntimeException("не может быть такого оператора");
        }
        return Result;
    }

    // по идее должно быть полиморфным
    SourceExpr getUnionExpr(SourceExpr PrevExpr, SourceExpr Expr, Integer Coeff, JoinWhere InJoin) {

        if(!Coeff.equals(1)) Expr = new LinearExpr(Expr,Coeff);
        if(PrevExpr==null) return Expr;
        
        if(DefaultOperator>2)
            return new CaseExpr(InJoin,Expr,PrevExpr);
        else
            return getUnionExpr(PrevExpr,Expr,DefaultOperator);
    }

    // добавляем на OR запрос
    void add(Source<? extends K,V> Source,Integer Coeff) {

        Join<K,V> Join = new Join<K,V>((Source<K,V>) Source,this);
        for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.exprs.entrySet())
            properties.put(MapExpr.getKey(), getUnionExpr(properties.get(MapExpr.getKey()),MapExpr.getValue(),Coeff, Join.inJoin));
        where = where.or(Join.inJoin);
    }
}