package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.form.stat.FormSelectTop;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class FormPropertyDataInterface<P extends PropertyInterface> {

    private final FormEntity form;
    private final ImSet<GroupObjectEntity> valueGroups;

    private final ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters; // with values shouldn't be cached
    private final FormSelectTop<P> selectTop;

    public FormPropertyDataInterface(FormEntity form, ImSet<GroupObjectEntity> valueGroups, ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters, FormSelectTop<P> selectTop) {
        this.form = form;
        this.valueGroups = valueGroups;
        this.contextFilters = contextFilters;
        this.selectTop = selectTop;
    }

    public <T extends PropertyInterface> PropertyMapImplement<?, T> getWhere(GroupObjectEntity groupObject, ImRevMap<P, T> mapValues, ImRevMap<ObjectEntity, T> mapObjects) {
        ImSet<FilterEntity> filters = form.getGroupFixedFilters(valueGroups).get(groupObject);
        if(filters == null)
            filters = SetFact.EMPTY();
        ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextGroupFilters = form.getGroupContextFilters(this.contextFilters, valueGroups).get(groupObject);
        if(contextGroupFilters == null)
            contextGroupFilters = SetFact.EMPTY();

        return groupObject.getWhereProperty(filters, contextGroupFilters, mapValues, mapObjects);
    }

    public <T extends PropertyInterface> SelectTop<T> getSelectTop(GroupObjectEntity groupObject, ImRevMap<P, T> mapValues) {
        return selectTop.mapValues(mapValues).getSelectTop(groupObject);
    }

    public <T extends PropertyInterface> ImOrderMap<PropertyInterfaceImplement<T>, Boolean> getOrders(GroupObjectEntity group, ImRevMap<ObjectEntity, T> mapObjects) {
        ImOrderMap<OrderEntity, Boolean> orders = form.getGroupOrdersList(valueGroups).get(group);
        if(orders == null)
            orders = MapFact.EMPTYORDER();

        return orders.mergeOrder(group.getOrderObjects().toOrderMap(false)).mapOrderKeys(orderEntity -> ((OrderEntity<?>)orderEntity).getImplement(mapObjects));
    }


}
