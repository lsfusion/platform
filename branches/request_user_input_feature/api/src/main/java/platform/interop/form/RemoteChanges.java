package platform.interop.form;

import platform.interop.action.ClientAction;

import java.io.Serializable;

public class RemoteChanges implements Serializable {

    public final byte[] formChanges;
    public final ClientAction[] actions;
    public final int classID;
    public final boolean resumeInvocation;

    public RemoteChanges(byte[] formChanges, ClientAction[] actions, int classID, boolean resumeInvocation) {
        this.formChanges = formChanges;
        this.actions = actions;
        this.classID = classID;
        this.resumeInvocation = resumeInvocation;
    }
}
