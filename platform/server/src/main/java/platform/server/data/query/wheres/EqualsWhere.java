package platform.server.data.query.wheres;

import platform.server.data.query.exprs.*;
import platform.server.data.query.CompileSource;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.HashContext;
import platform.server.data.query.translators.Translator;
import platform.server.data.classes.StringClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.where.Where;
import platform.server.where.DataWhereSet;
import platform.server.caches.ParamLazy;
import platform.interop.Compare;

public class EqualsWhere extends CompareWhere {

    private EqualsWhere(AndExpr iOperator1, AndExpr iOperator2) {
        super(iOperator1, iOperator2);

        assert !(operator1 instanceof ValueExpr && operator2 instanceof KeyExpr);
    }

    public static Where create(AndExpr operator1, AndExpr operator2) {
        return create(new EqualsWhere(operator1, operator2));
    }

    public EqualsWhere(KeyExpr iOperator1, KeyExpr iOperator2) {
        super(iOperator1, iOperator2);
    }

    public EqualsWhere(KeyExpr iOperator1, ValueExpr iOperator2) {
        super(iOperator1, iOperator2);
    }

    public EqualsWhere(AndExpr iOperator1, ValueExpr iOperator2) {
        super(iOperator1, iOperator2);
    }

    static boolean containsMask(String string) {
        return string.contains("%") || string.contains("_");
    }
    static String getCompare(AndExpr expr) {
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

    public DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(operator1.getFollows());
        follows.addAll(operator2.getFollows());
        return follows;
    }

    public InnerJoins getInnerJoins() {
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr)
            return new InnerJoins((KeyExpr)operator1,(ValueExpr)operator2);
        return operator1.getWhere().and(operator2.getWhere()).getInnerJoins().and(new InnerJoins(this));
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof EqualsWhere && operator1.equals(((EqualsWhere)o).operator1) && operator2.equals(((EqualsWhere)o).operator2));
    }

    public int hashContext(HashContext hashContext) {
        return operator1.hashContext(hashContext)*31 + operator2.hashContext(hashContext)*31*31;
    }

    @ParamLazy
    public Where translate(Translator translator) {
        return operator1.translate(translator).compare(operator2.translate(translator),Compare.EQUALS);
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return new EqualsWhere(operator1.packFollowFalse(falseWhere),operator2.packFollowFalse(falseWhere));
    }

    public ClassExprWhere calculateClassWhere() {

        ClassExprWhere classWhere1 = operator1.getWhere().getClassWhere();
        ClassExprWhere classWhere2 = operator2.getWhere().getClassWhere();

        if(operator2 instanceof VariableClassExpr) {
            if(operator1 instanceof StaticClassExpr)
                classWhere2 = classWhere2.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExpr)operator1).getStaticClass()));
            else {
                if(operator1 instanceof MapExpr)
                    classWhere1 = classWhere1.andEquals((MapExpr)operator1,(VariableClassExpr)operator2);
                if(operator2 instanceof MapExpr)
                    classWhere2 = classWhere2.andEquals((MapExpr)operator2,(VariableClassExpr)operator1);
            }
        } else
        if(operator1 instanceof VariableClassExpr)
            classWhere1 = classWhere1.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExpr)operator2).getStaticClass()));

        return classWhere1.and(classWhere2);
    }
}
