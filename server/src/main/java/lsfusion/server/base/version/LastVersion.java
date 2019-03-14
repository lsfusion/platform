package lsfusion.server.base.version;

public class LastVersion extends Version {

    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    public boolean canSee(Version version) {
        return true;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }
}
