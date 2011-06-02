package platform.server.caches;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;

// у кого контекст внутри, то есть можно говорить об equals который
public abstract class TwinsInnerContext<I extends TwinsInnerContext<I>> extends InnerContext<I> implements TwinImmutableInterface {

    // множественное наследование TwinImmutableObject {

    @Override
    public boolean equals(Object o) {
        return TwinImmutableObject.equals(this, o);
    }

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = immutableHashCode();
            hashCoded = true;
        }
        return hashCode;
    }

    // }

    public boolean twins(TwinImmutableInterface o) {
        return mapInner((I) o,false)!=null;
    }

    public int immutableHashCode() {
        return hashInner(false);
    }
}
