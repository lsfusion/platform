package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DeleteObjectActionProperty extends ActionProperty {
    public static final String NAME = "deleteAction";

    public DeleteObjectActionProperty(String sID, CustomClass valueClass) {
        super(NAME, sID, "Удалить (" + valueClass + ")", new ValueClass[]{valueClass});
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        ((FormInstance<?>)executeForm.form).changeClass((CustomObjectInstance) BaseUtils.singleValue(mapObjects), BaseUtils.singleValue(keys), -1);
    }
}
