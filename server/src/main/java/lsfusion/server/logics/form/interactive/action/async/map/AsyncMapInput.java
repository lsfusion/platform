package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.file.AppImage;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.action.input.InputContextListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputContextPropertyListEntity;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Predicate;

public class AsyncMapInput<T extends PropertyInterface> extends AsyncMapValue<T> {

    public final InputContextListEntity<?, T> list;

    public final ImList<AsyncMapInputListAction<T>> actions;
    public final boolean strict;
    public final boolean multipleInput;

    public final boolean hasDrawOldValue;
    public final PropertyInterfaceImplement<T> oldValue;

    public final String customEditorFunction;

    public AsyncMapInput(DataClass type, InputContextListEntity<?, T> list, ImList<AsyncMapInputListAction<T>> actions, boolean strict, boolean multipleInput, boolean hasDrawOldValue, PropertyInterfaceImplement<T> oldValue, String customEditorFunction) {
        super(type);

        this.list = list;
        this.actions = actions; // the actions are never null (may be empty), unlike the list
        this.strict = strict;
        this.multipleInput = multipleInput;

        this.hasDrawOldValue = hasDrawOldValue;
        this.oldValue = oldValue;

        this.customEditorFunction = customEditorFunction;
    }

    public AsyncMapInput<T> override(String action, AsyncMapEventExec<T> asyncExec) {
        return new AsyncMapInput<>(type, list, actions.mapListValues(a -> a.replace(action, asyncExec)), strict, multipleInput, hasDrawOldValue, oldValue, customEditorFunction);
    }

    private <P extends PropertyInterface> AsyncMapInput<P> override(InputContextListEntity<?, P> list, ImList<AsyncMapInputListAction<P>> actions, PropertyInterfaceImplement<P> oldValue) {
        return new AsyncMapInput<>(type, list, actions, strict, multipleInput, hasDrawOldValue, oldValue, customEditorFunction);
    }

