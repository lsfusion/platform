package platform.server.logics.property.actions.flow;

import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.classes.CustomClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

public abstract class ListCaseActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<UnionProperty.Interface, PropertyInterface> abstractWhere;
    protected final boolean isExclusive;

    public boolean isAbstract() {
        return abstractWhere != null;
    }

    protected void addWhereOperand(ActionPropertyMapImplement<?, PropertyInterface> action) {
        ((CaseUnionProperty) abstractWhere.property).addOperand(action.mapWhereProperty().map(abstractWhere.mapping.reverse()));
    }

    protected void addWhereCase(CalcPropertyInterfaceImplement<PropertyInterface> where, ActionPropertyMapImplement<?, PropertyInterface> action) {
        ImRevMap<PropertyInterface, UnionProperty.Interface> abstractMap = abstractWhere.mapping.reverse();
        ((CaseUnionProperty) abstractWhere.property).addCase(where.map(abstractMap), action.mapWhereProperty().map(abstractMap));
    }

    protected <I extends PropertyInterface> ListCaseActionProperty(String sID, String caption, boolean isExclusive, ImOrderSet<I> innerInterfaces) {
        super(sID, caption, innerInterfaces.size());

        this.abstractWhere = null;
        this.isExclusive = isExclusive;
    }

    public <I extends PropertyInterface> ListCaseActionProperty(String sID, String caption, boolean isExclusive, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(sID, caption, innerInterfaces.size());

        this.isExclusive = isExclusive;
        abstractWhere = DerivedProperty.createUnion(getSID() + "_case", isExclusive, interfaces, LogicalClass.instance, getMapInterfaces(innerInterfaces).join(mapClasses));
    }

    protected abstract CalcPropertyMapImplement<?, PropertyInterface> calculateWhereProperty();

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        if(isAbstract())
            return abstractWhere;

        return calculateWhereProperty();
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if(isAbstract()) {
            CaseUnionProperty caseProp = (CaseUnionProperty) abstractWhere.property;
            caseProp.finalizeInit();
            caseProp.checkAbstract();
        }
    }

    protected abstract ImList<ActionPropertyMapImplement<?, PropertyInterface>> getListActions();

    public ImSet<ActionProperty> getDependActions() {
        return getListActions().mapListValues(new GetValue<ActionProperty, ActionPropertyMapImplement<?, PropertyInterface>>() {
            public ActionProperty getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                return value.property;
            }
        }).toOrderSet().getSet();
    }
}
