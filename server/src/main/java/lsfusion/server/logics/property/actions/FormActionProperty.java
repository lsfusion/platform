package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.AnyValuePropertyHolder;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.remote.RemoteForm;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends SystemExplicitActionProperty {

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

    protected boolean allowNulls() {
        return false;
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
