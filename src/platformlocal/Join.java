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
        if(SourceExpr.containsNull(Case.data)) { // если есть null просто все null'им
            Map<U,AndExpr> Exprs = new HashMap<U, AndExpr>();
            for(U Expr : JoinSource.getProperties())
                Exprs.put(Expr,JoinSource.getType(Expr).getExpr(null));
            put(Case,Exprs);
            return Where.FALSE;
        }

        for(CompiledJoin<?> Join : TranslatedJoins) {
            Map<U,JoinExpr> MergeExprs = Join.merge(JoinSource,Case.data);
            if(MergeExprs!=null) {
                put(Case,MergeExprs);
                return Join.inJoin;
            }
        }

        // создаем новый
        CompiledJoin<J> AddJoin = new CompiledJoin<J>((DataSource<J,Object>)JoinSource,Case.data,NoAlias);
        TranslatedJoins.add(AddJoin);
        put(Case, (Map<U,? extends AndExpr>) AddJoin.exprs);
        return AddJoin.inJoin;
    }
}

class Join<J,U>  {
    Source<J,U> source;
    Map<J,SourceExpr> joins;
    Map<U,JoinExpr<J,U>> exprs = new HashMap<U, JoinExpr<J,U>>();
    JoinWhere inJoin;

    // теоретически только для таблиц может быть
    boolean noAlias = false;

