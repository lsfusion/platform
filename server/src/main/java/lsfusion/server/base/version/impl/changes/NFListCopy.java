package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFCol;
import lsfusion.server.base.version.interfaces.NFList;

import java.util.function.Function;

public class NFListCopy<K> extends NFCopy<NFList<K>, K> implements NFListChange<K> {

    public NFListCopy(NFList<K> col, Function<K, K> mapping) {
        super(col, mapping);
    }

    @Override
    public void proceedList(MList<K> list, Version version) {
        for(K element : col.getNFListIt(version))
            list.add(this.mapping.apply(element));
    }
}
