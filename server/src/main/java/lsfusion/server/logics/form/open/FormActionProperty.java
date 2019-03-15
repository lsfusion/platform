package lsfusion.server.logics.form.open;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public abstract class FormActionProperty<O extends ObjectSelector> extends SystemExplicitAction {

    public final FormSelector<O> form;
    public final ImRevMap<O, ClassPropertyInterface> mapObjects;

    private static <O extends ObjectSelector> ValueClass[] getValueClasses(FormSelector<O> form, ImList<O> objects, ActionOrProperty... extraProps) {
        int extraPropInterfaces = 0;
        for(ActionOrProperty extraProp : extraProps)
            if(extraProp != null)
                extraPropInterfaces += extraProp.interfaces.size();

        int size = objects.size();
        ValueClass[] valueClasses = new ValueClass[size
                + extraPropInterfaces];
        for (int i = 0; i < size; i++) {
            valueClasses[i] = form.getBaseClass(objects.get(i));
        }

        for(ActionOrProperty extraProp : extraProps) 
            if(extraProp != null) {
                ImMap<PropertyInterface, ValueClass> interfaceClasses = extraProp.getInterfaceClasses(ClassType.formPolicy);
                ImOrderSet<PropertyInterface> propInterfaces = extraProp.getFriendlyOrderInterfaces();
                for (int i = 0; i < propInterfaces.size(); ++i) {
                    valueClasses[size + i] = interfaceClasses.get(propInterfaces.get(i));
                }
            }
        return valueClasses;
    }

    @Override
    protected boolean allowNulls() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean checkNulls(ImSet<ClassPropertyInterface> dataKeys) {
        return !dataKeys.containsAll(notNullInterfaces);
    }
    
    private final ImSet<ClassPropertyInterface> notNullInterfaces;

    //assert objects из form
    //assert getProperties одинаковой длины
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(LocalizedString caption,
                              FormSelector<O> form,
                              final ImList<O> objectsToSet,
                              final ImList<Boolean> nulls, boolean extraNotNull,
                              ActionOrProperty... extraProps) {
        super(caption, getValueClasses(form, objectsToSet, extraProps));

        ImOrderSet<ClassPropertyInterface> objectInterfaces = getOrderInterfaces()
                .subOrder(0, objectsToSet.size());

        this.form = form;
        mapObjects = objectInterfaces.mapOrderRevKeys(new GetIndex<O>() { // такой же дебилизм и в SessionDataProperty
                    public O getMapValue(int i) {
                        return objectsToSet.get(i);
                    }
                });
        ImSet<ClassPropertyInterface> notNullInterfaces = objectInterfaces.mapOrderValues(new GetIndex<Boolean>() {
            public Boolean getMapValue(int i) {
                return nulls.get(i);
            }
        }).filterFnValues(new SFunctionSet<Boolean>() {
            public boolean contains(Boolean element) {
                return !element;
            }
        }).keys();
        if(extraNotNull)
            notNullInterfaces = notNullInterfaces.addExcl(getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size()).getSet());
        this.notNullInterfaces = notNullInterfaces;
    }

    protected abstract void executeCustom(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException;

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<O, ? extends ObjectValue> mapObjectValues = mapObjects.join(context.getKeys());
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> resolvedForm = form.getForm(context.getBL().LM, context.getSession(), mapObjectValues);
        if(resolvedForm == null)
            return;
        executeCustom(resolvedForm.first, resolvedForm.second.rightJoin(mapObjectValues), context, resolvedForm.second);
    }

    @Override
    protected boolean isSync() {
        return true; // тут сложно посчитать что изменяется, поэтому пока просто считаем синхронным, чтобы не компилировался FOR
    }

    @IdentityLazy
    @Override    
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return mapObjects.filterFnRev(new SFunctionSet<O>() {
            public boolean contains(O element) {
                return element.noClasses();
            }
        }).valuesSet();
    }
}
