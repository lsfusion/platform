package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.reverse;
import static platform.server.logics.property.derived.DerivedProperty.createForAction;

public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<?, PropertyInterface> ifProp;
    private final ActionPropertyMapImplement<?, PropertyInterface> trueAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> falseAction;

    private final boolean ifClasses; // костыль из-за невозможности работы с ClassWhere на уровне свойств, используется в UnionProperty для генерации editActions

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, boolean not, List<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction, boolean ifClasses) {
        super(sID, caption, innerInterfaces.size());

        Map<I, PropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
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

        this.ifClasses = ifClasses;

        finalizeInit();
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createIfElseUProp(interfaces, ifProp,
                trueAction != null ? trueAction.mapWhereProperty() : null,
                falseAction !=null ? falseAction.mapWhereProperty() : null, ifClasses);
    }

    public Set<ActionProperty> getDependActions() {
        Set<ActionProperty> result = new HashSet<ActionProperty>();
        if (trueAction != null) {
            result.add(trueAction.property);
        }
        if (falseAction != null) {
            result.add(falseAction.property);
        }
        return result;
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        ifProp.mapFillDepends(result);
        result.addAll(super.getUsedProps());
        return result;
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type trueType = trueAction == null ? null : trueAction.property.getSimpleRequestInputType();
        Type falseType = falseAction == null ? null : falseAction.property.getSimpleRequestInputType();

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
        if (ifClasses) {
            return new ClassWhere<PropertyInterface>(DataObject.getMapClasses(context.getSession().getCurrentObjects(context.getKeys()))).
                    means(((CalcPropertyMapImplement<?, PropertyInterface>) ifProp).mapClassWhere());
        } else {
            return ifProp.read(context, context.getKeys()) != null;
        }
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        return falseAction == null; // нужно разбивать на if true и if false, потом реализуем
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp.property;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(Map<PropertyInterface, T> mapping, Collection<T> context, CalcPropertyMapImplement<PW, T> push, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(interfaces, ifProp, BaseUtils.toMap(interfaces), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(Collection<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, OrderedMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, Map<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, trueAction.map(mapInnerInterfaces), null, false);
            }
        });
    }

}
