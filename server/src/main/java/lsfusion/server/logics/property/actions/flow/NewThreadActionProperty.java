package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.EnvRunnable;
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
import lsfusion.server.session.ExecutionEnvironment;

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

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {

        final DBManager dbManager = context.getDbManager();

        EnvRunnable run = new EnvRunnable() {
            @Override
            public void run(ExecutionEnvironment env) {
                try {
                    proceed(context.override(env, ThreadLocalContext.getStack()));
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

            Runnable runContext = new ScheduleRunnable(run, dbManager);

            ScheduledExecutorService executor = ExecutorFactory.createNewThreadService(context);
            if(repeat!=null)
                executor.scheduleAtFixedRate(runContext, delay, repeat, TimeUnit.MILLISECONDS);
            else
                executor.schedule(runContext, delay, TimeUnit.MILLISECONDS);
            executor.shutdown();
        }
        return FlowResult.FINISH;
    }

    class ScheduleRunnable implements Runnable {
        EnvRunnable r;
        DBManager dbManager;

        ScheduleRunnable(EnvRunnable r, DBManager dbManager) {
            this.r = r;
            this.dbManager = dbManager;
        }

        public void run() {
            try (DataSession session = dbManager.createSession()) {
                r.run(session);
            } catch (Throwable t) {
                throw Throwables.propagate(t);
            }
        }
    }
}
