package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

interface Translator {

    IntraWhere translate(IntraWhere Where);
    SourceExpr translate(SourceExpr Expr);
}

class ExprTranslator implements Translator {

    private Map<IntraWhere, IntraWhere> Wheres = new HashMap<IntraWhere, IntraWhere>();
//    Map<DataWhere,IntraWhere> Wheres = new HashMap<DataWhere, IntraWhere>();
    private Map<SourceExpr,SourceExpr> Exprs = new HashMap<SourceExpr, SourceExpr>();

    void put(DataWhere Where, IntraWhere To) {
        Wheres.put(Where,To);
    }

    void put(ObjectExpr Expr, SourceExpr To) {
        Exprs.put(Expr,To);
    }

    public IntraWhere translate(IntraWhere Where) {
        IntraWhere Result = Wheres.get(Where);
        if(Result==null) {
            Result = Where.translate(this);
//            Wheres.put(IntraWhere,Result); // короче работает только если equals'ы и hashCode'ы перебить у Or\And'ов
            // все равно по другому работает
        }

        return Result;
    }

    public SourceExpr translate(SourceExpr Expr) {
        SourceExpr Result = Exprs.get(Expr);
        if(Result==null) {
            Result = Expr.translate(this);
//            Exprs.put(Expr,Result);
        }
        return Result;
    }
}

class MapWhere<T> extends HashMap<T, OuterWhere> {

    void add(T Object, IntraWhere Where) {
        OuterWhere InWhere = get(Object);
        if(InWhere==null) {
            InWhere = new OuterWhere();
            InWhere.out(Where);
            put(Object,InWhere);
        }
        InWhere.out(Where);
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

    <J extends Join> void fillJoins(List<J> Joins);
//    void fillJoins(List<? extends Join> Joins);
    void fillJoinWheres(MapWhere<JoinData> Joins, OuterWhere AndWhere);
}

class AndJoinQuery {

    AndJoinQuery(InnerWhere iWhere, String iAlias) {
        Where = iWhere;
        Alias = iAlias;
    }

    InnerWhere Where;
    String Alias;
    Map<String,SourceExpr> Properties = new HashMap<String, SourceExpr>();
}

abstract class AbstractJoinQuery<K,V,J extends Join> extends Source<K,V> {

    AbstractJoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);

        for(K Key : Keys)
            MapKeys.put(Key,new KeyExpr<K>(Key));
    }

    Map<V,SourceExpr> Properties = new HashMap<V, SourceExpr>();
    IntraWhere Where = new InnerWhere();

    Map<K,KeyExpr<K>> MapKeys = new HashMap<K, KeyExpr<K>>();

    Collection<V> getProperties() {
        return Properties.keySet();
    }

    Type getType(V Property) {
        return Properties.get(Property).getType();
    }

    // скомпилированные св-ва
    List<J> getJoins() {
        List<J> Joins = new ArrayList<J>();
        for(SourceExpr Property : Properties.values())
            Property.fillJoins(Joins);
        Where.fillJoins(Joins);
        return Joins;
    }

    // перетранслирует
    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        // закинем перекодирование ключей
        for(Map.Entry<K,KeyExpr<K>> MapKey : MapKeys.entrySet())
            Translated.put(MapKey.getValue(), Translated.translate(Join.Joins.get(MapKey.getKey())));

        // рекурсивно погнали остальные JoinQuery
        for(Join CompileJoin : getJoins())
            CompileJoin.Source.compileJoin(CompileJoin, Translated, TranslatedJoins);

        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            Translated.put(Join.Exprs.get(MapProperty.getKey()),Translated.translate(new CaseExpr(Where,MapProperty.getValue())));
        Translated.put(Join.InJoin,Translated.translate(Where));
    }

    String stringOrder(LinkedHashMap<String,Boolean> OrderSelect) {
        String OrderString = "";
        for(Map.Entry<String,Boolean> Where : OrderSelect.entrySet())
            OrderString = (OrderString.length()==0?"":OrderString+",") + Where.getKey() + " " + (Where.getValue()?"DESC":"ASC");
        return OrderString;
    }
}

