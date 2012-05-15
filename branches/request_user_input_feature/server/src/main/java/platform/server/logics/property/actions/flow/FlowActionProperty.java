package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;

public abstract class FlowActionProperty extends ActionProperty {

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, List<I> listInterfaces, Collection<? extends PropertyInterfaceImplement<I>> used) {
        this(sID, caption, getClasses(listInterfaces, used));
    }

    protected FlowActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    public abstract FlowResult execute(ExecutionContext context) throws SQLException;

    public static FlowResult execute(ExecutionContext context, ActionPropertyMapImplement<ClassPropertyInterface> implement) throws SQLException {
        return implement.property.execute(context.map(implement.mapping));
    }

    public static <M> FlowResult execute(ExecutionContext context, ActionPropertyImplement<M> implement, Map<M, DataObject> keys, Map<ClassPropertyInterface, M> mapInterfaces) throws SQLException {
        return implement.property.execute(context.override(join(implement.mapping, keys), crossInnerValues(mapInterfaces, implement.mapping)));
    }
}
