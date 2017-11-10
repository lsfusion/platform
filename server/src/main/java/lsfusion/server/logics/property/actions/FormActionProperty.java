package lsfusion.server.logics.property.actions;

import lsfusion.base.*;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;
import java.util.List;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public abstract class FormActionProperty<O extends ObjectSelector> extends SystemExplicitActionProperty {

    public final FormSelector<O> form;
    public final ImRevMap<O, ClassPropertyInterface> mapObjects;

    private static <O extends ObjectSelector> ValueClass[] getValueClasses(FormSelector<O> form, List<O> objects, Property... extraProps) {
        int extraPropInterfaces = 0;
        for(Property extraProp : extraProps)
            if(extraProp != null)
                extraPropInterfaces += extraProp.interfaces.size();

        int size = objects.size();
        ValueClass[] valueClasses = new ValueClass[size
                + extraPropInterfaces];
        for (int i = 0; i < size; i++) {
            valueClasses[i] = form.getBaseClass(objects.get(i));
        }

        for(Property extraProp : extraProps) 
            if(extraProp != null) {
                ImMap<PropertyInterface, ValueClass> interfaceClasses = extraProp.getInterfaceClasses(ClassType.formPolicy);
                ImOrderSet<PropertyInterface> propInterfaces = extraProp.getFriendlyPropertyOrderInterfaces();
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
                              final List<O> objectsToSet,
                              final List<Boolean> nulls, boolean extraNotNull,
                              Property... extraProps) {
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

    protected abstract void executeCustom(FormEntity<?> form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException;

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<O, ? extends ObjectValue> mapObjectValues = mapObjects.join(context.getKeys());
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> resolvedForm = form.getForm(context.getBL().LM, context.getSession(), mapObjectValues);
        if(resolvedForm == null)
            return;
        executeCustom(resolvedForm.first, resolvedForm.second.rightJoin(mapObjectValues), context, resolvedForm.second);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
