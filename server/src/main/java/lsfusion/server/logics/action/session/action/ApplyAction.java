package lsfusion.server.logics.action.session.action;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class ApplyAction extends KeepContextAction {
    private final ActionMapImplement<?, PropertyInterface> action;
    private final Property canceled;
    private final Property applyMessage;
    private final FunctionSet<SessionDataProperty> keepSessionProperties;
    private final boolean serializable;

    public <I extends PropertyInterface> ApplyAction(BaseLogicsModule LM, ActionMapImplement<?, I> action,
                                                     LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                     FunctionSet<SessionDataProperty> keepSessionProperties, boolean serializable) {
        super(caption, innerInterfaces.size());
        this.keepSessionProperties = keepSessionProperties;
        this.serializable = serializable;

        this.action = action.map(getMapInterfaces(innerInterfaces).reverse());
        this.canceled = LM.getCanceled().property;
        this.applyMessage = LM.getApplyMessage().property;
        
        finalizeInit();
    }
    
    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        
        MList<ActionMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        if(action != null)
            actions.add(action);

        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<PropertyInterfaceImplement<PropertyInterface>, ActionMapImplement<?, PropertyInterface>>() {
                            public PropertyInterfaceImplement<PropertyInterface> getMapValue(ActionMapImplement<?, PropertyInterface> value) {
                                return value.mapCalcWhereProperty();
                            }});
        return PropertyFact.createUnion(interfaces, listWheres);
        
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        
        try {
            if (serializable)
                DBManager.pushTIL(Connection.TRANSACTION_REPEATABLE_READ);

            Result<String> rApplyMessage = new Result<>();
            boolean applied = context.apply(action == null ? SetFact.<ActionValueImplement>EMPTYORDER() : SetFact.<ActionValueImplement>singletonOrder(action.getValueImplement(context.getKeys(), context.getObjectInstances(), context.getFormAspectInstance())), keepSessionProperties, rApplyMessage);
            canceled.change(context, !applied ? true : null);
            applyMessage.change(context, rApplyMessage.result);
        } finally {
            if (serializable)
                DBManager.popTIL();
        }
        return FlowResult.FINISH;
    }

    public ImSet<Action> getDependActions() {
        ImSet<Action> result = SetFact.EMPTY();
        if (action != null) {
            result = result.merge(action.action);
        }        
        return result;
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this because events are called (and we need step out)
    }

    @Override
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.APPLY)
            return true;
        if (type == ChangeFlowType.READONLYCHANGE)
            return true;
        return super.hasFlow(type);
    }
}
