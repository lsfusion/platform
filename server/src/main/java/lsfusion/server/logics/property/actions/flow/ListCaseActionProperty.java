package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

public abstract class ListCaseActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<UnionProperty.Interface, PropertyInterface> abstractWhere;
    protected final boolean isExclusive;

    public enum AbstractType { CASE, MULTI, LIST }

    protected final AbstractType type;

    public boolean isAbstract() {
        return abstractWhere != null;
    }

    public AbstractType getAbstractType() {
        return type;
    }


    protected void addWhereOperand(ActionPropertyMapImplement<?, PropertyInterface> action, Version version) {
        ((CaseUnionProperty) abstractWhere.property).addOperand(action.mapWhereProperty().map(abstractWhere.mapping.reverse()), version);
    }

    protected void addWhereCase(CalcPropertyInterfaceImplement<PropertyInterface> where, ActionPropertyMapImplement<?, PropertyInterface> action, Version version) {
        ImRevMap<PropertyInterface, UnionProperty.Interface> abstractMap = abstractWhere.mapping.reverse();
        ((CaseUnionProperty) abstractWhere.property).addCase(where.map(abstractMap), action.mapWhereProperty().map(abstractMap), version);
    }

    protected <I extends PropertyInterface> ListCaseActionProperty(String caption, boolean isExclusive, ImOrderSet<I> innerInterfaces) {
        super(caption, innerInterfaces.size());

        this.abstractWhere = null;
        this.type = null;
        this.isExclusive = isExclusive;
    }

    public <I extends PropertyInterface> ListCaseActionProperty(String caption, boolean isExclusive, boolean isChecked, AbstractType type, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, innerInterfaces.size());

        this.isExclusive = isExclusive;
        this.type = type;

        CaseUnionProperty.Type caseType = null;
        switch (type) {
            case CASE: caseType = CaseUnionProperty.Type.CASE; break;
            case MULTI: caseType = CaseUnionProperty.Type.MULTI; break;
            case LIST: caseType = CaseUnionProperty.Type.VALUE; break;
        }
        abstractWhere = DerivedProperty.createUnion(isExclusive, isChecked, caseType, interfaces, LogicalClass.instance, getMapInterfaces(innerInterfaces).join(mapClasses));
    }

    protected abstract CalcPropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty();

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        if(isAbstract())
            return abstractWhere;

        return calcCaseWhereProperty();
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if (isAbstract()) {
            CaseUnionProperty caseProp = (CaseUnionProperty) abstractWhere.property;
            caseProp.finalizeInit();
            try {
                caseProp.checkAbstract();
            } catch (CaseUnionProperty.NotFullyImplementedException e) {
                throw new RuntimeException("Action is not fully implemented : " + this +  ", Calculated : " + e.fullClassValueWhere + ", Specified : " + e.classValueWhere);
            }
            markRecursions(this);
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
