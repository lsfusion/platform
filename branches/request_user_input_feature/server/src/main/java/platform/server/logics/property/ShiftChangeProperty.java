package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.classes.IntegralClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

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

        finalizeInit();
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

    @IdentityLazy
    private PropertyImplement<?, Interface<P>> getIsClassProperty() {
        return IsClassProperty.getProperty(getInterfaceClasses());
    }

    protected void fillDepends(Set<Property> depends, boolean events) {
        depends.add(getIsClassProperty().property);
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return propChanges.getUsedChanges(getDepends());
    }

    protected Expr calculateExpr(Map<Interface<P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.TRUE.and(getIsClassProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere());
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

    public Set<Property> getDataChangeProps() {
        return BaseUtils.toSet((Property)property, (Property)reverse.property);
    }

    // без решения reverse'а и timeChanges не включишь этот механизм
    @Override
    public QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return QuickSet.add(property.getUsedDataChanges(propChanges), reverse.property.getUsedDataChanges(propChanges), property.getUsedChanges(propChanges), reverse.property.getUsedChanges(propChanges));
    }

    @Override
    protected MapDataChanges<Interface<P>> calculateDataChanges(PropertyChange<Interface<P>> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        Map<P, Interface<P>> mapInterfaces = getMapInterfaces();
        PropertyChange<P> mapChange = change.map(mapInterfaces);
        Map<P, Expr> mapExprs = mapChange.getMapExprs();

        Where reverseWhere = reverse.mapExpr(mapExprs, propChanges).getWhere();
        Expr propertyExpr = property.getExpr(mapExprs, propChanges);
        ValueExpr shiftExpr = new ValueExpr(1, (IntegralClass) property.getType());

        return property.getDataChanges(new PropertyChange<P>(mapChange, propertyExpr.diff(shiftExpr).and(propertyExpr.compare(shiftExpr, Compare.EQUALS).not()).
            ifElse(reverseWhere,propertyExpr.sum(shiftExpr))), propChanges, changedWhere).
                add(reverse.mapJoinDataChanges(mapExprs, CaseExpr.NULL, reverseWhere, null, propChanges)).map(mapInterfaces); // reverse'им
    }
}
