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
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
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
        this.doAction = doAction.map(mapInterfaces);
        this.elseAction = elseAction != null ? elseAction.map(mapInterfaces) : null;

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {

        MList<ActionMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        actions.add(requestAction);
        actions.add(doAction);
        if(elseAction != null)
            actions.add(elseAction);

        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        (Function<ActionMapImplement<?, PropertyInterface>, PropertyInterfaceImplement<PropertyInterface>>) ActionMapImplement::mapCalcWhereProperty);
        return PropertyFact.createUnion(interfaces, listWheres);
    }


    public ImSet<Action> getDependActions() {
        return elseAction != null ?
                SetFact.toSet(requestAction.action, doAction.action, elseAction.action) :
                SetFact.toSet(requestAction.action, doAction.action);
    }

    @Override
    public FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;

        boolean isRequestPushed = context.isRequestPushed();
        if (!isRequestPushed)
            result = requestAction.execute(context);

        if (result != FlowResult.FINISH)
            return result;

        boolean isRequestCanceled = context.isRequestCanceled();
        if (!isRequestCanceled) {
            if (isRequestPushed) { // оптимизация
                result = context.popRequest(() -> doAction.execute(context));
            } else
                result = doAction.execute(context);
        } else if(elseAction != null)
            result = elseAction.execute(context);

        return result;
    }

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return requestAction.action.getSimpleRequestInputType(optimistic, true);
    }
}

