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
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

public class Group extends AbstractNode {

    private final String canonicalName;

    public final LocalizedString caption;

    private NFOrderSet<AbstractNode> children = NFFact.orderSet(true);

    public Group(String canonicalName, LocalizedString caption) {
        this.canonicalName = canonicalName;
        this.caption = caption;
    }
    
    public static final Group NOGROUP = new Group(null, null); 

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

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        for(AbstractNode child : getChildrenListIt()) // getting children list automatically finalizes it
            if(child instanceof Group)
                child.finalizeAroundInit();
    }

    public void add(AbstractNode prop, Version version) {
        Group prevParent = prop.getNFParent(version);
        if (prevParent != null) {
            if (prevParent == this) // не только оптимизация, но и mutable логика
            {
                return;
            }
            prevParent.remove(prop, version);
        }
        children.add(prop, version);
        prop.setParent(this, version);
    }

    @IdentityStartLazy
    public ImMap<ActionOrProperty, Integer> getIndexedPropChildren() { // оптимизация
        MExclMap<ActionOrProperty, Integer> mResult = MapFact.mExclMap();
        int count = 0;
        for (AbstractNode child : getChildrenListIt()) {
            count++;
            if (child instanceof ActionOrProperty) {
                mResult.exclAdd((ActionOrProperty) child, count); // can be not exclusive when the property is cached and then added several times to private group (see the whole commit for example)
            }
        }
        return mResult.immutable();
    }

    public void remove(AbstractNode prop, Version version) {
        children.remove(prop, version);
        prop.parent.set(null, version);
    }

    public boolean hasChild(ActionOrProperty prop) {
        for (AbstractNode child : getChildrenIt()) {
            if (child.hasChild(prop)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNFChild(ActionOrProperty prop, Version version) {
        for (AbstractNode child : getNFChildrenIt(version)) {
            if (child.hasNFChild(prop, version)) {
                return true;
            }
        }
        return false;
    }

    public ImOrderSet<ActionOrProperty> getActionOrProperties() {
        MOrderSet<ActionOrProperty> result = SetFact.mOrderSet();
        for (AbstractNode child : getChildrenListIt()) {
            result.addAll(child.getActionOrProperties());
        }
        return result.immutableOrder();
    }
    
    public ImList<Group> getParentGroups() {
        MList<Group> mResult = ListFact.mList();
        fillParentGroups(mResult);
        return mResult.immutableList();        
    }
    public void fillParentGroups(MList<Group> mResult) {
        Group parent = getParent();
        if (parent != null) {
            parent.fillParentGroups(mResult);
        }
        mResult.add(this);
    }
    public ImList<Group> getChildGroups() {
        MList<Group> mResult = ListFact.mList();
        fillChildGroups(mResult);
        return mResult.immutableList();
    }
    public void fillChildGroups(MList<Group> mResult) {
        mResult.add(this);
        for (AbstractNode child : getChildrenListIt()) {
            if (child instanceof Group) {
                ((Group) child).fillChildGroups(mResult);
            }
        }
    }

    @Override
    protected ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version) {
        MList<ActionOrPropertyClassImplement> mResult = ListFact.mList();
        for (AbstractNode child : getNFChildrenListIt(version)) {
            mResult.addAll(child.getActionOrProperties(valueClasses, mapClasses, version));
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

    public ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImSet<ValueClassWrapper> classLists, Version version) {
        return getActionOrProperties(classLists, classLists.group(key -> key.valueClass), version);
    }

    public ImList<ActionOrPropertyClassImplement> getActionOrProperties(ValueClass valueClass, Version version) {
        return getActionOrProperties(valueClass != null ? SetFact.singleton(new ValueClassWrapper(valueClass)) : SetFact.EMPTY(), version);
    }
}
