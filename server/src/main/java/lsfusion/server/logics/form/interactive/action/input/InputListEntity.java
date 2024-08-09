package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.AppImage;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import static lsfusion.server.logics.property.oraction.PropertyInterface.getIdentityMap;

// pretty similar to ContextFilterEntity
public abstract class InputListEntity<P extends PropertyInterface, V extends PropertyInterface, T extends ActionOrProperty<P>> {

    protected final T property;

    protected final ImRevMap<P, V> mapValues; // external context

    public final boolean newSession;

    protected InputListEntity(T property, ImRevMap<P, V> mapValues, boolean newSession) {
        this.property = property;
        this.mapValues = mapValues;
        this.newSession = newSession;

        assert property.interfaces.containsAll(mapValues.keys());
    }

    public static <P extends PropertyInterface, V extends PropertyInterface, T extends ActionOrProperty<P>> InputListEntity<P, V, T> create(ActionOrProperty<P> property, ImRevMap<P, V> mapValues) {
        if (property instanceof Property)
            return (InputListEntity<P, V, T>) new InputPropertyListEntity<>((Property<P>)property, mapValues);
        else
            return (InputListEntity<P, V, T>) new InputActionListEntity<>((Action<P>) property, mapValues);
    }

    public P singleInterface() {
        return getInterfaces().single();
    }

    public ImSet<P> getInterfaces() {
        return property.interfaces.removeIncl(mapValues.keys());
    }

    public <C extends PropertyInterface> InputListEntity<P, C, ?> map(ImRevMap<V, C> map) {
        return create(mapValues.join(map));
    }

    public <C extends PropertyInterface> InputListEntity<P, C, ?> mapInner(ImRevMap<V, C> map) {
        // here it's not evident if we should consider the case like FOR f=g(a) DO INPUT ... LIST x(d) IF g(d) = f as a simple input
        // we won't since we don't do that in FilterEntity, ContextFilterEntity.getInputListEntity
        ImRevMap<P, C> joinMapValues = mapValues.innerJoin(map);
        if(joinMapValues.size() != mapValues.size())
            return null;
        return create(joinMapValues);
    }

    public <C extends PropertyInterface> InputListEntity<?, C, ?> mapJoin(ImMap<V, PropertyInterfaceImplement<C>> mapping) {
        ImMap<P, PropertyInterfaceImplement<C>> mappedValues = mapValues.join(mapping);

        ImRevMap<P, C> identityMap = getIdentityMap(mappedValues);
        if(identityMap != null) // optimization + fix of not empty orders
            return create(identityMap);

        return createJoin(mappedValues);
    }

    public abstract InputListEntity<P, V, ?> newSession();

    protected abstract <C extends PropertyInterface> InputListEntity<P, C, ?> create(ImRevMap<P, C> joinMapValues);

    protected abstract <C extends PropertyInterface> InputListEntity<?, C, ?> createJoin(ImMap<P, PropertyInterfaceImplement<C>> mappedValues);

    public abstract InputValueList<P, ?> map(ImMap<V, ? extends ObjectValue> map);

    public ImMap<V, ValueClass> getInterfaceClasses() {
        return mapValues.innerCrossJoin(property.getInterfaceClasses(ClassType.wherePolicy));
    }

    public static <X extends PropertyInterface, V extends PropertyInterface> InputContextAction<?, V> getResetAction(BaseLogicsModule baseLM, LP targetProp) {
        assert targetProp.listInterfaces.isEmpty();
        LA<X> reset = (LA<X>) baseLM.addResetAProp(targetProp);
        return new InputContextAction<>(AppServerImage.RESET, AppImage.INPUT_RESET, (String)null, null, null, QuickAccess.DEFAULT, reset.getActionOrProperty(), MapFact.EMPTYREV());
    }

//    public boolean isDefaultWYSInput() {
//        return Property.isDefaultWYSInput(property.getValueClass(ClassType.tryEditPolicy));
//    }

    @Override
    public String toString() {
        return property + "(" + mapValues + ")" + (newSession ? " NEW" : "");
    }
}
