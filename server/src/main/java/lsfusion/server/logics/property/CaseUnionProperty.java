package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityStartLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.CaseUnionDrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NFListImpl;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.property.cases.*;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.DataChanges;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.session.StructChanges;

import java.util.List;

public class CaseUnionProperty extends IncrementUnionProperty {

    // immutable реализация
    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, boolean isExclusive, ImList<CalcCase<Interface>> cases) {
        super(caption, interfaces);
        this.cases = cases;
        this.isExclusive = isExclusive;

        finalizeInit();
    }

    private static class OperandCase implements GetValue<CalcCase<Interface>, CalcPropertyInterfaceImplement<Interface>> {
        private final boolean caseClasses;

        private OperandCase(boolean caseClasses) {
            this.caseClasses = caseClasses;
        }

        public CalcCase getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
            return new CalcCase(caseClasses ? ((CalcPropertyMapImplement<?, Interface>)value).mapClassProperty() : value, value);
        }
    }

    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses, boolean isExclusive, boolean toReverse) {
        this(caption, interfaces, isExclusive, (toReverse ? operands.reverseList() : operands).mapListValues(new OperandCase(caseClasses)));
    }

    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImCol<CalcPropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(caption, interfaces, true, operands.mapColValues(new OperandCase(caseClasses)).toList());
    }

    @Override
    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        assert finalized;
        return getWheres().merge(getProps());
    }

    public ImSet<CalcPropertyInterfaceImplement<Interface>> getWheres() {
        return getCases().getCol().mapMergeSetValues(new GetValue<CalcPropertyInterfaceImplement<Interface>, CalcCase<Interface>>() {
            public CalcPropertyInterfaceImplement<Interface> getMapValue(CalcCase<Interface> value) {
                return value.where;
            }});
    }
    public ImSet<CalcPropertyInterfaceImplement<Interface>> getProps() {
        return getCases().getCol().mapMergeSetValues(new GetValue<CalcPropertyInterfaceImplement<Interface>, CalcCase<Interface>>() {
            public CalcPropertyInterfaceImplement<Interface> getMapValue(CalcCase<Interface> value) {
                return value.implement;
            }
        });
    }

    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<CalcProperty> mPropValues = SetFact.mSet(); fillDepends(mPropValues, getProps());
        MSet<CalcProperty> mPropWheres = SetFact.mSet(); fillDepends(mPropWheres, getWheres());
        return SetFact.add(propChanges.getUsedDataChanges(mPropValues.immutable()), propChanges.getUsedChanges(mPropValues.immutable()));
    }

    public enum Type { CASE, MULTI, VALUE }

    private boolean isExclusive;

    // только для abstract
    private static class AbstractInfo {
        public final boolean checkExclusiveImplementations;
        public final boolean checkAllImplementations;

        public final Type type;
        
        public AbstractInfo(boolean checkExclusiveImplementations, boolean checkAllImplementations, Type type) {
            this.checkExclusiveImplementations = checkExclusiveImplementations;
            this.checkAllImplementations = checkAllImplementations;
            this.type = type;
        }
    }
    private AbstractInfo abs;
    public Graph<CalcCase<Interface>> abstractGraph;
    
    public Type getAbstractType() {
        return abs.type;
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(CalcCase<Interface> operand : getCases()) {
            Where caseWhere;
            if(operand.isSimple()) {
                WhereBuilder operandWhere = new WhereBuilder();
                result = result.add(operand.implement.mapDataChanges(change, operandWhere, propChanges));
                caseWhere = operandWhere.toWhere();
            } else {
                caseWhere = operand.where.mapExpr(change.getMapExprs(), propChanges).getWhere();
                result = result.add(operand.implement.mapDataChanges(change.and(caseWhere), null, propChanges));
            }
            if(changedWhere!=null) changedWhere.add(caseWhere);

            if(!isExclusive)
                change = change.and(caseWhere.not());
        }
        return result;
    }

    private boolean checkPrereadNull(CalcCase<Interface> cCase, ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
       return JoinProperty.checkPrereadNull(joinImplement, true, SetFact.singleton(cCase.where), calcType, propChanges); // isExclusive ? SetFact.toSet(cCase.where, cCase.property) : SetFact.singleton(cCase.where)
    }

    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        if(isAbstract() && calcType instanceof CalcClassType)
            return getVirtualTableExpr(joinImplement, (CalcClassType) calcType);

        ImList<CalcCase<Interface>> cases = getCases();

        // до непосредственно вычисления, для хинтов
        ImList<Pair<Expr, Expr>> caseExprs = cases.mapListValues(new GetValue<Pair<Expr, Expr>, CalcCase<Interface>>() {
            public Pair<Expr, Expr> getMapValue(CalcCase<Interface> value) {
                if(checkPrereadNull(value, joinImplement, calcType, propChanges))
                    return new Pair<>(Expr.NULL, Expr.NULL);
                    
                return new Pair<>(
                        value.where.mapExpr(joinImplement, calcType, propChanges, changedWhere),
                        value.implement.mapExpr(joinImplement, calcType, propChanges, changedWhere));
            }});

        CaseExprInterface exprCases = Expr.newCases(isExclusive, caseExprs.size());
        for(Pair<Expr, Expr> caseExpr : caseExprs)
            exprCases.add(caseExpr.first.getWhere(), caseExpr.second);
        return exprCases.getFinal();
    }

    // вообще Case W1 E1, W2 E2, Wn En - эквивалентен Exclusive (W1 - E1, W2 AND !W1 - E2, ... )
    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // вообще инкрементальность делается следующим образом
        // Wi AND (OR(Cwi) OR CЕi) AND !OR(Wi-1) - Ei или вставлять прмежуточные (но у 1-го подхода - не надо отрезать сзади ничего, changed более релевантен)
        ImList<CalcCase<Interface>> cases = getCases();

        // до непосредственно вычисления, для хинтов
        ImList<Pair<Pair<Expr, Where>, Pair<Expr, Where>>> caseExprs = cases.mapListValues(new GetValue<Pair<Pair<Expr, Where>, Pair<Expr, Where>>, CalcCase<Interface>>() {
            public Pair<Pair<Expr, Where>, Pair<Expr, Where>> getMapValue(CalcCase<Interface> propCase) {
                if(checkPrereadNull(propCase, joinImplement, CalcType.EXPR, propChanges))
                    return new Pair<>(new Pair<>(Expr.NULL, Where.FALSE), new Pair<>(Expr.NULL, Where.FALSE));

                WhereBuilder changedWhereCase = new WhereBuilder();
                WhereBuilder changedExprCase = new WhereBuilder();
                return new Pair<>(
                        new Pair<>(propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase), changedWhereCase.toWhere()),
                        new Pair<>(propCase.implement.mapExpr(joinImplement, propChanges, changedExprCase), changedExprCase.toWhere()));
            }});

        int size=cases.size();
        CaseExprInterface exprCases = Expr.newCases(isExclusive, isExclusive ? size + 1 : size * 2);

        Where changedUpWheres = Where.FALSE; // для не exclusive
        Where changedAllWhere = Where.FALSE; // для exclusive
        Where nullWhere = Where.FALSE; // для exclusive
        for(int i=0;i<size;i++) {
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
    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        ActionPropertyMapImplement<?, Interface> result = null;
        for(CalcCase<Interface> propCase : getCases().reverseList()) {
            ActionPropertyMapImplement<?, Interface> editAction = propCase.implement.mapEditAction(editActionSID, filterProperty);
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
    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        for(CalcCase<Interface> operand : getCases())
            result.addAll(operand.implement.mapChangeProps());
        return result.immutable();
    }


    public void addImplicitCase(CalcPropertyMapImplement<?, Interface> property, List<ResolveClassSet> signature, boolean sameNamespace, Version version) {
        addAbstractCase(new ImplicitCalcCase<>(property, signature, sameNamespace), version);
    }

    private Object cases;
    private boolean isLast;
    private void addAbstractCase(AbstractCalcCase<Interface> aCase, Version version) {
        NFListImpl.add(isLast, (NFList<AbstractCalcCase<Interface>>) cases, aCase, version);
    }
    
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

        if(isAbstract()) {
            FinalizeResult<CalcCase<Interface>> finalize = AbstractCase.finalizeCalcCases(
                    interfaces, (NFList<AbstractCalcCase<Interface>>) cases, abs.type == Type.MULTI, abs.checkExclusiveImplementations);
            cases = finalize.cases;
            isExclusive = finalize.isExclusive;
            abstractGraph = finalize.graph;
            
            checkAbstract();
            
            abs = null;
        }
    }

    // для постзадания
    public CaseUnionProperty(boolean checkExclusiveImplementations, boolean checkAllImplementations, boolean isLast, Type type, LocalizedString caption, ImOrderSet<Interface> interfaces, ValueClass valueClass, ImMap<Interface, ValueClass> interfaceClasses) {
        super(caption, interfaces);

        abs = new AbstractInfo(checkExclusiveImplementations, checkAllImplementations, type); 

        cases = NFFact.list();
        this.isLast = isLast;

        classValueWhere = new ClassWhere<>(MapFact.<Object, ValueClass>addExcl(interfaceClasses, "value", valueClass), true);
    }

    public void addCase(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property, Version version) {
        assert abs.type == Type.CASE;
//        
//        if(property instanceof CalcPropertyMapImplement)
//            where = DerivedProperty.createAnd(interfaces, where, ((CalcPropertyMapImplement<?, Interface>)property).mapClassProperty());

        addCase(new ExplicitCalcCase(where, property), version);
    }

    public void addOperand(CalcPropertyMapImplement<?, Interface> operand, List<ResolveClassSet> signature, Version version) {
        assert isAbstract();

        ExplicitCalcCase addCase;
        if (abs.type == Type.MULTI)
            addCase = new ExplicitCalcCase(operand.mapClassProperty(), operand, signature);
        else
            addCase = new ExplicitCalcCase(operand, operand);

       addCase(addCase, version);
    }

    public <L extends PropertyInterface> void addCase(ExplicitCalcCase addCase, Version version) {
        assert isAbstract();

        CalcPropertyMapImplement<L, Interface> caseWhere = (CalcPropertyMapImplement<L, Interface>) addCase.where;
        CalcPropertyInterfaceImplement<Interface> caseImplement = (CalcPropertyInterfaceImplement<Interface>)addCase.implement;
        String caseCaption = caseImplement.toString();
        checkContainsAll(caseWhere.property, caseCaption, caseWhere.mapping, caseImplement);

        addAbstractCase(addCase, version);
    }

    public ImList<CalcCase<Interface>> getCases() {
        return (ImList<CalcCase<Interface>>)cases;
    }
    public ImList<ExplicitCalcCase<Interface>> getNFCases(Version version) {
        return BaseUtils.immutableCast(((NFList<AbstractCalcCase<Interface>>)cases).getNFList(version).filterList(new SFunctionSet<AbstractCalcCase<Interface>>() {
            public boolean contains(AbstractCalcCase<Interface> element) {
                return element instanceof ExplicitCalcCase; // only explicit cases, for backward compatibility
            }
        }));
    }

