package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.RecursiveExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChanges;

import java.util.*;

import static platform.base.BaseUtils.*;


public class RecursiveProperty<T extends PropertyInterface> extends ComplexIncrementProperty<RecursiveProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    private Collection<T> getInnerInterfaces() {
        return BaseUtils.merge(mapInterfaces.values(), mapIterate.keySet());
    }

    protected final Map<Interface, T> mapInterfaces;
    protected final Map<T, T> mapIterate; // старый на новый
    protected final CalcPropertyMapImplement<?, T> initial;
    protected final CalcPropertyMapImplement<?, T> step;
    
    protected final Cycle cycle;
    
    protected boolean isLogical() {
        boolean isIntegral = initial.property.getType() instanceof IntegralClass;
        assert step.property.getType() instanceof IntegralClass == isIntegral;
        return !isIntegral;
    }
    
    protected boolean isCyclePossible() {
        return cycle!=Cycle.IMPOSSIBLE;
    }

    @IdentityLazy
    public CalcProperty getConstrainedProperty() {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND G!=G' == false
        assert cycle == Cycle.NO;
        assert !isLogical();

        IntegralClass integralClass = (IntegralClass)getType();
        CalcProperty constraint = DerivedProperty.createCompare(interfaces, getImplement(), DerivedProperty.<Interface>createStatic(integralClass.div(integralClass.getSafeInfiniteValue(), 2), integralClass), Compare.GREATER).property;
        constraint.caption = ServerResourceBundle.getString("logics.property.cycle.detected", caption);
        return constraint;
    }

    public RecursiveProperty(String sID, String caption, List<Interface> interfaces, Cycle cycle, Map<Interface, T> mapInterfaces, Map<T, T> mapIterate, CalcPropertyMapImplement<?, T> initial, CalcPropertyMapImplement<?, T> step) {
        super(sID, caption, interfaces);
        this.mapInterfaces = mapInterfaces;
        this.mapIterate = mapIterate;
        this.cycle = cycle;

        Collection<T> innerInterfaces = getInnerInterfaces();

        // в initial докинем недостающие ключи
        Collection<CalcPropertyInterfaceImplement<T>> and = new ArrayList<CalcPropertyInterfaceImplement<T>>();
        for(Map.Entry<T, T> mapIt : mapIterate.entrySet())
            and.add(DerivedProperty.createCompare(Compare.EQUALS, mapIt.getKey(), mapIt.getValue()));
        initial = DerivedProperty.createAnd(innerInterfaces, initial, and);

        this.initial = initial;
        this.step = step;
    }

    protected Where getLogicalIncrementWhere(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        Result<Map<KeyExpr, KeyExpr>> mapIterate = new Result<Map<KeyExpr, KeyExpr>>();
        Map<KeyExpr, Expr> group = new HashMap<KeyExpr, Expr>();
        Map<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);

        WhereBuilder initialWhere = new WhereBuilder(); // cg(Pb, b) (gN(Pb, b) - gp(Pb, b))
        Where initialChanged = initial.mapExpr(recursiveKeys, propChanges, initialWhere).getWhere().xor(initial.mapExpr(recursiveKeys).getWhere()).and(initialWhere.toWhere());

        WhereBuilder stepWhere = new WhereBuilder(); // // cs(Pb, b) * (sN(Pb,b)-sp(Pb,b)) * f(Pb)
        Expr newStep = step.mapExpr(recursiveKeys, propChanges, stepWhere); // STEP sn(pb,b)
        Where stepChanged = newStep.getWhere().xor(step.mapExpr(recursiveKeys).getWhere()).and(stepWhere.toWhere()).and(getExpr(join(replaceValues(mapInterfaces, reverse(this.mapIterate)), recursiveKeys)).getWhere());
        Expr changedExpr = RecursiveExpr.create(mapIterate.result, ValueExpr.get(initialChanged.or(stepChanged)), newStep, isCyclePossible(), group);

        return changedExpr.getWhere();
    }

    protected Expr getSumIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        assert !isLogical();

        Result<Map<KeyExpr, KeyExpr>> mapIterate = new Result<Map<KeyExpr, KeyExpr>>();
        Map<KeyExpr, Expr> group = new HashMap<KeyExpr, Expr>();
        Map<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);

        WhereBuilder initialWhere = new WhereBuilder(); // cg(Pb, b) (gN(Pb, b) - gp(Pb, b))
        Expr initialChanged = initial.mapExpr(recursiveKeys, propChanges, initialWhere).diff(initial.mapExpr(recursiveKeys)).and(initialWhere.toWhere());

        WhereBuilder stepWhere = new WhereBuilder(); // // cs(Pb, b) * (sN(Pb,b)-sp(Pb,b)) * f(Pb)
        Expr newStep = step.mapExpr(recursiveKeys, propChanges, stepWhere); // STEP sn(pb,b)
        Expr stepChanged = newStep.diff(step.mapExpr(recursiveKeys)).and(stepWhere.toWhere()).mult(getExpr(join(replaceValues(mapInterfaces, reverse(this.mapIterate)), recursiveKeys)), (IntegralClass) getType());
        return RecursiveExpr.create(mapIterate.result, initialChanged.sum(stepChanged), newStep, isCyclePossible(), group);
    }

    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        if(isLogical() || cycle == Cycle.YES) { // если допускаются циклы, то придется все пересчитывать
            if(changedWhere!=null) changedWhere.add(getLogicalIncrementWhere(joinImplement, propChanges));
            return calculateNewExpr(joinImplement, propChanges);
        } else {
            Expr changedExpr = getSumIncrementExpr(joinImplement, propChanges);
            if(changedWhere!=null) changedWhere.add(changedExpr.getWhere());
            return changedExpr.sum(prevExpr);
        }
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        Result<Map<KeyExpr, KeyExpr>> mapIterate = new Result<Map<KeyExpr, KeyExpr>>();
        Map<KeyExpr, Expr> group = new HashMap<KeyExpr, Expr>();
        Map<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);
        return RecursiveExpr.create(mapIterate.result, initial.mapExpr(recursiveKeys, propChanges),
                step.mapExpr(recursiveKeys, propChanges), isCyclePossible(), group);
    }

    // проталкивание значений на уровне property
    protected Map<T, Expr> getRecursiveKeys(Map<Interface,? extends Expr> joinImplement, Result<Map<KeyExpr, KeyExpr>> mapKeysIterate, Map<KeyExpr, Expr> group) {
        Map<T, KeyExpr> recursiveKeys = KeyExpr.getMapKeys(getInnerInterfaces());
        
        Map<T, Expr> result = new HashMap<T, Expr>(BaseUtils.filterKeys(recursiveKeys, mapIterate.keySet())); // старые закинем как было
        for(Map.Entry<Interface, ? extends Expr> mapExpr : joinImplement.entrySet()) {
            T inner = mapInterfaces.get(mapExpr.getKey());
            if(mapExpr.getValue().isValue() && !mapIterate.containsValue(inner)) // если не итерируемый ключ
                result.put(inner, mapExpr.getValue());
            else {
                KeyExpr keyExpr = recursiveKeys.get(inner);
                result.put(inner, keyExpr);
                group.put(keyExpr, mapExpr.getValue());
            }
        }

        mapKeysIterate.set(crossJoin(join(mapIterate, recursiveKeys), recursiveKeys));
        return result;
    }

    @Override
    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
        initial.mapFillDepends(depends);
        step.mapFillDepends(depends);
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(propClasses, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && !isStored()))
            return calculateNewExpr(joinImplement, propChanges);

        assert !propClasses;
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

}
