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

abstract class Select {

    Select() {
        Joins = new ArrayList<Select>();
        Wheres = new ArrayList<Where>();
    }
    
    // к кому Join'ся
    public Select JoinTo;
    // как Join'ся (LEFT, INNER, FULL)
    public String JoinType = "";
    // обратная связь по дереву
    public List<Select> Joins;
    // фильтры на Select
    public List<Where> Wheres;
    
    // под каким Alias'ом попала в запрос
    public String Alias;
    
    abstract public String GetSource();
    
    // получает строку FROM
    public String GetFrom(Select ExecQuery,boolean First) {
        // зафиксируем Alias
        ExecQuery.FillAlias(this);
        
        //получаем все Join строки
        String JoinFrom = "";
        Iterator<Select> j = Joins.iterator();
        while (j.hasNext())
        {
            Select Join = j.next();
            JoinFrom = JoinFrom + Join.GetFrom(ExecQuery,false);
        }

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

    protected Integer AliasCounter = 0;
    public void FillAlias(Select Object)
    {
        AliasCounter++;
        Object.Alias = "T" + AliasCounter.toString();
    }           
}

// запрос
// из таблицы

class SelectTable extends Select {
    
    SelectTable(String iName) {Name=iName;}
    
    // имя таблицы
    protected String Name;
        
    public String GetSource() {

        return Name;
    }
    
    public String GetDelete() {

        String DeleteFrom = GetFrom(this,true);
        return "DELETE " + Alias + DeleteFrom;
    }
}

abstract class Query extends Select {
    
    // собственно основной метод который получает Select
    abstract public String GetSelect(StringBuilder InsertString);
    
    public String GetSource() {
        return "("+GetSelect(new StringBuilder())+")";
    }
}

// объединяет запросы (направо считает более новыми значениями) 

class UnionQuery extends Query {
      
    // по сути чтобы разделить
    Collection<String> Keys;
    Collection<String> Values;
    Map<String,Integer> ValueKeys;
    List<Query> Unions;
    Map<Query,Integer> SumCoeffs;
    
    UnionQuery() {
        Keys = new ArrayList();
        Values = new ArrayList();
        ValueKeys = new HashMap();
        Unions = new ArrayList();
        SumCoeffs = new HashMap();
    }

    public String GetSelect(StringBuilder InsertString) {

        Collection<String> Fields = new ArrayList(Keys);
        Fields.addAll(Values);

        SelectQuery ResultQuery = null;
        if(Unions.size()==1 && Unions.get(0) instanceof SelectQuery) {
            // если один запрос его и возвращаем
            ResultQuery = (SelectQuery)Unions.get(0);
        } else {
            // иначе строим UnionQuery
            ListIterator<Query> i = Unions.listIterator();
            // сольем в один collection

            ResultQuery = new SelectQuery(null);
        
            Query LastQuery=null;
            while(i.hasNext()) {
                Query Query = i.next();
                Integer Coeff = SumCoeffs.get(Query);

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
                    Integer FieldCoeff = (Values.contains(Field)?Coeff:null);
                    SourceExpr NewValue = new FieldSourceExpr(Query,Field);
                    if(LastQuery==null) {
                        if(FieldCoeff!=null) {
                            FormulaSourceExpr Formula = new FormulaSourceExpr(FieldCoeff+"*ISNULL(prm1,0)");
                            Formula.Params.put("prm1",NewValue);
                            NewValue = Formula;
                        }                        
                    } else {
                        SourceExpr OldValue = ResultQuery.Expressions.get(Field);
                        if(FieldCoeff==null)
                            NewValue = new IsNullSourceExpr(NewValue,OldValue);
                        else {
                            FormulaSourceExpr Formula = new FormulaSourceExpr(FieldCoeff+"*ISNULL(prm2,0)+ISNULL(prm1,0)");
                            Formula.Params.put("prm1",OldValue);
                            Formula.Params.put("prm2",NewValue);
                            NewValue = Formula;
                        }
                    }
                    ResultQuery.Expressions.put(Field,NewValue);
                }

                LastQuery = Query;
            }
        }           

        Iterator<String> ivs = ValueKeys.keySet().iterator();
        while(ivs.hasNext()) {
            String ValueField = ivs.next();
            ResultQuery.Expressions.put(ValueField,new ValueSourceExpr(ValueKeys.get(ValueField)));
        }

        return ResultQuery.GetSelect(InsertString);
    }
}
abstract class FromQuery extends Query {
    // откуда делать Select
    Select From;
    FromQuery(Select iFrom) {
        From = iFrom;
    }
}

class GroupQuery extends FromQuery {
    
    GroupQuery(Select iFrom) {
        super(iFrom);
        GroupBy = new HashMap();
        AggrExprs = new HashMap();
    }

