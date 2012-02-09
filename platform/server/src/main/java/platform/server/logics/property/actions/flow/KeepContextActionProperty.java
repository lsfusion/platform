package platform.server.logics.property.actions.flow;

import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public abstract class KeepContextActionProperty extends FlowActionProperty {

    protected <I extends PropertyInterface> KeepContextActionProperty(String sID, String caption, List<I> listInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, listInterfaces, used);
    }

    protected static void execute(PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> implement, ExecutionContext context) throws SQLException {
        ((ActionProperty) implement.property).execute(context.map(implement.mapping));
    }
}
