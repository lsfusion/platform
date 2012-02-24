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
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.nullJoin;

public class ExecutionContext {
    private final Map<ClassPropertyInterface, DataObject> keys;
    private final ObjectValue value;
    private final DataSession session;
    private final Modifier modifier;
    private final List<ClientAction> actions;
    private final RemoteForm form;
    private final Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects;
    private final boolean groupLast; // обозначает, что изменение последнее, чтобы форма начинала определять, что изменилось

    private final boolean inFormSession;
    private final FormInstance formInstance;

    public ExecutionContext(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier modifier, List<ClientAction> actions, RemoteForm form, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) {
        this.keys = keys;
        this.value = value;
        this.session = session;
        this.modifier = modifier;
        this.actions = actions;
        this.form = form;
        this.mapObjects = mapObjects;
        this.groupLast = groupLast;

        this.formInstance = form != null ? form.form : null;
        this.inFormSession = form != null && formInstance.session == session;
    }

    public Map<ClassPropertyInterface, DataObject> getKeys() {
        return keys;
    }

    public DataObject getKeyValue(ClassPropertyInterface key) {
        return keys.get(key);
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
        return session;
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
        return formInstance;
    }

    public Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> getObjectInstances() {
        return mapObjects;
    }

    public ObjectInstance getObjectInstance(ObjectEntity object) {
        return formInstance.instanceFactory.getInstance(object);
    }

    public PropertyObjectInterfaceInstance getObjectInstance(ClassPropertyInterface cls) {
        return mapObjects.get(cls);
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        return mapObjects != null ? BaseUtils.singleValue(mapObjects) : null;
    }

    public boolean isGroupLast() {
        return groupLast;
    }

    public boolean isInFormSession() {
        return inFormSession;
    }

    public Modifier getModifier() {
        if (inFormSession) {
            return formInstance;
        } else {
            return modifier;
        }
    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        if (inFormSession) {
            return formInstance.addObject(cls);
        } else {
            return getSession().addObject(cls, modifier);
        }
    }

    public String applyChanges(BusinessLogics BL) throws SQLException {
        if (inFormSession) {
            form.applyChanges(null, actions);
            return null;
        } else {
            return getSession().apply(BL, actions);
        }
    }

    public void cancelChanges() throws SQLException {
        if (inFormSession) {
            form.cancelChanges();
        } else {
            session.restart(true);
        }
    }

    public ExecutionContext override(DataSession newSession) {
        return new ExecutionContext(keys, value, newSession, newSession.modifier, new ArrayList<ClientAction>(), form, mapObjects, groupLast);
    }

    public ExecutionContext override(Map<ClassPropertyInterface, DataObject> keys) {
        return new ExecutionContext(keys, value, session, modifier, actions, form, mapObjects, groupLast);
    }

    public ExecutionContext map(Map<ClassPropertyInterface, ClassPropertyInterface> map, ObjectValue value) {
        return new ExecutionContext(join(map, keys), value, session, modifier, actions, form, nullJoin(map, mapObjects), groupLast);
    }

    public void emitExceptionIfNotInFormSession() {
        if (!inFormSession) {
            throw new IllegalStateException("Property should only be used in form's session!");
        }
    }
}
