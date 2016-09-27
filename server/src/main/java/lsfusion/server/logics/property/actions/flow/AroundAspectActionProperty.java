package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;

public abstract class AroundAspectActionProperty extends KeepContextActionProperty {
    protected final ActionPropertyMapImplement<?, PropertyInterface> aspectActionImplement;

    public <P extends PropertyInterface, I extends PropertyInterface> AroundAspectActionProperty(String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<P, I> action) {
        super(caption, innerInterfaces.size());

        this.aspectActionImplement = action.map(getMapInterfaces(innerInterfaces).reverse());
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.singleton((ActionProperty) aspectActionImplement.property);
    }

    public final FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return aroundAspect(context);
    }

    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ExecutionContext<PropertyInterface> innerContext = beforeAspect(context);
        if(innerContext==null)
            return FlowResult.FINISH;

        try {
            FlowResult result = proceed(innerContext);

            afterAspect(result, context, innerContext);

            return result;
        } finally {
            finallyAspect(context, innerContext);
        }
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return context;
    }

    protected FlowResult proceed(ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        return aspectActionImplement.execute(innerContext);
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
    }

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
    }

    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return aspectActionImplement.mapCalcWhereProperty();
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return aspectActionImplement.property.ignoreReadOnlyPolicy();
    }
}
