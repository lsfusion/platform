/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;


abstract class From {

    From() {
        Joins = new ArrayList();
        Wheres = new ArrayList();
    }
    
    // к кому Join'ся
    public From JoinTo;
    // как Join'ся (LEFT, INNER, FULL)
    public String JoinType = "";
    // обратная связь по дереву
    public List<From> Joins;
    // фильтры на Select
    public List<Where> Wheres;
    
    // под каким Alias'ом попала в запрос
    public String Alias;
    
    abstract public String GetSource();
    
    // получает строку FROM
    public String GetFrom(AliasCounter Counter,boolean First) {
        // зафиксируем Alias
        Counter.FillAlias(this);
        
        //получаем все Join строки
        String JoinFrom = "";
        Iterator<From> j = Joins.iterator();
        while (j.hasNext())
            JoinFrom = JoinFrom + j.next().GetFrom(Counter,false);

        String WhereFrom="";
        Iterator<Where> w = Wheres.iterator();
        while (w.hasNext())
            WhereFrom = (WhereFrom.length()==0?(First?" WHERE ":" ON "):WhereFrom+" AND ") + w.next().GetSelect(this);

        String SourceFrom = GetSource() + " " + Alias;

        if (First)
            return " FROM " + SourceFrom + JoinFrom + WhereFrom;
        else
            return ((JoinType.length()==0)?"":" "+JoinType) + " JOIN " + SourceFrom + (WhereFrom.length()==0?" ON 1=1":WhereFrom) + JoinFrom;
    }

}

class AliasCounter {
    
    protected Integer Counter = 0;
    public void FillAlias(From Object)
    {
        Counter++;
        Object.Alias = "T" + Counter.toString();
    }           
}

// запрос
// из таблицы

class FromTable extends From {
    
    FromTable(String iName) {Name=iName;}
    
    // имя таблицы
    protected String Name;
        
    public String GetSource() {

        return Name;
    }
    
    public String GetDelete() {

        String DeleteFrom = GetFrom(new AliasCounter(),true);
        return "DELETE " + Alias + DeleteFrom;
    }
}

class FromQuery extends From {
    
    FromQuery(Query iFrom) {From=iFrom;}
    
    Query From;

    public String GetSource() {

        return "("+From.GetSelect(new ArrayList())+")";
    }

}

abstract class Query {
    
    // собственно основной метод который получает Select
    abstract public String GetSelect(Collection<String> GetFields);
    
    public String GetSource() {
        return "("+GetSelect(new ArrayList())+")";
    }
    
    abstract boolean containsField(String Field);
}

// объединяет запросы (направо считает более новыми значениями) 


class UnionQuery extends Query {
      
    // по сути чтобы разделить
    Collection<String> Keys;
    Collection<String> Values;
    Map<String,Integer> ValueKeys;
    List<Query> Unions;
    Map<Query,Integer> Coeffs;
    int Operator;
    
    UnionQuery(int iOperator) {
        Keys = new ArrayList();
        Values = new ArrayList();
        ValueKeys = new HashMap();
        Unions = new ArrayList();
        Coeffs = new HashMap();
        Operator = iOperator;        
    }

