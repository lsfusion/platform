package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

interface Translator {

    Where translate(JoinWhere Where);
    SourceExpr translate(ObjectExpr Expr);
}

class ExprTranslator implements Translator {

    private Map<JoinWhere,Where> Wheres = new HashMap<JoinWhere, Where>();
//    Map<DataWhere,Where> Wheres = new HashMap<DataWhere, Where>();
    private Map<ObjectExpr,SourceExpr> Exprs = new HashMap<ObjectExpr, SourceExpr>();

    void put(JoinWhere Where, Where To) {
        Wheres.put(Where,To);
    }

    void put(ObjectExpr Expr, SourceExpr To) {
        Exprs.put(Expr,To);
    }

    void putAll(Map<? extends ObjectExpr,? extends SourceExpr> Map) {
        Exprs.putAll(Map);
    }

    public Where translate(JoinWhere Where) {
        Where Result = Wheres.get(Where);
        if(Result==null) Result = Where;
        return Result;
    }

    public SourceExpr translate(ObjectExpr Expr) {
        SourceExpr Result = Exprs.get(Expr);
        if(Result==null) Result = Expr;
        return Result;
    }
}

class MapWhere<T> extends HashMap<T,Where> {

    void add(T Object, Where Where) {
        Where InWhere = get(Object);
        if(InWhere!=null)
            InWhere = InWhere.or(Where);
        else
            InWhere = Where;
        put(Object,InWhere);
    }
}

interface QueryData {
}

interface JoinData extends QueryData {
    Join getJoin();
    SourceExpr getFJExpr();
    String getFJString(String FJExpr);
}

interface SourceJoin {

    String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax);

    <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values);
//    void fillJoins(List<? extends Join> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins,Where andWhere);
}

class AndJoinQuery {

    AndJoinQuery(Where iJoinWhere,Where iQueryWhere, String iAlias) {
        JoinWhere = iJoinWhere;
        QueryWhere = iQueryWhere;
        Alias = iAlias;

        Where = JoinWhere.and(QueryWhere);
    }

    Where JoinWhere;
    Where QueryWhere;
    Where Where;
    String Alias;
    Map<String,SourceExpr> Properties = new HashMap<String, SourceExpr>();
}

// нужен для Map'а ключей / значений
class CompiledQuery<K,V> {

    String From;
    Map<K,String> KeySelect;
    Map<V,String> PropertySelect;
    Collection<String> WhereSelect;

    String Select;
    List<K> KeyOrder = new ArrayList<K>();
    List<V> PropertyOrder = new ArrayList<V>();
    Map<K,String> KeyNames;
    Map<V,String> PropertyNames;
    Map<V,Type> PropertyTypes;

    Map<ValueExpr,String> Params;

    CompiledQuery(CompiledQuery<K,V> Compile,Map<ValueExpr,ValueExpr> MapValues) {
        From = Compile.From;
        WhereSelect = Compile.WhereSelect;
        KeySelect = Compile.KeySelect;
        PropertySelect = Compile.PropertySelect;

        Select = Compile.Select;
        KeyOrder = Compile.KeyOrder;
        PropertyOrder = Compile.PropertyOrder;
        KeyNames = Compile.KeyNames;
        PropertyNames = Compile.PropertyNames;
        PropertyTypes = Compile.PropertyTypes;

        Params = BaseUtils.join(MapValues,Compile.Params);
    }

    // перемаппит другой CompiledQuery
    <MK,MV> CompiledQuery(CompiledQuery<MK,MV> Compile,Map<K,MK> MapKeys,Map<V,MV> MapProperties,Map<ValueExpr,ValueExpr> MapValues) {
        From = Compile.From;
        WhereSelect = Compile.WhereSelect;
        KeySelect = BaseUtils.join(MapKeys,Compile.KeySelect);
        PropertySelect = BaseUtils.join(MapProperties,Compile.PropertySelect);

        Select = Compile.Select;
        for(MK Key : Compile.KeyOrder)
            for(Map.Entry<K,MK> MapKey : MapKeys.entrySet())
                if(MapKey.getValue()==Key) {KeyOrder.add(MapKey.getKey()); break;}
        for(MV Property : Compile.PropertyOrder)
            for(Map.Entry<V,MV> MapProperty : MapProperties.entrySet())
                if(MapProperty.getValue()==Property) {PropertyOrder.add(MapProperty.getKey()); break;}
        KeyNames = BaseUtils.join(MapKeys,Compile.KeyNames);
        PropertyNames = BaseUtils.join(MapProperties,Compile.PropertyNames);
        PropertyTypes = BaseUtils.join(MapProperties,Compile.PropertyTypes);

        Params = BaseUtils.join(MapValues,Compile.Params);
    }

