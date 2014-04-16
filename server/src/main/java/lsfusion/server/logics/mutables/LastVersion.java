package lsfusion.server.logics.mutables;

public class LastVersion implements Version {

    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    public boolean canSee(Version version) {
        return true;
    }

    public int compareTo(Version o) {
        return getOrder().compareTo(o.getOrder());
    }
}
