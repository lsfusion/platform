package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

// the language can catch an error but cannot raise one, so a failing assertion has to come out of java
public class FailAction extends InternalAction {
    private final ClassPropertyInterface messageInterface;

    public FailAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        messageInterface = interfaces.iterator().next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        throw new RuntimeException((String) context.getKeyValue(messageInterface).getValue());
    }
}
