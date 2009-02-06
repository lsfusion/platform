package platformlocal;

import java.util.*;

class CaseJoins<J,U> extends HashMap<MapCase<J>,Map<U,? extends AndExpr>> implements CaseWhere<MapCase<J>> {

    Collection<CompiledJoin> TranslatedJoins;
    DataSource<J,U> JoinSource;
    boolean NoAlias;

    CaseJoins(Collection<CompiledJoin> iTranslatedJoins,DataSource<J,U> iJoinSource,boolean iNoAlias) {
        TranslatedJoins = iTranslatedJoins;
        JoinSource = iJoinSource;
        NoAlias = iNoAlias;
    }

    public Where getCaseWhere(MapCase<J> Case) {
        if(SourceExpr.containsNull(Case.Data)) { // если есть null просто все null'им
            Map<U,AndExpr> Exprs = new HashMap<U, AndExpr>();
            for(U Expr : JoinSource.getProperties())
                Exprs.put(Expr,new NullExpr(JoinSource.getType(Expr)));
            put(Case,Exprs);
            return new OrWhere();
        }

        for(CompiledJoin<?> Join : TranslatedJoins) {
            Map<U,JoinExpr> MergeExprs = Join.merge(JoinSource,Case.Data);
            if(MergeExprs!=null) {
                put(Case,MergeExprs);
                return Join.InJoin;
            }
        }

        // создаем новый
        CompiledJoin<J> AddJoin = new CompiledJoin<J>((DataSource<J,Object>)JoinSource,Case.Data,NoAlias);
        TranslatedJoins.add(AddJoin);
        put(Case, (Map<U,? extends AndExpr>) AddJoin.Exprs);
        return AddJoin.InJoin;
    }
}

class Join<J,U>  {
    Source<J,U> Source;
    Map<J,SourceExpr> Joins;
    Map<U,JoinExpr<J,U>> Exprs = new HashMap<U, JoinExpr<J,U>>();
    JoinWhere InJoin;

    // теоретически только для таблиц может быть
    boolean NoAlias = false;

    Join(Source<J,U> iSource) {
        this(iSource,new HashMap<J, SourceExpr>());
    }

    Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins) {
        this(iSource,iJoins,false);
    }

    Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins, boolean iNoAlias) {
        Source = iSource;
        Joins = (Map<J,SourceExpr>) iJoins;
        NoAlias = iNoAlias;

        InJoin = new JoinWhere(this);
        for(U Property : Source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));
    }

    // конструктор когда надо просто ключи протранслировать
    <K> Join(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource) {
        this(iSource);

        for(J Implement : Source.Keys)
            Joins.put(Implement,MapSource.MapKeys.get(iJoins.get(Implement)));
    }

    <K> Join(Source<J,U> iSource,JoinQuery<K,?> MapSource,Map<K,J> iJoins) {
         this(iSource);

         for(K Implement : MapSource.Keys)
             Joins.put(iJoins.get(Implement),MapSource.MapKeys.get(Implement));
    }

    <V> Join(Source<J,U> iSource,JoinQuery<J,V> MapSource) {
        this(iSource);

        for(J Key : Source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }

    void addJoin(List<Join> FillJoins) {
        FillJoins.add(this);
    }

    void fillJoins(List<? extends Join> FillJoins, Set<ValueExpr> Values) {
        if(FillJoins.contains(this)) return;

        for(SourceExpr Join : Joins.values())
            Join.fillJoins(FillJoins, Values);
        addJoin((List<Join>) FillJoins);
    }

    void translate(ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins, DataSource<J,U> JoinSource) {
        MapCaseList<J> CaseList = CaseExpr.translateCase(Joins, Translated, true);

        // перетранслируем InJoin'ы в OR (And Where And NotWhere And InJoin)
        CaseJoins<J,U> CaseJoins = new CaseJoins<J,U>(TranslatedJoins,JoinSource,NoAlias);
        Translated.put(InJoin,CaseList.getWhere(CaseJoins));
        // перетранслируем все выражения в CaseWhen'ы
        for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet()) {
            ExprCaseList TranslatedExpr = new ExprCaseList();
            for(MapCase<J> Case : CaseList) // здесь напрямую потому как MapCaseList уже все проверил
                TranslatedExpr.add(new ExprCase(Case.Where,CaseJoins.get(Case).get(MapJoin.getKey())));
            Translated.put(MapJoin.getValue(),TranslatedExpr.getExpr());
        }
    }

    public String toString() {
        return Source.toString();
    }

    // для кэша
    public <EJ,EU> boolean equals(Join<EJ, EU> Join, Map<ValueExpr, ValueExpr> MapValues, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {

        // проверить что кол-во Keys в Source совпадает
        Collection<Map<J,EJ>> MapSet = MapBuilder.buildPairs(Source.Keys,Join.Source.Keys);
        if(MapSet==null) return false;

        for(Map<J,EJ> MapKeys : MapSet) {
            boolean Equal = true;
            for(Map.Entry<J,EJ> MapKey : MapKeys.entrySet()) {
                if(!Joins.get(MapKey.getKey()).equals(Join.Joins.get(MapKey.getValue()), MapExprs, MapWheres)) {
                    Equal = false;
                    break;
                }
            }
            if(!Equal) continue;

            Map<U,EU> MapProperties = new HashMap<U, EU>();
            if(Source.equals(Join.Source,MapKeys,MapProperties,MapValues)) {
                for(Map.Entry<U,EU> MapProp : MapProperties.entrySet())
                    MapExprs.put(Exprs.get(MapProp.getKey()),Join.Exprs.get(MapProp.getValue()));
                MapWheres.put(InJoin,Join.InJoin);
                return true;
            }
        }

        return false;

    }

    boolean Hashed = false;
    int Hash = 0;
    int hash() {
        if(!Hashed) {
            // нужен симметричный хэш относительно выражений
            for(SourceExpr Join : Joins.values())
                Hash += Join.hash();
            Hash += Source.hash()*31;
            Hashed = true;
        }
        return Hash;
    }
}