    public String GetSelect(Collection<String> GetFields) {

        Collection<String> Fields = new ArrayList(Keys);
        Fields.addAll(Values);

        SelectQuery ResultQuery = null;
        if(Unions.size()==1 && Unions.get(0) instanceof SelectQuery && (Coeffs.size()==0 || Coeffs.get(Unions.get(0))==1)) {
            // если один запрос его и возвращаем и коэффициент не 1
            ResultQuery = (SelectQuery)Unions.get(0);
        } else {
            // иначе строим UnionQuery
            ListIterator<Query> i = Unions.listIterator();
            // сольем в один collection

            ResultQuery = new SelectQuery(null);
        
            From LastQuery=null;
            while(i.hasNext()) {
                Query SelectQuery = i.next();
                From Query = new FromQuery(SelectQuery);

                if(LastQuery==null)
                    ResultQuery.From = Query;
                else {
                    LastQuery.Joins.add(Query);
                    Query.JoinType = "FULL";

                    Iterator<String> ik = Keys.iterator();
                    while(ik.hasNext()) {
                        String Field = ik.next();
                        Query.Wheres.add(new FieldWhere(ResultQuery.Expressions.get(Field),Field));
                    }
                }

                Iterator<String> ifi = Fields.iterator();
                while(ifi.hasNext()) {
                    String Field = ifi.next();
                    if(SelectQuery.containsField(Field)) {
                       SourceExpr NewValue = new FieldSourceExpr(Query,Field);
                       if(Values.contains(Field)) {
                            // значение применяем оператор
                            Integer FieldCoeff = Coeffs.get(SelectQuery);
                            
                            ListSourceExpr OldValue = null;
                            if(ResultQuery.Expressions.containsKey(Field))
                                OldValue = (ListSourceExpr)ResultQuery.Expressions.get(Field);
                            else {
                                OldValue = new ListSourceExpr(Operator);
                                ResultQuery.Expressions.put(Field,OldValue);
                            }
                        
                            OldValue.AddOperand(NewValue,FieldCoeff);
                        } else {
                            // ключ всегда на IsNull
                            if(LastQuery!=null)
                               NewValue = new IsNullSourceExpr(NewValue,ResultQuery.Expressions.get(Field));
                            ResultQuery.Expressions.put(Field,NewValue);
                        }
                    }
                }
                LastQuery = Query;
            }
        }

        Iterator<String> ivs = ValueKeys.keySet().iterator();
        while(ivs.hasNext()) {
            String ValueField = ivs.next();
            ResultQuery.Expressions.put(ValueField,new ValueSourceExpr(ValueKeys.get(ValueField)));
        }

        return ResultQuery.GetSelect(GetFields);
    }

    boolean containsField(String Field) {
        return Keys.contains(Field) || Values.contains(Field);
    }
}
abstract class DataQuery extends Query {
    // откуда делать Select
    From From;
    DataQuery(From iFrom) {
        From = iFrom;
    }
}

class GroupQuery extends DataQuery {
    
    GroupQuery(From iFrom) {
        super(iFrom);
        GroupBy = new HashMap();
        AggrExprs = new HashMap();
    }

    Map<String,SourceExpr> GroupBy;
    Map<String,GroupExpression> AggrExprs;
    
    public String GetSelect(Collection<String> GetFields) {
        String FromString = From.GetFrom(new AliasCounter(),true);
        String ExprString = "", GroupString = "";

        Iterator<String> i = GroupBy.keySet().iterator();
        while (i.hasNext()) {
            String Field = i.next();
            String Source = GroupBy.get(Field).GetSource();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Source + " AS " + Field;
            GroupString = (GroupString.length()==0?"":GroupString+',') + Source;
            GetFields.add(Field);
        }

        Iterator<String> j = AggrExprs.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + AggrExprs.get(Field).GetSelect() + " AS " + Field;
            GetFields.add(Field);
        }
        
        return "SELECT " + ExprString + FromString + " GROUP BY " + GroupString;
    }
    
    boolean containsField(String Field) {
        return GroupBy.containsKey(Field) || AggrExprs.containsKey(Field);
    }
}

class SelectQuery extends DataQuery {
    Map<String,SourceExpr> Expressions;

    Integer Top = 0;

    SelectQuery(From iFrom) { 
        super(iFrom);
        Expressions = new HashMap();
    }

    public String GetSelect(Collection<String> GetFields) {
        String FromString = From.GetFrom(new AliasCounter(),true);
        String ExprString = "";

        Iterator<String> j = Expressions.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Expressions.get(Field).GetSource() + " AS " + Field;
            GetFields.add(Field);
        }

        return "SELECT " + (Top>0?"TOP "+Top.toString()+" ":"") + ExprString + FromString;
   }
    
    // получае Update
    public String GetUpdate() {
        String FromString  = From.GetFrom(new AliasCounter(),true);
        String ExprString = "";

        Iterator<String> j = Expressions.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Field + " = " + Expressions.get(Field).GetSource();
        }
        
        return "UPDATE " + From.Alias + " SET " + ExprString + FromString;
    }

    boolean containsField(String Field) {
        return Expressions.containsKey(Field);
    }
}

