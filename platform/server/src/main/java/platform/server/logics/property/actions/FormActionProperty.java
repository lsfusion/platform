package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.FormEventType;
import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.listener.FormEventListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.*;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends CustomActionProperty {

    public final FormEntity form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final PropertyObjectEntity[] setProperties;
    private final OrderEntity[] getProperties;
    public List<PropertyObjectEntity> closeProperties = new ArrayList<PropertyObjectEntity>();
    public Set<ObjectEntity> seekOnOk = new HashSet<ObjectEntity>();
    private DataClass valueClass;
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
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession, boolean isModal) {
        super(sID, caption, getValueClasses(objectsToSet));

        this.valueClass = valueClass;

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

    public void execute(ExecutionContext context) throws SQLException {
        final FormInstance thisFormInstance = context.getFormInstance();
        final FormInstance newFormInstance = thisFormInstance.createForm(form, BaseUtils.join(mapObjects, context.getKeys()), newSession, !form.isPrintForm);
        if(form.isPrintForm && !newFormInstance.areObjectsFounded()) {
            context.getRemoteForm().requestUserInteraction(
                    new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
//            context.addAction(new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
        } else {
            for (Map.Entry<ObjectEntity, ClassPropertyInterface> entry : mapObjects.entrySet()) {
                newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(entry.getKey()), context.getKeyValue(entry.getValue()));
            }

            if (form instanceof SelfInstancePostProcessor) {
                ((SelfInstancePostProcessor) form).postProcessSelfInstance(context.getKeys(), context.getRemoteForm(), newFormInstance);
            }

            final RemoteForm newRemoteForm = context.getRemoteForm().createForm(newFormInstance);

            for (int i = 0; i < setProperties.length; i++) {
                newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(setProperties[i]),
                                               getProperties[i] != null ? getProperties[i].getValue(thisFormInstance.instanceFactory, context.getSession(), context.getModifier()) : context.getValueObject(),
                                               newRemoteForm, null);
            }

            if (!seekOnOk.isEmpty() || !closeProperties.isEmpty()) {
                newFormInstance.addEventListener(new FormEventListener() {
                    @Override
                    public void handleEvent(Object event) {
                        if (event.equals(FormEventType.OK)) {
                            for (ObjectEntity object : seekOnOk) {
                                try {
                                    ObjectInstance objectInstance = newFormInstance.instanceFactory.getInstance(object);
                                    if (objectInstance != null) // в принципе пока FormActionProperty может ссылаться на ObjectEntity из разных FormEntity
                                    thisFormInstance.seekObject(object.baseClass, objectInstance.getObjectValue());
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if (event.equals(FormEventType.CLOSE)) {
                            for (PropertyObjectEntity property : closeProperties) {
                                try {
                                    newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(property),
                                                                   true,
                                                                   newRemoteForm, null);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }

            context.getRemoteForm().requestUserInteraction(new FormClientAction(form.isPrintForm, newSession, isModal, newRemoteForm));
        }
    }

    public static interface SelfInstancePostProcessor {
        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, RemoteForm executeForm, FormInstance selfFormInstance);
    }

    @Override
    public DataClass getValueClass() {
        if (valueClass != null)
            return valueClass;
        else
            return super.getValueClass();
    }
}
