/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.math.BigDecimal;

// абстрактный класс источников
abstract class Source<K,V> {

    Collection<K> Keys;

    Source(Collection<? extends K> iKeys) {
        Keys=(Collection<K>)iKeys;
    }
    Source() {Keys=new ArrayList();}

    abstract String getSource(SQLSyntax Syntax);

    String getKeyString(K Key, String Alias) {
        return Alias + "." + getKeyName(Key);
    }

    // проверяет что Property заведомо не null
    boolean isNotNullProperty(V Property) {
        return false;
    }

    String getPropertyString(V Value, String Alias) {
        return Alias + "." + getPropertyName(Value);
    }

    abstract Collection<V> getProperties();

    abstract String getDBType(V Property);

    // по умолчанию fillSelectString вызывает
    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        return fillSelectString(getSource(Syntax),KeySelect,PropertySelect,WhereSelect);
    }

    // заполняет структуру Select'а из строки Source
    String fillSelectString(String Source,Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect) {

        String Alias = "G";
        for(K Key : Keys)
            KeySelect.put(Key,getKeyString(Key,Alias));
        for(V Property : getProperties())
            PropertySelect.put(Property,getPropertyString(Property,Alias));

        return Source+" "+Alias;
    }

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {
        Map<K,String> KeySelect = new HashMap();
        Map<V,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String From = fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

        return getSelectString(From,KeySelect,PropertySelect,WhereSelect, KeyOrder, PropertyOrder);
    }

    // получает строку по которой можно определить входит ли ряд в запрос Select
    String getInSelectString(String Alias) {
        if(Keys.size()>0)
            return getKeyString(Keys.iterator().next(),Alias);
        else
            return Alias + ".subkey";

    }

    String getSelectString(String From, Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, List<K> KeyOrder, List<V> PropertyOrder) {
        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        if(KeySelect.size()==0)
            ExpressionString = "1 AS subkey";

        for(Map.Entry<V,String> Property : PropertySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + getPropertyName(Property.getKey());
            PropertyOrder.add(Property.getKey());
        }

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        return "SELECT " + (ExpressionString.length()==0?"1":ExpressionString) + " FROM " + From + (WhereString.length()==0?"":" WHERE " + WhereString);

    }

    Object convertResult(Object Result) {
        if(Result instanceof BigDecimal)
            return ((BigDecimal)Result).toBigInteger().intValue();
        else
        if(Result instanceof Long)
            return ((Long)Result).intValue();
        else
            return Result;

    }

    // из-за templatов сюда кинем
    LinkedHashMap<Map<K,Integer>,Map<V,Object>> executeSelect(DataSession Session) throws SQLException {

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> ExecResult = new LinkedHashMap();

        Statement Statement = Session.Connection.createStatement();

//        System.out.println(getSelect(new ArrayList(),new ArrayList(), Session.Syntax));
        try {
            ResultSet Result = Statement.executeQuery(getSelect(new ArrayList(),new ArrayList(),Session.Syntax));
            try {
                while(Result.next()) {
                    Map<K,Integer> RowKeys = new HashMap();
                    for(K Key : Keys)
                        RowKeys.put(Key,(Integer)convertResult(Result.getObject(getKeyName(Key))));
                    Map<V,Object> RowProperties = new HashMap();
                    for(V Property : getProperties())
                        RowProperties.put(Property,convertResult(Result.getObject(getPropertyName(Property))));

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
        Collection<String> ResultFields = new ArrayList();
//        System.out.println(Select.GetSelect(ResultFields));
        System.out.println(getSelect(new ArrayList(),new ArrayList(), Session.Syntax));

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> Result = executeSelect(Session);

        for(Map.Entry<Map<K,Integer>,Map<V,Object>> RowMap : Result.entrySet()) {
            for(K Key : Keys) {
                System.out.print(getKeyName(Key)+"-"+RowMap.getKey().get(Key));
                System.out.print(" ");
            }
            System.out.print("---- ");
            for(V Property : getProperties()) {
                System.out.print(RowMap.getValue().get(Property));
                System.out.print(" ");
            }

            System.out.println("");
        }
    }

    // по сути тоже самое что и InsertSelect
    Table createView(DataSession Session,String ViewName,Map<K,KeyField> MapKeys,Map<V,PropertyField> MapProperties) throws SQLException {

        if(1==1) return null;

        Table View = new Table(ViewName);
        for(K Key : Keys) {
            KeyField KeyField = new KeyField(getKeyName(Key),"integer");
            View.Keys.add(KeyField);
            MapKeys.put(Key,KeyField);
        }
        for(V Property : getProperties()) {
            PropertyField PropertyField = new PropertyField(getPropertyName(Property),getDBType(Property));
            View.Properties.add(PropertyField);
            MapProperties.put(Property,PropertyField);
        }

        Session.Execute("CREATE VIEW " + ViewName + " AS " + getSelect(new ArrayList(),new ArrayList(),Session.Syntax));

        return View;
    }

    Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {

        if(this==Merge) {
            for(Map.Entry<K,?> MapKey : MergeKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return null;

            for(V Field : getProperties())
                MergeProps.put(Field,Field);

            return (Source<K, Object>)((Source<K,?>)this);
        } else {
            if(Keys.size()!=Merge.Keys.size()) return null;

            if(Merge instanceof JoinQuery && !(this instanceof JoinQuery))
                // нужно перевернуть MapKeys вызвать proceedMerge у JoinQuery, а затем перевернуть
                return ((JoinQuery) Merge).reverseMerge(this, MergeKeys, MergeProps);

            return proceedMerge(Merge, MergeKeys, MergeProps);
        }
    }

    Source<K, Object> proceedMerge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        // по умолчанию не merge'ся
        return null;
    }

    Map<K,String> KeyNames = new HashMap();
    int KeyCount = 0;

    String getKeyName(K Key) {
        String KeyName = KeyNames.get(Key);
        if(KeyName==null) {
            KeyName = "dkey"+(KeyCount++);
            KeyNames.put(Key,KeyName);
        }

        return KeyNames.get(Key);
    }

    // сделаем кэш Value
    Map<V,String> ValueNames = new HashMap();
    int ValueCount = 0;

    String getPropertyName(V Value) {
        String ValueName = ValueNames.get(Value);
        if(ValueName==null) {
            ValueName = "value"+(ValueCount++);
            ValueNames.put(Value,ValueName);
        }
        return ValueName;
    }

    // записывается в Join'ы
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {
        
        TranslatedJoins.add(Join.translate(Translated, Translated, Outer));
    }

    // возвращает источник без заданных выражений, но только с объектами им равными (короче устанавливает фильтр)
    Source<K,V> mergePropertyValue(Map<? extends V, Integer> MergeValues) {
        
        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Join<K,V> SourceJoin = new UniJoin<K,V>(this,Query,true);
        Map<V,SourceExpr> DumbMap = new HashMap();
        // раскидываем по dumb'ам или на выход
        for(Map.Entry<V,JoinExpr<K,V>> Property : SourceJoin.Exprs.entrySet()) {
            if(MergeValues.containsKey(Property.getKey()))
                DumbMap.put(Property.getKey(),Property.getValue());
            else
                Query.add(Property.getKey(),Property.getValue());
        }
        Query.add((new Join<V,Object>(new DumbSource<V,Object>((Map<V,Integer>) MergeValues, new HashMap()),DumbMap, true)).InJoin);
        return Query.compile();
    }

    // возвращает источник с "проставленными" ключами со значениями 
    Source<K, V> mergeKeyValue(Map<K, Integer> MergeKeys,Collection<K> CompileKeys) {

        JoinQuery<K,V> Query = new JoinQuery<K,V>(CompileKeys);
        Join<K,V> SourceJoin = new Join<K,V>(this,true);
        for(Map.Entry<K,SourceExpr> MapKey : Query.MapKeys.entrySet())
            SourceJoin.Joins.put(MapKey.getKey(),MapKey.getValue());
        for(Map.Entry<K,Integer> MapKey : MergeKeys.entrySet())
            SourceJoin.Joins.put(MapKey.getKey(),new ValueSourceExpr(MapKey.getValue()));
        Query.addAll(SourceJoin.Exprs);
        return Query.compile();
    }

    // оборачивает Source в JoinQuery 
    Source<K, V> getJoinQuery() {
        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Join<K,V> SourceJoin = new UniJoin<K,V>(this,Query,true);
        Query.addAll(SourceJoin.Exprs);
        return Query.compile();
    }

    Source<K, V> compile() {
        return this;
    }
}

// постоянный источник из одной записи
class DumbSource<K,V> extends Source<K,V> {

    Map<K,Integer> ValueKeys;
    Map<V,Object> Values;

    DumbSource(Map<K,Integer> iValueKeys,Map<V,Object> iValues) {
        super(iValueKeys.keySet());
        ValueKeys = iValueKeys;
        Values = iValues;
    }

    String getSource(SQLSyntax Syntax) {
        return "dumb";
    }

    String getKeyString(K Key,String Alias) {
        return ValueKeys.get(Key).toString();
    }

    String getPropertyString(V Property,String Alias) {
        Object Value = Values.get(Property);
        if(Value instanceof String)
            return "'" + Value + "'";
        else
            return Value.toString();

    }

    Collection<V> getProperties() {
        return Values.keySet();
    }

    String getDBType(V Property) {
        Object ObjectValue = Values.get(Property);
        if(ObjectValue==null)
            throw new RuntimeException();

        if(ObjectValue instanceof Integer)
            return "integer";
        else
            return "char(50)";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        for(Map.Entry<K,Integer> ValueKey : ValueKeys.entrySet())
            KeySelect.put(ValueKey.getKey(),ValueKey.getValue().toString());
        for(Map.Entry<V,Object> Value : Values.entrySet())
            PropertySelect.put(Value.getKey(),(Value.getValue()==null?"NULL":(Value.getValue() instanceof String?"'"+Value.getValue()+"'":Value.getValue().toString())));

        return "dumb";
    }

    void outSelect(DataSession Session) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        if(Join.Inner) {
            ExprTranslator JoinTranslated = new ExprTranslator();
            for(Map.Entry<K,Integer> ValueKey : ValueKeys.entrySet()) {
                SourceExpr JoinExpr = Join.Joins.get(ValueKey.getKey()).translate(Translated);
                JoinTranslated.put(JoinExpr,new ValueSourceExpr(ValueKey.getValue()));
                // если св-во также докидываем в фильтр
                if(!(JoinExpr instanceof KeyExpr))
                    JoinWheres.add(new FieldExprCompareWhere(JoinExpr,ValueKey.getValue(),0));
            }
            Translated.merge(JoinTranslated);

            // все свои значения также затранслируем
            for(Map.Entry<V,Object> MapValue : Values.entrySet())
                Translated.put(Join.Exprs.get(MapValue.getKey()),ValueSourceExpr.getExpr(MapValue.getValue(),"integer"));

            // после чего дальше даже не компилируемся - proceedCompile словит транслированные ключи
        } else
            super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }
}

class EmptySource<K> extends Source<K,Object> {

    Object NullValue = new Object();
    String DBType;

    EmptySource(Collection<? extends K> iKeys,String iDBType) {
        super(iKeys);
        DBType = iDBType;
    }

    String getSource(SQLSyntax Syntax) {
        return "empty";
    }

    Collection<Object> getProperties() {
        List<Object> Result = new ArrayList();
        Result.add(NullValue);
        return Result;
    }

    String getDBType(Object Property) {
        return DBType;  //To change body of implemented methods use File | Settings | File Templates.
    }

    String getKeyString(K Key, String Alias) {
        return "NULL";
    }
}
// таблицы\Views

class Field {
    String Name;
    String Type;

    Field(String iName,String iType) {Name=iName;Type=iType;}

    String GetDeclare(SQLSyntax Syntax) {
        return Name + " " + Syntax.convertType(Type);
    }
}

class KeyField extends Field {
    KeyField(String iName,String iType) {super(iName,iType);}
}

class PropertyField extends Field {
    PropertyField(String iName,String iType) {super(iName,iType);}
}

class Table extends Source<KeyField,PropertyField> {
    String Name;
    Collection<PropertyField> Properties = new ArrayList();

    Table(String iName) {Name=iName;}

    String getSource(SQLSyntax Syntax) {
        return Name;
    }

    Set<List<PropertyField>> Indexes = new HashSet();

    Collection<PropertyField> getProperties() {
        return Properties;
    }

    String getDBType(PropertyField Property) {
        return Property.Type;
    }

    String getKeyName(KeyField Key) {
        return Key.Name;
    }

    String getPropertyName(PropertyField Property) {
        return Property.Name;
    }
}

// временная таблица на момент сессии
class SessionTable extends Table {

    SessionTable(String iName) {
        super(iName);
    }

    String getSource(SQLSyntax Syntax) {
        return Syntax.getSessionTableName(Name);
    }

}

abstract class Query<K,V> extends Source<K,V> {

    Query(Collection<? extends K> iKeys) {
        super(iKeys);
    }

    Query() {
        super();
    }

    String getSource(SQLSyntax Syntax) {
        return "(" + getSelect(new ArrayList(),new ArrayList(), Syntax) + ")";
    }

    // скомпилирован ли запрос
    Source<K,V> Compiled = null;

    Source<K, V> compile() {
        if(Compiled==null)
            Compiled = proceedCompile();

        return Compiled;
    }

    // компилирует - запрос - приводит к выполняемой структуре, оптимизирует
    abstract Source<K, V> proceedCompile();
}

class Join<J,U> {
    Source<J,U> Source;
    Map<J,SourceExpr> Joins;

    boolean Inner;

    // теоретически только для таблиц может быть
    boolean NoAlias = false;

    SourceWhere InJoin;

    Map<U,JoinExpr<J,U>> Exprs = new HashMap();

    Join(Source<J,U> iSource,boolean iInner) {
        Source = iSource;
        Inner = iInner;

        InJoin = new JoinWhere(this);

        for(U Property : Source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));

        Joins = new HashMap();
    }

    Join(Source<J,U> iSource,Map<J,SourceExpr> iJoins,boolean iInner) {
        Source = iSource;
        for(U Property : Source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));

        Joins = iJoins;
        Inner = iInner;

        InJoin = new JoinWhere(this);
    }

    void fillJoins(List<Join> FillJoins, Set<SourceWhere> Wheres) {
        if(!FillJoins.contains(this)) {
            for(SourceExpr Join : Joins.values())
                Join.fillJoins(FillJoins,Wheres);

            FillJoins.add(this);
        }
    }

    String getFrom(Map<Join, String> JoinAlias, Collection<String> WhereSelect, SQLSyntax Syntax) {
        String JoinString = "";
        String SourceString = Source.getSource(Syntax);
        String Alias = null;
        if(NoAlias)
            Alias = SourceString;
        else {
            Alias = "t"+(JoinAlias.size()+1);
            SourceString = SourceString + " " + Alias;
        }
        JoinAlias.put(this,Alias);

        for(Map.Entry<J,SourceExpr> KeyJoin : Joins.entrySet()) {
            String KeyJoinString = KeyJoin.getValue().getJoin(Source.getKeyString(KeyJoin.getKey(),Alias),JoinAlias, Syntax);
            if(KeyJoinString!=null) {
                if(WhereSelect==null)
                    JoinString = (JoinString.length()==0?"":JoinString+" AND ") + KeyJoinString;
                else
                    WhereSelect.add(KeyJoinString);
            }
        }

        return SourceString + (WhereSelect==null?" ON "+(JoinString.length()==0?"1=1":JoinString):"");
    }

    Join<J,U> translate(ExprTranslator Translated, ExprTranslator JoinTranslated,boolean ForceOuter) {
        boolean ChangedJoins = false;
        Map<J,SourceExpr> TransMap = new HashMap();
        for(Map.Entry<J,SourceExpr> Join : Joins.entrySet()) {
            SourceExpr TransJoin = Join.getValue().translate(Translated);
            TransMap.put(Join.getKey(), TransJoin);
            ChangedJoins = ChangedJoins || (TransJoin!=Join.getValue());
        }

        if(!ChangedJoins)
            return this;
        else {
            Join<J,U> TranslatedJoin = new Join<J,U>(Source,TransMap,Inner && !ForceOuter);
            TranslatedJoin.NoAlias = NoAlias;
            fillJoinTranslate(TranslatedJoin, null, JoinTranslated);
            return TranslatedJoin;
        }
    }

    // заполняет перетрансляцию к новому Join'у
    void fillJoinTranslate(Join<?,?> TranslatedJoin,Map<Object,Object> MergeProps, ExprTranslator JoinTranslated) {

        for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet())
            JoinTranslated.put(MapJoin.getValue(),TranslatedJoin.Exprs.get(MergeProps==null?MapJoin.getKey():MergeProps.get(MapJoin.getKey())));
        JoinTranslated.put(InJoin,TranslatedJoin.InJoin);        
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr JoinValue : Joins.values())
            JoinValue.fillObjectExprs(Exprs);
    }

    Join<J, Object> merge(Join<?, ?> Merge, ExprTranslator Translated, ExprTranslator MergeTranslated, ExprTranslator JoinTranslated, ExprTranslator JoinMergeTranslated) {
        // нужно построить карту между Joins'ами (с учетом Translate'ов) (точнее и записать в новый Join Translate'утые выражения

        // проверить что кол-во Keys в Source совпадает

        Collection<Map<J, Object>> MapSet = (new MapBuilder<J, Object>()).buildPairs(Source.Keys, Merge.Source.Keys);
        if(MapSet==null) return null;

        for(Map<J,?> MapKeys : MapSet) {
            Map<J,SourceExpr> MergeExprs = new HashMap();
            for(J Key : Source.Keys) {
                SourceExpr MergeExpr = Joins.get(Key).translate(Translated);
                if(!Merge.Joins.get(MapKeys.get(Key)).translate(MergeTranslated).equals(MergeExpr))
                    break;
                else
                    MergeExprs.put(Key,MergeExpr);
            }

            if(MergeExprs.size()!=Source.Keys.size())
                continue;

            // есть уже карта попробуем merge'уть
            Map<Object, Object> MergeProps = new HashMap();
            Source<J, Object> MergeSource = Source.merge(Merge.Source, MapKeys, MergeProps);
            if(MergeSource!=null) {
                // нужно перетранслировать JoinExpr'ы
                Join<J, Object> MergedJoin = new Join<J, Object>(MergeSource, MergeExprs, Inner || Merge.Inner);
                fillJoinTranslate(MergedJoin, null, JoinTranslated);
                Merge.fillJoinTranslate(MergedJoin, MergeProps, JoinMergeTranslated);
                return MergedJoin;
            }
        }

        return null;
    }

    public String fillSingleSelect(Collection<String> WhereSelect, SQLSyntax Syntax, Map<SourceExpr, String> KeyValues) {

        Set<KeyExpr> JoinExprs = new HashSet(KeyValues.keySet());
        // проверим сначала что в Join'ах только не пересекающиеся keyExpr'ы
        for(SourceExpr JoinExpr : Joins.values())
            if(!(JoinExpr instanceof KeyExpr) || !JoinExprs.add((KeyExpr) JoinExpr))
                return null;

        // для этого получаем fillSelect' Join'а
        // в JoinExpr'ах подменяем SourceString'и на PropertySelect
        // в KeyExpr'ах на KeySelect
        Map<J,String> KeySelect = new HashMap();
        Map<U,String> PropertySelect = new HashMap();
        String From = Source.fillSelect(KeySelect,PropertySelect,WhereSelect,Syntax);
        // если там не KeyExpr, то уже глюк
        for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet())
            ((KeyExpr)MapJoin.getValue()).Source = KeySelect.get(MapJoin.getKey());
        // закинем KeyValues
        for(Map.Entry<SourceExpr,String> KeyValue : KeyValues.entrySet())
            ((KeyExpr)KeyValue.getKey()).Source = KeyValue.getValue();
        for(Map.Entry<U,JoinExpr<J,U>> MapExpr : Exprs.entrySet())
            MapExpr.getValue().StringSource = PropertySelect.get(MapExpr.getKey());

        return From;
    }

    void pushJoinValues() {

        if(Source instanceof Query) {
            // заполняем статичные значения
            Map<J,Integer> MergeKeys = new HashMap();
            for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet())
                if(MapJoin.getValue() instanceof ValueSourceExpr)
                    MergeKeys.put(MapJoin.getKey(), (Integer) ((ValueSourceExpr)MapJoin.getValue()).Value);

            if(MergeKeys.size() > 0) {
                CollectionExtend.removeAll(Joins,MergeKeys.keySet());
                Source = Source.mergeKeyValue(MergeKeys,Joins.keySet());
            }
        }
    }
}

