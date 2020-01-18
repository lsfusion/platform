package lsfusion.server.logics.form.open.interactive;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

public class FormInteractiveAction<O extends ObjectSelector> extends FormAction<O> {

    // INPUT
    private final ImList<O> inputObjects;
    private final ImList<LP> inputProps;    
    private final ImList<Boolean> inputNulls;

    private final Boolean syncType;
    private final WindowFormType windowType;
    
    private ModalityType getModalityType(boolean syncType) {
        WindowFormType windowType = this.windowType;
        if(windowType == null) {
            if(syncType)
                windowType = WindowFormType.FLOAT;
            else
                windowType = WindowFormType.DOCKED;
        }

        if(syncType) {
            if (windowType == WindowFormType.FLOAT) {
                if(!inputObjects.isEmpty())
                    return ModalityType.DIALOG_MODAL;
                return ModalityType.MODAL;
            } else {
                assert (windowType == WindowFormType.DOCKED);
                return ModalityType.DOCKED_MODAL;
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
    private final ImSet<ClassPropertyInterface> contextInterfaces;
    private final ImSet<ContextFilterSelector<?, ClassPropertyInterface, O>> contextFilters;

    private final boolean readOnly;
    private final boolean checkOnOk;
    
    @Override
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return super.getNoClassesInterfaces().addExcl(contextInterfaces);
    }

    @IdentityInstanceLazy
    @Override
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        PropertyMapImplement<?, ClassPropertyInterface> result = super.calcWhereProperty();
        if(!contextFilters.isEmpty()) { // filters don't stop form from showing, however they can be used for param classes, so we're using the same hack as in SystemAction
            PropertyMapImplement<?, ClassPropertyInterface> contextFilterWhere = PropertyFact.createUnion(interfaces, contextFilters.mapSetValues((Function<ContextFilterSelector<?, ClassPropertyInterface, O>, PropertyMapImplement<?, ClassPropertyInterface>>) filter -> filter.getWhereProperty(mapObjects)).addExcl(PropertyFact.createTrue()).toList());
            result = PropertyFact.createAnd(result, contextFilterWhere);
        }
        return result;
    }

    public <C extends PropertyInterface> FormInteractiveAction(LocalizedString caption,
                                                               FormSelector<O> form,
                                                               final ImList<O> objectsToSet, final ImList<Boolean> nulls,
                                                               ImList<O> inputObjects, ImList<LP> inputProps, ImList<Boolean> inputNulls,
                                                               ImOrderSet<C> orderInterfaces, ImList<ContextFilterSelector<?, C, O>> contextFilters,
                                                               ManageSessionType manageSession,
                                                               Boolean noCancel,
                                                               Boolean syncType,
                                                               WindowFormType windowType, boolean forbidDuplicate,
                                                               boolean checkOnOk,
                                                               boolean readOnly) {
        super(caption, form, objectsToSet, nulls, false, BaseUtils.genArray(null, orderInterfaces.size(), ValueClass[]::new));

        this.inputObjects = inputObjects;
        this.inputProps = inputProps;
        this.inputNulls = inputNulls;

        this.syncType = syncType;
        this.windowType = windowType;

        this.forbidDuplicate = forbidDuplicate;

        this.manageSession = manageSession;
        this.noCancel = noCancel;

        ImRevMap<C, ClassPropertyInterface> mapInterfaces = getMapInterfaces(orderInterfaces, objectsToSet.size()).reverse();
        this.contextInterfaces = mapInterfaces.valuesSet();
        this.contextFilters = contextFilters.mapListValues((Function<ContextFilterSelector<?, C, O>, ContextFilterSelector<?, ClassPropertyInterface, O>>) 
                                                filter -> filter.map(mapInterfaces)).toOrderExclSet().getSet();
        
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

    private boolean heuristicSyncType(ExecutionContext<ClassPropertyInterface> context) {
        FormInstance formInstance;
        return context.hasMoreSessionUsages || ((formInstance = context.getFormInstance(false, false)) != null && formInstance.isFloat());
    }

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapObjects) throws SQLException, SQLHandledException {
        ImRevMap<O, ObjectEntity> mapRevObjects = mapObjects.reverse();

        // context filters
        MExclSet<ContextFilterInstance> mContextFilters = SetFact.mExclSet();
        for(ContextFilterSelector<?, ClassPropertyInterface, O> contextFilter : this.contextFilters)
            mContextFilters.exclAddAll(contextFilter.getInstances(context.getKeys(), mapRevObjects));
        ImSet<ContextFilterInstance> contextFilters = mContextFilters.immutable();

        // sync and modality types
        boolean syncType; 
        if(this.syncType != null)
            syncType = this.syncType;
        else
            syncType = heuristicSyncType(context);
        ModalityType modalityType = getModalityType(syncType);

        FormInstance newFormInstance = context.createFormInstance(form, mapObjectValues, context.getSession(), syncType, noCancel, manageSession, checkOnOk, isShowDrop(), true, modalityType.isModal(), contextFilters, readOnly);
        context.requestFormUserInteraction(newFormInstance, modalityType, forbidDuplicate, context.stack);

        if (syncType) {
            FormCloseType formResult = newFormInstance.getFormResult();

            ImList<RequestResult> result = null;
            if(formResult != FormCloseType.CLOSE) {
                ImList<ObjectEntity> resolvedInputObjects = inputObjects.mapList(mapRevObjects);
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

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.FORMCHANGE) {
            if(!readOnly) { // если не read only
                FormEntity staticForm = form.getStaticForm();
                if(staticForm == null || !staticForm.hasNoChange()) // и форма не известна и может что-то изменять
                    return true;
            }
        }
        if(type == ChangeFlowType.NEEDMORESESSIONUSAGES && syncType == null)
            return true;
        return super.hasFlow(type);
    }
}
