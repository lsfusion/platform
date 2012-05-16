package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Map;

public class ChangeReadClassActionProperty extends CustomReadValueActionProperty {

    private BaseClass baseClass;

    public ChangeReadClassActionProperty(BaseClass baseClass) {
        super("CHANGE_CLASS", "Изменить класс", new ValueClass[]{baseClass});
    }

    @Override
    protected DataClass getReadType(ExecutionContext context) {
        Map<ClassPropertyInterface,PropertyObjectInterfaceInstance> mapObjects = context.getObjectInstances();
        if(mapObjects.size() > 0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance) {
            CustomObjectInstance object = (CustomObjectInstance) BaseUtils.singleValue(mapObjects);
            return object.baseClass.getActionClass(object.currentClass);
        }
        return null;
    }

    @Override
    protected void executeRead(ExecutionContext context, Object userValue) throws SQLException {
        context.changeClass(context.getSingleObjectInstance(), context.getSingleKeyValue(), (Integer) userValue);
    }
}
