package lsfusion.server.base.version.impl.changes;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;

import java.util.List;
import java.util.function.Function;

public class NFComplexOrderSetCopy<K> implements NFComplexOrderSetChange<K> {

    public final NFComplexOrderSet<K> col;
    public final Function<K, K> mapping;

    public NFComplexOrderSetCopy(NFComplexOrderSet<K> col, Function<K, K> mapping) {
        this.col = col;
        this.mapping = mapping;
    }

    @Override
    public void proceedComplexOrderSet(List<K> list, List<Integer> groupList, Version version) {
        Pair<ImOrderSet<K>, ImList<Integer>> nf = col.getNF(version);
        for(int i=0,size=nf.first.size(); i<size; i++) {
            list.add(mapping.apply(nf.first.get(i)));

            groupList.add(nf.second.get(i));
        }
    }
}
