package lsfusion.server.logics.property.actions;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
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
    private final FormPrintType printType;

    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;

    private final AnyValuePropertyHolder chosenValueProperty;

    private final ObjectEntity contextObject;
    private final CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement;

    private final PropertyDrawEntity initFilterProperty;

    private static ValueClass[] getValueClasses(ObjectEntity[] objects, CalcProperty contextProperty) {
        ValueClass[] valueClasses = new ValueClass[objects.length + (contextProperty == null ? 0 : contextProperty.interfaces.size())];
        for (int i = 0; i < objects.length; i++) {
            valueClasses[i] = objects[i].baseClass;
        }

        if (contextProperty != null) {
            ImMap<PropertyInterface, ValueClass> interfaceClasses = contextProperty.getInterfaceClasses(ClassType.FULL);
            ImOrderSet<PropertyInterface> propInterfaces = contextProperty.getOrderInterfaces();
            for (int i = 0; i < propInterfaces.size(); ++i) {
                valueClasses[objects.length + i] = interfaceClasses.get(propInterfaces.get(i));
            }
        }

        return valueClasses;
    }

    //assert objects и startAction из form
    //assert getProperties и startAction одинаковой длины
    //startAction привязаны к созадаваемой форме
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(String caption,
                              FormEntity form,
                              final ObjectEntity[] objectsToSet,
                              ActionPropertyObjectEntity startAction,
                              FormSessionScope sessionScope,
                              ModalityType modalityType,
                              boolean checkOnOk,
                              boolean showDrop,
                              FormPrintType printType,
                              ConcreteCustomClass formResultClass,
                              LCP formResultProperty,
                              AnyValuePropertyHolder chosenValueProperty,
                              ObjectEntity contextObject,
                              CalcProperty contextProperty,
                              PropertyDrawEntity initFilterProperty) {
        super(caption, getValueClasses(objectsToSet, contextProperty));

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        this.modalityType = modalityType;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;
        this.printType = printType;
        this.sessionScope = sessionScope;
        this.startAction = startAction;

        this.contextObject = contextObject;
        this.initFilterProperty = initFilterProperty;

        this.contextPropertyImplement = contextProperty == null ? null : contextProperty.getImplement(
                getOrderInterfaces().subOrder(objectsToSet.length, interfaces.size())
        );

        mapObjects = getOrderInterfaces()
                .subOrder(0, objectsToSet.length)
                .mapOrderRevKeys(new GetIndex<ObjectEntity>() { // такой же дебилизм и в SessionDataProperty
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

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Result<ImSet<PullChangeProperty>> pullProps = new Result<ImSet<PullChangeProperty>>();
        ImSet<FilterEntity> contextFilters = null;
        if (contextPropertyImplement != null) {
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapValues(context.getDataKeys());
            final FormInstance thisFormInstance = context.getFormInstance();
            contextFilters = thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps);
        }

        final FormInstance newFormInstance = context.createFormInstance(form,
                                                                        mapObjects.join(context.getKeys()),
                                                                        context.getSession(),
                                                                        modalityType.isModal(),
                                                                        sessionScope,
                                                                        checkOnOk,
                                                                        showDrop,
                                                                        printType == null,
                                                                        contextFilters,
                                                                        initFilterProperty,
                                                                        pullProps.result);

        if (printType != null && !newFormInstance.areObjectsFound()) {
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
            if (printType != null) {
                context.requestUserInteraction(new ReportClientAction(form.getSID(), modalityType.isModal(), newRemoteForm.reportManager.getReportData(), printType, SystemProperties.isDebug));
            } else {
                context.requestUserInteraction(new FormClientAction(form.getCanonicalName(), form.getSID(), newRemoteForm, modalityType));
            }

            if (modalityType.isModal()) {
                //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

                FormCloseType formResult = newFormInstance.getFormResult();

                if (formResultProperty != null) {
                    formResultProperty.change(formResultClass.getDataObject(formResult.asString()), context);
                }

                if (chosenValueProperty != null) {
                    for (GroupObjectEntity group : form.getGroupsIt()) {
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
