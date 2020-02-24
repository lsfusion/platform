package lsfusion.server.physics.admin.authentication.security.policy;

public class EditClassSecurityPolicy {

    public AddClassSecurityPolicy add = new AddClassSecurityPolicy();
    public RemoveClassSecurityPolicy remove = new RemoveClassSecurityPolicy();
    public ChangeClassSecurityPolicy change = new ChangeClassSecurityPolicy();

    public void override(EditClassSecurityPolicy edit) {
        add.override(edit.add);
        remove.override(edit.remove);
        change.override(edit.change);
    }
}
