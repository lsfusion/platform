package platform.server.auth;

public class ClassSecurityPolicy {

    ViewClassSecurityPolicy view = new ViewClassSecurityPolicy();
    public EditClassSecurityPolicy edit = new EditClassSecurityPolicy();

    public void override(ClassSecurityPolicy cls) {
        view.override(cls.view);
        edit.override(cls.edit);
    }

    public void setReplaceMode(boolean replaceMode) {
        edit.setReplaceMode(replaceMode);
    }
}
