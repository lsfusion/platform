package platform.server.logics.property.actions;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

// с явным задание классов параметров (where определяется этими классами)
public abstract class ExplicitActionProperty extends BaseActionProperty<ClassPropertyInterface> {

    protected ExplicitActionProperty(String sID, ValueClass... classes) {
        this(sID, "sys", classes);
    }

    protected ExplicitActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, IsClassProperty.getInterfaces(classes));
    }

    protected abstract void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException;

    protected boolean allowNulls() {
        return false;
    }

    public final FlowResult aspectExecute(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ImMap<ClassPropertyInterface, ? extends ObjectValue> keys = context.getKeys();
        ImMap<ClassPropertyInterface,DataObject> dataKeys = DataObject.filterDataObjects(keys);
        if(!allowNulls() && dataKeys.size() < keys.size())
            proceedNullException();
        else
            if(IsClassProperty.fitInterfaceClasses(context.getSession().getCurrentClasses(dataKeys))) // если подходит по классам выполнем
                executeCustom(context);
        return FlowResult.FINISH;
    }

    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    protected boolean isVolatile() {
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.VOLATILE && isVolatile();
    }
}
