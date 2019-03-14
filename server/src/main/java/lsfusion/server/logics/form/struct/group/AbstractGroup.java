package lsfusion.server.logics.form.struct.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.property.oraction.Property;
import lsfusion.server.logics.form.struct.property.PropertyClassImplement;
import lsfusion.server.logics.form.struct.ValueClassWrapper;

public class AbstractGroup extends AbstractNode {

    private final String canonicalName;

    public final LocalizedString caption;

    private NFOrderSet<AbstractNode> children = NFFact.orderSet(true);

    public AbstractGroup(String canonicalName, LocalizedString caption) {
        this.canonicalName = canonicalName;
        this.caption = caption;
    }

    public boolean system = false;

    public boolean createContainer() {
        return !system;
    }

    private boolean isSimple;
    public void changeChildrenToSimple(Version version) {
        isSimple = true;
        children = NFFact.simpleOrderSet(children.getNFOrderSet(version));
    }

    public ImSet<AbstractNode> getChildren() {
        return children.getSet();
    }

    public Iterable<AbstractNode> getChildrenIt() {
        return children.getIt();
    }

    public Iterable<AbstractNode> getChildrenListIt() {
        return children.getListIt();
    }

    public Iterable<AbstractNode> getNFChildrenIt(Version version) {
        return children.getNFIt(version);
    }

    public Iterable<AbstractNode> getNFChildrenListIt(Version version) {
        return children.getNFListIt(version);
    }

    public void add(AbstractNode prop, Version version) {
        assert !(version.isTemporary() && isSimple); // не добавляем в simple (без финализации) коллекции, так как может привести к утечкам памяти в EVAL
        AbstractGroup prevParent = prop.getNFParent(version);
        if (prevParent != null) {
            if (prevParent == this) // не только оптимизация, но и mutable логика
            {
                return;
            }
            prevParent.remove(prop, version);
        }
        children.add(prop, version);
        prop.parent.set(this, version);
    }

    @IdentityStartLazy
    public ImMap<Property, Integer> getIndexedPropChildren() { // оптимизация
        MExclMap<Property, Integer> mResult = MapFact.mExclMap();
        int count = 0;
        for (AbstractNode child : getChildrenListIt()) {
            count++;
            if (child instanceof Property) {
                mResult.exclAdd((Property) child, count);
            }
        }
        return mResult.immutable();
    }

    public void remove(AbstractNode prop, Version version) {
        children.remove(prop, version);
        prop.parent.set(null, version);
    }

    public boolean hasChild(Property prop) {
        for (AbstractNode child : getChildrenIt()) {
            if (child.hasChild(prop)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNFChild(Property prop, Version version) {
        for (AbstractNode child : getNFChildrenIt(version)) {
            if (child.hasNFChild(prop, version)) {
                return true;
            }
        }
        return false;
    }

    public ImOrderSet<Property> getProperties() {
        MOrderSet<Property> result = SetFact.mOrderSet();
        for (AbstractNode child : getChildrenListIt()) {
            result.addAll(child.getProperties());
        }
        return result.immutableOrder();
    }
    
    public ImList<AbstractGroup> getParentGroups() {
        MList<AbstractGroup> mResult = ListFact.mList();
        fillParentGroups(mResult);
        return mResult.immutableList();        
    }
    public void fillParentGroups(MList<AbstractGroup> mResult) {
        AbstractGroup parent = getParent();
        if (parent != null) {
            parent.fillParentGroups(mResult);
        }
        mResult.add(this);
    }
    public ImList<AbstractGroup> getChildGroups() {
        MList<AbstractGroup> mResult = ListFact.mList();
        fillChildGroups(mResult);
        return mResult.immutableList();
    }
    public void fillChildGroups(MList<AbstractGroup> mResult) {
        mResult.add(this);
        for (AbstractNode child : getChildrenListIt()) {
            if (child instanceof AbstractGroup) {
                ((AbstractGroup) child).fillChildGroups(mResult);
            }
        }
    }

    @Override
    protected ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version) {
        MList<PropertyClassImplement> mResult = ListFact.mList();
        for (AbstractNode child : getNFChildrenListIt(version)) {
            mResult.addAll(child.getProperties(valueClasses, mapClasses, version));
        }
        return mResult.immutableList();
    }

    @Override
    public String toString() {
        return caption == null ? super.toString() : ThreadLocalContext.localize(caption);
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }
    
    private String integrationSID;

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getName();
    }
    
    // todo [dale]: Используется для идентификации групп свойств в reflection, желательно перевести на canonical names
    public String getSID() {
        return CanonicalNameUtils.toSID(canonicalName);
    }

    public boolean isNamed() {
        return canonicalName != null;
    }
}