class MapJoin<J,U,K> extends Join<J,U> {

    // конструктор когда надо просто ключи протранслировать
    MapJoin(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource,boolean iInner) {
        super(iSource,iInner);

        for(J Implement : Source.Keys)
            Joins.put(Implement,MapSource.MapKeys.get(iJoins.get(Implement)));
    }

    MapJoin(Source<J,U> iSource,JoinQuery<K,?> MapSource,Map<K,J> iJoins,boolean iInner) {
         super(iSource,iInner);

         for(K Implement : MapSource.Keys)
             Joins.put(iJoins.get(Implement),MapSource.MapKeys.get(Implement));
     }

}

class UniJoin<K,U> extends Join<K,U> {
    UniJoin(Source<K,U> iSource,JoinQuery<K,?> MapSource,boolean iInner) {
        super(iSource,iInner);

        for(K Key : Source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }
}

abstract class TranslateExpr<TranslateType extends TranslateExpr> {

    // транслирует (рекурсивно инстанцирует объекты с новыми входными параметрами)
    abstract TranslateType translate(ExprTranslator Translated);
}

abstract class SourceJoin<TranslateType extends SourceJoin> extends TranslateExpr<TranslateType> {

    abstract public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
    
    abstract void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres);
    abstract void fillObjectExprs(Collection<ObjectExpr> Exprs);

    public TranslateType translate(ExprTranslator Translated) {
        TranslateType Translate = (TranslateType) Translated.get(this);
        if(Translate==null) {
            Translate = proceedTranslate(Translated);
            Translated.put(this,Translate);
            return Translate;
        }

        return Translate;
    }

    abstract TranslateType proceedTranslate(ExprTranslator Translated);
}

// абстрактный класс выражений
abstract class SourceExpr extends SourceJoin<SourceExpr> {

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return KeySource + "=" + getSource(JoinAlias, Syntax);
    }

    abstract String getDBType();
}

abstract class ObjectExpr extends SourceExpr {
    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Exprs.add(this);
    }
}

class KeyExpr<K> extends ObjectExpr {
    K Key;

    KeyExpr(K iKey) {Key=iKey;}

    String Source;
    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source;
    }

    void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(Source!=null) return super.getJoin(KeySource,JoinAlias, Syntax);

        Source = KeySource;
        return null;
    }

    String getDBType() {
        return "integer";
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }
}

class JoinExpr<J,U> extends ObjectExpr {
    U Property;
    Join<J,U> From;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        From = iFrom;
        Property = iProperty;
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        From.fillJoins(Joins,Wheres);
    }

    // для fillSingleSelect'а
    String StringSource = null;
    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(StringSource!=null) return StringSource;
        
        return From.Source.getPropertyString(Property,JoinAlias.get(From));
    }

    String getDBType() {
        return From.Source.getDBType(Property);
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }
}

// формулы
class ValueSourceExpr extends SourceExpr {

    Object Value;
    ValueSourceExpr(Object iValue) {
        Value=iValue;
    }

    String getStringValue() {
        if(Value instanceof String)
            return "'" + Value + "'";
        else
            return Value.toString();
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getStringValue();
    }

    void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    String getDBType() {
        if(Value instanceof Integer)
            return "integer";
        else
            return "char(50)";
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueSourceExpr that = (ValueSourceExpr) o;

        if (Value != null ? !Value.equals(that.Value) : that.Value != null) return false;

        return true;
    }

    public int hashCode() {
        return (Value != null ? Value.hashCode() : 0);
    }

    static SourceExpr getExpr(Object Value,String DBType) {
        return (Value==null?new StaticNullSourceExpr(DBType):new ValueSourceExpr(Value));
    }
}

class StaticNullSourceExpr extends SourceExpr {

    String DBType;
    StaticNullSourceExpr(String iDBType) {
        DBType = iDBType;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    String getDBType() {
        return DBType;
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StaticNullSourceExpr that = (StaticNullSourceExpr) o;

        if (!DBType.equals(that.DBType)) return false;

        return true;
    }

    public int hashCode() {
        return DBType.hashCode();
    }
}

class CoeffExpr<T> {
    T Expr;
    Integer Coeff;

