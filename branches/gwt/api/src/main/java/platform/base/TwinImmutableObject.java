package platform.base;

public abstract class TwinImmutableObject extends ImmutableObject implements TwinImmutableInterface {

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((TwinImmutableInterface) obj);
    }

    public static boolean equals(TwinImmutableInterface obj1, Object obj2) {
        return obj1 == obj2 || obj2!=null && obj1.getClass() == obj2.getClass() && obj1.twins((TwinImmutableInterface) obj2);
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
}
