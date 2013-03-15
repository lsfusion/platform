package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

public class CaseUnionProperty extends IncrementUnionProperty {

    // immutable реализация
    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, boolean isExclusive, ImList<Case> cases) {
        super(sID, caption, interfaces);
        this.cases = cases;
        this.isExclusive = isExclusive;

        finalizeInit();
    }

    private static class OperandCase implements GetValue<Case, CalcPropertyInterfaceImplement<Interface>> {
        private final boolean caseClasses;

        private OperandCase(boolean caseClasses) {
            this.caseClasses = caseClasses;
        }

        public Case getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
            return new Case(caseClasses ? ((CalcPropertyMapImplement<?, Interface>)value).mapClassProperty() : value, value);
        }
    }
    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(sID, caption, interfaces, false, operands.reverseList().mapListValues(new OperandCase(caseClasses)));
    }

    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImCol<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(sID, caption, interfaces, true, operands.mapColValues(new OperandCase(caseClasses)).toList());
    }

    public static class Case {
        CalcPropertyInterfaceImplement<Interface> where;
        CalcPropertyInterfaceImplement<Interface> property;

        public Case(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property) {
            this.where = where;
            this.property = property;
        }

        public boolean isSimple() { // дебильновато конечно, но не хочется классы плодить пока
            return where == property;
        }
    }

    @Override
    protected ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        assert finalized;
        return getWheres().merge(getProps());
    }

    protected ImSet<CalcPropertyInterfaceImplement<Interface>> getWheres() {
        return getCases().getCol().mapMergeSetValues(new GetValue<CalcPropertyInterfaceImplement<Interface>, Case>() {
            public CalcPropertyInterfaceImplement<Interface> getMapValue(Case value) {
                return value.where;
            }});
    }
    protected ImSet<CalcPropertyInterfaceImplement<Interface>> getProps() {
        return getCases().getCol().mapMergeSetValues(new GetValue<CalcPropertyInterfaceImplement<Interface>, Case>() {
            public CalcPropertyInterfaceImplement<Interface> getMapValue(Case value) {
                return value.property;
            }
        });
    }

    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<CalcProperty> mPropValues = SetFact.mSet(); fillDepends(mPropValues, getProps());
        MSet<CalcProperty> mPropWheres = SetFact.mSet(); fillDepends(mPropWheres, getWheres());
        return SetFact.add(propChanges.getUsedDataChanges(mPropValues.immutable()), propChanges.getUsedChanges(mPropValues.immutable()));
    }

    private final boolean isExclusive;

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(Case operand : getCases()) {
            Where caseWhere;
            if(operand.isSimple()) {
                WhereBuilder operandWhere = new WhereBuilder();
                result = result.add(operand.property.mapDataChanges(change, operandWhere, propChanges));
                caseWhere = operandWhere.toWhere();
            } else {
                caseWhere = operand.where.mapExpr(change.getMapExprs(), propChanges).getWhere();
                result = result.add(operand.property.mapDataChanges(change.and(caseWhere), null, propChanges));
            }
            if(changedWhere!=null) changedWhere.add(caseWhere);

            if(!isExclusive)
                change = change.and(caseWhere.not());
        }
        return result;
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(isAbstract() && propClasses)
            return getClassTableExpr(joinImplement);

        CaseExprInterface exprCases = Expr.newCases(isExclusive);
        for(Case propCase : getCases())
            exprCases.add(propCase.where.mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, propClasses, propChanges, changedWhere));
        return exprCases.getFinal();
    }

    // вообще Case W1 E1, W2 E2, Wn En - эквивалентен Exclusive (W1 - E1, W2 AND !W1 - E2, ... )
    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // вообще инкрементальность делается следующим образом
        // Wi AND (OR(Cwi) OR CЕi) AND !OR(Wi-1) - Ei или вставлять прмежуточные (но у 1-го подхода - не надо отрезать сзади ничего, changed более релевантен)

        CaseExprInterface exprCases = Expr.newCases(isExclusive);

        Where changedUpWheres = Where.FALSE; // для не exclusive
        Where changedAllWhere = Where.FALSE; // для exclusive
        Where nullWhere = Where.FALSE; // для exclusive
        for(Case propCase : getCases()) {
            WhereBuilder changedWhereCase = new WhereBuilder(); // высчитываем Where
            Where caseWhere = propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase).getWhere();

            if(isExclusive) // интересуют только изменения этого where
                changedUpWheres = changedWhereCase.toWhere();
            else
                changedUpWheres = changedUpWheres.or(changedWhereCase.toWhere());

            WhereBuilder changedExprCase = new WhereBuilder(); // высчитываем Property
            Expr caseExpr = propCase.property.mapExpr(joinImplement, propChanges, changedExprCase);

            Where changedCaseWhere = caseWhere.and(changedUpWheres.or(changedExprCase.toWhere()));
            exprCases.add(changedCaseWhere, caseExpr);

            if(isExclusive) {
                changedAllWhere = changedAllWhere.exclOr(changedCaseWhere); // фокус в том что changedCaseWhere не особо нужен в nullWhere, но если его добавить только в changed, то prevExpr может не уйти
                nullWhere = nullWhere.exclOr(changedWhereCase.toWhere().and(propCase.where.mapExpr(joinImplement).getWhere()));
            } else {
                exprCases.add(caseWhere, prevExpr);
                if(changedWhere!=null) changedWhere.add(changedWhereCase.toWhere().or(changedExprCase.toWhere()));
            }
        }
        if(isExclusive) {
            Where changedOrWhere = changedAllWhere.or(nullWhere);
            if(changedWhere!=null) changedWhere.add(changedOrWhere);
            exprCases.add(changedOrWhere.not(), prevExpr);
        }

        return exprCases.getFinal();
    }

    @Override
    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        ActionPropertyMapImplement<?, Interface> result = null;
        for(Case propCase : getCases().reverseList()) {
            ActionPropertyMapImplement<?, Interface> editAction = propCase.property.mapEditAction(editActionSID, filterProperty);
            if (editAction != null) {
                boolean simpleCase = propCase.isSimple();
                if (result == null && simpleCase) {
                    result = editAction;
                } else {
                    CalcPropertyMapImplement<?, Interface> where = (CalcPropertyMapImplement<?, Interface>) propCase.where;
                    if(simpleCase)
                        where = (CalcPropertyMapImplement<?, Interface>) where.mapClassProperty();
                    result = DerivedProperty.createIfAction(interfaces, where, editAction, result);
                }
            }
        }
        return result;
    }

    @Override
    @IdentityLazy
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        for(Case operand : getCases())
            result.addAll(operand.property.mapChangeProps());
        return result.immutable();
    }


    private Object cases;
    private boolean caseClasses;

    private ClassWhere<Object> classValueWhere;

    @Override
    public ImSet<SessionCalcProperty> getSessionCalcDepends() {
        if(isAbstract())
            return SetFact.EMPTY();

        return super.getSessionCalcDepends();
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if(isAbstract())
            cases = ((MList<Case>)cases).immutableList();
    }

    // для постзадания
    public CaseUnionProperty(String sID, boolean isExclusive, boolean caseClasses, String caption, ImOrderSet<Interface> interfaces, ValueClass valueClass, ImMap<Interface, ValueClass> interfaceClasses) {
        super(sID, caption, interfaces);

        this.isExclusive = isExclusive;
        this.caseClasses = caseClasses;
        cases = ListFact.mList();

        classValueWhere = new ClassWhere<Object>(MapFact.<Object, ValueClass>addExcl(interfaceClasses, "value", valueClass), true);
    }

    public void addCase(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property) {
        assert !caseClasses;

        ((MList<Case>)cases).add(new Case(where, property));
    }

    public void addOperand(CalcPropertyMapImplement<?,Interface> operand) {
        assert isAbstract();

        Case addCase;
        if(caseClasses)
            addCase = new Case(operand.mapClassProperty(), operand);
        else
            addCase = new Case(operand, operand);

        ((MList<Case>)cases).add(addCase);
    }

    public ImList<Case> getCases() {
        return (ImList<Case>)cases;
    }

    public boolean isAbstract() {
        return classValueWhere != null;
    }

    public ClassWhere<Object> getClassValueWhere(boolean full) {
        if(isAbstract())
            return classValueWhere;

        return super.getClassValueWhere(full);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(isAbstract())
            return getInterfaceClasses();

        return super.getInterfaceCommonClasses(commonValue);
    }

    protected boolean checkFull() {
        return false;
    }

    public void checkExclusive() {
        assert isAbstract();

        ImList<Case> listCases = getCases();
        MList<ClassWhere<Object>> mListClasses = ListFact.<ClassWhere<Object>>mList(); // совместная обработка

        ClassWhere<Object> fullClassValueWhere = ClassWhere.FALSE();
        for(Case operand : listCases) {
            ClassWhere<Object> operandClassValueWhere = BaseUtils.immutableCast(((CalcPropertyMapImplement<?, Interface>) operand.where).mapClassWhere());
            mListClasses.add(operandClassValueWhere);

            if(operand.property instanceof CalcPropertyMapImplement)
                operandClassValueWhere = operandClassValueWhere.and(((CalcPropertyMapImplement<?, Interface>) operand.property).mapClassValueWhere());
            else { // идиотизм, но ту еще есть вопросы
                Interface operandInterface = (Interface)operand.property;
                ValueClass valueClass = operandClassValueWhere.filterKeys(SetFact.<Object>singleton(operandInterface)).getCommonParent(SetFact.<Object>singleton(operandInterface)).singleValue();
                operandClassValueWhere = operandClassValueWhere.and(new ClassWhere<Object>(operandInterface, valueClass.getUpSet()));
            }
            if(!operandClassValueWhere.means(classValueWhere))
                throw new RuntimeException("Wrong Classes. Property : " + this + ", Operand : " + operand.property +  ", Calculated : " + operandClassValueWhere + ", Specified : " + classValueWhere);

            fullClassValueWhere = fullClassValueWhere.or(operandClassValueWhere);
        }
        ImList<ClassWhere<Object>> listClasses = mListClasses.immutableList();

        if (isExclusive)
            for(int i=0;i<listCases.size();i++)
                for(int j=i+1;j<listCases.size();j++) {
                    CalcPropertyMapImplement<?, Interface> op1 = (CalcPropertyMapImplement<?, Interface>) listCases.get(i).where;
                    CalcPropertyMapImplement<?, Interface> op2 = (CalcPropertyMapImplement<?, Interface>) listCases.get(j).where;
                    if(op1.mapIntersect(op2))
                        throw new RuntimeException("Exclusive Intersect. Property : " + this + ", Operand 1 : " + op1.property +  ", Operand 2 : " + op2.property + ", Classes 1 : " + listClasses.get(i) + ", Classes 2 : " + listClasses.get(j));
                }

        if(checkFull() && classValueWhere.means(fullClassValueWhere))
            throw new RuntimeException("Property is not fully implemented : " + this +  ", Calculated : " + fullClassValueWhere + ", Specified : " + classValueWhere);
    }
}
