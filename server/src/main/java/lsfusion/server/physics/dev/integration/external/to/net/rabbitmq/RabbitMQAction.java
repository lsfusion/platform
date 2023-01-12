package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import lsfusion.base.Pair;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMQAction extends InternalAction {

    public RabbitMQAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public RabbitMQAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected static ConsumerMonitorServer consumerMonitorServer;

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (consumerMonitorServer == null) {
            consumerMonitorServer = new ConsumerMonitorServer(context.getLogicsInstance());
        }
    }

    protected class ConsumerMonitorServer extends MonitorServer {

        public Map<Pair<String, String>, Pair<Channel, Connection>> consumers = new HashMap<>();

        private LogicsInstance logicsInstance;

        public ConsumerMonitorServer(LogicsInstance logicsInstance) {
            this.logicsInstance = logicsInstance;
        }

        public void startConsume(ExecutionContext context, String host, String queue, String user, String password, boolean local) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(host);

                factory.setCredentialsProvider(new DefaultCredentialsProvider(user, password));

                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                if(local) { //it's local channel, we create it
                    channel.queueDeclare(queue, false, false, false, null);
                }

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    try {
                        ThreadLocalContext.aspectBeforeMonitorHTTP(ConsumerMonitorServer.this);
                        findAction("processConsumed[STRING]").execute(context, new DataObject(message));
                    } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                        throw Throwables.propagate(e);
                    } finally {
                        ThreadLocalContext.aspectAfterMonitorHTTP(ConsumerMonitorServer.this);
                    }
                    System.out.println(message);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); //confirm delivery
                };

                channel.basicConsume(queue, false, deliverCallback, consumerTag -> {
                });

                consumers.put(Pair.create(host, queue), Pair.create(channel, connection));

            } catch (IOException | TimeoutException e) {
                throw Throwables.propagate(e);
            }
        }

        public void stopConsume(String host, String queue) {
            try {
                Pair<Channel, Connection> consumer = consumers.remove(Pair.create(host, queue));
                if (consumer != null) {
                    stopConsume(consumer);
                }
            } catch (IOException | TimeoutException e) {
                throw Throwables.propagate(e);
            }
        }

        public void stopConsume() throws IOException, TimeoutException {
            for(Pair<Channel, Connection> consumer : consumers.values()) {
                stopConsume(consumer);
            }
            consumers = new HashMap<>();
        }

        public void stopConsume(Pair<Channel, Connection> consumer) throws IOException, TimeoutException {
            consumer.first.close();
            consumer.second.close();
        }

        @Override
        public String getEventName() {
            return "rabbitMQ";
        }

        @Override
        public LogicsInstance getLogicsInstance() {
            return logicsInstance;
        }
    }
}
