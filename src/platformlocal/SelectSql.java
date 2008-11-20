/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

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

    String getPropertyString(V Value, String Alias, SQLSyntax Syntax) {
        return Alias + "." + getPropertyName(Value);
    }

    abstract Collection<V> getProperties();

    abstract Type getType(V Property);

    // по умолчанию fillSelectString вызывает
    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        return fillSelectString(getSource(Syntax),KeySelect,PropertySelect,WhereSelect,Syntax);
    }

    // заполняет структуру Select'а из строки Source
    String fillSelectString(String Source,Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        String Alias = "G";
        for(K Key : Keys)
            KeySelect.put(Key,getKeyString(Key,Alias));
        for(V Property : getProperties())
            PropertySelect.put(Property,getPropertyString(Property,Alias,Syntax));

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

    // из-за templatов сюда кинем
    LinkedHashMap<Map<K,Integer>,Map<V,Object>> executeSelect(DataSession Session) throws SQLException {

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> ExecResult = new LinkedHashMap();
        if(compile() instanceof EmptySource) return ExecResult;

        Statement Statement = Session.Connection.createStatement();

//        System.out.println(getSelect(new ArrayList(),new ArrayList(), Session.Syntax));
        try {
            ResultSet Result = Statement.executeQuery(getSelect(new ArrayList(),new ArrayList(),Session.Syntax));
            try {
                while(Result.next()) {
                    Map<K,Integer> RowKeys = new HashMap();
                    for(K Key : Keys)
                        RowKeys.put(Key,Type.Object.read(Result.getObject(getKeyName(Key))));
                    Map<V,Object> RowProperties = new HashMap();
                    for(V Property : getProperties())
                        RowProperties.put(Property,getType(Property).read(Result.getObject(getPropertyName(Property))));

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
                System.out.print(Key+"-"+RowMap.getKey().get(Key)); 
                System.out.print(" ");
            }
            System.out.print("---- ");
            for(V Property : getProperties()) {
                System.out.print(Property+"-"+RowMap.getValue().get(Property));
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
            KeyField KeyField = new KeyField(getKeyName(Key),Type.Integer);
            View.Keys.add(KeyField);
            MapKeys.put(Key,KeyField);
        }
        for(V Property : getProperties()) {
            PropertyField PropertyField = new PropertyField(getPropertyName(Property), getType(Property));
            View.Properties.add(PropertyField);
            MapProperties.put(Property,PropertyField);
        }

        Session.Execute("CREATE VIEW " + ViewName + " AS " + getSelect(new ArrayList(),new ArrayList(),Session.Syntax));

        return View;
    }

    <MK,MV> Source<K, Object> merge(Source<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps,MergeType Type) {
        if(this==Merge) {
            for(Map.Entry<K,MK> MapKey : MergeKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return null;

            for(MV Field : Merge.getProperties())
                MergeProps.put(Field,Field);

            return (Source<K, Object>)((Source<K,?>)this);
        } else {
            if(Keys.size()!=Merge.Keys.size()) return null;

            // сначала пробуем прямой Merge, затем обратный
            Source<K,Object> Result = correctMerge(Merge, MergeKeys, MergeProps, Type, true);
            if(Result!=null) return Result;
            return Merge.reverseMerge(this, MergeKeys, MergeProps, Type);
        }
    }

    public <MK,MV> Source<K,Object> correctMerge(Source<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type, boolean MergeEqual) {

        if(!(this instanceof JoinQuery) && Merge instanceof JoinQuery)
            return getCompiledJoinQuery().proceedMerge(Merge, MergeKeys, MergeProps, Type, MergeEqual);

        if(!(this instanceof UnionQuery) && Merge instanceof UnionQuery)
            return getUnionQuery(2).proceedMerge(Merge, MergeKeys, MergeProps, Type, MergeEqual);

        return proceedMerge(Merge, MergeKeys, MergeProps, Type, MergeEqual);
    }

    public <MK,MV> Source<MK,Object> reverseMerge(Source<MK,MV> Merge, Map<MK,K> MergeKeys, Map<V, Object> MergeProps, MergeType Type) {

        Map<K, MK> ReverseKeys = CollectionExtend.reverse(MergeKeys);
        Map<MV,Object> ReverseProps = new HashMap<MV, Object>();
        Source<K, Object> ReverseSource = correctMerge(Merge, ReverseKeys, ReverseProps, reverseType(Type), false);
        if(ReverseSource==null) return null;

        // создадим JoinQuery для перекодирования ключей
        JoinQuery<MK,Object> MergedSource = new JoinQuery<MK,Object>(Merge.Keys);
        Join<K,Object> ReverseJoin = new Join<K,Object>(ReverseSource,true);
        for(Map.Entry<K,MK> MapKey : ReverseKeys.entrySet())
            ReverseJoin.Joins.put(MapKey.getKey(),MergedSource.MapKeys.get(MapKey.getValue()));

        // закидываем св-ва параметра Merge
        Map<Object,MV> ProceedProps = new HashMap<Object, MV>();
        for(Map.Entry<MV,Object> MapProperty : ReverseProps.entrySet()) {
            MergedSource.add(MapProperty.getKey(),ReverseJoin.Exprs.get(MapProperty.getValue()));
            ProceedProps.put(MapProperty.getValue(),MapProperty.getKey());
        }

        // закидываем свои св-ва
        for(V Property : getProperties()) {
            Object ReverseProp = ProceedProps.get(Property);
            if(ReverseProp==null) {
                ReverseProp = new Object();
                MergedSource.add(ReverseProp,ReverseJoin.Exprs.get(Property));
            }
            MergeProps.put(Property,ReverseProp);
        }

        return MergedSource.compile();
    }

    <MK,MV> Source<K,Object> proceedMerge(Source<MK, MV> Merge, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type, boolean MergeEqual) {
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
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Where> TranslatedWheres, Collection<Join> TranslatedJoins, boolean Outer) {
        
        TranslatedJoins.add(Join.translate(Translated, Translated, Outer));
    }

    // записывается в Union'ы
    void compileUnions(Union<K, V> Union, Map<PropertyUnion,ExprUnion> ToTranslate, Collection<Union<K, Object>> TranslatedUnions) {

        TranslatedUnions.add((Union<K, Object>) Union);
    }

    // возвращает источник без заданных выражений, но только с объектами им равными (короче устанавливает фильтр)
    Source<K,V> mergePropertyValue(Map<? extends V, ValueSourceExpr> MergeValues) {
        
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
        Query.add(new Join<V,Object>(new DumbSource<V,Object>((Map<V,ValueSourceExpr>) MergeValues, new HashMap()),DumbMap, true));
        return Query.compile();
    }

    // возвращает источник с "проставленными" ключами со значениями 
    Source<K, V> mergeKeyValue(Map<K, ValueSourceExpr> MergeKeys,Collection<K> CompileKeys) {

        JoinQuery<K,V> Query = new JoinQuery<K,V>(CompileKeys);
        Join<K,V> SourceJoin = new Join<K,V>(this,true);
        for(Map.Entry<K,SourceExpr> MapKey : Query.MapKeys.entrySet())
            SourceJoin.Joins.put(MapKey.getKey(),MapKey.getValue());
        for(Map.Entry<K,ValueSourceExpr> MapKey : MergeKeys.entrySet())
            SourceJoin.Joins.put(MapKey.getKey(),MapKey.getValue());
        Query.addAll(SourceJoin.Exprs);
        return Query.compile();
    }

    JoinQuery<K,V> getJoinQuery() {
        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Join<K,V> SourceJoin = new UniJoin<K,V>(this,Query,true);
        Query.addAll(SourceJoin.Exprs);
        return Query;
    }

    <T> JoinQuery<T,V> getMapQuery(Map<T,K> MapKeys) {
        JoinQuery<T,V> Query = new JoinQuery<T,V>(MapKeys.keySet());
        Join<K,V> SourceJoin = new MapJoin<K,V,T>(this,Query,MapKeys,true);
        Query.addAll(SourceJoin.Exprs);
        return Query;
    }

    // оборачивает Source в JoinQuery 
    Source<K, V> getCompiledJoinQuery() {
        return getJoinQuery().compile();
    }

    Source<K,V> getUnionQuery(int DefaultOperator) {
        UnionQuery<K,V> Query = new UnionQuery<K,V>(Keys, DefaultOperator);
        Query.add(this,1);
        return Query.compile();
    }

    Source<K,V> getEmptySource() {

        Map<V,Type> NullProps = new HashMap();
        for(V Property : getProperties())
            NullProps.put(Property, getType(Property));
        return new EmptySource<K,V>(Keys,NullProps);
    }

    Source<K, V> compile() {
        return this;
    }

    static public enum MergeType {
        EQUAL, INNER, LEFT, RIGHT, FULL
    }

    public static MergeType EQUAL = MergeType.EQUAL;
    public static MergeType FULL = MergeType.FULL;

    static MergeType getJoinType(boolean LeftInner, boolean RightInner) {
        if(LeftInner)
            return (RightInner?MergeType.INNER :MergeType.LEFT);
        else
            return (RightInner?MergeType.RIGHT :MergeType.FULL);
    }

    static MergeType reverseType(MergeType Type) {
        if(Type==MergeType.LEFT) return MergeType.RIGHT;
        if(Type==MergeType.RIGHT) return MergeType.LEFT;
        return Type;
    }
}

class SessionQuery<K,V> extends JoinQuery<K,V> {

    SessionTable Cursor;
    
    SessionQuery(DataSession Session,Source<K,V> Source) throws SQLException {
        super(Source.Keys);

        // создаем SessionTable
        Cursor = new SessionTable(Session.getCursorName());
        Map<KeyField,SourceExpr> MapCursorKeys = new HashMap<KeyField,SourceExpr>();
        for(K Key : Keys) {
            KeyField CursorKey = new KeyField(Source.getKeyName(Key), Type.Object);
            Cursor.Keys.add(CursorKey);
            MapCursorKeys.put(CursorKey,MapKeys.get(Key));
        }
        Map<V,PropertyField> MapCursorProps = new HashMap<V,PropertyField>();
        for(V Property : Source.getProperties()) {
            PropertyField CursorProp = new PropertyField(Source.getPropertyName(Property), Source.getType(Property));
            Cursor.Properties.add(CursorProp);
            MapCursorProps.put(Property,CursorProp);
        }
        Session.CreateTemporaryTable(Cursor);
        Join<KeyField,PropertyField> CursorJoin = new Join<KeyField,PropertyField>(Cursor,MapCursorKeys,true);
        Joins.add(CursorJoin);
        for(Map.Entry<V,PropertyField> MapProp : MapCursorProps.entrySet())
            Properties.put(MapProp.getKey(),CursorJoin.Exprs.get(MapProp.getValue()));
    }
}


class TypedObject {
    Object Value;
    Type Type;

    TypedObject(Object iValue, Type iType) {
        Value = iValue;
        Type = iType;
    }

    static String getString(Object Value, Type Type, SQLSyntax Syntax) {
        if(Value==null)
            return Type.NULL;
        else
            return Type.getString(Value,Syntax);
    }

    String getString(SQLSyntax Syntax) {
        return getString(Value,Type,Syntax);
    }
}

// постоянный источник из одной записи
class DumbSource<K,V> extends Source<K,V> {

    Map<K,ValueSourceExpr> ValueKeys;
    Map<V,ValueSourceExpr> Values;

    DumbSource(Map<K,ValueSourceExpr> iValueKeys,Map<V,ValueSourceExpr> iValues) {
        super(iValueKeys.keySet());
        ValueKeys = iValueKeys;
        Values = iValues;
    }

    String getSource(SQLSyntax Syntax) {
        return "dumb";
    }

    String getKeyString(K Key,String Alias) {
        return ValueKeys.get(Key).getString(null);
    }

    String getPropertyString(V Property, String Alias, SQLSyntax Syntax) {
        return Values.get(Property).getString(Syntax);
    }

    Collection<V> getProperties() {
        return Values.keySet();
    }

    Type getType(V Property) {
        return Values.get(Property).getType();
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        for(Map.Entry<K,ValueSourceExpr> ValueKey : ValueKeys.entrySet())
            KeySelect.put(ValueKey.getKey(),ValueKey.getValue().getString(Syntax));
        for(Map.Entry<V,ValueSourceExpr> Value : Values.entrySet())
            PropertySelect.put(Value.getKey(),Value.getValue().getString(Syntax));

        return "dumb";
    }

    void outSelect(DataSession Session) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Where> TranslatedWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        if(Join.Inner) {
            ExprTranslator JoinTranslated = new ExprTranslator();
            for(Map.Entry<K,ValueSourceExpr> ValueKey : ValueKeys.entrySet()) {
                SourceExpr JoinExpr = Join.Joins.get(ValueKey.getKey()).translate(Translated);
                JoinTranslated.put(JoinExpr,ValueKey.getValue());
                // если св-во также докидываем в фильтр
                if(!(JoinExpr instanceof KeyExpr))
                    TranslatedWheres.add(new FieldExprCompareWhere(JoinExpr,ValueKey.getValue(),FieldExprCompareWhere.EQUALS));
            }
            Translated.merge(JoinTranslated);

            // все свои значения также затранслируем
            for(Map.Entry<V,ValueSourceExpr> MapValue : Values.entrySet())
                Translated.put(Join.Exprs.get(MapValue.getKey()),MapValue.getValue());

            // после чего дальше даже не компилируемся - proceedCompile словит транслированные ключи
        } else
            super.compileJoins(Join, Translated, TranslatedWheres, TranslatedJoins, Outer);
    }
}

class EmptySource<K,V> extends Source<K,V> {

    Map<V,Type> NullProps;

    EmptySource(Collection<? extends K> iKeys,Map<V,Type> iNullProps) {
        super(iKeys);
        NullProps = iNullProps;
    }

    public EmptySource(Collection<? extends K> iKeys,V Property,Type DBType) {
        this(iKeys,Collections.singletonMap(Property,DBType));
    }

    String getSource(SQLSyntax Syntax) {
        return "empty";
    }

    Collection<V> getProperties() {
        return NullProps.keySet();
    }

    Type getType(V Property) {
        return NullProps.get(Property);
    }

    String getKeyString(K Key, String Alias) {
        return Type.NULL;
    }

    String getPropertyString(V Value, String Alias, SQLSyntax Syntax) {
        return Type.NULL;
    }

    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Where> TranslatedWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        for(SourceExpr JoinExpr : Join.Exprs.values())
            Translated.put(JoinExpr,new ValueSourceExpr(null,JoinExpr.getType()));
        Translated.put(Join.InJoin,StaticWhere.False);
    }

    void compileUnions(Union<K, V> Union, Map<PropertyUnion, ExprUnion> ToTranslate, Collection<Union<K, Object>> TranslatedUnions) {
        for(PropertyUnion<K, V> Expr : Union.Exprs.values())
            ToTranslate.put(Expr,new EmptyUnion<K>(Expr.getType()));
    }
}
// таблицы\Views

class Field {
    String Name;
    Type Type;

    Field(String iName,Type iType) {Name=iName;Type=iType;}

    String GetDeclare(SQLSyntax Syntax) {
        return Name + " " + Type.getDB(Syntax);
    }

    public String toString() {
        return Name;
    }
}

class KeyField extends Field {
    KeyField(String iName,Type iType) {super(iName,iType);}
}

class PropertyField extends Field {
    PropertyField(String iName,Type iType) {super(iName,iType);}
}

class Table extends Source<KeyField,PropertyField> {
    String Name;
    Collection<PropertyField> Properties = new ArrayList();

    Table(String iName) {Name=iName;}

    String getSource(SQLSyntax Syntax) {
        return Name;
    }

    public String toString() {
        return Name;
    }

    Set<List<PropertyField>> Indexes = new HashSet();

    Collection<PropertyField> getProperties() {
        return Properties;
    }

    Type getType(PropertyField Property) {
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
    Source<J,U> source;
    Map<J,SourceExpr> Joins;

    boolean Inner;

    // теоретически только для таблиц может быть
    boolean NoAlias = false;

    Where InJoin;

    Map<U,JoinExpr<J,U>> Exprs = new HashMap();

    Join(Source<J,U> iSource,boolean iInner) {
        this(iSource,new HashMap(),iInner);
    }

    Join(Source<J,U> iSource,Map<J,? extends SourceExpr> iJoins,boolean iInner) {
        source = iSource;
        Inner = iInner;
        InJoin = new JoinWhere(this);
        for(U Property : source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));

        Joins = (Map<J,SourceExpr>) iJoins;
    }

    void fillJoins(List<Join> FillJoins, Set<Where> Wheres, Set<Join> DependJoins) {
        Set<Join> InDepend = new HashSet<Join>();
        for(SourceExpr Join : Joins.values())
            Join.fillJoins(FillJoins,Wheres,InDepend);
        DependJoins.addAll(InDepend);

        if(!FillJoins.contains(this)) {
            if(Inner) { // вставляем сразу после первой зависимости
                int InsertJoin = 0;
                for(int i=0;i<FillJoins.size();i++)
                    if(InDepend.contains(FillJoins.get(i)))
                        InsertJoin = i+1;
                FillJoins.add(InsertJoin,this);
            } else // в конец
                FillJoins.add(this);
        }
        DependJoins.add(this);
    }

    String getFrom(Map<Join, String> JoinAlias, Collection<String> WhereSelect, SQLSyntax Syntax) {
        String JoinString = "";
        String SourceString = source.getSource(Syntax);
        String Alias = null;
        if(NoAlias)
            Alias = SourceString;
        else {
            Alias = "t"+(JoinAlias.size()+1);
            SourceString = SourceString + " " + Alias;
        }
        JoinAlias.put(this,Alias);

        for(Map.Entry<J,SourceExpr> KeyJoin : Joins.entrySet()) {
            String KeyJoinString = KeyJoin.getValue().getJoin(source.getKeyString(KeyJoin.getKey(),Alias),JoinAlias, Syntax);
            if(KeyJoinString!=null) {
                if(WhereSelect==null)
                    JoinString = (JoinString.length()==0?"":JoinString+" AND ") + KeyJoinString;
                else
                    WhereSelect.add(KeyJoinString);
            }
        }

        return SourceString + (WhereSelect==null?" ON "+(JoinString.length()==0?StaticWhere.TRUE:JoinString):"");
    }

    Set<KeyExpr> getKeyJoins() {
        Set<KeyExpr> Result = new HashSet<KeyExpr>();
        for(SourceExpr JoinExpr : Joins.values())
            if(JoinExpr instanceof KeyExpr)
                Result.add((KeyExpr)JoinExpr);
        return Result;
    }

    Join<J,U> translate(ExprTranslator Translated, ExprTranslator JoinTranslated,boolean ForceOuter) {
        boolean ChangedJoins = (ForceOuter && Inner);
        Map<J,SourceExpr> TransMap = new HashMap();
        for(Map.Entry<J,SourceExpr> Join : Joins.entrySet()) {
            SourceExpr TransJoin = Join.getValue().translate(Translated);
            TransMap.put(Join.getKey(), TransJoin);
            ChangedJoins = ChangedJoins || (TransJoin!=Join.getValue());
        }

        if(!ChangedJoins)
            return this;
        else {
            Join<J,U> TranslatedJoin = new Join<J,U>(source,TransMap,Inner && !ForceOuter);
            TranslatedJoin.NoAlias = NoAlias;
            fillJoinTranslate(TranslatedJoin, null, JoinTranslated);
            return TranslatedJoin;
        }
    }

    // заполняет перетрансляцию к новому Join'у
    <MJ,MU> void fillJoinTranslate(Join<MJ,MU> TranslatedJoin,Map<U,Object> MergeProps, ExprTranslator JoinTranslated) {

        for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet())
            JoinTranslated.put(MapJoin.getValue(),TranslatedJoin.Exprs.get(MergeProps==null?MapJoin.getKey():MergeProps.get(MapJoin.getKey())));
        JoinTranslated.put(InJoin,TranslatedJoin.InJoin);        
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr JoinValue : Joins.values())
            JoinValue.fillObjectExprs(Exprs);
    }

    <MJ,MU> Join<J, Object> merge(Join<MJ,MU> Merge, ExprTranslator Translated, ExprTranslator MergeTranslated, ExprTranslator JoinTranslated, ExprTranslator JoinMergeTranslated,Source.MergeType Type) {
       // нужно построить карту между Joins'ами (с учетом Translate'ов) (точнее и записать в новый Join Translate'утые выражения

        // проверить что кол-во Keys в Source совпадает

        Collection<Map<J, MJ>> MapSet = MapBuilder.buildPairs(source.Keys, Merge.source.Keys);
        if(MapSet==null) return null;

        for(Map<J,MJ> MapKeys : MapSet) {
            Map<J,SourceExpr> MergeExprs = new HashMap();
            for(J Key : source.Keys) {
                SourceExpr MergeExpr = Joins.get(Key).translate(Translated);
                if(!Merge.Joins.get(MapKeys.get(Key)).translate(MergeTranslated).equals(MergeExpr))
                    break;
                else
                    MergeExprs.put(Key,MergeExpr);
            }

            if(MergeExprs.size()!= source.Keys.size())
                continue;

            // есть уже карта попробуем merge'уть
            Map<MU, Object> MergeProps = new HashMap();
            if(Type==null) Type = Source.getJoinType(Inner, Merge.Inner);
            Source<J, Object> MergeSource = source.merge(Merge.source, MapKeys, MergeProps, Type);
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

    public <K> String fillSingleSelect(Collection<String> WhereSelect, SQLSyntax Syntax, Map<KeyExpr<K>, ValueSourceExpr> KeyValues) {

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
        String From = source.fillSelect(KeySelect,PropertySelect,WhereSelect,Syntax);
        // если там не KeyExpr, то уже глюк
        for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet())
            ((KeyExpr)MapJoin.getValue()).Source = KeySelect.get(MapJoin.getKey());
        // закинем KeyValues
        for(Map.Entry<KeyExpr<K>,ValueSourceExpr> KeyValue : KeyValues.entrySet())
            KeyValue.getKey().Source = KeyValue.getValue().getString(Syntax);
        for(Map.Entry<U,JoinExpr<J,U>> MapExpr : Exprs.entrySet())
            MapExpr.getValue().StringSource = PropertySelect.get(MapExpr.getKey());

        return From;
    }

    <K> void pushJoinValues(Map<KeyExpr<K>, ValueSourceExpr> KeyValues) {

        if(source instanceof Query) {
            // заполняем статичные значения
            Map<J,ValueSourceExpr> MergeKeys = new HashMap();
            for(Map.Entry<J,SourceExpr> MapJoin : Joins.entrySet()) {
                ValueSourceExpr JoinValue = null;
                if(MapJoin.getValue() instanceof ValueSourceExpr)
                    JoinValue = (ValueSourceExpr) MapJoin.getValue();
                else {
                    ValueSourceExpr KeyValue = KeyValues.get(MapJoin.getValue());
                    if(KeyValue!=null) JoinValue = KeyValue;
                }
                if(JoinValue!=null)
                    MergeKeys.put(MapJoin.getKey(),JoinValue);
            }

            if(MergeKeys.size() > 0) {
                CollectionExtend.removeAll(Joins,MergeKeys.keySet());
                source = source.mergeKeyValue(MergeKeys,Joins.keySet());
            }
        }
    }

    boolean isEmpty(ExprTranslator Translated) {
        if(source instanceof EmptySource) return true;

        for(SourceExpr Join : Joins.values()) {
            SourceExpr JoinExpr = Join.translate(Translated);
            if(JoinExpr instanceof ValueSourceExpr && ((ValueSourceExpr)JoinExpr).Object.Value == null)
                return true;
        }

        return false;
    }
}

