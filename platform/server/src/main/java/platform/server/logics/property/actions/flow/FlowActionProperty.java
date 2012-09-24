package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;

public abstract class FlowActionProperty extends ActionProperty<PropertyInterface> {

    public static List<PropertyInterface> genInterfaces(int size) {
        List<PropertyInterface> result = new ArrayList<PropertyInterface>();
        for(int i=0;i<size;i++)
            result.add(new PropertyInterface(i));
        return result;
    }

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, int size) {
        super(sID, caption, genInterfaces(size));
    }

    @Override
    public abstract FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException;

    public static <P extends PropertyInterface, M> FlowResult execute(ExecutionContext<PropertyInterface> context, ActionPropertyImplement<P, M> implement, Map<M, DataObject> keys, Map<PropertyInterface, M> mapInterfaces) throws SQLException {
        return implement.property.execute(context.override(join(implement.mapping, keys),
                BaseUtils.<Map<P, CalcPropertyInterfaceImplement<PropertyInterface>>>immutableCast(crossInnerValues(implement.mapping, mapInterfaces))));
    }
}
