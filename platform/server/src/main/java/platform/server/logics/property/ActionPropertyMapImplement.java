package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LAP;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.crossJoin;

public class ActionPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends ActionPropertyImplement<P, T> implements PropertyInterfaceImplement<T> {

    public ActionPropertyMapImplement(ActionProperty<P> property) {
        super(property);
    }

    public ActionPropertyMapImplement(ActionProperty<P> property, Map<P, T> mapping) {
        super(property, mapping);
    }

    public <K extends PropertyInterface> ActionPropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new ActionPropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public <L extends PropertyInterface> void mapEventAction(LogicsModule lm, CalcPropertyMapImplement<L, T> where, boolean session, boolean resolve) {
        lm.addEventAction(property, where.map(BaseUtils.reverse(mapping)), new OrderedMap<CalcPropertyInterfaceImplement<P>, Boolean>(), false, session, resolve);
    }

    public ActionPropertyObjectEntity<P> mapObjects(Map<T, ? extends PropertyObjectInterfaceEntity> mapObjects) {
        return new ActionPropertyObjectEntity<P>(property, BaseUtils.join(mapping, mapObjects));
    }

    public CalcPropertyMapImplement<?, T> mapWhereProperty() {
        return property.getWhereProperty().map(mapping);
    }

    public LAP<P> createLP(List<T> listInterfaces) {
        return new LAP<P>(property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(mapping)));
    }

    public FlowResult execute(ExecutionContext<T> context) throws SQLException {
        return property.execute(context.map(mapping));
    }

    public T mapSimpleDelete() {
        P simpleDelete = property.getSimpleDelete();
        if(simpleDelete!=null)
            return mapping.get(simpleDelete);
        return null;
    }

    public List<ActionPropertyMapImplement<?, T>> getList() {
        return DerivedProperty.mapActionImplements(mapping, property.getList());
    }
/*    public ActionPropertyMapImplement<?, T> compile() {
        return property.compile().map(mapping);
    }*/
    public boolean hasPushFor(Collection<T> context, boolean ordersNotNull) {
        return property.hasPushFor(mapping, context, ordersNotNull);
    }
    public CalcProperty getPushWhere(Collection<T> context, boolean ordersNotNull) {
        return property.getPushWhere(mapping, context, ordersNotNull);
    }
    public ActionPropertyMapImplement<?, T> pushFor(Collection<T> context, CalcPropertyMapImplement<?, T> where, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        return property.pushFor(mapping, context, where, orders, ordersNotNull);
    }
    public boolean hasFlow(ChangeFlowType... types) {
        for(ChangeFlowType type : types)
            if(property.hasFlow(type))
                return true;
        return false;
    }
    
    public ActionPropertyValueImplement<P> getValueImplement(Map<T, DataObject> mapObjects) {
        return new ActionPropertyValueImplement<P>(property, BaseUtils.join(mapping, mapObjects));
    }
}