    CompiledQuery(CompiledJoinQuery<K,V> Query, SQLSyntax Syntax) {

        KeySelect = new HashMap<K, String>();
        PropertySelect = new HashMap<V, String>();
        WhereSelect = new ArrayList<String>();

        KeyNames = new HashMap<K,String>();

        int KeyCount = 0;
        for(K Key : Query.Keys.keySet())
            KeyNames.put(Key,"jkey"+(KeyCount++));

        int PropertyCount = 0;
        PropertyNames = new HashMap<V, String>();
        PropertyTypes = new HashMap<V, Type>();
        for(Map.Entry<V,SourceExpr> Property : Query.properties.entrySet()) {
            PropertyNames.put(Property.getKey(),"jprop"+(PropertyCount++));
            PropertyTypes.put(Property.getKey(),Property.getValue().getType());
        }

        int ParamCount = 0;
        Params = new HashMap<ValueExpr, String>();
        Map<ValueExpr,String> QueryParams = new HashMap<ValueExpr, String>();
        for(Map.Entry<ValueExpr,ValueExpr> MapValue : Query.Values.entrySet()) {
            String Param = "qwer" + (ParamCount++) + "ffd";
            QueryParams.put(MapValue.getValue(),Param);
            Params.put(MapValue.getKey(),Param);
        }

        JoinWheres QueryJoins = Query.Where.getInnerJoins(); //getJoinWhere()
        if(Syntax.useFJ() || QueryJoins.size()==0) {
            LinkedHashMap<String,Boolean> OrderSelect = new LinkedHashMap<String,Boolean>();

            if(QueryJoins.size()==0) {
                for(K Key : Query.Keys.keySet())
                    KeySelect.put(Key, Type.NULL);
                for(V Property : Query.properties.keySet())
                    PropertySelect.put(Property,Type.NULL);
                From = "empty";
            } else
            if(QueryJoins.size()==1) { // "простой" запрос
                Map.Entry<Where,Where> InnerJoin = QueryJoins.entrySet().iterator().next();
                From = fillAndSelect(Query.Keys, InnerJoin.getKey(), InnerJoin.getValue(), Query.properties, Query.Orders, KeySelect, PropertySelect, WhereSelect, QueryParams, OrderSelect, Syntax);
            } else {
                // создаем And подзапросыs
                Collection<AndJoinQuery> AndProps = new ArrayList<AndJoinQuery>();
                for(Map.Entry<Where,Where> AndWhere : QueryJoins.entrySet())
                    AndProps.add(new AndJoinQuery(AndWhere.getKey(),AndWhere.getValue(),"f"+AndProps.size()));

                // сюда будут класться данные для чтения
                Map<QueryData,String> FJSource = new HashMap<QueryData, String>();

                // параметры
                FJSource.putAll(QueryParams);

                // бежим по всем property, определяем Join параметры с Where
                MapWhere<JoinData> JoinDataWheres = new MapWhere<JoinData>();
                for(Map.Entry<V,SourceExpr> JoinProp : Query.properties.entrySet())
                    JoinProp.getValue().fillJoinWheres(JoinDataWheres,Where.TRUE);

                // группируем по Join'ам
                MapWhere<Join> JoinWheres = new MapWhere<Join>();
                for(Map.Entry<JoinData,Where> JoinData : JoinDataWheres.entrySet())
                    JoinWheres.add(JoinData.getKey().getJoin(),JoinData.getValue());

                // сначала распихиваем Join по And'ам
                Map<Join,Collection<AndJoinQuery>> JoinAnds = new HashMap<Join,Collection<AndJoinQuery>>();
                for(Map.Entry<Join,Where> JoinWhere : JoinWheres.entrySet())
                    JoinAnds.put(JoinWhere.getKey(),getWhereSubSet(AndProps,JoinWhere.getValue()));

                // затем все данные по Join'ам по вариантам
                int JoinNum = 0;
                for(Map.Entry<JoinData,Where> JoinData : JoinDataWheres.entrySet()) {
                    String JoinName = "join_" + (JoinNum++);
                    Collection<AndJoinQuery> DataAnds = getWhereSubSet(JoinAnds.get(JoinData.getKey().getJoin()), JoinData.getValue());
                    for(AndJoinQuery And : DataAnds)
                        And.Properties.put(JoinName,JoinData.getKey().getFJExpr());
                    String JoinSource = ""; // заполняем Source
                    if(DataAnds.size()==0)
                        throw new RuntimeException("Не должно быть");
                    if(DataAnds.size()==1)
                        JoinSource = DataAnds.iterator().next().Alias+'.'+JoinName;
                    else {
                        for(AndJoinQuery And : DataAnds)
                            JoinSource = (JoinSource.length()==0?"":JoinSource+",") + And.Alias + '.' + JoinName;
                        JoinSource = "COALESCE(" + JoinSource + ")";
                    }
                    FJSource.put(JoinData.getKey(),JoinData.getKey().getFJString(JoinSource));
                }

                // генерируем имена для Order'ов
                int io = 0;
                for(Map.Entry<SourceExpr,Boolean> Order : Query.Orders.entrySet()) {
                    String OrderName = "order_" + (io++);
                    String FJOrder = "";
                    for(AndJoinQuery And : AndProps) {
                        And.Properties.put(OrderName,Order.getKey());
                        FJOrder = (FJOrder.length()==0?"":FJOrder+",") + And.Alias + "." + OrderName;
                    }
                    OrderSelect.put("COALESCE("+FJOrder+")",Order.getValue());
                }

                // бежим по всем And'ам делаем Join запросы, потом объединяем их FULL'ами
                From = "";
                boolean Second = true; // для COALESCE'ов
                for(AndJoinQuery And : AndProps) {
                    // закинем в And.Properties OrderBy
                    Map<K,String> AndKeySelect = new HashMap<K, String>(); Collection<String> AndWhereSelect = new ArrayList<String>();
                    LinkedHashMap<String,String> AndPropertySelect = new LinkedHashMap<String, String>();
                    LinkedHashMap<String,Boolean> AndOrderSelect = new LinkedHashMap<String, Boolean>();
                    String AndFrom = fillAndSelect(Query.Keys, And.JoinWhere, And.QueryWhere, And.Properties, Query.Orders, AndKeySelect, AndPropertySelect, AndWhereSelect, QueryParams, AndOrderSelect, Syntax);

                    String AndSelect = "(" + Syntax.getSelect(AndFrom,Source.stringExpr(Source.mapNames(AndKeySelect,KeyNames,new ArrayList<K>()),AndPropertySelect),
                            Source.stringWhere(AndWhereSelect),JoinQuery.stringOrder(AndOrderSelect),"", Query.Top==0?"":String.valueOf(Query.Top)) + ") "+And.Alias;

                    if(From.length()==0) {
                        From = AndSelect;
                        for(K Key : Query.Keys.keySet())
                            KeySelect.put(Key,And.Alias+"."+KeyNames.get(Key));
                    } else {
                        String AndJoin = "";
                        for(Map.Entry<K,String> MapKey : KeySelect.entrySet()) {
                            String AndKey = And.Alias + "." + KeyNames.get(MapKey.getKey());
                            AndJoin = (AndJoin.length()==0?"":AndJoin + " AND ") + AndKey + "=" + (Second?MapKey.getValue():"COALESCE("+MapKey.getValue()+")");
                            MapKey.setValue(MapKey.getValue()+","+AndKey);
                        }
                        From = From + " FULL JOIN " + AndSelect + " ON " + (AndJoin.length()==0?Where.TRUE_STRING :AndJoin);
                        Second = false;
                    }
                }

                // полученные KeySelect'ы в Data
                for(Map.Entry<K,String> MapKey : KeySelect.entrySet()) {
                    MapKey.setValue("COALESCE("+MapKey.getValue()+")"); // обернем в COALESCE
                    FJSource.put(Query.Keys.get(MapKey.getKey()),MapKey.getValue());
                }

                // закидываем PropertySelect'ы
                for(Map.Entry<V,SourceExpr> MapProp : Query.properties.entrySet()) {
                    String PropertyValue = MapProp.getValue().getSource(FJSource, Syntax);
                    if(PropertyValue.equals(Type.NULL))
                        PropertyValue = Syntax.getNullValue(MapProp.getValue().getType());
                    PropertySelect.put(MapProp.getKey(),PropertyValue);
                }
            }

            Select = Syntax.getSelect(From,Source.stringExpr(
                    Source.mapNames(KeySelect,KeyNames,KeyOrder),
                    Source.mapNames(PropertySelect,PropertyNames,PropertyOrder)),
                    Source.stringWhere(WhereSelect),JoinQuery.stringOrder(OrderSelect),"",Query.Top==0?"":String.valueOf(Query.Top));
        } else {
            // в Properties закинем Orders,
            HashMap<Object,SourceExpr> UnionProps = new HashMap<Object, SourceExpr>(Query.properties);
            LinkedHashMap<String,Boolean> OrderNames = new LinkedHashMap<String, Boolean>();
            int io = 0;
            for(Map.Entry<SourceExpr,Boolean> Order : Query.Orders.entrySet()) {
                String OrderName = "order_"+io;
                UnionProps.put(OrderName,Order.getKey());
                OrderNames.put(OrderName,Order.getValue());
            }

/*            From = "";
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
  */
            String Alias = "G";
            for(K Key : Query.Keys.keySet())
                KeySelect.put(Key, Alias + "." + KeyNames.get(Key));
            for(V Property : Query.properties.keySet())
                PropertySelect.put(Property, Alias + "." + PropertyNames.get(Property));

            From = "(" + From + ") "+Alias;

            Select = Syntax.getUnionOrder(From,JoinQuery.stringOrder(OrderNames),Query.Top==0?"":String.valueOf(Query.Top));
        }
    }

