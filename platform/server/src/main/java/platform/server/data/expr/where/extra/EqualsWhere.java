package platform.server.data.expr.where.extra;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprStatJoin;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWhere;

import java.util.Set;

public class EqualsWhere extends CompareWhere<EqualsWhere> {

    // public только для symmetricWhere
    public EqualsWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(operator1.compatibleEquals(operator2))
            return operator1.getWhere();
        if(operator1 instanceof StaticExpr && operator1.getClass() == operator2.getClass())
            return FALSE;
        return create(operator1, operator2, new EqualsWhere(operator1, operator2));
    }

    public EqualsWhere(KeyExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected String getCompareSource(CompileSource compile) {
        return "=";
    }

    @Override
    public boolean twins(TwinImmutableInterface o) {
        return (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator1) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator2) ||
               (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator2) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator1)));
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31;
    }

    protected EqualsWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new EqualsWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.EQUALS;
    }

    @Override
    public KeyEquals calculateKeyEquals() {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEquals((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEquals((KeyExpr) operator2, operator1);
        return super.calculateKeyEquals();
    }

    @Override
    public MeanClassWhere getMeanClassWhere() {
        QuickSet<QuickSet<VariableClassExpr>> equals = new QuickSet<QuickSet<VariableClassExpr>>();
        ClassExprWhere classWhere = getOperandWhere().getClassWhere();

        if(operator2 instanceof VariableClassExpr && operator1 instanceof StaticClassExprInterface)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExprInterface)operator1).getStaticClass()));
        if(operator2 instanceof VariableClassExpr && operator1 instanceof VariableClassExpr)
            equals.add(new QuickSet<VariableClassExpr>((VariableClassExpr)operator1, (VariableClassExpr) operator2));
        if(operator1 instanceof VariableClassExpr && operator2 instanceof StaticClassExprInterface)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExprInterface)operator2).getStaticClass()));

        return new MeanClassWhere(classWhere, equals);
    }
    // повторяет FormulaWhere так как должен andEquals сделать
    @Override
    public ClassExprWhere calculateClassWhere() {
        return getMeanClassWhere().getClassWhere(operator1, operator2); // именно так а не как Formula потому как иначе бесконечный цикл getMeanClassWheres -> MeanClassWhere.getClassWhere -> means(isFalse) и т.д. пойдет
    }
}