class CompiledQuery<K,V> {
    CompiledJoinQuery Query;
    Map<K,Object> MapKeys;
    Map<V,Object> MapProps;
//    Map<VariableExpr,Object> MapVars;

    CompiledQuery(CompiledJoinQuery iQuery) {
        Query = iQuery;
        MapKeys = new HashMap<K, Object>();
        MapProps = new HashMap<V, Object>();
    }

    public String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        Map<Object,String> CompiledKeys = new HashMap<Object, String>(); Map<Object,String> CompiledProps = new HashMap<Object, String>();
        String From = Query.fillSelect(CompiledKeys,CompiledProps,WhereSelect,new LinkedHashMap<String, Boolean>(),Syntax);
        KeySelect.putAll(BaseUtils.join(MapKeys,CompiledKeys)); PropertySelect.putAll(BaseUtils.join(MapProps,CompiledProps));
        return From;
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession Session,boolean OutSelect) throws SQLException {
        LinkedHashMap<Map<K, Integer>, Map<V, Object>> Result = new LinkedHashMap<Map<K, Integer>, Map<V, Object>>();
        LinkedHashMap<Map<Object, Integer>, Map<Object, Object>> CompiledResult = Query.executeSelect(Session,OutSelect);
        for(Map.Entry<Map<Object,Integer>,Map<Object,Object>> CompiledRow : CompiledResult.entrySet())
            Result.put(BaseUtils.join(MapKeys,CompiledRow.getKey()),BaseUtils.join(MapProps,CompiledRow.getValue()));
        return Result;
    }

    void outSelect(DataSession Session) throws SQLException {
        // выведем на экран
        Collection<String> ResultFields = new ArrayList();

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

    public boolean isEmpty() {
        return Query.Where.isFalse();
    }

    public String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax, int Top) {
        List<Object> CompiledKeys = new ArrayList<Object>(); List<Object> CompiledProps = new ArrayList<Object>();        
        String Select = Query.getSelect(CompiledKeys, CompiledProps, Syntax, Top);
        for(Object Key : CompiledKeys)
            for(Map.Entry<K,Object> MapKey : MapKeys.entrySet())
                if(MapKey.getValue()==Key) {KeyOrder.add(MapKey.getKey()); break;}
        for(Object Property : CompiledProps)
            for(Map.Entry<V,Object> MapProp : MapProps.entrySet())
                if(MapProp.getValue()==Property) {PropertyOrder.add(MapProp.getKey()); break;}
        return Select;
    }
}

//class VariableExpr extends SourceExpr {

//}

// запрос Join
class JoinQuery<K,V> extends AbstractJoinQuery<K,V,Join> {

    JoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);
    }

    void and(IntraWhere AddWhere) {
        Where = Where.in(AddWhere);
    }

    void putKeyWhere(Map<K,Integer> KeyValues) {
        for(Map.Entry<K,Integer> MapKey : KeyValues.entrySet())
            and(new CompareWhere(MapKeys.get(MapKey.getKey()),new ValueExpr(MapKey.getValue(),Type.Object),CompareWhere.EQUALS));
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        return compile().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);
    }
    void outSelect(DataSession Session) throws SQLException {
        outSelect(Session, new LinkedHashMap<SourceExpr, Boolean>(), 0);
    }
    void outSelect(DataSession Session, LinkedHashMap<SourceExpr,Boolean> Orders, int Top) throws SQLException {
        compile(Orders,Top).outSelect(Session);
    }
    
    CompiledQuery<K,V> compile() {
        return compile(new LinkedHashMap<SourceExpr, Boolean>(),0);
    }

    CompiledQuery<K,V> Compile = null;
    LinkedHashMap<SourceExpr,Boolean> CompiledOrders = null;
    CompiledQuery<K,V> compile(LinkedHashMap<SourceExpr,Boolean> Orders,int SelectTop) {

//        System.out.println("compile..." + this);
        if(Compile==null || !Orders.equals(CompiledOrders)) {
            CompiledOrders = Orders;
            ExprTranslator Translated = new ExprTranslator();

            // чтобы сливать Join'ы
            CompiledJoinQuery CompiledQuery = new CompiledJoinQuery(Keys);
            // сразу в транслятор ключи закинем
            for(Map.Entry<K,KeyExpr<K>> MapKey : MapKeys.entrySet())
                Translated.put(MapKey.getValue(),CompiledQuery.MapKeys.get(MapKey.getKey()));

            // закидываем перекомпиляцию ключей
            List<CompiledJoin> CompiledJoins = new ArrayList<CompiledJoin>();
            for(Join Join : getJoins())
                Join.Source.compileJoin(Join, Translated, CompiledJoins);

            CompiledQuery.Where = new JoinTranslator(Translated).translate(Where);
//            CompiledQuery.IntraWhere = Translated.translate(IntraWhere);

            for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
                CompiledQuery.Properties.put(MapProperty.getKey(),Translated.translate(MapProperty.getValue()).compile(CompiledQuery.Where));
            for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet())
                CompiledQuery.Orders.put(Translated.translate(Order.getKey()).compile(CompiledQuery.Where),Order.getValue());
            CompiledQuery.Top = SelectTop;

            Compile = new CompiledQuery<K,V>(CompiledQuery);
            for(K Key : Keys) Compile.MapKeys.put(Key,Key);
            for(V Property : Properties.keySet()) Compile.MapProps.put(Property,Property);                
        }

        return Compile;
    }

    class JoinTranslator implements Translator {

        ExprTranslator Translator;
        JoinTranslator(ExprTranslator iTranslator) {
            Translator = iTranslator;
        }

        public IntraWhere translate(IntraWhere Where) {
            if(Where instanceof ObjectWhere)
                return Translator.translate(Where).getJoinWhere();
            else
                return Where.translate(this);
        }

        public SourceExpr translate(SourceExpr Expr) {
            throw new RuntimeException("only for wheres");
        }
    }

    <MK, MV> JoinQuery<K, Object> or(JoinQuery<MK, MV> ToMergeQuery, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps) {

        // результат
        JoinQuery<K,Object> MergeQuery = new JoinQuery<K,Object>(Keys);

        // себя Join'им
        CompiledQuery<K,V> OrCompile = compile();
        Join<Object,Object> Join = new Join<Object,Object>(OrCompile.Query,MergeQuery,OrCompile.MapKeys);
        MergeQuery.Properties.putAll(BaseUtils.join(OrCompile.MapProps,Join.Exprs));

        // Merge запрос Join'им, надо пересоздать объекты и построить карту
        CompiledQuery<MK,MV> OrMergeCompile = ToMergeQuery.compile();
        Join<Object,Object> MergeJoin = new Join<Object,Object>(OrMergeCompile.Query,MergeQuery,BaseUtils.join(MergeKeys,OrMergeCompile.MapKeys));
        for(Map.Entry<MV,Object> MergeExpr : OrMergeCompile.MapProps.entrySet()) {
            Object MergeObject = new Object();
            MergeProps.put(MergeExpr.getKey(),MergeObject);
            MergeQuery.Properties.put(MergeObject, MergeJoin.Exprs.get(MergeExpr.getValue()));
        }

        // закинем их на Or
        OuterWhere Where = new OuterWhere();
        Where.out(Join.InJoin);
        Where.out(MergeJoin.InJoin);
        MergeQuery.and(Where);

        CompiledQuery<K,Object> CompiledMerge = MergeQuery.compile();
        Map<Object,SourceExpr> CompiledProps = BaseUtils.join(CompiledMerge.MapProps,CompiledMerge.Query.Properties);

        // погнали сливать одинаковые (именно из раных)
        for(Map.Entry<MV,Object> MergeProperty : MergeProps.entrySet()) {
            SourceExpr MergeExpr = CompiledProps.get(MergeProperty.getValue());
            for(V Property : Properties.keySet()) {
                if(CompiledProps.get(Property).equals(MergeExpr)) {
                    MergeQuery.Properties.remove(MergeProperty.getValue());
                    MergeProperty.setValue(Property);
                    break;
                }
            }
        }

//        System.out.println("ored");
        return MergeQuery;
    }

    static LinkedHashMap<SourceExpr,Boolean> reverseOrder(LinkedHashMap<SourceExpr,Boolean> Orders) {
        LinkedHashMap<SourceExpr,Boolean> Result = new LinkedHashMap<SourceExpr, Boolean>();
        for(Map.Entry<SourceExpr, Boolean> Order : Orders.entrySet())
            Result.put(Order.getKey(),!Order.getValue());
        return Result;
    }

    String getKeyName(K Key) {
        throw new RuntimeException("не должно быть");
    }

    String getPropertyName(V Value) {
        throw new RuntimeException("не должно быть");
    }

    // для кэша
    <EK,EV> boolean equals(Source<EK,EV> Source,Map<K,EK> EqualKeys,Map<V,EV> MapProperties, Map<ObjectExpr, ObjectExpr> MapValues) {
        if(!(Source instanceof JoinQuery)) return false;

        JoinQuery<EK,EV> Query = (JoinQuery<EK,EV>)Source;

        if(Properties.size()!=Query.Properties.size()) return false;

        Map<ObjectExpr,ObjectExpr> MapExprs = new HashMap<ObjectExpr, ObjectExpr>(MapValues);
        for(Map.Entry<K,EK> MapKey : EqualKeys.entrySet())
            MapExprs.put(MapKeys.get(MapKey.getKey()),Query.MapKeys.get(MapKey.getValue()));

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

        Map<JoinWhere,JoinWhere> MapWheres = new HashMap<JoinWhere, JoinWhere>();
        // бежим
        for(Join Join : Joins) {
            Collection<Join> HashJoins = HashQueryJoins.get(Join.hash());
            if(HashJoins==null) return false;
            boolean Found = false;
            Iterator<Join> i = HashJoins.iterator();
            while(i.hasNext())
                if(Join.equals(i.next(),MapExprs, MapWheres)) {i.remove(); Found = true; break;}
            if(!Found) return false;
        }

        if(!Where.equals(Query.Where,MapExprs,MapWheres)) return false; 

        // свойства проверим
        Map<EV,V> QueryMap = new HashMap<EV, V>();
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet()) {
            EV MapQuery = null;
            for(Map.Entry<EV,SourceExpr> MapQueryProperty : Query.Properties.entrySet())
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

    int hash() {
        // должны совпасть hash'и properties и hash'и wheres
        int Hash = 0;
        for(SourceExpr Property : Properties.values())
            Hash += Property.hash();
        return Where.hash()*31+Hash;
    }
}

