package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.concurrent.Executors;
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

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(50, new DaemonThreadFactory("newthread-pool"));

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {

        final DBManager dbManager = context.getDbManager();
        final LogicsInstanceContext logicsContext = context.getLogicsInstance().getContext();

        Runnable run = new Runnable() {
            public void run() {
                try {
                    try (DataSession session = dbManager.createSession()) {
                        proceed(context.override(session));
                    }
                } catch (Throwable t) {
                    throw Throwables.propagate(t);
                }
            }
        };

        if (connectionProp != null) {
            ObjectValue connectionObject = connectionProp.readClasses(context, context.getKeys());
            if(connectionObject instanceof DataObject)
                context.getNavigatorsManager().pushNotificationCustomUser((DataObject) connectionObject, run);
        } else {

            Runnable runContext = new ScheduleRunnable(run, logicsContext);

            if (repeat != null) {
                executor.scheduleAtFixedRate(runContext, delay, repeat, TimeUnit.MILLISECONDS);
            } else {
                executor.schedule(runContext, delay, TimeUnit.MILLISECONDS);
            }
        }
        return FlowResult.FINISH;
    }

    class ScheduleRunnable implements Runnable {
        Runnable r;
        LogicsInstanceContext logicsContext;

        ScheduleRunnable(Runnable r, LogicsInstanceContext logicsContext) {
            this.r = r;
            this.logicsContext = logicsContext;
        }

        public void run() {
            ThreadLocalContext.set(logicsContext);
            r.run();
        }
    }
}
