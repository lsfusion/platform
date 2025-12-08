package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFCol;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFSet;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class NFCopy<This extends NFCol<T>, T> implements NFColChange<T> {

    public final This col;
    public final Function<T, T> mapping;

    public NFCopy(This col, Function<T, T> mapping) {
        this.col = col;
        this.mapping = mapping;
    }


    @Override
    public void proceedCol(MCol<T> mCol, Version version) {
        for(T element : col.getNFIt(version))
            mCol.add(this.mapping.apply(element));
    }
}
