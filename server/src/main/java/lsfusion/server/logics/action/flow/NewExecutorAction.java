package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
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
import java.util.HashSet;
import java.util.Set;

public class NewExecutorAction extends AroundAspectAction {
    private final PropertyInterfaceImplement<PropertyInterface> threadsProp;
    private final PropertyInterfaceImplement<PropertyInterface> connectionProp;
    Boolean sync;

    public <I extends PropertyInterface> NewExecutorAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                           ActionMapImplement<?, I> action,
                                                           PropertyInterfaceImplement<I> threadsProp,
                                                           PropertyInterfaceImplement<I> connectionProp,
                                                           Boolean sync) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.threadsProp = threadsProp != null ? threadsProp.map(mapInterfaces) : null;
        this.connectionProp = connectionProp != null ? connectionProp.map(mapInterfaces) : null;
        this.sync = sync;

        finalizeInit();
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps(ImSet<Action<?>> recursiveAbstracts) {
        // Body changes are recursive (run in another thread). NEWTHREAD ... TO writes go through
        // service.applyResults() in OUR thread after await(), so they are current-session (false)
        // — and they're not in the body's own change set, so we merge them in.
        ImMap<Property, Boolean> base = super.aspectChangeExtProps(recursiveAbstracts).replaceValues(true);
        Set<Property> toTargets = new HashSet<>();
        Set<Action<?>> visited = new HashSet<>();
        for (Action<?> dep : getDependActions()) {
            collectToTargets(dep, toTargets, visited);
        }
        if (toTargets.isEmpty()) {
            return base;
        }
        return base.merge(SetFact.toExclSet(toTargets.toArray(new Property[0])).toMap(false), Action.addValue);
    }

    private static void collectToTargets(Action<?> action, Set<Property> toTargets, Set<Action<?>> visited) {
        if (!visited.add(action)) return;
        // Nested NEWEXECUTOR applies its own TO targets via its own await — those stay
        // recursive at our level, not current-session.
        if (action instanceof NewExecutorAction) return;
        if (action instanceof NewThreadAction) {
            NewThreadAction.ResultTarget rt = ((NewThreadAction) action).getResultTarget();
            if (rt != null) toTargets.add(rt.toProp.property);
        }
        for (Action<?> dep : action.getDependActions()) {
            collectToTargets(dep, toTargets, visited);
        }
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
        ScheduledFutureService service = createService(context, sync);
        if (service == null)
            return FlowResult.FINISH;

        try {
            FlowResult result;
            try {
                result = proceed(context.override(service));
            } finally {
                service.shutdown();
            }

            if (sync && result.isFinish()) {
                NestedThreadException nestedThreadException = service.await();
                if (nestedThreadException != null) {
                    throw nestedThreadException;
                }
                service.applyResults(context);
            }

            return result;
        } catch (InterruptedException e) {
            service.interruptIfPossible(context);
            throw Throwables.propagate(e);
        }
    }

    private ScheduledFutureService createService(ExecutionContext<PropertyInterface> context, boolean sync) throws SQLException, SQLHandledException {
        if (connectionProp != null) {
            ObjectValue connectionObject = connectionProp.readClasses(context);
            if (!(connectionObject instanceof DataObject))
                return null;
            return new ClientFutureService((DataObject) connectionObject, context.getNavigatorsManager(), sync);
        }
        Integer nThreads = (Integer) threadsProp.read(context, context.getKeys());
        if (nThreads == null || nThreads == 0)
            nThreads = TaskRunner.availableProcessors();
        return new ServerFutureService(ExecutorFactory.createNewThreadService(context, nThreads, sync), sync);
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, ImSet<Action<?>> recursiveAbstracts) {
        return aspectActionImplement.mapAsyncEventExec(optimistic, recursiveAbstracts);
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewExecutorAction(interfaces, action, threadsProp, connectionProp, sync);
    }
}