    String getCoeff() {
        return (Coeff==null || Coeff.equals(1)?"":(Coeff.equals(-1)?"-":Coeff.toString()+"*"));
    }

    CoeffExpr(T iExpr, Integer iCoeff) {
        Expr = iExpr;
        Coeff = iCoeff;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoeffExpr coeffExpr = (CoeffExpr) o;

        if (Coeff != null ? !Coeff.equals(coeffExpr.Coeff) : coeffExpr.Coeff != null) return false;
        if (!Expr.equals(coeffExpr.Expr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Expr.hashCode();
        result = 31 * result + (Coeff != null ? Coeff.hashCode() : 0);
        return result;
    }
}

class ListCoeffExpr<T> extends ArrayList<CoeffExpr<T>> {
    void put(T Expr,Integer Coeff) {
        add(new CoeffExpr<T>(Expr,Coeff));
    }
}

class UnionSourceExpr extends SourceExpr {

    UnionSourceExpr(int iOperator) {Operator=iOperator;Operands= new ListCoeffExpr<SourceExpr>();}
    UnionSourceExpr(int iOperator,ListCoeffExpr<SourceExpr> iOperands) {Operator=iOperator;Operands=iOperands;}

    // 0 - MAX
    // 1 - +
    // 2 - ISNULL
    int Operator;
    ListCoeffExpr<SourceExpr> Operands;

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        ListCoeffExpr<String> StringOperands = new ListCoeffExpr<String>();
        for(CoeffExpr<SourceExpr> Operand : Operands)
            StringOperands.put(Operand.Expr.getSource(JoinAlias, Syntax),Operand.Coeff);

        return UnionQuery.getExpr(StringOperands,Operator,false, Syntax);
    }

    String getDBType() {
        return Operands.iterator().next().Expr.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        boolean ChangedOperands = false;
        ListCoeffExpr<SourceExpr> TransOperands = new ListCoeffExpr<SourceExpr>();
        for(CoeffExpr<SourceExpr> Operand : Operands) {
            SourceExpr TransOperand = Operand.Expr.translate(Translated);
            TransOperands.put(TransOperand,Operand.Coeff);
            ChangedOperands = ChangedOperands || TransOperand != Operand.Expr;
        }

        if(!ChangedOperands)
            return this;
        else
            return new UnionSourceExpr(Operator,TransOperands);
    }


    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        for(CoeffExpr<SourceExpr> Operand : Operands)
            Operand.Expr.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(CoeffExpr<SourceExpr> Operand : Operands)
            Operand.Expr.fillObjectExprs(Exprs);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnionSourceExpr that = (UnionSourceExpr) o;

        if (!Operands.equals(that.Operands)) return false;

        return true;
    }

    public int hashCode() {
        return Operands.hashCode();
    }
}

class SelectWhereOperand extends TranslateExpr<SelectWhereOperand> {
    SourceWhere Where;
    SourceExpr Expr;

    SelectWhereOperand(SourceWhere iWhere, SourceExpr iExpr) {
        Where = iWhere;
        Expr = iExpr;
    }

    SelectWhereOperand translate(ExprTranslator Translated) {
        SourceWhere TranslatedWhere = Where.translate(Translated);
        SourceExpr TranslatedExpr = Expr.translate(Translated);
        if(TranslatedWhere.equals(Where) && TranslatedExpr.equals(Expr))
            return this;
        else
            return new SelectWhereOperand(TranslatedWhere, TranslatedExpr);
    }
}

class SelectWhereExpr extends SourceExpr {
    ListCoeffExpr<SelectWhereOperand> Operands;

    SelectWhereExpr() {
        Operands = new ListCoeffExpr<SelectWhereOperand>();
    }

    SelectWhereExpr(ListCoeffExpr<SelectWhereOperand> iOperands) {
        Operands = iOperands;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String Source = "";
        for(CoeffExpr<SelectWhereOperand> Operand : Operands) {
            String SourceExpr = Operand.Expr.Expr.getSource(JoinAlias, Syntax);
            if(Source.length()==0)
                Source = SourceExpr;
            else
                Source = CaseWhenSourceExpr.getExpr(Operand.Expr.Where.getSource(JoinAlias, Syntax), SourceExpr, Source, Syntax);
        }

        return Source;
    }

    void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        for(CoeffExpr<SelectWhereOperand> Operand : Operands) {
            Operand.Expr.Expr.fillJoins(Joins, Wheres);
            Operand.Expr.Where.fillJoins(Joins, Wheres);
        }
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(CoeffExpr<SelectWhereOperand> Operand : Operands) {
            Operand.Expr.Expr.fillObjectExprs(Exprs);
            Operand.Expr.Where.fillObjectExprs(Exprs);
        }
    }

    SourceExpr proceedTranslate(ExprTranslator Translated) {

        boolean ChangedOperands = false;
        ListCoeffExpr<SelectWhereOperand> TranslatedOperands = new ListCoeffExpr<SelectWhereOperand>();
        for(CoeffExpr<SelectWhereOperand> Operand : Operands) {
            SelectWhereOperand TranslatedOperand = Operand.Expr.translate(Translated);
            if(TranslatedOperand.equals(Operand.Expr))
                TranslatedOperands.add(Operand);
            else {
                ChangedOperands = true;
                TranslatedOperands.put(TranslatedOperand,Operand.Coeff);
            }
        }

        if(!ChangedOperands)
            return this;
        else
            return new SelectWhereExpr(TranslatedOperands);
    }

    String getDBType() {
        return Operands.get(0).Expr.Expr.getDBType();
    }
}

class NullEmptySourceExpr extends SourceExpr {

    NullEmptySourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Expr.fillJoins(Joins, Wheres);    //To change body of overridden methods use File | Settings | File Templates.
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(Expr.getSource(JoinAlias, Syntax),(Expr.getDBType().equals("integer")?"0":"''"), false);
    }

    String getDBType() {
        return Expr.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransExpr = Expr.translate(Translated);
        if(TransExpr==Expr)
            return this;
        else
            return new NullEmptySourceExpr(TransExpr);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullEmptySourceExpr that = (NullEmptySourceExpr) o;

        if (!Expr.equals(that.Expr)) return false;

        return true;
    }

    public int hashCode() {
        return Expr.hashCode();
    }
}

class NullSourceExpr extends SourceExpr {

    NullSourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Expr.fillJoins(Joins, Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    String getDBType() {
        return Expr.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransExpr = Expr.translate(Translated);
        if(TransExpr==Expr)
            return this;
        else
            return new NullSourceExpr(TransExpr);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullSourceExpr that = (NullSourceExpr) o;

        if (!Expr.equals(that.Expr)) return false;

        return true;
    }

    public int hashCode() {
        return Expr.hashCode();
    }
}

class CaseWhenSourceExpr extends SourceExpr {

    CaseWhenSourceExpr(SourceWhere iWhere,SourceExpr iTrueExpr,SourceExpr iFalseExpr) {
        Where=iWhere; TrueExpr=iTrueExpr; FalseExpr=iFalseExpr;
    }
    
