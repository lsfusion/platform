package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.classes.*;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ChangeReadClassActionProperty extends CustomReadClassActionProperty {

    public ChangeReadClassActionProperty(BaseClass baseClass) {
        super("CHANGE_CLASS", "Изменить класс", new ValueClass[]{baseClass});
    }

    protected Read getReadClass(ExecutionContext context) {
        CustomObjectInstance object = (CustomObjectInstance) context.getSingleObjectInstance();
        if(object == null)
            return new Read((CustomClass) BaseUtils.single(interfaces).interfaceClass, true);
        else
            return new Read(object.baseClass, object.currentClass, true);
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, ObjectClass readClass) throws SQLException {
        context.changeClass(context.getSingleObjectInstance(), context.getSingleKeyValue(), (ConcreteObjectClass) readClass);
    }
}