class CompiledJoin<J> extends Join<J,Object> {

    CompiledJoin(DataSource<J, Object> iSource, Map<J, ? extends SourceExpr> iJoins, boolean iNoAlias) {
        super(iSource, iJoins, iNoAlias);
    }

    DataSource<J,Object> getDataSource() {
        return (DataSource<J,Object>)Source;
    }

    <MJ,MU> Map<MU, JoinExpr> merge(DataSource<MJ,MU> MergeSource,Map<MJ,? extends SourceExpr> MergeJoins) {

        // проверить что кол-во Keys в Source совпадает
        Collection<Map<J, MJ>> MapSet = MapBuilder.buildPairs(Source.Keys,MergeSource.Keys);
        if(MapSet==null) return null;

        Map<MU,Object> MergeProps = new HashMap<MU,Object>();
        for(Map<J,MJ> MapKeys : MapSet) {
            if(!BaseUtils.mapEquals(Joins,MergeJoins,MapKeys)) // нужны только совпадающие ключи
                continue;

            // есть уже карта попробуем merge'уть
            Source<J, Object> Merged = getDataSource().merge(MergeSource, MapKeys, MergeProps);
            if(Merged!=null) { // нашли, изменим Source
                Source = Merged;
                Map<MU,JoinExpr> MergeExprs = new HashMap<MU,JoinExpr>();
                for(Map.Entry<MU,Object> MergeProp : MergeProps.entrySet()) { // докинем недостающие JoinExpr'ы
                    JoinExpr<J,Object> JoinExpr = Exprs.get(MergeProp.getValue());
                    if(JoinExpr==null) {
                        JoinExpr = new JoinExpr<J,Object>(this,MergeProp.getValue());
                        Exprs.put(MergeProp,JoinExpr);
                    }
                    MergeExprs.put(MergeProp.getKey(),JoinExpr);
                }
                return MergeExprs;
            }
        }
        return null;
    }

    String getFrom(String From, Map<QueryData, String> QueryData, boolean Inner, Collection<String> WhereSelect, Map<AndExpr, ValueExpr> ExprValues, Map<ValueExpr,String> Params, SQLSyntax Syntax) {

        if(From.length()==0 && !Inner)
            From = "dumb";

        // если GroupQuery проталкиваем внутрь ValueExpr'ы, и And в частности KeyExpr'ы внутрь
        DataSource<J,Object> FromSource = null;
        Map<J,SourceExpr> FromJoins;
        if(Source instanceof GroupQuery) {
            FromJoins = new HashMap<J,SourceExpr>();
            // заполняем статичные значения
            Map<J,ValueExpr> MergeKeys = new HashMap<J, ValueExpr>();
            for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet()) {
                ValueExpr JoinValue = null;
                if(MapJoin.getValue() instanceof ValueExpr)
                    JoinValue = (ValueExpr) MapJoin.getValue();
                else {
                    ValueExpr KeyValue = ExprValues.get(MapJoin.getValue());
                    if(KeyValue!=null) JoinValue = KeyValue;
                }
                if(JoinValue!=null)
                    MergeKeys.put(MapJoin.getKey(),JoinValue);
                else
                    FromJoins.put(MapJoin.getKey(),MapJoin.getValue());
            }

            if(MergeKeys.size() > 0)
                FromSource = (DataSource<J,Object>) ((GroupQuery)Source).mergeKeyValue(MergeKeys,FromJoins.keySet());
        } else
            FromJoins = Joins;

