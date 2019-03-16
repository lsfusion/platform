package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Iterator;

public class WriteToServerSocketActionProperty extends InternalAction {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface ipInterface;
    private final ClassPropertyInterface portInterface;

    public WriteToServerSocketActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        charsetInterface = i.next();
        ipInterface = i.next();
        portInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String text = (String) context.getDataKeyValue(textInterface).object;
        String charset = (String) context.getDataKeyValue(charsetInterface).object;
        String ip = (String) context.getDataKeyValue(ipInterface).object;
        Integer port = (Integer) context.getDataKeyValue(portInterface).object;

        try {
            ServerLoggers.printerLogger.info(String.format("Write to server socket started for ip %s port %s", ip, port));
            try (OutputStream os = new Socket(ip, port).getOutputStream()) {
                os.write(text.getBytes(charset));
            }
            findProperty("printed[]").change(true, context);
            ServerLoggers.printerLogger.info(String.format("Write to server socket finished for ip %s port %s", ip, port));

        } catch (ConnectException e) {
            ServerLoggers.printerLogger.error("Write to server socket error", e);
            try {
                findProperty("printed[]").change((Boolean) null, context);
            } catch (ScriptingErrorLog.SemanticErrorException ignored) {
            }
            context.delayUserInteraction(new MessageClientAction(String.format("Сокет %s:%s недоступен. \n%s", ip, port, e.getMessage()), "Ошибка"));
        }catch (Exception e) {
            try {
                findProperty("printed[]").change((Boolean) null, context);
            } catch (ScriptingErrorLog.SemanticErrorException ignored) {
            }
            ServerLoggers.printerLogger.error("Write to server socket error", e);
            throw Throwables.propagate(e);
        }
    }
}