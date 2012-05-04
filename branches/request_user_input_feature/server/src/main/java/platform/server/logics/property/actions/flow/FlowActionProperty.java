package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.join;

public abstract class FlowActionProperty extends ActionProperty {

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, List<I> listInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        this(sID, caption, getClasses(listInterfaces, used));
    }

    protected FlowActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    public final void execute(ExecutionContext context) throws SQLException {
        FlowResult result = flowExecute(context);
        if (result == FlowResult.FINISH || result == FlowResult.RETURN) {
            //нормальное завершение
            return;
        }

        if (result == FlowResult.THROWS) {
            throw new IllegalStateException("Action throwed an exception: ...");
        }

        if (result == FlowResult.BREAK) {
            throw new IllegalStateException("Break isn't allowed here!");
        }
    }

    protected abstract FlowResult flowExecute(ExecutionContext context) throws SQLException;

    protected static FlowResult execute(ExecutionContext context, PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> implement) throws SQLException {
        FlowActionProperty property = (FlowActionProperty) implement.property;
        return property.flowExecute(
                context.map(implement.mapping, property.getValueClass().getDefaultObjectValue())
        );
    }

    protected static <M> FlowResult execute(ExecutionContext context, PropertyImplement<ClassPropertyInterface, M> implement, Map<M, DataObject> keys) throws SQLException {
        FlowActionProperty property = (FlowActionProperty) implement.property;
        return property.flowExecute(
                context.override(join(implement.mapping, keys))
        );
    }
}
