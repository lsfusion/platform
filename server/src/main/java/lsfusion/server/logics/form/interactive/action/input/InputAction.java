package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.AppImage;
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
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInputListAction;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
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

    private final ClassPropertyInterface oldValueInterface;
    // CONTEXT
    protected final ImSet<ClassPropertyInterface> contextInterfaces;
    
    protected final InputListEntity<?, ClassPropertyInterface, ?> contextList;
    protected final InputContextSelector<ClassPropertyInterface> contextSelector;
    protected final ImList<InputContextAction<?, ClassPropertyInterface>> contextActions; // + value param
    private final String customChangeFunction;

    private static ValueClass[] getValueClasses(boolean hasOldValue, ValueClass valueClass, int contextInterfaces) {
        return ArrayUtils.addAll(hasOldValue ? new ValueClass[]{valueClass} : new ValueClass[]{}, BaseUtils.genArray(null, contextInterfaces, ValueClass[]::new));
    }

    public <C extends PropertyInterface> InputAction(LocalizedString caption, ValueClass valueClass, LP targetProp, boolean hasOldValue,
                                                     ImOrderSet<C> orderContextInterfaces, InputListEntity<?, C, ?> contextList, InputContextSelector<C> contextSelector, ImList<InputContextAction<?, C>> contextActions, String customChangeFunction) {
        super(caption, getValueClasses(hasOldValue, valueClass, orderContextInterfaces.size()));

        this.valueClass = valueClass;
        this.targetProp = targetProp;
        assert targetProp != null && targetProp.listInterfaces.isEmpty();
        this.customChangeFunction = customChangeFunction;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();

        if(hasOldValue) {
            this.oldValueInterface = orderInterfaces.get(0);
            orderInterfaces = orderInterfaces.subOrder(1, orderInterfaces.size());
        } else
            this.oldValueInterface = null;

        ImRevMap<C, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(0, orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        assert valueClass instanceof DataClass || ((InputPropertyListEntity<?, C>)contextList).singleInterface() != null;
        assert (contextList == null) == (contextSelector == null);
        assert !(contextList instanceof InputActionListEntity && contextSelector.getFilterAndOrders().first != null);
        this.contextList = contextList != null ? contextList.map(mapContextInterfaces) : null;
        this.contextSelector = contextSelector != null ? contextSelector.map(mapContextInterfaces) : null;
        
        this.contextActions = contextActions.mapListValues((InputContextAction<?, C> value) -> value.map(mapContextInterfaces));
    }

    @IdentityInstanceLazy
    private InputListEntity<?, ClassPropertyInterface, ?> mergeFullContextList() {
        if(contextList instanceof InputPropertyListEntity)
            return ((InputPropertyListEntity<?, ClassPropertyInterface>) contextList).merge(contextSelector.getFilterAndOrders());

        return contextList;
    }
    private InputListEntity<?, ClassPropertyInterface, ?> getFullContextList() {
        if(contextList == null) {
            assert contextSelector == null;
            return null;
        }

        assert contextSelector != null;
        return mergeFullContextList();
    }

    private DataClass getInputClass() {
        return valueClass instanceof DataClass ? (DataClass) valueClass : ((InputPropertyListEntity<?, ClassPropertyInterface>)getFullContextList()).getDataClass();
    }

    private ImList<AsyncMapInputListAction<ClassPropertyInterface>> getActions() {
        return contextActions.mapListValues((i, value) -> new AsyncMapInputListAction<>(value.image, value.id, value.getAsyncEventExec(), value.keyStroke, value.bindingModesMap, value.priority, value.quickAccessList, i));
    }

    private boolean isStrict() {
        return !(valueClass instanceof DataClass);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        boolean hasOldValue = oldValueInterface != null;
        Object oldValue = hasOldValue ? context.getKeyObject(oldValueInterface) : null;

        InputResult userValue = context.inputUserData(getInputClass(), oldValue, hasOldValue, getFullContextList(), customChangeFunction, getInputList(), getInputListActions(context.getRemoteContext()));

        Integer contextAction;
        if(userValue != null && (contextAction = userValue.contextAction) != null)
            contextActions.get(contextAction).execute(context, userValue.value);
        else {
            ImList<RequestResult> requestResults = null;
            if(userValue != null) {
                ObjectValue value = userValue.value;
                if (!(valueClass instanceof DataClass))
                    value = ((InputPropertyListEntity<?, ClassPropertyInterface>) getFullContextList()).readObject(context, value);
                requestResults = RequestResult.get(value, valueClass.getType(), targetProp);
            }
            context.writeRequested(requestResults);
        }
    }

    @IdentityInstanceLazy
    private InputList getInputList() {
        return new InputList(isStrict());
    }

    private InputListAction[] getInputListActions(ConnectionContext context) {
        ImList<AsyncMapInputListAction<ClassPropertyInterface>> actions = getActions();
        return actions.mapListValues(action -> action.map(context)).toArray(new InputListAction[actions.size()]);
    }

    @Override
    public AsyncMapEventExec<ClassPropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        boolean hasOldValue = !optimistic && oldValueInterface != null;
        return new AsyncMapInput<>(getInputClass(), getFullContextList(), getActions(), isStrict(), hasOldValue, hasOldValue ? oldValueInterface : null, customChangeFunction);
    }

//    FormInteractiveAction doesn't include contextFilters, so not sure that InputAction should
//    @Override
//    protected ImMap<Property, Boolean> aspectUsedExtProps() {
//        return super.aspectUsedExtProps();
//    }

    @Override
    public ImSet<Action> getDependActions() {
        return BaseUtils.immutableCast(getDependContextActions().mapListValues((InputContextAction<?, ClassPropertyInterface> value) -> value.action).getCol().toSet());
    }

    // in theory should be replaced, but it's used only
//    @Override
//    public ActionMapImplement<?, ClassPropertyInterface> replace(ActionReplacer replacer) {
//        return super.replace(replacer);
//    }

    // we're removing the "new" action (just like in FormInteractiveAction all property dependendencies are not include), because it has global changes, which "break" for example FORMCHANGE flow checks (and forms with SELECTOR get MANAGESESSION for auto management sessions)
    protected ImList<InputContextAction<?, ClassPropertyInterface>> getDependContextActions() {
        return contextActions.filterList(element -> !element.id.equals(AppImage.INPUT_NEW));
    }

    @IdentityInstanceLazy
    @Override
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        PropertyMapImplement<?, ClassPropertyInterface> result = super.calcWhereProperty();
        InputListEntity<?, ClassPropertyInterface, ?> fullContextList = getFullContextList(); // should be called after everything is initialized
        if(fullContextList != null) { // filters don't stop form from showing, however they can be used for param classes, so we're using the same hack as in SystemAction
            ImList<PropertyMapImplement<?, ClassPropertyInterface>> contextList = getDependContextActions().mapListValues((InputContextAction<?, ClassPropertyInterface> value) -> IsClassProperty.getMapProperty(value.getInterfaceClasses()));
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
        if(type == ChangeFlowType.INTERACTIVEWAIT)
            return true;
        return super.hasFlow(type);
    }
}
