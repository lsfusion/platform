package lsfusion.server.logics.property.actions;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
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
    private final ImList<ObjectEntity> inputObjects;
    private final ImList<LCP> inputProps;    
    private final ImList<Boolean> inputNulls;
    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;
    private final AnyValuePropertyHolder chosenValueProperty;

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
    private final ImList<ObjectEntity> contextObjects;
    private final ImList<CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface>> contextPropertyImplements;
    private final PropertyDrawEntity initFilterProperty;
    private final boolean readOnly;
    private final boolean checkOnOk;

    public FormInteractiveActionProperty(LocalizedString caption,
                                         FormEntity form,
                                         final List<ObjectEntity> objectsToSet, final List<Boolean> nulls,
                                         ImList<ObjectEntity> inputObjects, ImList<LCP> inputProps, ImList<Boolean> inputNulls,
                                         ImList<ObjectEntity> contextObjects, ImList<CalcProperty> contextProperties,
                                         Boolean manageSession,
                                         boolean isAdd,
                                         boolean syncType,
                                         WindowFormType windowType,
                                         boolean checkOnOk,
                                         boolean showDrop,
                                         ConcreteCustomClass formResultClass,
                                         LCP formResultProperty,
                                         AnyValuePropertyHolder chosenValueProperty,
                                         PropertyDrawEntity initFilterProperty, boolean readOnly) {
        super(caption, form, objectsToSet, nulls, true, contextProperties.toArray(new CalcProperty[contextProperties.size()]));

        assert inputObjects.isEmpty() || syncType; // если ввод, то синхронный
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

        this.contextObjects = contextObjects;
        this.contextPropertyImplements = contextProperties.mapListValues(new GetValue<CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface>, CalcProperty>() {
            public CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> getMapValue(CalcProperty value) {
                return value.getImplement(
                        getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
                );
            }
        });
        
        this.initFilterProperty = initFilterProperty;
        this.readOnly = readOnly;
        this.checkOnOk = checkOnOk;
    }
    
    private boolean isShowDrop() {
        if (showDrop) // temporary
            return true;
        for(Boolean inputNull : inputNulls)
            if (inputNull)
                return true;
        return false;
    }

    protected boolean isVolatile() {
        return true;
    }

    @Override
    protected void executeCustom(ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<FilterEntity> contextFilters = getContextFilters(context, pullProps);
        
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

            ImList<RequestResult> result = null;
            if(formResult != FormCloseType.CLOSE) {
                MList<RequestResult> mResult = ListFact.mList(inputObjects.size());
                for (int i = 0; i < inputObjects.size(); i++) {
                    ObjectEntity inputObject = inputObjects.get(i);
                    ObjectInstance object = newFormInstance.instanceFactory.getInstance(inputObject);

                    ObjectValue chosenValue = (formResult == FormCloseType.OK ? object.getObjectValue() : NullValue.instance);
                    mResult.add(new RequestResult(chosenValue, object.getType(), inputProps.get(i)));
                }
                result = mResult.immutableList();
            }
            context.writeRequested(result);
        }
    }

    private ImSet<FilterEntity> getContextFilters(ExecutionContext<ClassPropertyInterface> context, Result<ImSet<PullChangeProperty>> pullProps) {
        MSet<FilterEntity> mContextFilters = SetFact.mSet();
        for(int i=0,size=contextObjects.size();i<size;i++) {
            ObjectEntity contextObject = contextObjects.get(i);
            CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement = contextPropertyImplements.get(i);
            
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapObjectValues(context.getKeys());
            assert propertyValues != null; // extraNotNull - true
            final FormInstance thisFormInstance = context.getFormInstance(false, true);
            mContextFilters.addAll(thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps));
        }
        return mContextFilters.immutable();
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return !isAdd;
    }
}
