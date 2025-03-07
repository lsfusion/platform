package lsfusion.server.logics.action.session.action;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.FormChangeFlowType;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

public class ApplyAction extends KeepContextAction {
    private final ActionMapImplement<?, PropertyInterface> action;
    private final Property canceled;
    private final Property applyMessage;
    private final FunctionSet<SessionDataProperty> keepSessionProperties;
    private final boolean serializable;

    public <I extends PropertyInterface> ApplyAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<?, I> action, FunctionSet<SessionDataProperty> keepSessionProperties, boolean serializable, Property canceled, Property applyMessage) {
        super(caption, innerInterfaces.size());
        this.keepSessionProperties = keepSessionProperties;
        this.serializable = serializable;

        this.action = action.map(getMapInterfaces(innerInterfaces).reverse());
        this.canceled = canceled;
        this.applyMessage = applyMessage;
        
        finalizeInit();
    }
    
    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return super.aspectChangeExtProps(recursiveAbstracts).replaceValues(true);
    }

    @Override
    public ImMap<Property, Boolean> calculateUsedExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return super.calculateUsedExtProps(recursiveAbstracts).replaceValues(true);
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        
        MList<ActionMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        if(action != null)
            actions.add(action);

        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        (Function<ActionMapImplement<?, PropertyInterface>, PropertyInterfaceImplement<PropertyInterface>>) ActionMapImplement::mapCalcWhereProperty);
        return PropertyFact.createUnion(interfaces, listWheres);
        
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        Result<String> rApplyMessage = new Result<>();
        boolean applied = context.apply(action == null ? SetFact.EMPTYORDER() : SetFact.singletonOrder(action.getValueImplement(context.getKeys(), context.getObjectInstances(), context.getFormAspectInstance())), serializable, keepSessionProperties, rApplyMessage);
        canceled.change(context, !applied ? true : null);
        applyMessage.change(context, rApplyMessage.result);
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
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore(FormChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if (type == ChangeFlowType.APPLY)
            return true;
        if (type.isManageSession())
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        ActionMapImplement<?, PropertyInterface> replacedAction = action.mapReplaceExtend(replacer, recursiveAbstracts);
        if(replacedAction == null)
            return null;

        return PropertyFact.createApplyAction(interfaces, replacedAction, keepSessionProperties, serializable, canceled, applyMessage);
    }
}
