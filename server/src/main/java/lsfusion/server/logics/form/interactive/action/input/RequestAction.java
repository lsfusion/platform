package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

public class RequestAction extends KeepContextAction {
    
    private final ActionMapImplement<?, PropertyInterface> requestAction;
    private final ActionMapImplement<?, PropertyInterface> doAction;
    private final ActionMapImplement<?, PropertyInterface> elseAction;

    public <I extends PropertyInterface> RequestAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<?, I> requestAction,
                                                       ActionMapImplement<?, I> doAction, ActionMapImplement<?, I> elseAction) {
        super(caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.requestAction = requestAction.map(mapInterfaces);
        this.doAction = doAction != null ? doAction.map(mapInterfaces) : null;
        this.elseAction = elseAction != null ? elseAction.map(mapInterfaces) : null;

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {

        MList<ActionMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        actions.add(requestAction);
        if(doAction != null)
            actions.add(doAction);
        if(elseAction != null)
            actions.add(elseAction);

        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        (Function<ActionMapImplement<?, PropertyInterface>, PropertyInterfaceImplement<PropertyInterface>>) ActionMapImplement::mapCalcWhereProperty);
        return PropertyFact.createUnion(interfaces, listWheres);
    }


    public ImSet<Action> getDependActions() {
        ImSet<Action> result = SetFact.EMPTY();
        result = result.merge(requestAction.action);

        if (doAction != null) {
            result = result.merge(doAction.action);
        }
        if (elseAction != null) {
            result = result.merge(elseAction.action);
        }
        return result;
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer) {
        ActionMapImplement<?, PropertyInterface> replacedRequestAction = requestAction.mapReplaceExtend(replacer);
        ActionMapImplement<?, PropertyInterface> replacedDoAction = doAction != null ? doAction.mapReplaceExtend(replacer) : null;
        ActionMapImplement<?, PropertyInterface> replacedElseAction = elseAction != null ? elseAction.mapReplaceExtend(replacer) : null;
        if(replacedRequestAction == null && replacedDoAction == null && replacedElseAction == null)
            return null;

        if(replacedRequestAction == null)
            replacedRequestAction = requestAction;
        if(replacedDoAction == null)
            replacedDoAction = doAction;
        if(replacedElseAction == null)
            replacedElseAction = elseAction;
        return PropertyFact.createRequestAction(interfaces, replacedRequestAction, replacedDoAction, replacedElseAction);
    }

    // there is a hack when doAction is set empty instead of null
    private boolean hasDoOrElseAction() {
        return doAction != null && !doAction.getList().isEmpty() || elseAction != null;        
    }

    @Override
    public FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        if (!context.isRequestPushed()) {
            FlowResult result = requestAction.execute(!context.hasMoreSessionUsages && hasDoOrElseAction() ? context.override(true) : context);
            if (result != FlowResult.FINISH)
                return result;
        }

        ActionMapImplement<?, PropertyInterface> execAction = !context.isRequestCanceled() ? doAction : elseAction;
        if(execAction != null)
            return context.popRequest(() -> execAction.execute(context));
        
        return FlowResult.FINISH;
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        return requestAction.mapAsyncEventExec(optimistic, recursive);
    }

    public ActionMapImplement<?, PropertyInterface> getDoAction() {
        return doAction;
    }
}

