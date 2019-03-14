package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.EnvStackRunnable;
import lsfusion.server.ServerLoggers;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.base.context.ExecutorFactory;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.property.implement.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewThreadActionProperty extends AroundAspectAction {

    private CalcPropertyInterfaceImplement<PropertyInterface> periodProp;
    private CalcPropertyInterfaceImplement<PropertyInterface> delayProp;
    private CalcPropertyInterfaceImplement<PropertyInterface> connectionProp;

    public <I extends PropertyInterface> NewThreadActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                                 ActionPropertyMapImplement<?, I> action,
                                                                 CalcPropertyInterfaceImplement<I> period,
                                                                 CalcPropertyInterfaceImplement<I> delay,
                                                                 CalcPropertyInterfaceImplement<I> connection) {
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
