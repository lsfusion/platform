package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFCol;
import lsfusion.server.base.version.interfaces.NFOrderSet;

import java.util.List;
import java.util.function.Function;

public class NFOrderSetCopy<K> extends NFASetCopy<NFOrderSet<K>, K> implements NFOrderSetChange<K> {

    public NFOrderSetCopy(NFOrderSet<K> col, Function<K, K> mapping) {
        super(col, mapping);
    }

    @Override
    public void proceedOrderSet(List<K> list, Version version) {
        for(K element : col.getNFOrderSet(version))
            list.add(this.mapping.apply(element));
    }

}
