package platform.server.session;

import platform.interop.action.ClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.data.QueryEnvironment;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormEnvironment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExecutionEnvironment {

    private ExecutionEnvironmentInterface current;
    private ObjectValue lastUserInput;
    private boolean wasUserInput = false;

    public ExecutionEnvironment(ExecutionEnvironmentInterface current) {
        this.current = current;
    }

    public DataSession getSession() {
        return current.getSession();
    }
    public QueryEnvironment getQueryEnv() {
        return current.getQueryEnv();
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

    public <P extends PropertyInterface> List<ClientAction> change(CalcProperty<P> property, PropertyChange<P> change) throws SQLException {
        current.fireChange(property, change);

        DataChanges userDataChanges = null;
        if(property instanceof DataProperty) // оптимизация
            userDataChanges = getSession().getUserDataChanges((DataProperty)property, (PropertyChange<ClassPropertyInterface>) change);
        return change(userDataChanges != null ? userDataChanges : property.getDataChanges(change, current.getModifier()));
    }

    public <P extends PropertyInterface> List<ClientAction> change(DataChanges mapChanges) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>(); // сначала читаем изменения, чтобы не было каскадных непредсказуемых эффектов, потом изменяем
        for(Map.Entry<DataProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : mapChanges.read(this).entrySet()) {
            for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propRow.getValue().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> row = iterator.next();
                getSession().changeProperty(propRow.getKey(), row.getKey(), row.getValue().get("value"), !iterator.hasNext());
            }
        }
        return actions;

    }

    public <P extends PropertyInterface> List<ClientAction> execute(ActionProperty<P> property, PropertyChange<P> set, FormEnvironment<P> formEnv) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(Map.Entry<Map<P, DataObject>, Map<String, ObjectValue>> row : set.executeClasses(this).entrySet())
            actions.addAll(execute(property, row.getKey(), formEnv, row.getValue().get("value")));
        return actions;
    }

    public <P extends PropertyInterface> List<ClientAction> execute(ActionProperty<P> property, PropertySet<P> set, FormEnvironment<P> formEnv) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(Map<P, DataObject> row : set.executeClasses(this))
            actions.addAll(execute(property, row, formEnv, null));
        return actions;
    }

    public <P extends PropertyInterface> List<ClientAction> execute(ActionProperty<P> property, Map<P, DataObject> change, FormEnvironment<P> formEnv, ObjectValue requestInput) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>();
        ExecutionContext<P> context = new ExecutionContext<P>(change, null, this, actions, formEnv, true);

        if(requestInput != null) {
            context = context.pushUserInput(requestInput);
        }

        property.execute(context);

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

    public ObjectValue getLastUserInput() {
        return lastUserInput;
    }
    public boolean getWasUserInput() {
        return wasUserInput;
    }

    public void setLastUserInput(ObjectValue lastUserInput) {
        this.lastUserInput = lastUserInput;
        this.wasUserInput = true;
    }
}