    SourceWhere Where;
    SourceExpr TrueExpr;
    SourceExpr FalseExpr;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Where.fillJoins(Joins,Wheres);
        TrueExpr.fillJoins(Joins,Wheres);
        FalseExpr.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> FillExprs) {
        Where.fillObjectExprs(FillExprs);
        TrueExpr.fillObjectExprs(FillExprs);
        FalseExpr.fillObjectExprs(FillExprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        return getExpr(Where.getSource(JoinAlias, Syntax),TrueExpr.getSource(JoinAlias, Syntax),FalseExpr.getSource(JoinAlias, Syntax),Syntax);
    }

    static String getExpr(String WhereSource,String TrueSource,String FalseSource,SQLSyntax Syntax) {

        if(TrueSource.equals("NULL") && FalseSource.equals("NULL"))
            return "NULL";
        else
            return "(CASE WHEN " + WhereSource + " THEN " + TrueSource + " ELSE " + FalseSource + " END)";
    }

    String getDBType() {
        return TrueExpr.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {

        SourceWhere TransWhere = Where.translate(Translated);
        SourceExpr TransTrueExpr = TrueExpr.translate(Translated);
        SourceExpr TransFalseExpr = FalseExpr.translate(Translated);
        if(TransWhere==Where && TransTrueExpr==TrueExpr && TransFalseExpr==FalseExpr)
            return this;
        else
            return new CaseWhenSourceExpr(TransWhere, TransTrueExpr, TransFalseExpr);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CaseWhenSourceExpr that = (CaseWhenSourceExpr) o;

        if (!FalseExpr.equals(that.FalseExpr)) return false;
        if (!TrueExpr.equals(that.TrueExpr)) return false;
        if (!Where.equals(that.Where)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Where.hashCode();
        result = 31 * result + TrueExpr.hashCode();
        result = 31 * result + FalseExpr.hashCode();
        return result;
    }
}

class IsNullSourceExpr extends SourceExpr {

    IsNullSourceExpr(SourceExpr iPrimaryExpr,SourceExpr iSecondaryExpr) {PrimaryExpr=iPrimaryExpr;SecondaryExpr=iSecondaryExpr;};
    
    SourceExpr PrimaryExpr;
    SourceExpr SecondaryExpr;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        PrimaryExpr.fillJoins(Joins,Wheres);
        SecondaryExpr.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        PrimaryExpr.fillObjectExprs(Exprs);
        SecondaryExpr.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(PrimaryExpr.getSource(JoinAlias, Syntax),SecondaryExpr.getSource(JoinAlias, Syntax), false);
    }

    String getDBType() {
        return PrimaryExpr.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransPrimaryExpr = PrimaryExpr.translate(Translated);
        SourceExpr TransSecondaryExpr = SecondaryExpr.translate(Translated);
        if(TransPrimaryExpr==PrimaryExpr && TransSecondaryExpr==SecondaryExpr)
            return this;
        else
            return new IsNullSourceExpr(TransPrimaryExpr, TransSecondaryExpr);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IsNullSourceExpr that = (IsNullSourceExpr) o;

        if (!PrimaryExpr.equals(that.PrimaryExpr)) return false;
        if (!SecondaryExpr.equals(that.SecondaryExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = PrimaryExpr.hashCode();
        result = 31 * result + SecondaryExpr.hashCode();
        return result;
    }
}

class FormulaSourceExpr extends SourceExpr {

    FormulaSourceExpr(String iFormula) {
        Formula = iFormula;
        Params = new HashMap<String,SourceExpr>();
    }

    FormulaSourceExpr(String iFormula,Map<String,SourceExpr> iParams) {
        Formula = iFormula;
        Params = iParams;
    }
    
    String Formula;

    Map<String,SourceExpr> Params;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Param : Params.values())
            Param.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String SourceString = Formula;
        Iterator<String> i = Params.keySet().iterator();

        while (i.hasNext()) {
            String Prm = i.next();
            SourceString = SourceString.replace(Prm,Params.get(Prm).getSource(JoinAlias, Syntax));
        }

         return SourceString;
     }

    String getDBType() {
        return Params.values().iterator().next().getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        boolean ChangedParams = false;
        Map<String,SourceExpr> TransParams = new HashMap();
        for(Map.Entry<String,SourceExpr> Prm : Params.entrySet()) {
            SourceExpr TransParam = Prm.getValue().translate(Translated);
            TransParams.put(Prm.getKey(), TransParam);
            ChangedParams = ChangedParams || TransParam!=Prm.getValue();
        }

        if(!ChangedParams)
            return this;
        else
            return new FormulaSourceExpr(Formula,TransParams); 
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaSourceExpr that = (FormulaSourceExpr) o;

        if (!Formula.equals(that.Formula)) return false;
        if (!Params.equals(that.Params)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Formula.hashCode();
        result = 31 * result + Params.hashCode();
        return result;
    }
}

// от формулы отличается чисто чтобы не умножать на 1
class MultiplySourceExpr extends SourceExpr {

    Collection<SourceExpr> Operands;

    MultiplySourceExpr() {
        Operands = new ArrayList();
    }

    MultiplySourceExpr(Collection<SourceExpr> iOperands) {
        Operands = iOperands;
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        for(SourceExpr Operand : Operands)
            Operand.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Operand : Operands)
            Operand.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String Source = "";
        for(SourceExpr Operand : Operands) {
            String OperandSource = Operand.getSource(JoinAlias, Syntax);
            if(!OperandSource.equals("1"))
                Source = (Source.length()==0?"":Source+"*") + OperandSource;
        }

        if(Source.length()==0)
            return "1";
        else
            return Source;
    }

    String getDBType() {
        return Operands.iterator().next().getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        boolean ChangedOperands = false;
        Collection<SourceExpr> TransOperands = new ArrayList();
        for(SourceExpr Operand : Operands) {
            SourceExpr TransOperand = Operand.translate(Translated);
            TransOperands.add(TransOperand);
            ChangedOperands = ChangedOperands || TransOperand!=Operand;
        }

        if(!ChangedOperands)
            return this;
        else
            return new MultiplySourceExpr(TransOperands);

    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiplySourceExpr that = (MultiplySourceExpr) o;

        if (!Operands.equals(that.Operands)) return false;

        return true;
    }

    public int hashCode() {
        return Operands.hashCode();
    }
}

// SourceExpr возвращаюший 1 если FormulaWhere true и Null в противном случае
class FormulaWhereSourceExpr extends SourceExpr {

    FormulaSourceWhere FormulaWhere;
    boolean NotNull;

    FormulaWhereSourceExpr(FormulaSourceWhere iFormulaWhere,boolean iNotNull) {
        FormulaWhere = iFormulaWhere;
        NotNull = iNotNull;
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        FormulaWhere.fillJoins(Joins,Wheres);
        if(NotNull)
            Wheres.add(FormulaWhere);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        FormulaWhere.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        // если NotNull нефиг 2 раза проверять
        if(NotNull)
            return "1";
        else
            return "CASE WHEN " + FormulaWhere.getSource(JoinAlias, Syntax) + " THEN 1 ELSE NULL END";
    }

    String getDBType() {
        return FormulaWhere.getDBType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        FormulaSourceWhere TransExpr = (FormulaSourceWhere) FormulaWhere.translate(Translated);
        if(TransExpr== FormulaWhere)
            return this;
        else
            return new FormulaWhereSourceExpr(TransExpr,NotNull);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaWhereSourceExpr that = (FormulaWhereSourceExpr) o;

        if (NotNull != that.NotNull) return false;
        if (!FormulaWhere.equals(that.FormulaWhere)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = FormulaWhere.hashCode();
        result = 31 * result + (NotNull ? 1 : 0);
        return result;
    }
}


// Wheres
abstract class SourceWhere extends SourceJoin<SourceWhere> {

    public abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
}

interface Where {
}

class JoinWhere extends SourceWhere {
    Join From;

    JoinWhere(Join iFrom) {
        From=iFrom;
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        From.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        From.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(From.Inner)
            return "1=1";
        else
            return "NOT " + From.Source.getInSelectString(JoinAlias.get(From)) + " IS NULL";
    }

    SourceWhere proceedTranslate(ExprTranslator Translated) {
        return this;
    }
}

class TrueWhere extends SourceWhere {

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "1=1";
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        return this;
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
    }
}

class FieldExprCompareWhere extends SourceWhere {

    SourceExpr Source;
    Object Value;
    int Compare;

    FieldExprCompareWhere(SourceExpr iSource,Object iValue,int iCompare) {Source=iSource;Value=iValue;Compare=iCompare;}

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source.getSource(JoinAlias, Syntax) + (Compare==0?"=":(Compare==1?">":(Compare==2?"<":(Compare==3?">=":(Compare==4?"<=":"<>"))))) + (Value instanceof String?"'"+Value+"'":(Value instanceof SourceExpr?((SourceExpr)Value).getSource(JoinAlias, Syntax):Value.toString()));
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {

        SourceExpr TransSource = Source.translate(Translated);
        Object TransValue = Value instanceof SourceExpr ? ((SourceExpr) Value).translate(Translated) : Value;

        if(TransSource==Source && TransValue==Value)
            return this;
        else
            return new FieldExprCompareWhere(TransSource,TransValue,Compare);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Source.fillObjectExprs(Exprs);
        if(Value instanceof SourceExpr)
            ((SourceExpr)Value).fillObjectExprs(Exprs);
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Source.fillJoins(Joins,Wheres);
        if(Value instanceof SourceExpr)
            ((SourceExpr)Value).fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldExprCompareWhere that = (FieldExprCompareWhere) o;

        if (Compare != that.Compare) return false;
        if (!Source.equals(that.Source)) return false;
        if (!Value.equals(that.Value)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + Value.hashCode();
        result = 31 * result + Compare;
        return result;
    }
}

class SourceIsNullWhere extends SourceWhere {
    SourceIsNullWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return (Not?"NOT ":"") + Source.getSource(JoinAlias, Syntax) + " IS NULL";
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Source.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransSource = Source.translate(Translated);
        if(TransSource==Source)
            return this;
        else
            return new SourceIsNullWhere(TransSource,Not);
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Source.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourceIsNullWhere that = (SourceIsNullWhere) o;

        if (Not != that.Not) return false;
        if (!Source.equals(that.Source)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + (Not ? 1 : 0);
        return result;
    }
}

class FieldOPWhere extends SourceWhere {
    FieldOPWhere(SourceWhere iOp1,SourceWhere iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    SourceWhere Op1, Op2;
    boolean And;

    static SourceWhere getExpr(Collection<SourceWhere> Wheres, boolean And) {
        // делаем OR на InJoin'ы этих Source'ов
        SourceWhere AllWheres = null;
        for(SourceWhere Where : Wheres) {
            if(AllWheres==null)
                AllWheres = Where;
            else
                AllWheres = new FieldOPWhere(Where, AllWheres, false);
        }

        if(AllWheres==null) {
            AllWheres = new TrueWhere();
            if(And)
                return new NotWhere(AllWheres);
        }

        return AllWheres;
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Op1.fillObjectExprs(Exprs);
        Op2.fillObjectExprs(Exprs);
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Op1.fillJoins(Joins,Wheres);
        Op2.fillJoins(Joins,Wheres);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "(" + Op1.getSource(JoinAlias, Syntax) + " " + (And?"AND":"OR") + " " + Op2.getSource(JoinAlias, Syntax) + ")";
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        SourceWhere TransOp1 = Op1.translate(Translated);
        SourceWhere TransOp2 = Op2.translate(Translated);
        if(TransOp1==Op1 && TransOp2==Op2)
            return this;
        else
            return new FieldOPWhere(TransOp1, TransOp2,And);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldOPWhere that = (FieldOPWhere) o;

        if (And != that.And) return false;
        if (!Op1.equals(that.Op1)) return false;
        if (!Op2.equals(that.Op2)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Op1.hashCode();
        result = 31 * result + Op2.hashCode();
        result = 31 * result + (And ? 1 : 0);
        return result;
    }
}

class NotWhere extends SourceWhere {

    SourceWhere Where;

    NotWhere(SourceWhere iWhere) {
        Where = iWhere;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NOT " + Where.getSource(JoinAlias, Syntax);
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        SourceWhere TranslatedWhere = Where.translate(Translated);
        if(TranslatedWhere.equals(Where))
            return this;
        else
            return new NotWhere(TranslatedWhere);
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Where.fillJoins(Joins, Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Where.fillObjectExprs(Exprs);
    }
}

class FieldSetValueWhere extends SourceWhere {

    FieldSetValueWhere(SourceExpr iExpr,Collection<Integer> iSetValues) {Expr=iExpr; SetValues=iSetValues;};
    SourceExpr Expr;
    Collection<Integer> SetValues;

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String ListString = "";
        for(Integer Value : SetValues)
            ListString = (ListString.length()==0?"":ListString+',') + Value;

        return Expr.getSource(JoinAlias, Syntax) + " IN (" + ListString + ")";
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransExpr = Expr.translate(Translated);
        if(TransExpr==Expr)
            return this;
        else
            return new FieldSetValueWhere(TransExpr,SetValues);
    }

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        Expr.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldSetValueWhere that = (FieldSetValueWhere) o;

        if (!Expr.equals(that.Expr)) return false;
        if (!SetValues.equals(that.SetValues)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Expr.hashCode();
        result = 31 * result + SetValues.hashCode();
        return result;
    }
}

class FormulaSourceWhere extends SourceWhere {

    FormulaSourceWhere(String iFormula) {
        Formula = iFormula;
        Params = new HashMap<String,SourceExpr>();
    }

    FormulaSourceWhere(String iFormula,Map<String,SourceExpr> iParams) {
        Formula = iFormula;
        Params = iParams;
    }

    String Formula;

    Map<String,SourceExpr> Params;

    public void fillJoins(List<Join> Joins, Set<SourceWhere> Wheres) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Param : Params.values())
            Param.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String SourceString = Formula;
        Iterator<String> i = Params.keySet().iterator();

        while (i.hasNext()) {
            String Prm = i.next();
            SourceString = SourceString.replace(Prm,Params.get(Prm).getSource(JoinAlias, Syntax));
        }

         return SourceString;
     }

    String getDBType() {
        return Params.values().iterator().next().getDBType();
    }

    public SourceWhere proceedTranslate(ExprTranslator Translated) {
        boolean ChangedParams = false;
        Map<String,SourceExpr> TransParams = new HashMap();
        for(Map.Entry<String,SourceExpr> Prm : Params.entrySet()) {
            SourceExpr TransParam = Prm.getValue().translate(Translated);
            TransParams.put(Prm.getKey(), TransParam);
            ChangedParams = ChangedParams || TransParam!=Prm.getValue();
        }

        if(!ChangedParams)
            return this;
        else
            return new FormulaSourceWhere(Formula,TransParams);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaSourceWhere that = (FormulaSourceWhere) o;

        if (!Formula.equals(that.Formula)) return false;
        if (!Params.equals(that.Params)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Formula.hashCode();
        result = 31 * result + Params.hashCode();
        return result;
    }
}

abstract class SelectQuery<K,V> extends Query<K,V> {

    SelectQuery() {super();}
    SelectQuery(Collection<? extends K> iKeys) {super(iKeys);}

}

class ExprTranslator extends HashMap<SourceJoin,SourceJoin> {

    void merge(ExprTranslator MergeTranslated) {
        if(MergeTranslated.size()>0) {
            for(Map.Entry<SourceJoin,SourceJoin> MapTranslate : entrySet())
                MapTranslate.setValue(MapTranslate.getValue().translate(MergeTranslated));
            putAll(MergeTranslated);
        }
    }
}

// запрос Join
class JoinQuery<K,V> extends SelectQuery<K,V> {
    protected Map<V,SourceExpr> Properties = new HashMap();
    protected Set<SourceWhere> Wheres = new HashSet();

    Map<K,SourceExpr> MapKeys = new HashMap();

    // скомпилированные св-ва
    List<Join> Joins = new ArrayList();

    void add(V Property,SourceExpr Expr) {
        // сразу же Join'ы дополнительные Wheres закинем
        Expr.fillJoins(Joins,Wheres);
        Properties.put(Property,Expr);
    }

    public void addAll(Map<V,? extends SourceExpr> AllProperties) {
        for(Map.Entry<V,? extends SourceExpr> MapProp : AllProperties.entrySet())
            add(MapProp.getKey(),MapProp.getValue());
    }

    void add(SourceWhere Where) {

        Where.fillJoins(Joins,Wheres);

        // если JoinWhere то он нафиг не нужен в Where 
        if(!(Where instanceof JoinWhere && ((JoinWhere)Where).From.Inner))
            Wheres.add(Where);
    }

    private Map<SourceExpr,String> KeyValues = new HashMap();

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

    String getDBType(V Property) {
        return Properties.get(Property).getDBType();
    }

    void putDumbJoin(Map<K,Integer> KeyValues) {

        Join<K,Object> DumbJoin = new Join<K,Object>(new DumbSource<K,Object>(KeyValues,new HashMap()),true);
        for(K Object : KeyValues.keySet())
            DumbJoin.Joins.put(Object,MapKeys.get(Object));
        add(DumbJoin.InJoin);
    }

    void checkExprs(Collection<ObjectExpr> Exprs,List<Join> TranslatedJoins) {
        for(ObjectExpr Expr : Exprs) {
            if(Expr instanceof KeyExpr) {
                if(!MapKeys.values().contains(Expr))
                    throw new RuntimeException("Wrong KeyExpr");
            } else {
                if(!TranslatedJoins.contains(((JoinExpr)Expr).From))
                    throw new RuntimeException("Wrong JoinExpr");
            }
        }

    }
    // для тестирования проверяет что все транлировалось нормально
    void checkTranslate(ExprTranslator Translated,List<Join> TranslatedJoins) {
        // проверяем SourceWheres
        Set<ObjectExpr> NewExprs;
        for(Join TransJoin : TranslatedJoins) {
            NewExprs = new HashSet();
            TransJoin.fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
        }

        for(SourceExpr PropertyValue : Properties.values()) {
            NewExprs = new HashSet();
            PropertyValue.translate(Translated).fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
        }

        for(SourceWhere Where : Wheres) {
            NewExprs = new HashSet();
            Where.translate(Translated).fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
        }
    }


    Source<K, V> proceedCompile() {

        // из всех Properties,Wheres вытягивают Joins, InnerJoins, SourceWhere        
        ExprTranslator Translated = new ExprTranslator();
        List<Join> TranslatedJoins = new ArrayList();

        for(Join Join : Joins) {
            Join.Source = Join.Source.compile();
            Join.Source.compileJoins(Join, Translated, Wheres, TranslatedJoins, false);
        }

//        checkTranslate(Translated,TranslatedJoins);

        // слитые операнды
        List<Join> MergedJoins = new ArrayList();

        // если есть 2 (compatible) Join объединяем - заменяем все 1-й и 2-й JoinExpr'ы на JoinExpr'ы результата
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
            if(!ToMergeTranslated) {
                ExprTranslator JoinTranslated = new ExprTranslator();
                ToMerge = ToMerge.translate(Translated, JoinTranslated, false);
                Translated.merge(JoinTranslated);
            }

            MergedJoins.add(ToMerge);
        }

//        checkTranslate(Translated,MergedJoins);

        // перетранслируем все
        Joins = MergedJoins;
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            MapProperty.setValue(MapProperty.getValue().translate(Translated));
        Set<SourceWhere> TranslatedWheres = new HashSet();
        for(SourceWhere Where : Wheres)
            TranslatedWheres.add(Where.translate(Translated));
        Wheres = TranslatedWheres;

        // протолкнем внутрь значения
        for(Join Join : Joins)
            Join.pushJoinValues();

        // проверим если вдруг Translat'ился ключ закинем сразу в Source
        KeyValues = new HashMap();
        for(SourceExpr Key : MapKeys.values()) {
            SourceExpr ValueKey = Key.translate(Translated);
            if(Key!=ValueKey)
                KeyValues.put(Key,((ValueSourceExpr)ValueKey).getStringValue());
        }

        return this;
    }

    private boolean recMerge(ListIterator<Join> RecJoin, Collection<Join> ToMergeJoins, ExprTranslator Translated, ExprTranslator MergeTranslated, Set<Join> ProceededJoins, Set<Join> MergedJoins, List<Join> TranslatedJoins) {
        if(!RecJoin.hasNext())
            return true;

        Join<?,?> CurrentJoin = RecJoin.next();

        for(Join<?,?> ToMergeJoin : ToMergeJoins)
            if(!MergedJoins.contains(ToMergeJoin) && CurrentJoin.Inner==ToMergeJoin.Inner) {
                // надо бы проверить что Join уже не merge'ут
                ExprTranslator JoinTranslated = new ExprTranslator();
                ExprTranslator JoinMergeTranslated = new ExprTranslator();
                Join<?,?> MergedJoin = CurrentJoin.merge(ToMergeJoin, Translated, MergeTranslated, JoinTranslated, JoinMergeTranslated);
                if(MergedJoin!=null) {
                    ProceededJoins.add(CurrentJoin);
                    MergedJoins.add(ToMergeJoin);
                    TranslatedJoins.add(MergedJoin);
                    Translated.putAll(JoinTranslated);
                    MergeTranslated.putAll(JoinMergeTranslated);
                    if(recMerge(RecJoin, ToMergeJoins, Translated, MergeTranslated, ProceededJoins, MergedJoins, TranslatedJoins))
                        return true;
                    ProceededJoins.remove(CurrentJoin);
                    MergedJoins.remove(ToMergeJoin);
                    TranslatedJoins.remove(MergedJoin);
                    CollectionExtend.removeAll(Translated,JoinTranslated.keySet());
                    CollectionExtend.removeAll(MergeTranslated,JoinMergeTranslated.keySet());
                }
            }

        if(!CurrentJoin.Inner && recMerge(RecJoin, ToMergeJoins, Translated, MergeTranslated, ProceededJoins, MergedJoins, TranslatedJoins))
            return true;

        RecJoin.previous();

        return false;
    }

    Source<K, Object> proceedMerge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        // Set<Join> merge'им с Set<Join> если совпадают, докидываем Left Joins (пытаясь попарно слить), проверяем на совпадение SourceWheres
        // для сравнения Join'ов (как и Where) нужны сразу же "перекодированные" выражения									так как сами бежим по Map'ам то перекодируем
        // накапливаем карты слияний , по умолчанию в этот Map закидываем KeyExpr'ы, затем они накапливаются
        // также надо затем слить Property причем сравнив чтобы не дать одинаковые

        int InnerCount = 0;
        for(Join Join : Joins)
            if(Join.Inner) InnerCount++;

        // если один иннер оператор и нету Source'ов
        if(!(Merge instanceof SelectQuery) && InnerCount==1 && Wheres.size()==0)
            return proceedMerge(Merge.getJoinQuery(),MergeKeys,MergeProps);

        if(!(Merge instanceof JoinQuery)) return null;

        JoinQuery<?,?> MergeJoin = (JoinQuery<?,?>) Merge;

        // также надо бы проверить на кол-во Inner Join'ов, а также SourceWherers совпадает
        if(Wheres.size()!=MergeJoin.Wheres.size()) return null;

        int InnerMergeCount = 0;
        for(Join Join : MergeJoin.Joins)
            if(Join.Inner) InnerMergeCount++;
        if(InnerCount!=InnerMergeCount) return null;

        JoinQuery<K,Object> Result = new JoinQuery<K,Object>(Keys);
        Result.Compiled = Result;

        // попытаемся все ключи помапить друг на друга
        Set<Join> ProceededJoins = new HashSet();
        Set<Join> MergedJoins = new HashSet();
        // замапим ключи
        ExprTranslator Translated = new ExprTranslator();
        ExprTranslator MergeTranslated = new ExprTranslator();
        for(Map.Entry<K,SourceExpr> MapKey : Result.MapKeys.entrySet()) {
            Translated.put(MapKeys.get(MapKey.getKey()),MapKey.getValue());
            MergeTranslated.put(MergeJoin.MapKeys.get(MergeKeys.get(MapKey.getKey())),MapKey.getValue());
        }
        if(recMerge(Joins.listIterator(), MergeJoin.Joins, Translated, MergeTranslated, ProceededJoins, MergedJoins, Result.Joins)) {
            // пробежимся по всем необработанным Join'ам и Translate'им их
            for(Join Join : Joins)
                if(!ProceededJoins.contains(Join))
                    Result.Joins.add(Join.translate(Translated,Translated,false));
            for(Join Join : MergeJoin.Joins)
                if(!MergedJoins.contains(Join))
                    Result.Joins.add(Join.translate(MergeTranslated,MergeTranslated,false));

            // проверим что SourceWheres совпадают (потом может закинем в recMerge)
            for(SourceWhere Where : Wheres)
                Result.Wheres.add(Where.translate(Translated));

            for(SourceWhere Where : MergeJoin.Wheres)
                if(!Result.Wheres.contains(Where.translate(MergeTranslated)))
                    return null;

            // сделаем Map в обратную сторону, чтобы не кидать
            Map<SourceExpr,Object> BackProperties = new HashMap();
            for(Map.Entry<V,SourceExpr> MapProp : Properties.entrySet()) {
                SourceExpr TransExpr = MapProp.getValue().translate(Translated);

                Result.Properties.put(MapProp.getKey(),TransExpr);
                BackProperties.put(TransExpr,MapProp.getKey());
            }
            for(Map.Entry<?,SourceExpr> MapProp : MergeJoin.Properties.entrySet()) {
                SourceExpr TransExpr = MapProp.getValue().translate(MergeTranslated);

                Object PropertyObject = BackProperties.get(TransExpr);
                if(PropertyObject==null) {
                    PropertyObject = new Object();
                    Result.Properties.put(PropertyObject,TransExpr);
                }
                MergeProps.put(MapProp.getKey(),PropertyObject);
            }

            return Result;
        }

        return null;
    }

    // перетранслирует
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        // если есть JoinQuery (пока если Left без Wheres) то доливаем все Join'ы с сооты. Map'ами, JoinExpr заменяем на SourceExpr'ы с перебитыми KeyExpr'ами и соотв. JoinRxpr'ами
        Outer = Outer || !Join.Inner;
        if((!Outer || Wheres.size()==0)) {
            // закинем перекодирование ключей
            for(Map.Entry<K,SourceExpr> MapKey : MapKeys.entrySet())
                Translated.put(MapKey.getValue(),Join.Joins.get(MapKey.getKey()).translate(Translated));

            // нужно также рекурсию гнать на случай LJ (A J (B FJ C))
            for(Join CompileJoin : Joins)
                CompileJoin.Source.compileJoins(CompileJoin, Translated, JoinWheres, TranslatedJoins, Outer);

            JoinWheres.addAll(Wheres);

            // InJoin'ы заменим на AND Inner InJoin'ов
            List<SourceWhere> CompileInJoin = new ArrayList();
            for(Join CompileJoin : Joins)
                if(CompileJoin.Inner)
                    CompileInJoin.add(CompileJoin.InJoin);
            Translated.put(Join.InJoin,FieldOPWhere.getExpr(CompileInJoin,true).translate(Translated));
            // хакинем перекодирование значение
            for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
                Translated.put(Join.Exprs.get(MapProperty.getKey()),MapProperty.getValue().translate(Translated));
        } else
            super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }

    // заполняет Select для одного Join'а
    String fillSingleSelect(Join<?,?> SingleJoin,Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        if(!SingleJoin.Inner) return null;

        String From = SingleJoin.fillSingleSelect(WhereSelect,Syntax,KeyValues);
        if(From==null) return null;

        fillSources(null,KeySelect,PropertySelect,WhereSelect,Syntax);

        // скинем Source'ы
        for(JoinExpr<?, ?> MapExpr : SingleJoin.Exprs.values())
            MapExpr.StringSource = null;

        return From;
    }

    // считывает с заполненными ключами\алиасами значения
    void fillSources(Map<Join,String> JoinAlias,Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        // проверим что все KeyExpr'ы заполнились
        // формально надо отдельно от Left Join'ов проверять но например в JoinProperty при инкрементных изменениях нужно именно Left Join ключей
        for(SourceExpr Key : MapKeys.values())
            if(((KeyExpr<K>)Key).Source==null)
                throw new RuntimeException("не хватает ключей");

        // ключи заполняем
        for(Map.Entry<K,SourceExpr> MapKey : MapKeys.entrySet())
            KeySelect.put(MapKey.getKey(),((KeyExpr<K>)MapKey.getValue()).Source);
        // погнали Properties заполнять
        for(Map.Entry<V,SourceExpr> JoinProp : Properties.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(JoinAlias, Syntax);
            if(PropertyValue.equals("NULL"))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getDBType());
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        for(SourceWhere Where : Wheres)
            WhereSelect.add(Where.getSource(JoinAlias, Syntax));

        // Source'ы в KeyExpr надо сбросить
        for(SourceExpr Key : MapKeys.values())
            ((KeyExpr<K>)Key).Source = null;
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        if(compile()!=this) return compile().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

        // если у нас ровно один Inner Join, то возьмем за основу его fillSelect
        if(Joins.size()==1) {
            String SingleFrom = fillSingleSelect(Joins.iterator().next(), KeySelect, PropertySelect, WhereSelect, Syntax);
            if(SingleFrom!=null)
                return SingleFrom;
        }

        // закинем KeyValues
        for(Map.Entry<SourceExpr,String> KeyValue : KeyValues.entrySet())
            ((KeyExpr<K>)KeyValue.getKey()).Source = KeyValue.getValue();

        Map<Join,String> JoinAlias = new HashMap();
        String From = "";
        // теперь определим KeyExpr'ы заодно строя Join'ы
        for(Join Join : Joins)
            if(Join.Inner)
                From = (From.length()==0?"":From + " JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null), Syntax);

        if(From.length()==0)
            From = "dumb";

        Collection<KeyExpr<K>> InnerNullKeys = new ArrayList();
        for(SourceExpr Key : MapKeys.values())
            if(((KeyExpr<K>)Key).Source==null)
                InnerNullKeys.add((KeyExpr<K>)Key);

        for(Join Join : Joins)
            if(!Join.Inner)
                From = From + " LEFT JOIN " + Join.getFrom(JoinAlias,null, Syntax);

        // нужно доставать ключи из LEFT JOIN'а и кидать на NOT NULL
        for(KeyExpr<K> NullKey : InnerNullKeys)
            WhereSelect.add("NOT "+NullKey.Source+" IS NULL");

        fillSources(JoinAlias, KeySelect, PropertySelect, WhereSelect, Syntax);

        return From;
    }

    public Source<?,Object> reverseMerge(Source<?,?> Merge, Map<?,K> MergeKeys, Map<Object, Object> MergeProps) {

        Map<K,Object> ReverseKeys = new HashMap();
        for(Map.Entry<?,K> MapKey : MergeKeys.entrySet())
            ReverseKeys.put(MapKey.getValue(),MapKey.getKey());

        Map<Object,Object> ReverseProps = new HashMap();
        Source<K, Object> ReverseSource = proceedMerge(Merge, ReverseKeys, ReverseProps);
        if(ReverseSource==null) return null;

        // создадим JoinQuery для перекодирования ключей
        JoinQuery<?,Object> MergedSource = new JoinQuery<Object,Object>(Merge.Keys);
        Join<K,Object> ReverseJoin = new Join<K,Object>(ReverseSource,true);
        for(Map.Entry<K,Object> MapKey : ReverseKeys.entrySet())
            ReverseJoin.Joins.put(MapKey.getKey(),MergedSource.MapKeys.get(MapKey.getValue()));

        // закидываем св-ва параметра Merge
        Map<Object,Object> ProceedProps = new HashMap();
        for(Map.Entry<Object,Object> MapProperty : ReverseProps.entrySet()) {
            MergedSource.add(MapProperty.getKey(),ReverseJoin.Exprs.get(MapProperty.getValue()));
            ProceedProps.put(MapProperty.getValue(),MapProperty.getKey());
        }

        // закидываем свои св-ва
        for(V Property : Properties.keySet()) {
            Object ReverseProp = ProceedProps.get(Property);
            if(ReverseProp==null) {
                ReverseProp = new Object();
                MergedSource.add(ReverseProp,ReverseJoin.Exprs.get(Property));
            }
            MergeProps.put(Property,ReverseProp);
        }

        return MergedSource.compile();
    }
}

class OrderedJoinQuery<K,V> extends JoinQuery<K,V> {

    int Top;

    boolean Up;
    LinkedHashMap<SourceExpr,Boolean> Orders = new LinkedHashMap();

    // кривоватая перегрузка но плодить параметры еще хуже
    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {

        JoinQuery<K,Object> Query = new JoinQuery<K,Object>(Keys);
        // изврат конечно но по другому фиг сделаешь
        Query.MapKeys = MapKeys;
        Query.Wheres.addAll(Wheres);
        Query.Properties.putAll(Properties);
        Query.Joins.addAll(Joins);
        for(SourceExpr Order : Orders.keySet())
            Query.add(Order,Order);

        Map<K,String> KeySelect = new HashMap();
        Map<Object,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        // закинем в Properties 
        String From = Query.fillSelect(KeySelect,PropertySelect,WhereSelect,Syntax);

        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        for(V Property : Properties.keySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + PropertySelect.get(Property) + " AS " + getPropertyName(Property);
            PropertyOrder.add(Property);
        }

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        String OrderString = "";
        for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet())
            OrderString = (OrderString.length()==0?"":OrderString+",") + PropertySelect.get(Order.getKey())+(Up==Order.getValue()?" ASC":" DESC");

        return "SELECT " + Syntax.getTop(Top, ExpressionString + " FROM " + From,
                (OrderString.length()==0?"":" ORDER BY "+OrderString),WhereString);
    }

    OrderedJoinQuery(Collection<? extends K> iKeys) {super(iKeys);}

    OrderedJoinQuery<K,V> copy() {
        OrderedJoinQuery<K,V> Cloned = new OrderedJoinQuery<K,V>(Keys);
        Cloned.MapKeys = MapKeys;
        Cloned.Wheres.addAll(Wheres);
        Cloned.Properties.putAll(Properties);
        Cloned.Joins.addAll(Joins);
        Cloned.Top = Top;
        Cloned.Up = Up;
        Cloned.Orders.putAll(Orders);

        return Cloned;
    }
}

abstract class ExprUnion<K> {
    abstract SourceExpr translateExpr(Map<Union<K, Object>, Join<K, ?>> MapJoins, ExprTranslator Translated);

