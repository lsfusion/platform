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

    public Where getCaseWhere(MapCase<J> cCase) {
        if(SourceExpr.containsNull(cCase.data)) { // если есть null просто все null'им
            Map<U,AndExpr> Exprs = new HashMap<U, AndExpr>();
            for(U Expr : JoinSource.getProperties())
                Exprs.put(Expr,JoinSource.getType(Expr).getExpr(null));
            put(cCase,Exprs);
            return Where.FALSE;
        }

        for(CompiledJoin<?> Join : TranslatedJoins) {
            Map<U,JoinExpr> MergeExprs = Join.merge(JoinSource, cCase.data);
            if(MergeExprs!=null) {
                put(cCase,MergeExprs);
                return Join.inJoin;
            }
        }

        // создаем новый
        CompiledJoin<J> AddJoin = new CompiledJoin<J>((DataSource<J,Object>)JoinSource, cCase.data,NoAlias);
        TranslatedJoins.add(AddJoin);
        put(cCase, (Map<U,? extends AndExpr>) AddJoin.exprs);
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
    public <EJ,EU> boolean equals(Join<EJ, EU> join, Map<ValueExpr, ValueExpr> mapValues, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {

        // проверить что кол-во Keys в Source совпадает
        for(Map<J,EJ> mapKeys : new Pairs<J,EJ>(source.keys, join.source.keys)) {
            boolean equal = true;
            for(Map.Entry<J,EJ> mapKey : mapKeys.entrySet()) {
                if(!joins.get(mapKey.getKey()).equals(join.joins.get(mapKey.getValue()), mapExprs, mapWheres)) {
                    equal = false;
                    break;
                }
            }
            if(!equal) continue;

            Map<U,EU> mapProperties = new HashMap<U, EU>();
            if(source.equals(join.source,mapKeys,mapProperties, mapValues)) {
                for(Map.Entry<U,EU> mapProp : mapProperties.entrySet())
                    mapExprs.put(exprs.get(mapProp.getKey()), join.exprs.get(mapProp.getValue()));
                mapWheres.put(inJoin, join.inJoin);
                return true;
            }
        }

        return false;

    }

    boolean hashed = false;
    int hash = 0;
    int hash() {
        if(!hashed) {
            // нужен симметричный хэш относительно выражений
            for(SourceExpr join : joins.values())
                hash += join.hash();
            hash += source.hash()*31;
            hashed = true;
        }
        return hash;
    }
}

class CompiledJoin<J> extends Join<J,Object> {

    CompiledJoin(DataSource<J, Object> iSource, Map<J, ? extends SourceExpr> iJoins, boolean iNoAlias) {
        super(iSource, iJoins, iNoAlias);
    }

    DataSource<J,Object> getDataSource() {
        return (DataSource<J,Object>) source;
    }

    <MJ,MU> Map<MU, JoinExpr> merge(DataSource<MJ,MU> mergeSource,Map<MJ,? extends SourceExpr> mergeJoins) {

        // проверить что кол-во Keys в Source совпадает
        Map<MU,Object> mergeProps = new HashMap<MU,Object>();
        for(Map<J,MJ> mapKeys : new Pairs<J,MJ>(source.keys, mergeSource.keys)) {
            if(!BaseUtils.mapEquals(joins, mergeJoins,mapKeys)) // нужны только совпадающие ключи
                continue;

            // есть уже карта попробуем merge'уть
            Source<J, Object> merged = getDataSource().merge(mergeSource, mapKeys, mergeProps);
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

    String getFrom(String from, Map<QueryData, String> queryData, boolean inner, Collection<String> whereSelect, Map<AndExpr, ValueExpr> exprValues, Map<ValueExpr,String> params, SQLSyntax syntax) {

        if(from.length()==0 && !inner)
            from = "dumb";

        // если GroupQuery проталкиваем внутрь ValueExpr'ы, и And в частности KeyExpr'ы внутрь
        DataSource<J,Object> fromSource = null;
        Map<J,SourceExpr> fromJoins;
        if(source instanceof GroupQuery) {
            fromJoins = new HashMap<J,SourceExpr>();
            // заполняем статичные значения
            Map<J,ValueExpr> mergeKeys = new HashMap<J, ValueExpr>();
            for(Map.Entry<J,SourceExpr> mapJoin : joins.entrySet()) {
                ValueExpr joinValue = null;
                if(mapJoin.getValue() instanceof ValueExpr)
                    joinValue = (ValueExpr) mapJoin.getValue();
                else {
                    ValueExpr keyValue = exprValues.get(mapJoin.getValue());
                    if(keyValue!=null) joinValue = keyValue;
                }
                if(joinValue!=null)
                    mergeKeys.put(mapJoin.getKey(),joinValue);
                else
                    fromJoins.put(mapJoin.getKey(),mapJoin.getValue());
            }

            if(mergeKeys.size() > 0)
                fromSource = (DataSource<J,Object>) ((GroupQuery) source).mergeKeyValue(mergeKeys,fromJoins.keySet());
        } else
            fromJoins = joins;

        if(fromSource==null) fromSource = getDataSource();

        String joinString = "";
        String sourceString = fromSource.getSource(syntax, params);
        String alias = null;
        if(noAlias)
            alias = sourceString;
        else {
            alias = "t"+(queryData.size()+1);
            sourceString = sourceString + " " + alias;
        }

        for(Map.Entry<J,SourceExpr> keyJoin : fromJoins.entrySet()) {
            String keySourceString = alias + "." + fromSource.getKeyName(keyJoin.getKey());
            // KeyExpr'а есди не было закинем
            String keyJoinString = keyJoin.getValue().getSource(queryData, syntax);
            if(Main.debugWatch && keyJoinString==null) // кинем висячий ключ на выход
                keyJoinString = keyJoin.getValue().toString();        

            if(keyJoinString==null) {// значит KeyExpr которого еще не было
                if(!inner)
                    throw new RuntimeException("Не хватает ключей");
                queryData.put((KeyExpr)keyJoin.getValue(),keySourceString);
            } else {
                keySourceString = keySourceString + "=" + keyJoinString;
                if(from.length()==0)
                    whereSelect.add(keySourceString);
                else
                    joinString = (joinString.length()==0?"":joinString+" AND ") + keySourceString;
            }
        }

        // закинем все Expr'ы и Where
        for(Map.Entry<Object,JoinExpr<J,Object>> joinExpr : exprs.entrySet())
            queryData.put(joinExpr.getValue(),alias+"."+fromSource.getPropertyName(joinExpr.getKey()));
        queryData.put(inJoin,alias+"."+fromSource.getInSourceName()+" IS NOT NULL");

        if(from.length()==0)
            return sourceString;
        else
            return from + (inner ?"":" LEFT")+" JOIN " + sourceString + " ON "+(joinString.length()==0?Where.TRUE_STRING :joinString);
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
    Join<?,?> from;

    JoinWhere(Join iFrom) {
        from =iFrom;
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        from.fillJoins(joins, values);
    }

    public Join getJoin() {
        return from;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(this,Where.TRUE);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Joins.add(this,AndWhere);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public String toString() {
        return "IN JOIN " + from.toString();
    }

    public Where translate(Translator translator) {
        return translator.translate(this);
    }

    DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet();
        for(SourceExpr Expr : from.joins.values())
            follows.addAll(((AndExpr)Expr).getFollows());
        return follows;
    }

    public SourceExpr getFJExpr() {
        return new CaseExpr(this,Type.bit.getExpr(true));
    }

    public String getFJString(String exprFJ) {
        return exprFJ + " IS NOT NULL";
    }

    public Where copy() {
        return this;
    }

    // для кэша
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return mapWheres.get(this)==where;
    }

    public int getHash() {
        return from.hash();
    }
}
