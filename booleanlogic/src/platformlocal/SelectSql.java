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
        LinkedHashMap<String,String> NamedProperties = new LinkedHashMap<String, String>();
        for(Map.Entry<V,String> Property : PropertySelect.entrySet()) {
            NamedProperties.put(getPropertyName(Property.getKey()),Property.getValue());
            PropertyOrder.add(Property.getKey());
        }
        return getSelectString(From, KeySelect, NamedProperties, WhereSelect, KeyOrder);
    }

    String getSelectString(String From, Map<K, String> KeySelect, LinkedHashMap<String, String> PropertySelect, Collection<String> WhereSelect, List<K> KeyOrder) {

        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        if(KeySelect.size()==0)
            ExpressionString = "1 AS subkey";

        for(Map.Entry<String,String> Property : PropertySelect.entrySet())
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + Property.getKey();

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        return "SELECT " + (ExpressionString.length()==0?"1":ExpressionString) + " FROM " + From + (WhereString.length()==0?"":" WHERE " + WhereString);
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

    <MK,MV> Source<K, Object> merge(Source<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps) {

        if(this==Merge) {
            for(Map.Entry<K,MK> MapKey : MergeKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return null;

            for(MV Field : Merge.getProperties())
                MergeProps.put(Field,Field);

            return (Source<K, Object>)((Source<K,?>)this);
        }

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
    void compileJoins(Join<K, V> Join, ExprTranslator Translated, Collection<Join> TranslatedJoins) {
        Join.translate(Translated, Translated, TranslatedJoins);
    }

    // возвращает источник с "проставленными" ключами со значениями
    JoinQuery<K,V> getJoinQuery() {
        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Join<K,V> SourceJoin = new UniJoin<K,V>(this,Query);
        Query.add(SourceJoin.InJoin);
        Query.addAll(SourceJoin.Exprs);
        return Query;
    }

    <T> JoinQuery<T,V> getMapQuery(Map<T,K> MapKeys) {
        JoinQuery<T,V> Query = new JoinQuery<T,V>(MapKeys.keySet());
        Join<K,V> SourceJoin = new MapJoin<K,V,T>(this,Query,MapKeys);
        Query.add(SourceJoin.InJoin);
        Query.addAll(SourceJoin.Exprs);
        return Query;
    }

    Source<K,V> getEmptySource() {

        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        for(V Property : getProperties())
            Query.add(Property, new ValueExpr(null,getType(Property)));
        Query.add(new OrWhere());
        return Query;
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
        Query.Where = Where;
        Query.Properties.putAll(Properties);
        Query.Joins.addAll(Joins);
        for(SourceExpr Order : Orders.keySet())
            Query.add(Order,Order);

        Map<K,String> KeySelect = new HashMap<K, String>();
        Map<Object,String> PropertySelect = new HashMap<Object, String>();
        Collection<String> WhereSelect = new ArrayList<String>();
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
        Cloned.Where = Where;
        Cloned.Properties.putAll(Properties);
        Cloned.Joins.addAll(Joins);
        Cloned.Top = Top;
        Cloned.Up = Up;
        Cloned.Orders.putAll(Orders);

        return Cloned;
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
            Map<KeyField,String> KeySelect = new HashMap<KeyField, String>();
            Map<PropertyField,String> PropertySelect = new HashMap<PropertyField, String>();
            Collection<String> WhereSelect = new ArrayList<String>();
            String FromSelect = Change.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

            for(KeyField Key : Table.Keys)
                WhereSelect.add(Table.getSource(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            
            List<KeyField> KeyOrder = new ArrayList<KeyField>();
            List<PropertyField> PropertyOrder = new ArrayList<PropertyField>();
            String SelectString = Change.getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder);

            String SetString = "";
            for(KeyField Field : KeyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;
            for(PropertyField Field : PropertyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;

            return "UPDATE " + Table.getSource(Syntax) + " SET ("+SetString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
        } else {
            Map<KeyField,String> KeySelect = new HashMap<KeyField, String>();
            Map<PropertyField,String> PropertySelect = new HashMap<PropertyField, String>();
            Collection<String> WhereSelect = new ArrayList<String>();

            String WhereString = "";
            String FromSelect;

            if(UpdateModel==1) {
                // SQL-серверная модель когда она подхватывает первый Join и старую таблицу уже не вилит
                // построим JoinQuery куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы)
                JoinQuery<KeyField, PropertyField> UpdateQuery = new JoinQuery<KeyField, PropertyField>(Table.Keys);
                Join<KeyField, PropertyField> TableJoin = new UniJoin<KeyField, PropertyField>(Table, UpdateQuery);
                TableJoin.NoAlias = true;
                UpdateQuery.add(TableJoin.InJoin);

                Join<KeyField, PropertyField> ChangeJoin = new UniJoin<KeyField, PropertyField>(Change, UpdateQuery);
                UpdateQuery.add(ChangeJoin.InJoin);
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
        LeftKeysQuery.add(new UniJoin<KeyField,PropertyField>(Change,LeftKeysQuery).InJoin);
        // исключим ключи которые есть
        LeftKeysQuery.add(new NotWhere((new UniJoin<KeyField,PropertyField>(Table,LeftKeysQuery)).InJoin));

        return (new ModifyQuery(Table,LeftKeysQuery)).getInsertSelect(Syntax);
    }

    String getInsertSelect(SQLSyntax Syntax) {

        List<KeyField> KeyOrder = new ArrayList<KeyField>();
        List<PropertyField> PropertyOrder = new ArrayList<PropertyField>();
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

