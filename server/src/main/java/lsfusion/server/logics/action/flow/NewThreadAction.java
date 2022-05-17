package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.stack.*;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
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

    // in theory we can also pass Thread, and then add ExecutionStackAspect.getStackString to message (to get multi thread stack)
    @StackNewThread
    @StackMessage("NEWTHREAD")
    @ThisMessage
    protected void run(ExecutionContext<PropertyInterface> context) { //, @ParamMessage (profile = false) String callThreadStack) {
        try {
            proceed(context);
        } catch (Throwable t) {
            ServerLoggers.schedulerLogger.error("New thread error : ", t);
            throw Throwables.propagate(t);
        }
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
//        String callThread = ExecutionStackAspect.getStackString();
        if (connectionProp != null) {
            ObjectValue connectionObject = connectionProp.readClasses(context);
            if(connectionObject instanceof DataObject)
                context.getNavigatorsManager().pushNotificationCustomUser((DataObject) connectionObject,
                        (env, stack) -> run(context.override(env, stack))); //, callThread));
        } else {
            context.getSession().registerThreadStack();
            Long delay = delayProp != null ? ((Number) delayProp.read(context, context.getKeys())).longValue() : 0L;
            Long period = periodProp != null ? ((Number) periodProp.read(context, context.getKeys())).longValue() : null;

            Runnable runContext = () -> {
                ExecutionStack stack = ThreadLocalContext.getStack();
                try {
                    run(context.override(stack)); //, callThread);
                } finally {
                    if (periodProp == null) {
                        try {
                            context.getSession().unregisterThreadStack();
                        } catch (SQLException ignored) {
                        }
                    }
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

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewThreadAction(interfaces, action, periodProp, delayProp, connectionProp);
    }
}
