package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;

import java.util.Set;
import java.util.function.Function;

public class NFSetCopy<K> extends NFASetCopy<NFSet<K>, K> {

    public NFSetCopy(NFSet<K> col, Map<K> mapping) {
        super(col, mapping);
    }
}
