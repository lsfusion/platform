package platform.server.data.expr.where;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.StringClass;
import platform.server.data.expr.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.where.EqualMap;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWhere;

import java.util.HashMap;
import java.util.Map;

public class EqualsWhere extends CompareWhere<EqualsWhere> {

    // public только для symmetricWhere
    public EqualsWhere(BaseExpr operator1, BaseExpr operator2) {
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

    public boolean twins(AbstractSourceJoin o) {
        return (operator1.equals(((EqualsWhere)o).operator1) && operator2.equals(((EqualsWhere)o).operator2)) ||
               (operator1.equals(((EqualsWhere)o).operator2) && operator2.equals(((EqualsWhere)o).operator1)) ;
    }

    @IdentityLazy
    public int hashContext(HashContext hashContext) {
        return operator1.hashContext(hashContext)*31 + operator2.hashContext(hashContext)*31;
    }

    protected EqualsWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new EqualsWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.EQUALS;
    }

    @Override
    public ObjectJoinSets groupObjectJoinSets() {
        assert !(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1));
        assert !(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2));
        return super.groupObjectJoinSets();
    }

    @Override
    public KeyEquals groupKeyEquals() {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEquals((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEquals((KeyExpr) operator2, operator1);
        return super.groupKeyEquals();
    }

    @Override
    public MeanClassWhere getMeanClassWhere() {
        Map<VariableClassExpr,VariableClassExpr> equals = new HashMap<VariableClassExpr, VariableClassExpr>();
        ClassExprWhere classWhere = operator1.getWhere().getClassWhere().and(operator2.getWhere().getClassWhere());

        if(operator2 instanceof VariableClassExpr && operator1 instanceof StaticClassExpr)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExpr)operator1).getStaticClass()));
        if(operator2 instanceof VariableClassExpr && operator1 instanceof VariableClassExpr)
            equals.put((VariableClassExpr)operator1,(VariableClassExpr)operator2);
        if(operator1 instanceof VariableClassExpr && operator2 instanceof StaticClassExpr)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExpr)operator2).getStaticClass()));

        return new MeanClassWhere(classWhere, equals);
    }
    // повторяет FormulaWhere так как должен andEquals сделать
    public ClassExprWhere calculateClassWhere() {
        MeanClassWhere meanWhere = getMeanClassWhere(); // именно так а не как Formula потому как иначе бесконечный цикл getMeanClassWheres -> MeanClassWhere.getClassWhere -> means(isFalse) и т.д. пойдет
        if(operator1 instanceof VariableClassExpr && operator2 instanceof VariableClassExpr) {
            assert meanWhere.equals.size()==1;
            EqualMap equalMap = new EqualMap(2);
            equalMap.add(operator1,operator2);
            return meanWhere.classWhere.andEquals(equalMap);
        } else {
            assert meanWhere.equals.size()==0;
            return meanWhere.classWhere;
        }
    }

}
