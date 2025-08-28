package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class GetStatusRabbitMQAction extends InternalAction {
    private final ClassPropertyInterface channelInterface;

    public GetStatusRabbitMQAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        channelInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject channelObject = context.getDataKeyValue(channelInterface);

            String host = (String) findProperty("host[Channel]").read(context, channelObject); //localhost
            String queue = (String) findProperty("queue[Channel]").read(context, channelObject); //"hello";
            String user = (String) findProperty("user[Channel]").read(context, channelObject);
            String password = (String) findProperty("password[Channel]").read(context, channelObject);
            String virtualHost = (String) findProperty("vHost[Channel]").read(context, channelObject);

            String status = context.getLogicsInstance().getRabbitMQServer().getStatus(host, queue, user, password, virtualHost);
            context.requestUserInteraction(new MessageClientAction(status, "RabbitMQ"));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}