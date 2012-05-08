package platform.server.session;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExecutionEnvironment {

    private ExecutionEnvironmentInterface current;

    public ExecutionEnvironment(ExecutionEnvironmentInterface current) {
        this.current = current;
    }

    public DataSession getSession() {
        return current.getSession();
    }
    public Modifier getModifier() {
        return current.getModifier();
    }
    public FormInstance getFormInstance() {
        return current.getFormInstance();
    }

    public boolean isInTransaction() {
        return current.isInTransaction();
    }

    public <P extends PropertyInterface> List<ClientAction> execute(Property<P> property, PropertyChange<P> change, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        current.fireChange(property, change);

        MapDataChanges<P> userDataChanges = null;
        if(property instanceof UserProperty) { // оптимизация
            userDataChanges = (MapDataChanges<P>) current.getSession().getUserDataChanges((UserProperty)property, (PropertyChange<ClassPropertyInterface>) change);
//            assert userDataChanges == null || BaseUtils.hashEquals(userDataChanges,property.getDataChanges(change, modifier));
//            из-за того что DataSession знает конкретные значения, а в модифайере все прячется в таблицы, верхний assertion не работает
        }
        return execute(userDataChanges!=null?userDataChanges:property.getDataChanges(change, current.getModifier()), mapObjects);
    }

    public <P extends PropertyInterface> List<ClientAction> execute(MapDataChanges<P> mapChanges, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>(); // сначала читаем изменения, чтобы не было каскадных непредсказуемых эффектов, потом изменяем
        for(Map.Entry<UserProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : mapChanges.changes.read(current.getSession()).entrySet()) {
            for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propRow.getValue().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> row = iterator.next();
                UserProperty property = propRow.getKey();
                Map<ClassPropertyInterface, P> mapInterfaces = mapChanges.map.get(property);
                if(property instanceof DataProperty)
                    getSession().changeProperty((DataProperty)property, row.getKey(), row.getValue().get("value"), !iterator.hasNext());
                else
                    ((ExecuteProperty)property).execute(new ExecutionContext(row.getKey(), row.getValue().get("value"), this, actions, mapInterfaces == null ? null : BaseUtils.nullJoin(mapInterfaces, mapObjects), !iterator.hasNext()));
            }
        }
        return actions;

    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        return current.addObject(cls);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass cls, boolean groupLast) throws SQLException {
        current.changeClass(objectInstance, object, cls, groupLast);
    }

    public void apply(BusinessLogics BL, List<ClientAction> actions) throws SQLException {
        current.apply(BL, actions);
    }

    public void cancel() throws SQLException {
        current = current.cancel();
    }

}
