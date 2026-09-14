package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.DockedWindowFormType;
import lsfusion.interop.form.FormActivateType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncOpenForm;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapOpenForm<T extends PropertyInterface> extends AsyncMapExec<T> {
    
    public final FormSelector formSelector;

    private final FormActivateType activateType;
    private final boolean modal;
    private final WindowFormType type;

    public final CustomClass propertyClass;
    public final T propertyInterface;

    public AsyncMapOpenForm(FormSelector formSelector, FormActivateType activateType, boolean modal, WindowFormType type, CustomClass propertyClass, T parameterInterface) {
        this.formSelector = formSelector;
        this.activateType = activateType;
        this.modal = modal;
        this.type = type;
        this.propertyClass = propertyClass;
        this.propertyInterface = parameterInterface;
        assert propertyClass == null || propertyInterface == null;
    }

    private <P extends PropertyInterface> AsyncMapOpenForm<P> override(P propertyInterface) {
        return new AsyncMapOpenForm<P>(formSelector, activateType, modal, type, propertyClass, propertyInterface);
    }
    
    @Override
    public AsyncMapOpenForm<T> newSession() {
        return this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapOpenForm<P> map(ImRevMap<T, P> mapping) {
        if(propertyInterface == null)
            return (AsyncMapOpenForm<P>) this;

        return override(mapping.get(propertyInterface));
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        if(propertyInterface == null)
            return (AsyncMapOpenForm<P>) this;

        return override(mapping.get(propertyInterface));
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        if(propertyInterface == null)
            return (AsyncMapOpenForm<P>) this;

        PropertyInterfaceImplement<P> mapJoin = mapping.get(propertyInterface);
        if (mapJoin instanceof PropertyMapImplement) {
            ValueClass valueClass = ((PropertyMapImplement<?, P>) mapJoin).property.getValueClass(ClassType.tryEditPolicy);
            if(valueClass instanceof CustomClass)
                return new AsyncMapOpenForm<>(formSelector, activateType, modal, type, (CustomClass)valueClass, null);
            mapJoin = null;
        }
        return override((P) mapJoin);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?, ?> drawProperty, GroupObjectEntity toDraw) {
        
        CustomClass objectClass = propertyClass;
        if(propertyInterface != null) {
            ObjectEntity object = mapObjects.get(propertyInterface);
            if(object != null) {
                ValueClass objectBaseClass = object.baseClass;
                if (objectBaseClass instanceof CustomClass)
                    objectClass = (CustomClass) objectBaseClass;
            }
        }
        
        FormEntity staticForm = formSelector != null ? formSelector.getStaticForm(ThreadLocalContext.getBusinessLogics(), objectClass) : null;

        return new AsyncOpenForm(staticForm != null ? staticForm.getCanonicalName() : null, 
                                 staticForm != null ? staticForm.getLocalizedCaption() : null,
                                 staticForm != null ? staticForm.getImage(context) : null,
                                 activateType, modal, type);
    }

    @Override
    protected AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        assert input.getClass() == getClass();

        AsyncMapOpenForm<T> asyncOpenForm = (AsyncMapOpenForm<T>) input;
        
        // the form is the point of the pre-opening, so without a common one there is nothing to pre-open : the client would get a window that never gets its form and stays loading
        // (it also keeps the merge order independent - a failed merge can't be "filled" by the form of the next branch)
        if(formSelector == null || asyncOpenForm.formSelector == null)
            return null;
        FormSelector mergedForm = formSelector.merge(asyncOpenForm.formSelector);
        if(mergedForm == null)
            return null;

        CustomClass mergedClass;
        if(propertyClass == null)
            mergedClass = asyncOpenForm.propertyClass;
        else if(asyncOpenForm.propertyClass == null)
            mergedClass = propertyClass;
        else
            mergedClass = ClassFormSelector.merge(propertyClass, asyncOpenForm.propertyClass);
        
        WindowFormType mergedType = mergeType(type, asyncOpenForm.type);
        if(mergedType == null)
            return null;

        return new AsyncMapOpenForm<>(mergedForm, mergeActivateType(activateType, asyncOpenForm.activateType), modal || asyncOpenForm.modal, mergedType, mergedClass, BaseUtils.nullEquals(propertyInterface, asyncOpenForm.propertyInterface) ? propertyInterface : null);
    }

    // the branch that would reuse an open form wins, the way OR-ing the flag this replaced did. Returning null for
    // branches that disagree would NOT mean "nothing is predicted": a failed merge falls back to one branch's own
    // prediction (Action.getBranchAsyncEventExec), which may well be the branch that does not reuse. Predicting the
    // reuse is the safe direction: a client that reuses early only confirms the open with this address, a branch
    // that opens without ACTIVATE does not match that confirmation and opens as usual, and an arrival looks for a
    // duplicate by itself either way
    private static FormActivateType mergeActivateType(FormActivateType activateType, FormActivateType other) {
        if(activateType == null)
            return other;
        if(other == null)
            return activateType;
        return activateType.ordinal() >= other.ordinal() ? activateType : other;
    }

    // the type byte doubles as a priority - the lower one wins - which is enough while a type is only a kind. A docked
    // open also carries WHERE it goes, and two branches that name different windows have no common destination, so
    // they do not merge: the branch fallback (Action.getBranchAsyncEventExec) predicts one of them, and when the form
    // arrives for the other window, the placeholder put in the predicted one is closed on arrival
    private static WindowFormType mergeType(WindowFormType type, WindowFormType other) {
        if(type instanceof DockedWindowFormType && other instanceof DockedWindowFormType
                && !((DockedWindowFormType) type).window.equals(((DockedWindowFormType) other).window))
            return null;
        return type.getType() <= other.getType() ? type : other;
    }

    @Override
    public int getOptimisticPriority() {
        return 1;
    }
}
