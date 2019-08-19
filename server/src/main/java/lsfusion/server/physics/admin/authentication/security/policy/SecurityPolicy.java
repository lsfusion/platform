package lsfusion.server.physics.admin.authentication.security.policy;

public class SecurityPolicy {
    public final long ID;
    public Boolean configurator;
    public Boolean editObjects;

    public SecurityPolicy() {
        this(-1);
    }

    public SecurityPolicy(long ID) {
        this.ID = ID;
    }

    public ClassSecurityPolicy cls = new ClassSecurityPolicy();
    public PropertySecurityPolicy property = new PropertySecurityPolicy();
    public NavigatorSecurityPolicy navigator = new NavigatorSecurityPolicy();
    public FormSecurityPolicy form = new FormSecurityPolicy();

    public void override(SecurityPolicy policy) {
        cls.override(policy.cls);
        property.override(policy.property);
        navigator.override(policy.navigator);
        form.override(policy.form);
        
        if (policy.configurator != null) {
            configurator = policy.configurator;
        }
        if (policy.editObjects != null) {
            editObjects = policy.editObjects;
        }
    }

    public void setReplaceMode(boolean replaceMode) {
        cls.setReplaceMode(replaceMode);
        property.setReplaceMode(replaceMode);
        navigator.replaceMode = replaceMode;
        form.replaceMode = replaceMode;
    }
}
