package lsfusion.server.physics.dev.integration.external.to.net.rabbitmq;

import com.google.common.base.Throwables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lsfusion.base.Pair;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class StopConsumersRabbitMQAction extends RabbitMQAction {
    public StopConsumersRabbitMQAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        super.executeInternal(context);
        try {
            StartConsumerRabbitMQAction.consumerMonitorServer.stopConsume();
        } catch (IOException | TimeoutException e) {
            throw Throwables.propagate(e);
        }
    }
}
