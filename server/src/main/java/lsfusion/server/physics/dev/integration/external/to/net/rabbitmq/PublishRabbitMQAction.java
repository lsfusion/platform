package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;

public class PublishRabbitMQAction extends InternalAction {
    private final ClassPropertyInterface channelInterface;
    private final ClassPropertyInterface messageInterface;


    public PublishRabbitMQAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        channelInterface = i.next();
        messageInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject channelObject = context.getDataKeyValue(channelInterface);
            String message = (String) context.getDataKeyValue(messageInterface).object;

            String host = (String) findProperty("host[Channel]").read(context, channelObject); //localhost
            String queue = (String) findProperty("queue[Channel]").read(context, channelObject); //"hello";
            String user = (String) findProperty("user[Channel]").read(context, channelObject);
            String password = (String) findProperty("password[Channel]").read(context, channelObject);
            boolean local = findProperty("local[Channel]").read(context, channelObject) != null;

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setCredentialsProvider(new DefaultCredentialsProvider(user, password));
            try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
                if(local) { //it's local channel, we create it
                    channel.queueDeclare(queue, false, false, false, null);
                }
                channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}