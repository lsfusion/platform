package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.form.instance.FormInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final boolean doApply;
    private final BusinessLogics BL;
    private final Set<SessionDataProperty> sessionUsed;
    private final Set<SessionDataProperty> localUsed;
    private final boolean singleApply;

    public <I extends PropertyInterface> NewSessionActionProperty(String sID, String caption, List<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean doApply, boolean singleApply, 
                                                                  Set<SessionDataProperty> sessionUsed, Set<SessionDataProperty> localUsed, BusinessLogics BL) {
        super(sID, caption, innerInterfaces, action);

        this.BL = BL;
        this.doApply = doApply;
        this.singleApply = singleApply;
        this.sessionUsed = sessionUsed;
        this.localUsed = localUsed; // именно так, потому что getDepends (used) нельзя вызывать до завершения инициализации

        finalizeInit();
    }

    @Override
    protected PropsNewSession aspectChangeExtProps() {
        return super.aspectChangeExtProps().wrapNewSession();
    }

    @Override
    public PropsNewSession aspectUsedExtProps() {
        return super.aspectChangeExtProps().wrapNewSession();
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return super.getWhereProperty().mapOld();
    }

    @IdentityLazy
    private Set<SessionDataProperty> getUsed() {
        return BaseUtils.mergeSet(CalcProperty.used(localUsed, aspectActionImplement.property.getUsedProps()), sessionUsed);
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
            innerContext.apply(BL);
        }

        innerContext.getSession().close();

        FormInstance<?> formInstance = context.getFormInstance();
        if (formInstance != null) {
            formInstance.refreshData();
        }
    }
}