class OrderedSelectQuery extends SelectQuery {
    List<SourceExpr> Orders;

    boolean Descending = false;

    OrderedSelectQuery(From iFrom) {
        super(iFrom);
        Orders = new ArrayList<SourceExpr>();
    }

    public String GetSelect(Collection<String> GetFields) {
        String Select = super.GetSelect(GetFields);
        
        String OrderString="";
        Iterator<SourceExpr> o = Orders.iterator();
        while(o.hasNext())
            OrderString = (OrderString.length()==0?" ORDER BY ":OrderString+',') + o.next().GetSource();

        return Select+OrderString+(Descending?" DESC":" ASC");
    }
}      

// WHERE классы

abstract class Where {

    abstract public String GetSelect(From From);
}

class FieldWhere extends Where {
    
    FieldWhere(SourceExpr iSource,String iField) {Field = iField; Source = iSource;};
    
    String Field;
    // можно было бы номер в дереве но это не очень удобно
    SourceExpr Source;

    public String GetSelect(From From) {
        return From.Alias + '.' + Field + '=' + Source.GetSource();
    }
}

class SourceIsNullWhere extends Where {
    SourceIsNullWhere(SourceExpr iSource) {Source = iSource;}

    SourceExpr Source;

    public String GetSelect(From From) {
        return Source.GetSource() + " IS NULL";
    }
}

class FieldValueWhere extends Where {
    
    FieldValueWhere(Integer iValue,String iField) {
        Value = iValue;
        Field = iField;
    }
            
    Integer Value;
    String Field;

    public String GetSelect(From From) {
        return From.Alias + '.' + Field + '=' + Value.toString();
    }
}

class FieldSetValueWhere extends Where {
    FieldSetValueWhere(Collection<Integer> iSetValues,String iField) {Field=iField; SetValues=iSetValues;};
    String Field;
    Collection<Integer> SetValues;

    public String GetSelect(From From) {
        String ListString = "";
        Iterator<Integer> i = SetValues.iterator();
        while(i.hasNext()) {
            ListString = (ListString.length()==0?"":ListString+',') + i.next().toString();
        }
        return From.Alias + '.' + Field + " IN (" + ListString + ")";
    }

}

// не привязанные к таблице отборы

class FieldExprCompareWhere extends Where {
    FieldExprCompareWhere(SourceExpr iSource,Object iValue,int iCompare) {Source=iSource;Value=iValue;Compare=iCompare;}
            
    SourceExpr Source;
    Object Value;
    int Compare;

    @Override
    public String GetSelect(From From) {
        return Source.GetSource() + (Compare==0?"=":(Compare==1?">":(Compare==2?"<":(Compare==3?">=":"<=")))) + (Value instanceof String?"'"+Value+"'":(Value instanceof SourceExpr?((SourceExpr)Value).GetSource():Value.toString()));
    }
}

// оператор AND или OR

class FieldOPWhere extends Where {
    FieldOPWhere(Where iOp1,Where iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    Where Op1, Op2;
    boolean And;

    public String GetSelect(From From) {
        return "(" + Op1.GetSelect(From) + " " + (And?"AND":"OR") + " " + Op2.GetSelect(From) + ")";
    }
}

// EXPR классы
// ОНИ В КЭШАХ УЧАВСТВУЮТ, ПОЭТОМУ EQUALS ПЕРЕГРУЖАТСЯ

abstract class SourceExpr {
    
    abstract public String GetSource();
}


class ValueSourceExpr extends SourceExpr {

    ValueSourceExpr(Object iValue) {Value=iValue;};
    Object Value;

    public String GetSource() {
        if(Value instanceof String) 
            return "'" + Value + "'";
        else
            return Value.toString();        
    }
    
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof ValueSourceExpr)) return false;
        
        return Value.equals(((ValueSourceExpr)o).Value);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.Value != null ? this.Value.hashCode() : 0);
        return hash;
    }
}


class FieldSourceExpr extends SourceExpr {
    
    FieldSourceExpr(From iSource,String iSourceField) {Source=iSource;SourceField=iSourceField;};
    
    From Source;
    protected String SourceField;

