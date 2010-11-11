package platform.server.caches;

// у кого контекст внутри, то есть можно говорить об equals который
public abstract class TwinsInnerContext<I extends TwinsInnerContext<I>> extends InnerContext<I> {

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && mapInner((I) obj,false)!=null;
    }

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashInner(false);
            hashCoded = true;
        }
        return hashCode;
    }
}
