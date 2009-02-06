/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.*;
import java.sql.SQLException;

// абстрактный класс источников
abstract class Source<K,V> {

    Collection<K> Keys;

    Source(Collection<? extends K> iKeys) {
        Keys=(Collection<K>)iKeys;
    }
    Source() {Keys=new ArrayList();}

    abstract Collection<V> getProperties();
    abstract Type getType(V Property);

    // вспомогательные методы
    static String stringExpr(LinkedHashMap<String,String> KeySelect,LinkedHashMap<String,String> PropertySelect) {
        String ExpressionString = "";
        for(Map.Entry<String,String> Key : KeySelect.entrySet())
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + Key.getKey();
        if(KeySelect.size()==0)
            ExpressionString = "1 AS subkey";
        for(Map.Entry<String,String> Property : PropertySelect.entrySet())
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + Property.getKey();
        return ExpressionString;
    }

    static <T> LinkedHashMap<String,String> mapNames(Map<T,String> Exprs,Map<T,String> Names,List<T> Order) {
        LinkedHashMap<String,String> Result = new LinkedHashMap<String, String>();
        for(Map.Entry<T,String> Name : Names.entrySet()) {
            Result.put(Name.getValue(),Exprs.get(Name.getKey()));
            Order.add(Name.getKey());
        }
        return Result;
    }

    static String stringWhere(Collection<String> WhereSelect) {
        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;
        return WhereString;
    }

    abstract Map<ValueExpr,ValueExpr> getValues();

    // записывается в Join'ы
    abstract void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins);

    <EK, EV> boolean equals(Source<EK, EV> Source, Map<K, EK> MapKeys, Map<V, EV> MapProperties, Map<ValueExpr, ValueExpr> MapValues) {
        if(this==Source) {
            for(Map.Entry<K,EK> MapKey : MapKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return false;

            for(V Field : getProperties())
                MapProperties.put(Field, (EV) Field);

            return true;
        }
        return false;
    }

    boolean Hashed = false;
    int Hash;
    int hash() {
        if(!Hashed) {
            Hash = getHash();
            Hashed = true;
        }
        return Hash;
    }
    int getHash() {
        return hashCode();
    }

    abstract int hashProperty(V Property);

    int getComplexity() {
        Set<JoinQuery> Queries = new HashSet<JoinQuery>();
        fillJoinQueries(Queries);
        return Queries.size();
    }
    abstract void fillJoinQueries(Set<JoinQuery> Queries);
}

abstract class DataSource<K,V> extends Source<K,V> {

    DataSource(Collection<? extends K> iKeys) {
        super(iKeys);
    }
    DataSource() {
    }

    abstract String getSource(SQLSyntax Syntax, Map<ValueExpr, String> Params);

    abstract String getKeyName(K Key);
    abstract String getPropertyName(V Property);

    // получает строку по которой можно определить входит ли ряд в запрос Select
    String getInSourceName() {
        return (Keys.size()>0?getKeyName(Keys.iterator().next()):"subkey");
    }

//    abstract <MK,MV> DataSource<K, Object> merge(DataSource<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps);
    <MK, MV> DataSource<K, Object> merge(DataSource<MK, MV> Merge, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps) {
        if(this==Merge) {
            for(Map.Entry<K,MK> MapKey : MergeKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return null;

            for(MV Field : Merge.getProperties())
                MergeProps.put(Field,Field);

            return (DataSource<K, Object>)((DataSource<K,?>)this);
        }
        return null;
    }

    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        Join.translate(Translated, TranslatedJoins, this);
    }

    abstract DataSource<K,V> translateValues(Map<ValueExpr, ValueExpr> Values);
}

class TypedObject {
    Object Value;
    Type type;

    TypedObject(Object iValue, Type iType) {
        Value = iValue;
        type = iType;
    }

    static String getString(Object Value, Type Type, SQLSyntax Syntax) {
        if(Value==null)
            return Type.NULL;
        else
            return Type.getString(Value,Syntax);
    }

    String getString(SQLSyntax Syntax) {
        return getString(Value, type,Syntax);
    }

    public String toString() {
        if(Value==null)
            return Type.NULL;
        else
            return Value.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypedObject that = (TypedObject) o;

        if (Value != null ? !Value.equals(that.Value) : that.Value != null) return false;

        return true;
    }

