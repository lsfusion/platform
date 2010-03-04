package platform.server.data.expr.where;

import platform.interop.Compare;
import platform.server.caches.ParamLazy;
import platform.server.caches.HashContext;
import platform.server.classes.StringClass;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.InnerJoins;
import platform.server.data.expr.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.base.BaseUtils;

public class EqualsWhere extends CompareWhere<EqualsWhere> {

    private EqualsWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof ValueExpr && operator2 instanceof ValueExpr)
            return BaseUtils.hashEquals(operator1,operator2)?Where.TRUE:Where.FALSE;
        if(BaseUtils.hashEquals(operator1,operator2))
            return operator1.getWhere();
        return create(new EqualsWhere(operator1, operator2));
    }

    public EqualsWhere(KeyExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    static boolean containsMask(String string) {
        return string.contains("%") || string.contains("_");
    }
    static String getCompare(BaseExpr expr) {
        if(expr instanceof ValueExpr && ((ValueExpr)expr).objectClass instanceof StringClass && containsMask((String)((ValueExpr)expr).object))
            return " LIKE ";
        else
            return "=";
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    public String getSource(CompileSource compile) {
        return operator1.getSource(compile) + getCompare(operator2) + operator2.getSource(compile);
    }
    
    @Override
    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String result = operator1.getWhere().isTrue()?"":op1Source + " IS NULL";
        String op2Source = operator2.getSource(compile);
        if(!operator2.getWhere().isTrue())
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + op1Source + getCompare(operator2) + op2Source;
        if(result.length()==0)
            return compare;
        else
            return "(" + result + " OR " + compare + ")";
    }

    public InnerJoins getInnerJoins() {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new InnerJoins((KeyExpr)operator1,operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new InnerJoins((KeyExpr)operator2,operator1);
        return operator1.getWhere().and(operator2.getWhere()).getInnerJoins().and(new InnerJoins(this));
    }

    public boolean twins(AbstractSourceJoin o) {
        return (operator1.equals(((EqualsWhere)o).operator1) && operator2.equals(((EqualsWhere)o).operator2)) ||
               (operator1.equals(((EqualsWhere)o).operator2) && operator2.equals(((EqualsWhere)o).operator1)) ;
    }

    public int hashContext(HashContext hashContext) {
        return operator1.hashContext(hashContext)*31 + operator2.hashContext(hashContext)*31;
    }

    protected EqualsWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new EqualsWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.EQUALS;
    }

    public ClassExprWhere calculateClassWhere() {

        ClassExprWhere classWhere1 = operator1.getWhere().getClassWhere();
        ClassExprWhere classWhere2 = operator2.getWhere().getClassWhere();

        if(operator2 instanceof VariableClassExpr) {
            if(operator1 instanceof StaticClassExpr)
                classWhere2 = classWhere2.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExpr)operator1).getStaticClass()));
            else {
                if(operator1 instanceof InnerExpr)
                    classWhere1 = classWhere1.andEquals((InnerExpr)operator1,(VariableClassExpr)operator2);
                if(operator2 instanceof InnerExpr)
                    classWhere2 = classWhere2.andEquals((InnerExpr)operator2,(VariableClassExpr)operator1);
            }
        } else
        if(operator1 instanceof VariableClassExpr)
            classWhere1 = classWhere1.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExpr)operator2).getStaticClass()));

        return classWhere1.and(classWhere2);
    }
}