    // в общем случае получить AndJoinQuery под которые подходит Where
    static Collection<AndJoinQuery> getWhereSubSet(Collection<AndJoinQuery> AndWheres,Where Where) {

        Collection<AndJoinQuery> Result = new ArrayList<AndJoinQuery>();
        Where ResultWhere = Where.FALSE;
        while(Result.size()<AndWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery LastQuery = null;
            Where LastWhere = null;
            for(AndJoinQuery And : AndWheres)
                if(!Result.contains(And)) {
                    LastQuery = And;
                    LastWhere = ResultWhere.or(LastQuery.Where);
                    if(Where.means(LastWhere)) {
                        Result.add(LastQuery);
                        return Result;
                    }
                }
            ResultWhere = LastWhere;
            Result.add(LastQuery);
        }
        return Result;
    }
    
    static <K,AV> String fillAndSelect(Map<K, KeyExpr> MapKeys, Where JoinWhere, Where QueryWhere, Map<AV, SourceExpr> CompiledProps, LinkedHashMap<SourceExpr, Boolean> CompiledOrders, Map<K, String> KeySelect, Map<AV, String> PropertySelect, Collection<String> WhereSelect, Map<ValueExpr,String> Params, LinkedHashMap<String, Boolean> OrderSelect, SQLSyntax Syntax) {

        Map<AndExpr,ValueExpr> ExprValues = new HashMap<AndExpr, ValueExpr>();
        Map<QueryData,String> QueryData = new HashMap<QueryData,String>();

        // параметры
        QueryData.putAll(Params);

        // бежим по JoinWhere, заполняем ExprValues, QueryData
        for(OrObjectWhere DataWhere : JoinWhere.getOr()) {
            if(DataWhere instanceof CompareWhere) { // ничего проверять не будем, на получении InnerJoins все проверено
                CompareWhere Compare = (CompareWhere)DataWhere;
                ExprValues.put((AndExpr)Compare.Operator1,(ValueExpr)Compare.Operator2);
                QueryData.put((KeyExpr)Compare.Operator1,Compare.Operator2.getSource(QueryData,Syntax));
            }
        }

        // заполняем все необходимые Join'ы
        List<CompiledJoin> AllJoins = new ArrayList<CompiledJoin>();
        // сначала fillJoins по JoinWhere чтобы Inner'ы вперед пошли
        JoinWhere.fillJoins(AllJoins,new HashSet<ValueExpr>());
        // по where запроса
        QueryWhere.fillJoins(AllJoins,new HashSet<ValueExpr>());
        // по свойствам
        for(SourceExpr Property : CompiledProps.values()) // здесь также надо скомпайлить чтобы не пошли лишние Joins
            Property.fillJoins(AllJoins, new HashSet<ValueExpr>());

        String From = "";
        for(CompiledJoin Join : AllJoins)
            From = Join.getFrom(From,QueryData,JoinWhere.means(Join.inJoin),WhereSelect,ExprValues,Params,Syntax);
        if(From.length()==0) From = "dumb";

        // ключи заполняем
        for(Map.Entry<K,KeyExpr> MapKey : MapKeys.entrySet())
            KeySelect.put(MapKey.getKey(),QueryData.get(MapKey.getValue()));

        // свойства
        for(Map.Entry<AV,SourceExpr> JoinProp : CompiledProps.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(QueryData, Syntax);
            if(PropertyValue.equals(Type.NULL))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getType());
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        // порядки
        for(Map.Entry<SourceExpr,Boolean> Order : CompiledOrders.entrySet())
            if(!(Order.getKey() instanceof ValueExpr) && !(Order.getKey() instanceof AndExpr && ExprValues.containsKey((AndExpr)Order.getKey()))) // некоторые СУБД не любят ORDER BY по константам
                OrderSelect.put(Order.getKey().getSource(QueryData, Syntax),Order.getValue());

        WhereSelect.add(QueryWhere.getSource(QueryData, Syntax));

        return From;
    }
    
