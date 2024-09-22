package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;
import java.util.function.Function;

public class InputContextPropertyListEntity<P extends PropertyInterface, V extends PropertyInterface> implements InputContextListEntity<P, V> {

    protected final InputPropertyListEntity<P, V> view;

    protected final InputFilterEntity<?, V> filter;

    protected final ImOrderMap<InputOrderEntity<?, V>, Boolean> orders;

    public InputContextPropertyListEntity(InputPropertyListEntity<P, V> view) {
        this.view = view;
        this.filter = null;
        this.orders = null;
    }

    @Override
    public ImSet<V> getValues() {
        ImSet<V> viewValues = view.mapValues.valuesSet();
        if(filter == null)
            return viewValues;

        MSet<V> mResult = SetFact.mSet(viewValues);
        mResult.addAll(filter.mapValues.valuesSet());
        for(InputOrderEntity<?, V> order : orders.keyIt())
            mResult.addAll(order.mapValues.valuesSet());
        return mResult.immutable();
    }

    @Override
    public ImMap<V, ValueClass> getInterfaceClasses() {
        return getFullFilter().getInterfaceClasses();
    }

    public InputContextPropertyListEntity(InputPropertyListEntity<P, V> view, InputFilterEntity<?, V> filter, ImOrderMap<InputOrderEntity<?, V>, Boolean> orders) {
        this.view = view;
        this.filter = filter;
        this.orders = orders;
        assert (filter != null) == (orders != null);
    }

    public DataClass getDataClass() {
        return view.getDataClass();
    }

    public boolean isNewSession() {
        return view.newSession;
    }

    public ObjectValue readObject(ExecutionContext<V> context, ObjectValue userValue) throws SQLException, SQLHandledException {
        return FormInstance.getAsyncKey(map(context), context.getSession(), context.getModifier(), userValue);
    }

    @Override
    public InputPropertyValueList<P> map(ExecutionContext<V> context) {
        return (InputPropertyValueList<P>) InputContextListEntity.super.map(context);
    }

    public InputPropertyValueList<?> map() {
        return map(MapFact.EMPTY(), MapFact.EMPTY());
    }

    @Override
    public InputPropertyValueList<?> map(ImMap<V, PropertyObjectInterfaceInstance> outerMapping, Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter) {
        return (InputPropertyValueList<?>) InputContextListEntity.super.map(outerMapping, valuesGetter);
    }

    private static class Compiled<C extends PropertyInterface, V extends PropertyInterface> {
        private final ImRevMap<C, V> mapValues;
        private final C singleInterface;

        private final PropertyMapImplement<?, C> view;
        private final ImOrderMap<PropertyInterfaceImplement<C>, Boolean> orders;

        public Compiled(ImRevMap<C, V> mapValues, C singleInterface, PropertyMapImplement<?, C> view, ImOrderMap<PropertyInterfaceImplement<C>, Boolean> orders) {
            this.mapValues = mapValues;
            this.singleInterface = singleInterface;
            this.view = view;
            this.orders = orders;
        }

        public InputPropertyValueList<?> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects) {
            return new InputPropertyValueList<C>(mapValues.keys().addExcl(singleInterface), view, orders, (ImMap<C, ObjectValue>) mapValues.join(map), mapValues.join(mapObjects));
        }
    }

    private static <X extends PropertyInterface, V extends PropertyInterface, O extends PropertyInterface> PropertyMapImplement<O, X> mapOrder(InputOrderEntity<O, V> order, ImRevMap<X, V> mapValues, X singleInterface) {
        return new PropertyMapImplement<>(order.property, MapFact.addRevExcl(order.mapValues.innerCrossValues(mapValues), order.singleInterface(), singleInterface));
    }

    private static <P extends PropertyInterface, V extends PropertyInterface, X extends PropertyInterface> PropertyMapImplement<X, P> mapFilter(InputFilterEntity<X, V> filter, ImRevMap<P, V> mapValues, P singleInterface) {
        return new PropertyMapImplement<>(filter.property, MapFact.addRevExcl(filter.mapValues.innerCrossValues(mapValues), filter.singleInterface(), singleInterface));
    }

    @IdentityInstanceLazy
    private InputFilterEntity<?, V> getFullFilter() {
        InputFilterEntity<P, V> viewFilter = new InputFilterEntity<>(view.property, view.mapValues);
        if(filter == null)
            return viewFilter;

        return InputFilterEntity.and(viewFilter, filter);
    }

    @IdentityInstanceLazy
    private <C extends PropertyInterface> Compiled<C, V> compile() {
        ImSet<V> usedValues = getValues();
        ImRevMap<C, V> mappedValues = usedValues.mapRevKeys(() -> (C) new PropertyInterface<>());
        C singleInterface = (C) new PropertyInterface();

        return new Compiled<>(mappedValues, singleInterface,
                mapFilter(getFullFilter(), mappedValues, singleInterface),
                orders.mapOrderKeys(order -> mapOrder(order, mappedValues, singleInterface)));
    }

    public InputPropertyValueList<?> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects) {
        if(filter == null)
            return view.map(map, mapObjects);

        return compile().map(map, mapObjects);
    }

    @Override
    public <C extends PropertyInterface> InputContextListEntity<P, C> map(ImRevMap<V, C> map) {
        return new InputContextPropertyListEntity<>(view.map(map), filter != null ? filter.map(map) : null, orders != null ? orders.mapOrderKeys(order -> order.map(map)) : null);
    }

    public <C extends PropertyInterface> InputContextPropertyListEntity<JoinProperty.Interface, C> createJoin(ImMap<V, PropertyInterfaceImplement<C>> mappedValues) {
        return new InputContextPropertyListEntity<>(view.createJoin(mappedValues), filter != null ? filter.createJoin(mappedValues) : null, orders != null ? orders.mapOrderKeys(order -> order.createJoin(mappedValues)) : null);
    }

    public InputPropertyListEntity<P, V> getSelectViewEntity() {
        return view;
    }
    public InputFilterEntity<?, V> getSelectFilterEntity() {
        return filter != null ? filter : new InputFilterEntity<>(view.property, view.mapValues); // maybe full filter should be here
    }
    public ImOrderMap<InputOrderEntity<?, V>, Boolean> getSelectOrderEntities() {
        return orders != null ? orders : MapFact.EMPTYORDER();
    }

    public InputContextListEntity<P, V> newSession() {
        return new InputContextPropertyListEntity<>(view.newSession(), filter, orders);
    }
}
