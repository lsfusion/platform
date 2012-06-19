package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.server.classes.ByteArrayClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;
import platform.server.logics.property.actions.flow.FlowActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// глобальный action
public class PasteActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> PasteActionProperty(String sID, String caption, List<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeWYSAction) {
        super(sID, caption, innerInterfaces, changeWYSAction);
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException {

        ObjectValue objectValue = context.requestUserData(ByteArrayClass.instance, null);
        if(!(objectValue instanceof DataObject))
            return FlowResult.FINISH;

        Map<Map<ObjectInstance, DataObject>, ObjectValue> pasteRows = (Map<Map<ObjectInstance, DataObject>, ObjectValue>)objectValue.getValue();
        for(Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> row : pasteRows.entrySet()) {
            ExecutionContext<PropertyInterface> innerContext = context.pushUserInput(row.getValue()); // нужно для getObjectInstances перегузить
            proceed(innerContext.override(BaseUtils.replace(innerContext.getKeys(), BaseUtils.rightJoin(innerContext.getObjectInstances(), row.getKey()))));
        }

        return FlowResult.FINISH;
    }
}
