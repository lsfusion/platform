package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.CaseUnionDrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ScriptParsingException;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataChanges;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.session.StructChanges;

import static lsfusion.base.BaseUtils.capitalize;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CaseUnionProperty extends IncrementUnionProperty {

    // immutable реализация
    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, boolean isExclusive, ImList<Case> cases) {
        super(sID, caption, interfaces);
        this.cases = NFFact.finalList(cases);
        this.isExclusive = isExclusive;
        this.isChecked = false;
        this.abstractType = null;

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

    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses, boolean toReverse) {
        this(sID, caption, interfaces, false, (toReverse ? operands.reverseList() : operands).mapListValues(new OperandCase(caseClasses)));
    }

    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(sID, caption, interfaces, operands, caseClasses, true);
    }

    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses, ValueClass valueClass, ImMap<Interface, ValueClass> interfaceClasses) {
        this(sID, caption, interfaces, operands, caseClasses);

        classValueWhere = new ClassWhere<Object>(MapFact.<Object, ValueClass>addExcl(interfaceClasses, "value", valueClass), true);
    }

    public CaseUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImCol<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(sID, caption, interfaces, true, operands.mapColValues(new OperandCase(caseClasses)).toList());
    }

    public static class Case {
        public final CalcPropertyInterfaceImplement<Interface> where;
        public final CalcPropertyInterfaceImplement<Interface> property;

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

    public enum Type { CASE, MULTI, VALUE }

    private final boolean isExclusive;
    private final boolean isChecked;
    private final Type abstractType;

    public Type getAbstractType() {
        return abstractType;
    }

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

    private boolean checkPrereadNull(Case cCase, ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
       return JoinProperty.checkPrereadNull(joinImplement, true, SetFact.singleton(cCase.where), calcType, propChanges); // isExclusive ? SetFact.toSet(cCase.where, cCase.property) : SetFact.singleton(cCase.where)
    }

    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        if(isAbstract() && calcType.isClass())
            return getClassTableExpr(joinImplement, calcType);

        ImList<Case> cases = getCases();

        // до непосредственно вычисления, для хинтов
        ImList<Pair<Expr, Expr>> caseExprs = cases.mapListValues(new GetValue<Pair<Expr, Expr>, Case>() {
            public Pair<Expr, Expr> getMapValue(Case value) {
                if(checkPrereadNull(value, joinImplement, calcType, propChanges))
                    return new Pair<Expr, Expr>(Expr.NULL, Expr.NULL);
                    
                return new Pair<Expr, Expr>(
                        value.where.mapExpr(joinImplement, calcType, propChanges, changedWhere),
                        value.property.mapExpr(joinImplement, calcType, propChanges, changedWhere));
            }});

        CaseExprInterface exprCases = Expr.newCases(isExclusive);
        for(Pair<Expr, Expr> caseExpr : caseExprs)
            exprCases.add(caseExpr.first.getWhere(), caseExpr.second);
        return exprCases.getFinal();
    }

    // вообще Case W1 E1, W2 E2, Wn En - эквивалентен Exclusive (W1 - E1, W2 AND !W1 - E2, ... )
    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // вообще инкрементальность делается следующим образом
        // Wi AND (OR(Cwi) OR CЕi) AND !OR(Wi-1) - Ei или вставлять прмежуточные (но у 1-го подхода - не надо отрезать сзади ничего, changed более релевантен)
        ImList<Case> cases = getCases();

        // до непосредственно вычисления, для хинтов
        ImList<Pair<Pair<Expr, Where>, Pair<Expr, Where>>> caseExprs = cases.mapListValues(new GetValue<Pair<Pair<Expr, Where>, Pair<Expr, Where>>, Case>() {
            public Pair<Pair<Expr, Where>, Pair<Expr, Where>> getMapValue(Case propCase) {
                if(checkPrereadNull(propCase, joinImplement, CalcType.EXPR, propChanges))
                    return new Pair<Pair<Expr, Where>, Pair<Expr, Where>>(new Pair<Expr, Where>(Expr.NULL, Where.FALSE), new Pair<Expr, Where>(Expr.NULL, Where.FALSE));

                WhereBuilder changedWhereCase = new WhereBuilder();
                WhereBuilder changedExprCase = new WhereBuilder();
                return new Pair<Pair<Expr, Where>, Pair<Expr, Where>>(
                        new Pair<Expr, Where>(propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase), changedWhereCase.toWhere()),
                        new Pair<Expr, Where>(propCase.property.mapExpr(joinImplement, propChanges, changedExprCase), changedExprCase.toWhere()));
            }});

        CaseExprInterface exprCases = Expr.newCases(isExclusive);

        Where changedUpWheres = Where.FALSE; // для не exclusive
        Where changedAllWhere = Where.FALSE; // для exclusive
        Where nullWhere = Where.FALSE; // для exclusive
        for(int i=0,size=cases.size();i<size;i++) {
            Pair<Pair<Expr, Where>, Pair<Expr, Where>> pCaseExpr = caseExprs.get(i);
            Where caseWhere = pCaseExpr.first.first.getWhere();
            Where changedWhereCase = pCaseExpr.first.second;
            Expr caseExpr = pCaseExpr.second.first;
            Where changedExprCase = pCaseExpr.second.second;

            if(isExclusive) // интересуют только изменения этого where
                changedUpWheres = changedWhereCase;
            else
                changedUpWheres = changedUpWheres.or(changedWhereCase);

            Where changedCaseWhere = caseWhere.and(changedUpWheres.or(changedExprCase));
            exprCases.add(changedCaseWhere, caseExpr);

            if(isExclusive) {
                changedAllWhere = changedAllWhere.exclOr(changedCaseWhere); // фокус в том, что changedCaseWhere не особо нужен в nullWhere, но если его добавить только в changed, то prevExpr может не уйти
                nullWhere = nullWhere.exclOr(changedWhereCase.and(cases.get(i).where.mapExpr(joinImplement).getWhere()));
            } else {
                exprCases.add(caseWhere, prevExpr);
                if(changedWhere!=null) changedWhere.add(changedWhereCase.or(changedExprCase));
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


    private NFList<Case> cases;

    private ClassWhere<Object> classValueWhere;

    @Override
    public boolean noOld() {
        return isAbstract() || super.noOld();
    }
    @Override
    public ImSet<OldProperty> getParseOldDepends() {
        if(isAbstract())
            return SetFact.EMPTY();

        return super.getParseOldDepends();
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if(isAbstract())
            cases.finalizeCol();
    }

    // для постзадания
    public CaseUnionProperty(String sID, boolean isExclusive, boolean isChecked, Type type, String caption, ImOrderSet<Interface> interfaces, ValueClass valueClass, ImMap<Interface, ValueClass> interfaceClasses) {
        super(sID, caption, interfaces);

        this.isExclusive = isExclusive;
        this.isChecked = isChecked;
        this.abstractType = type;
        cases = NFFact.list();

        classValueWhere = new ClassWhere<Object>(MapFact.<Object, ValueClass>addExcl(interfaceClasses, "value", valueClass), true);
    }

    public void addCase(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property, Version version) {
        assert abstractType == Type.CASE;

        addCase(new Case(where, property), version);
    }

    public void addOperand(CalcPropertyMapImplement<?,Interface> operand, Version version) {
        assert isAbstract();

        Case addCase;
        if (abstractType == Type.MULTI)
            addCase = new Case(operand.mapClassProperty(), operand);
        else
            addCase = new Case(operand, operand);

       addCase(addCase, version);
    }

    public void addCase(Case addCase, Version version) {
        if (isAbstract()) {
            ClassWhere<Object> caseClassValueWhere = getCaseClassValueWhere(addCase);
            if (!caseClassValueWhere.means(classValueWhere, false)) {
                throw new ScriptParsingException("wrong signature of implementation " + addCase.property + " (specified " + caseClassValueWhere + ") for abstract property " + this + " (expected " + classValueWhere + ")");
            }
        }

        if (isExclusive) {
            ImList<Case> listCases = getNFCases(version);

            for (int i = 0; i < listCases.size(); i++) {
                CalcPropertyMapImplement<?, Interface> op1 = (CalcPropertyMapImplement<?, Interface>) listCases.get(i).where;
                CalcPropertyMapImplement<?, Interface> op2 = (CalcPropertyMapImplement<?, Interface>) addCase.where;
                if (op1.mapIntersect(op2)) {
                    throw new ScriptParsingException("signature intersection of property " + addCase.property + " (WHEN " + addCase.where +") with previosly defined implementation " + listCases.get(i).property + " (WHEN " + listCases.get(i).where +") for abstract property " + this + "\n" +
                            "Classes 1 : " + op1.mapClassWhere(ClassType.ASSERTFULL) + ", Classes 2 : " + op2.mapClassWhere(ClassType.ASSERTFULL));
                }
            }
        }

        cases.add(addCase, version);
    }

    public ImList<Case> getCases() {
        return cases.getList();
    }
    public ImList<Case> getNFCases(Version version) {
        return cases.getNFList(version);
    }

    public boolean isAbstract() {
        return classValueWhere != null;
    }

    public ClassWhere<Object> getClassValueWhere(ClassType type, PrevClasses prevSameClasses) {
        if(isAbstract())
            return classValueWhere;

        return super.getClassValueWhere(type, prevSameClasses);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue, PrevClasses prevSameClasses) {
        if(isAbstract())
            return getInterfaceClasses(ClassType.ASSERTFULL, prevSameClasses);

        return super.getInterfaceCommonClasses(commonValue, prevSameClasses);
    }

    protected boolean isChecked() {
        return isChecked;
    }

    public static class NotFullyImplementedException extends RuntimeException {
        public ClassWhere fullClassValueWhere;
        public ClassWhere classValueWhere;
        public NotFullyImplementedException(String msg, ClassWhere full, ClassWhere cur) {
            super(msg);
            this.fullClassValueWhere = full;
            this.classValueWhere = cur;
        }
    }

    public void checkAbstract() {
        if (isAbstract() && isChecked()) {
            ClassWhere<Object> fullClassValueWhere = ClassWhere.FALSE();
            for (Case operand : getCases()) {
                fullClassValueWhere = fullClassValueWhere.or(getCaseClassValueWhere(operand));
            }

            if (!classValueWhere.filterKeys(interfaces).means(fullClassValueWhere.filterKeys(interfaces), true)) {
                throw new NotFullyImplementedException("Property is not fully implemented : " + this +  ", Calculated : " + fullClassValueWhere + ", Specified : " + classValueWhere, fullClassValueWhere, classValueWhere);
            }
        }
    }

    private ClassWhere<Object> getCaseClassValueWhere(Case propCase) {
        ClassWhere<Object> operandClassValueWhere = BaseUtils.immutableCast(((CalcPropertyMapImplement<?, Interface>) propCase.where).mapClassWhere(ClassType.ASSERTFULL));
        if(propCase.property instanceof CalcPropertyMapImplement)
            operandClassValueWhere = operandClassValueWhere.and(((CalcPropertyMapImplement<?, Interface>) propCase.property).mapClassValueWhere(ClassType.ASSERTFULL));
        else { // идиотизм, но ту еще есть вопросы
            Interface operandInterface = (Interface)propCase.property;
            ValueClass valueClass = operandClassValueWhere.filterKeys(SetFact.<Object>singleton(operandInterface)).getCommonParent(SetFact.<Object>singleton(operandInterface)).singleValue();
            operandClassValueWhere = operandClassValueWhere.and(new ClassWhere<Object>(operandInterface, valueClass.getUpSet()));
        }
        return operandClassValueWhere;
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull() && getImplement().property.isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new CaseUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.case.union"), this, LM
        );
    }
}