    String translateParam(String Query,Map<String,String> ParamValues) {
        Map<String,String> TranslateMap = new HashMap<String, String>();
        int ParamNum = 0;
        for(Map.Entry<String,String> ParamValue : ParamValues.entrySet()) {
            String Translate = "transp" + (ParamNum++) + "nt";
            Query = Query.replaceAll(ParamValue.getKey(),Translate);
            TranslateMap.put(Translate,ParamValue.getValue());
        }
        for(Map.Entry<String,String> TranslateValue : TranslateMap.entrySet())
            Query = Query.replaceAll(TranslateValue.getKey(),TranslateValue.getValue());        
        return Query;
    }
    Map<String,String> getMapValues(SQLSyntax Syntax) {
        Map<String,String> Result = new HashMap<String, String>();
        for(Map.Entry<ValueExpr,String> Param : Params.entrySet())
            Result.put(Param.getValue(),Param.getKey().getString(Syntax));
        return Result;
    }

    // нужны для транслирования параметров
    private String fillSelect(Map<String,String> Params, Map<K, String> FillKeySelect, Map<V, String> FillPropertySelect, Collection<String> FillWhereSelect) {
        for(Map.Entry<K,String> MapKey : KeySelect.entrySet())
            FillKeySelect.put(MapKey.getKey(),translateParam(MapKey.getValue(),Params));
        for(Map.Entry<V,String> MapProp : PropertySelect.entrySet())
            FillPropertySelect.put(MapProp.getKey(),translateParam(MapProp.getValue(),Params));
        for(String Where : WhereSelect)
            FillWhereSelect.add(translateParam(Where,Params));
        return translateParam(From,Params);
    }

    // для GroupQuery
    String fillSelect(Map<K, String> FillKeySelect, Map<V, String> FillPropertySelect, Collection<String> FillWhereSelect, Map<ValueExpr,String> MapValues) {
        return fillSelect(BaseUtils.join(BaseUtils.reverse(Params), MapValues),FillKeySelect,FillPropertySelect,FillWhereSelect);
    }

    // для update
    String fillSelect(Map<K, String> FillKeySelect, Map<V, String> FillPropertySelect, Collection<String> FillWhereSelect, SQLSyntax Syntax) {
        return fillSelect(getMapValues(Syntax),FillKeySelect,FillPropertySelect,FillWhereSelect);
    }

    // для выполнения в InsertSelect
    public String getSelect(SQLSyntax Syntax) {
        return translateParam(Select,getMapValues(Syntax));
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession Session,boolean OutSelect) throws SQLException {
        LinkedHashMap<Map<K,Integer>,Map<V,Object>> ExecResult = new LinkedHashMap<Map<K, Integer>, Map<V, Object>>();
        Statement Statement = Session.Connection.createStatement();

        try {
            String Execute = getSelect(Session.Syntax);
            if(OutSelect)
                System.out.println(Execute);
            ResultSet Result = Statement.executeQuery(Execute);
            try {
                while(Result.next()) {
                    Map<K,Integer> RowKeys = new HashMap<K, Integer>();
                    for(Map.Entry<K,String> Key : KeyNames.entrySet())
                        RowKeys.put(Key.getKey(),Type.Object.read(Result.getObject(Key.getValue())));
                    Map<V,Object> RowProperties = new HashMap<V, Object>();
                    for(Map.Entry<V,String> Property : PropertyNames.entrySet())
                        RowProperties.put(Property.getKey(),
                                PropertyTypes.get(Property.getKey()).read(Result.getObject(Property.getValue())));
                     ExecResult.put(RowKeys,RowProperties);
                }
            } finally {
                Result.close();
            }
        } finally {
            Statement.close();
        }

        return ExecResult;
    }

