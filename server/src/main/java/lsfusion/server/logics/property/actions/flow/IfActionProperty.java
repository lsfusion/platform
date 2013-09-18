package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createForAction;

public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<?, PropertyInterface> ifProp;
    private final ActionPropertyMapImplement<?, PropertyInterface> trueAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> falseAction;

    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, boolean not, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction) {
        super(sID, caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.ifProp = ifProp.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
        if (!not) {
            this.trueAction = mapTrue;
            this.falseAction = mapFalse;
        } else {
            this.trueAction = mapFalse;
            this.falseAction = mapTrue;
        }

        finalizeInit();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createIfElseUProp(interfaces, ifProp,
                trueAction != null ? trueAction.mapWhereProperty() : null,
                falseAction !=null ? falseAction.mapWhereProperty() : null);
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        if (trueAction != null) {
            result = result.merge(trueAction.property);
        }
        if (falseAction != null) {
            result = result.merge(falseAction.property);
        }
        return result;
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
        ifProp.mapFillDepends(used);
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type trueType = trueAction == null ? null : trueAction.property.getSimpleRequestInputType(optimistic);
        Type falseType = falseAction == null ? null : falseAction.property.getSimpleRequestInputType(optimistic);

        if (!optimistic && (trueType == null || falseType == null)) {
            return null;
        }

        return trueType == null
               ? falseType
               : falseType == null
                 ? trueType
                 : trueType.getCompatible(falseType);
    }

    @Override
    public CustomClass getSimpleAdd() {
        return null; // пока ничего не делаем, так как на клиенте придется, "отменять" изменения
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return null; // по аналогии с верхним
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        if (readIf(context)) {
            if (trueAction != null) {
                return trueAction.execute(context);
            }
        } else {
            if (falseAction != null) {
                return falseAction.execute(context);
            }
        }
        return FlowResult.FINISH;
    }

    private boolean readIf(ExecutionContext<PropertyInterface> context) throws SQLException {
        return ifProp.read(context, context.getKeys()) != null;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return falseAction == null; // нужно разбивать на if true и if false, потом реализуем
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp.property;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(interfaces, ifProp, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, trueAction.map(mapInnerInterfaces), null, false, SetFact.<PropertyInterface>EMPTY(), false);
            }
        });
    }

}
