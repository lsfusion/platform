package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Iterator;

public class StartConsumerRabbitMQAction extends RabbitMQAction {
    private final ClassPropertyInterface channelInterface;

    public StartConsumerRabbitMQAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        channelInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        super.executeInternal(context);
        try {
            DataObject channelObject = context.getDataKeyValue(channelInterface);

            String host = (String) findProperty("host[Channel]").read(context, channelObject); //localhost
            String queue = (String) findProperty("queue[Channel]").read(context, channelObject); //"hello";
            String user = (String) findProperty("user[Channel]").read(context, channelObject);
            String password = (String) findProperty("password[Channel]").read(context, channelObject);
            boolean local = findProperty("local[Channel]").read(context, channelObject) != null;

            consumerMonitorServer.startConsume(context, host, queue, user, password, local);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }
}
