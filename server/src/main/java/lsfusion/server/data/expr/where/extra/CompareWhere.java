package lsfusion.server.data.expr.where.extra;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.Compare;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.where.OrObjectWhere;
import lsfusion.server.data.where.OrWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.logics.DataObject;

public abstract class CompareWhere<This extends CompareWhere<This>> extends BinaryWhere<This> {
    
    protected CompareWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    // такой же where но без прямых сравнений
    private Where symmetricWhere = null;
    @ManualLazy
    // A>B = !(A=B) AND !(B>A) AND A AND B
    // A=B = !(A>B) AND !(B>A) AND A AND B
    private Where getSymmetricWhere() {
        if(symmetricWhere==null) {
            GreaterWhere backCompare = new GreaterWhere(operator2, operator1, false);
            CompareWhere signCompare;
            if (this instanceof GreaterWhere) {
                assert !((GreaterWhere)this).orEquals;
                signCompare = new EqualsWhere(operator1, operator2);
            } else
                signCompare = new GreaterWhere(operator1, operator2, false);

            OrObjectWhere[] operators = getOperandWhere().getOr();
            OrObjectWhere[] symmetricOrs = new OrObjectWhere[operators.length+2];
            System.arraycopy(operators, 0, symmetricOrs, 0, operators.length);
            symmetricOrs[operators.length] = backCompare.not();
            symmetricOrs[operators.length+1] = signCompare.not(); 

            symmetricWhere = toWhere(symmetricOrs);
        }
        return symmetricWhere;
    }

    public boolean checkTrue(Where where) {
        return OrWhere.checkTrue(getSymmetricWhere(),where);
    }

    public static <K> Where compare(ImMap<K, ? extends Expr> map1, ImMap<K, ? extends Expr> map2) {
        Where where = TRUE;
        for(int i=0,size=map1.size();i<size;i++)
            where = where.and(map1.getValue(i).compare(map2.get(map1.getKey(i)), Compare.EQUALS));
        return where;
    }

    public static <K> Where compareExprValues(ImRevMap<K, KeyExpr> mapKeys, ImMap<K, ? extends Expr> mapValues) {
        return compare(mapKeys.filterIncl(mapValues.keys()), mapValues);
    }

    public static <K> Where compareValues(ImMap<K,? extends Expr> map,ImMap<K, DataObject> mapValues) {
        Where where = TRUE;
        for(int i=0,size=map.size();i<size;i++)
            where = where.and(map.getValue(i).compare(mapValues.get(map.getKey(i)), Compare.EQUALS));
        return where;
    }

    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String op2Source = operator2.getSource(compile);

        String result = "";
        if(!compile.means(operator1.getNotNullWhere()))
            result = op1Source + " IS NULL";
        if(!compile.means(operator2.getNotNullWhere()))
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + op1Source + getCompareSource(compile) + op2Source;
        if(result.length()==0)
            return compare;
        else
            return "(" + result + " OR " + compare + ")";
    }

    @Override
    public MeanClassWhere getMeanClassWhere() {
        ImSet<ImSet<VariableClassExpr>> comps = SetFact.EMPTY();
        ClassExprWhere classWhere = getOperandWhere().getClassWhere();

        boolean isEquals = isEquals();
        ConcreteClass staticClass;
        if(operator2 instanceof VariableSingleClassExpr && operator1 instanceof StaticClassExprInterface &&
                (staticClass = ((StaticClassExprInterface)operator1).getStaticClass()) != null && (isEquals || staticClass instanceof DataClass))
            classWhere = classWhere.and(new ClassExprWhere((VariableSingleClassExpr)operator2, staticClass));
        if(operator2 instanceof VariableClassExpr && operator1 instanceof VariableClassExpr)
            comps = SetFact.singleton(SetFact.toSet((VariableClassExpr)operator1, (VariableClassExpr) operator2));
        if(operator1 instanceof VariableSingleClassExpr && operator2 instanceof StaticClassExprInterface &&
                (staticClass = ((StaticClassExprInterface)operator2).getStaticClass()) != null && (isEquals || staticClass instanceof DataClass))
            classWhere = classWhere.and(new ClassExprWhere((VariableSingleClassExpr)operator1,staticClass));

        return new MeanClassWhere(classWhere, comps, isEquals);
    }

    protected abstract boolean isEquals();

    // повторяет FormulaWhere так как должен andEquals сделать
    @Override
    public ClassExprWhere calculateClassWhere() {
        return getMeanClassWhere().getClassWhere(operator1, operator2, isEquals()); // именно так а не как Formula потому как иначе бесконечный цикл getMeanClassWheres -> MeanClassWhere.getClassWhere -> means(isFalse) и т.д. пойдет
    }
}
