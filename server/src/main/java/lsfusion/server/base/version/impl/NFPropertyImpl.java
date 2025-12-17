package lsfusion.server.base.version.impl;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFCopy;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFProperty;

public class NFPropertyImpl<K> extends NFImpl<NFList<K>, K> implements NFProperty<K> {

    public NFPropertyImpl() {
    }

    public NFPropertyImpl(K changes) {
        super(changes);
    }

    protected NFList<K> initMutable() {
        return new NFListImpl<>();
    }

    public K getNF(Version version) {
        return getNF(version, false);
    }

    public K getNF(Version version, boolean allowRead) {
        if(checkVersionFinal(version, allowRead)) // не proceedVersionFinal, так как результат может быть null и его не отличишь
            return getFinalChanges();
        
        ImList<K> list = getChanges().getNFList(version);
        int last = list.size();
        if(last == 0)
            return null;
        else
            return list.get(list.size() - 1);
    }

    protected boolean checkFinal(Object object) {
        return !(object instanceof NFList);
    }

    public void set(K value, Version version) {
        getChanges().add(value, version);
    }

    public void set(NFProperty<K> value, NFCopy.Map<K> mapping, Version version) {
        Object setChanges = ((NFPropertyImpl<K>) value).getChangesAsIs();
        NFList<K> changes = getChanges();
        if(setChanges instanceof NFList) {
            changes.add((NFList<K>)setChanges, mapping, version);
        } else
            changes.add(mapping.apply((K)setChanges), version);
    }

    public K get() {
        return getFinal();
    }
}
