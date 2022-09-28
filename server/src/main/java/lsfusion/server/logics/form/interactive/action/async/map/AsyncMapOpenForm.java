package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncOpenForm;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapOpenForm<T extends PropertyInterface> extends AsyncMapExec<T> {
    
    public final FormSelector formSelector;

    private final boolean forbidDuplicate;
    private final boolean modal;
    private final WindowFormType type;

    public final CustomClass propertyClass;
    public final T propertyInterface;

    public AsyncMapOpenForm(FormSelector formSelector, boolean forbidDuplicate, boolean modal, WindowFormType type, CustomClass propertyClass, T parameterInterface) {
        this.formSelector = formSelector;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
        this.type = type;
        this.propertyClass = propertyClass;
        this.propertyInterface = parameterInterface;
        assert propertyClass == null || propertyInterface == null;
    }

    private <P extends PropertyInterface> AsyncMapOpenForm<P> override(P propertyInterface) {
        return new AsyncMapOpenForm<P>(formSelector, forbidDuplicate, modal, type, propertyClass, propertyInterface);
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
                return new AsyncMapOpenForm<>(formSelector, forbidDuplicate, modal, type, (CustomClass)valueClass, null);
            mapJoin = null;
        }
        return override((P) mapJoin);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        
        CustomClass objectClass = propertyClass;
        if(propertyInterface != null) {
            ObjectEntity object = mapObjects.get(propertyInterface);
            if(object != null) {
                ValueClass objectBaseClass = object.baseClass;
                if (objectBaseClass instanceof CustomClass)
                    objectClass = (CustomClass) objectBaseClass;
            }
        }
        
        FormEntity staticForm = formSelector != null ? formSelector.getStaticForm(ThreadLocalContext.getBaseLM(), objectClass) : null;

        return new AsyncOpenForm(staticForm != null ? staticForm.getCanonicalName() : null, 
                                 staticForm != null ? staticForm.getAsyncCaption() : null, 
                                 forbidDuplicate, modal, type);
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapOpenForm))
            return null;

        AsyncMapOpenForm<T> asyncOpenForm = (AsyncMapOpenForm<T>) input;
        
        FormSelector mergedForm;
        if(formSelector == null)
            mergedForm = asyncOpenForm.formSelector;
        else if(asyncOpenForm.formSelector == null)
            mergedForm = formSelector;
        else
            mergedForm = formSelector.merge(asyncOpenForm.formSelector);

        CustomClass mergedClass;
        if(propertyClass == null)
            mergedClass = asyncOpenForm.propertyClass;
        else if(asyncOpenForm.propertyClass == null)
            mergedClass = propertyClass;
        else
            mergedClass = ClassFormSelector.merge(propertyClass, asyncOpenForm.propertyClass);
        
        return new AsyncMapOpenForm<>(mergedForm, forbidDuplicate || asyncOpenForm.forbidDuplicate, modal || asyncOpenForm.modal, type.getType() <= asyncOpenForm.type.getType() ? type : asyncOpenForm.type, mergedClass, BaseUtils.nullEquals(propertyInterface, asyncOpenForm.propertyInterface) ? propertyInterface : null);
    }

    @Override
    public int getMergeOptimisticPriority() {
        return 1;
    }
}
