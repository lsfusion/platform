package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.interop.ModalityType;
import platform.interop.action.FormClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.action.ReportClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormCloseType;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.remote.RemoteForm;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends SystemActionProperty {

    public final FormEntity<?> form;
    public final ImRevMap<ObjectEntity, ClassPropertyInterface> mapObjects;
    private final ActionPropertyObjectEntity<?> startAction;
    public ActionPropertyObjectEntity<?> closeAction;
    public Set<ObjectEntity> seekOnOk = new HashSet<ObjectEntity>();
    private final boolean checkOnOk;
    private final FormSessionScope sessionScope;
    private final ModalityType modalityType;
    private final boolean showDrop;

    private final ConcreteCustomClass formResultClass;
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
    public FormActionProperty(String sID, String caption, FormEntity form, final ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, boolean showDrop, ConcreteCustomClass formResultClass, LCP formResultProperty, AnyValuePropertyHolder chosenValueProperty) {
        super(sID, caption, getValueClasses(objectsToSet));

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        this.modalityType = modalityType;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;
        this.sessionScope = sessionScope;
        this.startAction = startAction;

        mapObjects = getOrderInterfaces().mapOrderRevKeys(new GetIndex<ObjectEntity>() { // такой же дебилизм и в SessionDataProperty
            public ObjectEntity getMapValue(int i) {
                return objectsToSet[i];
            }
        });
        this.form = form;
    }

    protected boolean isVolatile() {
        return true;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        final FormInstance newFormInstance = context.createFormInstance(form, mapObjects.join(context.getKeys()), context.getSession(), modalityType.isModal(), sessionScope, checkOnOk, showDrop, !form.isPrintForm);

        if (form.isPrintForm && !newFormInstance.areObjectsFound()) {
            context.requestUserInteraction(
                    new MessageClientAction(ServerResourceBundle.getString("form.navigator.form.do.not.fit.for.specified.parameters"), form.caption));
        } else {
            for (int i = 0, size = mapObjects.size(); i < size; i++) {
                newFormInstance.forceChangeObject(newFormInstance.instanceFactory.getInstance(mapObjects.getKey(i)), context.getKeyValue(mapObjects.getValue(i)));
            }

            final FormInstance thisFormInstance = context.getFormInstance();

            if (startAction != null) {
                newFormInstance.instanceFactory.getInstance(startAction).execute(newFormInstance);
            }

            RemoteForm newRemoteForm = context.createRemoteForm(newFormInstance);
            if (form.isPrintForm) {
                context.requestUserInteraction(new ReportClientAction(form.getSID(), modalityType.isModal(), newRemoteForm.reportManager.getReportData()));
            } else {
                context.requestUserInteraction(new FormClientAction(newRemoteForm, modalityType));
            }

            if (modalityType.isModal()) {
                //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

                FormCloseType formResult = newFormInstance.getFormResult();

                if (formResultProperty != null) {
                    formResultProperty.change(formResultClass.getDataObject(formResult.asString()), context);
                }

                if (chosenValueProperty != null) {
                    for (GroupObjectEntity group : form.groups) {
                        for (ObjectEntity object : group.getObjects()) {
                            chosenValueProperty.write(
                                    object.baseClass.getType(), newFormInstance.instanceFactory.getInstance(object).getObjectValue(), context, new DataObject(object.getSID())
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
                    if (closeAction != null) {
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
}