    void outSelect(DataSession Session) throws SQLException {
        // выведем на экран
        LinkedHashMap<Map<K, Integer>, Map<V, Object>> Result = executeSelect(Session,true);

        for(Map.Entry<Map<K,Integer>,Map<V,Object>> RowMap : Result.entrySet()) {
            for(Map.Entry<K,Integer> Key : RowMap.getKey().entrySet()) {
                System.out.print(Key.getKey()+"-"+Key.getValue());
                System.out.print(" ");
            }
            System.out.print("---- ");
            for(Map.Entry<V,Object> Property : RowMap.getValue().entrySet()) {
                System.out.print(Property.getKey()+"-"+Property.getValue());
                System.out.print(" ");
            }

            System.out.println("");
        }
    }
}

// поиск в кэше
class JoinCache<K,V> {
    JoinQuery<K,V> In;

    CompiledJoinQuery<K,V> Out;

    LinkedHashMap<SourceExpr,Boolean> Orders;
    int Top;

    JoinCache(JoinQuery<K, V> iIn, CompiledJoinQuery<K, V> iOut, LinkedHashMap<SourceExpr,Boolean> iOrders, int iTop) {
        In = iIn;
        Out = iOut;
        Orders = iOrders;
        Top = iTop;
    }

    <CK,CV> CompiledJoinQuery<CK,CV> cache(JoinQuery<CK,CV> Query, LinkedHashMap<SourceExpr,Boolean> QueryOrders, int SelectTop) {
        if(Top!=SelectTop) return null;

        Map<CK,K> MapKeys = new HashMap<CK,K>();
        Map<CV,V> MapProps = new HashMap<CV,V>();
        Map<ValueExpr,ValueExpr> MapValues = new HashMap<ValueExpr, ValueExpr>();
        if(Query.equalsMap(In,MapKeys,MapProps,MapValues,QueryOrders,Orders)) // нашли нужный кэш
            return new CompiledJoinQuery<CK,CV>(Out,MapKeys,MapProps,MapValues);

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

    private List<Join> Joins = null;
    Map<ValueExpr,ValueExpr> Values = null;
    
    private void fillJoins() {
        if(Joins==null) {
            Joins = new ArrayList<Join>();
            Set<ValueExpr> JoinValues = new HashSet<ValueExpr>();
            for(SourceExpr Property : properties.values())
                Property.fillJoins(Joins, JoinValues);
            where.fillJoins(Joins, JoinValues);

            if(Values==null) {
                Values = new HashMap<ValueExpr, ValueExpr>();
                for(Join Join : Joins)
                    JoinValues.addAll(Join.source.getValues().keySet());
                for(ValueExpr JoinValue : JoinValues)
                    Values.put(JoinValue,JoinValue);
            }
        }
    }
    // скомпилированные св-ва
    List<Join> getJoins() {
        fillJoins();
        return Joins;
    }
    Map<ValueExpr,ValueExpr> getValues() {
        fillJoins();
        return Values;
    }

    // перетранслирует
    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        parse().compileJoin(Join, Translated, TranslatedJoins);
    }

    static String stringOrder(LinkedHashMap<String,Boolean> OrderSelect) {
        String OrderString = "";
        for(Map.Entry<String,Boolean> Where : OrderSelect.entrySet())
            OrderString = (OrderString.length()==0?"":OrderString+",") + Where.getKey() + " " + (Where.getValue()?"DESC":"ASC");
        return OrderString;
    }

    int hashProperty(V Property) {
        return properties.get(Property).hash();
    }

    void and(Where AddWhere) {
        where = where.and(AddWhere);
    }

    void putKeyWhere(Map<K,Integer> KeyValues) {
        for(Map.Entry<K,Integer> MapKey : KeyValues.entrySet())
            and(new CompareWhere(mapKeys.get(MapKey.getKey()),new ValueExpr(MapKey.getValue(),Type.Object),CompareWhere.EQUALS));
    }

    void fillJoinQueries(Set<JoinQuery> Queries) {
        Queries.add(this);
        for(Join Join : getJoins())
            Join.source.fillJoinQueries(Queries);
    }

    CompiledJoinQuery<K,V> Compile = null;
    LinkedHashMap<SourceExpr,Boolean> CompiledOrders = null;
    int CompiledTop = 0;
    CompiledJoinQuery<K,V> parse(LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) {
//        System.out.println("compile..." + this);
//        if(1==1) return new CompiledJoinQuery<K,V>(this,Orders,SelectTop,Params);
        if(Compile!=null && Orders.equals(CompiledOrders)) return Compile;
        CompiledOrders = Orders;
        CompiledTop = SelectTop;

        Compile = getCache(this,Orders,SelectTop);
        if(Compile!=null) {
//            System.out.println("cached");
            return Compile;
        }

//        System.out.println("not cached");

        Compile = new CompiledJoinQuery<K,V>(this,Orders,SelectTop);
        putCache(this,Compile,Orders,SelectTop);

        return Compile;
    }
    CompiledJoinQuery<K,V> parse() {
        return parse(new LinkedHashMap<SourceExpr, Boolean>(),0);
    }

    CompiledQuery<K,V> compile(SQLSyntax Syntax) {
        return parse().compileSelect(Syntax);
    }
    CompiledQuery<K,V> compile(SQLSyntax Syntax,LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) {
        return parse(Orders,SelectTop).compileSelect(Syntax);
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

    static LinkedHashMap<SourceExpr,Boolean> reverseOrder(LinkedHashMap<SourceExpr,Boolean> Orders) {
        LinkedHashMap<SourceExpr,Boolean> Result = new LinkedHashMap<SourceExpr, Boolean>();
        for(Map.Entry<SourceExpr, Boolean> Order : Orders.entrySet())
            Result.put(Order.getKey(),!Order.getValue());
        return Result;
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession Session) throws SQLException {
        return compile(Session.Syntax).executeSelect(Session, false);
    }
    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession Session,LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) throws SQLException {
        return compile(Session.Syntax,Orders,SelectTop).executeSelect(Session,false);
    }

