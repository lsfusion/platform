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

    abstract String getKeyName(K Key);
    abstract String getPropertyName(V Property);

    String getSelectString(String From, Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, List<K> KeyOrder, List<V> PropertyOrder) {
        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
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
    LinkedHashMap<Map<K,Integer>,Map<V,Object>> executeSelect(DataAdapter Adapter) throws SQLException {

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> ExecResult = new LinkedHashMap();

        Statement Statement = Adapter.Connection.createStatement();

//        System.out.println(getSelect(new ArrayList(),new ArrayList(), Adapter));
        try {
            ResultSet Result = Statement.executeQuery(getSelect(new ArrayList(),new ArrayList(), Adapter));
            try {
                while(Result.next()) {
                    Map<K,Integer> RowKeys = new HashMap();
                    for(K Key : Keys)
                        RowKeys.put(Key,(Integer)Result.getObject(getKeyName(Key)));
                    Map<V,Object> RowProperties = new HashMap();
                    for(V Property : getProperties())
                        RowProperties.put(Property,Result.getObject(getPropertyName(Property)));

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

    void outSelect(DataAdapter Adapter) throws SQLException {
        // выведем на экран
        Collection<String> ResultFields = new ArrayList();
//        System.out.println(Select.GetSelect(ResultFields));

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> Result = executeSelect(Adapter);

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
    Table createView(DataAdapter Adapter,String ViewName,Map<K,KeyField> MapKeys,Map<V,PropertyField> MapProperties) throws SQLException {

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

        Adapter.Execute("CREATE VIEW " + ViewName + " AS " + getSelect(new ArrayList(),new ArrayList(), Adapter));

        return View;
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

    // пока забьем на эту реализацию
    String getKeyName(K Key) {
        return null;
    }

    String getPropertyName(V Property) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    void outSelect(DataAdapter Adapter) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

// таблицы\Views

class Field {
    String Name;
    String Type;

    Field(String iName,String iType) {Name=iName;Type=iType;}

    String GetDeclare() {
        return Name + " " + Type;
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

abstract class Query<K,V> extends Source<K,V> {

    int KeyCount = 0;

    Query(Collection<? extends K> iKeys) {
        super(iKeys);
        for(K Key : Keys)
            MapKeys.put(Key,new KeyExpr<K>(Key,"key"+(KeyCount++)));
    }

    Query() {
        super();
    }

    Map<K,SourceExpr> MapKeys = new HashMap();

    KeyExpr<K> addKey(K Key) {
        Keys.add(Key);
        KeyExpr<K> KeyExpr = new KeyExpr<K>(Key,"key"+(KeyCount++));
        MapKeys.put(Key,KeyExpr);
        return KeyExpr;
    }

    String getSource(SQLSyntax Syntax) {
        return "(" + getSelect(new ArrayList(),new ArrayList(), Syntax) + ")";
    }

    // сделаем кэш Value
    Map<V,String> ValueNames = new HashMap();
    int ValueCount = 0;

    String getKeyName(K Key) {
        return ((KeyExpr<K>)MapKeys.get(Key)).KeyName;
    }

    String getPropertyName(V Value) {
        String ValueName = ValueNames.get(Value);
        if(ValueName==null) {
            ValueName = "value"+(ValueCount++);
            ValueNames.put(Value,ValueName);
        }
        return ValueName;
    }
}

class Join<J,U> {
    Source<J,U> Source;
    Map<J,SourceExpr> Joins;

    Join(Source<J,U> iSource) {
        Source = iSource;
        Joins = new HashMap();
    }

    Join(Source<J,U> iSource,Map<J,SourceExpr> iJoins) {
        Source = iSource;
        Joins = iJoins;
    }

    void fillJoins(LinkedHashSet<Join> JoinSet,Set<Where> Wheres) {
        for(SourceExpr Join : Joins.values())
            Join.fillJoins(JoinSet,Wheres);

        JoinSet.add(this);
    }

    String getFrom(Map<Join, String> JoinAlias, Collection<String> WhereSelect, SQLSyntax Syntax) {
        String JoinString = "";
        String Alias = "t"+(JoinAlias.size()+1);
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

        return Source.getSource(Syntax) + " " + Alias + (WhereSelect==null?" ON "+(JoinString.length()==0?"1=1":JoinString):"");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Join join = (Join) o;

        if (!Joins.equals(join.Joins)) return false;
        if (!Source.equals(join.Source)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + Joins.hashCode();
        return result;
    }
}

class MapJoin<J,U,K> extends Join<J,U> {

    // конструктор когда надо просто ключи протранслировать
    MapJoin(Source<J,U> iSource,Map<J,K> iJoins,Query<K,?> MapSource) {
        super(iSource);

        for(J Implement : Source.Keys)
            Joins.put(Implement,MapSource.MapKeys.get(iJoins.get(Implement)));
    }

    MapJoin(Source<J,U> iSource,Query<K,?> MapSource,Map<K,J> iJoins) {
         super(iSource);

         for(K Implement : MapSource.Keys)
             Joins.put(iJoins.get(Implement),MapSource.MapKeys.get(Implement));
     }

}

class UniJoin<K,U> extends Join<K,U> {
    UniJoin(Source<K,U> iSource,Query<K,?> MapSource) {
        super(iSource);

        for(K Key : Source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }
}

// абстрактный класс выражений
abstract class SourceExpr {

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {}

    abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return KeySource + "=" + getSource(JoinAlias, Syntax);
    }

    abstract String getDBType();
}

class KeyExpr<K> extends SourceExpr {
    K Key;
    String KeyName;

    KeyExpr(K iKey,String iKeyName) {Key=iKey;KeyName=iKeyName;}

    String Source;
    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source;
    }

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(Source!=null) return super.getJoin(KeySource,JoinAlias, Syntax);

        Source = KeySource;
        return null;
    }

    String getDBType() {
        return "integer";
    }
}

class JoinExpr<J,U> extends SourceExpr {
    U Property;
    Join<J,U> From;
    boolean NotNull;

    JoinExpr(Join<J,U> iFrom,U iProperty,boolean iNotNull) {
        From = iFrom;
        Property = iProperty;
        if(Property==null)
            From=From;
        NotNull = iNotNull;
    }

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
        if(NotNull)
            Wheres.add(new JoinWhere(From));
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return From.Source.getPropertyString(Property,JoinAlias.get(From));
    }

    String getDBType() {
        return From.Source.getDBType(Property);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoinExpr joinExpr = (JoinExpr) o;

        if (NotNull != joinExpr.NotNull) return false;
        if (!From.equals(joinExpr.From)) return false;
        if (!Property.equals(joinExpr.Property)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Property.hashCode();
        result = 31 * result + From.hashCode();
        result = 31 * result + (NotNull ? 1 : 0);
        return result;
    }
}

// формулы
class ValueSourceExpr extends SourceExpr {

    Object Value;
    ValueSourceExpr(Object iValue) {
        Value=iValue;
        if(Value==null)
            Value = Value;
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
//        if(Value==null) return "NULL";

        if(Value instanceof String)
            return "'" + Value + "'";
        else
            return Value.toString();
    }

    String getDBType() {
        if(Value instanceof Integer)
            return "integer";
        else
            return "char(50)";
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
}

class StaticNullSourceExpr extends SourceExpr {

    String DBType;
    StaticNullSourceExpr(String iDBType) {
        DBType = iDBType;
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    String getDBType() {
        return DBType;
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

class UnionSourceExpr extends SourceExpr {

    UnionSourceExpr(int iOperator) {Operator=iOperator;}

    // 0 - MAX
    // 1 - +
    // 2 - ISNULL
    int Operator;
    LinkedHashMap<SourceExpr,Integer> Operands = new LinkedHashMap();

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        LinkedHashMap<String,Integer> StringOperands = new LinkedHashMap<String,Integer>();
        for(Map.Entry<SourceExpr,Integer> Operand : Operands.entrySet())
            StringOperands.put(Operand.getKey().getSource(JoinAlias, Syntax),Operand.getValue());

        return UnionQuery.getExpr(StringOperands,Operator,false, Syntax);
    }

    String getDBType() {
        return Operands.keySet().iterator().next().getDBType();
    }

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        for(SourceExpr Operand : Operands.keySet())
            Operand.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnionSourceExpr that = (UnionSourceExpr) o;

        if (Operator != that.Operator) return false;
        if (Operands != null ? !Operands.equals(that.Operands) : that.Operands != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Operator;
        result = 31 * result + (Operands != null ? Operands.hashCode() : 0);
        return result;
    }
}

class NullEmptySourceExpr extends SourceExpr {

    NullEmptySourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins, Wheres);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(Expr.getSource(JoinAlias, Syntax),(Expr.getDBType().equals("integer")?"0":"''"), false);
    }

    String getDBType() {
        return Expr.getDBType();
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

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins, Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    String getDBType() {
        return Expr.getDBType();
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

class NullValueSourceExpr extends SourceExpr {

    NullValueSourceExpr(Collection<SourceExpr> iExprs,SourceExpr iTrueExpr,SourceExpr iFalseExpr) {Exprs=iExprs; TrueExpr=iTrueExpr; FalseExpr=iFalseExpr;};
    
    Collection<SourceExpr> Exprs;
    SourceExpr TrueExpr;
    SourceExpr FalseExpr;

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        for(SourceExpr Expr : Exprs)
            Expr.fillJoins(Joins,Wheres);

        TrueExpr.fillJoins(Joins,Wheres);
        FalseExpr.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String Filter = "";
        for(SourceExpr Expr : Exprs)
            Filter = (Filter.length()==0?"":Filter+" OR ") + Expr.getSource(JoinAlias, Syntax) + " IS NULL ";

        String TrueSource = TrueExpr.getSource(JoinAlias, Syntax);
        String FalseSource = FalseExpr.getSource(JoinAlias, Syntax);

        if(TrueSource.equals("NULL") && FalseSource.equals("NULL"))
            return "NULL";
        else
            return "(CASE WHEN " + Filter + " THEN " + TrueSource + " ELSE " + FalseSource + " END)";
    }

    String getDBType() {
        return TrueExpr.getDBType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullValueSourceExpr that = (NullValueSourceExpr) o;

        if (!Exprs.equals(that.Exprs)) return false;
        if (!FalseExpr.equals(that.FalseExpr)) return false;
        if (!TrueExpr.equals(that.TrueExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Exprs.hashCode();
        result = 31 * result + TrueExpr.hashCode();
        result = 31 * result + FalseExpr.hashCode();
        return result;
    }
}

class IsNullSourceExpr extends SourceExpr {

    IsNullSourceExpr(SourceExpr iPrimaryExpr,SourceExpr iSecondaryExpr) {PrimaryExpr=iPrimaryExpr;SecondaryExpr=iSecondaryExpr;};
    
    SourceExpr PrimaryExpr;
    SourceExpr SecondaryExpr;

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        PrimaryExpr.fillJoins(Joins,Wheres);
        SecondaryExpr.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(PrimaryExpr.getSource(JoinAlias, Syntax),SecondaryExpr.getSource(JoinAlias, Syntax), false);
    }

    String getDBType() {
        return PrimaryExpr.getDBType();
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
    
    String Formula;

    Map<String,SourceExpr> Params;

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres);
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

// SourceExpr возвращаюший 1 если FormulaExpr true и Null в противном случае
class FormulaWhereSourceExpr extends SourceExpr {

    FormulaSourceExpr FormulaExpr;
    boolean NotNull;

    FormulaWhereSourceExpr(FormulaSourceExpr iFormulaExpr,boolean iNotNull) {
        FormulaExpr = iFormulaExpr;
        NotNull = iNotNull;
    }

    void fillDependExprs(LinkedHashSet<SourceExpr> Exprs) {

    }

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        FormulaExpr.fillJoins(Joins,Wheres);
        if(NotNull)
            Wheres.add(new SourceExprWhere(FormulaExpr,false));
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        // если NotNull нефиг 2 раза проверять
        if(NotNull)
            return "1";
        else
            return "CASE WHEN " + FormulaExpr.getSource(JoinAlias, Syntax) + " THEN 1 ELSE NULL END";
    }

    String getDBType() {
        return FormulaExpr.getDBType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaWhereSourceExpr that = (FormulaWhereSourceExpr) o;

        if (NotNull != that.NotNull) return false;
        if (!FormulaExpr.equals(that.FormulaExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = FormulaExpr.hashCode();
        result = 31 * result + (NotNull ? 1 : 0);
        return result;
    }
}


// Wheres
abstract class Where {

    abstract void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres);

    abstract void fillWheres(Set<Join> InnerJoins,Collection<SourceWhere> Wheres);
}

class JoinWhere extends Where {
    Join From;

    JoinWhere(Join iFrom) {From=iFrom;}

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
    }

    // по сути разделяет отборы на Inner и прямые
    void fillWheres(Set<Join> InnerJoins, Collection<SourceWhere> Wheres) {
        InnerJoins.add(From);
    }
}

abstract class SourceWhere extends Where {

    void fillWheres(Set<Join> InnerJoins, Collection<SourceWhere> Wheres) {
        Wheres.add(this);
    }

    abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
}

class ExcludeJoinWhere extends SourceWhere {
    Join From;

    ExcludeJoinWhere(Join iFrom) {From=iFrom;}

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return From.Source.getKeyString(From.Source.Keys.iterator().next(),JoinAlias.get(From)) + " IS NULL";
    }

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
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

    void fillJoins(LinkedHashSet<Join> Joins,Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
        if(Value instanceof SourceExpr)
            ((SourceExpr)Value).fillJoins(Joins,Wheres);
    }
}

class SourceIsNullWhere extends SourceWhere {
    SourceIsNullWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return (Not?"NOT ":"") + Source.getSource(JoinAlias, Syntax) + " IS NULL";
    }

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
    }
}

class FieldOPWhere extends SourceWhere {
    FieldOPWhere(SourceWhere iOp1,SourceWhere iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    SourceWhere Op1, Op2;
    boolean And;

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Op1.fillJoins(Joins,Wheres);
        Op2.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "(" + Op1.getSource(JoinAlias, Syntax) + " " + (And?"AND":"OR") + " " + Op2.getSource(JoinAlias, Syntax) + ")";
    }
}

class SourceExprWhere extends SourceWhere {
    SourceExprWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return (Not?"NOT ":"") + Source.getSource(JoinAlias, Syntax);
    }

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
    }
}

class FieldSetValueWhere extends SourceWhere {

    FieldSetValueWhere(SourceExpr iExpr,Collection<Integer> iSetValues) {Expr=iExpr; SetValues=iSetValues;};
    SourceExpr Expr;
    Collection<Integer> SetValues;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String ListString = "";
        for(Integer Value : SetValues)
            ListString = (ListString.length()==0?"":ListString+',') + Value;

        return Expr.getSource(JoinAlias, Syntax) + " IN (" + ListString + ")";
    }

    void fillJoins(LinkedHashSet<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins,Wheres);
    }
}

abstract class SelectQuery<K,V> extends Query<K,V> {

    SelectQuery() {super();}
    SelectQuery(Collection<? extends K> iKeys) {super(iKeys);}
}

// запрос Join
class JoinQuery<K,V> extends SelectQuery<K,V> {
    Map<V,SourceExpr> Properties = new HashMap();
    Collection<Where> Wheres = new ArrayList();

    JoinQuery() {super();}
    JoinQuery(Collection<? extends K> iKeys) {super(iKeys);}

    Collection<V> getProperties() {
        return Properties.keySet();
    }

    String getDBType(V Property) {
        return Properties.get(Property).getDBType();
    }

    void putDumbJoin(Map<K,Integer> KeyValues) {

        Join<K,Object> DumbJoin = new Join<K,Object>(new DumbSource<K,Object>(KeyValues,new HashMap()));
        for(K Object : KeyValues.keySet())
            DumbJoin.Joins.put(Object,MapKeys.get(Object));
        Wheres.add(new JoinWhere(DumbJoin));
    }

    String fillOrderSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, Map<SourceExpr, String> OrderSelect, SQLSyntax Syntax) {
        LinkedHashSet<Join> Joins = new LinkedHashSet();
        Set<Where> QueryWheres = new HashSet();
        for(SourceExpr PropertyExpr : Properties.values())
            PropertyExpr.fillJoins(Joins,QueryWheres);
        for(SourceExpr PropertyExpr : OrderSelect.keySet())
            PropertyExpr.fillJoins(Joins,QueryWheres);
        for(Where Where : Wheres) {
            Where.fillJoins(Joins,QueryWheres);
            QueryWheres.add(Where);
        }

        Set<Join> InnerJoins = new HashSet();
        Collection<SourceWhere> JoinWheres = new ArrayList();
        for(Where Where : QueryWheres)
            Where.fillWheres(InnerJoins,JoinWheres);

        Map<Join,String> JoinAlias = new HashMap();
        String From = "";
        // теперь определим KeyExpr'ы заодно строя Join'ы
        for(Join Join : Joins)
            if(InnerJoins.contains(Join))
                From = (From.length()==0?"":From + " JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null), Syntax);

        // проверим что все KeyExpr'ы заполнились
        for(SourceExpr Key : MapKeys.values())
            if(((KeyExpr<K>)Key).Source==null)
                throw new RuntimeException();
        if(From.length()==0)
            From = "dumb";

        for(Join Join : Joins)
            if(!InnerJoins.contains(Join))
                From = From + " LEFT JOIN " + Join.getFrom(JoinAlias,null, Syntax);

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

        // порядки
        for(SourceExpr Order : OrderSelect.keySet())
            OrderSelect.put(Order,Order.getSource(JoinAlias, Syntax));

        // условия
        for(SourceWhere Where : JoinWheres)
            WhereSelect.add(Where.getSource(JoinAlias, Syntax));

        // Source'ы в KeyExpr надо сбросить
        for(SourceExpr Key : MapKeys.values())
            ((KeyExpr<K>)Key).Source = null;

        return From;
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        return fillOrderSelect(KeySelect,PropertySelect,WhereSelect,new HashMap(), Syntax);
    }

}

class OrderedJoinQuery<K,V> extends JoinQuery<K,V> {

    int Top;

    boolean Up;
    LinkedHashMap<SourceExpr,Boolean> Orders = new LinkedHashMap();

    // кривоватая перегрузка но плодить параметры еще хуже
    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {
        Map<K,String> KeySelect = new HashMap();
        Map<V,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        Map<SourceExpr,String> OrderSelect = new HashMap();
        for(SourceExpr Order : Orders.keySet())
            OrderSelect.put(Order,null);
        String From = fillOrderSelect(KeySelect,PropertySelect,WhereSelect,OrderSelect, Syntax);

        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        for(Map.Entry<V,String> Property : PropertySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + getPropertyName(Property.getKey());
            PropertyOrder.add(Property.getKey());
        }

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        String OrderString = "";
        for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet())
            OrderString = (OrderString.length()==0?"":OrderString+",") + OrderSelect.get(Order.getKey())+(Up==Order.getValue()?" ASC":" DESC");

        return "SELECT " + Syntax.getTop(Top, ExpressionString + " FROM " + From + (WhereString.length()==0?"":" WHERE " + WhereString) +
                (OrderString.length()==0?"":" ORDER BY "+OrderString));
    }

    OrderedJoinQuery(Collection<? extends K> iKeys) {super(iKeys);}
}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends SelectQuery<K,V> {
    LinkedHashMap<Source<K,V>,Integer> Unions = new LinkedHashMap();

    // как в List 0 - MAX, 1 - SUM, 2 - NVL, плюс 3 - если есть в Source
    int Operator;

    UnionQuery(Collection<? extends K> iKeys,int iOperator) {super(iKeys); Operator=iOperator;}

    // Safe - кдючи чисто никаких чисел типа 1 с которыми проблемы
    static String getExpr(LinkedHashMap<String, Integer> Operands, int Operator, boolean Safe, SQLSyntax Syntax) {
        String Result = "";
        for(Map.Entry<String,Integer> Operand : Operands.entrySet()) {
            Integer Coeff = Operand.getValue();
            if(Coeff==null) Coeff = 1;
            String OperandString = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString()));
            if(Operator==1)
                OperandString += Syntax.isNULL(Operand.getKey(),"0", false);
            else
                OperandString += Operand.getKey();

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
                        Result = Result+(Coeff>=0?"+":"") + OperandString;
                        break;
                    case 0:
                        if(!OperandString.equals("NULL"))
                            Result = "CASE WHEN "+OperandString+" IS NULL OR "+Result+">"+OperandString+" THEN "+Result+" ELSE "+OperandString+" END";
                        break;
                }
            }
        }

        if(Operands.size()<=1)
            return Result;
        else
        if((Safe || Syntax.isNullSafe()) && Operator==2)
            return "COALESCE(" + Result + ")";    
        else
            return "("+Result+")";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        // ключи надо по NVL(2) делать, выражения по оператору
        Map<K,LinkedHashMap<String,Integer>> UnionKeys = new HashMap();
        // для Operator <=2
        Map<V,LinkedHashMap<String,Integer>> UnionValues = null;

        if(Operator<=2)
            UnionValues = new HashMap();

        int AliasCount = 0;
        String From = "";
        for(Map.Entry<Source<K,V>,Integer> Union : Unions.entrySet()) {
            Source<K,V> Source = Union.getKey();
            Integer Coeff = Union.getValue();

            String Alias = "f"+(AliasCount++);

            String FromSource = Source.getSource(Syntax) + " " + Alias;

            if(From.length()==0) {
                // первый Union
                for(K Key : Keys) {
                    LinkedHashMap<String,Integer> JoinKeySource = new LinkedHashMap<String,Integer>();
                    JoinKeySource.put(Source.getKeyString(Key,Alias),1);
                    UnionKeys.put(Key,JoinKeySource);
                }
            } else {
                String JoinOn = "";
                for(K Key : Keys) {
                    String KeySource = Source.getKeyString(Key,Alias);
                    LinkedHashMap<String,Integer> JoinKeySource = UnionKeys.get(Key);
                    JoinOn = (JoinOn.length()==0?"":JoinOn+" AND ") + KeySource + "=" + getExpr(JoinKeySource,2,true, Syntax);
                    JoinKeySource.put(KeySource,1);
                }

                FromSource = " FULL JOIN " + FromSource + " ON " + JoinOn;
            }

            for(V Property : Source.getProperties()) {
                if(Operator<=2) {
                    LinkedHashMap<String,Integer> PropertySource = UnionValues.get(Property);
                    if(PropertySource==null) {
                        PropertySource = new LinkedHashMap<String,Integer>();
                        UnionValues.put(Property,PropertySource);
                    }

                    PropertySource.put(Source.getPropertyString(Property,Alias),Coeff);
                } else {
                    String SourceExpr = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString())) + Source.getPropertyString(Property,Alias);
                    String PrevExpr = PropertySelect.get(Property);
                    if(PrevExpr!=null)
                        SourceExpr = "(CASE WHEN " + Source.getKeyString(Keys.iterator().next(),Alias) + " IS NULL "
                            + " THEN " + PrevExpr + " ELSE " + SourceExpr + " END)";
                    PropertySelect.put(Property,SourceExpr);
                }
            }

            From += FromSource;
        }

        String Expressions = "";
        // ключи
        for(K Key : Keys)
            KeySelect.put(Key,getExpr(UnionKeys.get(Key),2,true, Syntax));
        // свойства
        if(Operator<=2)
            for(Map.Entry<V,LinkedHashMap<String,Integer>> Property : UnionValues.entrySet())
                PropertySelect.put(Property.getKey(),getExpr(Property.getValue(),Operator,false, Syntax));

        return From;
    }

    JoinQuery<K,V> newJoinQuery(Integer Coeff) {

        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Unions.put(Query,Coeff);

        return Query;
    }

    Collection<V> getProperties() {
        Set<V> ValueSet = new HashSet();

        for(Source<K,V> Union : Unions.keySet())
            ValueSet.addAll(Union.getProperties());

        return ValueSet;
    }

    String getDBType(V Property) {
        for(Source<K,V> Union : Unions.keySet())
            if(Union.getProperties().contains(Property))
                return Union.getDBType(Property);

        return null;
    }
}