    public String GetSource() {
        return Source.Alias + '.' + SourceField;
    }
    
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof FieldSourceExpr)) return false;
        
        return Source.equals(((FieldSourceExpr)o).Source) && SourceField.equals(((FieldSourceExpr)o).SourceField);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.Source != null ? this.Source.hashCode() : 0);
        hash = 67 * hash + (this.SourceField != null ? this.SourceField.hashCode() : 0);
        return hash;
    }
}


class IsNullSourceExpr extends SourceExpr {
    
    IsNullSourceExpr(SourceExpr iPrimaryExpr,SourceExpr iSecondaryExpr) {PrimaryExpr=iPrimaryExpr;SecondaryExpr=iSecondaryExpr;};
    
    SourceExpr PrimaryExpr;
    SourceExpr SecondaryExpr;

    public String GetSource() {
        return "ISNULL(" + PrimaryExpr.GetSource() + "," + SecondaryExpr.GetSource() + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof IsNullSourceExpr)) return false;
        
        return PrimaryExpr.equals(((IsNullSourceExpr)o).PrimaryExpr) && SecondaryExpr.equals(((IsNullSourceExpr)o).SecondaryExpr);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.PrimaryExpr != null ? this.PrimaryExpr.hashCode() : 0);
        hash = 83 * hash + (this.SecondaryExpr != null ? this.SecondaryExpr.hashCode() : 0);
        return hash;
    }
}
class FormulaSourceExpr extends SourceExpr {
   
   FormulaSourceExpr(String iFormula) {Formula=iFormula; Params=new HashMap<String,SourceExpr>();};
    
   String Formula;
   Map<String,SourceExpr> Params;
    
   public String GetSource() {

       String SourceString = Formula;
       Iterator<String> i = Params.keySet().iterator();
       while (i.hasNext()) {
           String Prm = i.next();
           SourceString = SourceString.replace(Prm,Params.get(Prm).GetSource());
       }
       
        return SourceString;
    }

    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof FormulaSourceExpr)) return false;
        
        return Formula.equals(((FormulaSourceExpr)o).Formula) && Params.equals(((FormulaSourceExpr)o).Params);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.Formula != null ? this.Formula.hashCode() : 0);
        hash = 59 * hash + (this.Params != null ? this.Params.hashCode() : 0);
        return hash;
    }
}

class ListSourceExpr extends SourceExpr {

    ListSourceExpr(int iOperator) {
        Operator = iOperator;
        Operands = new ArrayList();
        Coeffs = new HashMap();
    }
    
    // 0 - MAX
    // 1 - +
    // 2 - ISNULL
    int Operator;
    List<SourceExpr> Operands;
    Map<SourceExpr,Integer> Coeffs;
    
    void AddOperand(SourceExpr Operand,Integer Coeff) {
        Operands.add(Operand);
        Coeffs.put(Operand,Coeff);
    }

    public String GetSource() {
        ListIterator<SourceExpr> i = Operands.listIterator();
        String Result = "";
        while(i.hasNext()) {
            SourceExpr Operand = i.next();
            Integer Coeff = Coeffs.get(Operand);
            if(Coeff==null) Coeff = 1;
            String OperandString = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString()));
            if(Operator==1)
                OperandString += "ISNULL(" + Operand.GetSource() + ",0)";
            else
                OperandString += Operand.GetSource();

            if(Operator==0 || Operator==1) {
                if(Result.length()==0) {
                    Result = OperandString;
                } else {
                    if(Operator==1)
                        Result = Result+(Coeff>=0?"+":"") + OperandString;
                    else
                        Result = "(CASE WHEN "+OperandString+" IS NULL OR "+Result+">"+OperandString+" THEN "+Result+" ELSE "+OperandString+" END)";
                }
            } else
                Result = (Result.length()==0?OperandString:"ISNULL(" + OperandString + "," + Result + ")");
        }

        return Result;
    }
}

class GroupExpression {

    SourceExpr Expr;
    int Operation;

    GroupExpression(SourceExpr iExpr,int iOperation) {Expr=iExpr; Operation=iOperation;};
    
    public String GetSelect() {
        return (Operation==1?"SUM":"MAX") + "(" + Expr.GetSource() + ")";
    }
}

