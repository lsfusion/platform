package lsfusion.server.logics.property.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcPropertyClassImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;

import java.util.List;

// set'ы свойств, нужен в общем то когда входы динамической длины
public abstract class PropertySet extends AbstractPropertyNode {
    protected abstract Class<?> getPropertyClass();

    public boolean hasChild(Property prop) {
        return getPropertyClass().isInstance(prop);
    }

    public boolean hasNFChild(Property prop, Version version) {
        return hasChild(prop);
    }

    @Override
    protected ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean useObjSubsets, boolean anyInInterface, Version version) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : FormEntity.getSubsets(valueClasses, useObjSubsets)) {
            if (isInInterface(classes)) {
                mResultList.addAll(getProperties(classes, version));
            }
        }
        return mResultList.immutableList();
    }

    public ImList<Property> getProperties(Version version, ValueClass... classes) {
        MExclSet<ValueClassWrapper> mClassList = SetFact.mExclSet(classes.length); // массивы
        for (ValueClass cls : classes) {
            mClassList.exclAdd(new ValueClassWrapper(cls));
        }

        ImList<PropertyClassImplement> clsImplements = getProperties(mClassList.immutable(), false, true, version);
        return clsImplements.mapListValues(new GetValue<Property, PropertyClassImplement>() {
            public Property getMapValue(PropertyClassImplement value) {
                return value.property;
            }});
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected abstract ImList<CalcPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classes, Version version);

    protected abstract boolean isInInterface(ImSet<ValueClassWrapper> classes);

    protected void setParent(AbstractNode node, Version version) {
        node.parent.set(parent.getNF(version), version);
    }
}