    ExprUnion translate(Map<PropertyUnion, ExprUnion> ToTranslate,Map<ExprUnion, ExprUnion> Translated) {
        ExprUnion<K> TranslateUnion = Translated.get(this);
        if(TranslateUnion==null) {
            TranslateUnion = proceedTranslate(ToTranslate, Translated);
            Translated.put(this,TranslateUnion);
        }

        return TranslateUnion;
    }

    abstract ExprUnion proceedTranslate(Map<PropertyUnion, ExprUnion> ToTranslate, Map<ExprUnion, ExprUnion> Translated);

    abstract String getString(Map<Union<K, Object>, String> MapAliases, SQLSyntax Syntax);

    // по сути следующие 2 метода для 3-го оператора
    abstract void fillUnions(Collection<Union> Unions);
    abstract boolean findOperator(int ToFind);

    abstract public String getDBType();
}

class PropertyUnion<K,V> extends ExprUnion<K> {
    V Property;
    Union<K,V> From;

    PropertyUnion(V iProperty, Union<K, V> iFrom) {
        Property = iProperty;
        From = iFrom;
    }

    SourceExpr translateExpr(Map<Union<K, Object>, Join<K, ?>> MapJoins, ExprTranslator Translated) {
        return MapJoins.get(From).Exprs.get(Property).translate(Translated);
    }

