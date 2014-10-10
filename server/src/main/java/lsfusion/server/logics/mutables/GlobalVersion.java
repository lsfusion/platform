package lsfusion.server.logics.mutables;

public class GlobalVersion extends Version {
    @Override
    public Integer getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean canSee(Version version) {
        return version == this;
    }
}
