package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputFilterEntity<P extends PropertyInterface, V extends PropertyInterface> {
    
    public final Property<P> property;
    
    public final ImRevMap<P, V> mapValues; // external context

    public InputFilterEntity(Property<P> property, ImRevMap<P, V> mapValues) {
        this.property = property;

        this.mapValues = mapValues;
        assert singleInterface() != null;
    }

    public <C extends PropertyInterface> InputFilterEntity<P, C> map(ImRevMap<V, C> map) {
        return new InputFilterEntity<>(property, mapValues.join(map));
    }

    public static <P1 extends PropertyInterface, P2 extends PropertyInterface, V extends PropertyInterface, X extends PropertyInterface>
    InputFilterEntity<?, V> and(InputFilterEntity<P1, V> il1, InputFilterEntity<P2, V> il2) {
        if(il2 == null)
            return il1;
        if(il1 == null)
            return il2;

        ImRevMap<P1, P2> matchedInnerParams = MapFact.singletonRev(il1.singleInterface(), il2.singleInterface());

        ImRevMap<P1, P2> matchedParams = il1.mapValues.innerCrossValues(il2.mapValues).addRevExcl(matchedInnerParams);

        ImRevMap<P1, JoinProperty.Interface> firstJoinParams = il1.property.interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> rightSecondParams = il2.property.interfaces.removeIncl(matchedParams.valuesSet()).mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> secondJoinParams = rightSecondParams.addRevExcl(matchedParams.innerCrossJoin(firstJoinParams));

        ImRevMap<V, JoinProperty.Interface> mapJoinValues = il1.mapValues.crossJoin(firstJoinParams).addRevExcl(il2.mapValues.innerCrossJoin(rightSecondParams));

        PropertyMapImplement<X, JoinProperty.Interface> andProperty = (PropertyMapImplement<X, JoinProperty.Interface>)
                PropertyFact.createAnd(firstJoinParams.valuesSet().addExcl(rightSecondParams.valuesSet()),
                        new PropertyMapImplement<>(il1.property, firstJoinParams),
                        new PropertyMapImplement<>(il2.property, secondJoinParams));

        return new InputFilterEntity<>(andProperty.property, andProperty.mapping.innerCrossValues(mapJoinValues));
    }

    // input value
    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public <O extends ObjectSelector> ContextFilterEntity<P, V, O> getFilter(O object) {
        return new ContextFilterEntity<>(property, mapValues, MapFact.singletonRev(singleInterface(), object));
    }

    public PropertyMapImplement<P, V> getWhereProperty(V objectInterface) {
        return new PropertyMapImplement<>(property, mapValues.addRevExcl(singleInterface(), objectInterface));
    }

    public ImSet<V> getUsedInterfaces() {
        return mapValues.valuesSet();
    }
}
