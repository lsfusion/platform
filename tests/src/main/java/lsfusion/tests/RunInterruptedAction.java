package lsfusion.tests;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

// an action, by its name, run while another thread interrupts this one after the given milliseconds, as the platform
// interrupts a thread - at a scheduler task's timeout, at a cancel in the process monitor -; whether the interrupt came
// out of the action - as an exception, or as the interrupt flag still set on the thread - goes into the property named
// third. The interrupt is taken off the thread afterwards, so that the test goes on
public class RunInterruptedAction extends InternalAction {

    public RunInterruptedAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LA<?> action;
        LP<?> interruptedProperty;
        try {
            action = findAction((String) getParam(0, context));
            interruptedProperty = findProperty((String) getParam(2, context));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
        long after = ((Number) getParam(1, context)).longValue();

        Thread thread = Thread.currentThread();
        DBManager dbManager = context.getDbManager();
        Thread interrupter = new Thread(() -> {
            Uninterruptibles.sleepUninterruptibly(after, TimeUnit.MILLISECONDS);
            try {
                ThreadUtils.interruptThread(dbManager, thread);
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        });
        interrupter.start();

        boolean interrupted = false;
        try {
            action.execute(context);
        } catch (Throwable t) {
            interrupted = true;
        } finally {
            Uninterruptibles.joinUninterruptibly(interrupter);
            if (Thread.interrupted())
                interrupted = true;
            ThreadUtils.interruptThread.remove(thread);
        }
        interruptedProperty.change(interrupted ? Boolean.TRUE : null, context);
    }
}
