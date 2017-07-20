package lsfusion.server.logics.mutables;

public class LastVersion extends Version {

    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    public boolean canSee(Version version) {
        return true;
    }
}
