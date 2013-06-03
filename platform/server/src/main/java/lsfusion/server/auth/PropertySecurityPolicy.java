package lsfusion.server.auth;

public class PropertySecurityPolicy {

    public ViewPropertySecurityPolicy view = new ViewPropertySecurityPolicy();
    public ChangePropertySecurityPolicy change = new ChangePropertySecurityPolicy();

    public void override(PropertySecurityPolicy policy) {
        view.override(policy.view);
        change.override(policy.change);
    }

    public void setReplaceMode(boolean replaceMode) {
        view.replaceMode = replaceMode;
        change.replaceMode = replaceMode;
    }
}