class MapJoin<J,U,K> extends Join<J,U> {

    // конструктор когда надо просто ключи протранслировать
    MapJoin(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource,boolean iInner) {
        super(iSource,iInner);

        for(J Implement : source.Keys)
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

        for(K Key : source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }
}

abstract class TranslateExpr<TranslateType extends TranslateExpr> {

    // транслирует (рекурсивно инстанцирует объекты с новыми входными параметрами)
    abstract TranslateType translate(ExprTranslator Translated);
}

abstract class SourceJoin<TranslateType extends SourceJoin> extends TranslateExpr<TranslateType> {

    abstract public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
    
    abstract void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins);
    abstract void fillObjectExprs(Collection<ObjectExpr> Exprs);

    public TranslateType translate(ExprTranslator Translated) {
        TranslateType Translate = (TranslateType) Translated.translate(this);
        if(Translate==null) {
            Translate = proceedTranslate(Translated);
            // нельзя, потому как например во внутреннем перемещении добавить, склады заполн. + кол-во в null
//            Translated.put(this,Translate);
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

    abstract Type getType();

    // заменяет Null на Min выражения
    SourceExpr getNullMinExpr() {
        return new IsNullSourceExpr(this, getType().getMinValueExpr());
    }
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

    void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(Source!=null) return super.getJoin(KeySource,JoinAlias, Syntax);

        Source = KeySource;
        return null;
    }

    Type getType() {
        return Type.Object;
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

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        From.fillJoins(Joins,Wheres, DependJoins);
    }

    // для fillSingleSelect'а
    String StringSource = null;
    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(StringSource!=null) return StringSource;

        return From.source.getPropertyString(Property,JoinAlias.get(From),Syntax);
    }

