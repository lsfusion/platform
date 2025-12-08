package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.Version;

import java.util.function.Function;

public interface NFSet<T> extends NFCol<T> {

    void add(NFSet<T> element, Function<T, T> mapper, Version version);

    ImSet<T> getNFSet(Version version);
    boolean containsNF(T element, Version version);
    ImSet<T> getSet();

    void remove(T element, Version version);

}
