package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

// what the server does every flushAsyncValuesCaches seconds : the cached values of LAZY properties, and the lists of
// values editors ask for, that depend on the changes applied since are dropped - for LAZY NOWAIT the only drop they
// get. Called by a test, so as not to wait for the server's own call
public class FlushLazyValuesAction extends InternalAction {

    public FlushLazyValuesAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        context.getDbManager().flushChanges();
    }
}
