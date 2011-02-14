package platform.server.logics;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.action.ContinueAutoActionsClientAction;
import platform.interop.action.FormClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends ActionProperty {

    public final FormEntity form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final PropertyObjectEntity[] setProperties;
    private final PropertyObjectEntity[] getProperties;
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
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean isModal, boolean newSession) {
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

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm thisRemoteForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
        try {
            FormInstance thisFormInstance = thisRemoteForm.form;
            FormInstance newFormInstance = thisFormInstance.createForm(form, BaseUtils.join(mapObjects, keys), newSession);
            for (Map.Entry<ObjectEntity, ClassPropertyInterface> entry : mapObjects.entrySet()) {
                newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(entry.getKey()), keys.get(entry.getValue()));
            }

            RemoteForm newRemoteForm = thisRemoteForm.createForm(newFormInstance);

            for (int i = 0; i < setProperties.length; i++) {
                PropertyObjectInstance setPropInstance = newFormInstance.instanceFactory.getInstance(setProperties[i]);

                Object readenValue = null;
                if (getProperties[i] != null) {
                    PropertyObjectInstance getPropInstance = thisFormInstance.instanceFactory.getInstance(getProperties[i]);
                    readenValue = getPropInstance.read(thisFormInstance.session.sql, thisFormInstance.session.modifier, thisFormInstance.session.env);
                }

                newFormInstance.changeProperty(setPropInstance, readenValue, newRemoteForm);
            }

            actions.add(new FormClientAction(form.isPrintForm, isModal, newRemoteForm));
            actions.add(new ContinueAutoActionsClientAction());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
