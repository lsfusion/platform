package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;

import java.util.Set;
import java.util.function.Function;

public class NFASetCopy<This extends NFSet<K>, K> extends NFCopy<This, K> implements NFSetChange<K> {

    public NFASetCopy(This col, Function<K, K> mapping) {
        super(col, mapping);
    }

    @Override
    public void proceedSet(Set<K> mSet, Version version) {
        for(K element : col.getNFCopySet(version))
            mSet.add(this.mapping.apply(element));
    }
}
