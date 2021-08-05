package lsfusion.server.logics.action.session.change;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.Property;

import java.util.function.Function;

public class StructChanges extends TwinImmutableObject {

    public final static Function<ModifyChange, ChangeType> getType = ModifyChange::getChangeType;

    private final ImMap<Property, ChangeType> changes;

    @Override
    public String toString() {
        return changes.toString();
    }

    public StructChanges remove(Property property) {
        assert changes.containsKey(property);
        return new StructChanges(changes.remove(property));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public int size() {
        return changes.size();
    }

    public boolean hasChanges(ImSet<Property> props) {
        for(int i=0,size=props.size();i<size;i++) {
            ChangeType changeType = changes.get(props.get(i));
            if(changeType.hasChanges)
                return true;
        }
        return false;
    }

    public StructChanges(ImMap<Property, ChangeType> changes) {
        this.changes = changes;
    }
    
    public StructChanges replace(ImSet<Property> remove, ImMap<Property, ChangeType> add) {
        return new StructChanges(changes.remove(remove).merge(add, MapFact.override()));
    }

    public StructChanges filterForProperty(Property<?> prop) {
        return new StructChanges(transformSetOrDropped(prop, changes.filter(prop.getRecDepends())));
    }

    private static ImMap<Property, ChangeType> transformSetOrDropped(Property<?> prop, ImMap<Property, ChangeType> filteredChanges) {
        // assert что recDepends включает SetOrDroppedDepends, внутри вызова есть
        ImMap<Property, Byte> setDroppedDepends = prop.getSetOrDroppedDepends();

        if(!setDroppedDepends.keys().intersect(filteredChanges.keys())) // оптимизация, тут с prev'ом может быть пересечение, которое можно не заметить, но опять таки см. isFakeChange
            return filteredChanges;

        ImFilterValueMap<Property, ChangeType> transformedChanges = filteredChanges.mapFilterValues();
        for(int i=0,size=filteredChanges.size();i<size;i++) {
            Property property = filteredChanges.getKey(i);
            ChangeType type = filteredChanges.getValue(i);
            Boolean changeSetDropped = type.getSetOrDropped();
            if (changeSetDropped != null) {
                if(!isFakeChange(setDroppedDepends, property, changeSetDropped))
                    type = ChangeType.get(type.isFinal(), null, type.hasChanges, type.hasPrevPrereads);
            }

            transformedChanges.mapValue(i, type);
        }
        return transformedChanges.immutableValue();
    }

    // должно быть синхронизировано с аналогичным методом в ChangedProperty
    // хотя тут ошибиться не так критично, так как в худшем случае пойдет по правильной но пессимистичной ветке (собсно уже сейчас может быть что Prev убьет SetOrChanged, а прямое свойство нет, но пока не хочется заморачиваться такой сложной оптимизацией)
    private static boolean isFakeChange(ImMap<Property, Byte> setDroppedDepends, Property property, Boolean changeSetDropped) {
        if(property instanceof OldProperty)
            return isSingleFakeChange(setDroppedDepends, ((OldProperty) property).property, !changeSetDropped);
        return isSingleFakeChange(setDroppedDepends, property, changeSetDropped);
    }

    private static boolean isSingleFakeChange(ImMap<Property, Byte> setDroppedDepends, Property property, boolean changeSetDropped) {
        Byte setDropped = setDroppedDepends.get(property);
        return setDropped != null && (setDropped & Property.getSetDropped(!changeSetDropped)) != 0; // если есть "противоположное" чтение - то есть в изменениях SET а у свойства DROPPED, тогда изменение принципиально (может давать Fake Change)
    }

    public ChangeType getUsedChange(Property property) {
        return changes.get(property);
    }

    public ImSet<Property> getUsedChanges(ImCol<Property> col) {
        MSet<Property> mResult = SetFact.mSet();
        for(Property<?> property : col)
            mResult.addAll(property.getUsedChanges(this));
        return mResult.immutable();
    }

    public ImSet<Property> getUsedDataChanges(ImCol<Property> col) {
        MSet<Property> mResult = SetFact.mSet();
        for(Property<?> property : col)
            mResult.addAll(property.getUsedDataChanges(this));
        return mResult.immutable();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return changes.equals(((StructChanges)o).changes);
    }

    public int immutableHashCode() {
        return changes.hashCode();
    }

    @IdentityInstanceLazy
    public StructChanges getPrev() {
        ImFilterValueMap<Property, ChangeType> mvResult = changes.mapFilterValues();
        for(int i=0,size=changes.size();i<size;i++) {
            ChangeType prevValue = changes.getValue(i).getPrev();
            if(prevValue!=null)
                mvResult.mapValue(i, prevValue);
        }
        ImMap<Property, ChangeType> changes = mvResult.immutableValue();
        if(changes.isEmpty()) // optimization of the most common case
            return PropertyChanges.EMPTY.getStruct();

        return new StructChanges(changes);
    }
}