    ExprUnion proceedTranslate(Map<PropertyUnion, ExprUnion> ToTranslate, Map<ExprUnion, ExprUnion> Translated) {
        ExprUnion TranslateUnion = ToTranslate.get(this);
        if(TranslateUnion==null)
            return this;
        else
            return TranslateUnion.translate(ToTranslate, Translated);
    }

    String getString(Map<Union<K, Object>, String> MapAliases, SQLSyntax Syntax) {
        return MapAliases.get(From)+"."+From.Source.getPropertyName(Property);
    }

    void fillUnions(Collection<Union> Unions) {
        Unions.add(From);
    }

    boolean findOperator(int ToFind) {
        return false;
    }

    public String getDBType() {
        return From.Source.getDBType(Property);
    }
}

class OperationUnion<K> extends ExprUnion<K> {
    ListCoeffExpr<ExprUnion<K>> Operands = new ListCoeffExpr<ExprUnion<K>>();
    int Operator;

    OperationUnion(int iOperator) {
        Operator = iOperator;
    }

    SourceExpr translateExpr(Map<Union<K, Object>, Join<K, ?>> MapJoins, ExprTranslator Translated) {
        if(Operator<=2) {
            UnionSourceExpr UnionExpr = new UnionSourceExpr(Operator);

            for(CoeffExpr<ExprUnion<K>> Operand : Operands)
                UnionExpr.Operands.put(Operand.Expr.translateExpr(MapJoins, Translated),Operand.Coeff);

            return UnionExpr;
        } else {
            SelectWhereExpr UnionExpr = new SelectWhereExpr();

            for(CoeffExpr<ExprUnion<K>> Operand : Operands) {
                Collection<Union> Unions = new ArrayList();
                Operand.Expr.fillUnions(Unions);

                // делаем OR на InJoin'ы этих Source'ов
                Collection<SourceWhere> InJoins = new ArrayList(); 
                for(Union Union : Unions)
                    InJoins.add(MapJoins.get(Union).InJoin.translate(Translated));
                
                UnionExpr.Operands.add(new CoeffExpr<SelectWhereOperand>(new SelectWhereOperand(FieldOPWhere.getExpr(InJoins,true),Operand.Expr.translateExpr(MapJoins, Translated)),Operand.Coeff));
            }

            return UnionExpr;
        }
    }

    ExprUnion proceedTranslate(Map<PropertyUnion, ExprUnion> ToTranslate, Map<ExprUnion, ExprUnion> Translated) {
        OperationUnion<K> TranslateUnion = new OperationUnion<K>(Operator);

        for(CoeffExpr<ExprUnion<K>> Operand : Operands)
            TranslateUnion.Operands.put(Operand.Expr.translate(ToTranslate, Translated),Operand.Coeff);

        return TranslateUnion;
    }

    String getString(Map<Union<K, Object>, String> MapAliases, SQLSyntax Syntax) {
        if(Operator<=2) {
            // обычный оператор
            ListCoeffExpr<String> StringOperands = new ListCoeffExpr<String>();
            for(CoeffExpr<ExprUnion<K>> Operand : Operands)
                StringOperands.put(Operand.Expr.getString(MapAliases, Syntax),Operand.Coeff);

            return UnionQuery.getExpr(StringOperands,Operator,false,Syntax);
        } else {
            // оператор выбора
            String SourceExpr = null;
            for(CoeffExpr<ExprUnion<K>> Operand : Operands) {
                String PrevExpr = SourceExpr;
                SourceExpr = Operand.getCoeff() + Operand.Expr.getString(MapAliases, Syntax);
                if(PrevExpr!=null) {
                    Collection<Union> Unions = new ArrayList();
                    Operand.Expr.fillUnions(Unions);
                    // когда есть хоть в одном источнике
                    String NotInSource = "";
                    for(Union Union : Unions)
                        NotInSource = (NotInSource.length()==0?"":NotInSource+" AND ") + Union.Source.getInSelectString(MapAliases.get(Union)) + " IS NULL";

                    SourceExpr = CaseWhenSourceExpr.getExpr(NotInSource, PrevExpr, SourceExpr, Syntax);
                }
            }

            return SourceExpr;
        }
    }

    void fillUnions(Collection<Union> Unions) {
        for(CoeffExpr<ExprUnion<K>> Operand : Operands)
            Operand.Expr.fillUnions(Unions);
    }

