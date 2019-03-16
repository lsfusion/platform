package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.EnvStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewThreadAction extends AroundAspectAction {

    private PropertyInterfaceImplement<PropertyInterface> periodProp;
    private PropertyInterfaceImplement<PropertyInterface> delayProp;
    private PropertyInterfaceImplement<PropertyInterface> connectionProp;

    public <I extends PropertyInterface> NewThreadAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                         ActionMapImplement<?, I> action,
                                                         PropertyInterfaceImplement<I> period,
                                                         PropertyInterfaceImplement<I> delay,
                                                         PropertyInterfaceImplement<I> connection) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        if (period != null) {
            this.periodProp = period.map(mapInterfaces);            
        }
        if (delay != null) {
            this.delayProp = delay.map(mapInterfaces);
        }
        if(connection != null) {
            this.connectionProp = connection.map(mapInterfaces);
        }
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {

        DataSession session = context.getSession();
        session.registerThreadStack();

        final EnvStackRunnable run = new EnvStackRunnable() {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack) {
                try {
                    DataSession session = context.getSession();
                    if(env != null) {
                        session.unregisterThreadStack(); // уже не нужна сессия
                        proceed(context.override(env, stack));
                    } else {
                        try {
                            proceed(context.override(stack));
                        } finally {
                            session.unregisterThreadStack();
                        }
                    }
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
            Long delay = 0L, period = null;
            if (delayProp != null) {
                delay = ((Number) delayProp.read(context, context.getKeys())).longValue();
            }
            if (periodProp != null) {
                period = ((Number) periodProp.read(context, context.getKeys())).longValue();
            }
            
            Runnable runContext = new Runnable() {
                public void run() {
                    run.run(null, ThreadLocalContext.getStack());
                }
            };
            boolean externalExecutor = context.getExecutorService() != null;
            ScheduledExecutorService executor = externalExecutor ? context.getExecutorService() : ExecutorFactory.createNewThreadService(context);
            if (period != null)
                executor.scheduleAtFixedRate(runContext, delay, period, TimeUnit.MILLISECONDS);
            else
                executor.schedule(runContext, delay, TimeUnit.MILLISECONDS);
            if (!externalExecutor && period == null)
                executor.shutdown();
        }
        return FlowResult.FINISH;
    }
}
