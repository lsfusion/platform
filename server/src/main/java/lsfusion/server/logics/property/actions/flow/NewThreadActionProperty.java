package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.EnvStackRunnable;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewThreadActionProperty extends AroundAspectActionProperty {

    private Long repeat;
    private long delay;
    private CalcPropertyInterfaceImplement<PropertyInterface> connectionProp;

    public <I extends PropertyInterface> NewThreadActionProperty(String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action, Long repeat, long delay, CalcPropertyInterfaceImplement connection) {
        super(caption, innerInterfaces, action);
        this.repeat = repeat;
        this.delay = delay;

        if(connection != null) {
            ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
            this.connectionProp = connection.map(mapInterfaces);
        }
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {

        final EnvStackRunnable run = new EnvStackRunnable() {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack) {
                try {
                    if(env != null)
                        proceed(context.override(env, stack));
                    else {
                        DataSession session = context.getSession();
                        session.registerThreadStack();
                        try {
                            proceed(context.override(stack));
                        } finally {
                            session.unregisterThreadStack();
                        }
                    };
                } catch (Throwable t) {
                    ServerLoggers.schedulerLogger.error("New thread error : ", t);
                    throw Throwables.propagate(t);
                }
            }
        };

        if (connectionProp != null) {
            ObjectValue connectionObject = connectionProp.readClasses(context, context.getKeys());
            if(connectionObject instanceof DataObject)
                context.getNavigatorsManager().pushNotificationCustomUser((DataObject) connectionObject, run);
        } else {
            Runnable runContext = new Runnable() {
                public void run() {
                    run.run(null, ThreadLocalContext.getStack());
                }
            };
            boolean externalExecutor = context.getExecutorService() != null;
            ScheduledExecutorService executor = externalExecutor ? context.getExecutorService() : ExecutorFactory.createNewThreadService(context);
            if (repeat != null)
                executor.scheduleAtFixedRate(runContext, delay, repeat, TimeUnit.MILLISECONDS);
            else
                executor.schedule(runContext, delay, TimeUnit.MILLISECONDS);
            if (!externalExecutor)
                executor.shutdown();
        }
        return FlowResult.FINISH;
    }
}
