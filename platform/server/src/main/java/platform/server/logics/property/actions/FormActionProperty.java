package platform.server.logics.property.actions;

import platform.interop.ModalityType;
import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.action.ReportClientAction;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormCloseType;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends SystemActionProperty {

    public final FormEntity<?> form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final ActionPropertyObjectEntity<?> startAction;
    public ActionPropertyObjectEntity<?> closeAction;
    public Set<ObjectEntity> seekOnOk = new HashSet<ObjectEntity>();
    private final boolean checkOnOk;
    private final FormSessionScope sessionScope;
    private final ModalityType modalityType;

    private final StaticCustomClass formResultClass;
    private final LCP formResultProperty;

    private final AnyValuePropertyHolder chosenValueProperty;

    public static ValueClass[] getValueClasses(ObjectEntity[] objects) {
        ValueClass[] valueClasses = new ValueClass[objects.length];
        for (int i = 0; i < objects.length; i++) {
            valueClasses[i] = objects[i].baseClass;
        }
        return valueClasses;
    }

    //assert objects и startAction из form
    //assert getProperties и startAction одинаковой длины
    //startAction привязаны к созадаваемой форме
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity setProperties, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, StaticCustomClass formResultClass, LCP formResultProperty, AnyValuePropertyHolder chosenValueProperty) {
        super(sID, caption, getValueClasses(objectsToSet));

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        this.modalityType = modalityType;
        this.checkOnOk = checkOnOk;
        this.sessionScope = sessionScope;
        this.startAction = setProperties;

        int i = 0; // такой же дебилизм и в SessionDataProperty
        mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (ClassPropertyInterface propertyInterface : interfaces) {
            mapObjects.put(objectsToSet[i++], propertyInterface);
        }
        this.form = form;
    }

    protected boolean isVolatile() {
        return true;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        final FormInstance newFormInstance = context.createFormInstance(form, join(mapObjects, context.getKeys()), context.getSession(), modalityType.isModal(), sessionScope, checkOnOk, !form.isPrintForm);

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

            if(startAction !=null)
                newFormInstance.instanceFactory.getInstance(startAction).execute(newFormInstance);


            RemoteForm newRemoteForm = context.createRemoteForm(newFormInstance);
            if (form.isPrintForm) {
                context.requestUserInteraction(new ReportClientAction(form.getSID(), modalityType.isModal(), newRemoteForm.reportManager.getReportData()));
            } else {
                context.requestUserInteraction(new FormClientAction(newRemoteForm, modalityType));
            }

            if (!modalityType.isModal()) {
                assert sessionScope.isManageSession();
            } else {
                //для немодальных форм следующее бессмысленно, т.к. они ещё открыты...

                FormCloseType formResult = newFormInstance.getFormResult();

                if (formResultProperty != null) {
                    formResultProperty.change(formResultClass.getID(formResult.asString()), context);
                }

                if (chosenValueProperty != null) {
                    for (GroupObjectEntity group : form.groups) {
                        for (ObjectEntity object : group.objects) {
                            chosenValueProperty.write(
                                    object.baseClass.getType(), newFormInstance.instanceFactory.getInstance(object).getObjectValue().getValue(), context, new DataObject(object.getSID())
                            );
                        }
                    }
                }

                if (formResult == FormCloseType.OK) {
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
                if (formResult == FormCloseType.CLOSE) {
                    if(closeAction !=null) {
                        try {
                            newFormInstance.instanceFactory.getInstance(closeAction).execute(newFormInstance);
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
    }

    public static interface SelfInstancePostProcessor {
        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, FormInstance executeForm, FormInstance selfFormInstance);
    }
}