        if(FromSource==null) FromSource = getDataSource();

        String JoinString = "";
        String SourceString = FromSource.getSource(Syntax, Params);
        String Alias = null;
        if(NoAlias)
            Alias = SourceString;
        else {
            Alias = "t"+(QueryData.size()+1);
            SourceString = SourceString + " " + Alias;
        }

        for(Map.Entry<J,SourceExpr> KeyJoin : FromJoins.entrySet()) {
            String KeySourceString = Alias + "." + FromSource.getKeyName(KeyJoin.getKey());
            // KeyExpr'а есди не было закинем
            String KeyJoinString = KeyJoin.getValue().getSource(QueryData, Syntax);
            if(KeyJoinString==null) {// значит KeyExpr которого еще не было
                QueryData.put((KeyExpr)KeyJoin.getValue(),KeySourceString);
                if(!Inner)
                    throw new RuntimeException("Не хватает ключей");
            } else {
                KeySourceString = KeySourceString + "=" + KeyJoinString;
                if(From.length()==0)
                    WhereSelect.add(KeySourceString);
                else
                    JoinString = (JoinString.length()==0?"":JoinString+" AND ") + KeySourceString;
            }
        }

        // закинем все Expr'ы и Where
        for(Map.Entry<Object,JoinExpr<J,Object>> JoinExpr : Exprs.entrySet())
            QueryData.put(JoinExpr.getValue(),Alias+"."+FromSource.getPropertyName(JoinExpr.getKey()));
        QueryData.put(InJoin,Alias+"."+FromSource.getInSourceName()+" IS NOT NULL");

        if(From.length()==0)
            return SourceString;
        else
            return From + (Inner?"":" LEFT")+" JOIN " + SourceString + " ON "+(JoinString.length()==0?Where.TRUE:JoinString);
    }

    CompiledJoin<J> translate(ExprTranslator Translated,Map<ValueExpr,ValueExpr> MapValues) {

        Map<J,SourceExpr> TransJoins = new HashMap<J, SourceExpr>();
        for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet())
            TransJoins.put(MapJoin.getKey(),MapJoin.getValue().translate(Translated));

        CompiledJoin<J> TransJoin = new CompiledJoin<J>(getDataSource().translateValues(MapValues),TransJoins,false);
        Translated.put(InJoin,TransJoin.InJoin);
        for(Map.Entry<Object,JoinExpr<J,Object>> Expr : Exprs.entrySet())
            Translated.put(Expr.getValue(),TransJoin.Exprs.get(Expr.getKey()));

        return TransJoin;
    }
}

class JoinWhere extends DataWhere implements JoinData {
    Join<?,?> From;

    JoinWhere(Join iFrom) {
        From=iFrom;
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        From.fillJoins(Joins,Values);
    }

    public Join getJoin() {
        return From;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(this,new AndWhere());
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Joins.add(this,AndWhere);
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return QueryData.get(this);
    }

    public String toString() {
        return "IN JOIN " + From.toString();
    }

    public Where translate(Translator Translator) {
        return Translator.translate(this);
    }

    boolean calculateFollow(DataWhere Where) {
        if(Where==this) return true;
        for(SourceExpr Expr : From.Joins.values())
            if(((AndExpr)Expr).follow(Where)) return true;
        return false;
    }
    Set<DataWhere> getFollows() {
        Set<DataWhere> Follows = new HashSet<DataWhere>();
        Follows.add(this);
        for(SourceExpr Expr : From.Joins.values())
            Follows.addAll(((AndExpr)Expr).getFollows());
        return Follows;
    }

    public SourceExpr getFJExpr() {
        return new CaseExpr(this,new ValueExpr(true,Type.Bit));
    }

    public String getFJString(String FJExpr) {
        return FJExpr + " IS NOT NULL"; 
    }

    public Where copy() {
        return this;
    }

    // для кэша
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return mapWheres.get(this)==where;
    }

    public int getHash() {
        return From.hash();
    }
}
