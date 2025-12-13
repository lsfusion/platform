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
    public final NFCopy.Map<K> mapping;

    public NFComplexOrderSetCopy(NFComplexOrderSet<K> col, NFCopy.Map<K> mapping) {
        this.col = col;
        this.mapping = mapping;
    }

    @Override
    public void proceedComplexOrderSet(List<K> list, List<Integer> groupList, Version version) {
        Pair<ImOrderSet<K>, ImList<Integer>> nf = col.getNFCopy(version);
        for(int i=0,size=nf.first.size(); i<size; i++) {
            K mappedElement = mapping.apply(nf.first.get(i));
            if(mappedElement != null && !list.contains(mappedElement)) {
                list.add(mappedElement);

                groupList.add(nf.second.get(i));
            }
        }
    }
}
