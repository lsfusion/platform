package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.*;

// чисто для оптимизации
public class ExclusiveUnionProperty extends ExclusiveCaseUnionProperty {

    private final Collection<CalcPropertyInterfaceImplement<Interface>> operands;
    private boolean isExclusive;

    private ClassWhere<Object> classValueWhere;

    @IdentityLazy
    protected Iterable<Case> getCases() {
        assert finalized;
        Collection<Case> result = new ArrayList<Case>();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            result.add(new Case(operand, operand));
        return result;
    }

    @Override
    public Set<SessionCalcProperty> getSessionCalcDepends() {
        if(isAbstract())
            return new HashSet<SessionCalcProperty>();

        return super.getSessionCalcDepends();
    }

    public ExclusiveUnionProperty(String sID, String caption, List<Interface> interfaces, Collection<CalcPropertyInterfaceImplement<Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    // для постзадания
    public ExclusiveUnionProperty(String sID, boolean isExclusive, String caption, List<Interface> interfaces, ValueClass valueClass, Map<Interface, ValueClass> interfaceClasses) {
        super(sID, caption, interfaces);

        this.isExclusive = isExclusive;

        operands = new ArrayList<CalcPropertyInterfaceImplement<Interface>>();

        classValueWhere = new ClassWhere<Object>(BaseUtils.<Object, ValueClass>add(interfaceClasses, "value", valueClass), true);
    }

    public void addOperand(CalcPropertyMapImplement<?,Interface> operand) {
        assert isAbstract();

        operands.add(operand);
    }

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        assert finalized;

        return propChanges.getUsedDataChanges(getDepends());
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        assert finalized;

        DataChanges result = new DataChanges();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            result = result.add(operand.mapDataChanges(change, changedWhere, propChanges));
        return result;
    }

    @Override
    protected boolean checkWhere() {
        return false;
    }

    public boolean isAbstract() {
        return classValueWhere != null;
    }

    public ClassWhere<Object> getClassValueWhere() {
        if(isAbstract())
            return classValueWhere;

        return super.getClassValueWhere();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(isAbstract() && propClasses)
            return getClassTableExpr(joinImplement);

        assert finalized;
        return super.calculateNewExpr(joinImplement, propClasses, propChanges, changedWhere);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Map<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(isAbstract())
            return getInterfaceClasses();

        return super.getInterfaceCommonClasses(commonValue);
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        assert finalized;

        return super.calculateIncrementExpr(joinImplement, propChanges, prevExpr, changedWhere);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected boolean checkFull() {
        return false;
    }

    private List<CalcPropertyMapImplement<?, Interface>> getPropertyOperands() {
        List<CalcPropertyMapImplement<?, Interface>> result = new ArrayList<CalcPropertyMapImplement<?, Interface>>();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            if(operand instanceof CalcPropertyMapImplement)
                result.add((CalcPropertyMapImplement<?, Interface>)operand);
        return result;
    }

    public void checkClasses() {
        assert isAbstract();

        List<CalcPropertyMapImplement<?, Interface>> listOperands = getPropertyOperands();
        List<ClassWhere<Object>> listClasses = new ArrayList<ClassWhere<Object>>();

        ClassWhere<Object> fullClassValueWhere = ClassWhere.STATIC(false);
        for(CalcPropertyMapImplement<?, Interface> operand : listOperands) {
            ClassWhere<Object> operandClassValueWhere = operand.mapClassValueWhere();
            if(!operandClassValueWhere.means(classValueWhere))
                throw new RuntimeException("Wrong Classes. Property : " + this + ", Operand : " + operand.property +  ", Calculated : " + operandClassValueWhere + ", Specified : " + classValueWhere);

            listClasses.add(operandClassValueWhere);
            fullClassValueWhere = fullClassValueWhere.or(operandClassValueWhere);
        }

        if (!isExclusive)
            for(int i=0;i<listOperands.size();i++)
                for(int j=i+1;j<listOperands.size();j++) {
                    CalcPropertyMapImplement<?, Interface> op1 = listOperands.get(i);
                    CalcPropertyMapImplement<?, Interface> op2 = listOperands.get(j);
                    if(op1.mapIntersect(op2))
                        throw new RuntimeException("Exclusive Intersect. Property : " + this + ", Operand 1 : " + op1.property +  ", Operand 2 : " + op2.property + ", Classes 1 : " + listClasses.get(i) + ", Classes 2 : " + listClasses.get(j));
                }

        if(checkFull() && classValueWhere.means(fullClassValueWhere))
            throw new RuntimeException("Property is not fully implemented : " + this +  ", Calculated : " + fullClassValueWhere + ", Specified : " + classValueWhere);
    }
}
