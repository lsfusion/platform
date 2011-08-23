package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ExecutionContext {
    private final Map<ClassPropertyInterface, DataObject> keys;
    private final ObjectValue value;
    private final DataSession session;
    private final Modifier<? extends Changes> modifier;
    private final List<ClientAction> actions;
    private final RemoteForm form;
    private final Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects;
    private final boolean groupLast; // обозначает, что изменение последнее, чтобы форма начинала определять, что изменилось

    public ExecutionContext(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm form, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) {
        this.keys = keys;
        this.value = value;
        this.session = session;
        this.modifier = modifier;
        this.actions = actions;
        this.form = form;
        this.mapObjects = mapObjects;
        this.groupLast = groupLast;
    }

    public Map<ClassPropertyInterface, DataObject> getKeys() {
        return keys;
    }

    public DataObject getKeyValue(ClassPropertyInterface key) {
        return keys.get(key);
    }

    public DataObject getKeyValue(Property property, int index) {
        return getKeyValue(((List<ClassPropertyInterface>)property.interfaces).get(index));
    }

    public Object getKeyObject(ClassPropertyInterface key) {
        return keys.get(key).object;
    }

    public DataObject getSingleKeyValue() {
        return BaseUtils.singleValue(keys);
    }

    public Object getSingleKeyObject() {
        return getSingleKeyValue().object;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public ObjectValue getValue() {
        return value;
    }

    public Object getValueObject() {
        return value.getValue();
    }

    public DataSession getSession() {
        if (form != null)
            return getFormInstance().session;
        else
            return session;
    }

    public Modifier<? extends Changes> getModifier() {
        if (form != null)
            return getFormInstance();
        else
            return modifier;
    }

    public List<ClientAction> getActions() {
        return actions;
    }

    public void addAction(ClientAction action) {
        actions.add(action);
    }

    public void addActions(List<ClientAction> actions) {
        this.actions.addAll(actions);
    }

    public RemoteForm getRemoteForm() {
        return form;
    }

    public FormInstance<?> getFormInstance() {
        return form.form;
    }

    public Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> getObjectInstances() {
        return mapObjects;
    }

    public int getObjectInstanceCount() {
        return mapObjects.size();
    }

    public ObjectInstance getObjectInstance(ObjectEntity object) {
        return getFormInstance().instanceFactory.getInstance(object);
    }

    public PropertyObjectInterfaceInstance getObjectInstance(ClassPropertyInterface cls) {
        return mapObjects.get(cls);
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        return BaseUtils.singleValue(mapObjects);
    }

    public boolean isGroupLast() {
        return groupLast;
    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        if (form != null)
            return getFormInstance().addObject(cls);
        else
            return getSession().addObject(cls, getModifier());
    }

    public void applyChanges(BusinessLogics BL) throws SQLException {
        if (form != null)
            form.applyChanges(getActions());
        else
            getSession().apply(BL);
    }

    public void cancelChanges() throws SQLException {
        if (form != null)
            form.cancelChanges();
        else
            getSession().restart(true);
    }
}
