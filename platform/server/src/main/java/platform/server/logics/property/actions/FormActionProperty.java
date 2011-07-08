package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.action.ContinueAutoActionsClientAction;
import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends ActionProperty {

    public final FormEntity form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final PropertyObjectEntity[] setProperties;
    private final OrderEntity[] getProperties;
    private final boolean newSession;
    private final boolean isModal;

    public static ValueClass[] getValueClasses(ObjectEntity[] objects) {
        ValueClass[] valueClasses = new ValueClass[objects.length];
        for (int i = 0; i < objects.length; i++) {
            valueClasses[i] = objects[i].baseClass;
        }
        return valueClasses;
    }

    //assert objects и setProperties из form
    //assert getProperties и setProperties одинаковой длины
    //setProperties привязаны к созадаваемой форме
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession, boolean isModal) {
        super(sID, caption, getValueClasses(objectsToSet));

        assert setProperties.length == getProperties.length;

        this.isModal = isModal;
        this.newSession = newSession;
        this.setProperties = setProperties;
        this.getProperties = getProperties;

        int i = 0; // такой же дебилизм и в SessionDataProperty
        mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (ClassPropertyInterface propertyInterface : interfaces) {
            mapObjects.put(objectsToSet[i++], propertyInterface);
        }
        this.form = form;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm thisRemoteForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects, boolean groupLast) {
        try {
            FormInstance thisFormInstance = thisRemoteForm.form;
            FormInstance newFormInstance = thisFormInstance.createForm(form, BaseUtils.join(mapObjects, keys), newSession, !form.isPrintForm);
            if(form.isPrintForm && !newFormInstance.areObjectsFounded()) {
                actions.add(new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
            } else {
               for (Map.Entry<ObjectEntity, ClassPropertyInterface> entry : mapObjects.entrySet()) {
                    newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(entry.getKey()), keys.get(entry.getValue()));
                }

                if (form instanceof SelfInstancePostProcessor) {
                    ((SelfInstancePostProcessor) form).postProcessSelfInstance(keys, thisRemoteForm, newFormInstance);
                }

                RemoteForm newRemoteForm = thisRemoteForm.createForm(newFormInstance);

                for (int i = 0; i < setProperties.length; i++) {
                    newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(setProperties[i]),
                                                   getProperties[i] != null ? getProperties[i].getValue(thisFormInstance.instanceFactory, session, modifier) : null,
                                                   newRemoteForm, null);
                }

                actions.add(new FormClientAction(form.isPrintForm, newSession, isModal, newRemoteForm));
            }
            actions.add(new ContinueAutoActionsClientAction());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface SelfInstancePostProcessor {
        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, RemoteForm executeForm, FormInstance selfFormInstance);
    }
}
