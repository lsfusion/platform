package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
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
            return new ClientFutureService((DataObject) connectionObject, context.getNavigatorsManager());
        }
        Integer nThreads = (Integer) threadsProp.read(context, context.getKeys());
        if (nThreads == null || nThreads == 0)
            nThreads = TaskRunner.availableProcessors();
        return new ServerFutureService(ExecutorFactory.createNewThreadService(context, nThreads, sync));
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