    Join(Source<J,U> iSource) {
        this(iSource,new HashMap<J, SourceExpr>());
    }

    Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins) {
        this(iSource,iJoins,false);
    }

    Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins, boolean iNoAlias) {
        source = iSource;
        joins = (Map<J,SourceExpr>) iJoins;
        noAlias = iNoAlias;

        inJoin = new JoinWhere(this);
        for(U property : source.getProperties())
            exprs.put(property,new JoinExpr<J,U>(this,property));
    }

    // конструктор когда надо просто ключи протранслировать
    <K> Join(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource) {
        this(iSource);

        for(J Implement : source.keys)
            joins.put(Implement,MapSource.mapKeys.get(iJoins.get(Implement)));
    }

    <K> Join(Source<J,U> iSource,JoinQuery<K,?> MapSource,Map<K,J> iJoins) {
         this(iSource);

         for(K Implement : MapSource.keys)
             joins.put(iJoins.get(Implement),MapSource.mapKeys.get(Implement));
    }

    <V> Join(Source<J,U> iSource,JoinQuery<J,V> MapSource) {
        this(iSource);

        for(J Key : source.keys)
            joins.put(Key,MapSource.mapKeys.get(Key));
    }

    void addJoin(List<Join> FillJoins) {
        FillJoins.add(this);
    }

    void fillJoins(List<? extends Join> FillJoins, Set<ValueExpr> Values) {
        if(FillJoins.contains(this)) return;

        for(SourceExpr Join : joins.values())
            Join.fillJoins(FillJoins, Values);
        addJoin((List<Join>) FillJoins);
    }

    void translate(ExprTranslator translated, Collection<CompiledJoin> translatedJoins, DataSource<J,U> joinSource) {
        MapCaseList<J> caseList = CaseExpr.translateCase(joins, translated, true, false);

        // перетранслируем InJoin'ы в OR (And Where And NotWhere And InJoin)
        CaseJoins<J,U> caseJoins = new CaseJoins<J,U>(translatedJoins, joinSource, noAlias);
        translated.put(inJoin,caseList.getWhere(caseJoins));
        // перетранслируем все выражения в CaseWhen'ы
        for(Map.Entry<U,JoinExpr<J,U>> mapJoin : exprs.entrySet()) {
            ExprCaseList TranslatedExpr = new ExprCaseList();
            for(MapCase<J> mapCase : caseList) // здесь напрямую потому как MapCaseList уже все проверил
                TranslatedExpr.add(new ExprCase(mapCase.where,caseJoins.get(mapCase).get(mapJoin.getKey())));
            translated.put(mapJoin.getValue(),TranslatedExpr.getExpr(mapJoin.getValue().getType()));
        }
    }

    public String toString() {
        return source.toString();
    }

    // для кэша
    public <EJ,EU> boolean equals(Join<EJ, EU> Join, Map<ValueExpr, ValueExpr> MapValues, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {

        // проверить что кол-во Keys в Source совпадает
        Collection<Map<J,EJ>> MapSet = MapBuilder.buildPairs(source.keys,Join.source.keys);
        if(MapSet==null) return false;

        for(Map<J,EJ> MapKeys : MapSet) {
            boolean Equal = true;
            for(Map.Entry<J,EJ> MapKey : MapKeys.entrySet()) {
                if(!joins.get(MapKey.getKey()).equals(Join.joins.get(MapKey.getValue()), MapExprs, MapWheres)) {
                    Equal = false;
                    break;
                }
            }
            if(!Equal) continue;

            Map<U,EU> MapProperties = new HashMap<U, EU>();
            if(source.equals(Join.source,MapKeys,MapProperties,MapValues)) {
                for(Map.Entry<U,EU> MapProp : MapProperties.entrySet())
                    MapExprs.put(exprs.get(MapProp.getKey()),Join.exprs.get(MapProp.getValue()));
                MapWheres.put(inJoin,Join.inJoin);
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
            for(SourceExpr Join : joins.values())
                Hash += Join.hash();
            Hash += source.hash()*31;
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
        return (DataSource<J,Object>) source;
    }

    <MJ,MU> Map<MU, JoinExpr> merge(DataSource<MJ,MU> MergeSource,Map<MJ,? extends SourceExpr> MergeJoins) {

        // проверить что кол-во Keys в Source совпадает
        Collection<Map<J, MJ>> mapSet = MapBuilder.buildPairs(source.keys,MergeSource.keys);
        if(mapSet==null) return null;

        Map<MU,Object> mergeProps = new HashMap<MU,Object>();
        for(Map<J,MJ> mapKeys : mapSet) {
            if(!BaseUtils.mapEquals(joins,MergeJoins,mapKeys)) // нужны только совпадающие ключи
                continue;

            // есть уже карта попробуем merge'уть
            Source<J, Object> merged = getDataSource().merge(MergeSource, mapKeys, mergeProps);
            if(merged!=null) { // нашли, изменим Source
                source = merged;
                Map<MU,JoinExpr> mergeExprs = new HashMap<MU,JoinExpr>();
                for(Map.Entry<MU,Object> mergeProp : mergeProps.entrySet()) { // докинем недостающие JoinExpr'ы
                    JoinExpr<J,Object> joinExpr = exprs.get(mergeProp.getValue());
                    if(joinExpr ==null) {
                        joinExpr = new JoinExpr<J,Object>(this,mergeProp.getValue());
                        exprs.put(mergeProp.getValue(), joinExpr);
                    }
                    mergeExprs.put(mergeProp.getKey(), joinExpr);
                }
                return mergeExprs;
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
        if(source instanceof GroupQuery) {
            FromJoins = new HashMap<J,SourceExpr>();
            // заполняем статичные значения
            Map<J,ValueExpr> MergeKeys = new HashMap<J, ValueExpr>();
            for(Map.Entry<J,SourceExpr> MapJoin : joins.entrySet()) {
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
                FromSource = (DataSource<J,Object>) ((GroupQuery) source).mergeKeyValue(MergeKeys,FromJoins.keySet());
        } else
            FromJoins = joins;

        if(FromSource==null) FromSource = getDataSource();

        String JoinString = "";
        String SourceString = FromSource.getSource(Syntax, Params);
        String Alias = null;
        if(noAlias)
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
        for(Map.Entry<Object,JoinExpr<J,Object>> JoinExpr : exprs.entrySet())
            QueryData.put(JoinExpr.getValue(),Alias+"."+FromSource.getPropertyName(JoinExpr.getKey()));
        QueryData.put(inJoin,Alias+"."+FromSource.getInSourceName()+" IS NOT NULL");

        if(From.length()==0)
            return SourceString;
        else
            return From + (Inner?"":" LEFT")+" JOIN " + SourceString + " ON "+(JoinString.length()==0?Where.TRUE_STRING :JoinString);
    }

    CompiledJoin<J> translate(ExprTranslator Translated,Map<ValueExpr,ValueExpr> MapValues) {

        Map<J,SourceExpr> TransJoins = new HashMap<J, SourceExpr>();
        for(Map.Entry<J,SourceExpr> MapJoin : joins.entrySet())
            TransJoins.put(MapJoin.getKey(),MapJoin.getValue().translate(Translated));

        CompiledJoin<J> TransJoin = new CompiledJoin<J>(getDataSource().translateValues(MapValues),TransJoins,false);
        Translated.put(inJoin,TransJoin.inJoin);
        for(Map.Entry<Object,JoinExpr<J,Object>> Expr : exprs.entrySet())
            Translated.put(Expr.getValue(),TransJoin.exprs.get(Expr.getKey()));

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
        return new JoinWheres(this,Where.TRUE);
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

    DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet();
        for(SourceExpr Expr : From.joins.values())
            follows.addAll(((AndExpr)Expr).getFollows());
        return follows;
    }

    public SourceExpr getFJExpr() {
        return new CaseExpr(this,Type.Bit.getExpr(true));
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
