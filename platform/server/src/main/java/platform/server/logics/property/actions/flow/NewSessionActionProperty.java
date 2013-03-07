package platform.server.logics.property.actions.flow;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityLazy;
import platform.server.form.instance.FormInstance;
import platform.server.logics.property.*;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final boolean doApply;
    private final ImSet<SessionDataProperty> sessionUsed;
    private final ImSet<SessionDataProperty> localUsed;
    private final boolean singleApply;

    public <I extends PropertyInterface> NewSessionActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean doApply, boolean singleApply,
                                                                  ImSet<SessionDataProperty> sessionUsed, ImSet<SessionDataProperty> localUsed) {
        super(sID, caption, innerInterfaces, action);

        this.doApply = doApply;
        this.singleApply = singleApply;
        this.sessionUsed = sessionUsed;
        this.localUsed = localUsed; // именно так, потому что getDepends (used) нельзя вызывать до завершения инициализации

        finalizeInit();
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return super.getWhereProperty().mapOld();
    }

    @IdentityLazy
    private ImSet<SessionDataProperty> getUsed() {
        return CalcProperty.used(localUsed, aspectActionImplement.property.getUsedProps()).merge(sessionUsed);
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
        DataSession session = context.getSession();
        if(session.isInTransaction()) { // если в транзацкции
            session.addRecursion(aspectActionImplement.getValueImplement(context.getKeys()), getUsed(), singleApply);
            return null;
        }

        return context.override(session.createSession());
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.NEWSESSION || (!(type == ChangeFlowType.APPLY || type == ChangeFlowType.CANCEL) && super.hasFlow(type));
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException {
        if (doApply) {
            innerContext.apply();
        }

        innerContext.getSession().close();

        FormInstance<?> formInstance = context.getFormInstance();
        if (formInstance != null) {
            formInstance.refreshData();
        }
    }
}