    public void outSelect(DataSession Session) throws SQLException {
        compile(Session.Syntax).outSelect(Session);
    }
    public void outSelect(DataSession Session,LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) throws SQLException {
        compile(Session.Syntax,Orders,SelectTop).outSelect(Session);
    }

    // для кэша - возвращает выражения + where чтобы потом еще Orders сравнить
    <EK,EV> boolean equals(JoinQuery<EK,EV> Query,Map<K,EK> EqualKeys,Map<V,EV> MapProperties, Map<ValueExpr, ValueExpr> MapValues, LinkedHashMap<SourceExpr,Boolean> Orders, LinkedHashMap<SourceExpr,Boolean> SourceOrders) {
        if(this==Query) {
            if(BaseUtils.identity(EqualKeys) && BaseUtils.identity(MapValues) && Orders.equals(SourceOrders)) {
                for(V Property : properties.keySet())
                    MapProperties.put(Property, (EV) Property);
                return true;
            } else
                return false;
        }

        if(properties.size()!=Query.properties.size()) return false;
        if(Orders.size()!=SourceOrders.size()) return false;

        MapValues = BaseUtils.crossJoin(getValues(),Query.getValues(),MapValues);
        if(MapValues.values().contains(null)) return false;

        Map<ObjectExpr,ObjectExpr> MapExprs = new HashMap<ObjectExpr,ObjectExpr>(MapValues);
        MapExprs.putAll(BaseUtils.crossJoin(mapKeys,Query.mapKeys,EqualKeys));

        // бежим по всем Join'ам, пытаемся промаппить их друг на друга
        List<Join> Joins = getJoins();
        List<Join> QueryJoins = Query.getJoins();

        if(Joins.size()!=QueryJoins.size())
            return false;

        // строим hash
        Map<Integer,Collection<Join>> HashQueryJoins = new HashMap<Integer,Collection<Join>>();
        for(Join Join : QueryJoins) {
            Integer Hash = Join.hash();
            Collection<Join> HashJoins = HashQueryJoins.get(Hash);
            if(HashJoins==null) {
                HashJoins = new ArrayList<Join>();
                HashQueryJoins.put(Hash,HashJoins);
            }
            HashJoins.add(Join);
        }

        Map<JoinWhere,JoinWhere> MapWheres = new HashMap<JoinWhere,JoinWhere>();
        // бежим
        for(Join Join : Joins) {
            Collection<Join> HashJoins = HashQueryJoins.get(Join.hash());
            if(HashJoins==null) return false;
            boolean Found = false;
            Iterator<Join> i = HashJoins.iterator();
            while(i.hasNext())
                if(Join.equals(i.next(), MapValues, MapExprs, MapWheres)) {i.remove(); Found = true; break;}
            if(!Found) return false;
        }

        if(!where.equals(Query.where,MapExprs,MapWheres)) return false;

        // проверим Orders
        Iterator<Map.Entry<SourceExpr,Boolean>> iOrders = Orders.entrySet().iterator();
        Iterator<Map.Entry<SourceExpr,Boolean>> iSourceOrders = SourceOrders.entrySet().iterator();
        while(iOrders.hasNext()) { // и так одинаковое количество элементов
            Map.Entry<SourceExpr,Boolean> Order = iOrders.next(); Map.Entry<SourceExpr,Boolean> SourceOrder = iSourceOrders.next();
            if(!(Order.getValue().equals(SourceOrder.getValue()) && Order.getKey().equals(SourceOrder.getKey(),MapExprs,MapWheres))) return false;
        }

        // свойства проверим
        Map<EV,V> QueryMap = new HashMap<EV, V>();
        for(Map.Entry<V,SourceExpr> MapProperty : properties.entrySet()) {
            EV MapQuery = null;
            for(Map.Entry<EV,SourceExpr> MapQueryProperty : Query.properties.entrySet())
                if(!QueryMap.containsKey(MapQueryProperty.getKey()) &&
                    MapProperty.getValue().equals(MapQueryProperty.getValue(),MapExprs, MapWheres)) {
                    MapQuery = MapQueryProperty.getKey();
                    break;
                }
            if(MapQuery==null) return false;
            QueryMap.put(MapQuery,MapProperty.getKey());
        }

        // составляем карту возврата
        for(Map.Entry<EV,V> MapQuery : QueryMap.entrySet())
            MapProperties.put(MapQuery.getValue(),MapQuery.getKey());
        return true;
    }

    class EqualCache<EK,EV> {
        JoinQuery<EK,EV> Query;
        Map<K,EK> MapKeys;
        Map<ValueExpr,ValueExpr> MapValues;

        EqualCache(JoinQuery<EK, EV> iQuery, Map<K, EK> iMapKeys, Map<ValueExpr, ValueExpr> iMapValues) {
            Query = iQuery;
            MapKeys = iMapKeys;
            MapValues = iMapValues;
        }

        public boolean equals(Object o) {
            return this==o || (o instanceof EqualCache && Query.equals(((EqualCache)o).Query) && MapKeys.equals(((EqualCache)o).MapKeys) && MapValues.equals(((EqualCache)o).MapValues));
        }

