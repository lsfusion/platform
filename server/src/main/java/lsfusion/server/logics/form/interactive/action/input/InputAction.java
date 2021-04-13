package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.SQLException;

// можно было бы Input и Form унаследовать от ABSTRACT, но так как наследование зависит от опции INPUT в Form это не совсем корректно
public class InputAction extends SystemExplicitAction {

    private final DataClass dataClass;
    private final LP<?> targetProp;
            
    //  используется только для событий поэтому по идее не надо, так как в событиях user activity быть не может
//    public ImMap<Property, Boolean> aspectChangeExtProps() {
//        return getChangeProps(requestCanceledProperty.property, requestedPropertySet.getLCP(dataClass).property);
//    }

    private ClassPropertyInterface oldValueInterface;
    // CONTEXT
    protected final ImSet<ClassPropertyInterface> contextInterfaces;
    protected final InputListEntity<?, ClassPropertyInterface> contextList;

    private static <O extends ObjectSelector> ValueClass[] getValueClasses(boolean hasOldValue, DataClass dataClass, int contextInterfaces) {
        return ArrayUtils.addAll(hasOldValue ? new ValueClass[]{dataClass} : new ValueClass[]{}, BaseUtils.genArray(null, contextInterfaces, ValueClass[]::new));
    }

    public <C extends PropertyInterface> InputAction(LocalizedString caption, DataClass dataClass, LP targetProp, boolean hasOldValue,
                                                     ImOrderSet<C> orderContextInterfaces, InputListEntity<?, C> contextList) {
        super(caption, getValueClasses(hasOldValue, dataClass, orderContextInterfaces.size()));

        this.dataClass = dataClass;
        this.targetProp = targetProp;
        assert targetProp != null;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();

        this.oldValueInterface = hasOldValue ? orderInterfaces.get(0) : null;

        ImRevMap<C, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(hasOldValue ? 1 : 0, orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        this.contextList = contextList != null ? contextList.map(mapContextInterfaces) : null;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        boolean hasOldValue = oldValueInterface != null;
        Object oldValue = hasOldValue ? context.getKeyObject(oldValueInterface) : null;
        ObjectValue userValue = context.inputUserData(dataClass, oldValue, hasOldValue, contextList);
        context.writeRequested(RequestResult.get(userValue, dataClass, targetProp));
    }
    
    @Override
    public SimpleRequestInput<ClassPropertyInterface> getSimpleRequestInput(boolean optimistic, boolean inRequest) {
        if(inRequest && oldValueInterface == null)
            return new SimpleDataInput<>(dataClass, contextList, targetProp);
        return null;
    }

//    FormInteractiveAction doesn't include contextFilters, so not sure that InputAction should
//    @Override
//    protected ImMap<Property, Boolean> aspectUsedExtProps() {
//        return super.aspectUsedExtProps();
//    }

    @IdentityInstanceLazy
    @Override
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        PropertyMapImplement<?, ClassPropertyInterface> result = super.calcWhereProperty();
        if(contextList != null) { // filters don't stop form from showing, however they can be used for param classes, so we're using the same hack as in SystemAction
            PropertyMapImplement<?, ClassPropertyInterface> contextFilterWhere = PropertyFact.createUnion(interfaces, ListFact.toList(IsClassProperty.getMapProperty(contextList.getInterfaceClasses()), PropertyFact.createTrue()));
            result = PropertyFact.createAnd(result, contextFilterWhere); // mix of FormAction and ExtendContextAction (since we need sort of "grouping" in list clause)
        }
        return result;
    }

    @Override
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return contextInterfaces;
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getRequestChangeExtProps(1, index -> dataClass, index -> targetProp);
    }
}
