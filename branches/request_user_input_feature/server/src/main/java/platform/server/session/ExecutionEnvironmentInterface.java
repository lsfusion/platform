package platform.server.session;

import platform.interop.action.ClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ExecutionEnvironmentInterface {

    DataSession getSession();

    Modifier getModifier();

    FormInstance getFormInstance();

    boolean isInTransaction();

    <P extends PropertyInterface> void fireChange(Property<P> property, PropertyChange<P> change) throws SQLException;

    DataObject addObject(ConcreteCustomClass cls) throws SQLException;

    void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass cls, boolean groupLast) throws SQLException;

    boolean apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException;
    ExecutionEnvironmentInterface cancel() throws SQLException;
}
