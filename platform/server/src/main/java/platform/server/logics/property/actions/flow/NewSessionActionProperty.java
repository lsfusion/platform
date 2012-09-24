package platform.server.logics.property.actions.flow;

import platform.server.form.instance.FormInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final boolean doApply;
    private final BusinessLogics BL;

    public <I extends PropertyInterface> NewSessionActionProperty(String sID, String caption, List<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean doApply, BusinessLogics BL) {
        super(sID, caption, innerInterfaces, action);

        this.BL = BL;
        this.doApply = doApply;

        finalizeInit();
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return super.getWhereProperty().mapOld();
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
        return context.override(context.getSession().createSession());
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return !(type == ChangeFlowType.APPLY || type == ChangeFlowType.CANCEL) && super.hasFlow(type);
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
