package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewExecutorAction extends AroundAspectAction {
    private ScheduledExecutorService executor;
    private final PropertyInterfaceImplement<PropertyInterface> threadsProp;
    Boolean sync;

    public <I extends PropertyInterface> NewExecutorAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                           ActionMapImplement<?, I> action,
                                                           PropertyInterfaceImplement<I> threadsProp,
                                                           Boolean sync) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.threadsProp = threadsProp.map(mapInterfaces);
        this.sync = sync;

        finalizeInit();
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return super.aspectChangeExtProps(recursiveAbstracts).replaceValues(true);
    }

    @Override
    public ImMap<Property, Boolean> calculateUsedExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return super.calculateUsedExtProps(recursiveAbstracts).replaceValues(true);
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.calcWhereProperty().mapInterfaceClasses(ClassType.wherePolicy));
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        boolean sync = this.sync == null || this.sync;
        Integer nThreads = (Integer) threadsProp.read(context, context.getKeys());
        if(nThreads == null || nThreads == 0)
            nThreads = TaskRunner.availableProcessors();
        try {
            executor = ExecutorFactory.createNewThreadService(context, nThreads, sync);
            FlowResult result;

            ExecutionContext<PropertyInterface> newExecutorContext = context.override(executor, ListFact.mList(), ListFact.mList());
            try {
                result = proceed(newExecutorContext);
            } finally {
                executor.shutdown();
            }

            if(sync && result.isFinish()) {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                List<ThrowableWithStack> throwables = new ArrayList<>();
                try {
                    ImList<ScheduledFuture> futures = newExecutorContext.getFutures();
                    ImList<String> lsfStacks = newExecutorContext.getLsfStacks();
                    for (int i = 0; i < futures.size(); i++) {
                        try {
                            futures.get(i).get();
                        } catch (ExecutionException | InterruptedException e) {
                            throwables.add(new ThrowableWithStack(e, lsfStacks.get(i)));
                        }
                    }
                } finally {
                    newExecutorContext.clearFutures();
                    newExecutorContext.clearLsfStacks();
                }
                if(!throwables.isEmpty()) {
                    throw new NestedThreadException(throwables.toArray(new ThrowableWithStack[0]));
                }
            }

            return result;
        } catch (InterruptedException e) {
            ThreadUtils.interruptThreadExecutor(executor, context);

            throw Throwables.propagate(e);
        }
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, ImSet<Action<?>> recursiveAbstracts) {
        return aspectActionImplement.mapAsyncEventExec(optimistic, recursiveAbstracts);
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewExecutorAction(interfaces, action, threadsProp, sync);
    }
}
