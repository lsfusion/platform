package lsfusion.server.auth;

public class EditClassSecurityPolicy {

    public AddClassSecurityPolicy add = new AddClassSecurityPolicy();
    public RemoveClassSecurityPolicy remove = new RemoveClassSecurityPolicy();
    public ChangeClassSecurityPolicy change = new ChangeClassSecurityPolicy();

    public void override(EditClassSecurityPolicy edit) {
        add.override(edit.add);
        remove.override(edit.remove);
        change.override(edit.change);
    }

    public void setReplaceMode(boolean replaceMode) {
        add.replaceMode = replaceMode;
        remove.replaceMode = replaceMode;
        change.replaceMode = replaceMode;
    }
}
