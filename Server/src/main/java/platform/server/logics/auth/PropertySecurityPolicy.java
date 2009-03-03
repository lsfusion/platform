package platform.server.logics.auth;

public class PropertySecurityPolicy {

    public ViewPropertySecurityPolicy view = new ViewPropertySecurityPolicy();
    public ChangePropertySecurityPolicy change = new ChangePropertySecurityPolicy();

    public void override(PropertySecurityPolicy policy) {
        view.override(policy.view);
        change.override(policy.change);
    }
}
