package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;

// the code of the module LogicsExtendCode (BusinessLogics.createLogicsExtendCodeModule) : a module of the logics is
// compiled with the whole logics, as the server starts, not apart from it - so nothing is compiled here, only the right
// of the user is checked
public final class CompileLogicsExtendCodeAction extends CompileExtendCodeAction {
    public CompileLogicsExtendCodeAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected String compile(ExecutionContext<ClassPropertyInterface> context, String code) {
        return null;
    }
}
