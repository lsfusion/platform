package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import lsfusion.base.Pair;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMQServer extends MonitorServer {

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule LM;

    @Override
    public String getEventName() {
        return "rabbitMQ-server";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        LM = logicsInstance.getBusinessLogics().getModule("RabbitMQ");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        ServerLoggers.systemLogger.info("Binding RabbitMQServer");
        try(DataSession session = createSession()) {
            LM.findAction("restartConsumers[]").execute(session, getTopStack());
        } catch (SQLException | ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public RabbitMQServer() {
        super(DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public Map<Pair<String, String>, Pair<Channel, Connection>> consumers = new HashMap<>();

    public void startConsume(String host, String queue, String user, String password, boolean local) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);

            factory.setCredentialsProvider(new DefaultCredentialsProvider(user, password));

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            if (local) { //it's local channel, we create it
                channel.queueDeclare(queue, false, false, false, null);
            }

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try(DataSession session = createSession()) {
                    ThreadLocalContext.aspectBeforeMonitorHTTP(RabbitMQServer.this);
                    LM.findAction("processConsumed[STRING,STRING]").execute(session, getStack(), new DataObject(queue), new DataObject(message));
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(RabbitMQServer.this);
                }
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

    public void stopConsume(Pair<Channel, Connection> consumer) throws IOException, TimeoutException {
        consumer.first.close();
        consumer.second.close();
    }
}