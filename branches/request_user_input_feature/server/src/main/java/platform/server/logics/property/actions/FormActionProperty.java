package platform.server.logics.property.actions;

import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.form.entity.*;
import platform.server.form.instance.FormCloseType;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends CustomReadValueActionProperty {

    public final FormEntity<?> form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final PropertyObjectEntity[] setProperties;
    private final OrderEntity[] getProperties;
    public List<PropertyObjectEntity> closeProperties = new ArrayList<PropertyObjectEntity>();
    public Set<ObjectEntity> seekOnOk = new HashSet<ObjectEntity>();
    private DataClass valueClass;
    private final boolean checkOnOk;
    private final boolean newSession;
    private final boolean isModal;

    private final StaticCustomClass formResultClass;
    private final LP formResultProperty;

    private final AnyValuePropertyHolder chosenValueProperty;

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
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession, boolean isModal, boolean checkOnOk, StaticCustomClass formResultClass, LP formResultProperty, AnyValuePropertyHolder chosenValueProperty) {
        super(sID, caption, getValueClasses(objectsToSet));

        this.valueClass = valueClass;
        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        assert setProperties.length == getProperties.length;

        this.isModal = isModal;
        this.checkOnOk = checkOnOk;
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

    protected Type getReadType(ExecutionContext context) {
        return valueClass;
    }

    public void executeRead(ExecutionContext context, Object userValue) throws SQLException {

        final FormInstance newFormInstance = context.createFormInstance(form, join(mapObjects, context.getKeys()), context.getSession(), newSession, !form.isPrintForm);

        if (form.isPrintForm && !newFormInstance.areObjectsFound()) {
            context.requestUserInteraction(
                    new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
        } else {
            for (Map.Entry<ObjectEntity, ClassPropertyInterface> entry : mapObjects.entrySet()) {
                newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(entry.getKey()), context.getKeyValue(entry.getValue()));
            }

            final FormInstance thisFormInstance = context.getFormInstance();
            if(thisFormInstance!=null) {
                if (form instanceof SelfInstancePostProcessor) {
                    ((SelfInstancePostProcessor) form).postProcessSelfInstance(context.getKeys(), thisFormInstance, newFormInstance);
                }
            }

            for (int i = 0; i < setProperties.length; i++) {
                Object setValue = getProperties[i] != null && thisFormInstance!=null ?
                                  getProperties[i].getValue(thisFormInstance.instanceFactory, context.getSession(), context.getModifier())
                                  : userValue;
                newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(setProperties[i]), setValue, null);
            }

            context.requestUserInteraction(
                    new FormClientAction(form.isPrintForm, newSession, isModal,
                            context.createRemoteForm(newFormInstance, checkOnOk))
            );

            FormCloseType formResult = newFormInstance.getFormResult();

            if (formResultProperty != null) {
                formResultProperty.execute(formResultClass.getID(formResult.asString()), context);
            }

            if (chosenValueProperty != null) {
                for (GroupObjectEntity group : form.groups) {
                    for (ObjectEntity object : group.objects) {
                        chosenValueProperty.write(
                                object.baseClass, newFormInstance.instanceFactory.getInstance(object).getObjectValue().getValue(), context, new DataObject(object.getSID())
                        );
                    }
                }
            }

            if (!seekOnOk.isEmpty() && formResult == FormCloseType.OK) {
                for (ObjectEntity object : seekOnOk) {
                    try {
                        ObjectInstance objectInstance = newFormInstance.instanceFactory.getInstance(object);
                        // нужна проверка, т.к. в принципе пока FormActionProperty может ссылаться на ObjectEntity из разных FormEntity
                        if (objectInstance != null) {
                            thisFormInstance.expandCurrentGroupObject(object.baseClass);
                            thisFormInstance.forceChangeObject(object.baseClass, objectInstance.getObjectValue());
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (!closeProperties.isEmpty() && formResult == FormCloseType.CLOSE) {
                for (PropertyObjectEntity property : closeProperties) {
                    try {
                        newFormInstance.changeProperty(newFormInstance.instanceFactory.getInstance(property), true, null);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (thisFormInstance != null) {
                //обновляем текущую форму, чтобы подхватить изменения из вызываемой формы
                thisFormInstance.refreshData();
            }
        }
    }

    public static interface SelfInstancePostProcessor {
        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, FormInstance executeForm, FormInstance selfFormInstance);
    }
}