    Type getType() {
        return From.source.getType(Property);
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }
}

// формулы
class ValueSourceExpr extends SourceExpr {

    TypedObject Object;

    ValueSourceExpr(Object Value,Type Type) {
        Object = new TypedObject(Value,Type);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getString(Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return Object.getString(Syntax);
    }

    void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    Type getType() {
        return Object.Type;
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueSourceExpr that = (ValueSourceExpr) o;

        if (Object != null ? !Object.equals(that.Object) : that.Object != null) return false;

        return true;
    }

    public int hashCode() {
        return (Object != null ? Object.hashCode() : 0);
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

    Type getType() {
        return Operands.iterator().next().Expr.getType();
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


    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(CoeffExpr<SourceExpr> Operand : Operands)
            Operand.Expr.fillJoins(Joins,Wheres, DependJoins);
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
    Where Where;
    SourceExpr Expr;

    SelectWhereOperand(Where iWhere, SourceExpr iExpr) {
        Where = iWhere;
        Expr = iExpr;
    }

    SelectWhereOperand translate(ExprTranslator Translated) {
        Where TranslatedWhere = Where.translate(Translated);
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

    void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(CoeffExpr<SelectWhereOperand> Operand : Operands) {
            Operand.Expr.Expr.fillJoins(Joins, Wheres, DependJoins);
            Operand.Expr.Where.fillJoins(Joins, Wheres, DependJoins);
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

    Type getType() {
        return Operands.get(0).Expr.Expr.getType();
    }
}

// null перегоняет в Empty
class NullEmptySourceExpr extends SourceExpr {

    NullEmptySourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Expr.fillJoins(Joins, Wheres, DependJoins);    //To change body of overridden methods use File | Settings | File Templates.
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String ExprSource = Expr.getSource(JoinAlias, Syntax);
        String EmptySource = Expr.getType().getEmptyString();
        if(ExprSource.equals(Type.NULL)) return EmptySource;
        return Syntax.isNULL(ExprSource, EmptySource, false);
    }

    Type getType() {
        return Expr.getType();
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

// возвращает Null нужен чисто чтобы "протолкнуть" Joins
class NullJoinSourceExpr extends SourceExpr {

    NullJoinSourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Expr.fillJoins(Joins, Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Type.NULL;
    }

    Type getType() {
        return Expr.getType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransExpr = Expr.translate(Translated);
        if(TransExpr==Expr)
            return this;
        else
            return new NullJoinSourceExpr(TransExpr);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullJoinSourceExpr that = (NullJoinSourceExpr) o;

        if (!Expr.equals(that.Expr)) return false;

        return true;
    }

    public int hashCode() {
        return Expr.hashCode();
    }
}

// по аналогии возвращает Null нужен чисто чтобы "протолкнуть" Map
class NullMapSourceExpr<T> extends SourceExpr {

    NullMapSourceExpr(Map<T,SourceExpr> iMapExprs,Type iDBType) {
        MapExprs = iMapExprs;
        DBType = iDBType;
    }

    Map<T,SourceExpr> MapExprs;
    Type DBType;

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(SourceExpr Expr : MapExprs.values())
            Expr.fillJoins(Joins, Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Expr : MapExprs.values())
            Expr.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Type.NULL;
    }

    Type getType() {
        return DBType;
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {
        boolean ChangedExprs = false;
        Map<T,SourceExpr> TransMapExprs = new HashMap<T,SourceExpr>();
        for(Map.Entry<T,SourceExpr> MapExpr : MapExprs.entrySet()) {
            SourceExpr TransExpr = MapExpr.getValue().translate(Translated);
            TransMapExprs.put(MapExpr.getKey(),TransExpr);
            ChangedExprs = ChangedExprs || TransExpr!=MapExpr.getValue();
        }
        if(!ChangedExprs)
            return this;
        else
            return new NullMapSourceExpr<T>(TransMapExprs,DBType);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullMapSourceExpr that = (NullMapSourceExpr) o;

        if (!DBType.equals(that.DBType)) return false;
        if (!MapExprs.equals(that.MapExprs)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = MapExprs.hashCode();
        result = 31 * result + DBType.hashCode();
        return result;
    }
}

class CaseWhenSourceExpr extends SourceExpr {

    CaseWhenSourceExpr(Where iWhere,SourceExpr iTrueExpr,SourceExpr iFalseExpr) {
        Where=iWhere; TrueExpr=iTrueExpr; FalseExpr=iFalseExpr;
    }
    
    Where Where;
    SourceExpr TrueExpr;
    SourceExpr FalseExpr;

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Where.fillJoins(Joins,Wheres, DependJoins);
        TrueExpr.fillJoins(Joins,Wheres, DependJoins);
        FalseExpr.fillJoins(Joins,Wheres, DependJoins);
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

        if(WhereSource.length()==0)
            WhereSource = WhereSource;

        if(WhereSource.equals(StaticWhere.TRUE)) return TrueSource;
        if(WhereSource.equals(StaticWhere.FALSE)) return FalseSource;
        if(TrueSource.equals(Type.NULL) && FalseSource.equals(Type.NULL)) return Type.NULL;

        return "(CASE WHEN " + WhereSource + " THEN " + TrueSource + " ELSE " + FalseSource + " END)";
    }

    static SourceExpr getExpr(Where Where,SourceExpr Expr) {
        if(Where==StaticWhere.True) return Expr;
        return new CaseWhenSourceExpr(Where,Expr,new ValueSourceExpr(null,Expr.getType()));
    }

    Type getType() {
        return TrueExpr.getType();
    }

    public SourceExpr proceedTranslate(ExprTranslator Translated) {

        Where TransWhere = Where.translate(Translated);
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

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        PrimaryExpr.fillJoins(Joins,Wheres, DependJoins);
        SecondaryExpr.fillJoins(Joins,Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        PrimaryExpr.fillObjectExprs(Exprs);
        SecondaryExpr.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String PrimarySource = PrimaryExpr.getSource(JoinAlias, Syntax);
        String SecondarySource = SecondaryExpr.getSource(JoinAlias, Syntax);
        if(PrimarySource.equals(Type.NULL)) return SecondarySource;
        if(SecondarySource.equals(Type.NULL)) return PrimarySource;
        
        return Syntax.isNULL(PrimarySource, SecondarySource, false);
    }

    Type getType() {
        return PrimaryExpr.getType();
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

    FormulaSourceExpr(String iFormula,Type iDBType) {
        this(iFormula,new HashMap<String,SourceExpr>(),iDBType);
    }

    FormulaSourceExpr(String iFormula,Map<String,SourceExpr> iParams,Type iDBType) {
        Formula = iFormula;
        Params = iParams;
        DBType = iDBType;
    }
    
    String Formula;
    Type DBType;

    Map<String,SourceExpr> Params;

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres, DependJoins);
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

    Type getType() {
        return DBType;
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
            return new FormulaSourceExpr(Formula,TransParams,DBType); 
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
    Type DBType;

    MultiplySourceExpr(Type iDBType) {
        this(new ArrayList<SourceExpr>(),iDBType);
    }

    MultiplySourceExpr(Collection<SourceExpr> iOperands,Type iDBType) {
        Operands = iOperands;
        DBType = iDBType;
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(SourceExpr Operand : Operands)
            Operand.fillJoins(Joins,Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Operand : Operands)
            Operand.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String Source = "";
        for(SourceExpr Operand : Operands) {
            String OperandSource = Operand.getSource(JoinAlias, Syntax);
            if(OperandSource.equals(Type.NULL)) return Type.NULL;
            if(!OperandSource.equals("1"))
                Source = (Source.length()==0?"":Source+"*") + OperandSource;
        }

        if(Source.length()==0)
            return "1";
        else
            return Source;
    }

    Type getType() {
        return DBType;
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
            return new MultiplySourceExpr(TransOperands,DBType);

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

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        FormulaWhere.fillJoins(Joins,Wheres, DependJoins);
        if(NotNull)
            Wheres.add(FormulaWhere);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        FormulaWhere.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        // если NotNull нефиг 2 раза проверять
        return CaseWhenSourceExpr.getExpr((NotNull?StaticWhere.TRUE:FormulaWhere.getSource(JoinAlias, Syntax)),Syntax.getBitString(true),Type.NULL,Syntax);
    }

    Type getType() {
        return Type.Bit;
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
abstract class Where extends SourceJoin<Where> {

    public abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
}

class JoinWhere extends Where {
    Join From;

    JoinWhere(Join iFrom) {
        From=iFrom;
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        From.fillJoins(Joins,Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        From.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(From.Inner)
            return StaticWhere.TRUE;
        else
            return SourceIsNullWhere.getExpr(From.source.getInSelectString(JoinAlias.get(From)),Syntax,true);
    }

    Where proceedTranslate(ExprTranslator Translated) {
        return this;
    }
}

abstract class StaticWhere extends Where {

    final static String TRUE = "1=1";
    final static String FALSE = "1<>1";

    public Where proceedTranslate(ExprTranslator Translated) {
        return this;
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
    }

    static TrueWhere True = new TrueWhere();
    static FalseWhere False = new FalseWhere();
}

class TrueWhere extends StaticWhere {

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return TRUE;
    }
}

class FalseWhere extends StaticWhere {

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return FALSE;
    }
}

class FieldExprCompareWhere extends Where {

    SourceExpr Source;
    Object Value;

    static int EQUALS = 0;
    static int GREATER = 1;
    static int LESS = 2;
    static int GREATER_EQUALS = 3;
    static int LESS_EQUALS = 4;
    static int NOT_EQUALS = 5;

    int Compare;

    FieldExprCompareWhere(SourceExpr iSource,Object iValue,int iCompare) {
        Source=iSource;
        Compare=iCompare;

        Value=iValue;
        if(Value==null)
            Value = Source.getType().getMinValue();
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String KeySource = Source.getSource(JoinAlias, Syntax);
        String ValueSource = Value instanceof String ? "'" + Value + "'" : (Value instanceof SourceExpr ? ((SourceExpr) Value).getSource(JoinAlias, Syntax) : Value.toString());
        if(KeySource.equals(ValueSource)) return ((Compare==EQUALS || Compare==GREATER_EQUALS || Compare==LESS_EQUALS)?StaticWhere.TRUE:StaticWhere.FALSE);
        return KeySource + (Compare==EQUALS?"=":(Compare==GREATER?">":(Compare==LESS?"<":(Compare==GREATER_EQUALS?">=":(Compare==LESS_EQUALS?"<=":"<>"))))) + ValueSource;
    }

    public Where proceedTranslate(ExprTranslator Translated) {

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

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Source.fillJoins(Joins,Wheres, DependJoins);
        if(Value instanceof SourceExpr)
            ((SourceExpr)Value).fillJoins(Joins,Wheres, DependJoins);
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

class SourceIsNullWhere extends Where {
    SourceIsNullWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getExpr(Source.getSource(JoinAlias,Syntax),Syntax,Not);
    }

    static String getExpr(String SourceString,SQLSyntax Syntax,boolean Not) {
        if(SourceString.equals(Type.NULL)) return Not?StaticWhere.FALSE:StaticWhere.TRUE;
        return (Not?NotWhere.PREFIX:"") + SourceString + " IS NULL";
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Source.fillObjectExprs(Exprs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public Where proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransSource = Source.translate(Translated);
        if(TransSource==Source)
            return this;
        else
            return new SourceIsNullWhere(TransSource,Not);
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Source.fillJoins(Joins,Wheres, DependJoins);
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

class FieldOPWhere extends Where {
    FieldOPWhere(Where iOp1, Where iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    Where Op1, Op2;
    boolean And;

    static Where getWhere(Collection<Where> Wheres, boolean And) {
        // делаем OR на InJoin'ы этих Source'ов
        Where AllWheres = null;
        for(Where Where : Wheres) {
            if(Where==StaticWhere.True) {
                if(!And) return Where;
            } else
            if(Where==StaticWhere.False) {
                if(And) return Where;
            } else
                AllWheres = (AllWheres==null?Where:new FieldOPWhere(Where, AllWheres, And));
        }

        return (AllWheres==null?(And?StaticWhere.False:StaticWhere.True):AllWheres);
    }

    static Where getWhere(Where Where1, Where Where2, boolean And) {
        Collection<Where> Wheres = new ArrayList<Where>();
        Wheres.add(Where1);
        Wheres.add(Where2);
        return getWhere(Wheres,And);
    }

    static String getExpr(String Op1Source,String Op2Source, boolean And) {
        if(Op1Source.equals(StaticWhere.TRUE)) return And?Op2Source:StaticWhere.TRUE;
        if(Op1Source.equals(StaticWhere.FALSE)) return And?StaticWhere.FALSE:Op2Source;
        return "(" + Op1Source + " " + (And?"AND":"OR") + " " + Op2Source + ")";

    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Op1.fillObjectExprs(Exprs);
        Op2.fillObjectExprs(Exprs);
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Op1.fillJoins(Joins,Wheres, DependJoins);
        Op2.fillJoins(Joins,Wheres, DependJoins);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getExpr(Op1.getSource(JoinAlias,Syntax),Op2.getSource(JoinAlias, Syntax),And);
    }

    public Where proceedTranslate(ExprTranslator Translated) {
        Where TransOp1 = Op1.translate(Translated);
        Where TransOp2 = Op2.translate(Translated);
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

class NotWhere extends Where {

    Where Where;

    NotWhere(Where iWhere) {
        Where = iWhere;
    }

    final static String PREFIX = "NOT ";

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String WhereSource = Where.getSource(JoinAlias, Syntax);
        if(WhereSource.equals(StaticWhere.TRUE)) return StaticWhere.FALSE;
        if(WhereSource.equals(StaticWhere.FALSE)) return StaticWhere.TRUE;
        // изврат конечно, но лень еще один уровень абстракции прошивать
        if(WhereSource.startsWith(PREFIX)) return WhereSource.substring(PREFIX.length());
        return PREFIX + WhereSource;
    }

    public Where proceedTranslate(ExprTranslator Translated) {
        Where TranslatedWhere = Where.translate(Translated);
        if(TranslatedWhere.equals(Where))
            return this;
        else
            return new NotWhere(TranslatedWhere);
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Where.fillJoins(Joins, Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Where.fillObjectExprs(Exprs);
    }
}

class FieldSetValueWhere extends Where {

    FieldSetValueWhere(SourceExpr iExpr,Collection<Integer> iSetValues) {Expr=iExpr; SetValues=iSetValues;};
    SourceExpr Expr;
    Collection<Integer> SetValues;

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String ExprSource = Expr.getSource(JoinAlias, Syntax);
        if(SetValues.contains(ExprSource))
            return StaticWhere.TRUE;
        else {
            String ListString = "";
            for(Integer Value : SetValues)
                ListString = (ListString.length()==0?"":ListString+',') + Value;
            return ExprSource + " IN (" + ListString + ")";
        }
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        Expr.fillObjectExprs(Exprs);
    }

    public Where proceedTranslate(ExprTranslator Translated) {
        SourceExpr TransExpr = Expr.translate(Translated);
        if(TransExpr==Expr)
            return this;
        else
            return new FieldSetValueWhere(TransExpr,SetValues);
    }

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        Expr.fillJoins(Joins,Wheres, DependJoins);
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

class FormulaSourceWhere extends Where {

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

    public void fillJoins(List<Join> Joins, Set<Where> Wheres, Set<Join> DependJoins) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres, DependJoins);
    }

    void fillObjectExprs(Collection<ObjectExpr> Exprs) {
        for(SourceExpr Param : Params.values())
            Param.fillObjectExprs(Exprs);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String SourceString = Formula;

        for(Map.Entry<String,SourceExpr> Prm : Params.entrySet())
            SourceString = SourceString.replace(Prm.getKey(), Prm.getValue().getSource(JoinAlias, Syntax));

         return SourceString;
     }

    public Where proceedTranslate(ExprTranslator Translated) {
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

abstract class
        SelectQuery<K,V> extends Query<K,V> {

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

    public SourceJoin translate(SourceJoin ToTranslate) {
        return get(ToTranslate);
    }
}

// запрос Join
class JoinQuery<K,V> extends SelectQuery<K,V> {
    protected Map<V,SourceExpr> Properties = new HashMap();
    protected Set<Where> Wheres = new HashSet();

    Map<K,SourceExpr> MapKeys = new HashMap();

    // скомпилированные св-ва
    List<Join> Joins = new ArrayList();

    void add(V Property,SourceExpr Expr) {
        Expr.fillJoins(Joins,Wheres,new HashSet<Join>());
        Properties.put(Property,Expr);
    }

    public void addAll(Map<? extends V,? extends SourceExpr> AllProperties) {
        for(Map.Entry<? extends V,? extends SourceExpr> MapProp : AllProperties.entrySet())
            add(MapProp.getKey(),MapProp.getValue());
    }

    void add(Where Where) {
        Where.fillJoins(Joins,Wheres,new HashSet<Join>());
        Wheres.add(Where);
    }

    void add(Join Join) {
        Join.fillJoins(Joins,Wheres,new HashSet<Join>());
    }

    Map<KeyExpr<K>,ValueSourceExpr> KeyValues = new HashMap();

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

    void putDumbJoin(Map<K,Integer> KeyValues) {

        Map<K,ValueSourceExpr> KeyExprs = new HashMap<K,ValueSourceExpr>();
        for(Map.Entry<K,Integer> MapKey : KeyValues.entrySet())
            KeyExprs.put(MapKey.getKey(),new ValueSourceExpr(MapKey.getValue(),Type.Object));

        Join<K,Object> DumbJoin = new Join<K,Object>(new DumbSource<K,Object>(KeyExprs,new HashMap()),true);
        for(K Object : KeyValues.keySet())
            DumbJoin.Joins.put(Object,MapKeys.get(Object));
        add(DumbJoin);
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
    void checkTranslate(ExprTranslator Translated,List<Join> TranslatedJoins,Set<Where> TranslatedWheres) {
        // проверяем SourceWheres
        Set<ObjectExpr> NewExprs;
        for(Join TransJoin : TranslatedJoins) {
            NewExprs = new HashSet();
            TransJoin.fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
/*
            if(TransJoin.Inner) {
                List<Join> CheckLeft = new ArrayList();
                TransJoin.fillJoins(CheckLeft,new HashSet());
                for(Join LeftJoin : CheckLeft)
                    if(!LeftJoin.Inner)
                        throw new RuntimeException("Inner To Left");
            }*/
        }

        for(SourceExpr PropertyValue : Properties.values()) {
            NewExprs = new HashSet();
            PropertyValue.translate(Translated).fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
        }

        for(Where Where : TranslatedWheres) {
            NewExprs = new HashSet();
            Where.translate(Translated).fillObjectExprs(NewExprs);
            checkExprs(NewExprs,TranslatedJoins);
        }
    }


    Source<K, V> proceedCompile() {

        // из всех propertyViews,Wheres вытягивают Joins, InnerJoins, Where
        ExprTranslator Translated = new ExprTranslator();
        List<Join> TranslatedJoins = new ArrayList();
        Set<Where> TranslatedWheres = new HashSet();

        for(Join Join : Joins) {
            Join.source = Join.source.compile();
            if(Join.isEmpty(Translated) && Join.Inner)
                return getEmptySource();
            else
                Join.source.compileJoins(Join, Translated, TranslatedWheres, TranslatedJoins, false);
        }

        // перетранслируем св-ва в основном чтобы не пошли транслироваться (что надо при Merge) в DumbSource'ах константы - убивая тем самым Where
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            MapProperty.setValue(MapProperty.getValue().translate(Translated));
        for(Where Where : Wheres)
            TranslatedWheres.add(Where.translate(Translated));
        // проверим если вдруг Translat'ился ключ закинем сразу в Source
        for(SourceExpr Key : MapKeys.values()) {
            SourceExpr ValueKey = Key.translate(Translated);
            if(Key!=ValueKey)
                KeyValues.put((KeyExpr<K>) Key,(ValueSourceExpr)ValueKey);
        }

//        checkTranslate(Translated,TranslatedJoins,TranslatedWheres);
        
        Translated = new ExprTranslator();

        // флаг что было A J (A FJ B) -> A LJ B и теперь B тоже по хорошему надо слить
        boolean UnionToJoin = false;

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
                Join MergedSource = ToMerge.merge(Check, Translated, Translated, JoinTranslated, JoinTranslated, null);

                if(MergedSource!=null) {
                    // надо merge'нуть потому как Joins'ы могут быть транслированы                    
                    Translated.merge(JoinTranslated);

                    ToMergeTranslated = true;
                    itOperand.remove();

                    if(MergedSource.source instanceof JoinQuery && MergedSource.Inner)
                        UnionToJoin = true;

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

//        checkTranslate(Translated,MergedJoins,TranslatedWheres);

        // перетранслируем все
        Joins = MergedJoins;
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            MapProperty.setValue(MapProperty.getValue().translate(Translated));
        Wheres = new HashSet();
        for(Where Where : TranslatedWheres)
            Wheres.add(Where.translate(Translated));

        // еще раз "закомпайлим"
        if(UnionToJoin)
            return proceedCompile();
        
        // протолкнем внутрь значения
        for(Join<?,?> Join : Joins)
            Join.pushJoinValues(KeyValues);

        return this;
    }

    // MergedInnerJoins - Join'ы которые были Inner'ами в Merge и замапились на Left'ы
    private boolean recMerge(ListIterator<Join> RecJoin, Collection<Join> ToMergeJoins, ExprTranslator Translated, ExprTranslator MergeTranslated, Set<Join> ProceededJoins, Set<Join> MergedJoins, Set<Join> MergedInnerJoins, List<Join> TranslatedJoins, MergeType Type) {
        if(!RecJoin.hasNext())
            return true;

        Join<?,?> CurrentJoin = RecJoin.next();

        for(Join<?,?> ToMergeJoin : ToMergeJoins)
            if(!MergedJoins.contains(ToMergeJoin) && !(CurrentJoin.Inner && !ToMergeJoin.Inner)) { // Inner'ы только на Inner'ы
                // надо бы проверить что Join уже не merge'ут
                ExprTranslator JoinTranslated = new ExprTranslator();
                ExprTranslator JoinMergeTranslated = new ExprTranslator();
                Join<?,?> MergedJoin = CurrentJoin.merge(ToMergeJoin, Translated, MergeTranslated, JoinTranslated, JoinMergeTranslated, Type);
                if(MergedJoin!=null) {
                    if(ToMergeJoin.Inner && !CurrentJoin.Inner) MergedInnerJoins.add(MergedJoin);
                    ProceededJoins.add(CurrentJoin); MergedJoins.add(ToMergeJoin); TranslatedJoins.add(MergedJoin);
                    Translated.putAll(JoinTranslated); MergeTranslated.putAll(JoinMergeTranslated);
                    if(recMerge(RecJoin, ToMergeJoins, Translated, MergeTranslated, ProceededJoins, MergedJoins, MergedInnerJoins, TranslatedJoins, Type))
                        return true;
                    if(ToMergeJoin.Inner && !CurrentJoin.Inner) MergedInnerJoins.remove(MergedJoin);
                    ProceededJoins.remove(CurrentJoin); MergedJoins.remove(ToMergeJoin); TranslatedJoins.remove(MergedJoin);
                    CollectionExtend.removeAll(Translated,JoinTranslated.keySet()); CollectionExtend.removeAll(MergeTranslated,JoinMergeTranslated.keySet());
                }
            }

        if(!CurrentJoin.Inner && recMerge(RecJoin, ToMergeJoins, Translated, MergeTranslated, ProceededJoins, MergedJoins, MergedInnerJoins, TranslatedJoins, Type))
            return true;

        RecJoin.previous();

        return false;
    }

    <MK,MV> Source<K, Object> proceedMerge(Source<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type, boolean MergeEqual) {
        // Set<Join> merge'им с Set<Join> если совпадают, докидываем Left Joins (пытаясь попарно слить), проверяем на совпадение SourceWheres
        // для сравнения Join'ов (как и Where) нужны сразу же "перекодированные" выражения									так как сами бежим по Map'ам то перекодируем
        // накапливаем карты слияний , по умолчанию в этот Map закидываем KeyExpr'ы, затем они накапливаются
        // также надо затем слить Property причем сравнив чтобы не дать одинаковые

        int InnerCount = 0;
        for(Join Join : Joins)
            if(Join.Inner) InnerCount++;

        // если не JoinQuery преобразуем и пробуем слить с JoinQuery
        if(!(Merge instanceof JoinQuery))
            return proceedMerge(Merge.getCompiledJoinQuery(),MergeKeys,MergeProps,Type,MergeEqual);

        if(Type==MergeType.RIGHT)
            throw new RuntimeException("Вообще не должно быть");
        
        JoinQuery<MK,MV> MergeJoin = (JoinQuery<MK,MV>) Merge;

        // также надо бы проверить на кол-во Inner Join'ов, а также SourceWherers совпадает
        if(Wheres.size()>MergeJoin.Wheres.size() || (!MergeEqual && Wheres.size()==MergeJoin.Wheres.size())) return null;

        int InnerMergeCount = 0;
        for(Join Join : MergeJoin.Joins)
            if(Join.Inner) InnerMergeCount++;

        if(InnerCount>InnerMergeCount || (!MergeEqual && InnerCount==InnerMergeCount)) return null;

        if(Type==MergeType.EQUAL && !(InnerCount==InnerMergeCount && Wheres.size()==MergeJoin.Wheres.size())) return null;

        JoinQuery<K,Object> Result = new JoinQuery<K,Object>(Keys);
        Result.Compiled = Result;

        // попытаемся все ключи помапить друг на друга
        Set<Join> ProceededJoins = new HashSet<Join>();
        Set<Join> MergedJoins = new HashSet<Join>();
        Set<Join> MergedInnerJoins = new HashSet<Join>();
        // замапим ключи
        ExprTranslator Translated = new ExprTranslator();
        ExprTranslator MergeTranslated = new ExprTranslator();
        for(Map.Entry<K,SourceExpr> MapKey : Result.MapKeys.entrySet()) {
            Translated.put(MapKeys.get(MapKey.getKey()),MapKey.getValue());
            MergeTranslated.put(MergeJoin.MapKeys.get(MergeKeys.get(MapKey.getKey())),MapKey.getValue());
        }

        // сначала если повезет попробуем слить на EQUAL (тогда не надо может будет сложная ветка с LEFT JOIN'ом)
        if(recMerge(Joins.listIterator(), MergeJoin.Joins, Translated, MergeTranslated, ProceededJoins, MergedJoins, MergedInnerJoins, Result.Joins, MergeType.EQUAL)) {
            // если нужны ключи на LEFT или FULL, переделаем Join'ы в Outer'ы
            if(Type==MergeType.LEFT || Type==MergeType.FULL)
                for(Join Join : MergedInnerJoins)
                    Join.Inner = false;

            // пробежимся по всем необработанным Join'ам и Translate'им их
            for(Join Join : Joins)
                if(!ProceededJoins.contains(Join))
                    Result.Joins.add(Join.translate(Translated,Translated,false));
            for(Join Join : MergeJoin.Joins)
                if(!MergedJoins.contains(Join)) {
                    Join TranslatedJoin = Join.translate(MergeTranslated, MergeTranslated, true);
                    if(Join.Inner) MergedInnerJoins.add(TranslatedJoin);
                    Result.Joins.add(TranslatedJoin);
                }

            Collection<Where> MergedWheres = new HashSet<Where>();
            for(Where Where : MergeJoin.Wheres)
                MergedWheres.add(Where.translate(MergeTranslated));
            // проверим что SourceWheres совпадают (потом может закинем в recMerge)
            for(Where Where : Wheres) {
                Where TranslatedWhere = Where.translate(Translated);
                if(!MergedWheres.remove(TranslatedWhere)) return null;
                Result.Wheres.add(TranslatedWhere);
            }

            // условие куда будет складываться
            Where MergedExprWhere = StaticWhere.True;
            if(Type==MergeType.LEFT || Type==MergeType.FULL) {
                // закинем в MergedWheres Inner Join
                for(Join Join : MergedInnerJoins)
                    MergedWheres.add(Join.InJoin);
                if(!MergedWheres.isEmpty())
                    MergedExprWhere = FieldOPWhere.getWhere(MergedWheres, true);
            } else // Inner может появиться после
                Result.Wheres.addAll(MergedWheres);

            // сделаем Map в обратную сторону, чтобы не кидать
            Map<SourceExpr,Object> BackProperties = new HashMap();
            for(Map.Entry<V,SourceExpr> MapProp : Properties.entrySet()) {
                SourceExpr TransExpr = MapProp.getValue().translate(Translated);

                Result.Properties.put(MapProp.getKey(),TransExpr);
                BackProperties.put(TransExpr,MapProp.getKey());
            }
            for(Map.Entry<MV,SourceExpr> MapProp : MergeJoin.Properties.entrySet()) {
                SourceExpr TransExpr = CaseWhenSourceExpr.getExpr(MergedExprWhere,MapProp.getValue().translate(MergeTranslated));

                Object PropertyObject = BackProperties.get(TransExpr);
                if(PropertyObject==null) {
                    PropertyObject = new Object();
                    Result.Properties.put(PropertyObject,TransExpr);
                }
                MergeProps.put(MapProp.getKey(),PropertyObject);
            }

            return Result;
        }

        // теперь гоним на LEFT, если проходит делаем LJ
        if((Type==MergeType.LEFT || Type==MergeType.FULL) && recMerge(Joins.listIterator(), MergeJoin.Joins, Translated, MergeTranslated, ProceededJoins, MergedJoins, MergedInnerJoins, Result.Joins, MergeType.LEFT)) {
            // делаем LEFT JOIN
            JoinQuery<K,Object> MergedResult = new JoinQuery<K,Object>(Keys);
            MergedResult.addAll((new UniJoin<K, V>(this, MergedResult, true)).Exprs);
            Join<MK,MV> MergedLeftJoin = new MapJoin<MK, MV, K>(MergeJoin, MergedResult, MergeKeys, false);
            for(MV Property : MergeJoin.getProperties()) {
                // нужно пересоздавать Object а то пересекутся
                Object MergeProperty = new Object();
                MergeProps.put(Property,MergeProperty);
                MergedResult.add(MergeProperty,MergedLeftJoin.Exprs.get(Property));
            }

            return MergedResult.compile();
        }
  

        return null;
    }

    // перетранслирует
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Where> TranslatedWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        // если есть JoinQuery (пока если Left без Wheres) то доливаем все Join'ы с сооты. Map'ами, JoinExpr заменяем на SourceExpr'ы с перебитыми KeyExpr'ами и соотв. JoinRxpr'ами
        Outer = Outer || !Join.Inner;
//        if((!Outer || Wheres.size()==0)) {
            // закинем перекодирование ключей
            for(Map.Entry<K,SourceExpr> MapKey : MapKeys.entrySet())
                Translated.put(MapKey.getValue(),Join.Joins.get(MapKey.getKey()).translate(Translated));

            // нужно также рекурсию гнать на случай LJ (A J (B FJ C))
            for(Join CompileJoin : Joins)
                CompileJoin.source.compileJoins(CompileJoin, Translated, TranslatedWheres, TranslatedJoins, Outer);

            Collection<Where> JoinWheres = new ArrayList<Where>();
            for(Where Where : Wheres)
                JoinWheres.add(Where.translate(Translated));
        
            Where PropertyWhere = StaticWhere.True;
            if(Outer) {
                if(!JoinWheres.isEmpty())
                    PropertyWhere = FieldOPWhere.getWhere(JoinWheres,true);
            } else
                // закинем в общие Where
                TranslatedWheres.addAll(JoinWheres);

            for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
                Translated.put(Join.Exprs.get(MapProperty.getKey()), CaseWhenSourceExpr.getExpr(PropertyWhere,MapProperty.getValue().translate(Translated)));

            // InJoin'ы заменим на AND Inner InJoin'ов
            List<Where> CompileInJoin = new ArrayList();
            for(Join CompileJoin : Joins)
                if(CompileJoin.Inner)
                    CompileInJoin.add(CompileJoin.InJoin);
            Translated.put(Join.InJoin,FieldOPWhere.getWhere(PropertyWhere,FieldOPWhere.getWhere(CompileInJoin,true).translate(Translated),true));

  //      } else
  //          super.compileJoins(Join, Translated, TranslatedWheres, TranslatedJoins, Outer);
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
        // погнали propertyViews заполнять
        for(Map.Entry<V,SourceExpr> JoinProp : Properties.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(JoinAlias, Syntax);
            if(PropertyValue.equals(Type.NULL))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getType());
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        for(Where Where : Wheres) {
            String WhereString = Where.getSource(JoinAlias, Syntax);
            if(!WhereString.equals(StaticWhere.TRUE))
                WhereSelect.add(WhereString);
        }

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
        for(Map.Entry<KeyExpr<K>,ValueSourceExpr> KeyValue : KeyValues.entrySet())
            KeyValue.getKey().Source = KeyValue.getValue().getString(Syntax);

        Map<Join,String> JoinAlias = new HashMap();
        String From = "";
/*        // теперь определим KeyExpr'ы заодно строя Join'ы
        for(Join Join : Joins)
            if(Join.Inner)
                From = (From.length()==0?"":From + " JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null), Syntax);

        if(From.length()==0)
            From = "dumb";

        Collection<KeyExpr<K>> InnerNullKeys = new ArrayList();
        for(SourceExpr Key : MapKeys.values())
            if(((KeyExpr<K>)Key).Source==null)
                throw new RuntimeException("Not Enough Inner Keys");

        for(Join Join : Joins)
            if(!Join.Inner)
                From = From + " LEFT JOIN " + Join.getFrom(JoinAlias,null, Syntax);

/*        List<Join> ArrangedJoins = new ArrayList<Join>();
        Set<KeyExpr> FilledKeys = new HashSet<KeyExpr>(KeyValues.keySet());
        LinkedHashMap<Join,Set<KeyExpr>> LeftWait = new LinkedHashMap<Join, Set<KeyExpr>>();
        for(Join<?,?> Join : Joins) {
            // находим все ключи
            Set<KeyExpr> KeyJoins = Join.getKeyJoins();
            if(Join.Inner) {
                ArrangedJoins.add(Join);
                FilledKeys.addAll(KeyJoins);
            } else
                LeftWait.put(Join,KeyJoins);

            for(Iterator<Map.Entry<Join,Set<KeyExpr>>> i = LeftWait.entrySet().iterator();i.hasNext();) {
                Map.Entry<Join,Set<KeyExpr>> Left = i.next();
                if(FilledKeys.containsAll(Left.getValue())) { // дождались
                    ArrangedJoins.add(Left.getKey());
                    i.remove();
                }
            }
        }
        if(FilledKeys.size()!=MapKeys.size())
            throw new RuntimeException("Not Enough Inner Keys");
        Joins = ArrangedJoins;
  */
        if(Joins.isEmpty() || !Joins.get(0).Inner)
            From = "dumb";
        for(Join Join : Joins)
           From = (From.length()==0?"":From + (Join.Inner?"":" LEFT")+" JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null), Syntax);

        fillSources(JoinAlias, KeySelect, PropertySelect, WhereSelect, Syntax);

        return From;
    }

    public boolean readEmpty(DataSession Session) throws SQLException {
        OrderedJoinQuery<K,V> Cloned = new OrderedJoinQuery<K,V>(Keys);
        Cloned.MapKeys = MapKeys;
        Cloned.Wheres.addAll(Wheres);
        Cloned.Properties.putAll(Properties);
        Cloned.Joins.addAll(Joins);
        Cloned.Top = 1;
        return Cloned.executeSelect(Session).size()==0;
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
        Query.KeyValues.putAll(KeyValues);
        for(SourceExpr Order : Orders.keySet())
            Query.add(Order,Order);

        Map<K,String> KeySelect = new HashMap();
        Map<Object,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        // закинем в propertyViews
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

    abstract public Type getType();

    // есть 3-ка оператор 
    boolean containsInUnion() {
        return false;
    }
}

class EmptyUnion<K> extends ExprUnion<K> {

    Type DBType;

    EmptyUnion(Type iDBType) {
        DBType = iDBType;
    }

    SourceExpr translateExpr(Map<Union<K, Object>, Join<K, ?>> MapJoins, ExprTranslator Translated) {
        return new ValueSourceExpr(null,DBType);
    }

    ExprUnion proceedTranslate(Map<PropertyUnion, ExprUnion> ToTranslate, Map<ExprUnion, ExprUnion> Translated) {
        return this;
    }

    String getString(Map<Union<K, Object>, String> MapAliases, SQLSyntax Syntax) {
        return Type.NULL;
    }

    void fillUnions(Collection<Union> Unions) {
    }

    public Type getType() {
        return DBType;
    }
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

    public Type getType() {
        return From.Source.getType(Property);
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
                Collection<Where> InJoins = new ArrayList();
                for(Union Union : Unions)
                    InJoins.add(MapJoins.get(Union).InJoin.translate(Translated));
                
                UnionExpr.Operands.add(new CoeffExpr<SelectWhereOperand>(new SelectWhereOperand(FieldOPWhere.getWhere(InJoins,true),Operand.Expr.translateExpr(MapJoins, Translated)),Operand.Coeff));
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
                    String NotInSource = StaticWhere.TRUE;
                    for(Union Union : Unions)
                        NotInSource = FieldOPWhere.getExpr(NotInSource,SourceIsNullWhere.getExpr(Union.Source.getInSelectString(MapAliases.get(Union)),Syntax,false),true);

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

    public Type getType() {
        return Operands.iterator().next().Expr.getType();
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

    boolean containsInUnion() {
        if(Operator==3) return true;

        for(CoeffExpr<ExprUnion<K>> Operand : Operands)
            if(Operand.Expr.containsInUnion()) return true;

        return false;
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
    <MK,MV> void fillUnionTranslate(Union<MK,MV> TranslatedUnion,Map<V,Object> MergeProps, Map<PropertyUnion,ExprUnion> UnionTranslated) {
        for(Map.Entry<V,PropertyUnion<K,V>> MapUnion : Exprs.entrySet())
            UnionTranslated.put(MapUnion.getValue(),TranslatedUnion.Exprs.get(MergeProps==null?MapUnion.getKey():MergeProps.get(MapUnion.getKey())));
    }

    <MK,MV> Union<K, Object> merge(Union<MK,MV> Merge, Map<K,MK> MergeKeys, Map<PropertyUnion, ExprUnion> UnionTranslate, Map<PropertyUnion, ExprUnion> UnionMergeTranslate,boolean ContainsInUnion) {
        Map<MV, Object> MergeProps = new HashMap<MV, Object>();
        Source<K, Object> MergeSource = Source.merge(Merge.Source, MergeKeys, MergeProps, ContainsInUnion?Source.EQUAL:Source.FULL);
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

    Collection<Union<K,Object>> Operands = new ArrayList();
    Map<V, ExprUnion<K>> Operations = new HashMap();

    UnionQuery(Collection<? extends K> iKeys,int iDefaultOperator) {
        super(iKeys);
        DefaultOperator=iDefaultOperator;
    }
    UnionQuery(Collection<? extends K> iKeys,Collection<Union<K,Object>> iOperands) {
        super(iKeys);
        Operands = iOperands;
    }

    // Safe - кдючи чисто никаких чисел типа 1 с которыми проблемы
    static String getExpr(List<CoeffExpr<String>> Operands, int Operator, boolean Safe, SQLSyntax Syntax) {
        String Result = "";
        String SumNull = "";

        // вырежем null'ы именно заранее потому как Sum интересует кол-во операндов
        for(Iterator<CoeffExpr<String>> i = Operands.iterator();i.hasNext();)
            if(i.next().Expr.equals(Type.NULL))
                i.remove();

        if(Operands.size()==0)
            return Type.NULL;
        
        for(CoeffExpr<String> Operand : Operands) {
            String OperandString = Operand.getCoeff();
            if(Operator==1 && Operands.size()>1) {
                OperandString += Syntax.isNULL(Operand.Expr,"0", false);
                SumNull = (SumNull.length()==0?"":SumNull+" AND ") + SourceIsNullWhere.getExpr(Operand.Expr,Syntax,false);
            } else
                OperandString += Operand.Expr;

            if(Result.length()==0) {
                Result = OperandString;
            } else {
                switch(Operator) {
                    case 2:
                        if(!OperandString.equals(Type.NULL)) {
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
                        if(!OperandString.equals(Type.NULL)) {
                            if(Syntax.isGreatest())
                                Result = OperandString + "," + Result;
                            else
                                Result = CaseWhenSourceExpr.getExpr(FieldOPWhere.getExpr(SourceIsNullWhere.getExpr(OperandString,Syntax,false),Result+">"+OperandString,false),Result,OperandString,Syntax);
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
            return CaseWhenSourceExpr.getExpr(SumNull,Type.NULL,Result,Syntax);
        else
            return "("+Result+")";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        if(compile()!=this) return compile().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

        // если операнд один сделаем JoinQuery, он его "раскроет"
        if(Operands.size()==1)
            return getCompiledJoinQuery().fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

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

                FromSource = " FULL JOIN " + FromSource + " ON " + (JoinOn.length()==0?StaticWhere.TRUE:JoinOn);
            }

            From += FromSource;
        }

        // ключи (по NVL)
        for(K Key : Keys)
            KeySelect.put(Key,getExpr(UnionKeys.get(Key),2,true,Syntax));
        
        for(Map.Entry<V, ExprUnion<K>> Property : Operations.entrySet())
            PropertySelect.put(Property.getKey(),Property.getValue().getString(MapAliases, Syntax));

        return From;
    }

    int DefaultOperator;
    void add(Source<? extends K,V> Source,Integer Coeff) {

        // докидываем с DefaultOperator'ом
        Union<K,V> Union = new Union<K,V>((Source<K,V>) Source);
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

    Type getType(V Property) {
        return Operations.get(Property).getType();
    }

    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Where> TranslatedWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        // если есть UnionQuery не в Inner Join'ах или же у UnionQuery один операнд то из операндов делаем Join'ы, JoinExpr'ы заменяем на UnionSourceExpr'ы из UnionQuery с проставленными JoinExpr'ами
        Outer = Outer || !Join.Inner;
        if((Outer || Operands.size()==1)) {
            Map<Union<K,Object>,Join<K,?>> OperandJoins = new HashMap();
            // делаем Join'ы
            for(Union<K, Object> Operand : Operands) {
                Join<K,Object> JoinOperand = new Join<K,Object>(Operand.Source,Join.Joins,!Outer);
                OperandJoins.put(Operand,JoinOperand);

                Operand.Source.compileJoins(JoinOperand, Translated, TranslatedWheres, TranslatedJoins, Outer);
            }

            // InJoin'ы заменим на OR Inner InJoin'ов
            List<Where> CompileInJoin = new ArrayList();
            for(Join CompileJoin : OperandJoins.values())
                CompileInJoin.add(CompileJoin.InJoin);
            Translated.put(Join.InJoin,FieldOPWhere.getWhere(CompileInJoin,false).translate(Translated));
            /// все выражений протранслируем
            for(V Property : getProperties())
                Translated.put(Join.Exprs.get(Property),Operations.get(Property).translateExpr(OperandJoins, Translated));
        } else
            super.compileJoins(Join, Translated, TranslatedWheres, TranslatedJoins, Outer);
    }

    void compileUnions(Union<K, V> Union, Map<PropertyUnion, ExprUnion> ToTranslate, Collection<Union<K, Object>> TranslatedUnions) {

        TranslatedUnions.addAll(Operands);
        // закинем перекодирования (Map'а св-в)
        for(Map.Entry<V,PropertyUnion<K,V>> MapProperty : Union.Exprs.entrySet())
            ToTranslate.put(MapProperty.getValue(),Operations.get(MapProperty.getKey()));
    }

    private <MK> boolean recMerge(ListIterator<Union<MK, Object>> RecUnion, Collection<Union<K,Object>> ToMergeUnions, Map<K,MK> MergeKeys, Map<PropertyUnion,ExprUnion> ToTranslate, Map<PropertyUnion,ExprUnion> ToMergeTranslate, Set<Union<K,Object>> MergedUnions, Collection<Union<K,Object>> TranslatedUnions, boolean ContainsInUnion) {

        if(!RecUnion.hasNext())
            return true;

        Union<MK, Object> CurrentUnion = RecUnion.next();

        for(Union<K, Object> ToMergeUnion : ToMergeUnions)
            if(!MergedUnions.contains(ToMergeUnion)) {
                // надо бы проверить что Join уже не merge'ут
                Map<PropertyUnion,ExprUnion> UnionToTranslate = new HashMap<PropertyUnion, ExprUnion>();
                Map<PropertyUnion,ExprUnion> UnionToMergeTranslate = new HashMap<PropertyUnion, ExprUnion>();
                Union<K, Object> MergedUnion = ToMergeUnion.merge(CurrentUnion, MergeKeys, UnionToTranslate, UnionToMergeTranslate, ContainsInUnion);
                if(MergedUnion!=null) {
                    MergedUnions.add(ToMergeUnion); TranslatedUnions.add(MergedUnion);
                    ToTranslate.putAll(UnionToTranslate); ToMergeTranslate.putAll(UnionToMergeTranslate);
                    if(recMerge(RecUnion, ToMergeUnions, MergeKeys, ToTranslate, ToMergeTranslate, MergedUnions, TranslatedUnions, ContainsInUnion))
                        return true;
                    MergedUnions.remove(ToMergeUnion); TranslatedUnions.remove(MergedUnion);
                    CollectionExtend.removeAll(ToTranslate,UnionToTranslate.keySet()); CollectionExtend.removeAll(ToMergeTranslate,UnionToMergeTranslate.keySet());
                }
            }

        RecUnion.previous();

        return false;
    }

    <MK,MV> Source<K, Object> proceedMerge(Source<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type, boolean MergeEqual) {

        // если не JoinQuery преобразуем и пробуем слить с JoinQuery
        if(!(Merge instanceof UnionQuery))
            return proceedMerge(Merge.getUnionQuery(2),MergeKeys,MergeProps,Type,MergeEqual);

        UnionQuery<MK,MV> MergeUnion = (UnionQuery<MK,MV>)Merge;

        if(Operands.size()<MergeUnion.Operands.size() || (!MergeEqual && Operands.size()==MergeUnion.Operands.size())) return null;

        // для equal'ов должны совпадать операнды
        if(Type==Source.MergeType.EQUAL && !(Operands.size()==MergeUnion.Operands.size())) return null;

        UnionQuery<K,Object> Result = new UnionQuery<K,Object>(Keys,0);
        Result.Compiled = Result;

        // сливаем все Source
        Map<PropertyUnion,ExprUnion> ToTranslate = new HashMap<PropertyUnion,ExprUnion>();
        Map<PropertyUnion,ExprUnion> ToMergeTranslate = new HashMap<PropertyUnion,ExprUnion>();
        Set<Union<K,Object>> MergedUnions = new HashSet<Union<K, Object>>();
        if(recMerge((new ArrayList<Union<MK,Object>>(MergeUnion.Operands)).listIterator(),Operands,MergeKeys, ToTranslate, ToMergeTranslate, MergedUnions, Result.Operands, containsInUnion() || MergeUnion.containsInUnion())) {
            // оставшиеся Union'ы транслируем
            if(Type==MergeType.LEFT || Type==MergeType.FULL || Type==MergeType.EQUAL) {
                for(Union<K,Object> Union : Operands)
                    if(!MergedUnions.contains(Union))
                        Result.Operands.add(Union);

                // сделаем Map в обратную сторону
                Map<ExprUnion,Object> BackOperations = new HashMap<ExprUnion, Object>();
                // транслируем старые PropertyUnion на новые
                Map<ExprUnion, ExprUnion> Translated = new HashMap<ExprUnion, ExprUnion>();
                for(Map.Entry<V, ExprUnion<K>> MapProp : Operations.entrySet()) {
                    ExprUnion TransOperation = MapProp.getValue().translate(ToTranslate,Translated);

                    Result.Operations.put(MapProp.getKey(),TransOperation);
                    BackOperations.put(TransOperation,MapProp.getKey());
                }

                Translated = new HashMap<ExprUnion, ExprUnion>();
                for(Map.Entry<MV,ExprUnion<MK>> MapProp : MergeUnion.Operations.entrySet()) {
                    ExprUnion TransOperation = MapProp.getValue().translate(ToMergeTranslate,Translated);

                    Object PropertyObject = BackOperations.get(TransOperation);
                    if(PropertyObject==null) {
                        PropertyObject = new Object();
                        Result.Operations.put(PropertyObject,TransOperation);
                    }
                    MergeProps.put(MapProp.getKey(),PropertyObject);
                }

//              Result.checkTranslate(new HashMap(),Result.Operands);

                return Result;
            } else { // RIGHT или INNER
                // делаем LEFT JOIN
                JoinQuery<K,Object> MergedResult = new JoinQuery<K,Object>(Keys);

                Join<MK,MV> MergeInnerJoin = new MapJoin<MK,MV,K>(MergeUnion,MergedResult, MergeKeys,true);
                for(MV Property : MergeUnion.getProperties()) {
                    // нужно пересоздавать Object а то пересекутся
                    Object MergeProperty = new Object();
                    MergeProps.put(Property,MergeProperty);
                    MergedResult.add(MergeProperty,MergeInnerJoin.Exprs.get(Property));
                }

                Join<K,V> Join = new UniJoin<K,V>(this,MergedResult,false);
                MergedResult.addAll(Join.Exprs);

                return MergedResult.compile();
            }
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    boolean containsInUnion() {
        for(ExprUnion<K> Operation : Operations.values())
            if(Operation.containsInUnion()) return true;
        return false;
    }

    Source<K, V> proceedCompile() {

        boolean ContainsInUnion = containsInUnion();

        // компилируем все операнды
        for(Union<K, Object> Operand : Operands) {
            Operand.Source = Operand.Source.compile();
            ContainsInUnion = ContainsInUnion || (Operand.Source instanceof UnionQuery && ((UnionQuery)Operand.Source).containsInUnion());
        }

//        if(1==1) return;

        Collection<Union<K,Object>> TranslatedOperands = new ArrayList();
        // по всем операндам (SelectQuery) компилируем их
        // если есть UnionQuery то доливаем все операнды туда где был старый Source
        Map<PropertyUnion, ExprUnion> ToTranslate = new HashMap();
        for(Union<K, Object> Operand : Operands)
            Operand.Source.compileUnions(Operand, ToTranslate, TranslatedOperands);

        if(TranslatedOperands.size()==0)
            return getEmptySource();

//        checkTranslate(ToTranslate,Operands);

        Map<K,K> MapKeys = new HashMap();
        for(K Key : Keys)
            MapKeys.put(Key,Key);

        // для всех пар JoinQuery если Set<Join'ов> включает (compatible) Set<Join> другого, "доливаем" 1-й Join во 2-й - списки Join'ов объединяем, делаем Include, также заменяем PropExpr'ы и 1-го и 2-го JoinQuery на новые PropExprы
        // пока просто если есть 2 (compatible) Source объединяем заменяем PropExpr'ы

        // слитыве операнды
        Collection<Union<K,Object>> MergedOperands = new ArrayList();

        while(TranslatedOperands.size()>0) {
            Iterator<Union<K, Object>> itOperand = TranslatedOperands.iterator();
            // берем первый элемент, ишем с кем слить
            Union<K, Object> ToMerge = itOperand.next();
            itOperand.remove();
        
            while(itOperand.hasNext()) {
                Union<K, Object> Check = itOperand.next();
                Union<K, Object> MergedSource = ToMerge.merge(Check, MapKeys, ToTranslate, ToTranslate, ContainsInUnion);
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


    Source<K, V> mergeKeyValue(Map<K, ValueSourceExpr> MergeKeys, Collection<K> CompileKeys) {
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
    Map<V,Integer> Properties;
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
                WhereSelect.add(SourceIsNullWhere.getExpr(KeyExpr,Syntax,true));
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
        }
        for(Map.Entry<V,Integer> Property : Properties.entrySet())
            PropertySelect.put(Property.getKey(),(Property.getValue()==0?"MAX":"SUM")+"("+FromPropertySelect.get(Property.getKey())+")");

        return getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder) + (GroupBy.length()==0?"":" GROUP BY "+GroupBy);
    }

    <MK,MV> Source<K, Object> proceedMerge(Source<MK, MV> Merge, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type, boolean MergeEqual) {
        // если Merge'ся From'ы
        if(!(Merge instanceof GroupQuery)) return null;

        return proceedGroupMerge((GroupQuery)Merge,MergeKeys,MergeProps,Type);
    }

    <MB,MK extends MB,MV extends MB> Source<K, Object> proceedGroupMerge(GroupQuery<MB,MK,MV> MergeGroup, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps, MergeType Type) {
            // если Merge'ся From'ы

        if(Keys.size()!=MergeGroup.Keys.size()) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<Object, Object>> MapSet = MapBuilder.buildPairs(From.Keys, MergeGroup.From.Keys);
        if(MapSet==null) return null;

        for(Map<Object,Object> MapKeys : MapSet) {
            Map<Object,Object> FromMergeProps = new HashMap<Object, Object>();
            Source<Object, Object> MergedFrom = ((Source<Object, Object>) From).merge((Source<Object,Object>) MergeGroup.From, MapKeys, FromMergeProps, Source.FULL);
            if(MergedFrom!=null) {
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

                    GroupQuery<Object, K, Object> Result = new GroupQuery<Object, K, Object>(Keys, MergedFrom, MergedProperties);
                    return Result.compile();
                }
            }
        }

        return null;
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,V Property,int iOperator) {
        this(iKeys,iFrom,Collections.singletonMap(Property,(Integer)iOperator));
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,Map<V,Integer> iProperties) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;
    }
    
    Collection<V> getProperties() {
        return Properties.keySet();
    }

    Type getType(V Property) {
        return From.getType(Property);
    }

    Source<K, V> mergeKeyValue(Map<K, ValueSourceExpr> MergeKeys, Collection<K> CompileKeys) {
        GroupQuery<B, K, V> Result = new GroupQuery<B, K, V>(CompileKeys, From.mergePropertyValue(MergeKeys), Properties);
        return Result.compile();
    }

    Source<K, V> proceedCompile() {
        From = From.compile();
        if(From instanceof EmptySource)
            return getEmptySource();
        else
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
                UpdateQuery.add(TableJoin);

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
        LeftKeysQuery.add(new UniJoin<KeyField,PropertyField>(Change,LeftKeysQuery,true));
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