class CompiledJoinQuery extends AbstractJoinQuery<Object,Object,CompiledJoin> {

    CompiledJoinQuery(Collection<? extends Object> iKeys) {
        super(iKeys);
    }

    LinkedHashMap<SourceExpr,Boolean> Orders = new LinkedHashMap<SourceExpr, Boolean>();
    int Top;

    String getSelect(List<Object> KeyOrder, List<Object> PropertyOrder, SQLSyntax Syntax, int Top) {

        if(Syntax.useFJ()) {
            Map<Object,String> KeySelect = new HashMap<Object,String>();
            Map<Object,String> PropertySelect = new HashMap<Object,String>();
            Collection<String> WhereSelect = new ArrayList<String>();
            LinkedHashMap<String,Boolean> OrderSelect = new LinkedHashMap<String,Boolean>();
            String From = fillSelect(KeySelect,PropertySelect,WhereSelect,OrderSelect,Syntax);

            return Syntax.getSelect(From,stringExpr(KeySelect,KeyOrder,PropertySelect,PropertyOrder),
                stringWhere(WhereSelect),stringOrder(OrderSelect),"",Top);
        } else {
            OuterWhere OrWhere = Where.getOr();
            // если оказалось что IntraWhere false - то есть не может быть в запросе
            if(OrWhere.isFalse()) {
                Map<Object,String> KeySelect = new HashMap<Object, String>();
                Map<Object,String> PropertySelect = new HashMap<Object, String>();
                return Syntax.getSelect(fillEmptySelect(KeySelect, PropertySelect),
                        stringExpr(KeySelect,KeyOrder,PropertySelect,PropertyOrder),"","","",0);
            }

            // в Properties закинем Orders,
            HashMap<Object,SourceExpr> UnionProps = new HashMap<Object, SourceExpr>(Properties);
            LinkedHashMap<String,Boolean> OrderNames = new LinkedHashMap<String, Boolean>();
            int io = 0;
            for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet()) {
                String OrderName = "order_"+io;
                UnionProps.put(OrderName,Order.getKey());
                OrderNames.put(OrderName,Order.getValue());
            }

            String From = "";
            while(true) {
                InnerWhere AndWhere = OrWhere.iterator().next();
                Map<Object,String> AndKeySelect = new HashMap<Object, String>();
                LinkedHashMap<Object,String> AndPropertySelect = new LinkedHashMap<Object, String>();
                Collection<String> AndWhereSelect = new ArrayList<String>();
                String AndFrom = fillAndSelect(AndWhere,UnionProps,new LinkedHashMap<SourceExpr,Boolean>(),AndKeySelect,
                    AndPropertySelect,AndWhereSelect,new LinkedHashMap<String,Boolean>(),Syntax);

                LinkedHashMap<String,String> NamedProperties = new LinkedHashMap<String, String>();
                for(Object Property : Properties.keySet()) {
                    NamedProperties.put(getPropertyName(Property),AndPropertySelect.get(Property));
                    if(From.length()==0) PropertyOrder.add(Property);
                }
                for(String Order : OrderNames.keySet())
                    NamedProperties.put(Order,AndPropertySelect.get(Order));

                From = (From.length()==0?"":From+" UNION ALL ") +
                    Syntax.getSelect(AndFrom,stringExpr(AndKeySelect,From.length()==0?KeyOrder:new ArrayList<Object>(),NamedProperties),
                            stringWhere(AndWhereSelect),"","",0);

                OrWhere = OrWhere.inNot(AndWhere).getOr();
                if(OrWhere.isFalse()) break;
            }

            return Syntax.getUnionOrder(From,stringOrder(OrderNames),Top);
        }
    }

    // в общем случае получить AndJoinQuery под которые подходит IntraWhere
    Collection<AndJoinQuery> getWhereSubSet(Collection<AndJoinQuery> AndWheres, IntraWhere Where) {

        Collection<AndJoinQuery> Result = new ArrayList<AndJoinQuery>();
        OuterWhere ResultWhere = new OuterWhere();
        while(Result.size()<AndWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery LastQuery = null;
            OuterWhere LastWhere = null;
            for(AndJoinQuery And : AndWheres)
                if(!Result.contains(And)) {
                    LastQuery = And;
                    LastWhere = new OuterWhere(ResultWhere);
                    LastWhere.out(LastQuery.Where);
                    if(Where.followFalse(LastWhere).means(LastWhere)) {
                        Result.add(LastQuery);
                        return Result;
                    }
                }
            ResultWhere = LastWhere;
            Result.add(LastQuery);
        }
        return Result;
    }

    String fillSelect(Map<Object, String> KeySelect, Map<Object, String> PropertySelect, Collection<String> WhereSelect, LinkedHashMap<String,Boolean> OrderSelect, SQLSyntax Syntax) {

        if(Syntax.useFJ()) { // OuterWhere - сложный придется разбивать на подзапросы и FULL JOIN'ить
            OuterWhere CompiledWhere = Where.getOr(); //getJoinWhere()

            // если оказалось что IntraWhere false - то есть не может быть в запросе
            if(CompiledWhere.isFalse())
                return fillEmptySelect(KeySelect, PropertySelect);

            // если OuterWhere простой
            if(CompiledWhere.size()==1)
                return fillAndSelect(CompiledWhere.iterator().next(), Properties, Orders, KeySelect, PropertySelect, WhereSelect, OrderSelect, Syntax);

            Collection<AndJoinQuery> AndProps = new ArrayList<AndJoinQuery>();
            for(InnerWhere AndWhere : CompiledWhere)
                AndProps.add(new AndJoinQuery(AndWhere,"f"+AndProps.size()));

            // сюда будут класться данные для чтения
            Map<QueryData,String> FJSource = new HashMap<QueryData, String>();

            // бежим по всем property, определяем Join параметры с IntraWhere
            MapWhere<JoinData> JoinDataWheres = new MapWhere<JoinData>();
            for(Map.Entry<Object,SourceExpr> JoinProp : Properties.entrySet())
                JoinProp.getValue().fillJoinWheres(JoinDataWheres,new InnerWhere());

            // группируем по Join'ам
            MapWhere<Join> JoinWheres = new MapWhere<Join>();
            for(Map.Entry<JoinData, OuterWhere> JoinData : JoinDataWheres.entrySet())
                JoinWheres.add(JoinData.getKey().getJoin(),JoinData.getValue());

            // сначала распихиваем Join по And'ам
            Map<Join,Collection<AndJoinQuery>> JoinAnds = new HashMap<Join,Collection<AndJoinQuery>>();
            for(Map.Entry<Join, OuterWhere> JoinWhere : JoinWheres.entrySet())
                JoinAnds.put(JoinWhere.getKey(),getWhereSubSet(AndProps,JoinWhere.getValue()));

            // затем все данные по Join'ам по вариантам
            int JoinNum = 0;
            for(Map.Entry<JoinData, OuterWhere> JoinData : JoinDataWheres.entrySet()) {
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
            for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet()) {
                String OrderName = "order_" + (io++);
                String FJOrder = "";
                for(AndJoinQuery And : AndProps) {
                    And.Properties.put(OrderName,Order.getKey());
                    FJOrder = (FJOrder.length()==0?"":FJOrder+",") + And.Alias + "." + OrderName;
                }
                OrderSelect.put("COALESCE("+FJOrder+")",Order.getValue());
            }

            // бежим по всем And'ам делаем Join запросы, потом объединяем их FULL'ами
            String From = "";
            boolean Second = true; // для COALESCE'ов
            for(AndJoinQuery And : AndProps) {
                // закинем в And.Properties OrderBy
                Map<Object,String> AndKeySelect = new HashMap<Object, String>(); Collection<String> AndWhereSelect = new ArrayList<String>();
                LinkedHashMap<String,String> AndPropertySelect = new LinkedHashMap<String, String>();
                LinkedHashMap<String,Boolean> AndOrderSelect = new LinkedHashMap<String, Boolean>();
                String AndFrom = fillAndSelect(And.Where, And.Properties, Orders, AndKeySelect, AndPropertySelect, AndWhereSelect, AndOrderSelect, Syntax);

                String AndSelect = "(" + Syntax.getSelect(AndFrom,stringExpr(AndKeySelect,new ArrayList<Object>(),AndPropertySelect),
                        stringWhere(AndWhereSelect),stringOrder(AndOrderSelect),"",Top) + ") "+And.Alias;

//                System.out.println(And.Alias);
//                outAndSelect(Syntax.getSelect(AndFrom,stringExpr(AndKeySelect,new ArrayList<Object>(),AndPropertySelect),
//                        stringWhere(AndWhereSelect),stringOrder(AndOrderSelect),"",Top),And.Properties);
                if(From.length()==0) {
                    From = AndSelect;
                    for(Object Key : Keys)
                        KeySelect.put(Key,And.Alias+"."+getKeyName(Key));
                } else {
                    String AndJoin = "";
                    for(Map.Entry<Object,String> MapKey : KeySelect.entrySet()) {
                        String AndKey = And.Alias + "." + getKeyName(MapKey.getKey());
                        AndJoin = (AndJoin.length()==0?"":AndJoin + " AND ") + AndKey + "=" + (Second?MapKey.getValue():"COALESCE("+MapKey.getValue()+")");
                        MapKey.setValue(MapKey.getValue()+","+AndKey);
                    }
                    From = From + " FULL JOIN " + AndSelect + " ON " + (AndJoin.length()==0?ObjectWhere.TRUE:AndJoin);
                    Second = false;
                }
            }

            // полученные KeySelect'ы в Data
            for(Map.Entry<Object,String> MapKey : KeySelect.entrySet()) {
                MapKey.setValue("COALESCE("+MapKey.getValue()+")"); // обернем в COALESCE
                FJSource.put(MapKeys.get(MapKey.getKey()),MapKey.getValue());
            }

            // закидываем PropertySelect'ы
            for(Map.Entry<Object,SourceExpr> MapProp : Properties.entrySet()) {
                String PropertyValue = MapProp.getValue().getSource(FJSource, Syntax);
                if(PropertyValue.equals(Type.NULL))
                    PropertyValue = Syntax.getNullValue(MapProp.getValue().getType());
                PropertySelect.put(MapProp.getKey(),PropertyValue);
            }

            return From;
        } else {
            String Alias = "G";
            for(Object Key : Keys)
                KeySelect.put(Key, Alias + "." + getKeyName(Key));
            for(Object Property : getProperties())
                PropertySelect.put(Property, Alias + "." + getPropertyName(Property));

            return "(" + getSelect(new ArrayList<Object>(),new ArrayList<Object>(),Syntax,Top) + ") "+Alias;
        }
    }

    private String fillEmptySelect(Map<Object, String> KeySelect, Map<Object, String> PropertySelect) {
        for(Object Key : Keys)
            KeySelect.put(Key, Type.NULL);
        for(Object Property : Properties.keySet())
            PropertySelect.put(Property,Type.NULL);
        return "empty";
    }

    void outAndSelect(String AndSelect,Map<String,SourceExpr> Properties) {

        if(Main.Session==null) return;

        System.out.println(AndSelect);
        try {
            Statement Statement = Main.Session.Connection.createStatement();

//        System.out.println(getSelect(new ArrayList(),new ArrayList(), Session.Syntax));
            try {
                ResultSet Result = Statement.executeQuery(AndSelect);
                try {
                    while(Result.next()) {
                        for(Object Key : Keys)
                            System.out.print(Key+"-"+Type.Object.read(Result.getObject(getKeyName(Key)))+" ");
                        System.out.print(" --- ");

                        for(Map.Entry<String,SourceExpr> Property : Properties.entrySet())
                            System.out.print(Property.getKey()+"-"+Property.getValue().getType().read(Result.getObject(Property.getKey()))+" ");
                        System.out.println();
                    }
                } finally {
                    Result.close();
                }
            } finally {
                Statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    <AV> String fillAndSelect(InnerWhere AndWhere,Map<AV,SourceExpr> CompiledProps,LinkedHashMap<SourceExpr,Boolean> CompiledOrders,Map<Object, String> KeySelect, Map<AV, String> PropertySelect, Collection<String> WhereSelect, LinkedHashMap<String,Boolean> OrderSelect, SQLSyntax Syntax) {

        // скопируем и будем вырезать IntraWhere
        Collection<ObjectWhere> SelectWheres = new ArrayList<ObjectWhere>(AndWhere);

        Map<AndExpr,ValueExpr> ExprValues = new HashMap<AndExpr,ValueExpr>();
        Map<QueryData,String> QueryData = new HashMap<QueryData,String>();

        // сначала вычистим избыточные IntraWhere из Join'ов и узнаем все Join'ы
        // заодно поищем ExprValues запишем их в Source
        List<CompiledJoin> AllJoins = new ArrayList<CompiledJoin>();
        // сначала прямые чтобы Inner'ы вперед пошли
        for(ObjectWhere Where : AndWhere)
            if(Where instanceof DataWhere) {
                Where.fillJoins(AllJoins);
                if(Where instanceof CompareWhere) { // будем считать что всегда в первом операторе
                    CompareWhere Compare = (CompareWhere)Where;
                    if(Compare.Operator2 instanceof ValueExpr && Compare.Compare==CompareWhere.EQUALS) {
                        ExprValues.put((AndExpr)Compare.Operator1,(ValueExpr)Compare.Operator2);
                        if(Compare.Operator1 instanceof KeyExpr) { // проставим в KeyExpr'ы значения, вырежем из выполняемых
                            QueryData.put((KeyExpr)Compare.Operator1,((ValueExpr)Compare.Operator2).getString(Syntax));
                            SelectWheres.remove(Where);
                        }
                    }
                } else
                if(Where instanceof JoinWhere) // Join'ы тоже можно не проверять
                    SelectWheres.remove(Where);
            }
        // затем Not'ы
        for(ObjectWhere Where : AndWhere)
            if(!(Where instanceof DataWhere)) // если Not то наоборот на False транслируем
                Where.fillJoins(AllJoins);

        for(SourceExpr Property : CompiledProps.values()) // здесь также надо скомпайлить чтобы не пошли лишние Joins
            Property.fillJoins(AllJoins);

        String From = "";
        for(CompiledJoin Join : AllJoins)
            From = Join.getFrom(From,QueryData,AndWhere.means(Join.InJoin),WhereSelect,ExprValues,Syntax);
        if(From.length()==0) From = "dumb";

        // ключи заполняем
        for(Map.Entry<Object,KeyExpr<Object>> MapKey : MapKeys.entrySet())
            KeySelect.put(MapKey.getKey(),QueryData.get(MapKey.getValue()));

        // погнали propertyViews заполнять
        for(Map.Entry<AV,SourceExpr> JoinProp : CompiledProps.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(QueryData, Syntax);
            if(PropertyValue.equals(Type.NULL))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getType());
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        // погнали propertyViews заполнять
        for(Map.Entry<SourceExpr,Boolean> Order : CompiledOrders.entrySet())
            OrderSelect.put(Order.getKey().getSource(QueryData, Syntax),Order.getValue());

        for(IntraWhere Where : SelectWheres)
            WhereSelect.add(Where.getSource(QueryData, Syntax));

        return From;
    }

    LinkedHashMap<Map<Object,Integer>,Map<Object,Object>> executeSelect(DataSession Session,boolean OutSelect) throws SQLException {

        LinkedHashMap<Map<Object,Integer>,Map<Object,Object>> ExecResult = new LinkedHashMap<Map<Object, Integer>, Map<Object, Object>>();
        Statement Statement = Session.Connection.createStatement();

//        System.out.println(getSelect(new ArrayList(),new ArrayList(),Session.Syntax,Top));
        try {
            String Execute = getSelect(new ArrayList(), new ArrayList(), Session.Syntax, Top);
            if(OutSelect)
                System.out.println(Execute);
            ResultSet Result = Statement.executeQuery(Execute);
            try {
                while(Result.next()) {
                    Map<Object,Integer> RowKeys = new HashMap();
                    for(Object Key : Keys)
                        RowKeys.put(Key,Type.Object.read(Result.getObject(getKeyName(Key))));
                    Map<Object,Object> RowProperties = new HashMap();
                    for(Map.Entry<Object,SourceExpr> Property : Properties.entrySet())
                        RowProperties.put(Property.getKey(),
                                Property.getValue().getType().read(Result.getObject(getPropertyName(Property.getKey()))));

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
}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends JoinQuery<K,V> {

    UnionQuery(Collection<? extends K> iKeys, int iDefaultOperator) {
        super(iKeys);
        DefaultOperator = iDefaultOperator;
        Where = new OuterWhere();
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
                OuterWhere Where = new OuterWhere();
                Where.out(new CompareWhere(PrevExpr,Expr,CompareWhere.GREATER));
                Where.out(Expr.getWhere().not());
                Result = new CaseExpr(Where,PrevExpr,Expr);
                break;
            case 1: // SUM CE(Prev.null,New,New.null,Prev,true,New+Prev)
//                Result = new CaseExpr(Expr.getWhere().not(),PrevExpr,new CaseExpr(PrevExpr.getWhere().not(),Expr,new FormulaExpr(PrevExpr,Expr,true)));
                Result = new CaseExpr(Expr,PrevExpr,true);
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

        if(!Coeff.equals(1)) Expr = new FormulaExpr(Expr,Coeff);
        if(PrevExpr==null) return Expr;
        
        if(DefaultOperator>2)
            return new CaseExpr(InJoin,Expr,PrevExpr);
        else
            return getUnionExpr(PrevExpr,Expr,DefaultOperator);
    }

    // добавляем на OR запрос
    void add(Source<? extends K,V> Source,Integer Coeff) {

        Join<K,V> Join = new Join<K,V>((Source<K,V>) Source,this);
        for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.Exprs.entrySet())
            Properties.put(MapExpr.getKey(), getUnionExpr(Properties.get(MapExpr.getKey()),MapExpr.getValue(),Coeff, Join.InJoin));
        ((OuterWhere)Where).out(Join.InJoin);
    }
}