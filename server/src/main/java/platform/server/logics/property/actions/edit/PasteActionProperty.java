package platform.server.logics.property.actions.edit;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.ByteArrayClass;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

// глобальный action
public class PasteActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> PasteActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeWYSAction) {
        super(sID, caption, innerInterfaces, changeWYSAction);
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException {

        ObjectValue objectValue = context.requestUserData(ByteArrayClass.instance, null);
        if(!(objectValue instanceof DataObject))
            return FlowResult.FINISH;

        ImOrderMap<ImMap<ObjectInstance, DataObject>, ObjectValue> pasteRows = (ImOrderMap<ImMap<ObjectInstance, DataObject>, ObjectValue>)objectValue.getValue();
        for(int i=0,size=pasteRows.size();i<size;i++) {
            ExecutionContext<PropertyInterface> innerContext = context.pushUserInput(pasteRows.getValue(i)); // нужно для getObjectInstances перегузить
            proceed(innerContext.override(MapFact.override(innerContext.getKeys(), innerContext.getObjectInstances().innerJoin(pasteRows.getKey(i)))));
        }

        return FlowResult.FINISH;
    }
}
