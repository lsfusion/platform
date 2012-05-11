package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.interop.form.ServerResponse;
import platform.server.classes.ValueClass;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyMapImplement;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.flow.FlowActionProperty;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// глобальный action
public abstract class DefaultPasteActionProperty extends CustomActionProperty {

    protected abstract List<PropertyDrawInstance> getPasteProperties();
    protected abstract Map<Map<ClassPropertyInterface, DataObject>, ObjectValue> getPasteRows(PropertyDrawInstance propertyDraw);

    protected DefaultPasteActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    public void execute(ExecutionContext context) throws SQLException {
        
        for(PropertyDrawInstance propertyDraw : getPasteProperties()) {
            for(Map.Entry<Map<ClassPropertyInterface, DataObject>, ObjectValue> row : getPasteRows(propertyDraw).entrySet()) {
                context.pushUserInput(row.getValue());
                PropertyObjectInstance editAction = propertyDraw.getEditAction(ServerResponse.CHANGE_WYS);
                PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> changeAction = null;
                FlowActionProperty.execute(context, changeAction, row.getKey(), BaseUtils.toMap(interfaces));
                context.popUserInput(row.getValue());
            }

/*            бежим по колонкам (request колонок)
            бежим по рядам (request рядов)
            pushUserInput(getType(), parseString())
                    
            propertyDraw
            вызываем CHANGE_WYS			прямо в коде придется делать executeEditAction*/
        }

        //To change body of implemented methods use File | Settings | File Templates.
    }
}