    public AsyncMapInput<T> newSession() {
        return override(list != null ? list.newSession() : null, actions, oldValue);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapInput<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null, actions.mapListValues(action -> action.map(mapping)), oldValue != null ? oldValue.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null, actions.mapListValues(action -> action.mapInner(mapping)), oldValue != null ? oldValue.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null, actions.mapListValues(action -> action.mapJoin(mapping)), oldValue instanceof PropertyInterface ? mapping.get((T)oldValue) : null);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?, ?> drawProperty, GroupObjectEntity toDraw) {
        if (hasDrawOldValue && !(
                oldValue instanceof PropertyMapImplement && drawProperty != null && context instanceof FormInstanceContext && drawProperty.isProperty((FormInstanceContext) context) &&
                ((PropertyMapImplement<?, T>) oldValue).mapEntityObjects(mapObjects).equalsMap(drawProperty.getAssertCellProperty((FormInstanceContext) context))))
            return null;
        return new AsyncInput(type, multipleInput, list != null ? new InputList(strict, list.isDisableInputList()) : null,
                filter(((FormInstanceContext) context).securityPolicy, securityProperty, actions.mapListValues(action -> action.map(mapObjects, (FormInstanceContext) context, securityProperty, drawProperty, toDraw)).toArray(new InputListAction[actions.size()])), customEditorFunction);
    }

    public static InputListAction[] filter(SecurityPolicy policy, ActionOrProperty securityProperty, InputListAction[] actions) {
        if (policy != null && actions != null) {
            for (int i = 0; i < actions.length; i++) {
                if (actions[i].id.equals(AppImage.INPUT_NEW)) {
                    Predicate<SecurityPolicy> check = actions[i].check;
                    if ((check != null && !check.test(policy)) || !policy.checkPropertyEditObjectsPermission(securityProperty)) {
                        return ArrayUtils.remove(actions, i);
                    }
                    break;
                }
            }
        }
        return actions;
    }

    @Override
    protected AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        throw new UnsupportedOperationException(); // the inputs are merged all at once (see the merge above), so the pairwise fold never gets to them
    }

    static <T extends PropertyInterface> AsyncMapEventExec<T> mergeInputs(ImList<AsyncMapInput<T>> inputs, ImList<PropertyInterfaceImplement<T>> wheres) {
        AsyncMapInput<T> first = inputs.get(0);

        DataClass<?> mergedType = first.type;
        boolean mergedMultipleInput = first.multipleInput;
        boolean mergedHasDrawOldValue = first.hasDrawOldValue;
        PropertyInterfaceImplement<T> mergedOldValue = first.oldValue;
        boolean sameOldValues = true;
        boolean sameLists = true;
        for(int i = 1, size = inputs.size(); i < size; i++) {
            AsyncMapInput<T> input = inputs.get(i);

            // everything the client builds the editor from has to match, since it is chosen before the executed branch is known
            if((first.list == null) != (input.list == null) || first.strict != input.strict || !BaseUtils.nullEquals(first.customEditorFunction, input.customEditorFunction))
                return null;

            sameLists = sameLists && (first.list == null || first.list.equalsList(input.list));

            mergedType = mergedType.getCompatible(input.type, true);
            if(mergedType == null)
                return null;

            mergedMultipleInput = mergedMultipleInput || input.multipleInput;
            mergedHasDrawOldValue = mergedHasDrawOldValue || input.hasDrawOldValue;
            // the branches that have no old value don't conflict with the ones that have it, but once two of them disagree there is no common old value anymore
            if(sameOldValues && input.oldValue != null) {
                if(mergedOldValue == null)
                    mergedOldValue = input.oldValue;
                else if(!mergedOldValue.equals(input.oldValue)) {
                    mergedOldValue = null;
                    sameOldValues = false;
                }
            }
        }

        ImList<AsyncMapInputListAction<T>> mergedActions = mergeActions(inputs, wheres);
        if(mergedActions == null)
            return null;

        InputContextListEntity<?, T> mergedList = first.list;
        if(first.list != null && !sameLists) {
            // the lists are combined under the branch conditions, so that a row gets the values of the branch that applies to it
            mergedList = InputContextPropertyListEntity.mergeBranches(inputs.mapListValues(input -> input.list), wheres);
            if(mergedList == null)
                return null;
        }

        return new AsyncMapInput<>(mergedType, mergedList, mergedActions, first.strict, mergedMultipleInput, mergedHasDrawOldValue, mergedOldValue, first.customEditorFunction);
    }

    // the actions are matched one by one, since the executed branch resolves the chosen action by its index (see InputAction.executeInternal)
    private static <T extends PropertyInterface> ImList<AsyncMapInputListAction<T>> mergeActions(ImList<AsyncMapInput<T>> inputs, ImList<PropertyInterfaceImplement<T>> wheres) {
        ImList<AsyncMapInputListAction<T>> actions = inputs.get(0).actions;
        for(int i = 1, size = inputs.size(); i < size; i++)
            if(inputs.get(i).actions.size() != actions.size())
                return null;

        MList<AsyncMapInputListAction<T>> mResult = ListFact.mList(actions.size());
        for(int i = 0, size = actions.size(); i < size; i++) {
            int index = i;
            AsyncMapInputListAction<T> mergedAction = AsyncMapInputListAction.merge(inputs.mapListValues(input -> input.actions.get(index)), wheres);
            if(mergedAction == null)
                return null;
            mResult.add(mergedAction);
        }
        return mResult.immutableList();
    }

    @Override
    public <X extends PropertyInterface> Pair<InputContextListEntity<X, T>, AsyncDataConverter<X>> getAsyncValueList(Result<String> value) {
        return new Pair<>((InputContextListEntity<X, T>) list, null);
    }

    public static AsyncMode getAsyncMode(boolean strict) {
        return strict ? AsyncMode.OBJECTVALUES : AsyncMode.VALUES;
    }
}
