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
            return ((JoinType.length()==0)?"":" "+JoinType) + " JOIN " + SourceFrom + WhereFrom + JoinFrom;
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
    abstract public String GetSelect();

    public String GetSource() {
        return "("+GetSelect()+")";
    }
}

// UNION особо не по Order'ишь так как нету источника
class UnionQuery extends Query {
    
    FromQuery Query;
    List<FromQuery> ExtraUnions;
    
    public String GetSelect() {
        String Source = Query.GetSelect();
        
        Iterator<FromQuery> u = ExtraUnions.iterator();
        while (u.hasNext())
            Source = Source + " UNION ALL " + u.next().GetSelect();
        
        return Source;
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
        GroupBy = new ArrayList<SelectExpression>();
        AggrExprs = new ArrayList<GroupExpression>();
    }
    
    List<SelectExpression> GroupBy;
    List<GroupExpression> AggrExprs;
    
    public String GetSelect() {
        String FromString = From.GetFrom(this,true);
        String ExprString = "", GroupString = "";

        Iterator<SelectExpression> i = GroupBy.iterator();
        while (i.hasNext()) {
            SelectExpression Field = i.next();
            ExprString = (ExprString.length()==0?"":ExprString+',') + Field.GetSelect();
            GroupString = (GroupString.length()==0?"":GroupString+',') + Field.Expr.GetSource();
        }

        Iterator<GroupExpression> j = AggrExprs.iterator();
        while (j.hasNext())
            ExprString = (ExprString.length()==0?"":ExprString+',') + j.next().GetSelect();
        
        return "SELECT " + ExprString + FromString + " GROUP BY " + GroupString;
    }
}

class SelectQuery extends FromQuery {
    List<SelectExpression> Expressions;
   
    SelectQuery(Select iFrom) { 
        super(iFrom);
        Expressions = new ArrayList<SelectExpression>();
    }

    public String GetSelect() {
        String FromString = From.GetFrom(this,true);
        String ExprString = "";

        Iterator<SelectExpression> j = Expressions.iterator();
        while (j.hasNext())
            ExprString = (ExprString.length()==0?"":ExprString+',') + j.next().GetSelect();
        
        return "SELECT " + ExprString + FromString;
   }
    
    public String GetInsert() {
        String ExprString = "";

        Iterator<SelectExpression> j = Expressions.iterator();
        while (j.hasNext())
            ExprString = (ExprString.length()==0?"":ExprString+',') + j.next().Field;
        
        return ExprString;
    }
   
    // получае Update
    public String GetUpdate() {
        String FromString  = From.GetFrom(this,true);
        String ExprString = "";

        Iterator<SelectExpression> j = Expressions.iterator();
        while (j.hasNext())
            ExprString = (ExprString.length()==0?"":ExprString+',') + j.next().GetUpdate();
        
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

    @Override
    public String GetSelect() {
        String Select = super.GetSelect();
        
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
        return Source.GetSource() + (Compare==0?"=":(Compare==1?">=":"<=")) + (Value instanceof String?"'"+Value+"'":Value.toString());
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

abstract class Expression {
    
    Expression(SourceExpr Source,String AsField) {Field=AsField;Expr=Source;};
    // под каким имененем выбирать выражение
    String Field;
    SourceExpr Expr;
    
    abstract public String GetSelect();
}

class SelectExpression extends Expression {
    
    SelectExpression(SourceExpr Source,String AsField) {super(Source,AsField);};
    
    public String GetSelect() {
        return Expr.GetSource() + " AS " + Field;
    }

    public String GetUpdate() {
        return Field + " = " + Expr.GetSource();
    }
}

class GroupExpression extends Expression {
    
    GroupExpression(SourceExpr Source,String AsField) {super(Source,AsField);};
    
    public String GetSelect() {
        return "SUM(" + Expr.GetSource() + ") AS " + Field;
    }
}
