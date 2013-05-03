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
import platform.server.form.entity.drilldown.CaseUnionDrillDownFormEntity;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

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
        public CalcPropertyInterfaceImplement<Interface> where;
        public CalcPropertyInterfaceImplement<Interface> property;

        public Case(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property) {
            this.where = where;
            this.property = property;
        }

        public boolean isSimple() { // дебильновато конечно, но не хочется классы плодить пока
            return where == property;
        }
    }

    @Override
    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        assert finalized;
        return getWheres().merge(getProps());
    }

    public ImSet<CalcPropertyInterfaceImplement<Interface>> getWheres() {
        return getCases().getCol().mapMergeSetValues(new GetValue<CalcPropertyInterfaceImplement<Interface>, Case>() {
            public CalcPropertyInterfaceImplement<Interface> getMapValue(Case value) {
                return value.where;
            }});
    }
    public ImSet<CalcPropertyInterfaceImplement<Interface>> getProps() {
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
    public boolean noOld() {
        return isAbstract() || super.noOld();
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

        addCase(new Case(where, property));
    }

    public void addOperand(CalcPropertyMapImplement<?,Interface> operand) {
        assert isAbstract();

        Case addCase;
        if(caseClasses)
            addCase = new Case(operand.mapClassProperty(), operand);
        else
            addCase = new Case(operand, operand);

       addCase(addCase);
    }

    public void addCase(Case addCase) {
        if (isAbstract()) {
            ClassWhere<Object> caseClassValueWhere = getCaseClassValueWhere(addCase);
            if (!caseClassValueWhere.means(classValueWhere)) {
                throw new RuntimeException("Wrong signature of implementation " + addCase.property + " (specified " + caseClassValueWhere + ") for abstract property " + this + " (expected " + classValueWhere + ")");
            }
        }

        if (isExclusive) {
            ImList<Case> listCases = getCases();

            for (int i = 0; i < listCases.size(); i++) {
                CalcPropertyMapImplement<?, Interface> op1 = (CalcPropertyMapImplement<?, Interface>) listCases.get(i).where;
                CalcPropertyMapImplement<?, Interface> op2 = (CalcPropertyMapImplement<?, Interface>) addCase.where;
                if (op1.mapIntersect(op2)) {
                    throw new RuntimeException("Signature intersection of property " + addCase.property + " (WHEN " + addCase.where +") with previosly defined implementation " + listCases.get(i).property + " (WHEN " + listCases.get(i).where +") for abstract property " + this + "\n" +
                            "Classes 1 : " + op1.mapClassWhere() + ", Classes 2 : " + op2.mapClassWhere());
                }
            }
        }

        ((MList<Case>) cases).add(addCase);
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

    public void checkAbstract() {
        if(!isAbstract())
            return;

        ClassWhere<Object> fullClassValueWhere = ClassWhere.FALSE();
        for(Case operand : getCases()) {
            fullClassValueWhere = fullClassValueWhere.or(getCaseClassValueWhere(operand));
        }

        if(checkFull() && classValueWhere.means(fullClassValueWhere))
            throw new RuntimeException("Property is not fully implemented : " + this +  ", Calculated : " + fullClassValueWhere + ", Specified : " + classValueWhere);
    }

    private ClassWhere<Object> getCaseClassValueWhere(Case propCase) {
        ClassWhere<Object> operandClassValueWhere = BaseUtils.immutableCast(((CalcPropertyMapImplement<?, Interface>) propCase.where).mapClassWhere());
        if(propCase.property instanceof CalcPropertyMapImplement)
            operandClassValueWhere = operandClassValueWhere.and(((CalcPropertyMapImplement<?, Interface>) propCase.property).mapClassValueWhere());
        else { // идиотизм, но ту еще есть вопросы
            Interface operandInterface = (Interface)propCase.property;
            ValueClass valueClass = operandClassValueWhere.filterKeys(SetFact.<Object>singleton(operandInterface)).getCommonParent(SetFact.<Object>singleton(operandInterface)).singleValue();
            operandClassValueWhere = operandClassValueWhere.and(new ClassWhere<Object>(operandInterface, valueClass.getUpSet()));
        }
        return operandClassValueWhere;
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull() && getImplement().property.isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new CaseUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.case.union"), this, BL
        );
    }
}
