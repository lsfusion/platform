package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// needed for compile optimizations
public class PushRequestAction extends KeepContextAction {

    private final ActionMapImplement<?, PropertyInterface> doAction;

    public <I extends PropertyInterface> PushRequestAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<?, I> doAction) {
        super(caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.doAction = doAction != null ? doAction.map(mapInterfaces) : null;

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return doAction.mapCalcWhereProperty();
    }


    public ImSet<Action> getDependActions() {
        return SetFact.singleton(doAction.action);
    }

    @Override
    public FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return context.pushRequest(() -> doAction.execute(context));
    }
}
