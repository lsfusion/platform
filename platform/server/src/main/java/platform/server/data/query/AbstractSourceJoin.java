package platform.server.data.query;

import platform.server.where.DataWhereSet;
import net.jcip.annotations.Immutable;

abstract public class AbstractSourceJoin implements SourceJoin {

    boolean hashed = false;
    int hash;
    public int hash() {
        if(!hashed) {
            hash = DataWhereSet.hash(getHash());
            hashed = true;
        }
        return hash;
    }
    protected abstract int getHash();

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = getHashCode();
            hashCoded = true;
        }
        return hashCode;
    }
    protected int getHashCode() {
        return System.identityHashCode(this);
    }
}
