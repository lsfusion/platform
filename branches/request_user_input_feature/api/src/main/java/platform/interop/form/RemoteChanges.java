package platform.interop.form;

import platform.interop.action.ClientAction;

import java.io.Serializable;

public class RemoteChanges implements Serializable {

    public final byte[] formChanges;
    public final ClientAction[] actions;
    public final int currentClassId;
    public final boolean resumeInvocation;

    public RemoteChanges(byte[] formChanges, ClientAction[] actions, int currentClassId, boolean resumeInvocation) {
        this.formChanges = formChanges;
        this.actions = actions;
        this.currentClassId = currentClassId;
        this.resumeInvocation = resumeInvocation;
    }
}
