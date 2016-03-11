package lsfusion.erp.utils.printer;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class WriteToPrinterActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface printerNameInterface;


    public WriteToPrinterActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        charsetInterface = i.next();
        printerNameInterface = i.next();

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            String text = (String) context.getDataKeyValue(textInterface).object;
            String charset = (String) context.getDataKeyValue(charsetInterface).object;
            String printerName = (String) context.getDataKeyValue(printerNameInterface).object;
            ServerLoggers.systemLogger.info("Sending to printer" + printerName + ": \n" + text);

            String result = (String) context.requestUserInteraction(new WriteToPrinterClientAction(text, charset, printerName));
            findProperty("printed[]").change(result == null ? true : (Object) null, context);
            if (result != null)
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
        } catch (Exception e) {
            ServerLoggers.systemLogger.error("WriteToPrinter error", e);
            try {
                findProperty("printed[]").change((Object) null, context);
            } catch (ScriptingErrorLog.SemanticErrorException ignored) {
            }
            throw Throwables.propagate(e);
        }

    }
}
