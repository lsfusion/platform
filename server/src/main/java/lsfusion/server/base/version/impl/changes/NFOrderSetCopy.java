package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFCol;
import lsfusion.server.base.version.interfaces.NFOrderSet;

import java.util.Set;
import java.util.function.Function;

public class NFOrderSetCopy<K> extends NFASetCopy<NFOrderSet<K>, K> implements NFOrderSetChange<K> {

    public NFOrderSetCopy(NFOrderSet<K> col, Map<K> mapping) {
        super(col, mapping);
    }

    @Override
    public void proceedOrderSet(Set<K> set, Version version) {
        for(K element : col.getNFCopyOrderSet(version)) {
            set.add(this.mapping.apply(element));
        }
    }

}
