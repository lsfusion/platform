package platform.server.auth;

public class SecurityPolicy {
    public final int ID;

    SecurityPolicy() {
        this(-1);
    }

    public SecurityPolicy(int ID) {
        this.ID = ID;
    }

    public ClassSecurityPolicy cls = new ClassSecurityPolicy();
    public PropertySecurityPolicy property = new PropertySecurityPolicy();
    public NavigatorSecurityPolicy navigator = new NavigatorSecurityPolicy();

    public void override(SecurityPolicy policy) {
        cls.override(policy.cls);
        property.override(policy.property);
        navigator.override(policy.navigator);
    }

    public void setReplaceMode(boolean replaceMode) {
        cls.setReplaceMode(replaceMode);
        property.setReplaceMode(replaceMode);
        navigator.replaceMode = true;
    }
}
