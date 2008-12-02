package platformlocal;

import java.util.*;
import java.sql.SQLException;

class ExprTranslator {

    Map<DataWhere,Where> Wheres;
    Map<ObjectExpr,SourceExpr> Exprs;

    void merge(ExprTranslator MergeTranslated) {
        if(!(MergeTranslated.Wheres.isEmpty() && MergeTranslated.Exprs.isEmpty())) {
            for(Map.Entry<DataWhere,Where> MapTranslate : Wheres.entrySet())
                MapTranslate.setValue(MapTranslate.getValue().translate(MergeTranslated));
            for(Map.Entry<ObjectExpr,SourceExpr> MapTranslate : Exprs.entrySet())
                MapTranslate.setValue(MapTranslate.getValue().translate(MergeTranslated));
            Wheres.putAll(MergeTranslated.Wheres);
            Exprs.putAll(MergeTranslated.Exprs);
        }
    }
}

interface SourceJoin {

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);

    void fillJoins(List<Join> Joins);
}

//  JOIN'ы

class Join<J,U> {
    Source<J,U> Source;
    Map<J,SourceExpr> Joins;
    Map<U,JoinExpr<J,U>> Exprs = new HashMap();
    JoinWhere InJoin;

    // теоретически только для таблиц может быть
    boolean NoAlias = false;

    Join(Source<J,U> iSource) {
        this(iSource,new HashMap());
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

    // возвращает когда в Join'е
    Where getFullWhere() {
        Where Result = InJoin;
        for(SourceExpr Join : Joins.values())
            Result = Result.and(Join.getFullWhere());
        return Result;
    }

    void fillJoins(List<Join> FillJoins) {
        for(SourceExpr Join : Joins.values())
            Join.fillJoins(FillJoins);
        FillJoins.add(this);
    }

    String getFrom(Map<Join, String> JoinAlias, Collection<String> WhereSelect, Map<AndExpr, ValueExpr> ExprValues, SQLSyntax Syntax) {

        // если GroupQuery проталкиваем внутрь ValueExpr'ы, и And в частности KeyExpr'ы внутрь
        Source<J,U> FromSource = null;
        Map<J,SourceExpr> FromJoins;
        if(Source instanceof GroupQuery) {
            FromJoins = new HashMap<J,SourceExpr>();
            // заполняем статичные значения
            Map<J,ValueExpr> MergeKeys = new HashMap();
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
                    FromJoins.put(MapJoin.getKey(),JoinValue);
            }

            if(MergeKeys.size() > 0)
                FromSource = ((GroupQuery)Source).mergeKeyValue(MergeKeys,Joins.keySet());
        } else
            FromJoins = Joins;
        
        if(FromSource==null) FromSource = Source;

        String JoinString = "";
        String SourceString = FromSource.getSource(Syntax);
        String Alias = null;
        if(NoAlias)
            Alias = SourceString;
        else {
            Alias = "t"+(JoinAlias.size()+1);
            SourceString = SourceString + " " + Alias;
        }
        JoinAlias.put(this,Alias);

        for(Map.Entry<J,SourceExpr> KeyJoin : FromJoins.entrySet()) {
            String KeyJoinString = KeyJoin.getValue().getJoin(FromSource.getKeyString(KeyJoin.getKey(),Alias),JoinAlias, Syntax);
            if(KeyJoinString!=null) {
                if(WhereSelect==null)
                    JoinString = (JoinString.length()==0?"":JoinString+" AND ") + KeyJoinString;
                else
                    WhereSelect.add(KeyJoinString);
            }
        }

        return SourceString + (WhereSelect==null?" ON "+(JoinString.length()==0?ObjectWhere.TRUE:JoinString):"");
    }

