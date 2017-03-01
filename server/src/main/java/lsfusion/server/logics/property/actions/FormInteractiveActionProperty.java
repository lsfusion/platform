package lsfusion.server.logics.property.actions;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.WindowFormType;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;
import java.util.List;

public class FormInteractiveActionProperty extends FormActionProperty {

    // INPUT
    private final List<ObjectEntity> inputObjects;
    private final List<LCP> inputProps;    
    private final List<Boolean> inputNulls;
    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;
    private final AnyValuePropertyHolder chosenValueProperty;

    private boolean isInput() {
        return inputObjects != null;
    }

    private final boolean syncType;
    private final WindowFormType windowType;
    
    private ModalityType getModalityType() {
        if(syncType) {
            if (windowType == WindowFormType.FLOAT)
                return ModalityType.MODAL;
            else if (windowType == WindowFormType.DOCKED)
                return ModalityType.DOCKED_MODAL;
            else {
                assert (windowType == WindowFormType.DIALOG);
                return ModalityType.DIALOG_MODAL;
            }
        } else
            return ModalityType.DOCKED;
    }

    // SYSTEM ACTIONS
    private final Boolean manageSession;
    private final boolean showDrop;
    private final boolean isAdd;

    // CONTEXT
    private final ObjectEntity contextObject;
    private final CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement;
    private final PropertyDrawEntity initFilterProperty;
    private final boolean readOnly;
    private final boolean checkOnOk;

    public FormInteractiveActionProperty(LocalizedString caption,
                                         FormEntity form,
                                         final List<ObjectEntity> objectsToSet, final List<Boolean> nulls,
                                         List<ObjectEntity> inputObjects, List<LCP> inputProps, List<Boolean> inputNulls,
                                         Boolean manageSession,
                                         boolean isAdd,
                                         boolean syncType,
                                         WindowFormType windowType,
                                         boolean checkOnOk,
                                         boolean showDrop,
                                         ConcreteCustomClass formResultClass,
                                         LCP formResultProperty,
                                         AnyValuePropertyHolder chosenValueProperty,
                                         ObjectEntity contextObject,
                                         CalcProperty contextProperty,
                                         PropertyDrawEntity initFilterProperty, boolean readOnly) {
        super(caption, form, objectsToSet, nulls, contextProperty);

        assert inputObjects == null || syncType; // если ввод, то синхронный
        this.inputObjects = inputObjects;
        this.inputProps = inputProps;
        this.inputNulls = inputNulls;

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        this.syncType = syncType;
        this.windowType = windowType;

        this.showDrop = showDrop;
        this.manageSession = manageSession;
        this.isAdd = isAdd;

        this.contextObject = contextObject;
        this.contextPropertyImplement = contextProperty == null ? null : contextProperty.getImplement(
                getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
        );
        this.initFilterProperty = initFilterProperty;
        this.readOnly = readOnly;
        this.checkOnOk = checkOnOk;
    }
    
    private boolean isShowDrop() {
        if (showDrop) // temporary
            return true;
        if (isInput()) {
            for(Boolean inputNull : inputNulls)
                if (inputNull)
                    return true;
        }
        return false;
    }

    protected boolean isVolatile() {
        return true;
    }

    @Override
    protected void executeCustom(ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<FilterEntity> contextFilters = null;
        if (contextPropertyImplement != null) {
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapObjectValues(context.getKeys());
            if(propertyValues == null) { // вообще должно \ может проверяться на уровне allowNulls, но он целиком для всех параметров, поэтому пока так
                proceedNullException();
                return;
            }
            final FormInstance thisFormInstance = context.getFormInstance(false, true);
            contextFilters = thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps);
        }

        FormInstance newFormInstance = context.createFormInstance(form, mapObjectValues, context.getSession(), syncType, isAdd, manageSession, checkOnOk, isShowDrop(), true, contextFilters, initFilterProperty, pullProps.result, readOnly);
        context.requestFormUserInteraction(newFormInstance, getModalityType(), context.stack);

        if (syncType) {
            //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

            FormCloseType formResult = newFormInstance.getFormResult();
            formResultProperty.change(formResultClass.getDataObject(formResult.asString()), context);

            for (GroupObjectEntity group : form.getGroupsIt()) {
                for (ObjectEntity object : group.getObjects()) {
                    chosenValueProperty.write(
                            object.baseClass.getType(), newFormInstance.instanceFactory.getInstance(object).getObjectValue(), context, new DataObject(object.getSID())
                    );
                }
            }

            if(isInput()) {
                for (int i = 0; i < inputObjects.size(); i++) {
                    ObjectEntity inputObject = inputObjects.get(i);
                    ObjectInstance object = newFormInstance.instanceFactory.getInstance(inputObject);

                    ObjectValue chosenValue = null;
                    if (formResult != FormCloseType.CLOSE)
                        chosenValue = (formResult == FormCloseType.OK ? object.getObjectValue() : NullValue.instance);

                    context.writeRequested(chosenValue, object.getType(), inputProps.get(i));
                }
            }
        }
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return !isAdd;
    }
}
