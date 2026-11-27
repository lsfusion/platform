package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;

// the connection's code, compiled as a connection of its user compiles it when it is made
// (RemoteConnection.evaluateCustomizeCode), the navigator copy's checks included
public final class CompileConnectionExtendCodeAction extends CompileExtendCodeAction {
    public CompileConnectionExtendCodeAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected String compile(ExecutionContext<ClassPropertyInterface> context, String code) {
        RemoteConnection.evaluateCustomizeCode(context.getBL(), code);
        return null;
    }
}
