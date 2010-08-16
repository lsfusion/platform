package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AddObjectActionProperty extends ActionProperty {

    public static final String NAME = "addAction";

    private CustomClass valueClass;

    public AddObjectActionProperty(String sID, CustomClass valueClass) {
        super(NAME, sID, "Добавить (" + valueClass + ")", new ValueClass[] {}); // сам класс не передаем, поскольку это свойство "глобальное"

        this.valueClass = valueClass;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>)executeForm.form;
        if (valueClass.hasChildren())
            form.addObject((ConcreteCustomClass)form.getCustomClass((Integer)value.getValue()));
        else
            form.addObject((ConcreteCustomClass)valueClass);
    }

    @Override
    protected DataClass getValueClass() {
        if (valueClass.hasChildren())
            return ClassActionClass.getInstance(valueClass);
        else
            return super.getValueClass();
    }
}
