package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.CompareEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class FormPropertyDataInterface<P extends PropertyInterface> {

    public final FormEntity form;
    protected final ImSet<GroupObjectEntity> valueGroups;

    protected final ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters; // with values shouldn't be cached

    public <T extends PropertyInterface> PropertyMapImplement<?, T> getWhere(GroupObjectEntity groupObject, ImRevMap<P, T> mapValues, ImRevMap<ObjectEntity, T> mapObjects) {
        ImSet<FilterEntity> filters = form.getGroupFixedFilters(valueGroups).get(groupObject);
        if(filters == null)
            filters = SetFact.EMPTY();
        ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextGroupFilters = form.getGroupContextFilters(this.contextFilters, valueGroups).get(groupObject);
        if(contextGroupFilters == null)
            contextGroupFilters = SetFact.EMPTY();

        return groupObject.getWhereProperty(filters, contextGroupFilters, mapValues, mapObjects);
    }

    public <T> ImOrderMap<PropertyInterfaceImplement<>, Boolean> getOrders(GroupObjectEntity group) {
        ImOrderMap<OrderEntity, Boolean> orders = form.getGroupOrdersList(valueGroups).get(group);
        if(orders == null)
            orders = MapFact.EMPTYORDER();

        orders = orders.mergeOrder(group.getOrderObjects().toOrderMap(false));



        return orders;
    }


}