    public int hashCode() {
        return (Value != null ? Value.hashCode() : 0);
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

class Table extends DataSource<KeyField,PropertyField> {
    String Name;
    Collection<PropertyField> Properties = new ArrayList();

    Table(String iName) {Name=iName;}

    String getSource(SQLSyntax Syntax, Map<ValueExpr, String> Params) {
        return getName(Syntax);
    }
    
    String getName(SQLSyntax Syntax) {
        return Name;
    }

    void fillJoinQueries(Set<JoinQuery> Queries) {
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

    Map<ValueExpr,ValueExpr> getValues() {
        return new HashMap<ValueExpr,ValueExpr>();
    }

    public void outSelect(DataSession Session) throws SQLException {
        JoinQuery<KeyField,PropertyField> OutQuery = new JoinQuery<KeyField,PropertyField>(Keys);
        Join<KeyField,PropertyField> OutJoin = new Join<KeyField,PropertyField>(this,OutQuery);
        OutQuery.Properties.putAll(OutJoin.Exprs);
        OutQuery.Where = OutQuery.Where.and(OutJoin.InJoin);
        OutQuery.outSelect(Session);
    }

    int hashProperty(PropertyField Property) {
        return Property.hashCode();
    }

    DataSource<KeyField, PropertyField> translateValues(Map<ValueExpr, ValueExpr> Values) {
        return this;
    }
}

// временная таблица на момент сессии
class SessionTable extends Table {

    SessionTable(String iName) {
        super(iName);
    }

    String getName(SQLSyntax Syntax) {
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

}

class ModifyQuery {
    Table Table;
    JoinQuery<KeyField,PropertyField> Change;

    ModifyQuery(Table iTable,JoinQuery<KeyField,PropertyField> iChange) {
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
            CompiledQuery<KeyField, PropertyField> ChangeCompile = Change.compile(Syntax);
            String FromSelect = ChangeCompile.fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

            for(KeyField Key : Table.Keys)
                WhereSelect.add(Table.getName(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            
            List<KeyField> KeyOrder = new ArrayList<KeyField>();
            List<PropertyField> PropertyOrder = new ArrayList<PropertyField>();
            String SelectString = Syntax.getSelect(FromSelect,Source.stringExpr(
                    Source.mapNames(KeySelect,ChangeCompile.KeyNames,KeyOrder),
                    Source.mapNames(PropertySelect,ChangeCompile.PropertyNames,PropertyOrder)),
                    Source.stringWhere(WhereSelect),"","","");

            String SetString = "";
            for(KeyField Field : KeyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;
            for(PropertyField Field : PropertyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;

            return "UPDATE " + Table.getName(Syntax) + " SET ("+SetString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
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
                Join<KeyField, PropertyField> TableJoin = new Join<KeyField, PropertyField>(Table, UpdateQuery);
                TableJoin.NoAlias = true;
                UpdateQuery.and(TableJoin.InJoin);

                Join<KeyField, PropertyField> ChangeJoin = new Join<KeyField, PropertyField>(Change, UpdateQuery);
                UpdateQuery.and(ChangeJoin.InJoin);
                for(PropertyField ChangeField : Change.Properties.keySet())
                    UpdateQuery.Properties.put(ChangeField, ChangeJoin.Exprs.get(ChangeField));
                FromSelect = UpdateQuery.compile(Syntax).fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);
            } else {
                FromSelect = Change.compile(Syntax).fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

                for(KeyField Key : Table.Keys)
                    WhereSelect.add(Table.getName(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            }

            for(String Where : WhereSelect)
                WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

            String SetString = "";
            for(Map.Entry<PropertyField,String> SetProperty : PropertySelect.entrySet())
                SetString = (SetString.length()==0?"":SetString+",") + SetProperty.getKey().Name + "=" + SetProperty.getValue();

            return "UPDATE " + Syntax.getUpdate(Table.getName(Syntax)," SET "+SetString,FromSelect,(WhereString.length()==0?"":" WHERE "+WhereString));
        }
    }

    String getInsertLeftKeys(SQLSyntax Syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField,PropertyField> LeftKeysQuery = new JoinQuery<KeyField,PropertyField>(Table.Keys);
        // при Join'им ModifyQuery
        LeftKeysQuery.and(new Join<KeyField,PropertyField>(Change,LeftKeysQuery).InJoin);
        // исключим ключи которые есть
        LeftKeysQuery.and((new Join<KeyField,PropertyField>(Table,LeftKeysQuery)).InJoin.not());

        return (new ModifyQuery(Table,LeftKeysQuery)).getInsertSelect(Syntax);
    }

    String getInsertSelect(SQLSyntax Syntax) {

        CompiledQuery<KeyField, PropertyField> ChangeCompile = Change.compile(Syntax);

        String InsertString = "";
        for(KeyField KeyField : ChangeCompile.KeyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + KeyField.Name;
        for(PropertyField PropertyField : ChangeCompile.PropertyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + PropertyField.Name;

        return "INSERT INTO " + Table.getName(Syntax) + " (" + InsertString + ") " + ChangeCompile.getSelect(Syntax);
    }

    void outSelect(DataSession Session) throws SQLException {
        System.out.println("Table");
        Table.outSelect(Session);
        System.out.println("Source");
        Change.outSelect(Session);
    }
}

