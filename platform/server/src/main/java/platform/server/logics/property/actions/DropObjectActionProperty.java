package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.NullValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

// сбрасывает объект в null
public class DropObjectActionProperty extends SystemActionProperty {

    public DropObjectActionProperty(ValueClass valueClass) {
        super("drop" + valueClass.getSID(), ServerResourceBundle.getString("logics.property.actions.drop") + " " + valueClass, new ValueClass[]{valueClass});
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        PropertyObjectInterfaceInstance objectInstance = context.getSingleObjectInstance();
        if(objectInstance instanceof ObjectInstance) // не changeObject чтобы fire не вызывать, временно так
            context.getFormInstance().seekObject((ObjectInstance)objectInstance, NullValue.instance);
    }
}