    Map<String,SourceExpr> GroupBy;
    Map<String,GroupExpression> AggrExprs;
    
    public String GetSelect(StringBuilder InsertString) {
        String FromString = From.GetFrom(this,true);
        String ExprString = "", GroupString = "";

        Iterator<String> i = GroupBy.keySet().iterator();
        while (i.hasNext()) {
            String Field = i.next();
            String Source = GroupBy.get(Field).GetSource();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Source + " AS " + Field;
            GroupString = (GroupString.length()==0?"":GroupString+',') + Source;
            InsertString.append((InsertString.length()==0?"":",")+Field);
        }

        Iterator<String> j = AggrExprs.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + AggrExprs.get(Field).GetSelect() + " AS " + Field;
            InsertString.append((InsertString.length()==0?"":",")+Field);
        }
        
        return "SELECT " + ExprString + FromString + " GROUP BY " + GroupString;
    }
}

class SelectQuery extends FromQuery {
    Map<String,SourceExpr> Expressions;

    Integer Top = 0;

    SelectQuery(Select iFrom) { 
        super(iFrom);
        Expressions = new HashMap();
    }

    public String GetSelect(StringBuilder InsertString) {
        String FromString = From.GetFrom(this,true);
        String ExprString = "";

        Iterator<String> j = Expressions.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Expressions.get(Field).GetSource() + " AS " + Field;
            InsertString.append((InsertString.length()==0?"":",")+Field);
        }

        return "SELECT " + (Top>0?"TOP "+Top.toString()+" ":"") + ExprString + FromString;
   }
    
    // получае Update
    public String GetUpdate() {
        String FromString  = From.GetFrom(this,true);
        String ExprString = "";

        Iterator<String> j = Expressions.keySet().iterator();
        while (j.hasNext()) {
            String Field = j.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Field + " = " + Expressions.get(Field).GetSource();
        }
        
        return "UPDATE " + From.Alias + " SET " + ExprString + FromString;
    }
   
}

class OrderedSelectQuery extends SelectQuery {
    List<SourceExpr> Orders;

    boolean Descending = false;

    OrderedSelectQuery(Select iFrom) {
        super(iFrom);
        Orders = new ArrayList<SourceExpr>();
    }

    public String GetSelect(StringBuilder InsertString) {
        String Select = super.GetSelect(InsertString);
        
        String OrderString="";
        Iterator<SourceExpr> o = Orders.iterator();
        while(o.hasNext())
            OrderString = (OrderString.length()==0?" ORDER BY ":OrderString+',') + o.next().GetSource();

        return Select+OrderString+(Descending?" DESC":" ASC");
    }
}      

// WHERE классы

abstract class Where {

    abstract public String GetSelect(Select From);
}

class FieldWhere extends Where {
    
    FieldWhere(SourceExpr iSource,String iField) {Field = iField; Source = iSource;};
    
    String Field;
    // можно было бы номер в дереве но это не очень удобно
    SourceExpr Source;

    public String GetSelect(Select From) {
        return From.Alias + '.' + Field + '=' + Source.GetSource();
    }
}

class FieldIsNullWhere extends Where {
    FieldIsNullWhere(String iField) {Field = iField;}

    String Field;
    public String GetSelect(Select From) {
        return From.Alias + '.' + Field + " IS NULL";
    }
}

class FieldValueWhere extends Where {
    
    FieldValueWhere(Integer iValue,String iField) {
        Value = iValue;
        Field = iField;
    }
            
    Integer Value;
    String Field;

    public String GetSelect(Select From) {
        return From.Alias + '.' + Field + '=' + Value.toString();
    }
}

class FieldSetValueWhere extends Where {
    FieldSetValueWhere(Collection<Integer> iSetValues,String iField) {Field=iField; SetValues=iSetValues;};
    String Field;
    Collection<Integer> SetValues;

    public String GetSelect(Select From) {
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
    public String GetSelect(Select From) {
        return Source.GetSource() + (Compare==0?"=":(Compare==1?">":"<")) + (Value instanceof String?"'"+Value+"'":Value.toString());
    }
}

// оператор AND или OR

class FieldOPWhere extends Where {
    FieldOPWhere(Where iOp1,Where iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    Where Op1, Op2;
    boolean And;

    public String GetSelect(Select From) {
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
    
    FieldSourceExpr(Select iSource,String iSourceField) {Source=iSource;SourceField=iSourceField;};
    
    Select Source;
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

class GroupExpression {

    SourceExpr Expr;

    GroupExpression(SourceExpr iExpr) {Expr=iExpr;};
    
    public String GetSelect() {
        return "SUM(" + Expr.GetSource() + ")";
    }
}
