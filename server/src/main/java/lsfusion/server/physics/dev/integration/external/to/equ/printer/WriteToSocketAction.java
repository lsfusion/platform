package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Iterator;

public class WriteToSocketAction extends InternalAction {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface ipInterface;
    private final ClassPropertyInterface portInterface;
    private final ClassPropertyInterface isClientInterface;

    public WriteToSocketAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        charsetInterface = i.next();
        ipInterface = i.next();
        portInterface = i.next();
        isClientInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String text = (String) context.getKeyValue(textInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        String ip = (String) context.getKeyValue(ipInterface).getValue();
        Integer port = (Integer) context.getKeyValue(portInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;

        if (text != null && charset != null && ip != null && port != null) {

            try {
                ServerLoggers.printerLogger.info(String.format("Write to socket started for ip %s port %s", ip, port));

                if (isClient) {
                    context.delayUserInteraction(new ExecuteClientAction() {
                        @Override
                        public void execute(ClientActionDispatcher dispatcher) {
                            try (OutputStream os = new Socket(ip, port).getOutputStream()) {
                                os.write(text.getBytes(charset));
                            } catch (IOException e) {
                                throw Throwables.propagate(e);
                            }
                        }

                    });
                } else {
                    try (OutputStream os = new Socket(ip, port).getOutputStream()) {
                        os.write(text.getBytes(charset));
                    }
                }

                findProperty("printed[]").change(true, context);
                ServerLoggers.printerLogger.info(String.format("Write to socket finished for ip %s port %s", ip, port));

            } catch (Exception e) {
                ServerLoggers.printerLogger.error("Write to socket error", e);
                try {
                    findProperty("printed[]").change((Boolean) null, context);
                } catch (ScriptingErrorLog.SemanticErrorException ignored) {
                }
                if (e instanceof ConnectException) {
                    context.delayUserInteraction(new MessageClientAction(String.format("Сокет %s:%s недоступен. \n%s", ip, port, e.getMessage()), "Ошибка"));
                } else {
                    throw Throwables.propagate(e);
                }
            }

        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}