    void translate(ExprTranslator Translated, ExprTranslator JoinTranslated,Collection<Join> TranslatedJoins) {
        List<CaseMap<J>> CaseList = CaseExpr.translateCase(Joins, Translated);
        if(CaseList==null) // не изменились Joins
            TranslatedJoins.add(this);
        else { // погнали Case'ы делать Join'ы
            // перетранслируем InJoin'ы в OR (And Where And NotWhere And InJoin)
            OrWhere JoinWhere = new OrWhere();
            Map<CaseMap<J>,Join<J,U>> CaseJoins = new HashMap<CaseMap<J>, Join<J, U>>();
            for(CaseMap<J> Case : CaseList) {
                Join<J, U> TranslatedJoin = new Join<J, U>(Source, Case.Map, NoAlias);
                CaseJoins.put(Case, TranslatedJoin);
                TranslatedJoins.add(TranslatedJoin);
                JoinWhere.or(Case.FullWhere.and(TranslatedJoin.InJoin));
            }
            JoinTranslated.Wheres.put(InJoin,JoinWhere);
            // перетранслируем все выражения в CaseWhen'ы
            for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet()) {
                List<Case> TranslatedExpr = new ArrayList<Case>();
                for(CaseMap<J> Case : CaseList)
                    TranslatedExpr.add(new Case(Case.Where,CaseJoins.get(Case).Exprs.get(MapJoin.getKey())));
                JoinTranslated.Exprs.put(MapJoin.getValue(),new CaseExpr(TranslatedExpr));
            }
        }
    }

    <MJ,MU> Join<J, Object> merge(Join<MJ,MU> Merge, ExprTranslator Translated, ExprTranslator MergeTranslated, ExprTranslator JoinTranslated, ExprTranslator JoinMergeTranslated) {
       // нужно построить карту между Joins'ами (с учетом Translate'ов) (точнее и записать в новый Join Translate'утые выражения

        // проверить что кол-во Keys в Source совпадает
        Collection<Map<J, MJ>> MapSet = MapBuilder.buildPairs(Source.Keys, Merge.Source.Keys);
        if(MapSet==null) return null;

        for(Map<J,MJ> MapKeys : MapSet) {
            Map<J,SourceExpr> MergeExprs = new HashMap();
            for(J Key : Source.Keys) {
                SourceExpr MergeExpr = Joins.get(Key).translate(Translated);
                if(!Merge.Joins.get(MapKeys.get(Key)).translate(MergeTranslated).equals(MergeExpr))
                    break;
                else
                    MergeExprs.put(Key,MergeExpr);
            }

            if(MergeExprs.size()!= Source.Keys.size())
                continue;

            // есть уже карта попробуем merge'уть
            Map<MU, Object> MergeProps = new HashMap();
            Source<J, Object> MergeSource = Source.merge(Merge.Source, MapKeys, MergeProps);
            if(MergeSource!=null) {
                // нужно перетранслировать JoinExpr'ы
                Join<J, Object> MergedJoin = new Join<J, Object>(MergeSource, MergeExprs);
                // без каких-либо Where их трансояция еще прокинула
                for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet())
                    JoinTranslated.Exprs.put(MapJoin.getValue(),MergedJoin.Exprs.get(MapJoin.getKey()));
                JoinTranslated.Wheres.put(InJoin,MergedJoin.InJoin);
                for(Map.Entry<MU,JoinExpr<MJ,MU>> MapJoin : Merge.Exprs.entrySet())
                    JoinMergeTranslated.Exprs.put(MapJoin.getValue(),MergedJoin.Exprs.get(MergeProps.get(MapJoin.getKey())));
                JoinTranslated.Wheres.put(Merge.InJoin,MergedJoin.InJoin);
                return MergedJoin;
            }
        }

        return null;
    }

    <K> void pushJoinValues() {
    }
}

class MapJoin<J,U,K> extends Join<J,U> {

    // конструктор когда надо просто ключи протранслировать
    MapJoin(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource) {
        super(iSource);

        for(J Implement : Source.Keys)
            Joins.put(Implement,MapSource.MapKeys.get(iJoins.get(Implement)));
    }

    MapJoin(Source<J,U> iSource,JoinQuery<K,?> MapSource,Map<K,J> iJoins) {
         super(iSource);

         for(K Implement : MapSource.Keys)
             Joins.put(iJoins.get(Implement),MapSource.MapKeys.get(Implement));
     }

}

class UniJoin<K,U> extends Join<K,U> {
    UniJoin(Source<K,U> iSource,JoinQuery<K,?> MapSource) {
        super(iSource);

        for(K Key : Source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }
}

// запрос Join
class JoinQuery<K,V> extends Query<K,V> {
    Map<V,SourceExpr> Properties = new HashMap<V, SourceExpr>();
    OrWhere Where;

    Map<K,KeyExpr<K>> MapKeys = new HashMap<K, KeyExpr<K>>();

    // скомпилированные св-ва
    List<Join> Joins = new ArrayList<Join>();

    void add(V Property,SourceExpr Expr) {
        Expr.fillJoins(Joins);
        Properties.put(Property,Expr);
    }

