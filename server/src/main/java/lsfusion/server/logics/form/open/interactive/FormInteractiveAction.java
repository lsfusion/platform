package lsfusion.server.logics.form.open.interactive;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.interop.form.ContainerShowFormType;
import lsfusion.interop.form.ModalityShowFormType;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapOpenForm;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Consumer;

public class FormInteractiveAction<O extends ObjectSelector> extends FormAction<O> {

    // INPUT
    private final ImList<O> inputObjects;
    private final ImList<LP> inputProps;    
    private final ImList<Boolean> inputNulls;

    private final Boolean syncType;
    private final WindowFormType windowType;
    private final Integer inContainerId;

    private ShowFormType getShowFormType(boolean syncType) {
        if(inContainerId != null) {
            return new ContainerShowFormType(inContainerId);
        } else {
            WindowFormType windowType = this.windowType;
            if (windowType == null) {
                windowType = syncType ? WindowFormType.FLOAT : WindowFormType.DOCKED;
            }

            if (syncType) {
                switch (windowType) {
                    case FLOAT:
                        return inputObjects.isEmpty() ? ModalityShowFormType.MODAL : ModalityShowFormType.DIALOG_MODAL;
                    case DOCKED:
                        return ModalityShowFormType.DOCKED_MODAL;
                    case EMBEDDED:
                        return ModalityShowFormType.EMBEDDED;
                    case POPUP:
                        return ModalityShowFormType.POPUP;
                    default:
                        throw new UnsupportedOperationException();
                }
            } else
                return ModalityShowFormType.DOCKED;
        }
    }

    // SYSTEM ACTIONS
    private final ManageSessionType manageSession;
    private final Boolean noCancel;

    // NAVIGATOR
    private final Boolean forbidDuplicate;

    private final boolean readOnly;
    private final boolean checkOnOk;
    
    public <C extends PropertyInterface> FormInteractiveAction(LocalizedString caption,
                                                               FormSelector<O> form,
                                                               final ImList<O> objectsToSet, final ImList<Boolean> nulls,
                                                               ImList<O> inputObjects, ImList<LP> inputProps, ImList<Boolean> inputNulls,
                                                               ImOrderSet<C> orderInterfaces, ImSet<ContextFilterSelector<C, O>> contextFilters,
                                                               Consumer<ImRevMap<C, ClassPropertyInterface>> mapContext,
                                                               ManageSessionType manageSession,
                                                               Boolean noCancel,
                                                               Boolean syncType,
                                                               WindowFormType windowType,
                                                               Integer inContainerId,
                                                               boolean forbidDuplicate,
                                                               boolean checkOnOk,
                                                               boolean readOnly) {
        super(caption, form, objectsToSet, nulls, orderInterfaces, contextFilters, mapContext);

        this.inputObjects = inputObjects;
        this.inputProps = inputProps;
        this.inputNulls = inputNulls;
        assert !inputProps.containsNull();

        this.syncType = syncType;
        this.windowType = windowType;
        this.inContainerId = inContainerId;

        this.forbidDuplicate = forbidDuplicate;

        this.manageSession = manageSession;
        this.noCancel = noCancel;

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
        return context.hasMoreSessionUsages || ((formInstance = context.getFormInstance(false, false)) != null && formInstance.isModal()) || (windowType != null && windowType.isModal());
    }

    //todo: same as above
    private boolean heuristicSyncType() {
        return true;
    }

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapObjects, ImSet<ContextFilterInstance> contextFilters) throws SQLException, SQLHandledException {
        ImRevMap<O, ObjectEntity> mapRevObjects = mapObjects.reverse();

        // sync and modality types
        boolean syncType; 
        if(this.syncType != null)
            syncType = this.syncType;
        else
            syncType = heuristicSyncType(context);
        ShowFormType showFormType = getShowFormType(syncType);

        ImList<ObjectEntity> resolvedInputObjects = inputObjects.mapList(mapRevObjects);

        FormInstance newFormInstance = context.createFormInstance(form, resolvedInputObjects.getCol().toSet(), mapObjectValues, context.getSession(), syncType, noCancel, manageSession, checkOnOk, isShowDrop(), true, showFormType.getWindowType(), contextFilters, readOnly);
        context.requestFormUserInteraction(newFormInstance, showFormType, forbidDuplicate, context.stack);

        if (syncType) {
            FormCloseType formResult = newFormInstance.getFormResult();

            ImList<RequestResult> result = null;
            if(formResult != FormCloseType.CLOSE) {
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
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getRequestChangeExtProps(inputObjects.size(), i -> inputObjects.get(i).getType(), inputProps::get);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.FORMCHANGE) {
            if (!readOnly && !getForm().hasNoChange()) 
                return true;
        }
        if(type == ChangeFlowType.INTERACTIVEFORM)
            return true;
        if(type == ChangeFlowType.NEEDMORESESSIONUSAGES && syncType == null)
            return true;
        return super.hasFlow(type);
    }

//    @Override
//    public ImSet<Action> getDependActions() {
//        return getForm().getPropertyDrawsList().
//                filterOrder((PropertyDrawEntity element) -> element.getValueActionOrProperty() instanceof ActionObjectEntity).getSet().
//                mapSetValues(propertyDrawEntity -> (Action)propertyDrawEntity.getValueActionOrProperty().property);
//    }


    @Override
    public AsyncMapEventExec<ClassPropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        ShowFormType showFormType = getShowFormType();
        return new AsyncMapOpenForm<>(form, forbidDuplicate, showFormType.isModal(), showFormType.getWindowType(), null, mapObjects.size() == 1 ? mapObjects.singleValue() : null);
    }

    private ShowFormType getShowFormType() {
        boolean syncType;
        if(this.syncType != null)
            syncType = this.syncType;
        else
            syncType = heuristicSyncType();

        return getShowFormType(syncType);
    }
}
