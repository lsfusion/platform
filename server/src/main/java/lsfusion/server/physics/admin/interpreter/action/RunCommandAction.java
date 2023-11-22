package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

public class RunCommandAction extends InternalAction {
    private final ClassPropertyInterface commandInterface;
    private final ClassPropertyInterface directoryInterface;
    private final ClassPropertyInterface isClientInterface;
    private final ClassPropertyInterface waitInterface;

    public RunCommandAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        commandInterface = i.next();
        directoryInterface = i.next();
        isClientInterface = i.next();
        waitInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String command = (String) context.getKeyValue(commandInterface).getValue();
        String directory = (String) context.getKeyValue(directoryInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        boolean wait = context.getKeyValue(waitInterface).getValue() != null;
        if(command != null) {
            try {
                if (isClient)
                    processResult((ArrayList<String>) context.requestUserInteraction(new RunCommandClientAction(command, directory, wait)), context, (message) -> context.requestUserInteraction(new MessageClientAction(message, "Ошибка")));
                else
                    processResult(FileUtils.runCmd(command, directory, wait), context, (message) -> {throw new RuntimeException(message);});
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void processResult(ArrayList<String> result, ExecutionContext<ClassPropertyInterface> context, Consumer<String> action) {
        if (result != null) {
            try {
                findProperty("cmdSTDOUT[]").change(result.get(0), context);
                String stdErr = result.get(1);
                findProperty("cmdSTDERR[]").change(stdErr, context);

                String exitValue = result.get(2);
                if (exitValue != null)
                    action.accept("exitValue = " + exitValue + "\n" + stdErr);
            } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}