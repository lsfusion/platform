package platform.base.identity;

public class DefaultSIDGenerator extends DefaultIDGenerator {
    private final String prefix;

    public DefaultSIDGenerator(String prefix) {
        this.prefix = prefix;
    }

    public String genSID() {
        return prefix + idShift(1);
    }
}
