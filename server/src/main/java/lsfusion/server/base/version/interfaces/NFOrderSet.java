package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.FindIndex;
import lsfusion.server.base.version.Version;

public interface NFOrderSet<T> extends NFSet<T>, NFList<T> {

    void addFirst(T element, Version version);
    void add(T element, FindIndex<T> finder, Version version);
    
    void move(T element, FindIndex<T> finder, Version version);
    void move(T element, T otherElement, boolean isRightNeighbour, Version version);

    void addIfNotExistsToThenLast(T element, T to, boolean isRightNeighbour, Version version); /*{ // assert что нет
        if (!children.contains(compBefore)) {
            add(comp, version);
        } else {
            //сначала remove, чтобы indexOf вернул правильный индекс
            remove(comp, version);
            add(children.indexOf(compBefore), comp, version);
        }

    }
*/
    int size(Version version);

    ImOrderSet<T> getNFOrderSet(Version version);

    ImOrderSet<T> getOrderSet();
}
