package lsfusion.server.logics.property.actions;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.ContextFilter;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;

import java.sql.SQLException;

public class FormInteractiveActionProperty<O extends ObjectSelector> extends FormActionProperty<O> {

    // INPUT
    private final ImList<O> inputObjects;
    private final ImList<LCP> inputProps;    
    private final ImList<Boolean> inputNulls;

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
    private final ManageSessionType manageSession;
    private final Boolean noCancel;

    // NAVIGATOR
    private final Boolean forbidDuplicate;

    // CONTEXT
    private final ImList<O> contextObjects;
    private final ImList<CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface>> contextPropertyImplements;
    private final boolean readOnly;
    private final boolean checkOnOk;

    public FormInteractiveActionProperty(LocalizedString caption,
                                         FormSelector<O> form,
                                         final ImList<O> objectsToSet, final ImList<Boolean> nulls,
                                         ImList<O> inputObjects, ImList<LCP> inputProps, ImList<Boolean> inputNulls,
                                         ImList<O> contextObjects, ImList<CalcProperty> contextProperties,
                                         ManageSessionType manageSession,
                                         Boolean noCancel,
                                         boolean syncType,
                                         WindowFormType windowType, boolean forbidDuplicate,
                                         boolean checkOnOk,
                                         boolean readOnly) {
        super(caption, form, objectsToSet, nulls, true, contextProperties.toArray(new CalcProperty[contextProperties.size()]));

        assert inputObjects.isEmpty() || syncType; // если ввод, то синхронный
        this.inputObjects = inputObjects;
        this.inputProps = inputProps;
        this.inputNulls = inputNulls;

        this.syncType = syncType;
        this.windowType = windowType;

        this.forbidDuplicate = forbidDuplicate;

        this.manageSession = manageSession;
        this.noCancel = noCancel;

        this.contextObjects = contextObjects;
        this.contextPropertyImplements = contextProperties.mapListValues(new GetValue<CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface>, CalcProperty>() {
            public CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> getMapValue(CalcProperty value) {
                return value.getImplement(
                        getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
                );
            }
        });
        
        this.readOnly = readOnly;
        this.checkOnOk = checkOnOk;
    }
    
    private boolean isShowDrop() {
        for(Boolean inputNull : inputNulls)
            if (inputNull)
                return true;
        return false;
    }

    protected boolean isSync() {
        return true;
    }

    @Override
    protected void executeCustom(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        ImRevMap<O, ObjectEntity> mapRevResolvedObjects = mapResolvedObjects.reverse();

        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<ContextFilter> contextFilters = getContextFilters(context, pullProps, mapRevResolvedObjects);
        
        FormInstance newFormInstance = context.createFormInstance(form, mapObjectValues, context.getSession(), syncType, noCancel, manageSession, checkOnOk, isShowDrop(), true, contextFilters, pullProps.result, readOnly);
        context.requestFormUserInteraction(newFormInstance, getModalityType(), forbidDuplicate, context.stack);

        if (syncType) {
            //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

            FormCloseType formResult = newFormInstance.getFormResult();

            ImList<RequestResult> result = null;
            if(formResult != FormCloseType.CLOSE) {
                ImList<ObjectEntity> resolvedInputObjects = inputObjects.mapList(mapRevResolvedObjects);
                MList<RequestResult> mResult = ListFact.mList(resolvedInputObjects.size());
                for (int i = 0; i < resolvedInputObjects.size(); i++) {
                    ObjectEntity inputObject = resolvedInputObjects.get(i);
                    ObjectInstance object = newFormInstance.instanceFactory.getInstance(inputObject);

                    ObjectValue chosenValue = (formResult == FormCloseType.OK ? object.getObjectValue() : NullValue.instance);
                    mResult.add(new RequestResult(chosenValue, object.getType(), inputProps.get(i)));
                }
                result = mResult.immutableList();
            }
            context.writeRequested(result);
        }
    }

    private ImSet<ContextFilter> getContextFilters(ExecutionContext<ClassPropertyInterface> context, Result<ImSet<PullChangeProperty>> pullProps, ImRevMap<O, ObjectEntity> mapResolvedObjects) {
        MSet<ContextFilter> mContextFilters = SetFact.mSet();
        ImList<ObjectEntity> resolvedContextObjects = contextObjects.mapList(mapResolvedObjects);
        for(int i=0,size=resolvedContextObjects.size();i<size;i++) {
            ObjectEntity contextObject = resolvedContextObjects.get(i);
            CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement = contextPropertyImplements.get(i);
            
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapObjectValues(context.getKeys());
            assert propertyValues != null; // extraNotNull - true
            final FormInstance thisFormInstance = context.getFormInstance(false, true);
            mContextFilters.addAll(thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps));
        }
        return mContextFilters.immutable();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.FORMCHANGE) {
            if(!readOnly) { // если не read only
                FormEntity staticForm = form.getStaticForm();
                if(staticForm == null || !staticForm.hasNoChange()) // и форма не известна и может что-то изменять
                    return true;
            }
        }
        return super.hasFlow(type);
    }
}
