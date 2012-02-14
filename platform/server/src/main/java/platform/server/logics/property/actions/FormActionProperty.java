package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
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

    private final StaticCustomClass formResultClass;
    private final LP formResultProperty;

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
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession, boolean isModal, StaticCustomClass formResultClass, LP formResultProperty) {
        super(sID, caption, getValueClasses(objectsToSet));

        this.valueClass = valueClass;
        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;

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
        final RemoteForm thisRemoteForm = context.getRemoteForm();
        final FormInstance thisFormInstance = context.getFormInstance();
        final FormInstance newFormInstance = thisFormInstance.createForm(form, BaseUtils.join(mapObjects, context.getKeys()), newSession, !form.isPrintForm);
        if (form.isPrintForm && !newFormInstance.areObjectsFounded()) {
            thisRemoteForm.requestUserInteraction(
                    new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
        } else {
            for (Map.Entry<ObjectEntity, ClassPropertyInterface> entry : mapObjects.entrySet()) {
                newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(entry.getKey()), context.getKeyValue(entry.getValue()));
            }

            if (form instanceof SelfInstancePostProcessor) {
                ((SelfInstancePostProcessor) form).postProcessSelfInstance(context.getKeys(), thisRemoteForm, newFormInstance);
            }

            final RemoteForm newRemoteForm = thisRemoteForm.createForm(newFormInstance);

            for (int i = 0; i < setProperties.length; i++) {
                newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(setProperties[i]),
                                               getProperties[i] != null ? getProperties[i].getValue(thisFormInstance.instanceFactory, context.getSession(), context.getModifier()) : context.getValueObject(),
                                               newRemoteForm, null);
            }

            thisRemoteForm.requestUserInteraction(new FormClientAction(form.isPrintForm, newSession, isModal, newRemoteForm));

            String formResult = newFormInstance.getFormResult();

            if (formResultProperty != null) {
                formResultProperty.execute(formResultClass.getID(formResult), context);
            }

            if (!seekOnOk.isEmpty() && "ok".equals(formResult)) {
                for (ObjectEntity object : seekOnOk) {
                    try {
                        ObjectInstance objectInstance = newFormInstance.instanceFactory.getInstance(object);
                        // нужна проверка, т.к. в принципе пока FormActionProperty может ссылаться на ObjectEntity из разных FormEntity
                        if (objectInstance != null) {
                            thisFormInstance.seekObject(object.baseClass, objectInstance.getObjectValue());
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (!closeProperties.isEmpty() && "close".equals(formResult)) {
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
    }

    public static interface SelfInstancePostProcessor {
        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, RemoteForm executeForm, FormInstance selfFormInstance);
    }

    @Override
    public DataClass getValueClass() {
        if (valueClass != null) {
            return valueClass;
        } else {
            return super.getValueClass();
        }
    }
}
