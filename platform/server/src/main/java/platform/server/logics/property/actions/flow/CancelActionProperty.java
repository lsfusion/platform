package platform.server.logics.property.actions.flow;

import platform.server.classes.BaseClass;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class CancelActionProperty extends KeepContextActionProperty {

    public CancelActionProperty() {
        super("cancel", "cancel", 0);
    }

    public Set<ActionProperty> getDependActions() {
        return new HashSet<ActionProperty>();
    }

    @Override
    public boolean hasCancel() {
        return true;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        context.cancel();
        return FlowResult.FINISH;
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return new CalcPropertyMapImplement<PropertyInterface, PropertyInterface>(NullValueProperty.instance);
    }

}
