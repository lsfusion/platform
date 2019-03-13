package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.base.context.ExecutorFactory;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.tasks.TaskRunner;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewExecutorActionProperty extends AroundAspectActionProperty {
    ScheduledExecutorService executor;
    private final CalcPropertyInterfaceImplement threadsProp;

    public <I extends PropertyInterface> NewExecutorActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                                   ActionPropertyMapImplement<?, I> action,
                                                                   CalcPropertyInterfaceImplement threadsProp) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.threadsProp = threadsProp.map(mapInterfaces);

        finalizeInit();
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.calcWhereProperty().mapInterfaceClasses(ClassType.wherePolicy));
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer nThreads = (Integer) threadsProp.read(context, context.getKeys());
            if(nThreads == null || nThreads == 0)
                nThreads = TaskRunner.availableProcessors();
            executor = ExecutorFactory.createNewThreadService(context, nThreads);
            return proceed(context.override(executor));
        } finally {
            if(executor != null) {
                executor.shutdown();
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public CustomClass getSimpleAdd() {
        return aspectActionImplement.property.getSimpleAdd();
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return aspectActionImplement.property.getSimpleDelete();
    }
}
