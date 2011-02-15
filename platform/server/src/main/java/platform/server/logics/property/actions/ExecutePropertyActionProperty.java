package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecutePropertyActionProperty extends ActionProperty {

    private static ValueClass[] getValueClasses(LP dataProp) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        Result<ValueClass> returnValue = new Result<ValueClass>();
        ValueClass[] interfaces = dataProp.getCommonClasses(returnValue);

        result.add(returnValue.result);
        result.addAll(BaseUtils.toList(interfaces));

        return result.toArray(new ValueClass[result.size()]);
    }

    private LP dataProperty;

    public ExecutePropertyActionProperty(String sID, String caption, LP dataProperty) {
        super(sID, caption, getValueClasses(dataProperty));

        this.dataProperty = dataProperty;
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>)executeForm.form;
        DataSession session = form.session;

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>)interfaces;

        Object execValue = null;
        DataObject[] execInterfaces = new DataObject[listInterfaces.size() - 1];
        for (int i = 0; i < listInterfaces.size(); i++) {
            if (i == 0)
                execValue = keys.get(listInterfaces.get(i)).getValue();
            else
                execInterfaces[i-1] = keys.get(listInterfaces.get(i));
        }

        actions.addAll(dataProperty.execute(execValue, session, execInterfaces));
    }
}
