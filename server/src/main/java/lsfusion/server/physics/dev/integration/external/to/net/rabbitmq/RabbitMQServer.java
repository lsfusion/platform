package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import lsfusion.base.Pair;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static lsfusion.base.BaseUtils.nvl;

public class RabbitMQServer extends MonitorServer {

    private final Logger logger = ServerLoggers.httpServerLogger;

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
        if(LM != null) {
            logInfo("onStarted");
            try (DataSession session = createSession()) {
                LM.findAction("restartConsumers[]").execute(session, getTopStack());
            } catch (Throwable t) {
                logError("onStarted failed", t);
            }
        }
    }

    public RabbitMQServer() {
        super(DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public Map<Pair<String, String>, Consumer> consumers = new HashMap<>();

    public void startConsume(String host, String queue, String user, String password, boolean local, String virtualHost, Integer threadCount, Integer prefetchCount) {
        try {
            Consumer consumer = createConsumer(host, user, password, virtualHost, threadCount, prefetchCount);

            ((AutorecoveringConnection) consumer.connection).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                    logInfo("recovery started");
                }

                @Override
                public void handleRecovery(Recoverable recoverable) {
                    Pair<String, String> consumerKey = Pair.create(host, queue);
                    if (recoverable instanceof AutorecoveringConnection) {
                        consumers.get(consumerKey).connection = (Connection) recoverable;
                    } else if (recoverable instanceof AutorecoveringChannel) {
                        consumers.get(consumerKey).channel = (Channel) recoverable;
                    }
                    logInfo("recovery completed");
                }
            });

            if (local) { //it's local channel, we create it
                consumer.channel.queueDeclare(queue, false, false, false, null);
            }

            consumer.channel.basicConsume(queue, false, new DefaultConsumer(consumer.channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    consumer.executor.submit(() -> {
                        if(consumer.channel.isOpen()) {
                            String message = new String(body, StandardCharsets.UTF_8);
                            try {
                                ThreadLocalContext.aspectBeforeMonitorHTTP(RabbitMQServer.this);
                                try (DataSession session = createSession()) {
                                    LM.findAction("onMessage[STRING,STRING,STRING]").execute(session, getStack(), new DataObject(host), new DataObject(queue), new DataObject(message));
                                } catch (Exception e) {
                                    logError("onMessage failed", e);
                                    throw Throwables.propagate(e);
                                }
                            } finally {
                                ThreadLocalContext.aspectAfterMonitorHTTP(RabbitMQServer.this);
                            }
                            try {
                                consumer.channel.basicAck(envelope.getDeliveryTag(), false); //confirm delivery
                            } catch (IOException e) {
                                logError("onMessage confirm failed", e);
                                throw Throwables.propagate(e);
                            }
                        }
                    });
                }
            });

            consumers.put(Pair.create(host, queue), consumer);

        } catch (IOException | TimeoutException e) {
            logError("startConsume error", e);
            throw Throwables.propagate(e);
        }
    }

    public void stopConsume(String host, String queue) {
        try {
            Consumer consumer = consumers.remove(Pair.create(host, queue));
            if (consumer != null) {
                stopConsume(consumer);
            }
        } catch (IOException | TimeoutException e) {
            logError("stopConsume error", e);
            throw Throwables.propagate(e);
        }
    }

    public void stopConsume(Consumer consumer) throws IOException, TimeoutException {
        Channel channel = consumer.channel;
        if(channel.isOpen()) {
            channel.close();
        }
        Connection connection = consumer.connection;
        if(connection.isOpen()) {
            connection.close();
        }
    }

    public String getStatus(String host, String queue, String user, String password, String virtualHost) {
        try {
            Consumer consumer = createConsumer(host, user, password, virtualHost, null, null);
            AMQP.Queue.DeclareOk response = consumer.channel.queueDeclarePassive(queue);
            return String.format("Messages: %s\nConsumers: %s", response.getMessageCount(), response.getConsumerCount());
        } catch (IOException | TimeoutException e) {
            logError("getStatus error", e);
            throw Throwables.propagate(e);
        }
    }

    private Consumer createConsumer(String host, String user, String password, String virtualHost, Integer threadCount, Integer prefetchCount) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        if (virtualHost != null)
            factory.setVirtualHost(virtualHost);
        factory.setCredentialsProvider(new DefaultCredentialsProvider(user, password));
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.basicQos(nvl(prefetchCount, 1));
        return new Consumer(connection, channel, threadCount);
    }

    private void logInfo(String message) {
        logger.info("RabbitMQServer: " + message);
    }

    private void logError(String message, Throwable t) {
        logger.error("RabbitMQServer: " + message, t);
    }

    public static class Consumer {
        public Connection connection;
        public Channel channel;
        ExecutorService executor;

        public Consumer(Connection connection, Channel channel, Integer threadCount) {
            this.connection = connection;
            this.channel = channel;
            this.executor = Executors.newFixedThreadPool(nvl(threadCount, 1));
        }
    }
}