        public int hashCode() {
            return MapValues.hashCode()*31*31+MapKeys.hashCode()*31+Query.hashCode();
        }
    }
    
    // кэш для сравнений
    Map<EqualCache,Map<V,?>> CacheEquals = new HashMap<EqualCache, Map<V,?>>();
    <EK,EV> boolean equals(Source<EK,EV> Source,Map<K,EK> EqualKeys,Map<V,EV> MapProperties, Map<ValueExpr, ValueExpr> MapValues) {
        if(!(Source instanceof JoinQuery)) return false;

        JoinQuery<EK,EV> Query = (JoinQuery<EK,EV>)Source;
        
        EqualCache<EK,EV> EqualCache = new EqualCache<EK,EV>(Query, EqualKeys, MapValues);
        Map<V,EV> EqualMap = (Map<V,EV>) CacheEquals.get(EqualCache);
        if(EqualMap==null) {
            EqualMap = new HashMap<V, EV>();
            if(!equals(Query, EqualKeys, EqualMap, MapValues, new LinkedHashMap<SourceExpr,Boolean>(), new LinkedHashMap<SourceExpr,Boolean>()))
                return false;
            CacheEquals.put(EqualCache,EqualMap);
        }

        MapProperties.putAll(EqualMap);
        return true;
    }

    int getHash() {
        // должны совпасть hash'и properties и hash'и wheres
        int Hash = 0;
        for(SourceExpr Property : properties.values())
            Hash += Property.hash();
        return where.hash()*31+Hash;
    }

    static Map<Integer,Collection<JoinCache>> CacheCompile = new HashMap<Integer, Collection<JoinCache>>();
    static <K,V> CompiledJoinQuery<K,V> getCache(JoinQuery<K,V> Query,LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) {
        Collection<JoinCache> HashCaches = CacheCompile.get(Query.hash());
        if(HashCaches==null) return null;
        for(JoinCache<?,?> Cache : HashCaches) {
            CompiledJoinQuery<K,V> Result = Cache.cache(Query,Orders,SelectTop);
            if(Result!=null) return Result;
        }
        return null;
    }
    static <K,V> void putCache(JoinQuery<K,V> Query,CompiledJoinQuery<K,V> Compiled,LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) {
        Collection<JoinCache> HashCaches = CacheCompile.get(Query.hash());
        if(HashCaches==null) {
            HashCaches = new ArrayList<JoinCache>();
            CacheCompile.put(Query.hash(),HashCaches);
        }
        HashCaches.add(new JoinCache<K,V>(Query,Compiled,Orders,SelectTop));
    }

    <EK,EV> boolean equalsMap(JoinQuery<EK,EV> Query,Map<K,EK> EqualKeys,Map<V,EV> EqualProperties, Map<ValueExpr, ValueExpr> EqualValues, LinkedHashMap<SourceExpr,Boolean> Orders, LinkedHashMap<SourceExpr,Boolean> SourceOrders) {

        // переберем Values, Keys
        Collection<Map<K,EK>> KeyMaps = MapBuilder.buildPairs(keys, Query.keys);
        if(KeyMaps==null) return false;

        Collection<Map<ValueExpr,ValueExpr>> ValueMaps = MapBuilder.buildPairs(getValues().keySet(), Query.getValues().keySet());
        if(ValueMaps==null) return false;

        for(Map<ValueExpr,ValueExpr> ValueMap : ValueMaps)
            for(Map<K,EK> MapKeys : KeyMaps)
                if(equals(Query,MapKeys,EqualProperties,ValueMap,Orders,SourceOrders)) {
                    EqualKeys.putAll(MapKeys);
                    EqualValues.putAll(ValueMap);
                    return true;
                }
        return false;
    }

    // конструктор копирования
    JoinQuery(JoinQuery<K,V> Query) {
        this(Query.mapKeys);
        properties.putAll(Query.properties);
        where = Query.where;
        Values = Query.Values;
    }

    // конструктор фиксации переменных
    JoinQuery(Map<? extends V, ValueExpr> PropertyValues,JoinQuery<K,V> Query) {
        this(Query.keys);

        Join<K,V> SourceJoin = new Join<K,V>(Query,this);
        where = SourceJoin.inJoin;

        // раскидываем по dumb'ам или на выход
        for(Map.Entry<V,JoinExpr<K,V>> Property : SourceJoin.exprs.entrySet()) {
            ValueExpr PropertyValue = PropertyValues.get(Property.getKey());
            if(PropertyValue==null)
                properties.put(Property.getKey(), Property.getValue());
            else
                where = where.and(new CompareWhere(Property.getValue(),PropertyValue,CompareWhere.EQUALS));
        }
    }

    // конструктор трансляции переменных
    JoinQuery(JoinQuery<K,V> Query,Map<ValueExpr, ValueExpr> MapValues) {
        this(Query.mapKeys);

        properties = Query.properties;
        where = Query.where;
        Values = BaseUtils.join(MapValues,Query.getValues());

        if(Query.Compile!=null) {
            Compile = new CompiledJoinQuery<K,V>(Query.Compile,MapValues);
            CompiledOrders = Query.CompiledOrders;
            CompiledTop = Query.CompiledTop;
        }
    }
}

class CompiledJoinQuery<K,V> {

    Map<V,SourceExpr> properties;
    Where Where;

    Map<K,KeyExpr> Keys;

    List<CompiledJoin> Joins;
    Map<ValueExpr,ValueExpr> Values;

    LinkedHashMap<SourceExpr,Boolean> Orders;
    int Top;
    
