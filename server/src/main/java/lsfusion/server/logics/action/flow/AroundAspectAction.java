package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class AroundAspectAction extends KeepContextAction {
    protected final ActionMapImplement<?, PropertyInterface> aspectActionImplement;

    public <P extends PropertyInterface, I extends PropertyInterface> AroundAspectAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<P, I> action) {
        super(caption, innerInterfaces.size());

        this.aspectActionImplement = action.map(getMapInterfaces(innerInterfaces).reverse());
    }

    public ImSet<Action> getDependActions() {
        return SetFact.singleton(aspectActionImplement.action);
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

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException {
    }

    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return aspectActionImplement.mapCalcWhereProperty();
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        return aspectReplaceGenerics(replacer, recursiveAbstracts);
    }
    
    private <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> aspectReplaceGenerics(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        ActionMapImplement<T, PropertyInterface> aspectActionImplement = (ActionMapImplement<T, PropertyInterface>) this.aspectActionImplement;
        ActionMapImplement<?, T> replacedAction = aspectActionImplement.action.replace(replacer, recursiveAbstracts);
        if(replacedAction == null)
            return null;

        return createAspectImplement(interfaces, replacedAction.map(aspectActionImplement.mapping));
    }

    protected abstract <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action);
}
