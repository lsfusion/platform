package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.DataChanges;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.session.StructChanges;

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
    final CalcProperty<P> property;
    final CalcPropertyMapImplement<R,P> reverse;

    private static <P extends PropertyInterface> ImOrderSet<Interface<P>> getInterfaces(CalcProperty<P> property) {
        return property.getOrderInterfaces().mapOrderSetValues(new GetIndexValue<Interface<P>, P>() {
            public Interface<P> getMapValue(int i, P value) {
                return new Interface<P>(i, value);
            }
        });
    }

    // дебилизм из-за конструкторов
    public ShiftChangeProperty(String sID, String caption, CalcProperty<P> property, CalcPropertyMapImplement<R,P> reverse) {
        super(sID, caption, getInterfaces(property));

        this.property = property;
        this.reverse = reverse;

        finalizeInit();
    }

    public ImRevMap<P, Interface<P>> getMapInterfaces() {
        return interfaces.mapRevKeys(new GetValue<P, Interface<P>>() {
            public P getMapValue(Interface<P> value) {
                return value.mapInterface;
            }});
    }

    @IdentityInstanceLazy
    private CalcPropertyRevImplement<?, Interface<P>> getIsClassProperty() {
        return IsClassProperty.getProperty(getMapInterfaces().crossJoin(property.getInterfaceClasses(ClassType.ASSERTFULL))); // obsolete по идее
    }

    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
        depends.add((CalcProperty) getIsClassProperty().property);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getDepends());
    }

    protected Expr calculateExpr(ImMap<Interface<P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
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

    // без решения reverse'а и timeChanges не включишь этот механизм
    @Override
    public ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return SetFact.add(property.getUsedDataChanges(propChanges), ((CalcProperty<R>) reverse.property).getUsedDataChanges(propChanges), property.getUsedChanges(propChanges), reverse.property.getUsedChanges(propChanges));
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        return property.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface<P>> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        ImRevMap<P, Interface<P>> mapInterfaces = getMapInterfaces();
        PropertyChange<P> mapChange = change.map(mapInterfaces);
        ImMap<P, Expr> mapExprs = mapChange.getMapExprs();

        Where reverseWhere = reverse.mapExpr(mapExprs, propChanges).getWhere();
        Expr propertyExpr = property.getExpr(mapExprs, propChanges);
        ValueExpr shiftExpr = new ValueExpr(1, (IntegralClass) property.getType());

        return property.getDataChanges(new PropertyChange<P>(mapChange, propertyExpr.diff(shiftExpr).and(propertyExpr.compare(shiftExpr, Compare.EQUALS).not()).
                ifElse(reverseWhere, propertyExpr.sum(shiftExpr))), propChanges, changedWhere).
                add(reverse.mapJoinDataChanges(mapExprs, CaseExpr.NULL, reverseWhere, null, propChanges)); // reverse'им // .map(mapInterfaces)
    }
}