    boolean findOperator(int ToFind) {
        if(Operator==ToFind) return true;

        for(CoeffExpr<ExprUnion<K>> Operand : Operands)
            if(Operand.Expr.findOperator(ToFind))
                return true;

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDBType() {
        return Operands.iterator().next().Expr.getDBType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperationUnion that = (OperationUnion) o;

        if (Operator != that.Operator) return false;
        if (!Operands.equals(that.Operands)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Operands.hashCode();
        result = 31 * result + Operator;
        return result;
    }
}

class Union<K,V> {
    Source<K,V> Source;

    Union(Source<K, V> iSource) {
        Source = iSource;

        Exprs = new HashMap();
        for(V Property : Source.getProperties())
            Exprs.put(Property,new PropertyUnion<K, V>(Property,this));
    }

    Map<V,PropertyUnion<K,V>> Exprs;

    // заполняет перетрансляцию к новому Join'у
    void fillUnionTranslate(Union<?,?> TranslatedUnion,Map<Object,Object> MergeProps, Map<PropertyUnion,ExprUnion> UnionTranslated) {
        for(Map.Entry<V,PropertyUnion<K,V>> MapUnion : Exprs.entrySet())
            UnionTranslated.put(MapUnion.getValue(),TranslatedUnion.Exprs.get(MergeProps==null?MapUnion.getKey():MergeProps.get(MapUnion.getKey())));
    }

    Union<K, Object> merge(Union Merge, Map<K, ?> MergeKeys, Map<PropertyUnion, ExprUnion> UnionTranslate, Map<PropertyUnion, ExprUnion> UnionMergeTranslate) {
        Map<Object, Object> MergeProps = new HashMap();
        Source<K, Object> MergeSource = Source.merge(Merge.Source, MergeKeys, MergeProps);
        if(MergeSource!=null) {
            // нужно перетранслировать JoinExpr'ы
            Union<K, Object> MergedUnion = new Union<K, Object>(MergeSource);
            fillUnionTranslate(MergedUnion, null, UnionTranslate);
            Merge.fillUnionTranslate(MergedUnion, MergeProps, UnionMergeTranslate);
            return MergedUnion;
        }

        return null;
    }

    boolean isTable() {
        return Source instanceof Table;
    }
}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends SelectQuery<K,V> {
    // как в List 0 - MAX, 1 - SUM, 2 - NVL, плюс 3 - если есть в Source

    UnionQuery(Collection<? extends K> iKeys,int iDefaultOperator) {super(iKeys); DefaultOperator=iDefaultOperator;}

    // "скомпилированные" св-ва
    Collection<Union<K,Object>> Operands = new ArrayList();
    Map<V, ExprUnion<K>> Operations = new HashMap();

/*    Map<Object,PropertyUnion<K,Object>> getOperandMap(Source<K,Object> Operand) {
        Map<Object,PropertyUnion<K,Object>> Properties = new HashMap();
        for(Object Property : Operand.getProperties())
            Properties.put(Property,new PropertyUnion<K, Object>(Property,Operand));
        return Properties;
    }

    private void setOperands(Collection<Source<K,Object>> OperandSet) {
        Operands = new HashMap();
        for(Source<K,Object> Operand : OperandSet)
            Operands.put(Operand,getOperandMap(Operand));
    }
  */
    // скомпилированный конструктор
    UnionQuery(Collection<? extends K> iKeys,Collection<Union<K,Object>> iOperands) {
        super(iKeys);
        Operands = iOperands;
    }

    // Safe - кдючи чисто никаких чисел типа 1 с которыми проблемы
    static String getExpr(List<CoeffExpr<String>> Operands, int Operator, boolean Safe, SQLSyntax Syntax) {
        String Result = "";
        String SumNull = "";
        for(CoeffExpr<String> Operand : Operands) {
            if(Operand.Expr.startsWith("null"))
                Operand = Operand;

            String OperandString = Operand.getCoeff();
            if(Operator==1 && Operands.size()>1) {
                OperandString += Syntax.isNULL(Operand.Expr,"0", false);
                SumNull = (SumNull.length()==0?"":SumNull+" AND ") + Operand.Expr + " IS NULL ";
            } else
                OperandString += Operand.Expr;

            if(Result.length()==0) {
                Result = OperandString;
            } else {
                switch(Operator) {
                    case 2:
                        if(!OperandString.equals("NULL")) {
                            if(!Safe && !Syntax.isNullSafe())
                                Result = Syntax.isNULL(OperandString,Result,true);
                            else
                                Result = OperandString + "," + Result;
                        }
                        break;
                    case 1:
                        Result = Result+(Operand.Coeff==null || Operand.Coeff>=0?"+":"") + OperandString;
                        break;
                    case 0:
                        if(!OperandString.equals("NULL")) {
                            if(Syntax.isGreatest())
                                Result = OperandString + "," + Result;
                            else
                                Result = "CASE WHEN "+OperandString+" IS NULL OR "+Result+">"+OperandString+" THEN "+Result+" ELSE "+OperandString+" END";
                        }
                        break;
                }
            }
        }

        if(Operands.size()<=1)
            return Result;
        else
        if(Operator==2 && (Safe || Syntax.isNullSafe()))
            return "COALESCE(" + Result + ")";    
        else
        if(Operator==0 && Syntax.isGreatest())
            return "GREATEST(" + Result + ")";
        else
        if(Operator==1)
            return "(CASE WHEN " + SumNull + " THEN NULL ELSE " + Result + " END )";
        else
            return "("+Result+")";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        if(compile()!=this) return compile().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

        // если операнд один сделаем JoinQuery, он его "раскроет"
        if(Operands.size()==1)
            return getJoinQuery().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

        Map<K,ListCoeffExpr<String>> UnionKeys = new HashMap();
        Map<Union<K,Object>,String> MapAliases = new HashMap();

        // сначала сделаем Collection Full Join'а чтобы таблицы были первыми
        Collection<Union<K,Object>> JoinList = new ArrayList();
        for(Union<K, Object> Operand : Operands)
            if(Operand.isTable()) JoinList.add(Operand);
        for(Union<K, Object> Operand : Operands)
            if(!(Operand.isTable())) JoinList.add(Operand);

        // сначала заполняем ключи
        int AliasCount = 0;
        String From = "";
        for(Union<K, Object> Union : JoinList) {
            String Alias = "f"+(AliasCount++);
            MapAliases.put(Union,Alias);

            String FromSource = Union.Source.getSource(Syntax) + " " + Alias;

            if(From.length()==0) {
                // первый Union
                for(K Key : Keys) {
                    ListCoeffExpr<String> JoinKeySource = new ListCoeffExpr<String>();
                    JoinKeySource.put(Union.Source.getKeyString(Key,Alias),1);
                    UnionKeys.put(Key,JoinKeySource);
                }
            } else {
                String JoinOn = "";
                for(K Key : Keys) {
                    String KeySource = Union.Source.getKeyString(Key,Alias);
                    ListCoeffExpr<String> JoinKeySource = UnionKeys.get(Key);
                    JoinOn = (JoinOn.length()==0?"":JoinOn+" AND ") + KeySource + "=" + getExpr(JoinKeySource,2,true, Syntax);
                    JoinKeySource.put(KeySource,1);
                }

                FromSource = " FULL JOIN " + FromSource + " ON " + (JoinOn.length()==0?"1=1":JoinOn);
            }

            From += FromSource;
        }

        // ключи
        for(K Key : Keys)
            KeySelect.put(Key,getExpr(UnionKeys.get(Key),2,true,Syntax));
        
        // ключи надо по NVL(2) делать, выражения по оператору
        // для Operator <=2

        for(Map.Entry<V, ExprUnion<K>> Property : Operations.entrySet())
            PropertySelect.put(Property.getKey(),Property.getValue().getString(MapAliases, Syntax));

        return From;
    }

    int DefaultOperator;
    void add(Source<K,V> Source,Integer Coeff) {

        // докидываем с DefaultOperator'ом
        Union<K,V> Union = new Union<K,V>(Source);
        Operands.add((Union<K,Object>) Union);
        for(Map.Entry<V,PropertyUnion<K,V>> MapExpr : Union.Exprs.entrySet()) {
            OperationUnion<K> ToAddExpr = null;
            ExprUnion<K> ExistedExpr = Operations.get(MapExpr.getKey());
            // если не было или не совпадают операторы создадим новое иначе закинем в сущ-ее
            if(ExistedExpr!=null && (ExistedExpr instanceof OperationUnion) && ((OperationUnion<K>)ExistedExpr).Operator!=DefaultOperator)
                ToAddExpr = ((OperationUnion<K>)ExistedExpr);
            else {
                ToAddExpr = new OperationUnion<K>(DefaultOperator);
                if(ExistedExpr!=null)
                    ToAddExpr.Operands.put(ExistedExpr,1);
                Operations.put(MapExpr.getKey(),ToAddExpr);
            }
            ToAddExpr.Operands.put(MapExpr.getValue(),Coeff);
        }
    }

    Collection<V> getProperties() {
        return Operations.keySet();
    }

    String getDBType(V Property) {
        return Operations.get(Property).getDBType();
    }

    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        // если есть UnionQuery не в Inner Join'ах или же у UnionQuery один операнд то из операндов делаем Join'ы, JoinExpr'ы заменяем на UnionSourceExpr'ы из UnionQuery с проставленными JoinExpr'ами
        Outer = Outer || !Join.Inner;
        if((Outer || Operands.size()==1)) {
            Map<Union<K,Object>,Join<K,?>> OperandJoins = new HashMap();
            // делаем Join'ы
            for(Union<K, Object> Operand : Operands) {
                Join<K,Object> JoinOperand = new Join<K,Object>(Operand.Source,Join.Joins,!Outer);
                OperandJoins.put(Operand,JoinOperand);

                Operand.Source.compileJoins(JoinOperand, Translated, JoinWheres, TranslatedJoins, Outer);
            }

            // InJoin'ы заменим на OR Inner InJoin'ов
            List<SourceWhere> CompileInJoin = new ArrayList();
            for(Join CompileJoin : OperandJoins.values())
                CompileInJoin.add(CompileJoin.InJoin);
            Translated.put(Join.InJoin,FieldOPWhere.getExpr(CompileInJoin,false).translate(Translated));
            /// все выражений протранслируем
            for(V Property : getProperties())
                Translated.put(Join.Exprs.get(Property),Operations.get(Property).translateExpr(OperandJoins, Translated));
        } else
            super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }

    private boolean recMerge(ListIterator<Union<K, Object>> RecUnion, Collection<Union> ToMergeUnions, Map<K, ?> MergeKeys, Map<PropertyUnion,ExprUnion> ToTranslate, Map<PropertyUnion,ExprUnion> ToMergeTranslate, Set<Union> MergedUnions, Collection<Union<K,Object>> TranslatedUnions) {

        if(!RecUnion.hasNext())
            return true;

        Union<K, Object> CurrentUnion = RecUnion.next();

        for(Union ToMergeUnion : ToMergeUnions) {
            if(!MergedUnions.contains(ToMergeUnion)) {
                // надо бы проверить что Join уже не merge'ут
                Map<PropertyUnion,ExprUnion> UnionToTranslate = new HashMap();
                Map<PropertyUnion,ExprUnion> UnionToMergeTranslate = new HashMap();
                Union<K, Object> MergedUnion = CurrentUnion.merge(ToMergeUnion, MergeKeys, UnionToTranslate, UnionToMergeTranslate);
                if(MergedUnion!=null) {
                    MergedUnions.add(ToMergeUnion);
                    TranslatedUnions.add(MergedUnion);
                    ToTranslate.putAll(UnionToTranslate);
                    ToMergeTranslate.putAll(UnionToMergeTranslate);
                    if(recMerge(RecUnion, ToMergeUnions, MergeKeys, ToTranslate, ToMergeTranslate, MergedUnions, TranslatedUnions))
                        return true;
                    MergedUnions.remove(ToMergeUnion);
                    TranslatedUnions.remove(MergedUnion);
                    CollectionExtend.removeAll(ToTranslate,UnionToTranslate.keySet());
                    CollectionExtend.removeAll(ToMergeTranslate,UnionToMergeTranslate.keySet());
                }
            }
        }

        RecUnion.previous();

        return false;
    }

    Source<K, Object> proceedMerge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {

        if(!(Merge instanceof UnionQuery)) return null;

        UnionQuery MergeUnion = (UnionQuery)Merge;
        if(Operands.size()!=MergeUnion.Operands.size()) return null;

        UnionQuery<K,Object> Result = new UnionQuery<K,Object>(Keys,0);
        Result.Compiled = Result;

        // сливаем все Source
        Map<PropertyUnion,ExprUnion> ToTranslate = new HashMap();
        Map<PropertyUnion,ExprUnion> ToMergeTranslate = new HashMap();
        Set<Union> MergedUnions = new HashSet(); 
        if(recMerge((new ArrayList<Union<K,Object>>(Operands)).listIterator(),MergeUnion.Operands,MergeKeys, ToTranslate, ToMergeTranslate, MergedUnions, Result.Operands)) {
            // сделаем Map в обратную сторону
            Map<ExprUnion,Object> BackOperations = new HashMap();
            // транслируем старые PropertyUnion на новые
            Map<ExprUnion, ExprUnion> Translated = new HashMap();
            for(Map.Entry<V, ExprUnion<K>> MapProp : Operations.entrySet()) {
                ExprUnion TransOperation = MapProp.getValue().translate(ToTranslate,Translated);

                Result.Operations.put(MapProp.getKey(),TransOperation);
                BackOperations.put(TransOperation,MapProp.getKey());
            }

            Translated = new HashMap();
            for(Map.Entry<Object, ExprUnion<Object>> MapProp : ((UnionQuery<Object,Object>)MergeUnion).Operations.entrySet()) {
                ExprUnion TransOperation = MapProp.getValue().translate(ToMergeTranslate,Translated);

                Object PropertyObject = BackOperations.get(TransOperation);
                if(PropertyObject==null) {
                    PropertyObject = new Object();
                    Result.Operations.put(PropertyObject,TransOperation);
                }
                MergeProps.put(MapProp.getKey(),PropertyObject);
            }

//            Result.checkTranslate(new HashMap(),Result.Operands);

            return Result;
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    Source<K, V> proceedCompile() {
        // компилируем все операнды
        for(Union<K, Object> Operand : Operands)
            Operand.Source = Operand.Source.compile();

//        if(1==1) return;

        // по всем операндам (SelectQuery) компилируем их
        // если есть UnionQuery то доливаем все операнды туда где был старый Source
        Map<PropertyUnion, ExprUnion> ToTranslate = new HashMap();
        for(Union<K, Object> Operand : new ArrayList<Union<K, Object>>(Operands)) {
            if(Operand.Source instanceof UnionQuery) {
                Operands.remove(Operand);

                UnionQuery<K,Object> UnionOperand = (UnionQuery<K,Object>)Operand.Source;
                Operands.addAll(UnionOperand.Operands);
                // закинем перекодирования (Map'а св-в)
                for(Map.Entry<Object,PropertyUnion<K,Object>> MapProperty : Operand.Exprs.entrySet())
                    ToTranslate.put(MapProperty.getValue(),UnionOperand.Operations.get(MapProperty.getKey()));
            }
        }

//        checkTranslate(ToTranslate,Operands);

        Map<K,K> MapKeys = new HashMap();
        for(K Key : Keys)
            MapKeys.put(Key,Key);

        // для всех пар JoinQuery если Set<Join'ов> включает (compatible) Set<Join> другого, "доливаем" 1-й Join во 2-й - списки Join'ов объединяем, делаем Include, также заменяем PropExpr'ы и 1-го и 2-го JoinQuery на новые PropExprы
        // пока просто если есть 2 (compatible) Source объединяем заменяем PropExpr'ы

        // слитыве операнды
        Collection<Union<K,Object>> MergedOperands = new ArrayList();

        while(Operands.size()>0) {
            Iterator<Union<K, Object>> itOperand = Operands.iterator();
            // берем первый элемент, ишем с кем слить
            Union<K, Object> ToMerge = itOperand.next();
            itOperand.remove();
        
            while(itOperand.hasNext()) {
                Union<K, Object> Check = itOperand.next();
                Union<K, Object> MergedSource = ToMerge.merge(Check, MapKeys, ToTranslate, ToTranslate);
                if(MergedSource!=null) {
                    itOperand.remove();
                    ToMerge = MergedSource;
                }
            }

            MergedOperands.add(ToMerge);
        }

//        checkTranslate(ToTranslate,MergedOperands);

        Operands = MergedOperands;
        // транслируем все Operations
        Map<ExprUnion, ExprUnion> Translated = new HashMap();
        for(Map.Entry<V, ExprUnion<K>> Operation : Operations.entrySet())
            Operation.setValue(Operation.getValue().translate(ToTranslate,Translated));

//        checkTranslate(new HashMap(),Operands);

        return this;
    }

    void checkTranslate(Map<PropertyUnion, ExprUnion> ToTranslate,Collection<Union<K,Object>> TranslatedUnions) {
        // проверяем SourceWheres
        Map<ExprUnion, ExprUnion> Translated = new HashMap();
        for(ExprUnion<K> Operation : Operations.values()) {
            Collection<Union> Unions = new HashSet();
            Operation.translate(ToTranslate,Translated).fillUnions(Unions);
            if(!TranslatedUnions.containsAll(Unions))
                throw new RuntimeException("Wrong PropertyUnion");
        }
    }


    Source<K, V> mergeKeyValue(Map<K, Integer> MergeKeys, Collection<K> CompileKeys) {
        // бежим по всем операндам, делаем mergeKeyValue, подготавливаем транслятор
        Collection<Union<K,Object>> MergedOperands = new ArrayList();
        Map<PropertyUnion, ExprUnion> ToTranslate = new HashMap();
        for(Union<K, Object> Operand : Operands) {
            Union<K, Object> MergedUnion = new Union<K,Object>(Operand.Source.mergeKeyValue(MergeKeys, CompileKeys));
            Operand.fillUnionTranslate(MergedUnion,null,ToTranslate);
            MergedOperands.add(MergedUnion);
        }

        UnionQuery<K,V> MergedQuery = new UnionQuery<K,V>(CompileKeys,MergedOperands);
        MergedQuery.Compiled = MergedQuery;

        Map<ExprUnion, ExprUnion> Translated = new HashMap();
        for(Map.Entry<V, ExprUnion<K>> MapOperation : Operations.entrySet())
            MergedQuery.Operations.put(MapOperation.getKey(),MapOperation.getValue().translate(ToTranslate,Translated));

        return MergedQuery;
    }
}

// с GroupQuery пока неясно
class GroupQuery<B,K extends B,V extends B> extends Query<K,V> {
    Source<?,B> From; // вообще должен быть или K или V
    Map<V,Integer> Properties = new HashMap();
//    int Operator;

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {

//        compile();

        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String FromSelect = From.fillSelect(new HashMap(),FromPropertySelect,WhereSelect, Syntax);

        String GroupBy = "";
        Map<K,String> KeySelect = new HashMap();
        Map<V,String> PropertySelect = new HashMap();
        for(K Key : Keys) {
            String KeyExpr = FromPropertySelect.get(Key);
            KeySelect.put(Key,KeyExpr);
            if(!From.isNotNullProperty(Key))
                WhereSelect.add("NOT "+KeyExpr+" IS NULL");
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
        }
        for(Map.Entry<V,Integer> Property : Properties.entrySet())
            PropertySelect.put(Property.getKey(),(Property.getValue()==0?"MAX":"SUM")+"("+FromPropertySelect.get(Property.getKey())+")");

        return getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder) + (GroupBy.length()==0?"":" GROUP BY "+GroupBy);
    }

    Source<K, Object> proceedMerge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        // если Merge'ся From'ы
        if(!(Merge instanceof GroupQuery)) return null;

//        if(1==1) return null;

        GroupQuery<?,?,?> MergeGroup = (GroupQuery<?,?,?>)Merge;
        if(Keys.size()!=MergeGroup.Keys.size()) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<Object, Object>> MapSet = (new MapBuilder<Object, Object>()).buildPairs(From.Keys, MergeGroup.From.Keys);
        if(MapSet==null) return null;

        for(Map<Object,Object> MapKeys : MapSet) {
            Map<Object,Object> FromMergeProps = new HashMap();
            Source<Object, Object> MergedFrom = ((Source<Object, Object>) From).merge(MergeGroup.From, MapKeys, FromMergeProps);
            if(MergedFrom!=null) {
                // проверим что ключи совпали
                boolean KeyMatch = true;
                for(K Key : Keys)
                    KeyMatch = KeyMatch && Key.equals(FromMergeProps.get(MergeKeys.get(Key)));

                if(KeyMatch) {
                    Map<Object,Integer> MergedProperties = new HashMap(Properties);
                    for(Map.Entry<?,Integer> MergeProp : MergeGroup.Properties.entrySet()) {
                        Object MapProp = FromMergeProps.get(MergeProp.getKey());
                        MergedProperties.put(MapProp,MergeProp.getValue());
                        MergeProps.put(MergeProp.getKey(),MapProp);
                    }

                    GroupQuery<Object, K, Object> Result = new GroupQuery<Object, K, Object>(Keys, MergedFrom, MergedProperties);
                    return Result.compile();
                }
            }
        }

        return null;
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,V Property,int iOperator) {
        super(iKeys);
        From = iFrom;
        Properties.put(Property,iOperator);
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,Map<V,Integer> iProperties) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;
    }
    
