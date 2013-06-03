package lsfusion.server.logics.property.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.CalcPropertyClassImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;

import java.util.List;

// set'ы свойств, нужен в общем то когда входы динамической длины
public abstract class PropertySet extends AbstractNode {
    protected abstract Class<?> getPropertyClass();

    @Override
    public boolean hasChild(Property prop) {
        return getPropertyClass().isInstance(prop);
    }

    @Override
    public ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : classLists) {
            if (isInInterface(classes)) {
                mResultList.addAll(getProperties(classes));
            }
        }
        return mResultList.immutableList();
    }

    public ImList<Property> getProperties(ValueClass... classes) {
        MExclSet<ValueClassWrapper> mClassList = SetFact.mExclSet(classes.length); // массивы
        for (ValueClass cls : classes) {
            mClassList.exclAdd(new ValueClassWrapper(cls));
        }

        ImList<PropertyClassImplement> clsImplements = getProperties(SetFact.singleton(mClassList.immutable()), true);
        return clsImplements.mapListValues(new GetValue<Property, PropertyClassImplement>() {
            public Property getMapValue(PropertyClassImplement value) {
                return value.property;
            }});
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected abstract ImList<CalcPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classes);

    protected abstract boolean isInInterface(ImSet<ValueClassWrapper> classes);

    protected void setParent(AbstractNode node) {
        node.parent = parent;
    }
}
