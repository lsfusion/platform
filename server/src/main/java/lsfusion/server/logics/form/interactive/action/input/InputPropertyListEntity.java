package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.AppImage;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;
import java.util.Collections;

public class InputPropertyListEntity<P extends PropertyInterface, V extends PropertyInterface> extends InputListEntity<P, V, Property<P>> {

    protected final ImOrderMap<InputOrderEntity<?, V>, Boolean> orders;

    public InputPropertyListEntity(Property<P> property, ImRevMap<P, V> mapValues) {
        this(property, MapFact.EMPTYORDER(), mapValues, false);
    }

    public InputPropertyListEntity(Property<P> property, ImOrderMap<InputOrderEntity<?, V>, Boolean> orders, ImRevMap<P, V> mapValues, boolean newSession) {
        super(property, mapValues, newSession);

        // we'll have to patch with params if there are some extra params in order - to keep this structure: Property<P> and map to its params
        // todo: but for now we'll just ignore such orders
        // otherwise we'll have to do all map operations more complex
        this.orders = orders.filterOrder(order -> mapValues.valuesSet().containsAll(order.mapValues.valuesSet()));
    }

    public <X extends PropertyInterface> PropertyMapImplement<P, X> getProperty(ImRevMap<V, X> map, X objectInterface) {
        return new PropertyMapImplement<>(property, mapValues.join(map).addRevExcl(singleInterface(), objectInterface));
    }

    public ActionMapImplement<?, V> getAsyncUpdateAction(BaseLogicsModule lm, PropertyMapImplement<?, V> targetProp) {
        ImRevMap<P, PropertyInterfaceImplement<V>> mapIntValues = BaseUtils.immutableCast(mapValues);
        return PropertyFact.createIfAction(SetFact.EMPTY(), PropertyFact.createNot(lm.getRequestCanceledProperty().getImplement()), // IF NOT requestCanceled()
                PropertyFact.createJoinAction(lm.addAsyncUpdateAProp().getActionOrProperty(), // ASYNCUPDATE
                        // list(requestedProperty(), ...)
                        PropertyFact.createJoin(new PropertyImplement<>(property, MapFact.addExcl(mapIntValues, singleInterface(), targetProp)))), null);
    }

    public <X extends PropertyInterface> InputContextAction<?, V> getNewEditAction(BaseLogicsModule baseLM, ConcreteCustomClass baseClass, LP targetProp, FormSessionScope scope) {
        LP<P> lp = new LP<>(property);
        ImOrderSet<P> listInterfaces = lp.listInterfaces;
        P singleInterface = singleInterface();

        int contextParams = property.interfaces.size();
        int singleIndex = listInterfaces.indexOf(singleInterface) + 1; // object interface, have to be replaced with newIndex, and in set action it's gonna be a string interface

        LA<X> newEdit = (LA<X>) baseLM.addNewEditAction(baseClass, targetProp, contextParams, scope,
                BaseUtils.add(BaseUtils.add(lp, ActionOrPropertyUtils.getIntParams(lp, singleIndex, contextParams + 1)), // remapping single interface to the new object
                        singleIndex)); // replacing property with the string

        return new InputContextAction<>(AppServerImage.ADD, AppImage.INPUT_NEW, "INSERT", Collections.singletonMap("editing", BindingMode.ONLY), null, QuickAccess.EMPTY, newEdit.getActionOrProperty(),
                listInterfaces.mapSet(newEdit.listInterfaces).removeRev(singleInterface).crossJoin(mapValues));
    }

    public ObjectValue readObject(ExecutionContext<V> context, ObjectValue userValue) throws SQLException, SQLHandledException {
        ImMap<V, ? extends ObjectValue> map = context.getKeys();
        return FormInstance.getAsyncKey(map(map), context.getSession(), context.getModifier(), userValue);
    }

    public <J extends PropertyInterface, X extends PropertyInterface> InputPropertyListEntity<?, V> merge(Pair<InputFilterEntity<?, V>, ImOrderMap<InputOrderEntity<?, V>, Boolean>> filterAndOrders) {
        if(filterAndOrders == null)
            return null;

        InputFilterEntity<?, V> filter = filterAndOrders.first;
        ImOrderMap<InputOrderEntity<?, V>, Boolean> orders = filterAndOrders.second;

        if(filter == null && orders.isEmpty()) // it's not only the optimization, but also needed for the singleInterface assertion (and all the rest assertions)
            return this;

        assert singleInterface() != null;

        InputFilterEntity<J, V> mergeFilter = (InputFilterEntity<J, V>) InputFilterEntity.and(getInputFilterEntity(), filter);

        return new InputPropertyListEntity<>(mergeFilter.property, orders, mergeFilter.mapValues, newSession);
    }

    public InputFilterEntity<P, V> getInputFilterEntity() {
        return new InputFilterEntity<>(property, mapValues);
    }
    public ImOrderMap<InputOrderEntity<?, V>, Boolean> getInputOrderEntities() {
        return orders;
    }

    public DataClass getDataClass() {
        return (DataClass) property.getType();
    }

    public ImSet<V> getUsedInterfaces() {
        return mapValues.valuesSet();
    }

    public boolean isValueUnique(ImRevMap<V, StaticParamNullableExpr> listParamExprs) {
        if(!Property.isDefaultWYSInput(property.getValueClass(ClassType.typePolicy)))
            return false;

        ImMap<P, StaticParamNullableExpr> fixedExprs = mapValues.join(listParamExprs);
        return property.isValueUnique(fixedExprs, Property.ValueUniqueType.INPUT);
    }

    public <C extends PropertyInterface> InputPropertyListEntity<P, C> mapProperty(ImRevMap<V, C> map) {
        return map(map);
    }

    public InputPropertyListEntity<P, V> newSession() {
        return new InputPropertyListEntity<>(property, orders, mapValues, true);
    }

    protected  <C extends PropertyInterface> InputPropertyListEntity<P, C> create(ImRevMap<P, C> joinMapValues) {
        ImRevMap<V, C> map = mapValues.crossJoin(joinMapValues);
        return new InputPropertyListEntity<>(property, orders.mapOrderKeys(order -> order.map(map)), joinMapValues, newSession);
    }

    protected  <C extends PropertyInterface> InputPropertyListEntity<JoinProperty.Interface, C> createJoin(ImMap<P, PropertyInterfaceImplement<C>> mappedValues) {
        Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, C>> joinImplement = PropertyFact.createPartJoin(new PropertyImplement<>(property, mappedValues));
        return new InputPropertyListEntity<>(joinImplement.first, MapFact.EMPTYORDER(), joinImplement.second, newSession);
    }

    @Override
    public <C extends PropertyInterface> InputPropertyListEntity<P, C> map(ImRevMap<V, C> map) {
        return (InputPropertyListEntity<P, C>) super.map(map);
    }

    public InputPropertyValueList<P> map(ImMap<V, ? extends ObjectValue> map) {
        return new InputPropertyValueList<>(property, getOrderImplements(), BaseUtils.immutableCast(mapValues.join(map)));
    }

    @IdentityInstanceLazy
    private <X extends PropertyInterface> ImOrderMap<PropertyInterfaceImplement<P>, Boolean> getOrderImplements() {
        return BaseUtils.<ImOrderMap<InputOrderEntity<X, V>, Boolean>>immutableCast(orders).mapOrderKeys(
                order -> new PropertyMapImplement<>(order.property, MapFact.addRevExcl(order.mapValues.innerCrossValues(mapValues), order.singleInterface(), singleInterface())));
    }
}
