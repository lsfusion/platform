package lsfusion.server.logics.property.group;

import lsfusion.base.BaseUtils;
import lsfusion.base.ImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.interfaces.NFProperty;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;

import java.util.List;

public abstract class AbstractNode extends ImmutableObject {

    NFProperty<AbstractGroup> parent = NFFact.property(true);
    public AbstractGroup getParent() { return parent.get(); }
    public AbstractGroup getNFParent(Version version) { return parent.getNF(version); }
    
    public void finalizeAroundInit() {
        parent.finalizeChanges();
    }

    public abstract boolean hasChild(Property prop);

    public abstract boolean hasNFChild(Property prop, Version version);

    public abstract ImOrderSet<Property> getProperties();
    
    protected abstract ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean useObjSubsets, boolean anyInInterface, Version version);
    
    public ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classLists, boolean useObjSubsets, boolean anyInInterface, Version version) {
        return getProperties(classLists, classLists.group(new BaseUtils.Group<ValueClass, ValueClassWrapper>() { // для "кэширования" mapClasses так как очень часто вызывается
            public ValueClass group(ValueClassWrapper key) {
                return key.valueClass;
            }}), useObjSubsets, anyInInterface, version);
    }
    
    public ImList<PropertyClassImplement> getProperties(ValueClassWrapper classw, boolean anyInInterface, Version version) {
        return getProperties(SetFact.singleton(classw), false, anyInInterface, version);        
    }

    public abstract List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList);
}
