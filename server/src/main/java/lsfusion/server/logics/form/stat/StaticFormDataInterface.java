package lsfusion.server.logics.form.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.CompareEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;

import java.sql.SQLException;

public class StaticFormDataInterface extends AbstractFormDataInterface {

    protected final FormEntity form;
    protected final ImMap<ObjectEntity, ? extends ObjectValue> mapObjects;

    protected final ExecutionContext<?> context;
    
    protected final ImSet<ContextFilterInstance> contextFilters; // with values shouldn't be cached

    public StaticFormDataInterface(final FormEntity form, final ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, final ExecutionContext<?> context, ImSet<ContextFilterInstance> contextFilters) {
        this.form = form;
        this.mapObjects = mapObjects;
        this.context = context;
        
        this.contextFilters = contextFilters;
    }

    @Override
    public FormEntity getFormEntity() {
        return form;
    }

    @Override
    public BaseClass getBaseClass() {
        return context.getBL().LM.baseClass;
    }

    @Override
    public QueryEnvironment getQueryEnv() {
        return context.getQueryEnv();
    }

    @Override
    public DataSession getSession() {
        return context.getSession();
    }

    @Override
    public Modifier getModifier() {
        return context.getModifier();
    }

    @Override
    public ImOrderMap<CompareEntity, Boolean> getOrders(GroupObjectEntity groupObjectEntity, ImSet<GroupObjectEntity> valueGroups) {
        ImOrderMap<OrderEntity, Boolean> orders = form.getGroupOrdersList(valueGroups).get(groupObjectEntity);
        if(orders == null)
            orders = MapFact.EMPTYORDER();
        return BaseUtils.immutableCast(orders);
    }
    
    @Override
    public Where getWhere(GroupObjectEntity groupObjectEntity, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
        ImSet<FilterEntity> filters = form.getGroupFixedFilters(valueGroups).get(groupObjectEntity);
        if(filters == null)
            filters = SetFact.EMPTY();
        ImSet<ContextFilterInstance> contextGroupFilters = form.getGroupFilters(this.contextFilters, valueGroups).get(groupObjectEntity);
        if(contextGroupFilters == null)
            contextGroupFilters = SetFact.EMPTY();
        return groupObjectEntity.getWhere(mapExprs, context.getModifier(), SetFact.addExclSet(filters, contextGroupFilters));
    }

    @Override
    protected ObjectValue getValueObject(ObjectEntity object) {
        return mapObjects.get(object);
    }

    @Override
    protected ImSet<ObjectEntity> getValueObjects() {
        return mapObjects.keys();
    }
}