//    public ImList<ExplicitCalcCase<Interface>> getTestCases() {
//        if(cases instanceof NFList)
//            return getNFCases(Version.CURRENT);
//        else
//            return null;
//    }
//
    public boolean isAbstract() {
        return classValueWhere != null;
    }

    public ClassWhere<Object> calcClassValueWhere(CalcClassType type) {
        if(isAbstract())
            return classValueWhere;

        return super.calcClassValueWhere(type);
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, final InferType inferType) {
        if(isAbstract())
            return new Inferred<>(classValueWhere.getCommonExClasses(interfaces)); // чтобы рекурсии не было
        
        return op(getCases().mapListValues(new GetValue<Inferred<Interface>, CalcCase<Interface>>() {
            public Inferred<Interface> getMapValue(CalcCase<Interface> aCase) {
                return aCase.where.mapInferInterfaceClasses(ExClassSet.notNull(commonValue), inferType).and(aCase.implement.mapInferInterfaceClasses(commonValue, inferType), inferType);
            }}), true, inferType);
    }
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        if(isAbstract())
            return classValueWhere.getCommonExClasses(SetFact.singleton("value")).singleValue();
        return opInferValueClasses(getProps(), inferred, true, inferType);
    }

    public static class NotFullyImplementedException extends RuntimeException {
        public String fullClassValueWhere;
        public String classValueWhere;
        public NotFullyImplementedException(String msg, String full, String cur) {
            super(msg);
            this.fullClassValueWhere = full;
            this.classValueWhere = cur;
        }
    }

    public void checkAbstract() {
        if (isAbstract()) {
            if (abs.checkExclusiveImplementations) { // тут конечно она и implicit проверит заодно, но в addCase нужна final версия cases иначе с синхронизацией проблемы
                ImList<CalcCase<Interface>> listCases = getCases();

                for (int i = 0; i < listCases.size(); i++) {
                    CalcPropertyMapImplement<?, Interface> caseWhere = (CalcPropertyMapImplement<?, Interface>) listCases.get(i).where;
                    for (int j = i + 1; j < listCases.size(); j++) {
                        CalcPropertyMapImplement<?, Interface> prevCaseWhere = (CalcPropertyMapImplement<?, Interface>) listCases.get(j).where;
                        prevCaseWhere.mapCheckExclusiveness(listCases.get(j).implement.toString(), caseWhere, listCases.get(i).implement.toString());
                    }
                }
            }

            if (abs.checkAllImplementations) {
                ImList<CalcPropertyMapImplement<?, Interface>> cases = getCases().mapListValues(new GetValue<CalcPropertyMapImplement<?, Interface>, CalcCase<Interface>>() {
                    public CalcPropertyMapImplement<?, Interface> getMapValue(CalcCase<Interface> value) {
                        return (CalcPropertyMapImplement<?, Interface>) value.where;
                    }
                });

                checkAllImplementations(cases.mapListValues(new GetValue<CalcProperty<PropertyInterface>, CalcPropertyMapImplement<?, Interface>>() {
                    public CalcProperty<PropertyInterface> getMapValue(CalcPropertyMapImplement<?, Interface> value) {
                        return (CalcProperty<PropertyInterface>) value.property;
                    }
                }), cases.mapListValues(new GetValue<ImRevMap<PropertyInterface, Interface>, CalcPropertyMapImplement<?, Interface>>() {
                    public ImRevMap<PropertyInterface, Interface> getMapValue(CalcPropertyMapImplement<?, Interface> value) {
                        return (ImRevMap<PropertyInterface, Interface>) value.mapping;
                    }
                }));
            }
        }
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull() && getImplement().property.isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new CaseUnionDrillDownFormEntity(
                canonicalName, LocalizedString.create("{logics.property.drilldown.form.case.union}"), this, LM
        );
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        for (CalcCase<UnionProperty.Interface> calcCase : getCases()) {
            for (DataProperty change : calcCase.implement.mapChangeProps()) {
                if (change instanceof SessionDataProperty)
                   return true;
            }
        }
        return false;
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

    public IfUnionProperty(String sID, LocalizedString caption, ImOrderSet<Interface> interfaces, CalcPropertyInterfaceImplement<Interface> ifProp, CalcPropertyInterfaceImplement<Interface> trueProp, CalcPropertyInterfaceImplement<Interface> falseProp) {
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
