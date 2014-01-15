package lsfusion.server.logics.property.actions.edit;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ByteArrayClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.AroundAspectActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

// глобальный action
public class PasteActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> PasteActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeWYSAction) {
        super(sID, caption, innerInterfaces, changeWYSAction);
        
        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {

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