    private CompiledJoinQuery(Map<K,KeyExpr> iKeys) {
        Keys = iKeys;
    }

    CompiledJoinQuery(JoinQuery<K,V> Query,LinkedHashMap<SourceExpr,Boolean> QueryOrders, int QueryTop) {
        this(Query.mapKeys);

        ExprTranslator Translated = new ExprTranslator();

        Joins = new ArrayList<CompiledJoin>(); 
        for(Join Join : Query.getJoins())
            Join.source.compileJoin(Join, Translated, Joins);

        // Where
        Where = Query.where.translate(Translated);

        // свойства
        properties = new HashMap<V, SourceExpr>();
        for(Map.Entry<V,SourceExpr> MapProperty : Query.properties.entrySet())
            properties.put(MapProperty.getKey(),MapProperty.getValue().translate(Translated).compile(Where));
        // порядки
        Top = QueryTop;
        Orders = new LinkedHashMap<SourceExpr, Boolean>();
        for(Map.Entry<SourceExpr,Boolean> Order : QueryOrders.entrySet())
            Orders.put(Order.getKey().translate(Translated).compile(Where),Order.getValue());

        // Values
        Values = Query.getValues();

        Compiler = new Compiler();
    }

    Compiler Compiler;
    CompiledQuery<K,V> Compile = null;
    CompiledQuery<K,V> compileSelect(SQLSyntax Syntax) {
        synchronized(this) { // тут он уже в кэше может быть
            if(Compile==null)
                Compile = Compiler.compile(Syntax);
            return Compile;
        }
    }

    boolean isEmpty() {
        return Where.isFalse();
    }

    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {

        ExprTranslator JoinTranslated = new ExprTranslator();
        // закинем перекодирование ключей
        for(Map.Entry<K,KeyExpr> MapKey : Keys.entrySet())
            JoinTranslated.put(MapKey.getValue(), Join.joins.get(MapKey.getKey()).translate(Translated));

        JoinTranslated.putAll(BaseUtils.reverse(Values));

        // рекурсивно погнали остальные JoinQuery, здесь уже DataSource'ы причем без CaseExpr'ов
        for(CompiledJoin CompileJoin : Joins) // здесь по сути надо перетранслировать ValueExpr'ы а также в GroupQuery перебить JoinQuery на новые Values
            CompileJoin.translate(JoinTranslated, TranslatedJoins, CompileJoin.getDataSource().translateValues(Values));

        for(Map.Entry<V,SourceExpr> MapProperty : properties.entrySet())
            Translated.put(Join.exprs.get(MapProperty.getKey()),new CaseExpr(Where,MapProperty.getValue()).translate(JoinTranslated));
        Translated.put(Join.inJoin,Where.translate(JoinTranslated));
    }

    private class Compiler {
        CompiledQuery<K,V> compile(SQLSyntax Syntax) {
            return new CompiledQuery<K,V>(CompiledJoinQuery.this,Syntax);
        }
    }

    private class MapCompiler<MK,MV> extends Compiler {
        CompiledJoinQuery<MK,MV> MapQuery;
        Map<K,MK> MapKeys;
        Map<V,MV> MapProps;
        Map<ValueExpr,ValueExpr> MapValues;

        private MapCompiler(CompiledJoinQuery<MK, MV> iMapQuery, Map<K, MK> iMapKeys, Map<V, MV> iMapProps, Map<ValueExpr, ValueExpr> iMapValues) {
            MapQuery = iMapQuery;
            MapKeys = iMapKeys;
            MapProps = iMapProps;
            MapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax Syntax) {
            return new CompiledQuery<K,V>(MapQuery.compileSelect(Syntax), MapKeys, MapProps, MapValues);
        }
    }
    <MK,MV> CompiledJoinQuery(CompiledJoinQuery<MK,MV> Query, Map<K, MK> MapKeys, Map<V, MV> MapProps, Map<ValueExpr,ValueExpr> MapValues) {
        this(BaseUtils.join(MapKeys,Query.Keys));
        properties = BaseUtils.join(MapProps,Query.properties);
        Orders = Query.Orders;
        Top = Query.Top;
        Where = Query.Where;
        Joins = Query.Joins;
        Values = BaseUtils.join(MapValues,Query.Values);

        Compiler = new MapCompiler<MK,MV>(Query,MapKeys,MapProps,MapValues);
    }
    
    private class ValueCompiler extends Compiler {
        CompiledJoinQuery<K,V> MapQuery;
        Map<ValueExpr,ValueExpr> MapValues;

        private ValueCompiler(CompiledJoinQuery<K, V> iMapQuery, Map<ValueExpr, ValueExpr> iMapValues) {
            MapQuery = iMapQuery;
            MapValues = iMapValues;
        }

        CompiledQuery<K, V> compile(SQLSyntax Syntax) {
            return new CompiledQuery<K,V>(MapQuery.compileSelect(Syntax), MapValues);
        }
    }
    CompiledJoinQuery(CompiledJoinQuery<K,V> Query, Map<ValueExpr,ValueExpr> MapValues) {
        this(Query.Keys);
        properties = Query.properties;
        Orders = Query.Orders;
        Top = Query.Top;
        Where = Query.Where;
        Joins = Query.Joins;
        Values = BaseUtils.join(MapValues,Query.Values);

        Compiler = new ValueCompiler(Query,MapValues);
    }

}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends JoinQuery<K,V> {

    UnionQuery(Collection<? extends K> iKeys, int iDefaultOperator) {
        super(iKeys);
        DefaultOperator = iDefaultOperator;
        where = where.FALSE;
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