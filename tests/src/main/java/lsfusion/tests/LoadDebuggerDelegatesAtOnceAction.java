package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDebugger;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

// two threads loading the debugger's delegate classes of two modules at once, as two chunks of the start do. A put a
// plain map loses to another one at once is a matter of chance - a lab start of mycompany lost 4 of 615 -, so the test
// checks the kind of map the chunks share, and that the two loads keep both classes. The classes are of two modules
// with no actions, compiled into a directory of the test's own, and are taken out of the map afterwards. What the test
// saw goes into ddFailure
public class LoadDebuggerDelegatesAtOnceAction extends InternalAction {

    public LoadDebuggerDelegatesAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ActionDebugger debugger = ActionDebugger.getInstance();
            Field field = ActionDebugger.class.getDeclaredField("delegatesHolderClasses");
            field.setAccessible(true);
            Map<String, Class> holders = (Map<String, Class>) field.get(debugger);
            String failure = holders instanceof ConcurrentMap ? null :
                    "the debugger keeps its delegate classes in a " + holders.getClass().getName() + ", which the start's chunks put into at once";

            Path directory = Files.createTempDirectory("lsfusiondebugtest");
            ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 2, false);
            try {
                Future<?> first = threads.submit(() -> compile(debugger, directory.toFile(), "DdFirst"));
                Future<?> second = threads.submit(() -> compile(debugger, directory.toFile(), "DdSecond"));
                first.get(1, TimeUnit.MINUTES);
                second.get(1, TimeUnit.MINUTES);
                if (failure == null && !(holders.containsKey("DdFirst") && holders.containsKey("DdSecond")))
                    failure = "the debugger kept the delegate classes of " + holders.keySet();
            } finally {
                holders.remove("DdFirst");
                holders.remove("DdSecond");
                threads.shutdownNow();
                try (Stream<Path> files = Files.walk(directory)) {
                    files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
                }
            }

            findProperty("ddFailure[]").change(failure, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static void compile(ActionDebugger debugger, File directory, String module) {
        try {
            debugger.compileDelegatesHolders(directory, MapFact.<String, ImSet<DebugInfo>>singleton(module, SetFact.EMPTY()));
        } catch (IOException | ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }
}