/*
public class IfUnionProperty extends IncrementUnionProperty {

    private CalcPropertyInterfaceImplement<Interface> ifProp;
    private CalcPropertyInterfaceImplement<Interface> trueProp;
    private CalcPropertyInterfaceImplement<Interface> falseProp;

    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return SetFact.toSet(ifProp, trueProp, falseProp);
    }

    @Override
    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        ActionPropertyMapImplement<?, Interface> result = falseProp.mapEditAction(editActionSID, filterProperty);
        ActionPropertyMapImplement<?, Interface> trueAction = trueProp.mapEditAction(editActionSID, filterProperty);
        if (trueAction != null) {
            result = DerivedProperty.createIfAction(interfaces, (CalcPropertyMapImplement<?, Interface>) ifProp, trueAction, result);
        }
        return result;
    }

    @Override
    @IdentityLazy
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        result.addAll(trueProp.mapChangeProps());
        result.addAll(falseProp.mapChangeProps());
        return result.immutable();
    }

    public IfUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, CalcPropertyInterfaceImplement<Interface> ifProp, CalcPropertyInterfaceImplement<Interface> trueProp, CalcPropertyInterfaceImplement<Interface> falseProp) {
        super(sID, caption, interfaces);
        this.ifProp = ifProp;
        this.trueProp = trueProp;
        this.falseProp = falseProp;

        finalizeInit();
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr trueExpr = trueProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere); // до непосредственно вычисления, для хинтов
        Expr falseExpr = falseProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
        Where ifExpr = ifProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere();
        return trueExpr.ifElse(ifExpr, falseExpr);
    }

    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        WhereBuilder changedTrue = new WhereBuilder();
        Expr trueExpr = trueProp.mapExpr(joinImplement, propChanges, changedTrue);

        WhereBuilder changedFalse = new WhereBuilder();
        Expr falseExpr = falseProp.mapExpr(joinImplement, propChanges, changedFalse);

        WhereBuilder changedIf = new WhereBuilder();
        Where caseWhere = ifProp.mapExpr(joinImplement, propChanges, changedIf).getWhere();

        Where changedIfTrue = caseWhere.and(changedIf.toWhere().or(changedTrue.toWhere()));

        Where changedIfFalse = caseWhere.not().and(changedIf.toWhere().or(changedFalse.toWhere()));

        Where changedOrWhere = changedIfTrue.exclOr(changedIfFalse);
        if(changedWhere!=null) changedWhere.add(changedOrWhere);

        CaseExprInterface exprCases = Expr.newCases(true);
        exprCases.add(changedIfTrue, trueExpr);
        exprCases.add(changedIfFalse, falseExpr);
        exprCases.add(changedOrWhere, prevExpr);

        return exprCases.getFinal();
    }

    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<CalcProperty> mPropValues = SetFact.mSet(); trueProp.mapFillDepends(mPropValues); falseProp.mapFillDepends(mPropValues);
        MSet<CalcProperty> mPropWheres = SetFact.mSet(); ifProp.mapFillDepends(mPropWheres);
        return SetFact.add(propChanges.getUsedDataChanges(mPropValues.immutable()), propChanges.getUsedChanges(mPropWheres.immutable()));
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        Where ifWhere = ifProp.mapExpr(change.getMapExprs(), propChanges).getWhere();
        return trueProp.mapDataChanges(change.and(ifWhere), changedWhere, propChanges).add(
                falseProp.mapDataChanges(change.and(ifWhere.not()), changedWhere, propChanges));
    }
}
 */
