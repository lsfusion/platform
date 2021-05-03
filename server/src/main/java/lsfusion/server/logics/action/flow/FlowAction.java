package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class FlowAction extends Action<PropertyInterface> {

    public static ImOrderSet<PropertyInterface> genInterfaces(int size) {
        return SetFact.toOrderExclSet(size, genInterface);
    }

    protected <I extends PropertyInterface> FlowAction(LocalizedString caption, int size) {
        super(caption, genInterfaces(size));
    }

    @Override
    public abstract FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException;

    private ActionMapImplement<?, PropertyInterface> compile;
    private boolean compiled;
    @Override
    @ManualLazy
    public ActionMapImplement<?, PropertyInterface> compile() {
        if(!compiled) {
            compile = aspectCompile();
            compiled = true;
        }
        return compile;
    }
    
    protected ActionMapImplement<?, PropertyInterface> aspectCompile() {
        return null;
    }
}
