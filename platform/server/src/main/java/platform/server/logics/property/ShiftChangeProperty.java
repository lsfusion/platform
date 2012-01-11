package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.classes.IntegralClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftChangeProperty<P extends PropertyInterface, R extends PropertyInterface> extends ChangeProperty<ShiftChangeProperty.Interface<P>> {

    public static class Interface<P> extends PropertyInterface<Interface<P>> {
        P mapInterface;

        public Interface(int ID, P mapInterface) {
            super(ID);
            this.mapInterface = mapInterface;
        }

        public Interface(int ID) {
            super(ID);
        }
    }

    // map этого свойства на переданное
    final Property<P> property;
    final PropertyMapImplement<R,P> reverse;

    private static <P extends PropertyInterface> List<Interface<P>> getInterfaces(Property<P> property) {
        List<Interface<P>> result = new ArrayList<Interface<P>>();
        for(P propertyInterface : property.interfaces)
            result.add(new Interface<P>(result.size(),propertyInterface));
        return result;
    }

    // дебилизм из-за конструкторов
    public ShiftChangeProperty(String sID, String caption, Property<P> property, PropertyMapImplement<R,P> reverse) {
        super(sID, caption, getInterfaces(property));

        this.property = property;
        this.reverse = reverse;
    }

    public Map<P, Interface<P>> getMapInterfaces() {
        Map<P, Interface<P>> result = new HashMap<P, Interface<P>>();
        for(Interface<P> propertyInterface : interfaces)
            result.put(propertyInterface.mapInterface,propertyInterface);
        return result;
    }

    @IdentityLazy
    private Map<Interface<P>, ValueClass> getInterfaceClasses() {
        return BaseUtils.crossJoin(getMapInterfaces(), property.getMapClasses());
    }

    protected PropertyChanges calculateUsedChanges(PropertyChanges propChanges) {
        return ClassProperty.getIsClassUsed(getInterfaceClasses(), propChanges);
    }

    protected Expr calculateExpr(Map<Interface<P>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.TRUE.and(ClassProperty.getIsClassWhere(getInterfaceClasses(), joinImplement, propChanges, changedWhere));
//        return ((IntegralClass) property.getType()).getActionExpr().and(classWhere);

/*          слишком сложное выполнение
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        WhereBuilder dataChangesWhere = new WhereBuilder();
        property.getDataChanges(new PropertyChange<P>(mapKeys, property.changeExpr, Where.TRUE), dataChangesWhere, modifier); // получаем dataChangesWhere для каких keyExpr'ов можно менять
        Expr resultExpr = GroupExpr.create(mapKeys, ValueExpr.TRUE, dataChangesWhere.toWhere(), true, BaseUtils.join(getMapInterfaces(), joinImplement));
        if(changedWhere!=null)
            changedWhere.add(resultExpr.getWhere());
        return resultExpr;*/
    }

    // без решения reverse'а и timeChanges не включишь этот механизм
    @Override
    public PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        return property.getUsedDataChanges(propChanges).add(reverse.property.getUsedDataChanges(propChanges)).add(property.getUsedChanges(propChanges)).add(reverse.property.getUsedChanges(propChanges));
    }

    @Override
    protected MapDataChanges<Interface<P>> calculateDataChanges(PropertyChange<Interface<P>> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        Map<P, Interface<P>> mapInterfaces = getMapInterfaces();
        Map<P, KeyExpr> mapKeys = BaseUtils.join(mapInterfaces, change.mapKeys);

        Where reverseWhere = reverse.mapExpr(mapKeys, propChanges).getWhere();
        Expr propertyExpr = property.getExpr(mapKeys, propChanges);
        ValueExpr shiftExpr = new ValueExpr(1, (IntegralClass) property.getType());

        return property.getDataChanges(new PropertyChange<P>(mapKeys, propertyExpr.sum(shiftExpr.scale(-1)).and(propertyExpr.compare(shiftExpr, Compare.EQUALS).not()).
            ifElse(reverseWhere,propertyExpr.sum(shiftExpr)), change.where), propChanges, changedWhere).
                add(reverse.mapJoinDataChanges(mapKeys, CaseExpr.NULL, reverseWhere, null, propChanges)).map(mapInterfaces); // reverse'им
    }
}
