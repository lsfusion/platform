package platform.server.logics.property.actions.flow;

import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static platform.base.BaseUtils.reverse;
import static platform.base.BaseUtils.toListNoNull;

public abstract class AroundAspectActionProperty extends KeepContextActionProperty {
    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> aspectActionImplement;

    public <I extends PropertyInterface> AroundAspectActionProperty(String sID, String caption, List<I> innerInterfaces, PropertyMapImplement<ClassPropertyInterface, I> action) {
        super(sID, caption, innerInterfaces, toListNoNull((PropertyInterfaceImplement<I>) action));

        this.aspectActionImplement = action.map(reverse(getMapInterfaces(innerInterfaces)));
    }

    public Set<Property> getChangeProps() {
        return ((FlowActionProperty) aspectActionImplement.property).getChangeProps();
    }

    public Set<Property> getUsedProps() {
        return ((FlowActionProperty) aspectActionImplement.property).getUsedProps();
    }

    @Override
    public final FlowResult flowExecute(ExecutionContext context) throws SQLException {
        return executeAspect(context);
    }

    protected FlowResult executeAspect(ExecutionContext context) throws SQLException {
        ExecutionContext innerContext = beforeAspect(context);

        FlowResult result = proceed(innerContext);

        afterAspect(result, context, innerContext);

        return result;
    }

    protected ExecutionContext beforeAspect(ExecutionContext context) throws SQLException {
        return context;
    }

    protected FlowResult proceed(ExecutionContext innerContext) throws SQLException {
        return execute(innerContext, aspectActionImplement);
    }

    protected void afterAspect(FlowResult result, ExecutionContext context, ExecutionContext innerContext) throws SQLException {
    }
}
