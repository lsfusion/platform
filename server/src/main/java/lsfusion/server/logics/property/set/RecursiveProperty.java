package lsfusion.server.logics.property.set;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.form.filter.user.Compare;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.RecursiveExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;


public class RecursiveProperty<T extends PropertyInterface> extends ComplexIncrementProperty<RecursiveProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }});
    }

    private ImSet<T> getInnerInterfaces() {
        return mapInterfaces.valuesSet().merge(mapIterate.keys());
    }

    protected final ImRevMap<Interface, T> mapInterfaces;
    protected final ImRevMap<T, T> mapIterate; // старый на новый
    protected final PropertyMapImplement<?, T> initial;
    protected final PropertyMapImplement<?, T> step;
    
    protected final Cycle cycle;
    
    protected boolean isLogical() {
        boolean isIntegral = initial.property.getType() instanceof IntegralClass;
        assert step.property.getType() instanceof IntegralClass == isIntegral;
        return !isIntegral;
    }
    
    protected boolean isCyclePossible() {
        return cycle!=Cycle.IMPOSSIBLE;
    }

    @IdentityInstanceLazy
    public Property getConstrainedProperty() {
        assert cycle == Cycle.NO;
        assert !isLogical();

        IntegralClass<?> integralClass = (IntegralClass)getType();
        Property constraint = PropertyFact.createCompare(interfaces, getImplement(), PropertyFact.<Interface>createStatic(integralClass.div(integralClass.getSafeInfiniteValue(), 2), integralClass), Compare.GREATER).property;
        constraint.caption = LocalizedString.createFormatted("{logics.property.cycle.detected}", caption);
        return constraint;
    }

    public RecursiveProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, Cycle cycle, ImRevMap<Interface, T> mapInterfaces, ImRevMap<T, T> mapIterate, PropertyMapImplement<?, T> initial, PropertyMapImplement<?, T> step) {
        super(caption, interfaces);
        this.mapInterfaces = mapInterfaces;
        this.mapIterate = mapIterate;
        this.cycle = cycle;

        ImSet<T> innerInterfaces = getInnerInterfaces();

        // в initial докинем недостающие ключи
        ImCol<PropertyInterfaceImplement<T>> and = mapIterate.mapColValues(new GetKeyValue<PropertyInterfaceImplement<T>, T, T>() {
            public PropertyInterfaceImplement<T> getMapValue(T key, T value) {
                return PropertyFact.createCompare(Compare.EQUALS, key, value);
            }});
        initial = PropertyFact.createAnd(innerInterfaces, initial, and);

        this.initial = initial;
        this.step = step;
    }

    protected Where getLogicalIncrementWhere(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        Result<ImRevMap<KeyExpr, KeyExpr>> mapIterate = new Result<>();
        Result<ImMap<KeyExpr, Expr>> group = new Result<>();
        ImMap<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);
        
        if(checkPrereadNull(recursiveKeys, CalcType.EXPR, propChanges))
            return Where.FALSE;

        WhereBuilder initialWhere = new WhereBuilder(); // cg(Pb, b) (gN(Pb, b) - gp(Pb, b))
        Where initialChanged = initial.mapExpr(recursiveKeys, propChanges, initialWhere).getWhere().xor(initial.mapExpr(recursiveKeys).getWhere()).and(initialWhere.toWhere());

        WhereBuilder stepWhere = new WhereBuilder(); // // cs(Pb, b) * (sN(Pb,b)-sp(Pb,b)) * f(Pb)
        Expr newStep = step.mapExpr(recursiveKeys, propChanges, stepWhere); // STEP sn(pb,b)
        Where stepChanged = newStep.getWhere().xor(step.mapExpr(recursiveKeys).getWhere()).and(stepWhere.toWhere()).and(getExpr(mapInterfaces.replaceValues(this.mapIterate.reverse()).join(recursiveKeys)).getWhere());
        Expr changedExpr = RecursiveExpr.create(mapIterate.result, ValueExpr.get(initialChanged.or(stepChanged)), ValueExpr.get(newStep.getWhere()), isCyclePossible(), group.result);

        return changedExpr.getWhere();
    }

    protected Expr getSumIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        assert !isLogical();

        Result<ImRevMap<KeyExpr, KeyExpr>> mapIterate = new Result<>();
        Result<ImMap<KeyExpr, Expr>> group = new Result<>();
        ImMap<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);

        if(checkPrereadNull(recursiveKeys, CalcType.EXPR, propChanges))
            return Expr.NULL;

        WhereBuilder initialWhere = new WhereBuilder(); // cg(Pb, b) (gN(Pb, b) - gp(Pb, b))
        Expr initialChanged = initial.mapExpr(recursiveKeys, propChanges, initialWhere).diff(initial.mapExpr(recursiveKeys)).and(initialWhere.toWhere());

        WhereBuilder stepWhere = new WhereBuilder(); // // cs(Pb, b) * (sN(Pb,b)-sp(Pb,b)) * f(Pb)
        Expr newStep = step.mapExpr(recursiveKeys, propChanges, stepWhere); // STEP sn(pb,b)
        Expr stepChanged = newStep.diff(step.mapExpr(recursiveKeys)).and(stepWhere.toWhere()).mult(getExpr(mapInterfaces.replaceValues(this.mapIterate.reverse()).join(recursiveKeys)), (IntegralClass) getType());
        return RecursiveExpr.create(mapIterate.result, initialChanged.sum(stepChanged), newStep, isCyclePossible(), group.result);
    }

    private boolean checkPrereadNull(ImMap<T, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
        return JoinProperty.checkPrereadNull(joinImplement, step.property.isNotNull(calcType.getAlgInfo()), SetFact.singleton((PropertyInterfaceImplement<T>)initial), calcType, propChanges); // isExclusive ? SetFact.toSet(cCase.where, cCase.property) : SetFact.singleton(cCase.where)
    }

    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        if(isLogical() || cycle == Cycle.YES) { // если допускаются циклы, то придется все пересчитывать
            if(changedWhere!=null) changedWhere.add(getLogicalIncrementWhere(joinImplement, propChanges));
            return calculateNewExpr(joinImplement, CalcType.EXPR, propChanges);
        } else {
            Expr changedExpr = getSumIncrementExpr(joinImplement, propChanges);
            if(changedWhere!=null) changedWhere.add(changedExpr.getWhere());
            return changedExpr.sum(prevExpr);
        }
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges) {
        Result<ImRevMap<KeyExpr, KeyExpr>> mapIterate = new Result<>();
        Result<ImMap<KeyExpr, Expr>> group = new Result<>();
        ImMap<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);

        if(checkPrereadNull(recursiveKeys, CalcType.EXPR, propChanges))
            return Expr.NULL;

        return RecursiveExpr.create(mapIterate.result, initial.mapExpr(recursiveKeys, calcType, propChanges, null),
                step.mapExpr(recursiveKeys, calcType, propChanges, null), isCyclePossible(), group.result);
    }

    // проталкивание значений на уровне property
    protected ImMap<T, Expr> getRecursiveKeys(ImMap<Interface, ? extends Expr> joinImplement, Result<ImRevMap<KeyExpr, KeyExpr>> mapKeysIterate, Result<ImMap<KeyExpr, Expr>> group) {
        ImRevMap<T, KeyExpr> recursiveKeys = KeyExpr.getMapKeys(getInnerInterfaces());

        MExclMap<KeyExpr,Expr> mGroup = MapFact.mExclMapMax(joinImplement.size());
        ImValueMap<Interface, Expr> mvResult = joinImplement.mapItValues(); // есть mutable состояние
        for(int i=0,size=joinImplement.size();i<size;i++) {
            T inner = mapInterfaces.get(joinImplement.getKey(i));
            Expr expr = joinImplement.getValue(i);
            if(expr.isValue() && !mapIterate.containsValue(inner)) // если не итерируемый ключ
                mvResult.mapValue(i, expr);
            else {
                KeyExpr keyExpr = recursiveKeys.get(inner);
                mvResult.mapValue(i, keyExpr);
                mGroup.exclAdd(keyExpr, expr);
            }
        }

        mapKeysIterate.set(mapIterate.join(recursiveKeys).crossJoin(recursiveKeys));
        group.set(mGroup.immutable());
        return MapFact.override(recursiveKeys.filterInclRev(mapIterate.keys()), mapInterfaces.crossJoin(mvResult.immutableValue())); // старые закинем как было
    }

    @Override
    protected void fillDepends(MSet<Property> depends, boolean events) {
        initial.mapFillDepends(depends);
        step.mapFillDepends(depends);
    }

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(calcType, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && !isStored()))
            return calculateNewExpr(joinImplement, calcType, propChanges);

        assert calcType.isExpr();
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        return inferInnerInterfaceClasses(commonValue, inferType).map(mapInterfaces.reverse());
    }
    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return inferInnerValueClass(mapInterfaces.crossJoin(inferred), inferType);
    }

    private Inferred<T> inferInnerInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        Inferred<T> initialClasses = this.initial.mapInferInterfaceClasses(commonValue, inferType);

        // вообще рекурсию надо запускать как в RecursiveExpr, но пока смысла нет
        // remove - нужен как обратное действие добавлению недостающих ключей в конструкторе
        Inferred<T> iterInitialClasses = initialClasses.remove(mapIterate.keys()).map(mapInterfaces.valuesSet().removeIncl(mapIterate.valuesSet()).toRevMap().addRevExcl(mapIterate.reverse()));
        Inferred<T> stepClasses = step.mapInferInterfaceClasses(commonValue, inferType).and(iterInitialClasses, inferType).applyCompared(mapIterate.keys(), inferType);
        return stepClasses.remove(mapIterate.keys()).or( // без старых
//                stepClasses.keep(mapIterate.keys()).map(mapIterate), inferType).or( // старые отображенные на новые
                initialClasses, inferType); // начальные
    }
    private ExClassSet inferInnerValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.op(step.mapInferValueClass(inferred.addExcl(mapIterate.join(inferred)), inferType), initial.mapInferValueClass(inferred, inferType), true);
    }
}
