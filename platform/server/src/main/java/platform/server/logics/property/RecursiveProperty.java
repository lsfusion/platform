package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.RecursiveExpr;
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

    private final Map<Interface, T> mapInterfaces;
    private final Map<T, T> mapIterate; // старый на новый
    private final PropertyInterfaceImplement<T> initial;
    private final PropertyInterfaceImplement<T> step;

    @IdentityLazy
    public Property getConstrainedProperty() {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND G!=G' == false
        Property constraint = DerivedProperty.createCompare(interfaces, getImplement(), DerivedProperty.<Interface>createStatic(RecursiveExpr.maxvalue/2, RecursiveExpr.type), Compare.GREATER).property;
        constraint.caption = ServerResourceBundle.getString("logics.property.cycle.detected", caption);
        constraint.setConstraint(false);
        return constraint;
    }


    public RecursiveProperty(String sID, String caption, List<Interface> interfaces, Map<Interface, T> mapInterfaces, Map<T, T> mapIterate, PropertyInterfaceImplement<T> initial, PropertyInterfaceImplement<T> step) {
        super(sID, caption, interfaces);
        this.mapInterfaces = mapInterfaces;
        this.mapIterate = mapIterate;

        // приведем к работе с числами
        Collection<T> innerInterfaces = getInnerInterfaces();
        PropertyMapImplement<?, T> one = DerivedProperty.createStatic(1, RecursiveExpr.type);

        Collection<PropertyInterfaceImplement<T>> and = new ArrayList<PropertyInterfaceImplement<T>>();
        and.add(initial);
        for(Map.Entry<T, T> mapIt : mapIterate.entrySet())
            and.add(DerivedProperty.createCompare(Compare.EQUALS, mapIt.getKey(), mapIt.getValue()));
        this.initial = DerivedProperty.createAnd(innerInterfaces, one, and);
        this.step = DerivedProperty.createAnd(innerInterfaces, one, step);
    }

    // если нужна инкрементность
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Result<Map<KeyExpr, KeyExpr>> mapIterate = new Result<Map<KeyExpr, KeyExpr>>();
        Result<Map<KeyExpr, ? extends Expr>> group = new Result<Map<KeyExpr, ? extends Expr>>();
        Map<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);

        WhereBuilder initialWhere = new WhereBuilder(); // cg(Pb, b) (gN(Pb, b) - gp(Pb, b))
        Expr initialChanged = initial.mapExpr(recursiveKeys, propChanges, initialWhere).diff(initial.mapExpr(recursiveKeys)).and(initialWhere.toWhere());

        WhereBuilder stepWhere = new WhereBuilder(); // // cs(Pb, b) * (sN(Pb,b)-sp(Pb,b)) * f(Pb)
        Expr newStep = step.mapExpr(recursiveKeys, propChanges, stepWhere); // STEP sn(pb,b)
        Expr stepChanged = newStep.diff(step.mapExpr(recursiveKeys)).and(stepWhere.toWhere()).mult(getExpr(join(replaceValues(mapInterfaces, reverse(this.mapIterate)), recursiveKeys)), RecursiveExpr.type);
        Expr changedExpr = RecursiveExpr.create(mapIterate.result, initialChanged.sum(stepChanged), newStep, group.result);

        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere());
        return changedExpr.sum(prevExpr);
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        Result<Map<KeyExpr, KeyExpr>> mapIterate = new Result<Map<KeyExpr, KeyExpr>>();
        Result<Map<KeyExpr, ? extends Expr>> group = new Result<Map<KeyExpr, ? extends Expr>>();
        Map<T, Expr> recursiveKeys = getRecursiveKeys(joinImplement, mapIterate, group);
        return RecursiveExpr.create(mapIterate.result, initial.mapExpr(recursiveKeys, propChanges),
                step.mapExpr(recursiveKeys, propChanges), group.result);
    }

    // проталкивание значений на уровне property
    private Map<T, Expr> getRecursiveKeys(Map<Interface,? extends Expr> joinImplement, Result<Map<KeyExpr, KeyExpr>> mapKeysIterate, Result<Map<KeyExpr, ? extends Expr>> group) {
        Map<T, KeyExpr> recursiveKeys = KeyExpr.getMapKeys(getInnerInterfaces());
        mapKeysIterate.set(crossJoin(join(mapIterate, recursiveKeys), recursiveKeys));
        group.set(crossJoin(join(mapInterfaces, recursiveKeys), joinImplement));
        return BaseUtils.<Map<T, Expr>>immutableCast(recursiveKeys);
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        initial.mapFillDepends(depends);
        step.mapFillDepends(depends);
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {

        if(!hasChanges(propChanges) || (changedWhere==null && !isStored()))
            return calculateNewExpr(joinImplement, propChanges);

        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

}