    Collection<V> getProperties() {
        return Properties.keySet();
    }

    String getDBType(V Property) {
        return From.getDBType(Property);
    }

    Source<K, V> mergeKeyValue(Map<K, Integer> MergeKeys, Collection<K> CompileKeys) {
        GroupQuery<B, K, V> Result = new GroupQuery<B, K, V>(CompileKeys, From.mergePropertyValue(MergeKeys), Properties);
        return Result.compile();
    }

    Source<K, V> proceedCompile() {
        From = From.compile();
        return this;
    }
}

class ModifyQuery {
    Table Table;
    Source<KeyField,PropertyField> Change;

    ModifyQuery(Table iTable,Source<KeyField,PropertyField> iChange) {
        Table = iTable;
        Change = iChange;
    }

    String getUpdate(SQLSyntax Syntax) {

        int UpdateModel = Syntax.UpdateModel();
        if(UpdateModel==2) {
            // Oracl'вская модель Update'а
            Map<KeyField,String> KeySelect = new HashMap();
            Map<PropertyField,String> PropertySelect = new HashMap();
            Collection<String> WhereSelect = new ArrayList();
            String FromSelect = Change.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

            for(KeyField Key : Table.Keys)
                WhereSelect.add(Table.getSource(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            
            List<KeyField> KeyOrder = new ArrayList();
            List<PropertyField> PropertyOrder = new ArrayList();
            String SelectString = Change.getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder);

            String SetString = "";
            for(KeyField Field : KeyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;
            for(PropertyField Field : PropertyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;

            return "UPDATE " + Table.getSource(Syntax) + " SET ("+SetString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
        } else {
            Map<KeyField,String> KeySelect = new HashMap();
            Map<PropertyField,String> PropertySelect = new HashMap();
            Collection<String> WhereSelect = new ArrayList();

            String WhereString = "";
            String FromSelect = null;

            if(UpdateModel==1) {
                // SQL-серверная модель когда она подхватывает первый Join и старую таблицу уже не вилит
                // построим JoinQuery куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы)
                JoinQuery<KeyField, PropertyField> UpdateQuery = new JoinQuery<KeyField, PropertyField>(Table.Keys);
                Join<KeyField, PropertyField> TableJoin = new UniJoin<KeyField, PropertyField>(Table, UpdateQuery, true);
                TableJoin.NoAlias = true;
                UpdateQuery.add(TableJoin.InJoin);

                Join<KeyField, PropertyField> ChangeJoin = new UniJoin<KeyField, PropertyField>(Change, UpdateQuery, true);
                for(PropertyField ChangeField : Change.getProperties())
                    UpdateQuery.add(ChangeField,ChangeJoin.Exprs.get(ChangeField));
                FromSelect = UpdateQuery.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);
            } else {
                FromSelect = Change.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

                for(KeyField Key : Table.Keys)
                    WhereSelect.add(Table.getSource(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            }

            for(String Where : WhereSelect)
                WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

            String SetString = "";
            for(Map.Entry<PropertyField,String> SetProperty : PropertySelect.entrySet())
                SetString = (SetString.length()==0?"":SetString+",") + SetProperty.getKey().Name + "=" + SetProperty.getValue();

            return "UPDATE " + Syntax.getUpdate(Table.getSource(Syntax)," SET "+SetString,FromSelect,(WhereString.length()==0?"":" WHERE "+WhereString));
        }
    }

    String getInsertLeftKeys(SQLSyntax Syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField,PropertyField> LeftKeysQuery = new JoinQuery<KeyField,PropertyField>(Table.Keys);
        // при Join'им ModifyQuery
        LeftKeysQuery.add((new UniJoin<KeyField,PropertyField>(Change,LeftKeysQuery,true)).InJoin);
        // исключим ключи которые есть
        LeftKeysQuery.add(new NotWhere((new UniJoin<KeyField,PropertyField>(Table,LeftKeysQuery,false)).InJoin));

        return (new ModifyQuery(Table,LeftKeysQuery)).getInsertSelect(Syntax);
    }

    String getInsertSelect(SQLSyntax Syntax) {

        List<KeyField> KeyOrder = new ArrayList();
        List<PropertyField> PropertyOrder = new ArrayList();
        String SelectString = Change.getSelect(KeyOrder,PropertyOrder,Syntax);

        String InsertString = "";
        for(KeyField KeyField : KeyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + KeyField.Name;
        for(PropertyField PropertyField : PropertyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + PropertyField.Name;

        return "INSERT INTO " + Table.getSource(Syntax) + " (" + InsertString + ") " + SelectString;
    }

    void outSelect(DataSession Session) throws SQLException {
        System.out.println("Table");
        Table.outSelect(Session);
        System.out.println("Source");
        Change.outSelect(Session);
    }
}

