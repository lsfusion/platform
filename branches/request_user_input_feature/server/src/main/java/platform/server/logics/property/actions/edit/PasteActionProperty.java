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

    protected <I extends PropertyInterface> PasteActionProperty(String sID, String caption, List<I> innerInterfaces, ActionPropertyMapImplement<I> action) {
        super(sID, caption, innerInterfaces, action);
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext context) throws SQLException {

        ObjectValue objectValue = context.requestUserData(ByteArrayClass.instance, null);
        if(!(objectValue instanceof DataObject))
            return FlowResult.FINISH;

        Map<Map<ObjectInstance, DataObject>, ObjectValue> pasteRows = (Map<Map<ObjectInstance, DataObject>, ObjectValue>)objectValue.getValue();
        for(Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> row : pasteRows.entrySet()) {
            context.pushUserInput(row.getValue()); // нужно для getObjectInstances перегузить
            proceed(context.override(BaseUtils.replace(context.getKeys(), BaseUtils.rightJoin(context.getObjectInstances(), row.getKey()))));
            context.popUserInput(row.getValue());
        }

        return FlowResult.FINISH;
    }
}