    public void addAll(Map<? extends V,? extends SourceExpr> AllProperties) {
        for(Map.Entry<? extends V,? extends SourceExpr> MapProp : AllProperties.entrySet())
            add(MapProp.getKey(),MapProp.getValue());
    }

    void add(Where AddWhere) {
        AddWhere.fillJoins(Joins);
        Where = Where.and(AddWhere).getOr();
    }

    JoinQuery() {super();}
    JoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);

        for(K Key : Keys)
            MapKeys.put(Key,new KeyExpr<K>(Key));
    }

    KeyExpr<K> addKey(K Key) {
        Keys.add(Key);
        KeyExpr<K> KeyExpr = new KeyExpr<K>(Key);
        MapKeys.put(Key,KeyExpr);
        return KeyExpr;
    }

    boolean isNotNullProperty(V Property) {
        return (Properties.get(Property) instanceof KeyExpr);
    }

    Collection<V> getProperties() {
        return Properties.keySet();
    }

    Type getType(V Property) {
        return Properties.get(Property).getType();
    }

    void putKeyWhere(Map<K,Integer> KeyValues) {
        for(Map.Entry<K,Integer> MapKey : KeyValues.entrySet())
            add(new CompareWhere(MapKeys.get(MapKey.getKey()),new ValueExpr(MapKey.getValue(),Type.Object),CompareWhere.EQUALS));
    }

    // перетранслирует
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Join> TranslatedJoins) {
        // закинем перекодирование ключей
        for(Map.Entry<K,KeyExpr<K>> MapKey : MapKeys.entrySet())
            Translated.Exprs.put(MapKey.getValue(), Join.Joins.get(MapKey.getKey()).translate(Translated));

        // рекурсивно погнали остальные JoinQuery
        for(Join CompileJoin : Joins)
            CompileJoin.Source.compileJoins(CompileJoin, Translated, TranslatedJoins);

        Where TranslatedWhere = Where.translate(Translated);
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            Translated.Exprs.put(Join.Exprs.get(MapProperty.getKey()),CaseExpr.get(TranslatedWhere,MapProperty.getValue().translate(Translated)));
        Translated.Wheres.put(Join.InJoin,TranslatedWhere);
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        List<Join> CompiledJoins = new ArrayList<Join>();
        Map<V,SourceExpr> CompiledProps = new HashMap<V, SourceExpr>();
        // перетранслируем Where/вытянем следствия
        OrWhere CompiledWhere = compile(CompiledJoins, CompiledProps).getFullWhere().getOr();

        // напишем прообраз выполнения
        // если OrWhere простой
        if(CompiledWhere.size()==1)
            return fillAndSelect(CompiledWhere.iterator().next(), Properties, KeySelect, PropertySelect, WhereSelect, Syntax);

        // OrWhere - сложный придется разбивать на подзапросы и FULL JOIN'ить
        // итак у нас есть CE(W1,E1,W2,E2,… WN,EN,O) соотв. AndExpr - = CE(O), то есть A = <A.getFullWhere(),A>
        // расчитываем тройки <Wi AND !W(0..i-1) AND (если i=n, AND Ei.notNull),Wi,Ei> - которые надо расчитать
        // есть условия на WHERE : AW1 OR AW2 … OR AWN

        Collection<AndJoinQuery> AndProps = new ArrayList<AndJoinQuery>();
        for(AndWhere AndWhere : CompiledWhere)
            AndProps.add(new AndJoinQuery(AndWhere,"f"+AndProps.size()));

        // <нужен список пар <Wi,Ei>, в каки имена отображаются>>
        //есть частный случай когда AndWhere один, тогда
        for(Map.Entry<V,SourceExpr> JoinProp : CompiledProps.entrySet()) {
            String PropertyName = getPropertyName(JoinProp.getKey());

            LinkedHashMap<String,String> CaseWhen = new LinkedHashMap<String,String>();
            String CaseElse = null;
            
            // высчитываем каждый Case
            Where NotWhere = new AndWhere();
            List<Case> Cases = JoinProp.getValue().getCases();
            for(int i=0;i<Cases.size();i++) {
                Case Case = Cases.get(i);
                boolean LastCase = (i==Cases.size()-1);
                // рекурсивно
                Where CheckWhere = Case.Where.and(NotWhere);
                if(LastCase)
                    CheckWhere = CheckWhere.and(Case.Expr.getWhere());

                Collection<AndJoinQuery> ProceedAnd = new ArrayList<AndJoinQuery>(AndProps);
                while(true) {
                    // ищем куда закинуть заодно считаем
                    AndJoinQuery ReadQuery = null;
                    Where RestWhere = null;
                    Iterator<AndJoinQuery> iw = ProceedAnd.iterator();
                    //   ищем Where которое на AndNot FALSE 
                    while(iw.hasNext()) {
                        ReadQuery = iw.next();
                        RestWhere = CheckWhere.and(ReadQuery.Where.not());
                        if(RestWhere.getFullWhere().isFalse()) break;
                    }
                    iw.remove();
                    CheckWhere = RestWhere;

                    boolean LastAnd = (CheckWhere.isFalse() || ProceedAnd.size()==0);
                    if(LastCase && LastAnd) { // если это последний Case то (<Wi,Ei>)
                        ReadQuery.Properties.put(PropertyName,new CaseExpr(Case));
                        CaseElse = ReadQuery.Alias + PropertyName;
                    } else // если из (Wi и AWk) следует (AndNot False) Ei.notNull то (<Wi,Ei> notNull,<Wi,Ei>)
                    if(LastCase || Case.Where.and(ReadQuery.Where).and(Case.Expr.getWhere().not()).getFullWhere().isFalse()) {
                        ReadQuery.Properties.put(PropertyName,new CaseExpr(Case));
                        CaseWhen.put(ReadQuery.Alias+PropertyName+" IS NOT NULL",ReadQuery.Alias+PropertyName);
                    } else { // иначе закидываем <Wi,1> то (<Wi,1> notNull,Ei)
                        ReadQuery.Properties.put(PropertyName+"_w",new CaseExpr(Case.Where,new ValueExpr(1,Type.Bit)));
                        ReadQuery.Properties.put(PropertyName,Case.Expr);
                        CaseWhen.put(ReadQuery.Alias+PropertyName+"_w IS NOT NULL",ReadQuery.Alias+PropertyName);
                    }
                    if(LastAnd) break;
                }
                NotWhere = NotWhere.and(Case.Where.not());
            }

            String PropertySource;
            if(CaseWhen.size()>0) {
                PropertySource = "CASE";
                for(Map.Entry<String,String> Case : CaseWhen.entrySet()) {
                    PropertySource = PropertySource + " WHEN " + Case.getKey() + " THEN "+Case.getValue();
                }
                PropertySource = PropertySource + " ELSE " + CaseElse + " END";
            } else
                PropertySource = CaseElse;
            PropertySelect.put(JoinProp.getKey(),PropertySource);
        }

        // бежим по всем And'ам делаем Join запросы, потом объединяем их FULL'ами
        String From = "";
        for(AndJoinQuery AndQuery : AndProps) {
            Map<K,String> AndKeySelect = new HashMap<K, String>();
            LinkedHashMap<String,String> AndPropertySelect = new LinkedHashMap<String, String>();
            Collection<String> AndWhereSelect = new ArrayList<String>();

            String AndFrom = "(" + getSelectString(fillAndSelect(AndQuery.Where,AndQuery.Properties,AndKeySelect,AndPropertySelect,WhereSelect,Syntax), AndKeySelect, AndPropertySelect, AndWhereSelect, new ArrayList<K>()) + ") "+AndQuery.Alias;
            String AndJoin = "";
            for(K Key : Keys) {
                String PrevKey = KeySelect.get(Key);
                String AndKey = AndQuery.Alias + "." + getKeyName(Key);
                if(PrevKey!=null) {
                    AndJoin = (AndJoin.length()==0?"":AndJoin + " AND ") + AndKey + "=" + PrevKey;
                    AndKey = PrevKey + "," + AndKey;
                }
                KeySelect.put(Key,AndKey);
            }
            if(From.length()==0)
                From = AndFrom;
            else
                From = From + " FULL JOIN " + AndFrom + " ON " + (AndJoin.length()==0?ObjectWhere.TRUE:AndJoin);
        }

        return From;
    }

    private Where compile(List<Join> CompiledJoins, Map<V, SourceExpr> CompiledProps) {

        ExprTranslator Translated = new ExprTranslator();
        List<Join> TranslatedJoins = new ArrayList<Join>();
        // закидываем перекомпиляцию ключей
        for(Join Join : Joins)
            Join.Source.compileJoins(Join, Translated, TranslatedJoins);

        // сбрасываем транслятор
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            CompiledProps.put(MapProperty.getKey(),MapProperty.getValue().translate(Translated));
        Where CompiledWhere = Where.translate(Translated);

        Translated = new ExprTranslator();

        // сливаем Join'ы (если есть те у кого Or дает одно и тоже)
        while(TranslatedJoins.size()>0) {
            Iterator<Join> itOperand = TranslatedJoins.iterator();
            // берем первый элемент, ишем с кем слить
            Join ToMerge = itOperand.next();
            itOperand.remove();

            boolean ToMergeTranslated = false;
            while(itOperand.hasNext()) {
                ExprTranslator JoinTranslated = new ExprTranslator();
                Join Check = itOperand.next();
                Join MergedSource = ToMerge.merge(Check, Translated, Translated, JoinTranslated, JoinTranslated);

                if(MergedSource!=null) {
                    // надо merge'нуть потому как Joins'ы могут быть транслированы
                    Translated.merge(JoinTranslated);

                    ToMergeTranslated = true;
                    itOperand.remove();

                    ToMerge = MergedSource;
                }
            }

            // все равно надо хоть раз протранслировать, потому как Joins могут быть транслированы
            if(ToMergeTranslated)
                CompiledJoins.add(ToMerge);
            else {
                ExprTranslator JoinTranslated = new ExprTranslator();
                ToMerge.translate(Translated, JoinTranslated, CompiledJoins);
                Translated.merge(JoinTranslated);
            }
        }

//        checkTranslate(Translated,CompiledJoins,TranslatedWheres);

        // перетранслируем Properties
        for(Map.Entry<V,SourceExpr> MapProperty : CompiledProps.entrySet())
            MapProperty.setValue(MapProperty.getValue().translate(Translated));
        CompiledWhere = CompiledWhere.translate(Translated);
        return CompiledWhere;
    }

    <AV> String fillAndSelect(AndWhere AndWhere,Map<AV,SourceExpr> AndProps,Map<K, String> KeySelect, Map<AV, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        Map<AndExpr,ValueExpr> ExprValues = new HashMap<AndExpr, ValueExpr>();
        // сначала вычистим избыточные Where из Join'ов и узнаем все Join'ы
        // заодно поищем ExprValues запишем их в Source
        ExprTranslator Translator = new ExprTranslator();
        List<Join> AndJoins = new ArrayList<Join>();
        for(ObjectWhere Where : AndWhere) {
            Where.fillJoins(AndJoins);
            if(Where instanceof DataWhere) {
                Translator.Wheres.put((DataWhere)Where,new AndWhere());
                if(Where instanceof CompareWhere) {// будем считать что всегда в первом операторе
                    CompareWhere Compare = (CompareWhere)Where;
                    if(Compare.Operator2 instanceof ValueExpr && Compare.Compare==CompareWhere.EQUALS)
                        ExprValues.put(Compare.Operator1,(ValueExpr)Compare.Operator2);
                }
            } else // если Not то наоборот на False транслируем
                Translator.Wheres.put(((NotWhere)Where).Where,new OrWhere());
        }

        Map<AV,SourceExpr> CompiledProps = new HashMap<AV,SourceExpr>();
        for(Map.Entry<AV,SourceExpr> JoinProp : AndProps.entrySet()) {
            SourceExpr CompiledProperty = JoinProp.getValue().translate(Translator);
            CompiledProperty.fillJoins(AndJoins);
            CompiledProps.put(JoinProp.getKey(),CompiledProperty);
        }

        // проставим Source'ы в KeyExpr'ы для выполнения
        for(Map.Entry<AndExpr,ValueExpr> KeyValue : ExprValues.entrySet())
            if(KeyValue.getKey() instanceof KeyExpr)
                ((KeyExpr)KeyValue.getKey()).Source = KeyValue.getValue().getString(Syntax);

        Map<Join,String> JoinAlias = new HashMap<Join, String>();
        String From = "";
        // если нету ни одного JoinWhere
        for(Join Join : AndJoins) {
            boolean InJoin = AndWhere.remove(AndJoins.get(0).InJoin);
            if(From.length()==0 && !InJoin) From = "dumb";
            From = (From.length()==0?"":From + (InJoin?"":" LEFT")+" JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null),ExprValues,Syntax);
        }

        // ключи заполняем
        for(Map.Entry<K,KeyExpr<K>> MapKey : MapKeys.entrySet()) {
            if(MapKey.getValue().Source==null)
                throw new RuntimeException("не хватает ключей");
            KeySelect.put(MapKey.getKey(),MapKey.getValue().Source);
        }
        // погнали propertyViews заполнять
        for(Map.Entry<AV,SourceExpr> JoinProp : CompiledProps.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(JoinAlias, Syntax);
            if(PropertyValue.equals(Type.NULL))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getType());
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        for(Where Where : AndWhere)
            WhereSelect.add(Where.getSource(JoinAlias, Syntax));

        // Source'ы в KeyExpr надо сбросить
        for(KeyExpr<K> Key : MapKeys.values())
            Key.Source = null;

        return From;
    }

    public boolean readEmpty(DataSession Session) throws SQLException {
        OrderedJoinQuery<K,V> Cloned = new OrderedJoinQuery<K,V>(Keys);
        Cloned.MapKeys = MapKeys;
        Cloned.Where = Where;
        Cloned.Properties.putAll(Properties);
        Cloned.Joins.addAll(Joins);
        Cloned.Top = 1;
        return Cloned.executeSelect(Session).size()==0;
    }

    <MK, MV> JoinQuery<K, Object> or(JoinQuery<MK, MV> ToMergeQuery, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps) {

        // результат
        JoinQuery<K,Object> MergeQuery = new JoinQuery<K,Object>(Keys);

        // себя Join'им 
        Join<K,V> Join = new UniJoin<K,V>(this,MergeQuery);
        MergeQuery.addAll(Join.Exprs);

        // Merge запрос Join'им
        Join<MK,MV> MergeJoin = new MapJoin<MK,MV,K>(ToMergeQuery,MergeQuery,MergeKeys);
        MergeQuery.addAll(MergeJoin.Exprs);

        // закинем их на Or
        OrWhere Where = new OrWhere();
        Where.or(Join.InJoin);
        Where.or(MergeJoin.InJoin);
        MergeQuery.add(Where);

        Map<Object,SourceExpr> TranslatedProps = new HashMap<Object, SourceExpr>();
        MergeQuery.compile(new ArrayList<Join>(),TranslatedProps);

        // погнали сливать одинаковые (именно из раных)
        for(MV MergeProperty : ToMergeQuery.getProperties()) {
            SourceExpr MergeExpr = TranslatedProps.get(MergeProperty);
            for(V Property : getProperties()) {
                if(TranslatedProps.get(Property).equals(MergeExpr)) {
                    MergeProps.put(MergeProperty,Property);
                    break;
                }
            }
        }

        // выкинем все MergeProps из MergeQuery
        CollectionExtend.removeAll(MergeQuery.Properties,MergeProps.keySet());

        return MergeQuery;
    }
}

