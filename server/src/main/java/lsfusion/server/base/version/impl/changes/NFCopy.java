package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFCol;

public class NFCopy<This extends NFCol<T>, T> implements NFColChange<T> {

    public interface Map<T> {
        T apply(T element);
    }

    public final This col;
    public final Map<T> mapping;

    public NFCopy(This col, Map<T> mapping) {
        this.col = col;
        this.mapping = mapping;
    }


    @Override
    public void proceedCol(MCol<T> mCol, Version version) {
        for(T element : col.getNFCopyIt(version)) {
            mCol.add(this.mapping.apply(element));
        }
    }
}
