package lsfusion.server.logics.form.struct.group;

import lsfusion.base.BaseUtils;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.form.struct.ValueClassWrapper;

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
    
    protected abstract ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version);
    
    public ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classLists, Version version) {
        return getProperties(classLists, classLists.group(new BaseUtils.Group<ValueClass, ValueClassWrapper>() { // для "кэширования" mapClasses так как очень часто вызывается
            public ValueClass group(ValueClassWrapper key) {
                return key.valueClass;
            }}), version);
    }
    
    public ImList<PropertyClassImplement> getProperties(ValueClass valueClass, Version version) {
        return getProperties(valueClass != null ? SetFact.singleton(new ValueClassWrapper(valueClass)) : SetFact.<ValueClassWrapper>EMPTY(), version);
    }
}
