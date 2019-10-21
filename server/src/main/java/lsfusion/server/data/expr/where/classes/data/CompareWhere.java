package lsfusion.server.data.expr.where.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.StaticClassExprInterface;
import lsfusion.server.data.expr.classes.VariableClassExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.OrObjectWhere;
import lsfusion.server.data.where.OrWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.DataClass;

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
            boolean orExtra = true;
            if (this instanceof GreaterWhere) {
                orExtra = !((GreaterWhere)this).orEquals;
                signCompare = new EqualsWhere(operator1, operator2);
            } else
                signCompare = new GreaterWhere(operator1, operator2, false);

            OrObjectWhere[] operators = getOperandWhere().getOr();
            OrObjectWhere[] symmetricOrs = new OrObjectWhere[operators.length + (orExtra ? 2 : 1)];
            System.arraycopy(operators, 0, symmetricOrs, 0, operators.length);
            symmetricOrs[operators.length] = backCompare.not();
            if(orExtra)
                symmetricOrs[operators.length+1] = signCompare.not();

            symmetricWhere = toWhere(symmetricOrs);
        }
        return symmetricWhere;
    }

    public boolean checkTrue(Where where) {
        return OrWhere.checkTrue(getSymmetricWhere(),where);
    }

    public static <K> Where compare(ImMap<? extends K, ? extends Expr> map1, ImMap<K, ? extends Expr> map2) {
        Where where = Where.TRUE();
        for(int i=0,size=map1.size();i<size;i++)
            where = where.and(map1.getValue(i).compare(map2.get(map1.getKey(i)), Compare.EQUALS));
        return where;
    }
    public static <K> Where equalsNull(ImMap<? extends K, ? extends Expr> map1, ImMap<K, ? extends Expr> map2) {
        Where where = Where.TRUE();
        for(int i=0,size=map1.size();i<size;i++)
            where = where.and(map1.getValue(i).equalsNull(map2.get(map1.getKey(i))));
        return where;
    }

    public static <A extends Expr,B extends Expr> Where compare(ImMap<A, B> exprs) {
        return compare(exprs.keys().toMap(), exprs);
    }

    public static <K> Where compareExprValues(ImRevMap<K, KeyExpr> mapKeys, ImMap<K, ? extends Expr> mapValues) {
        return compare(mapKeys.filterIncl(mapValues.keys()), mapValues);
    }

    public static <K, KV extends K> Where compareInclValues(ImMap<K,? extends Expr> map,ImMap<KV, ? extends ObjectValue> mapValues) {
        return compareValues(map.filterIncl(mapValues.keys()), mapValues);
    }

    public static <K> Where compareValues(ImMap<K,? extends Expr> map,ImMap<K, ? extends ObjectValue> mapValues) {
        Where where = Where.TRUE();
        for(int i=0,size=map.size();i<size;i++)
            where = where.and(map.getValue(i).compare(mapValues.get(map.getKey(i)).getExpr(), Compare.EQUALS));
        return where;
    }

    @Override
    public MeanClassWhere getMeanClassWhere() {
        ImSet<ImSet<VariableClassExpr>> comps = SetFact.EMPTY();
        ClassExprWhere classWhere = getOperandClassWhere();

        boolean isEquals = isEquals();
        ConcreteClass staticClass;
        if(operator2 instanceof VariableSingleClassExpr && operator1 instanceof StaticClassExprInterface &&
                (staticClass = ((StaticClassExprInterface)operator1).getStaticClass()) != null && (isEquals || (staticClass instanceof DataClass && ((DataClass)staticClass).fixedSize())))
            classWhere = classWhere.and(new ClassExprWhere((VariableSingleClassExpr)operator2, staticClass));
        if(operator2 instanceof VariableClassExpr && operator1 instanceof VariableClassExpr)
            comps = SetFact.singleton(SetFact.toSet((VariableClassExpr)operator1, (VariableClassExpr) operator2));
        if(operator1 instanceof VariableSingleClassExpr && operator2 instanceof StaticClassExprInterface &&
                (staticClass = ((StaticClassExprInterface)operator2).getStaticClass()) != null && (isEquals || (staticClass instanceof DataClass && ((DataClass)staticClass).fixedSize())))
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