class AndJoinQuery {

    AndJoinQuery(AndWhere iWhere, String iAlias) {
        Where = iWhere;
        Alias = iAlias;
    }

    AndWhere Where;
    String Alias;
    Map<String,SourceExpr> Properties = new HashMap<String, SourceExpr>();
}


// с GroupQuery пока неясно
class GroupQuery<B,K extends B,V extends B,F> extends Query<K,V> {
    
    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,V Property,int iOperator) {
        this(iKeys,iFrom,Collections.singletonMap(Property,iOperator));
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,Map<V,Integer> iProperties) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;

        // докидываем условия на NotNull ключей
        for(B Property : From.getProperties())
            From.add(From.Properties.get(Property).getWhere());
    }

    JoinQuery<F,B> From;
    Map<V,Integer> Properties;

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {

        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap<B, String>();
        Collection<String> WhereSelect = new ArrayList<String>();
        String FromSelect = From.fillSelect(new HashMap<F, String>(),FromPropertySelect,WhereSelect, Syntax);

        String GroupBy = "";
        Map<K,String> KeySelect = new HashMap<K, String>();
        Map<V,String> PropertySelect = new HashMap<V, String>();
        for(K Key : Keys) {
            String KeyExpr = FromPropertySelect.get(Key);
            KeySelect.put(Key,KeyExpr);
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
        }
        for(Map.Entry<V,Integer> Property : Properties.entrySet())
            PropertySelect.put(Property.getKey(),(Property.getValue()==0?"MAX":"SUM")+"("+FromPropertySelect.get(Property.getKey())+")");

        return getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder) + (GroupBy.length()==0?"":" GROUP BY "+GroupBy);
    }

    <MK,MV> Source<K, Object> merge(Source<MK, MV> Merge, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps) {
        // если Merge'ся From'ы
        if(!(Merge instanceof GroupQuery)) return null;

        return proceedGroupMerge((GroupQuery)Merge,MergeKeys,MergeProps);
    }

    <MB,MK extends MB,MV extends MB,MF> Source<K, Object> proceedGroupMerge(GroupQuery<MB,MK,MV,MF> MergeGroup, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps) {
            // если Merge'ся From'ы

        if(Keys.size()!=MergeGroup.Keys.size()) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<F, MF>> MapSet = MapBuilder.buildPairs(From.Keys, MergeGroup.From.Keys);
        if(MapSet==null) return null;

        for(Map<F, MF> MapKeys : MapSet) {
            Map<MB,Object> FromMergeProps = new HashMap<MB, Object>();
            JoinQuery<F, Object> MergedFrom = From.or(MergeGroup.From, MapKeys, FromMergeProps);
            // проверим что ключи совпали
            boolean KeyMatch = true;
            for(K Key : Keys)
                KeyMatch = KeyMatch && Key.equals(FromMergeProps.get(MergeKeys.get(Key)));

            if(KeyMatch) {
                Map<Object,Integer> MergedProperties = new HashMap<Object, Integer>(Properties);
                for(Map.Entry<MV,Integer> MergeProp : MergeGroup.Properties.entrySet()) {
                    Object MapProp = FromMergeProps.get(MergeProp.getKey());
                    MergedProperties.put(MapProp,MergeProp.getValue());
                    MergeProps.put(MergeProp.getKey(),MapProp);
                }

                return new GroupQuery<Object, K, Object, F>(Keys, MergedFrom, MergedProperties);
            }
        }

        return null;
    }

    Collection<V> getProperties() {
        return Properties.keySet();
    }

    Type getType(V Property) {
        return From.getType(Property);
    }

    Source<K, V> mergeKeyValue(Map<K, ValueExpr> MergeKeys, Collection<K> CompileKeys) {

        JoinQuery<F,B> Query = new JoinQuery<F,B>(From.Keys);

        Join<F,B> SourceJoin = new UniJoin<F,B>(From,Query);
        Query.add(SourceJoin.InJoin);

        Map<B,SourceExpr> DumbMap = new HashMap();
        // раскидываем по dumb'ам или на выход
        for(Map.Entry<B,JoinExpr<F,B>> Property : SourceJoin.Exprs.entrySet()) {
            ValueExpr MergeValue = MergeKeys.get(Property.getKey());
            if(MergeValue==null)
                Query.add(Property.getKey(),Property.getValue());
            else
                Query.add(new CompareWhere(Property.getValue(),MergeValue,CompareWhere.EQUALS));
        }
        return new GroupQuery<B, K, V, F>(CompileKeys, Query, Properties);
    }
}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends JoinQuery<K,V> {

    UnionQuery(Collection<? extends K> iKeys, int iDefaultOperator) {
        super(iKeys);
        DefaultOperator = iDefaultOperator;
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
                Result = new CaseExpr(Expr.getWhere().and(CompareWhere.get(PrevExpr,Expr,CompareWhere.GREATER)),Expr,PrevExpr);
                break;
            case 1: // SUM CE(Prev.null,New,New.null,Prev,true,New+Prev)
                Result = new CaseExpr(Expr.getWhere().not(),PrevExpr,new CaseExpr(PrevExpr.getWhere().not(),Expr,SourceExpr.getSum(PrevExpr,Expr)));
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

        Expr = SourceExpr.coeff(Expr,Coeff);
        if(PrevExpr==null) return Expr;
        
        if(DefaultOperator>2)
            return new CaseExpr(InJoin,Expr,PrevExpr);
        else
            return getUnionExpr(PrevExpr,Expr,DefaultOperator);
    }

    // добавляем на OR запрос
    void add(Source<? extends K,V> Source,Integer Coeff) {

        Join<K,V> Join = new UniJoin<K,V>((Source<K,V>) Source,this);
        for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.Exprs.entrySet())
            add(MapExpr.getKey(),getUnionExpr(Properties.get(MapExpr.getKey()),MapExpr.getValue(),Coeff,Join.InJoin));
        Where.or(Join.InJoin);
    }
}

