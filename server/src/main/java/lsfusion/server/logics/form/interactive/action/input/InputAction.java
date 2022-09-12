package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInputList;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInputListAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.SQLException;

// now it's a mix of SystemExplicitAction (if hasOldValue) and ExtendContextAction (the same can be said about FormAction)
// contextActions can be implemented the other way - with case and list actions, but in that case async optimizations would be impossible
public class InputAction extends SystemExplicitAction {
    
    private final ValueClass valueClass;
    private final LP<?> targetProp;
            
    //  используется только для событий поэтому по идее не надо, так как в событиях user activity быть не может
//    public ImMap<Property, Boolean> aspectChangeExtProps() {
//        return getChangeProps(requestCanceledProperty.property, requestedPropertySet.getLCP(dataClass).property);
//    }

    private ClassPropertyInterface oldValueInterface;
    // CONTEXT
    protected final ImSet<ClassPropertyInterface> contextInterfaces;
    
    protected final InputListEntity<?, ClassPropertyInterface> contextList;
    protected final InputContextSelector<ClassPropertyInterface> contextSelector;
    protected final ImList<InputContextAction<?, ClassPropertyInterface>> contextActions; // + value param
    private final String customChangeFunction;

    private static ValueClass[] getValueClasses(boolean hasOldValue, ValueClass valueClass, int contextInterfaces) {
        return ArrayUtils.addAll(hasOldValue ? new ValueClass[]{valueClass} : new ValueClass[]{}, BaseUtils.genArray(null, contextInterfaces, ValueClass[]::new));
    }

    public <C extends PropertyInterface> InputAction(LocalizedString caption, ValueClass valueClass, LP targetProp, boolean hasOldValue,
                                                     ImOrderSet<C> orderContextInterfaces, InputListEntity<?, C> contextList, InputContextSelector<C> contextSelector, ImList<InputContextAction<?, C>> contextActions, String customChangeFunction) {
        super(caption, getValueClasses(hasOldValue, valueClass, orderContextInterfaces.size()));

        this.valueClass = valueClass;
        this.targetProp = targetProp;
        assert targetProp != null && targetProp.listInterfaces.isEmpty();
        this.customChangeFunction = customChangeFunction;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();

        this.oldValueInterface = hasOldValue ? orderInterfaces.get(0) : null;

        ImRevMap<C, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(hasOldValue ? 1 : 0, orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        this.contextList = contextList != null ? contextList.map(mapContextInterfaces) : null;
        assert valueClass instanceof DataClass || contextList.singleInterface() != null;
        this.contextSelector = contextSelector != null ? contextSelector.map(mapContextInterfaces) : null;
        
        this.contextActions = contextActions.mapListValues((InputContextAction<?, C> value) -> value.map(mapContextInterfaces));
    }

    @IdentityInstanceLazy
    private InputListEntity<?, ClassPropertyInterface> mergeFullContextList() {
        InputListEntity<?, ClassPropertyInterface> result = this.contextList;
        if(contextSelector != null)
            result = result.merge(contextSelector.getFilterAndOrders());
        return result;
    }
    private InputListEntity<?, ClassPropertyInterface> getFullContextList() {
        if(contextList == null)
            return null;

        if(contextSelector == null)
            return contextList;

        return mergeFullContextList();
    }

    private DataClass getInputClass(InputListEntity<?, ClassPropertyInterface> fullContextList) {
        return valueClass instanceof DataClass ? (DataClass) valueClass : fullContextList.getDataClass();
    }

    private AsyncMapInputList<ClassPropertyInterface> getMapInputList() {
        return new AsyncMapInputList<ClassPropertyInterface>(
                contextActions.mapListValues(value -> new AsyncMapInputListAction<>(value.image, value.getAsyncEventExec(), value.keyStroke, value.bindingModesMap, value.priority, value.quickAccessList)),
                !(valueClass instanceof DataClass));
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        boolean hasOldValue = oldValueInterface != null;
        Object oldValue = hasOldValue ? context.getKeyObject(oldValueInterface) : null;

        InputListEntity<?, ClassPropertyInterface> fullContextList = getFullContextList();
        InputResult userValue = context.inputUserData(getInputClass(fullContextList), oldValue, hasOldValue, fullContextList, customChangeFunction, getMapInputList().map());

        Integer contextAction;
        if(userValue != null && (contextAction = userValue.contextAction) != null)
            contextActions.get(contextAction).execute(context, userValue.value);
        else {
            ImList<RequestResult> requestResults = null;
            if(userValue != null) {
                ObjectValue value = userValue.value;
                if (!(valueClass instanceof DataClass))
                    value = fullContextList.readObject(context, value);
                requestResults = RequestResult.get(value, valueClass.getType(), targetProp);
            }
            context.writeRequested(requestResults);
        }
    }

    @Override
    public AsyncMapEventExec<ClassPropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        if (optimistic || oldValueInterface == null) {
            InputListEntity<?, ClassPropertyInterface> fullContextList = getFullContextList();
            return new AsyncMapInput<>(getInputClass(fullContextList), fullContextList, getMapInputList(), customChangeFunction);
        }
        return null;
    }

//    FormInteractiveAction doesn't include contextFilters, so not sure that InputAction should
//    @Override
//    protected ImMap<Property, Boolean> aspectUsedExtProps() {
//        return super.aspectUsedExtProps();
//    }

    @Override
    public ImSet<Action> getDependActions() {
        return BaseUtils.immutableCast(contextActions.mapListValues((InputContextAction<?, ClassPropertyInterface> value) -> value.action).getCol().toSet());
    }

    // in theory should be replaced, but it's used only
//    @Override
//    public ActionMapImplement<?, ClassPropertyInterface> replace(ActionReplacer replacer) {
//        return super.replace(replacer);
//    }

    @IdentityInstanceLazy
    @Override
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        PropertyMapImplement<?, ClassPropertyInterface> result = super.calcWhereProperty();
        InputListEntity<?, ClassPropertyInterface> fullContextList = getFullContextList(); // should be called after everything is initialized
        if(fullContextList != null) { // filters don't stop form from showing, however they can be used for param classes, so we're using the same hack as in SystemAction
            ImList<PropertyMapImplement<?, ClassPropertyInterface>> contextList = contextActions.mapListValues((InputContextAction<?, ClassPropertyInterface> value) -> IsClassProperty.getMapProperty(value.getInterfaceClasses()));
            result = PropertyFact.createAnd(result, PropertyFact.createUnion(interfaces, ListFact.add(contextList, ListFact.toList(IsClassProperty.getMapProperty(fullContextList.getInterfaceClasses()), PropertyFact.createTrue())))); // mix of FormAction and ExtendContextAction (since we need sort of "grouping" in list clause)
        }
        return result;
    }

    @Override
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return contextInterfaces;
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getRequestChangeExtProps(1, index -> valueClass.getType(), index -> targetProp);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.INPUT)
            return true;
        return super.hasFlow(type);
    }
}
