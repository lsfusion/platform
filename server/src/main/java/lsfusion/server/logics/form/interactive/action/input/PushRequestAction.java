package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.AroundAspectAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// needed for compile optimizations
public class PushRequestAction extends AroundAspectAction {

    public <I extends PropertyInterface> PushRequestAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<?, I> action) {
        super(caption, innerInterfaces, action);

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        if(!context.isRequestCanceled())
            return context.pushRequest(() -> proceed(context));
        return FlowResult.FINISH;
    }
    
    @Override
    public ActionMapImplement<?, PropertyInterface> aspectCompile() {
//        if(hasFlow(REQUESTPUSHED)) in theory flow should be checked for requestPushed, requestCanceled properties, but it's not clear how it should be handled
        // so now we'll just remove RequestAction
        return replace(new ActionReplacer() {
            public <P extends PropertyInterface> ActionMapImplement<?, P> replaceAction(Action<P> action) {
                if(action instanceof RequestAction) // if there is requestAction, we'll replace it with (since !isRequestCanceled)
                    return (ActionMapImplement<?, P>) ((RequestAction) action).getDoAction();
                return null;
            }
        });
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createPushRequestAction(interfaces, action);
    }
}
