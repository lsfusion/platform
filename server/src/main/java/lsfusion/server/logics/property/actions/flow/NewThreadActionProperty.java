package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewThreadActionProperty extends AroundAspectActionProperty {

    private Long repeat;
    private long delay;

    public <I extends PropertyInterface> NewThreadActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action, Long repeat, long delay) {
        super(sID, caption, innerInterfaces, action);
        this.repeat = repeat;
        this.delay = delay;
    }

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(50, new DaemonThreadFactory("newthread-pool"));
    
    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException {
        
        final DBManager dbManager = context.getDbManager();
        final LogicsInstanceContext logicsContext = context.getLogicsInstance().getContext();

        Runnable run = new Runnable() {
            public void run() {
                try {
                    ThreadLocalContext.set(logicsContext);
                    proceed(context.override(dbManager.createSession()));
                } catch (SQLException e) {
                    throw Throwables.propagate(e);
                } catch (SQLHandledException e) {
                    throw Throwables.propagate(e);
                }
            }
        };

        if(repeat!=null)
            executor.scheduleAtFixedRate(run, delay, repeat, TimeUnit.MILLISECONDS);
        else
            executor.schedule(run, delay, TimeUnit.MILLISECONDS);
        
        return FlowResult.FINISH;
    }
}
