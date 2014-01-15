package lsfusion.server.logics.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ActionPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> implements PropertyInterfaceImplement<T> {

    public ActionProperty<P> property;
    public ImRevMap<P, T> mapping;

    public ActionPropertyMapImplement(ActionProperty<P> property) {
        this.property = property;
        mapping = MapFact.EMPTYREV();
    }

    public ActionPropertyMapImplement(ActionProperty<P> property, ImRevMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public <K extends PropertyInterface> ActionPropertyMapImplement<P, K> map(ImRevMap<T, K> remap) {
        return new ActionPropertyMapImplement<P, K>(property, mapping.join(remap));
    }

    public <L extends PropertyInterface> void mapEventAction(LogicsModule lm, CalcPropertyMapImplement<L, T> where, Event event, boolean resolve) {
        lm.addEventAction(property, where.map(mapping.reverse()), MapFact.<CalcPropertyInterfaceImplement<P>, Boolean>EMPTYORDER(), false, event, resolve);
    }

    public ActionPropertyObjectEntity<P> mapObjects(ImMap<T, ? extends PropertyObjectInterfaceEntity> mapObjects) {
        return new ActionPropertyObjectEntity<P>(property, mapping.join(mapObjects));
    }

    public CalcPropertyMapImplement<?, T> mapWhereProperty() {
        return property.getWhereProperty().map(mapping);
    }

    public LAP<P> createLP(ImOrderSet<T> listInterfaces) {
        return new LAP<P>(property, listInterfaces.mapOrder(mapping.reverse()));
    }

    public FlowResult execute(ExecutionContext<T> context) throws SQLException, SQLHandledException {
        return property.execute(context.map(mapping));
    }

    public T mapSimpleDelete() {
        P simpleDelete = property.getSimpleDelete();
        if(simpleDelete!=null)
            return mapping.get(simpleDelete);
        return null;
    }

    public ImList<ActionPropertyMapImplement<?, T>> getList() {
        return DerivedProperty.mapActionImplements(mapping, property.getList());
    }
/*    public ActionPropertyMapImplement<?, T> compile() {
        return property.compile().map(mapping);
    }*/
    public boolean hasPushFor(ImSet<T> context, boolean ordersNotNull) {
        return property.hasPushFor(mapping, context, ordersNotNull);
    }
    public CalcProperty getPushWhere(ImSet<T> context, boolean ordersNotNull) {
        return property.getPushWhere(mapping, context, ordersNotNull);
    }
    public ActionPropertyMapImplement<?, T> pushFor(ImSet<T> context, CalcPropertyMapImplement<?, T> where, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        return property.pushFor(mapping, context, where, orders, ordersNotNull);
    }
    public boolean hasFlow(ChangeFlowType... types) {
        for(ChangeFlowType type : types)
            if(property.hasFlow(type))
                return true;
        return false;
    }

    public ImSet<OldProperty> mapParseOldDepends() {
        return property.getParseOldDepends();
    }

    public ActionPropertyValueImplement<P> getValueImplement(ImMap<T, ? extends ObjectValue> mapObjects) {
        return new ActionPropertyValueImplement<P>(property, mapping.join(mapObjects));
    }
}
