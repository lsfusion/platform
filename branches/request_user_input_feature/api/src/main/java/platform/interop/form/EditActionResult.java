package platform.interop.form;

import platform.interop.action.ClientAction;

import java.io.Serializable;

public class EditActionResult implements Serializable {
    public static EditActionResult finished = new EditActionResult(null, false, null, null);

    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";

    public final ClientAction[] actions;
    public final boolean resumeInvocation;
    public final byte[] readType;
    public final byte[] oldValue;

    public EditActionResult(byte[] readType, byte[] oldValue) {
        this(null, false, readType, oldValue);
    }

    public EditActionResult(ClientAction[] actions) {
        this(actions, true);
    }

    public EditActionResult(ClientAction[] actions, boolean resumeInvocation) {
        this(actions, resumeInvocation, null, null);
    }

    public EditActionResult(ClientAction[] actions, boolean resumeInvocation, byte[] readType, byte[] oldValue) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
        this.readType = readType;
        this.oldValue = oldValue;
    }
}
