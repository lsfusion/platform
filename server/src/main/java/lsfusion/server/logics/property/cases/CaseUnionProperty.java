package lsfusion.server.logics.property.cases;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFListImpl;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.language.ScriptParsingException;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.drilldown.form.CaseUnionDrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class CaseUnionProperty extends IncrementUnionProperty {

    // immutable реализация
    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, boolean isExclusive, ImList<CalcCase<Interface>> cases) {
        super(caption, interfaces);
        this.cases = cases;
        this.isExclusive = isExclusive;

        finalizeInit();
    }

    @IdentityLazy
    public ImSet<Property> getImplements() {
        ImList<CalcCase<Interface>> simpleCases = getSimpleCases();
        
        MSet<Property> mResult = SetFact.mSetMax(simpleCases.size());
        for(int i=0,size=simpleCases.size();i<size;i++) {
            CalcCase<Interface> simpleCase = simpleCases.get(i);
            if (simpleCase.implement instanceof PropertyMapImplement)
                mResult.add(((PropertyMapImplement) simpleCase.implement).property);
        }
        return mResult.immutable();
    }

    private static class OperandCase implements Function<PropertyInterfaceImplement<Interface>, CalcCase<Interface>> {
        private final boolean caseClasses;

        private OperandCase(boolean caseClasses) {
            this.caseClasses = caseClasses;
        }

        public CalcCase apply(PropertyInterfaceImplement<Interface> value) {
            return new CalcCase(caseClasses ? ((PropertyMapImplement<?, Interface>)value).mapClassProperty() : value, value);
        }
    }

    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<PropertyInterfaceImplement<Interface>> operands, boolean caseClasses, boolean isExclusive, boolean toReverse) {
        this(caption, interfaces, isExclusive, (toReverse ? operands.reverseList() : operands).mapListValues(new OperandCase(caseClasses)));
    }

    public CaseUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImCol<PropertyInterfaceImplement<Interface>> operands, boolean caseClasses) {
        this(caption, interfaces, true, operands.mapColValues(new OperandCase(caseClasses)).toList());
    }

    @Override
    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        assert finalized;
        return getWheres().merge(getProps());
    }

    public ImSet<PropertyInterfaceImplement<Interface>> getWheres() {
        return getCases().getCol().mapMergeSetValues(value -> value.where);
    }
    public ImSet<PropertyInterfaceImplement<Interface>> getProps() {
        return getCases().getCol().mapMergeSetValues(value -> value.implement);
    }

    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<Property> mPropValues = SetFact.mSet(); fillDepends(mPropValues, getProps());
        MSet<Property> mPropWheres = SetFact.mSet(); fillDepends(mPropWheres, getWheres());
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

    public void checkRecursions(Set<Property> propertyMarks) {
        assert isAbstract();
        checkRecursions(SetFact.EMPTY(), null, propertyMarks);
    }

    @Override
    protected boolean checkRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        if(abstractPath.contains(this)) { // found recursion
            if(path != null)
                throw new ScriptParsingException("Property " + this + " is recursive. One of the pathes : " + path);
            path = SetFact.EMPTY();
            abstractPath = SetFact.EMPTY();
        }        
        abstractPath = abstractPath.addExcl(this);
        return super.checkRecursions(abstractPath, path, marks);
    }

    @Override
    public boolean canBeHeurChanged(boolean global) {
        for(CalcCase<Interface> operand : getCases()) // считаем where сиблингов и потом ими xor'им change
            if(operand.implement instanceof PropertyMapImplement && ((PropertyMapImplement) operand.implement).property.canBeHeurChanged(global))
                return true;
        return false;
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(CalcCase<Interface> operand : getCases()) {
            Where caseWhere;
            if(operand.isSimple()) {
                WhereBuilder operandWhere = new WhereBuilder();
                result = result.add(operand.implement.mapJoinDataChanges(change, GroupType.ASSERTSINGLE_CHANGE(), operandWhere, propChanges));
                caseWhere = operandWhere.toWhere();
            } else {
                caseWhere = operand.where.mapExpr(change.getMapExprs(), propChanges).getWhere();
                result = result.add(operand.implement.mapJoinDataChanges(change.and(caseWhere), GroupType.ASSERTSINGLE_CHANGE(), null, propChanges));
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
        ImList<CalcCase<Interface>> cases = getCases();

        // до непосредственно вычисления, для хинтов
        ImList<Pair<Expr, Expr>> caseExprs = cases.mapListValues((Function<CalcCase<Interface>, Pair<Expr, Expr>>) value -> {
            if(checkPrereadNull(value, joinImplement, calcType, propChanges))
                return new Pair<>(Expr.NULL(), Expr.NULL());
                
            return new Pair<>(
                    value.where.mapExpr(joinImplement, calcType, propChanges, changedWhere),
                    value.implement.mapExpr(joinImplement, calcType, propChanges, changedWhere));
        });

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
        ImList<Pair<Pair<Expr, Where>, Pair<Expr, Where>>> caseExprs = cases.mapListValues((Function<CalcCase<Interface>, Pair<Pair<Expr, Where>, Pair<Expr, Where>>>) propCase -> {
            if(checkPrereadNull(propCase, joinImplement, CalcType.EXPR, propChanges))
                return new Pair<>(new Pair<>(Expr.NULL(), Where.FALSE()), new Pair<>(Expr.NULL(), Where.FALSE()));

            WhereBuilder changedWhereCase = new WhereBuilder();
            WhereBuilder changedExprCase = new WhereBuilder();
            return new Pair<>(
                    new Pair<>(propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase), changedWhereCase.toWhere()),
                    new Pair<>(propCase.implement.mapExpr(joinImplement, propChanges, changedExprCase), changedExprCase.toWhere()));
        });

        int size=cases.size();
        CaseExprInterface exprCases = Expr.newCases(isExclusive, isExclusive ? size + 1 : size * 2);

        Where changedUpWheres = Where.FALSE(); // для не exclusive
        Where changedAllWhere = Where.FALSE(); // для exclusive
        Where nullWhere = Where.FALSE(); // для exclusive
        PropertyChanges prevPropChanges = getPrevPropChanges(propChanges);
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
                nullWhere = nullWhere.exclOr(changedWhereCase.and(cases.get(i).where.mapExpr(joinImplement, prevPropChanges).getWhere()));
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
    public ActionMapImplement<?, Interface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        int lastNotNullAction = 0;
        ImList<CalcCase<Interface>> cases = getCases();
        MList<ActionCase<Interface>> mActionCases = ListFact.mList();
        for(CalcCase<Interface> propCase : cases) {
            ActionMapImplement<?, Interface> eventAction = propCase.implement.mapEventAction(eventActionSID, viewProperties);
            if(isExclusive) {
                if(eventAction == null)
                    continue;                                
            } else {
                if(eventAction == null)
                    eventAction = PropertyFact.createEmptyAction();                    
                else
                    lastNotNullAction = mActionCases.size() + 1;
            }

            PropertyMapImplement<?, Interface> where;
            if(propCase.isSimple())
                where = ((PropertyMapImplement<?, Interface>) propCase.implement).mapClassProperty();
            else
                where = (PropertyMapImplement<?, Interface>) propCase.where;
            mActionCases.add(new ActionCase<>(where, eventAction));
        }
        ImList<ActionCase<Interface>> actionCases = mActionCases.immutableList();
        
        if(!isExclusive && lastNotNullAction < actionCases.size()) // optimization, cutting nulls in the end
            actionCases = actionCases.subList(0, lastNotNullAction);
            
        if(actionCases.isEmpty())
            return null;

        return PropertyFact.createCaseAction(interfaces, isExclusive, actionCases);
    }

    @Override
    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        for(CalcCase<Interface> operand : getCases())
            result.addAll(operand.implement.mapChangeProps());
        return result.immutable();
    }


    public void addImplicitCase(PropertyMapImplement<?, Interface> property, List<ResolveClassSet> signature, boolean sameNamespace, Version version) {
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

        classValueWhere = new ClassWhere<>(MapFact.addExcl(interfaceClasses, "value", valueClass), true);
    }

    public void addCase(PropertyInterfaceImplement<Interface> where, PropertyInterfaceImplement<Interface> property, Version version) {
        assert abs.type == Type.CASE;
//        
//        if(property instanceof PropertyMapImplement)
//            where = PropertyFact.createAnd(interfaces, where, ((PropertyMapImplement<?, Interface>)property).mapClassProperty());

        addCase(new ExplicitCalcCase(where, property), version);
    }

    public void addOperand(PropertyMapImplement<?, Interface> operand, List<ResolveClassSet> signature, Version version) {
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

        PropertyMapImplement<L, Interface> caseWhere = (PropertyMapImplement<L, Interface>) addCase.where;
        PropertyInterfaceImplement<Interface> caseImplement = (PropertyInterfaceImplement<Interface>)addCase.implement;
        String caseInfo = caseImplement.toString();
        checkContainsAll(caseWhere.property, caseInfo, caseWhere.mapping, caseImplement, this.toString());

        addAbstractCase(addCase, version);
    }

    public ImList<CalcCase<Interface>> getCases() {
        return (ImList<CalcCase<Interface>>)cases;
    }
    public ImList<ExplicitCalcCase<Interface>> getNFCases(Version version) {
        return BaseUtils.immutableCast(((NFList<AbstractCalcCase<Interface>>)cases).getNFList(version).filterList(element -> {
            return element instanceof ExplicitCalcCase; // only explicit cases, for backward compatibility
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

    @Override
    protected boolean isClassVirtualized(CalcClassType calcType) {
        return isAbstract(); // for optimization purposes
    }

    public ClassWhere<Object> calcClassValueWhere(CalcClassType type) {
        if(isAbstract() && !finalized) {
            assert !AlgType.useInfer;
            return classValueWhere;
        }

        return super.calcClassValueWhere(type);
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, final InferType inferType) {
        if(isAbstract())
            return new Inferred<>(classValueWhere.getCommonExClasses(interfaces)); // чтобы рекурсии не было
        
        return op(getCases().mapListValues((Function<CalcCase<Interface>, Inferred<Interface>>) aCase -> aCase.where.mapInferInterfaceClasses(ExClassSet.notNull(commonValue), inferType).and(aCase.implement.mapInferInterfaceClasses(commonValue, inferType), inferType)), true, inferType);
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        if(isAbstract())
            return false;
        return opNeedInferForValueClass(getProps(), inferType);
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
                    PropertyMapImplement<?, Interface> caseWhere = (PropertyMapImplement<?, Interface>) listCases.get(i).where;
                    for (int j = i + 1; j < listCases.size(); j++) {
                        PropertyMapImplement<?, Interface> prevCaseWhere = (PropertyMapImplement<?, Interface>) listCases.get(j).where;
                        prevCaseWhere.mapCheckExclusiveness(listCases.get(j).implement.toString(), caseWhere, listCases.get(i).implement.toString(), this.toString());
                    }
                }
            }

            if (abs.checkAllImplementations) {
                ImList<PropertyMapImplement<?, Interface>> cases = getCases().mapListValues((Function<CalcCase<Interface>, PropertyMapImplement<?, Interface>>) value -> (PropertyMapImplement<?, Interface>) value.where);

                checkAllImplementations(cases.mapListValues(new Function<PropertyMapImplement<?, Interface>, Property<PropertyInterface>>() {
                    public Property<PropertyInterface> apply(PropertyMapImplement<?, Interface> value) {
                        return (Property<PropertyInterface>) value.property;
                    }
                }), cases.mapListValues(new Function<PropertyMapImplement<?, Interface>, ImRevMap<PropertyInterface, Interface>>() {
                    public ImRevMap<PropertyInterface, Interface> apply(PropertyMapImplement<?, Interface> value) {
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
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new CaseUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.case.union}"), this, LM
        );
    }

    @Override
    public boolean aspectDebugHasAlotKeys() { // оптимизация, так как hasAlotKeys единственный кто в debug вызывает getExpr и на очень сложных свойствах это сжирает время (процентов 10 от времени старта)
        ImList<CalcCase<Interface>> simpleCases = getSimpleCases();
        for (CalcCase<Interface> aCase : simpleCases.getCol().sort(Comparator.comparingLong(o -> o.implement.mapEstComplexity()))) {
            if (aCase.implement.mapHasAlotKeys())
                return true;
        }
        if (simpleCases.size() != getCases().size())
            return super.aspectDebugHasAlotKeys(); // значит есть WHERE, то есть AND
        return false;
    }

    private ImList<CalcCase<Interface>> getSimpleCases() {
        return getCases().filterList(element -> element.isSimple() || element.isClassSimple());
    }
}

/*
public class IfUnionProperty extends IncrementUnionProperty {

    private PropertyInterfaceImplement<Interface> ifProp;
    private PropertyInterfaceImplement<Interface> trueProp;
    private PropertyInterfaceImplement<Interface> falseProp;

    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return SetFact.toSet(ifProp, trueProp, falseProp);
    }

    @Override
    @IdentityInstanceLazy
    public ActionMapImplement<?, Interface> getDefaultEditAction(String editActionSID, Property filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        ActionMapImplement<?, Interface> result = falseProp.mapEditAction(editActionSID, filterProperty);
        ActionMapImplement<?, Interface> trueAction = trueProp.mapEditAction(editActionSID, filterProperty);
        if (trueAction != null) {
            result = PropertyFact.createIfAction(interfaces, (PropertyMapImplement<?, Interface>) ifProp, trueAction, result);
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

    public IfUnionProperty(String sID, LocalizedString caption, ImOrderSet<Interface> interfaces, PropertyInterfaceImplement<Interface> ifProp, PropertyInterfaceImplement<Interface> trueProp, PropertyInterfaceImplement<Interface> falseProp) {
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

    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<Property> mPropValues = SetFact.mSet(); trueProp.mapFillDepends(mPropValues); falseProp.mapFillDepends(mPropValues);
        MSet<Property> mPropWheres = SetFact.mSet(); ifProp.mapFillDepends(mPropWheres);
        return SetFact.add(propChanges.getUsedDataChanges(mPropValues.immutable()), propChanges.getUsedChanges(mPropWheres.immutable()));
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        Where ifWhere = ifProp.mapExpr(change.getMapExprs(), propChanges).getWhere();
        return trueProp.mapDataChanges(change.and(ifWhere), changedWhere, propChanges).add(
                falseProp.mapDataChanges(change.and(ifWhere.not()), changedWhere, propChanges));
    }
}
 */