// с GroupQuery пока неясно
class GroupQuery<B,K extends B,V extends B> extends Query<K,V> {
    Source<?,B> From; // вообще должен быть или K или V
    Collection<V> Properties = new ArrayList();
    int Operator;

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {
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
            WhereSelect.add("NOT "+KeyExpr+" IS NULL");
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
        }
        for(V Property : Properties)
            PropertySelect.put(Property,(Operator==0?"MAX":"SUM")+"("+FromPropertySelect.get(Property)+")");

        return getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder) + (GroupBy.length()==0?"":" GROUP BY "+GroupBy);
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,V Property,int iOperator) {
        super(iKeys);
        From = iFrom;
        Properties.add(Property);
        Operator = iOperator;
    }

    Collection<V> getProperties() {
        return Properties;
    }

    String getDBType(V Property) {
        return From.getDBType(Property);
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

        Map<KeyField,String> KeySelect = new HashMap();
        Map<PropertyField,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String FromSelect = Change.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

        for(KeyField Key : Table.Keys)
            WhereSelect.add(Table.Name+"."+Key.Name+"="+KeySelect.get(Key));

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        String SetString = "";
        for(Map.Entry<PropertyField,String> SetProperty : PropertySelect.entrySet())
            SetString = (SetString.length()==0?"":SetString+",") + SetProperty.getKey().Name + "=" + SetProperty.getValue();

        return "UPDATE " + Syntax.getUpdate(Table.Name," SET "+SetString,FromSelect,(WhereString.length()==0?"":" WHERE "+WhereString));
    }

    String getInsertLeftKeys(SQLSyntax Syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField,PropertyField> LeftKeysQuery = new JoinQuery<KeyField,PropertyField>(Table.Keys);
        // при Join'им ModifyQuery
        LeftKeysQuery.Wheres.add(new JoinWhere(new UniJoin<KeyField,PropertyField>(Change,LeftKeysQuery)));
        // исключим ключи которые есть
        LeftKeysQuery.Wheres.add(new ExcludeJoinWhere(new UniJoin<KeyField,PropertyField>(Table,LeftKeysQuery)));

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

        return "INSERT INTO " + Table.Name + " (" + InsertString + ") " + SelectString;
    }

    void outSelect(DataAdapter Adapter) throws SQLException {
        System.out.println("Table");
        Table.outSelect(Adapter);
        System.out.println("Source");
        Change.outSelect(Adapter);
    }
}

